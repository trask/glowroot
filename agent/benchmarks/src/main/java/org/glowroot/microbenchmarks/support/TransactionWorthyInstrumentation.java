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
import org.glowroot.xyzzy.instrumentation.api.Getter;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.OptionalThreadContext;
import org.glowroot.xyzzy.instrumentation.api.TimerName;
import org.glowroot.xyzzy.instrumentation.api.OptionalThreadContext.AlreadyInTransactionBehavior;
import org.glowroot.xyzzy.instrumentation.api.Span;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindThrowable;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindTraveler;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnBefore;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnReturn;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnThrow;
import org.glowroot.xyzzy.instrumentation.api.weaving.Pointcut;

public class TransactionWorthyInstrumentation {

    @Pointcut(className = "org.glowroot.microbenchmarks.core.support.TransactionWorthy",
            methodName = "doSomethingTransactionWorthy", methodParameterTypes = {})
    public static class TransactionWorthyAdvice {

        private static final TimerName timerName = Agent.getTimerName("transaction worthy");

        @OnBefore
        public static Span onBefore(OptionalThreadContext context) {
            return context.startIncomingSpan("Microbenchmark", "transaction worthy",
                    new GetterImpl(), new Object(), MessageSupplier.create("transaction worthy"),
                    timerName, AlreadyInTransactionBehavior.CAPTURE_LOCAL_SPAN);
        }

        @OnReturn
        public static void onReturn(@BindTraveler Span traceEntry) {
            traceEntry.end();
        }

        @OnThrow
        public static void onThrow(@BindThrowable Throwable t,
                @BindTraveler Span traceEntry) {
            traceEntry.endWithError(t);
        }
    }

    private static class GetterImpl implements Getter<Object> {

        @Override
        public String get(Object carrier, String key) {
            return null;
        }
    }
}
