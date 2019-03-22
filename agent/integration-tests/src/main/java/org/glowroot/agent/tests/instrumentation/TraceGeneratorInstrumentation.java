/*
 * Copyright 2011-2019 the original author or authors.
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

import java.util.Map;

import org.glowroot.xyzzy.instrumentation.api.Agent;
import org.glowroot.xyzzy.instrumentation.api.Getter;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.OptionalThreadContext;
import org.glowroot.xyzzy.instrumentation.api.TimerName;
import org.glowroot.xyzzy.instrumentation.api.OptionalThreadContext.AlreadyInTransactionBehavior;
import org.glowroot.xyzzy.instrumentation.api.Span;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindClassMeta;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindReceiver;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindTraveler;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnAfter;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnBefore;
import org.glowroot.xyzzy.instrumentation.api.weaving.Pointcut;

public class TraceGeneratorInstrumentation {

    @Pointcut(className = "org.glowroot.agent.tests.app.TraceGenerator", methodName = "call",
            methodParameterTypes = {"boolean"})
    public static class LevelOneAdvice {

        private static final TimerName timerName = Agent.getTimerName("trace generator");

        @OnBefore
        public static Span onBefore(OptionalThreadContext context,
                @BindReceiver Object traceGenerator,
                @BindClassMeta TraceGeneratorInvoker traceGeneratorInvoker) {
            String transactionType = traceGeneratorInvoker.transactionType(traceGenerator);
            String transactionName = traceGeneratorInvoker.transactionName(traceGenerator);
            String headline = traceGeneratorInvoker.headline(traceGenerator);
            Map<String, String> attributes = traceGeneratorInvoker.attributes(traceGenerator);
            String error = traceGeneratorInvoker.error(traceGenerator);

            Span traceEntry = context.startIncomingSpan(transactionType, transactionName,
                    new GetterImpl(), new Object(), MessageSupplier.create(headline), timerName,
                    AlreadyInTransactionBehavior.CAPTURE_LOCAL_SPAN);
            if (attributes != null) {
                for (Map.Entry<String, String> entry : attributes.entrySet()) {
                    context.addTransactionAttribute(entry.getKey(), entry.getValue());
                }
            }
            if (error != null) {
                context.setTransactionError(error);
            }
            return traceEntry;
        }

        @OnAfter
        public static void onAfter(@BindTraveler Span traceEntry) {
            traceEntry.end();
        }
    }

    private static class GetterImpl implements Getter<Object> {

        @Override
        public String get(Object carrier, String key) {
            return null;
        }
    }
}
