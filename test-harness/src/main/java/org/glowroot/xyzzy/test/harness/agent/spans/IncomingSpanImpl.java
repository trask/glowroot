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
package org.glowroot.xyzzy.test.harness.agent.spans;

import java.util.Queue;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Strings;
import com.google.common.collect.Queues;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.glowroot.xyzzy.engine.bytecode.api.ThreadContextThreadLocal;
import org.glowroot.xyzzy.engine.impl.NopTransactionService;
import org.glowroot.xyzzy.engine.impl.TimerNameImpl;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.Timer;
import org.glowroot.xyzzy.instrumentation.api.TraceEntry;
import org.glowroot.xyzzy.instrumentation.api.internal.ReadableMessage;
import org.glowroot.xyzzy.test.harness.ImmutableError;
import org.glowroot.xyzzy.test.harness.ImmutableIncomingSpan;
import org.glowroot.xyzzy.test.harness.Span;
import org.glowroot.xyzzy.test.harness.agent.Global;
import org.glowroot.xyzzy.test.harness.agent.TimerImpl;

public class IncomingSpanImpl implements TraceEntry, ParentSpanImpl {

    private final MessageSupplier messageSupplier;
    private final ThreadContextThreadLocal.Holder threadContextHolder;

    private final long startTimeNanos;

    private final TimerImpl mainThreadRootTimer;

    private final Queue<TimerImpl> auxThreadRootTimers = Queues.newConcurrentLinkedQueue();

    private final Queue<TimerImpl> asyncTimers = Queues.newConcurrentLinkedQueue();

    private final Queue<SpanImpl> childSpans = Queues.newConcurrentLinkedQueue();

    private volatile boolean async;
    private volatile String transactionType;
    private volatile String transactionName;
    private volatile @Nullable String user;

    private volatile long totalNanos;

    private volatile @Nullable Span.Error error;
    private volatile @Nullable Long locationStackTraceMillis;

    public IncomingSpanImpl(String transactionType, String transactionName,
            MessageSupplier messageSupplier, ThreadContextThreadLocal.Holder threadContextHolder,
            TimerNameImpl timerName, long startTimeNanos) {
        this.transactionType = transactionType;
        this.transactionName = transactionName;
        this.messageSupplier = messageSupplier;
        this.threadContextHolder = threadContextHolder;
        this.startTimeNanos = startTimeNanos;
        mainThreadRootTimer = new TimerImpl(timerName, startTimeNanos);
    }

    @Override
    public void end() {
        endInternal();
    }

    @Override
    public void endWithLocationStackTrace(long threshold, TimeUnit unit) {
        locationStackTraceMillis = unit.toMillis(threshold);
        endInternal();
    }

    @Override
    public void endWithError(Throwable t) {
        error = ImmutableError.builder()
                .exception(t)
                .build();
        endInternal();
    }

    @Override
    public void endWithError(String message) {
        error = ImmutableError.builder()
                .message(message)
                .build();
        endInternal();
    }

    @Override
    public void endWithError(String message, Throwable t) {
        error = ImmutableError.builder()
                .message(message)
                .exception(t)
                .build();
        endInternal();
    }

    @Override
    public void endWithInfo(Throwable t) {
        error = ImmutableError.builder()
                .exception(t)
                .build();
        endInternal();
    }

    @Override
    public Timer extend() {
        return NopTransactionService.TIMER;
    }

    @Override
    public Object getMessageSupplier() {
        return messageSupplier;
    }

    @Override
    public void addChildSpan(SpanImpl childSpan) {
        childSpans.add(childSpan);
    }

    @Override
    public ImmutableIncomingSpan toImmutable() {
        ImmutableIncomingSpan.Builder builder = ImmutableIncomingSpan.builder()
                .async(async)
                .totalNanos(totalNanos)
                .transactionType(transactionType)
                .transactionName(transactionName)
                .user(Strings.nullToEmpty(user))
                .mainThreadRootTimer(mainThreadRootTimer.toImmutable());
        // TODO merge auxThreadRootTimers
        // builder.auxThreadRootTimer(auxThreadRootTimer.toImmutable());
        // TODO merge asyncTimers
        for (TimerImpl asyncTimer : asyncTimers) {
            builder.addAsyncTimers(asyncTimer.toImmutable());
        }
        ReadableMessage message = (ReadableMessage) messageSupplier.get();
        builder.message(message.getText())
                .details(message.getDetail())
                .error(error)
                .locationStackTraceMillis(locationStackTraceMillis);
        for (SpanImpl childSpan : childSpans) {
            builder.addChildSpans(childSpan.toImmutable());
        }
        return builder.build();
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public void setTransactionName(String transactionName) {
        this.transactionName = transactionName;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void addAuxThreadRootTimer(TimerImpl auxThreadRootTimer) {
        auxThreadRootTimers.add(auxThreadRootTimer);
    }

    public void addAsyncTimer(TimerImpl asyncTimer) {
        asyncTimers.add(asyncTimer);
    }

    private void endInternal() {
        threadContextHolder.set(null);
        totalNanos = System.nanoTime() - startTimeNanos;
        Global.report(toImmutable());
    }
}
