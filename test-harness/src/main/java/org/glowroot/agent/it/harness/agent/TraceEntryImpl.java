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

import org.glowroot.agent.it.harness.model.ImmutableEntry;
import org.glowroot.agent.it.harness.model.ImmutableError;
import org.glowroot.agent.it.harness.model.ImmutableTrace;
import org.glowroot.agent.it.harness.model.Trace;
import org.glowroot.xyzzy.engine.impl.NopTransactionService;
import org.glowroot.xyzzy.instrumentation.api.AsyncTraceEntry;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.ThreadContext;
import org.glowroot.xyzzy.instrumentation.api.Timer;
import org.glowroot.xyzzy.instrumentation.api.internal.ReadableMessage;

public class TraceEntryImpl implements AsyncTraceEntry {

    private final ImmutableTrace.Builder trace;
    private final MessageSupplier messageSupplier;

    protected TraceEntryImpl(ImmutableTrace.Builder trace, MessageSupplier messageSupplier) {
        this.messageSupplier = messageSupplier;
        this.trace = trace;
    }

    @Override
    public void end() {
        end(toEntry().build());
    }

    @Override
    public void endWithLocationStackTrace(long threshold, TimeUnit unit) {
        end(toEntry()
                .locationStackTraceMillis(unit.toMillis(threshold))
                .build());
    }

    @Override
    public void endWithError(Throwable t) {
        end(toEntry()
                .error(ImmutableError.builder()
                        .exception(t)
                        .build())
                .build());
    }

    @Override
    public void endWithError(String message) {
        end(toEntry()
                .error(ImmutableError.builder()
                        .message(message)
                        .build())
                .build());
    }

    @Override
    public void endWithError(String message, Throwable t) {
        end(toEntry()
                .error(ImmutableError.builder()
                        .message(message)
                        .exception(t)
                        .build())
                .build());
    }

    @Override
    public void endWithInfo(Throwable t) {
        end(toEntry()
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
    public Object getMessageSupplier() {
        return messageSupplier;
    }

    @Override
    public void stopSyncTimer() {}

    @Override
    public Timer extendSyncTimer(ThreadContext currThreadContext) {
        return NopTransactionService.TIMER;
    }

    protected void postFinish() {}

    private void end(Trace.Entry entry) {
        trace.addEntries(entry);
        postFinish();
    }

    private ImmutableEntry.Builder toEntry() {
        return ImmutableEntry.builder()
                .message(((ReadableMessage) messageSupplier.get()).getText());
    }
}
