/*
 * Copyright 2019 the original author or authors.
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
package org.glowroot.agent.it.harness.agent;

import java.util.concurrent.TimeUnit;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.glowroot.agent.it.harness.model.ImmutableTrace;
import org.glowroot.xyzzy.engine.bytecode.api.ThreadContextPlus;
import org.glowroot.xyzzy.engine.bytecode.api.ThreadContextThreadLocal;
import org.glowroot.xyzzy.engine.impl.NopTransactionService;
import org.glowroot.xyzzy.instrumentation.api.AsyncQueryEntry;
import org.glowroot.xyzzy.instrumentation.api.AsyncTraceEntry;
import org.glowroot.xyzzy.instrumentation.api.AuxThreadContext;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.QueryEntry;
import org.glowroot.xyzzy.instrumentation.api.QueryMessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.Timer;
import org.glowroot.xyzzy.instrumentation.api.TimerName;
import org.glowroot.xyzzy.instrumentation.api.TraceEntry;

public class ThreadContextImpl implements ThreadContextPlus {

    private final ThreadContextThreadLocal.Holder threadContextHolder;

    private final ImmutableTrace.Builder trace;

    private @Nullable ServletRequestInfo servletRequestInfo;

    private int currentNestingGroupId;
    private int currentSuppressionKeyId;

    public ThreadContextImpl(ThreadContextThreadLocal.Holder threadContextHolder,
            ImmutableTrace.Builder trace, @Nullable ServletRequestInfo servletRequestInfo,
            int rootNestingGroupId, int rootSuppressionKeyId) {
        this.threadContextHolder = threadContextHolder;
        this.trace = trace;
        this.servletRequestInfo = servletRequestInfo;
        currentNestingGroupId = rootNestingGroupId;
        currentSuppressionKeyId = rootSuppressionKeyId;
    }

    ThreadContextThreadLocal.Holder getThreadContextHolder() {
        return threadContextHolder;
    }

    @Override
    public boolean isInTransaction() {
        return true;
    }

    @Override
    public TraceEntry startTransaction(String transactionType, String transactionName,
            MessageSupplier messageSupplier, TimerName timerName) {
        return NopTransactionService.TRACE_ENTRY;
    }

    @Override
    public TraceEntry startTransaction(String transactionType, String transactionName,
            MessageSupplier messageSupplier, TimerName timerName,
            AlreadyInTransactionBehavior alreadyInTransactionBehavior) {
        return NopTransactionService.TRACE_ENTRY;
    }

    @Override
    public TraceEntry startTraceEntry(MessageSupplier messageSupplier, TimerName timerName) {
        return NopTransactionService.TRACE_ENTRY;
    }

    @Override
    public AsyncTraceEntry startAsyncTraceEntry(MessageSupplier messageSupplier,
            TimerName timerName) {
        return NopTransactionService.ASYNC_TRACE_ENTRY;
    }

    @Override
    public QueryEntry startQueryEntry(String queryType, String queryText,
            QueryMessageSupplier queryMessageSupplier, TimerName timerName) {
        // TODO pass along queryType
        return new QueryEntryImpl(trace, queryText, queryMessageSupplier);
    }

    @Override
    public QueryEntry startQueryEntry(String queryType, String queryText, long queryExecutionCount,
            QueryMessageSupplier queryMessageSupplier, TimerName timerName) {
        // TODO pass along queryType and queryExecutionCount
        return new QueryEntryImpl(trace, queryText, queryMessageSupplier);
    }

    @Override
    public AsyncQueryEntry startAsyncQueryEntry(String queryType, String queryText,
            QueryMessageSupplier queryMessageSupplier, TimerName timerName) {
        // TODO pass along queryType
        return new QueryEntryImpl(trace, queryText, queryMessageSupplier);
    }

    @Override
    public TraceEntry startServiceCallEntry(String type, String text,
            MessageSupplier messageSupplier, TimerName timerName) {
        // TODO pass along type
        return new TraceEntryImpl(trace, messageSupplier);
    }

    @Override
    public AsyncTraceEntry startAsyncServiceCallEntry(String type, String text,
            MessageSupplier messageSupplier, TimerName timerName) {
        // TODO pass along type
        return new TraceEntryImpl(trace, messageSupplier);
    }

    @Override
    public Timer startTimer(TimerName timerName) {
        return NopTransactionService.TIMER;
    }

    @Override
    public AuxThreadContext createAuxThreadContext() {
        return new AuxThreadContextImpl(trace, servletRequestInfo);
    }

    @Override
    public void setTransactionAsync() {}

    @Override
    public void setTransactionAsyncComplete() {}

    @Override
    public void setTransactionOuter() {}

    @Override
    public void setTransactionType(String transactionType, int priority) {}

    @Override
    public void setTransactionName(String transactionName, int priority) {}

    @Override
    public void setTransactionUser(String user, int priority) {}

    @Override
    public void addTransactionAttribute(String name, String value) {}

    @Override
    public void setTransactionSlowThreshold(long threshold, TimeUnit unit, int priority) {}

    @Override
    public void setTransactionError(Throwable t) {}

    @Override
    public void setTransactionError(String message) {}

    @Override
    public void setTransactionError(String message, Throwable t) {}

    @Override
    public void addErrorEntry(Throwable t) {}

    @Override
    public void addErrorEntry(String message) {}

    @Override
    public void addErrorEntry(String message, Throwable t) {}

    @Override
    public void trackResourceAcquired(Object resource, boolean withLocationStackTrace) {}

    @Override
    public void trackResourceReleased(Object resource) {}

    @Override
    public @Nullable ServletRequestInfo getServletRequestInfo() {
        return servletRequestInfo;
    }

    @Override
    public void setServletRequestInfo(@Nullable ServletRequestInfo servletRequestInfo) {
        this.servletRequestInfo = servletRequestInfo;
    }

    @Override
    public int getCurrentNestingGroupId() {
        return currentNestingGroupId;
    }

    @Override
    public void setCurrentNestingGroupId(int nestingGroupId) {
        this.currentNestingGroupId = nestingGroupId;
    }

    @Override
    public int getCurrentSuppressionKeyId() {
        return currentSuppressionKeyId;
    }

    @Override
    public void setCurrentSuppressionKeyId(int suppressionKeyId) {
        this.currentSuppressionKeyId = suppressionKeyId;
    }
}
