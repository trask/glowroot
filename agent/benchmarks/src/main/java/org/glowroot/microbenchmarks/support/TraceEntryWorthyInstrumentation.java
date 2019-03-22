/*
 * Copyright 2014-2019 the original author or authors.
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
package org.glowroot.microbenchmarks.support;

import org.glowroot.xyzzy.instrumentation.api.Agent;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.ThreadContext;
import org.glowroot.xyzzy.instrumentation.api.TimerName;
import org.glowroot.xyzzy.instrumentation.api.TraceEntry;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindThrowable;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindTraveler;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnAfter;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnBefore;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnThrow;
import org.glowroot.xyzzy.instrumentation.api.weaving.Pointcut;

public class TraceEntryWorthyInstrumentation {

    @Pointcut(className = "org.glowroot.microbenchmarks.core.support.TraceEntryWorthy",
            methodName = "doSomethingTraceEntryWorthy", methodParameterTypes = {},
            timerName = "trace entry worthy")
    public static class TraceEntryWorthyAdvice {

        private static final TimerName timerName = Agent.getTimerName(TraceEntryWorthyAdvice.class);

        @OnBefore
        public static TraceEntry onBefore(ThreadContext context) {
            return context.startTraceEntry(MessageSupplier.create("trace entry worthy"), timerName);
        }

        @OnThrow
        public static void onThrow(@BindThrowable Throwable t,
                @BindTraveler TraceEntry traceEntry) {
            traceEntry.endWithError(t);
        }

        @OnAfter
        public static void onReturn(@BindTraveler TraceEntry traceEntry) {
            traceEntry.end();
        }
    }
}
