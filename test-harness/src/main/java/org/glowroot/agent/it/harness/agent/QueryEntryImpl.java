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

import org.glowroot.agent.it.harness.model.ImmutableEntry;
import org.glowroot.agent.it.harness.model.ImmutableError;
import org.glowroot.agent.it.harness.model.ImmutableQueryEntryMessage;
import org.glowroot.agent.it.harness.model.ImmutableTrace;
import org.glowroot.xyzzy.engine.impl.NopTransactionService;
import org.glowroot.xyzzy.instrumentation.api.AsyncQueryEntry;
import org.glowroot.xyzzy.instrumentation.api.QueryMessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.ThreadContext;
import org.glowroot.xyzzy.instrumentation.api.Timer;
import org.glowroot.xyzzy.instrumentation.api.internal.ReadableQueryMessage;

class QueryEntryImpl implements AsyncQueryEntry {

    private final ImmutableTrace.Builder trace;
    private final String queryText;
    private final QueryMessageSupplier queryMessageSupplier;

    QueryEntryImpl(ImmutableTrace.Builder trace, String queryText,
            QueryMessageSupplier queryMessageSupplier) {
        this.queryText = queryText;
        this.queryMessageSupplier = queryMessageSupplier;
        this.trace = trace;
    }

    @Override
    public void end() {
        trace.addEntries(toEntry().build());
    }

    @Override
    public void endWithLocationStackTrace(long threshold, TimeUnit unit) {
        trace.addEntries(toEntry()
                .locationStackTraceMillis(unit.toMillis(threshold))
                .build());
    }

    @Override
    public void endWithError(Throwable t) {
        trace.addEntries(toEntry()
                .error(ImmutableError.builder()
                        .exception(t)
                        .build())
                .build());
    }

    @Override
    public void endWithError(String message) {
        trace.addEntries(toEntry()
                .error(ImmutableError.builder()
                        .message(message)
                        .build())
                .build());
    }

    @Override
    public void endWithError(String message, Throwable t) {
        trace.addEntries(toEntry()
                .error(ImmutableError.builder()
                        .message(message)
                        .exception(t)
                        .build())
                .build());
    }

    @Override
    public void endWithInfo(Throwable t) {
        trace.addEntries(toEntry()
                .error(ImmutableError.builder()
                        .exception(t)
                        .build())
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

    private ImmutableEntry.Builder toEntry() {
        ReadableQueryMessage queryMessage = (ReadableQueryMessage) queryMessageSupplier.get();
        return ImmutableEntry.builder()
                .queryEntryMessage(ImmutableQueryEntryMessage.builder()
                        .queryText(queryText)
                        .prefix(queryMessage.getPrefix())
                        .suffix(queryMessage.getSuffix())
                        .build());
    }
}
