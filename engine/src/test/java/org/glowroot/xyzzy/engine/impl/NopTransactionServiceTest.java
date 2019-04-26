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
package org.glowroot.xyzzy.engine.impl;

import org.junit.Test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import org.glowroot.xyzzy.engine.impl.NopTransactionService;

public class NopTransactionServiceTest {

    @Test
    public void testNopTraceEntry() {
        NopTransactionService.TRACE_ENTRY.end();
        NopTransactionService.TRACE_ENTRY.endWithLocationStackTrace(0, MILLISECONDS);
        NopTransactionService.TRACE_ENTRY.endWithError(new Throwable());
        NopTransactionService.TRACE_ENTRY.endWithError("");
        NopTransactionService.TRACE_ENTRY.endWithError("", new Throwable());
        NopTransactionService.TRACE_ENTRY.endWithInfo(new Throwable());
        assertThat(NopTransactionService.TRACE_ENTRY.getMessageSupplier()).isNull();
    }

    @Test
    public void testNopQueryEntry() {
        assertThat(NopTransactionService.QUERY_ENTRY.extend())
                .isEqualTo(NopTransactionService.TIMER);
        NopTransactionService.QUERY_ENTRY.rowNavigationAttempted();
        NopTransactionService.QUERY_ENTRY.incrementCurrRow();
        NopTransactionService.QUERY_ENTRY.setCurrRow(0);
    }

    @Test
    public void testNopAsyncTraceEntry() {
        NopTransactionService.ASYNC_TRACE_ENTRY.stopSyncTimer();
        assertThat(NopTransactionService.ASYNC_TRACE_ENTRY.extendSyncTimer(null))
                .isEqualTo(NopTransactionService.TIMER);
    }

    @Test
    public void testNopAsyncQueryEntry() {
        NopTransactionService.ASYNC_QUERY_ENTRY.stopSyncTimer();
        assertThat(NopTransactionService.ASYNC_QUERY_ENTRY.extendSyncTimer(null))
                .isEqualTo(NopTransactionService.TIMER);
    }

    @Test
    public void testNopAuxThreadContext() {
        assertThat(NopTransactionService.AUX_THREAD_CONTEXT.start())
                .isEqualTo(NopTransactionService.TRACE_ENTRY);
        assertThat(NopTransactionService.AUX_THREAD_CONTEXT.startAndMarkAsyncTransactionComplete())
                .isEqualTo(NopTransactionService.TRACE_ENTRY);
    }
}
