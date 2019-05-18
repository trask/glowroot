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

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import org.glowroot.xyzzy.instrumentation.api.Agent;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.ThreadContext;
import org.glowroot.xyzzy.instrumentation.api.TimerName;
import org.glowroot.xyzzy.instrumentation.api.Span;
import org.glowroot.xyzzy.instrumentation.api.config.BooleanProperty;
import org.glowroot.xyzzy.instrumentation.api.config.ConfigService;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindTraveler;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnAfter;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnBefore;
import org.glowroot.xyzzy.instrumentation.api.weaving.Pointcut;

public class PauseInstrumentation {

    private static final ConfigService configService =
            Agent.getConfigService("glowroot-integration-tests");

    private static final BooleanProperty captureTraceEntryStackTraces =
            configService.getBooleanProperty("captureTraceEntryStackTraces");

    @Pointcut(className = "org.glowroot.agent.tests.app.Pause", methodName = "pause*",
            methodParameterTypes = {})
    public static class PauseAdvice {

        private static final TimerName timerName = Agent.getTimerName("pause");

        @OnBefore
        public static Span onBefore(ThreadContext context) {
            return context.startLocalSpan(MessageSupplier.create("Pause.pauseOneMillisecond()"),
                    timerName);
        }

        @OnAfter
        public static void onAfter(@BindTraveler Span traceEntry) {
            if (captureTraceEntryStackTraces.value()) {
                traceEntry.endWithLocationStackTrace(0, NANOSECONDS);
            } else {
                traceEntry.end();
            }
        }
    }
}
