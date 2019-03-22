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
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.glowroot.xyzzy.instrumentation.api.Agent;
import org.glowroot.xyzzy.instrumentation.api.Getter;
import org.glowroot.xyzzy.instrumentation.api.Message;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.OptionalThreadContext;
import org.glowroot.xyzzy.instrumentation.api.OptionalThreadContext.AlreadyInTransactionBehavior;
import org.glowroot.xyzzy.instrumentation.api.Span;
import org.glowroot.xyzzy.instrumentation.api.ThreadContext.Priority;
import org.glowroot.xyzzy.instrumentation.api.TimerName;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindTraveler;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnAfter;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnBefore;
import org.glowroot.xyzzy.instrumentation.api.weaving.Pointcut;

public class NestableCallInstrumentation {

    private static final ImmutableList<String> USERS = ImmutableList.of("able", "baker", "charlie");

    private static final AtomicInteger counter = new AtomicInteger();

    @Pointcut(className = "org.glowroot.agent.ui.sandbox.NestableCall", methodName = "execute",
            methodParameterTypes = {}, nestingGroup = "ui-sandbox-nestable")
    public static class NestableCallAdvice {
        private static final TimerName timerName = Agent.getTimerName("nestable");
        private static final Random random = new Random();
        @OnBefore
        public static Span onBefore(OptionalThreadContext context) {
            int count = counter.getAndIncrement();
            String transactionName;
            String headline;
            if (random.nextBoolean()) {
                transactionName = "Nestable with a very long trace headline";
                headline = "Nestable with a very long trace headline to test wrapping"
                        + " abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz";
            } else {
                transactionName =
                        Strings.repeat(String.valueOf((char) ('a' + random.nextInt(26))), 40);
                headline = transactionName;
            }
            if (random.nextInt(10) == 0) {
                // create a long-tail of transaction names to simulate long-tail of urls
                transactionName += random.nextInt(1000);
            }
            Span traceEntry;
            if (count % 10 == 0) {
                traceEntry = context.startIncomingSpan("Background", transactionName,
                        new GetterImpl(), new Object(), getRootMessageSupplier(headline), timerName,
                        AlreadyInTransactionBehavior.CAPTURE_LOCAL_SPAN);
            } else {
                traceEntry = context.startIncomingSpan("Sandbox", transactionName, new GetterImpl(),
                        new Object(), getRootMessageSupplier(headline), timerName,
                        AlreadyInTransactionBehavior.CAPTURE_LOCAL_SPAN);
            }
            int index = count % (USERS.size() + 1);
            if (index < USERS.size()) {
                context.setTransactionUser(USERS.get(index), Priority.USER_INSTRUMENTATION);
            } else {
                context.setTransactionUser(null, Priority.USER_INSTRUMENTATION);
            }
            if (random.nextBoolean()) {
                context.addTransactionAttribute("My First Attribute", "hello world");
                context.addTransactionAttribute("My First Attribute", "hello world");
                context.addTransactionAttribute("My First Attribute",
                        "hello world " + random.nextInt(10));
            }
            if (random.nextBoolean()) {
                context.addTransactionAttribute("Second", "val " + random.nextInt(10));
            }
            if (random.nextBoolean()) {
                context.addTransactionAttribute("A Very Long Attribute Value",
                        Strings.repeat("abcdefghijklmnopqrstuvwxyz", 3));
            }
            if (random.nextBoolean()) {
                context.addTransactionAttribute("Another",
                        "a b c d e f g h i j k l m n o p q r s t u v w x y z"
                                + " a b c d e f g h i j k l m n o p q r s t u v w x y z");
            }
            return traceEntry;
        }
        @OnAfter
        public static void onAfter(@BindTraveler Span traceEntry) {
            double value = random.nextDouble();
            if (value < 0.8) {
                traceEntry.end();
            } else if (value < 0.9) {
                traceEntry.endWithError(new IllegalStateException("root entry randomized error"));
            } else {
                String reallyLongErrorMessage = Strings.repeat("abcdefghijklmnopqrstuvwxyz ", 100);
                traceEntry.endWithError(new IllegalStateException(reallyLongErrorMessage));
            }
        }
    }

    private static MessageSupplier getRootMessageSupplier(final String traceEntryMessage) {
        return new MessageSupplier() {
            @Override
            public Message get() {
                Map<String, ?> detail = ImmutableMap
                        .of("attr1", getLongDetailValue(false), "attr2", "value2", "attr3",
                                ImmutableMap.of("attr31",
                                        ImmutableMap.of("attr311",
                                                ImmutableList.of("v311a", "v311b")),
                                        "attr32", getLongDetailValue(true), "attr33",
                                        getLongDetailValue(false)));
                return Message.create(traceEntryMessage, detail);
            }
        };
    }

    private static String getLongDetailValue(boolean spaces) {
        int length = new Random().nextInt(200);
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(i % 10);
            if (spaces && i % 10 == 0) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    private static class GetterImpl implements Getter<Object> {

        @Override
        public String get(Object carrier, String key) {
            return null;
        }
    }
}
