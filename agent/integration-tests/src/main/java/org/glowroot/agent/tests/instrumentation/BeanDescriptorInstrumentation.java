/*
 * Copyright 2016-2019 the original author or authors.
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
import org.glowroot.xyzzy.instrumentation.api.ClassInfo;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.MethodInfo;
import org.glowroot.xyzzy.instrumentation.api.OptionalThreadContext;
import org.glowroot.xyzzy.instrumentation.api.TimerName;
import org.glowroot.xyzzy.instrumentation.api.TraceEntry;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindClassMeta;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindMethodMeta;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindTraveler;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnAfter;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnBefore;
import org.glowroot.xyzzy.instrumentation.api.weaving.Pointcut;

// this is for testing weaving of bootstrap classes
public class BeanDescriptorInstrumentation {

    @Pointcut(className = "java.beans.BeanDescriptor", methodName = "getBeanClass",
            methodParameterTypes = {}, timerName = "get bean class")
    public static class GetBeanClassAdvice {

        private static final TimerName timerName = Agent.getTimerName(GetBeanClassAdvice.class);

        @OnBefore
        public static TraceEntry onBefore(OptionalThreadContext context,
                @BindClassMeta TestClassMeta meta) {
            return context.startTraceEntry(MessageSupplier.create(meta.classInfo.getName()),
                    timerName);
        }
        @OnAfter
        public static void onAfter(@BindTraveler TraceEntry traceEntry) {
            traceEntry.end();
        }
    }

    @Pointcut(className = "java.beans.BeanDescriptor", methodName = "getCustomizerClass",
            methodParameterTypes = {}, timerName = "get customizer class")
    public static class GetCustomizerClassAdvice {

        private static final TimerName timerName =
                Agent.getTimerName(GetCustomizerClassAdvice.class);

        @OnBefore
        public static TraceEntry onBefore(OptionalThreadContext context,
                @BindMethodMeta TestMethodMeta meta) {
            return context.startTraceEntry(MessageSupplier.create(meta.methodInfo.getName()),
                    timerName);
        }
        @OnAfter
        public static void onAfter(@BindTraveler TraceEntry traceEntry) {
            traceEntry.end();
        }
    }

    public static class TestClassMeta {

        private final ClassInfo classInfo;

        public TestClassMeta(ClassInfo classInfo) {
            this.classInfo = classInfo;
        }
    }

    public static class TestMethodMeta {

        private final MethodInfo methodInfo;

        public TestMethodMeta(MethodInfo methodInfo) {
            this.methodInfo = methodInfo;
        }
    }
}
