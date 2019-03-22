/*
 * Copyright 2013-2019 the original author or authors.
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
package org.glowroot.agent.ui.sandbox;

import org.glowroot.xyzzy.instrumentation.api.Agent;
import org.glowroot.xyzzy.instrumentation.api.Getter;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.OptionalThreadContext;
import org.glowroot.xyzzy.instrumentation.api.OptionalThreadContext.AlreadyInTransactionBehavior;
import org.glowroot.xyzzy.instrumentation.api.Span;
import org.glowroot.xyzzy.instrumentation.api.ThreadContext;
import org.glowroot.xyzzy.instrumentation.api.Timer;
import org.glowroot.xyzzy.instrumentation.api.TimerName;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindTraveler;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnAfter;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnBefore;
import org.glowroot.xyzzy.instrumentation.api.weaving.Pointcut;

// this is used to generate a trace with <multiple root nodes> (and with multiple timers) just to
// test this unusual situation
public class ExternalJvmMainInstrumentation {

    @Pointcut(className = "org.glowroot.agent.it.harness.impl.JavaagentMain", methodName = "main",
            methodParameterTypes = {"java.lang.String[]"})
    public static class MainAdvice {

        private static final TimerName timerName = Agent.getTimerName("external jvm main");

        @OnBefore
        public static Span onBefore(OptionalThreadContext context) {
            return context.startIncomingSpan("Sandbox", "javaagent container main",
                    new GetterImpl(), new Object(), MessageSupplier.create(
                            "org.glowroot.agent.it.harness.impl.JavaagentMain.main()"),
                    timerName, AlreadyInTransactionBehavior.CAPTURE_LOCAL_SPAN);
        }

        @OnAfter
        public static void onAfter(@BindTraveler Span traceEntry) {
            traceEntry.end();
        }
    }

    @Pointcut(className = "org.glowroot.agent.it.harness.impl.JavaagentMain",
            methodName = "timerMarkerOne", methodParameterTypes = {})
    public static class TimerMarkerOneAdvice {

        private static final TimerName timerName = Agent.getTimerName("timer one");

        @OnBefore
        public static Timer onBefore(ThreadContext context) {
            return context.startTimer(timerName);
        }

        @OnAfter
        public static void onAfter(@BindTraveler Timer timer) {
            timer.stop();
        }
    }

    @Pointcut(className = "org.glowroot.agent.it.harness.impl.JavaagentMain",
            methodName = "timerMarkerTwo", methodParameterTypes = {})
    public static class TimerMarkerTwoAdvice {

        private static final TimerName timerName = Agent.getTimerName("timer two");

        @OnBefore
        public static Timer onBefore(ThreadContext context) {
            return context.startTimer(timerName);
        }

        @OnAfter
        public static void onAfter(@BindTraveler Timer timer) {
            timer.stop();
        }
    }

    private static class GetterImpl implements Getter<Object> {

        @Override
        public String get(Object carrier, String key) {
            return null;
        }
    }
}
