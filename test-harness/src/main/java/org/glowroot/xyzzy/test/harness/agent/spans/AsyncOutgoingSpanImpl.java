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

import java.util.concurrent.TimeUnit;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.glowroot.xyzzy.engine.impl.NopTransactionService;
import org.glowroot.xyzzy.instrumentation.api.AsyncTraceEntry;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.ThreadContext;
import org.glowroot.xyzzy.instrumentation.api.Timer;
import org.glowroot.xyzzy.instrumentation.api.internal.ReadableMessage;
import org.glowroot.xyzzy.test.harness.ImmutableError;
import org.glowroot.xyzzy.test.harness.ImmutableOutgoingSpan;
import org.glowroot.xyzzy.test.harness.Span;
import org.glowroot.xyzzy.test.harness.agent.TimerImpl;

public class AsyncOutgoingSpanImpl implements AsyncTraceEntry, SpanImpl {

    private final long startTimeNanos;
    private final MessageSupplier messageSupplier;
    private final TimerImpl syncTimer;
    private final TimerImpl asyncTimer;

    private volatile long totalNanos;
    private volatile @Nullable Span.Error error;
    private volatile @Nullable Long locationStackTraceMillis;

    public AsyncOutgoingSpanImpl(long startTimeNanos, MessageSupplier messageSupplier,
            TimerImpl syncTimer, TimerImpl asyncTimer) {
        this.startTimeNanos = startTimeNanos;
        this.messageSupplier = messageSupplier;
        this.syncTimer = syncTimer;
        this.asyncTimer = asyncTimer;
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
    public @Nullable Object getMessageSupplier() {
        return null;
    }

    @Override
    public void stopSyncTimer() {
        syncTimer.stop(System.nanoTime());
    }

    @Override
    public Timer extendSyncTimer(ThreadContext currThreadContext) {
        // TODO implement
        return NopTransactionService.TIMER;
    }

    @Override
    public ImmutableOutgoingSpan toImmutable() {
        ReadableMessage message = (ReadableMessage) messageSupplier.get();
        return ImmutableOutgoingSpan.builder()
                .totalNanos(totalNanos)
                .message(message.getText())
                .prefix("") // TODO revisit
                .suffix("") // TODO revisit
                .details(message.getDetail())
                .error(error)
                .locationStackTraceMillis(locationStackTraceMillis)
                .build();
    }

    private void endInternal() {
        long endNanoTime = System.nanoTime();
        totalNanos = endNanoTime - startTimeNanos;
        asyncTimer.stop(endNanoTime);
    }
}
