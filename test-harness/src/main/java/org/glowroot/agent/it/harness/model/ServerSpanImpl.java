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
package org.glowroot.agent.it.harness.model;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.assertj.core.util.Lists;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.glowroot.xyzzy.engine.bytecode.api.ThreadContextThreadLocal;
import org.glowroot.xyzzy.engine.impl.NopTransactionService;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.Timer;
import org.glowroot.xyzzy.instrumentation.api.TraceEntry;

public class ServerSpanImpl extends ParentSpanImpl implements TraceEntry {

    private final MessageSupplier messageSupplier;
    private final ThreadContextThreadLocal.Holder threadContextHolder;

    private volatile boolean async;
    private volatile String transactionType;
    private volatile String transactionName;
    private volatile String user;

    private volatile TimerImpl mainThreadRootTimer;
    private volatile @Nullable TimerImpl auxThreadRootTimer;

    private final List<TimerImpl> asyncTimers = Lists.newArrayList();

    public ServerSpanImpl(MessageSupplier messageSupplier,
            ThreadContextThreadLocal.Holder threadContextHolder) {
        this.messageSupplier = messageSupplier;
        this.threadContextHolder = threadContextHolder;
    }

    @Override
    public void end() {}

    @Override
    public void endWithLocationStackTrace(long threshold, TimeUnit unit) {
        setLocationStackTraceMillis(unit.toMillis(threshold));
        threadContextHolder.set(null);
    }

    @Override
    public void endWithError(Throwable t) {
        setError(ImmutableError.builder()
                .exception(t)
                .build());
        threadContextHolder.set(null);
    }

    @Override
    public void endWithError(String message) {
        setError(ImmutableError.builder()
                .message(message)
                .build());
        threadContextHolder.set(null);
    }

    @Override
    public void endWithError(String message, Throwable t) {
        setError(ImmutableError.builder()
                .message(message)
                .exception(t)
                .build());
        threadContextHolder.set(null);
    }

    @Override
    public void endWithInfo(Throwable t) {
        setError(ImmutableError.builder()
                .exception(t)
                .build());
        threadContextHolder.set(null);
    }

    @Override
    public Timer extend() {
        return NopTransactionService.TIMER;
    }

    @Override
    public Object getMessageSupplier() {
        return messageSupplier;
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

    public void setMainThreadRootTimer(TimerImpl mainThreadRootTimer) {
        this.mainThreadRootTimer = mainThreadRootTimer;
    }

    public void setAuxThreadRootTimer(TimerImpl auxThreadRootTimer) {
        this.auxThreadRootTimer = auxThreadRootTimer;
    }

    @Override
    public ImmutableServerSpan toImmutable() {
        ImmutableServerSpan.Builder builder = ImmutableServerSpan.builder()
                .async(async)
                .transactionType(transactionType)
                .transactionName(transactionName)
                .user(user)
                .mainThreadRootTimer(mainThreadRootTimer.toImmutable())
                .auxThreadRootTimer(auxThreadRootTimer.toImmutable());
        for (TimerImpl asyncTimer : asyncTimers) {
            builder.addAsyncTimers(asyncTimer.toImmutable());
        }
        builder.message(getMessage())
                .details(getDetails())
                .error(getError())
                .locationStackTraceMillis(getLocationStackTraceMillis());
        for (SpanImpl childSpan : getChildSpans()) {
            builder.addChildSpans(childSpan.toImmutable());
        }
        return builder.build();
    }
}
