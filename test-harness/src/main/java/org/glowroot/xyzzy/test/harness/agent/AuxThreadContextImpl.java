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
import org.glowroot.xyzzy.instrumentation.api.AuxThreadContext;
import org.glowroot.xyzzy.instrumentation.api.ThreadContext.ServletRequestInfo;
import org.glowroot.xyzzy.instrumentation.api.Timer;
import org.glowroot.xyzzy.instrumentation.api.TraceEntry;
import org.glowroot.xyzzy.test.harness.agent.spans.IncomingSpanImpl;
import org.glowroot.xyzzy.test.harness.agent.spans.ParentSpanImpl;

public class AuxThreadContextImpl implements AuxThreadContext {

    private final IncomingSpanImpl incomingSpan;
    private final ParentSpanImpl parentSpan;
    private final @Nullable ServletRequestInfo servletRequestInfo;

    public AuxThreadContextImpl(IncomingSpanImpl incomingSpan, ParentSpanImpl parentSpan,
            @Nullable ServletRequestInfo servletRequestInfo) {
        this.incomingSpan = incomingSpan;
        this.parentSpan = parentSpan;
        this.servletRequestInfo = servletRequestInfo;
    }

    @Override
    public TraceEntry start() {
        return start(false);
    }

    @Override
    public TraceEntry startAndMarkAsyncTransactionComplete() {
        return start(true);
    }

    private TraceEntry start(boolean completeAsyncTransaction) {
        ThreadContextThreadLocal.Holder threadContextHolder = Global.getThreadContextHolder();
        ThreadContextPlus threadContext = threadContextHolder.get();
        if (threadContext != null) {
            if (completeAsyncTransaction) {
                threadContext.setTransactionAsyncComplete();
            }
            return NopTransactionService.TRACE_ENTRY;
        }
        threadContext = new ThreadContextImpl(threadContextHolder, incomingSpan, parentSpan,
                servletRequestInfo, 0, 0);
        threadContextHolder.set(threadContext);
        if (completeAsyncTransaction) {
            threadContext.setTransactionAsyncComplete();
        }
        return new AuxRootEntryImpl(threadContextHolder);
    }

    private static class AuxRootEntryImpl implements TraceEntry {

        private final ThreadContextThreadLocal.Holder threadContextHolder;

        private AuxRootEntryImpl(ThreadContextThreadLocal.Holder threadContextHolder) {
            this.threadContextHolder = threadContextHolder;
        }

        @Override
        public void end() {
            endInternal();
        }

        @Override
        public void endWithLocationStackTrace(long threshold, TimeUnit unit) {
            endInternal();
        }

        @Override
        public void endWithError(Throwable t) {
            endInternal();
        }

        @Override
        public void endWithError(String message) {
            endInternal();
        }

        @Override
        public void endWithError(String message, Throwable t) {
            endInternal();
        }

        @Override
        public void endWithInfo(Throwable t) {
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

        private void endInternal() {
            ThreadContextImpl threadContext = (ThreadContextImpl) threadContextHolder.get();
            threadContext.endAuxThreadContext();
            threadContextHolder.set(null);
        }
    }
}
