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
import org.glowroot.xyzzy.instrumentation.api.Span;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindParameter;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindTraveler;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnAfter;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnBefore;
import org.glowroot.xyzzy.instrumentation.api.weaving.Pointcut;

public class LogErrorInstrumentation {

    @Pointcut(className = "org.glowroot.agent.tests.app.LogError", methodName = "log",
            methodParameterTypes = {"java.lang.String"})
    public static class LogErrorAdvice {

        private static final TimerName timerName = Agent.getTimerName("log error");

        @OnBefore
        public static Span onBefore(ThreadContext context, @BindParameter(0) String message) {
            return context.startLocalSpan(MessageSupplier.create("ERROR -- {}", message),
                    timerName);
        }

        @OnAfter
        public static void onAfter(@BindTraveler Span traceEntry) {
            traceEntry.endWithError(new Exception("test error message"));
        }
    }

    @Pointcut(className = "org.glowroot.agent.tests.app.LogError",
            methodName = "addNestedErrorEntry", methodParameterTypes = {})
    public static class AddErrorEntryAdvice {

        private static final TimerName timerName = Agent.getTimerName("add nested error entry");

        @OnBefore
        public static Span onBefore(ThreadContext context) {
            Span traceEntry = context.startLocalSpan(
                    MessageSupplier.create("outer entry to test nesting level"), timerName);
            Span nestedSpan = context.startLocalSpan(
                    MessageSupplier.create("a nested span"), timerName);
            nestedSpan.endWithError(new Exception("test add nested error entry message"));
            return traceEntry;
        }

        @OnAfter
        public static void onAfter(@BindTraveler Span traceEntry) {
            traceEntry.end();
        }
    }
}
