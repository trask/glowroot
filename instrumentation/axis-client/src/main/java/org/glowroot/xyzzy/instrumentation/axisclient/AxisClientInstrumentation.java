/**
 * Copyright 2018-2019 the original author or authors.
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
package org.glowroot.xyzzy.instrumentation.axisclient;

import org.apache.axis.client.Call;

import org.glowroot.xyzzy.instrumentation.api.Agent;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.ThreadContext;
import org.glowroot.xyzzy.instrumentation.api.TimerName;
import org.glowroot.xyzzy.instrumentation.api.TraceEntry;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindReceiver;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindTraveler;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnAfter;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnBefore;
import org.glowroot.xyzzy.instrumentation.api.weaving.Pointcut;

public class AxisClientInstrumentation {

    @Pointcut(className = "org.apache.axis.client.Call", methodName = "invoke",
            methodParameterTypes = {}, nestingGroup = "http-client", timerName = "axis service")
    public static class ResourceAdvice {

        private static final TimerName timerName = Agent.getTimerName(ResourceAdvice.class);

        @OnBefore
        public static TraceEntry onBefore(ThreadContext context, @BindReceiver Call call) {
            String url = call.getTargetEndpointAddress();
            return context.startServiceCallEntry("HTTP", "POST " + url,
                    MessageSupplier.create("http client request: POST {}", url), timerName);
        }

        @OnAfter
        public static void onAfter(@BindTraveler TraceEntry traceEntry) {
            traceEntry.end();
        }
    }
}
