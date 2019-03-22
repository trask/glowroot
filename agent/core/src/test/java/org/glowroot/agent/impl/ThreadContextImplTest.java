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
package org.glowroot.agent.impl;

import com.google.common.base.Ticker;
import org.junit.Before;
import org.junit.Test;

import org.glowroot.xyzzy.engine.bytecode.api.ThreadContextThreadLocal;
import org.glowroot.xyzzy.engine.impl.NopTransactionService;
import org.glowroot.xyzzy.engine.impl.TimerNameImpl;
import org.glowroot.xyzzy.instrumentation.api.Getter;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.OptionalThreadContext.AlreadyInTransactionBehavior;
import org.glowroot.xyzzy.instrumentation.api.QueryMessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.Setter;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ThreadContextImplTest {

    private static final Getter<Object> GETTER = new GetterImpl();

    private static final Setter<Object> SETTER = new SetterImpl();

    private static final Object CARRIER = new Object();

    private ThreadContextImpl threadContext;

    private MessageSupplier messageSupplier = mock(MessageSupplier.class);
    private QueryMessageSupplier queryMessageSupplier = mock(QueryMessageSupplier.class);
    private TimerNameImpl timerName = mock(TimerNameImpl.class);

    @Before
    public void beforeEachTest() {
        Transaction transaction = mock(Transaction.class);
        MessageSupplier messageSupplier = mock(MessageSupplier.class);
        TimerNameImpl rootTimerName = mock(TimerNameImpl.class);
        Ticker ticker = mock(Ticker.class);
        ThreadContextThreadLocal.Holder threadContextHolder =
                mock(ThreadContextThreadLocal.Holder.class);
        threadContext =
                new ThreadContextImpl(transaction, null, null, messageSupplier, rootTimerName, 0,
                        false, 0, 0, null, false, ticker, threadContextHolder, null, 0, 0);
    }

    @Test
    public void testStartTransaction() {
        assertThat(threadContext.startIncomingSpan(null, "text", GETTER, CARRIER, messageSupplier,
                timerName, AlreadyInTransactionBehavior.CAPTURE_LOCAL_SPAN))
                        .isEqualTo(NopTransactionService.LOCAL_SPAN);
        assertThat(threadContext.startIncomingSpan("type", null, GETTER, CARRIER, messageSupplier,
                timerName, AlreadyInTransactionBehavior.CAPTURE_LOCAL_SPAN))
                        .isEqualTo(NopTransactionService.LOCAL_SPAN);
        assertThat(threadContext.startIncomingSpan("type", "text", GETTER, CARRIER, null, timerName,
                AlreadyInTransactionBehavior.CAPTURE_LOCAL_SPAN))
                        .isEqualTo(NopTransactionService.LOCAL_SPAN);
        assertThat(threadContext.startIncomingSpan("type", "text", GETTER, CARRIER, messageSupplier,
                null, AlreadyInTransactionBehavior.CAPTURE_LOCAL_SPAN))
                        .isEqualTo(NopTransactionService.LOCAL_SPAN);
    }

    @Test
    public void testStartTraceEntry() {
        assertThat(threadContext.startLocalSpan(null, timerName))
                .isEqualTo(NopTransactionService.LOCAL_SPAN);
        assertThat(threadContext.startLocalSpan(messageSupplier, null))
                .isEqualTo(NopTransactionService.LOCAL_SPAN);
        assertThat(threadContext.startLocalSpan(messageSupplier, timerName)
                .getClass().getName()).endsWith("$DummyTraceEntryOrQuery");
    }

    @Test
    public void testStartQueryEntry() {
        assertThat(threadContext.startQuerySpan(null, "text", queryMessageSupplier, timerName))
                .isEqualTo(NopTransactionService.QUERY_SPAN);
        assertThat(threadContext.startQuerySpan("type", null, queryMessageSupplier, timerName))
                .isEqualTo(NopTransactionService.QUERY_SPAN);
        assertThat(threadContext.startQuerySpan("type", "text", null, timerName))
                .isEqualTo(NopTransactionService.QUERY_SPAN);
        assertThat(threadContext.startQuerySpan("type", "text", queryMessageSupplier, null))
                .isEqualTo(NopTransactionService.QUERY_SPAN);
        assertThat(threadContext.startQuerySpan("type", "text", queryMessageSupplier, timerName)
                .getClass().getName()).endsWith("$DummyTraceEntryOrQuery");
    }

    @Test
    public void testStartQueryEntryWithExecutionCount() {
        assertThat(threadContext.startQuerySpan(null, "text", 1, queryMessageSupplier, timerName))
                .isEqualTo(NopTransactionService.QUERY_SPAN);
        assertThat(threadContext.startQuerySpan("type", null, 1, queryMessageSupplier, timerName))
                .isEqualTo(NopTransactionService.QUERY_SPAN);
        assertThat(threadContext.startQuerySpan("type", "text", 1, null, timerName))
                .isEqualTo(NopTransactionService.QUERY_SPAN);
        assertThat(threadContext.startQuerySpan("type", "text", 1, queryMessageSupplier, null))
                .isEqualTo(NopTransactionService.QUERY_SPAN);
        assertThat(threadContext.startQuerySpan("type", "text", 1, queryMessageSupplier, timerName)
                .getClass().getName()).endsWith("$DummyTraceEntryOrQuery");
    }

    @Test
    public void testStartAsyncQueryEntry() {
        assertThat(threadContext.startAsyncQuerySpan(null, "text", queryMessageSupplier, timerName))
                .isEqualTo(NopTransactionService.ASYNC_QUERY_SPAN);
        assertThat(threadContext.startAsyncQuerySpan("type", null, queryMessageSupplier, timerName))
                .isEqualTo(NopTransactionService.ASYNC_QUERY_SPAN);
        assertThat(threadContext.startAsyncQuerySpan("type", "text", null, timerName))
                .isEqualTo(NopTransactionService.ASYNC_QUERY_SPAN);
        assertThat(threadContext.startAsyncQuerySpan("type", "text", queryMessageSupplier, null))
                .isEqualTo(NopTransactionService.ASYNC_QUERY_SPAN);
        assertThat(
                threadContext.startAsyncQuerySpan("type", "text", queryMessageSupplier, timerName)
                        .getClass().getName()).endsWith("$DummyTraceEntryOrQuery");
    }

    @Test
    public void testStartServiceCallEntry() {
        assertThat(threadContext.startOutgoingSpan(null, "text", SETTER, CARRIER, messageSupplier,
                timerName)).isEqualTo(NopTransactionService.LOCAL_SPAN);
        assertThat(threadContext.startOutgoingSpan("type", null, SETTER, CARRIER, messageSupplier,
                timerName)).isEqualTo(NopTransactionService.LOCAL_SPAN);
        assertThat(
                threadContext.startOutgoingSpan("type", "text", SETTER, CARRIER, null, timerName))
                        .isEqualTo(NopTransactionService.LOCAL_SPAN);
        assertThat(threadContext.startOutgoingSpan("type", "text", SETTER, CARRIER, messageSupplier,
                null)).isEqualTo(NopTransactionService.LOCAL_SPAN);
        assertThat(threadContext
                .startOutgoingSpan("type", "text", SETTER, CARRIER, messageSupplier, timerName)
                .getClass().getName()).endsWith("$DummyTraceEntryOrQuery");
    }

    @Test
    public void testStartAsyncServiceCallEntry() {
        assertThat(threadContext.startAsyncOutgoingSpan(null, "text", SETTER, CARRIER,
                messageSupplier, timerName)).isEqualTo(NopTransactionService.ASYNC_SPAN);
        assertThat(threadContext.startAsyncOutgoingSpan("type", null, SETTER, CARRIER,
                messageSupplier, timerName)).isEqualTo(NopTransactionService.ASYNC_SPAN);
        assertThat(threadContext.startAsyncOutgoingSpan("type", "text", SETTER, CARRIER, null,
                timerName)).isEqualTo(NopTransactionService.ASYNC_SPAN);
        assertThat(threadContext.startAsyncOutgoingSpan("type", "text", SETTER, CARRIER,
                messageSupplier, null)).isEqualTo(NopTransactionService.ASYNC_SPAN);
        assertThat(threadContext
                .startAsyncOutgoingSpan("type", "text", SETTER, CARRIER, messageSupplier, timerName)
                .getClass().getName()).endsWith("$DummyTraceEntryOrQuery");
    }

    @Test
    public void testStartTimer() {
        assertThat(threadContext.startTimer(null)).isEqualTo(NopTransactionService.TIMER);

        threadContext.setCurrentTimer(null);
        assertThat(threadContext.startTimer(timerName)).isEqualTo(NopTransactionService.TIMER);
    }

    @Test
    public void testSetters() {
        threadContext.setTransactionType(null, 0);
        threadContext.setTransactionName(null, 0);
        threadContext.setTransactionUser(null, 0);
        threadContext.addTransactionAttribute(null, null);
        threadContext.setTransactionSlowThreshold(-1, MILLISECONDS, 0);
        threadContext.setTransactionSlowThreshold(0, null, 0);
        threadContext.setTransactionError((String) null);
    }

    private static class GetterImpl implements Getter<Object> {

        @Override
        public String get(Object carrier, String key) {
            return null;
        }
    }

    private static class SetterImpl implements Setter<Object> {

        @Override
        public void put(Object carrier, String key, String value) {}
    }
}
