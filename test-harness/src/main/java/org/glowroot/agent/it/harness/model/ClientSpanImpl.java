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

import java.util.concurrent.TimeUnit;

import org.glowroot.xyzzy.engine.impl.NopTransactionService;
import org.glowroot.xyzzy.instrumentation.api.AsyncTraceEntry;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.ThreadContext;
import org.glowroot.xyzzy.instrumentation.api.Timer;

public class ClientSpanImpl extends SpanImpl implements AsyncTraceEntry {

    private final String text;
    private final MessageSupplier messageSupplier;

    public ClientSpanImpl(String text, MessageSupplier messageSupplier) {
        this.text = text;
        this.messageSupplier = messageSupplier;
    }

    @Override
    public void end() {}

    @Override
    public void endWithLocationStackTrace(long threshold, TimeUnit unit) {
        setLocationStackTraceMillis(unit.toMillis(threshold));
    }

    @Override
    public void endWithError(Throwable t) {
        setError(ImmutableError.builder()
                .exception(t)
                .build());
    }

    @Override
    public void endWithError(String message) {
        setError(ImmutableError.builder()
                .message(message)
                .build());
    }

    @Override
    public void endWithError(String message, Throwable t) {
        setError(ImmutableError.builder()
                .message(message)
                .exception(t)
                .build());
    }

    @Override
    public void endWithInfo(Throwable t) {
        setError(ImmutableError.builder()
                .exception(t)
                .build());
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
    public void stopSyncTimer() {}

    @Override
    public Timer extendSyncTimer(ThreadContext currThreadContext) {
        return NopTransactionService.TIMER;
    }

    @Override
    public ImmutableClientSpan toImmutable() {
        return ImmutableClientSpan.builder()
                .message(getMessage())
                .details(getDetails())
                .error(getError())
                .locationStackTraceMillis(getLocationStackTraceMillis())
                .build();
    }
}
