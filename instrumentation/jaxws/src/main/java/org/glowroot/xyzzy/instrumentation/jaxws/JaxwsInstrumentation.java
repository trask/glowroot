/*
 * Copyright 2017-2019 the original author or authors.
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
package org.glowroot.xyzzy.instrumentation.jaxws;

import org.glowroot.xyzzy.instrumentation.api.Agent;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.ThreadContext;
import org.glowroot.xyzzy.instrumentation.api.TimerName;
import org.glowroot.xyzzy.instrumentation.api.TraceEntry;
import org.glowroot.xyzzy.instrumentation.api.ThreadContext.Priority;
import org.glowroot.xyzzy.instrumentation.api.ThreadContext.ServletRequestInfo;
import org.glowroot.xyzzy.instrumentation.api.checker.Nullable;
import org.glowroot.xyzzy.instrumentation.api.config.BooleanProperty;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindMethodMeta;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindThrowable;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindTraveler;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnBefore;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnReturn;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnThrow;
import org.glowroot.xyzzy.instrumentation.api.weaving.Pointcut;

public class JaxwsInstrumentation {

    private static final BooleanProperty useAltTransactionNaming =
            Agent.getConfigService("jaxws").getBooleanProperty("useAltTransactionNaming");

    @Pointcut(classAnnotation = "javax.jws.WebService", methodAnnotation = "javax.jws.WebMethod",
            methodParameterTypes = {".."}, timerName = "jaxws service")
    public static class ResourceAdvice {

        private static final TimerName timerName = Agent.getTimerName(ResourceAdvice.class);

        @OnBefore
        public static TraceEntry onBefore(ThreadContext context,
                @BindMethodMeta ServiceMethodMeta serviceMethodMeta) {

            if (useAltTransactionNaming.value()) {
                context.setTransactionName(serviceMethodMeta.getAltTransactionName(),
                        Priority.CORE_INSTRUMENTATION);
            } else {
                String transactionName = getTransactionName(context.getServletRequestInfo(),
                        serviceMethodMeta.getMethodName());
                context.setTransactionName(transactionName, Priority.CORE_INSTRUMENTATION);
            }
            return context.startTraceEntry(MessageSupplier.create("jaxws service: {}.{}()",
                    serviceMethodMeta.getServiceClassName(), serviceMethodMeta.getMethodName()),
                    timerName);
        }

        @OnReturn
        public static void onReturn(@BindTraveler TraceEntry traceEntry) {
            traceEntry.end();
        }

        @OnThrow
        public static void onThrow(@BindThrowable Throwable t,
                @BindTraveler TraceEntry traceEntry) {
            traceEntry.endWithError(t);
        }

        private static String getTransactionName(@Nullable ServletRequestInfo servletRequestInfo,
                String methodName) {
            if (servletRequestInfo == null) {
                return '#' + methodName;
            }
            String method = servletRequestInfo.getMethod();
            String uri = servletRequestInfo.getUri();
            if (method.isEmpty()) {
                return uri + '#' + methodName;
            } else {
                return method + ' ' + uri + '#' + methodName;
            }
        }
    }
}
