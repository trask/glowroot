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
package org.glowroot.agent.ui.sandbox;

import java.util.Map;
import java.util.Random;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.glowroot.xyzzy.instrumentation.api.Agent;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.QueryMessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.QuerySpan;
import org.glowroot.xyzzy.instrumentation.api.Span;
import org.glowroot.xyzzy.instrumentation.api.ThreadContext;
import org.glowroot.xyzzy.instrumentation.api.TimerName;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindClassMeta;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindReceiver;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindTraveler;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnAfter;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnBefore;
import org.glowroot.xyzzy.instrumentation.api.weaving.Pointcut;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class ExpensiveCallInstrumentation {

    private static final Random random = new Random();
    private static final Exception nestedCause =
            new IllegalArgumentException("A cause with a different stack trace");
    private static final Exception cause =
            new IllegalStateException("A cause with a different stack trace", nestedCause);

    @Pointcut(className = "org.glowroot.agent.ui.sandbox.ExpensiveCall", methodName = "execute0",
            methodParameterTypes = {})
    public static class ExpensiveCallAdvice0 {
        private static final TimerName timerName = Agent.getTimerName("expensive 0");
        @OnBefore
        public static QuerySpan onBefore(ThreadContext context) {
            // not delegating to onBeforeInternal(), this pointcut returns message supplier with
            // detail
            QueryMessageSupplier messageSupplier = getQueryMessageSupplierWithDetail();
            char randomChar = (char) ('a' + random.nextInt(26));
            String queryText;
            if (random.nextBoolean()) {
                queryText = "this is a short query " + randomChar;
            } else {
                queryText = "this is a long query " + randomChar
                        + " abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz"
                        + " abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz"
                        + " abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz"
                        + " abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz"
                        + " abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz";
            }
            return context.startQuerySpan("EQL", queryText, messageSupplier, timerName);
        }
        @OnAfter
        public static void onAfter(@BindTraveler QuerySpan query) {
            query.incrementCurrRow();
            query.incrementCurrRow();
            query.incrementCurrRow();
            if (random.nextDouble() < 0.05) {
                // Span.endWithLocationStackTrace() must be called directly from @On.. method
                // so it can strip back the stack trace to the method picked out by the @Pointcut
                query.endWithLocationStackTrace(0, NANOSECONDS);
            } else {
                onAfterInternal(query, 0);
            }
        }
    }

    @Pointcut(className = "org.glowroot.agent.ui.sandbox.ExpensiveCall", methodName = "execute1",
            methodParameterTypes = {})
    public static class ExpensiveCallAdvice1 {
        private static final TimerName timerName = Agent.getTimerName("expensive 1");
        @OnBefore
        public static Span onBefore(ThreadContext context, @BindReceiver Object expensiveCall,
                @BindClassMeta ExpensiveCallInvoker expensiveCallInvoker) {
            return onBeforeInternal(context, expensiveCall, expensiveCallInvoker, timerName);
        }
        @OnAfter
        public static void onAfter(@BindTraveler Span traceEntry) {
            if (traceEntry != null && random.nextDouble() < 0.05) {
                // Span.endWithLocationStackTrace() must be called directly from @On.. method
                // so it can strip back the stack trace to the method picked out by the @Pointcut
                traceEntry.endWithLocationStackTrace(0, NANOSECONDS);
            } else {
                onAfterInternal(traceEntry, 1);
            }
        }
    }

    @Pointcut(className = "org.glowroot.agent.ui.sandbox.ExpensiveCall", methodName = "execute2",
            methodParameterTypes = {})
    public static class ExpensiveCallAdvice2 {
        private static final TimerName timerName = Agent.getTimerName("expensive 2");
        @OnBefore
        public static Span onBefore(ThreadContext context, @BindReceiver Object expensiveCall,
                @BindClassMeta ExpensiveCallInvoker expensiveCallInvoker) {
            return onBeforeInternal(context, expensiveCall, expensiveCallInvoker, timerName);
        }
        @OnAfter
        public static void onAfter(@BindTraveler Span traceEntry) {
            if (traceEntry != null && random.nextDouble() < 0.05) {
                // Span.endWithLocationStackTrace() must be called directly from @On.. method
                // so it can strip back the stack trace to the method picked out by the @Pointcut
                traceEntry.endWithLocationStackTrace(0, NANOSECONDS);
            } else {
                onAfterInternal(traceEntry, 2);
            }
        }
    }

    @Pointcut(className = "org.glowroot.agent.ui.sandbox.ExpensiveCall", methodName = "execute3",
            methodParameterTypes = {})
    public static class ExpensiveCallAdvice3 {
        private static final TimerName timerName = Agent.getTimerName("expensive 3");
        @OnBefore
        public static Span onBefore(ThreadContext context, @BindReceiver Object expensiveCall,
                @BindClassMeta ExpensiveCallInvoker expensiveCallInvoker) {
            return onBeforeInternal(context, expensiveCall, expensiveCallInvoker, timerName);
        }
        @OnAfter
        public static void onAfter(@BindTraveler Span traceEntry) {
            if (traceEntry != null && random.nextDouble() < 0.05) {
                // Span.endWithLocationStackTrace() must be called directly from @On.. method
                // so it can strip back the stack trace to the method picked out by the @Pointcut
                traceEntry.endWithLocationStackTrace(0, NANOSECONDS);
            } else {
                onAfterInternal(traceEntry, 3);
            }
        }
    }

    @Pointcut(className = "org.glowroot.agent.ui.sandbox.ExpensiveCall", methodName = "execute4",
            methodParameterTypes = {})
    public static class ExpensiveCallAdvice4 {
        private static final TimerName timerName = Agent.getTimerName("expensive 4");
        @OnBefore
        public static Span onBefore(ThreadContext context, @BindReceiver Object expensiveCall,
                @BindClassMeta ExpensiveCallInvoker expensiveCallInvoker) {
            return onBeforeInternal(context, expensiveCall, expensiveCallInvoker, timerName);
        }
        @OnAfter
        public static void onAfter(@BindTraveler Span traceEntry) {
            if (traceEntry != null && random.nextDouble() < 0.05) {
                // Span.endWithLocationStackTrace() must be called directly from @On.. method
                // so it can strip back the stack trace to the method picked out by the @Pointcut
                traceEntry.endWithLocationStackTrace(0, NANOSECONDS);
            } else {
                onAfterInternal(traceEntry, 4);
            }
        }
    }

    @Pointcut(className = "org.glowroot.agent.ui.sandbox.ExpensiveCall", methodName = "execute5",
            methodParameterTypes = {})
    public static class ExpensiveCallAdvice5 {
        private static final TimerName timerName = Agent.getTimerName("expensive 5");
        @OnBefore
        public static Span onBefore(ThreadContext context, @BindReceiver Object expensiveCall,
                @BindClassMeta ExpensiveCallInvoker expensiveCallInvoker) {
            return onBeforeInternal(context, expensiveCall, expensiveCallInvoker, timerName);
        }
        @OnAfter
        public static void onAfter(@BindTraveler Span traceEntry) {
            if (traceEntry != null && random.nextDouble() < 0.05) {
                // Span.endWithLocationStackTrace() must be called directly from @On.. method
                // so it can strip back the stack trace to the method picked out by the @Pointcut
                traceEntry.endWithLocationStackTrace(0, NANOSECONDS);
            } else {
                onAfterInternal(traceEntry, 5);
            }
        }
    }

    @Pointcut(className = "org.glowroot.agent.ui.sandbox.ExpensiveCall", methodName = "execute6",
            methodParameterTypes = {})
    public static class ExpensiveCallAdvice6 {
        private static final TimerName timerName = Agent.getTimerName("expensive 6");
        @OnBefore
        public static Span onBefore(ThreadContext context, @BindReceiver Object expensiveCall,
                @BindClassMeta ExpensiveCallInvoker expensiveCallInvoker) {
            return onBeforeInternal(context, expensiveCall, expensiveCallInvoker, timerName);
        }
        @OnAfter
        public static void onAfter(@BindTraveler Span traceEntry) {
            if (traceEntry != null && random.nextDouble() < 0.05) {
                // Span.endWithLocationStackTrace() must be called directly from @On.. method
                // so it can strip back the stack trace to the method picked out by the @Pointcut
                traceEntry.endWithLocationStackTrace(0, NANOSECONDS);
            } else {
                onAfterInternal(traceEntry, 6);
            }
        }
    }

    @Pointcut(className = "org.glowroot.agent.ui.sandbox.ExpensiveCall", methodName = "execute7",
            methodParameterTypes = {})
    public static class ExpensiveCallAdvice7 {
        private static final TimerName timerName = Agent.getTimerName("expensive 7");
        @OnBefore
        public static Span onBefore(ThreadContext context, @BindReceiver Object expensiveCall,
                @BindClassMeta ExpensiveCallInvoker expensiveCallInvoker) {
            return onBeforeInternal(context, expensiveCall, expensiveCallInvoker, timerName);
        }
        @OnAfter
        public static void onAfter(@BindTraveler Span traceEntry) {
            if (traceEntry != null && random.nextDouble() < 0.05) {
                // Span.endWithLocationStackTrace() must be called directly from @On.. method
                // so it can strip back the stack trace to the method picked out by the @Pointcut
                traceEntry.endWithLocationStackTrace(0, NANOSECONDS);
            } else {
                onAfterInternal(traceEntry, 7);
            }
        }
    }

    @Pointcut(className = "org.glowroot.agent.ui.sandbox.ExpensiveCall", methodName = "execute8",
            methodParameterTypes = {})
    public static class ExpensiveCallAdvice8 {
        private static final TimerName timerName = Agent.getTimerName("expensive 8");
        @OnBefore
        public static Span onBefore(ThreadContext context, @BindReceiver Object expensiveCall,
                @BindClassMeta ExpensiveCallInvoker expensiveCallInvoker) {
            return onBeforeInternal(context, expensiveCall, expensiveCallInvoker, timerName);
        }
        @OnAfter
        public static void onAfter(@BindTraveler Span traceEntry) {
            if (traceEntry != null && random.nextDouble() < 0.05) {
                // Span.endWithLocationStackTrace() must be called directly from @On.. method
                // so it can strip back the stack trace to the method picked out by the @Pointcut
                traceEntry.endWithLocationStackTrace(0, NANOSECONDS);
            } else {
                onAfterInternal(traceEntry, 8);
            }
        }
    }

    @Pointcut(className = "org.glowroot.agent.ui.sandbox.ExpensiveCall", methodName = "execute9",
            methodParameterTypes = {})
    public static class ExpensiveCallAdvice9 {
        private static final TimerName timerName =
                Agent.getTimerName("expensive 9 really long to test wrapping");
        @OnBefore
        public static Span onBefore(ThreadContext context, @BindReceiver Object expensiveCall,
                @BindClassMeta ExpensiveCallInvoker expensiveCallInvoker) {
            return onBeforeInternal(context, expensiveCall, expensiveCallInvoker, timerName);
        }
        @OnAfter
        public static void onAfter(@BindTraveler Span traceEntry) {
            if (traceEntry != null && random.nextDouble() < 0.05) {
                // Span.endWithLocationStackTrace() must be called directly from @On.. method
                // so it can strip back the stack trace to the method picked out by the @Pointcut
                traceEntry.endWithLocationStackTrace(0, NANOSECONDS);
            } else {
                onAfterInternal(traceEntry, 9);
            }
        }
    }

    private static Span onBeforeInternal(ThreadContext context, Object expensiveCall,
            ExpensiveCallInvoker expensiveCallInvoker, TimerName timerName) {
        if (random.nextDouble() < 0.05) {
            return null;
        }
        MessageSupplier messageSupplier =
                MessageSupplier.create(expensiveCallInvoker.getTraceEntryMessage(expensiveCall));
        return context.startLocalSpan(messageSupplier, timerName);
    }

    private static void onAfterInternal(Span traceEntry, int num) {
        double value = random.nextDouble();
        if (value < 0.94) {
            traceEntry.end();
        } else {
            traceEntry.endWithError(new IllegalStateException(
                    "Exception in execute" + num + "\nwith no custom error message",
                    getRandomCause()));
        }
    }

    private static QueryMessageSupplier getQueryMessageSupplierWithDetail() {
        return new QueryMessageSupplier() {
            @Override
            public Map<String, ?> get() {
                return ImmutableMap.of("attr1", "value1\nwith newline", "attr2", "value2", "attr3",
                        ImmutableMap.of("attr31",
                                ImmutableMap.of("attr311",
                                        ImmutableList.of("v311aa", "v311bb")),
                                "attr32", "value32\nwith newline", "attr33", "value33"));
            }
        };
    }

    private static Exception getRandomCause() {
        if (random.nextBoolean()) {
            return cause;
        } else {
            return null;
        }
    }
}
