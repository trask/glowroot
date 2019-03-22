/*
 * Copyright 2011-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glowroot.agent.impl;

import com.google.common.base.Ticker;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.glowroot.agent.config.ConfigService;
import org.glowroot.agent.impl.Transaction.CompletionCallback;
import org.glowroot.agent.model.MergedThreadTimer;
import org.glowroot.agent.util.ThreadAllocatedBytes;
import org.glowroot.common.config.AdvancedConfig;
import org.glowroot.common.util.Clock;
import org.glowroot.xyzzy.engine.bytecode.api.ThreadContextPlus;
import org.glowroot.xyzzy.engine.bytecode.api.ThreadContextThreadLocal;
import org.glowroot.xyzzy.engine.impl.OptionalThreadContextImpl;
import org.glowroot.xyzzy.engine.impl.TimerNameCache;
import org.glowroot.xyzzy.engine.spi.AgentSPI;
import org.glowroot.xyzzy.engine.util.IterableWithSelfRemovableEntries;
import org.glowroot.xyzzy.engine.util.IterableWithSelfRemovableEntries.SelfRemovableEntry;
import org.glowroot.xyzzy.instrumentation.api.Getter;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.Span;
import org.glowroot.xyzzy.instrumentation.api.ThreadContext.ServletRequestInfo;
import org.glowroot.xyzzy.instrumentation.api.TimerName;
import org.glowroot.xyzzy.instrumentation.api.config.ConfigListener;

public class TransactionRegistry implements AgentSPI, ConfigListener {

    // collection of active running transactions
    private final IterableWithSelfRemovableEntries<Transaction> transactions =
            new IterableWithSelfRemovableEntries<Transaction>();

    // active thread context being executed by the current thread
    private final ThreadContextThreadLocal threadContextThreadLocal;

    private final ConfigService configService;
    private final TimerName auxThreadTimerName;

    private final Clock clock;
    private final Ticker ticker;

    private final TransactionCompletionCallback transactionCompletionCallback =
            new TransactionCompletionCallback();

    // cache for fast read access
    // visibility is provided by memoryBarrier below
    private boolean captureThreadStats;
    private int maxTraceEntries;
    private int maxQueryAggregates;
    private int maxServiceCallAggregates;
    private int maxProfileSamples;

    // intentionally not volatile for small optimization
    private @MonotonicNonNull TransactionProcessor transactionProcessor;
    // intentionally not volatile for small optimization
    private @Nullable ThreadAllocatedBytes threadAllocatedBytes;

    public static TransactionRegistry create(ThreadContextThreadLocal threadContextThreadLocal,
            ConfigService configService, TimerNameCache timerNameCache, Ticker ticker,
            Clock clock) {
        TransactionRegistry transactionRegistry = new TransactionRegistry(threadContextThreadLocal,
                configService, timerNameCache, ticker, clock);
        configService.addConfigListener(transactionRegistry);
        return transactionRegistry;
    }

    private TransactionRegistry(ThreadContextThreadLocal threadContextThreadLocal,
            ConfigService configService, TimerNameCache timerNameCache,
            Ticker ticker, Clock clock) {
        this.threadContextThreadLocal = threadContextThreadLocal;
        this.configService = configService;
        this.clock = clock;
        this.ticker = ticker;
        auxThreadTimerName =
                timerNameCache.getTimerName(MergedThreadTimer.AUXILIARY_THREAD_ROOT_TIMER_NAME);
    }

    @Nullable
    Transaction getCurrentTransaction() {
        ThreadContextImpl threadContext = (ThreadContextImpl) threadContextThreadLocal.get();
        if (threadContext == null) {
            return null;
        }
        return threadContext.getTransaction();
    }

    public ThreadContextThreadLocal.Holder getCurrentThreadContextHolder() {
        return threadContextThreadLocal.getHolder();
    }

    SelfRemovableEntry addTransaction(Transaction transaction) {
        return transactions.add(transaction);
    }

    public Iterable<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactionProcessor(TransactionProcessor transactionProcessor) {
        this.transactionProcessor = transactionProcessor;
    }

    public void setThreadAllocatedBytes(@Nullable ThreadAllocatedBytes threadAllocatedBytes) {
        this.threadAllocatedBytes = threadAllocatedBytes;
    }

    @Override
    public <C> Span startIncomingSpan(String transactionType, String transactionName,
            Getter<C> getter, C carrier, MessageSupplier messageSupplier, TimerName timerName,
            ThreadContextThreadLocal.Holder threadContextHolder, int rootNestingGroupId,
            int rootSuppressionKeyId) {
        // ensure visibility of recent configuration updates
        configService.readMemoryBarrier();
        long startTick = ticker.read();
        Transaction transaction = new Transaction(clock.currentTimeMillis(), startTick,
                transactionType, transactionName, messageSupplier, timerName, captureThreadStats,
                maxTraceEntries, maxQueryAggregates, maxServiceCallAggregates, maxProfileSamples,
                threadAllocatedBytes, transactionCompletionCallback, ticker, this, configService,
                threadContextHolder, rootNestingGroupId, rootSuppressionKeyId);
        SelfRemovableEntry transactionEntry = addTransaction(transaction);
        transaction.setTransactionEntry(transactionEntry);
        threadContextHolder.set(transaction.getMainThreadContext());
        return transaction.getMainThreadContext().getRootEntry();
    }

    @Nullable
    ThreadContextImpl startAuxThreadContextInternal(Transaction transaction,
            @Nullable TraceEntryImpl parentTraceEntry,
            @Nullable TraceEntryImpl parentThreadContextPriorEntry,
            @Nullable ServletRequestInfo servletRequestInfo,
            ThreadContextThreadLocal.Holder threadContextHolder) {
        long startTick = ticker.read();
        return transaction.startAuxThreadContext(parentTraceEntry, parentThreadContextPriorEntry,
                auxThreadTimerName, startTick, threadContextHolder, servletRequestInfo,
                threadAllocatedBytes);
    }

    public ThreadContextPlus createOptionalThreadContext(
            ThreadContextThreadLocal.Holder threadContextHolder, int currentNestingGroupId,
            int currentSuppressionKeyId) {
        return new OptionalThreadContextImpl(this, threadContextHolder, currentNestingGroupId,
                currentSuppressionKeyId);
    }

    @Override
    public void onChange() {
        AdvancedConfig advancedConfig = configService.getAdvancedConfig();
        captureThreadStats = configService.getTransactionConfig().captureThreadStats();
        maxQueryAggregates = advancedConfig.maxQueryAggregates();
        maxServiceCallAggregates = advancedConfig.maxServiceCallAggregates();
        maxTraceEntries = advancedConfig.maxTraceEntriesPerTransaction();
        maxProfileSamples = advancedConfig.maxProfileSamplesPerTransaction();
    }

    private class TransactionCompletionCallback implements CompletionCallback {

        @Override
        public void completed(Transaction transaction) {
            if (transactionProcessor != null) {
                transactionProcessor.processOnCompletion(transaction);
            }
        }
    }
}
