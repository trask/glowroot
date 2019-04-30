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
package org.glowroot.xyzzy.test.harness.agent;

import java.util.concurrent.TimeUnit;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.glowroot.xyzzy.engine.bytecode.api.ThreadContextPlus;
import org.glowroot.xyzzy.engine.bytecode.api.ThreadContextThreadLocal;
import org.glowroot.xyzzy.engine.impl.NopTransactionService;
import org.glowroot.xyzzy.engine.impl.TimerNameImpl;
import org.glowroot.xyzzy.instrumentation.api.AsyncQueryEntry;
import org.glowroot.xyzzy.instrumentation.api.AsyncTraceEntry;
import org.glowroot.xyzzy.instrumentation.api.AuxThreadContext;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.QueryEntry;
import org.glowroot.xyzzy.instrumentation.api.QueryMessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.Timer;
import org.glowroot.xyzzy.instrumentation.api.TimerName;
import org.glowroot.xyzzy.instrumentation.api.TraceEntry;
import org.glowroot.xyzzy.test.harness.agent.spans.AsyncOutgoingSpanImpl;
import org.glowroot.xyzzy.test.harness.agent.spans.AsyncQuerySpanImpl;
import org.glowroot.xyzzy.test.harness.agent.spans.ErrorSpanImpl;
import org.glowroot.xyzzy.test.harness.agent.spans.IncomingSpanImpl;
import org.glowroot.xyzzy.test.harness.agent.spans.LocalSpanImpl;
import org.glowroot.xyzzy.test.harness.agent.spans.OutgoingSpanImpl;
import org.glowroot.xyzzy.test.harness.agent.spans.ParentSpanImpl;
import org.glowroot.xyzzy.test.harness.agent.spans.QuerySpanImpl;

public class ThreadContextImpl implements ThreadContextPlus {

    private final ThreadContextThreadLocal.Holder threadContextHolder;

    private final IncomingSpanImpl incomingSpan;
    private final ParentSpanImpl parentSpan;

    private @Nullable ServletRequestInfo servletRequestInfo;

    private int currentNestingGroupId;
    private int currentSuppressionKeyId;

    public ThreadContextImpl(ThreadContextThreadLocal.Holder threadContextHolder,
            IncomingSpanImpl incomingSpan, ParentSpanImpl parentSpan,
            @Nullable ServletRequestInfo servletRequestInfo, int rootNestingGroupId,
            int rootSuppressionKeyId) {
        this.threadContextHolder = threadContextHolder;
        this.incomingSpan = incomingSpan;
        this.parentSpan = parentSpan;
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
        return startLocalSpanInternal(messageSupplier, (TimerNameImpl) timerName);
    }

    @Override
    public QueryEntry startQueryEntry(String queryType, String queryText,
            QueryMessageSupplier queryMessageSupplier, TimerName timerName) {
        // TODO pass along queryType
        return startQuerySpanInternal(queryText, queryMessageSupplier, (TimerNameImpl) timerName);
    }

    @Override
    public QueryEntry startQueryEntry(String queryType, String queryText, long queryExecutionCount,
            QueryMessageSupplier queryMessageSupplier, TimerName timerName) {
        // TODO pass along queryType and queryExecutionCount
        return startQuerySpanInternal(queryText, queryMessageSupplier, (TimerNameImpl) timerName);
    }

    @Override
    public AsyncQueryEntry startAsyncQueryEntry(String queryType, String queryText,
            QueryMessageSupplier queryMessageSupplier, TimerName timerName) {
        // TODO pass along queryType
        return startAsyncQuerySpanInternal(queryText, queryMessageSupplier,
                (TimerNameImpl) timerName);
    }

    @Override
    public TraceEntry startServiceCallEntry(String type, String text,
            MessageSupplier messageSupplier, TimerName timerName) {
        // TODO pass along type
        // TODO revisit the point of text
        return startOutgoingSpanInternal(messageSupplier, (TimerNameImpl) timerName);
    }

