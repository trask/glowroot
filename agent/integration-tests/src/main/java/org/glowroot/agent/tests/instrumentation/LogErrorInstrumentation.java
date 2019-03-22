/*
 * Copyright 2012-2019 the original author or authors.
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
package org.glowroot.agent.tests.instrumentation;

import org.glowroot.xyzzy.instrumentation.api.Agent;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.ThreadContext;
import org.glowroot.xyzzy.instrumentation.api.TimerName;
import org.glowroot.xyzzy.instrumentation.api.TraceEntry;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindParameter;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindTraveler;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnAfter;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnBefore;
import org.glowroot.xyzzy.instrumentation.api.weaving.Pointcut;

public class LogErrorInstrumentation {

    @Pointcut(className = "org.glowroot.agent.tests.app.LogError", methodName = "log",
            methodParameterTypes = {"java.lang.String"}, timerName = "log error")
    public static class LogErrorAdvice {

        private static final TimerName timerName = Agent.getTimerName(LogErrorAdvice.class);

        @OnBefore
        public static TraceEntry onBefore(ThreadContext context, @BindParameter String message) {
            return context.startTraceEntry(MessageSupplier.create("ERROR -- {}", message),
                    timerName);
        }

        @OnAfter
        public static void onAfter(@BindTraveler TraceEntry traceEntry) {
            traceEntry.endWithError("test error message");
        }
    }

    @Pointcut(className = "org.glowroot.agent.tests.app.LogError",
            methodName = "addNestedErrorEntry", methodParameterTypes = {},
            timerName = "add nested error entry")
    public static class AddErrorEntryAdvice {

        private static final TimerName timerName = Agent.getTimerName(AddErrorEntryAdvice.class);

        @OnBefore
        public static TraceEntry onBefore(ThreadContext context) {
            TraceEntry traceEntry = context.startTraceEntry(
                    MessageSupplier.create("outer entry to test nesting level"), timerName);
            context.addErrorEntry("test add nested error entry message");
            return traceEntry;
        }

        @OnAfter
        public static void onAfter(@BindTraveler TraceEntry traceEntry) {
            traceEntry.end();
        }
    }

    // this is just to generate an additional $glowroot$ method to test that consecutive
    // $glowroot$ methods in an entry stack trace are stripped out correctly
    @Pointcut(className = "org.glowroot.agent.tests.app.LogError", methodName = "log",
            methodParameterTypes = {"java.lang.String"}, timerName = "log error 2")
    public static class LogErrorAdvice2 {}
}
