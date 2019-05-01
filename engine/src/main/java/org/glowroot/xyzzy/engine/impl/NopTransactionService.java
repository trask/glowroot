/*
 * Copyright 2015-2019 the original author or authors.
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
package org.glowroot.xyzzy.engine.impl;

import java.util.concurrent.TimeUnit;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.glowroot.xyzzy.instrumentation.api.AsyncQueryEntry;
import org.glowroot.xyzzy.instrumentation.api.AsyncTraceEntry;
import org.glowroot.xyzzy.instrumentation.api.AuxThreadContext;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.QueryEntry;
import org.glowroot.xyzzy.instrumentation.api.ThreadContext;
import org.glowroot.xyzzy.instrumentation.api.Timer;
import org.glowroot.xyzzy.instrumentation.api.TraceEntry;

public class NopTransactionService {

    public static final Timer TIMER = NopTimer.INSTANCE;
    public static final TraceEntry TRACE_ENTRY = NopAsyncQueryEntry.INSTANCE;
    public static final QueryEntry QUERY_ENTRY = NopAsyncQueryEntry.INSTANCE;
    public static final AsyncTraceEntry ASYNC_TRACE_ENTRY = NopAsyncQueryEntry.INSTANCE;
    public static final AsyncQueryEntry ASYNC_QUERY_ENTRY = NopAsyncQueryEntry.INSTANCE;
    public static final AuxThreadContext AUX_THREAD_CONTEXT = NopAuxThreadContext.INSTANCE;

    private NopTransactionService() {}

    private static class NopAsyncQueryEntry implements AsyncQueryEntry {

        private static final NopAsyncQueryEntry INSTANCE = new NopAsyncQueryEntry();

        private NopAsyncQueryEntry() {}

        @Override
        public void end() {}

        @Override
        public void endWithLocationStackTrace(long threshold, TimeUnit unit) {}

        @Override
        public void endWithError(Throwable t) {}

        @Override
        public void endWithError(@Nullable String message) {}

        @Override
        public void endWithError(@Nullable String message, Throwable t) {}

        @Override
        public void endWithInfo(Throwable t) {}

        @Override
        public @Nullable MessageSupplier getMessageSupplier() {
            return null;
        }

        @Override
        public Timer extend() {
            return NopTimer.INSTANCE;
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
            return NopTimer.INSTANCE;
        }
    }

    private static class NopAuxThreadContext implements AuxThreadContext {

        private static final NopAuxThreadContext INSTANCE = new NopAuxThreadContext();

        private NopAuxThreadContext() {}

        @Override
        public TraceEntry start() {
            return NopTransactionService.TRACE_ENTRY;
        }

        @Override
        public TraceEntry startAndMarkAsyncTransactionComplete() {
            return NopTransactionService.TRACE_ENTRY;
        }
    }

    private static class NopTimer implements Timer {

        private static final NopTimer INSTANCE = new NopTimer();

        private NopTimer() {}

        @Override
        public void stop() {}
    }
}
