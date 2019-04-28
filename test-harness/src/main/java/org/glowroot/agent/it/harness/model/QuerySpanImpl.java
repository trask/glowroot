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

import org.checkerframework.checker.nullness.qual.Nullable;

import org.glowroot.xyzzy.engine.impl.NopTransactionService;
import org.glowroot.xyzzy.instrumentation.api.AsyncQueryEntry;
import org.glowroot.xyzzy.instrumentation.api.QueryMessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.ThreadContext;
import org.glowroot.xyzzy.instrumentation.api.Timer;
import org.glowroot.xyzzy.instrumentation.api.internal.ReadableQueryMessage;

public class QuerySpanImpl extends SpanImpl implements AsyncQueryEntry {

    private final String queryText;
    private final QueryMessageSupplier queryMessageSupplier;

    public QuerySpanImpl(String queryText, QueryMessageSupplier queryMessageSupplier) {
        this.queryText = queryText;
        this.queryMessageSupplier = queryMessageSupplier;
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
    public @Nullable Object getMessageSupplier() {
        return null;
    }

    @Override
    public void rowNavigationAttempted() {}

    @Override
    public void incrementCurrRow() {}

    @Override
    public void setCurrRow(long row) {}

    @Override
    public void stopSyncTimer() {}

    @Override
    public Timer extendSyncTimer(ThreadContext currThreadContext) {
        return NopTransactionService.TIMER;
    }

    @Override
    public ImmutableClientSpan toImmutable() {
        ReadableQueryMessage message = (ReadableQueryMessage) queryMessageSupplier.get();
        return ImmutableClientSpan.builder()
                .message(queryText)
                .prefix(message.getPrefix())
                .suffix(message.getSuffix())
                .details(getDetails())
                .error(getError())
                .locationStackTraceMillis(getLocationStackTraceMillis())
                .build();
    }
}