    @Override
    public AsyncTraceEntry startAsyncServiceCallEntry(String type, String text,
            MessageSupplier messageSupplier, TimerName timerName) {
        // TODO pass along type
        // TODO revisit the point of text
        return startAsyncOutgoingSpanInternal(messageSupplier, (TimerNameImpl) timerName);
    }

    @Override
    public Timer startTimer(TimerName timerName) {
        return NopTransactionService.TIMER;
    }

    @Override
    public AuxThreadContext createAuxThreadContext() {
        return new AuxThreadContextImpl(incomingSpan, parentSpan, servletRequestInfo);
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
    public void addErrorEntry(Throwable t) {
        parentSpan.addChildSpan(new ErrorSpanImpl(null, t));
    }

    @Override
    public void addErrorEntry(String message) {
        parentSpan.addChildSpan(new ErrorSpanImpl(message, null));
    }

    @Override
    public void addErrorEntry(String message, Throwable t) {
        parentSpan.addChildSpan(new ErrorSpanImpl(message, null));

    }

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

    private LocalSpanImpl startLocalSpanInternal(MessageSupplier messageSupplier,
            TimerNameImpl timerName) {
        long startNanoTime = System.nanoTime();
        TimerImpl timer = new TimerImpl(timerName, startNanoTime);
        LocalSpanImpl localSpan = new LocalSpanImpl(System.nanoTime(), messageSupplier, timer);
        parentSpan.addChildSpan(localSpan);
        return localSpan;
    }

    private OutgoingSpanImpl startOutgoingSpanInternal(MessageSupplier messageSupplier,
            TimerNameImpl timerName) {
        long startNanoTime = System.nanoTime();
        TimerImpl timer = new TimerImpl(timerName, startNanoTime);
        OutgoingSpanImpl outgoingSpan =
                new OutgoingSpanImpl(System.nanoTime(), messageSupplier, timer);
        parentSpan.addChildSpan(outgoingSpan);
        return outgoingSpan;
    }

    private AsyncOutgoingSpanImpl startAsyncOutgoingSpanInternal(MessageSupplier messageSupplier,
            TimerNameImpl timerName) {
        long startNanoTime = System.nanoTime();
        TimerImpl syncTimer = new TimerImpl(timerName, startNanoTime);
        TimerImpl asyncTimer = new TimerImpl(timerName, startNanoTime);
        incomingSpan.addAsyncTimer(asyncTimer);
        AsyncOutgoingSpanImpl asyncOutgoingSpan =
                new AsyncOutgoingSpanImpl(startNanoTime, messageSupplier, syncTimer, asyncTimer);
        parentSpan.addChildSpan(asyncOutgoingSpan);
        return asyncOutgoingSpan;
    }

    private QuerySpanImpl startQuerySpanInternal(String queryText,
            QueryMessageSupplier queryMessageSupplier, TimerNameImpl timerName) {
        long startNanoTime = System.nanoTime();
        TimerImpl timer = new TimerImpl(timerName, startNanoTime);
        QuerySpanImpl querySpan =
                new QuerySpanImpl(startNanoTime, queryText, queryMessageSupplier, timer);
        parentSpan.addChildSpan(querySpan);
        return querySpan;
    }

    private AsyncQuerySpanImpl startAsyncQuerySpanInternal(String text,
            QueryMessageSupplier queryMessageSupplier, TimerNameImpl timerName) {
        long startNanoTime = System.nanoTime();
        TimerImpl syncTimer = new TimerImpl(timerName, startNanoTime);
        TimerImpl asyncTimer = new TimerImpl(timerName, startNanoTime);
        incomingSpan.addAsyncTimer(asyncTimer);
        AsyncQuerySpanImpl asyncQuerySpan = new AsyncQuerySpanImpl(System.nanoTime(), text,
                queryMessageSupplier, syncTimer, asyncTimer);
        parentSpan.addChildSpan(asyncQuerySpan);
        return asyncQuerySpan;
    }
}
