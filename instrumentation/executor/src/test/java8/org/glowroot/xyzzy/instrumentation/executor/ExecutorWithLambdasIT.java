/*
 * Copyright 2019 the original author or authors.
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
package org.glowroot.xyzzy.instrumentation.executor;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;

import com.google.common.collect.Queues;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.xyzzy.test.harness.AppUnderTest;
import org.glowroot.xyzzy.test.harness.Container;
import org.glowroot.xyzzy.test.harness.IncomingSpan;
import org.glowroot.xyzzy.test.harness.LocalSpans;
import org.glowroot.xyzzy.test.harness.Span;
import org.glowroot.xyzzy.test.harness.TransactionMarker;
import org.glowroot.xyzzy.test.harness.impl.JavaagentContainer;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.glowroot.xyzzy.test.harness.util.HarnessAssertions.assertSingleLocalSpanMessage;

public class ExecutorWithLambdasIT {

    private static Container container;

    @BeforeClass
    public static void setUp() throws Exception {
        // tests only work with javaagent container because they need to weave bootstrap classes
        // that implement Executor and ExecutorService
        container = JavaagentContainer.create();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        container.close();
    }

    @After
    public void afterEachTest() throws Exception {
        container.resetInstrumentationConfig();
    }

    @Test
    public void shouldCaptureExecute() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(DoExecuteRunnableWithLambda.class);
        // then
        checkTrace(incomingSpan);
    }

    @Test
    public void shouldCaptureNestedExecute() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(DoNestedExecuteRunnableWithLambda.class);
        // then
        checkTrace(incomingSpan);
    }

    private static void checkTrace(IncomingSpan incomingSpan) {
        assertThat(incomingSpan.auxThreadRootTimer()).isNotNull();
        assertThat(incomingSpan.asyncTimers()).isEmpty();
        assertThat(incomingSpan.auxThreadRootTimer().name()).isEqualTo("auxiliary thread");
        assertThat(incomingSpan.auxThreadRootTimer().count()).isEqualTo(3);
        // should be 300ms, but margin of error, esp. in travis builds is high
        assertThat(incomingSpan.auxThreadRootTimer().totalNanos())
                .isGreaterThanOrEqualTo(MILLISECONDS.toNanos(250));
        assertThat(incomingSpan.auxThreadRootTimer().childTimers().size()).isEqualTo(1);
        assertThat(incomingSpan.auxThreadRootTimer().childTimers().get(0).name())
                .isEqualTo("test local span");

        List<Span> spans = incomingSpan.childSpans();
        assertThat(spans.size()).isBetween(1, 3);
        for (Span span : spans) {
            assertSingleLocalSpanMessage(span).isEqualTo("test local span / CreateLocalSpan");
        }
    }

    public static class DoExecuteRunnableWithLambda implements AppUnderTest, TransactionMarker {

        private ThreadPoolExecutor executor;
        private CountDownLatch latch;

        @Override
        public void executeApp() throws Exception {
            executor =
                    new ThreadPoolExecutor(1, 1, 60, MILLISECONDS, Queues.newLinkedBlockingQueue());
            // need to pre-create threads, otherwise lambda execution will be captured by the
            // initial thread run, and won't really test lambda execution capture
            executor.prestartAllCoreThreads();
            transactionMarker();
        }

        @Override
        public void transactionMarker() throws Exception {
            latch = new CountDownLatch(3);
            executor.execute(this::run);
            executor.execute(this::run);
            executor.execute(this::run);
            latch.await();
            executor.shutdown();
            executor.awaitTermination(10, SECONDS);
        }

        private void run() {
            LocalSpans.createTestSpan(100);
            latch.countDown();
        }
    }

    public static class DoNestedExecuteRunnableWithLambda
            implements AppUnderTest, TransactionMarker {

        private ThreadPoolExecutor executor;
        private CountDownLatch latch;

        @Override
        public void executeApp() throws Exception {
            executor =
                    new ThreadPoolExecutor(1, 1, 60, MILLISECONDS, Queues.newLinkedBlockingQueue());
            // need to pre-create threads, otherwise lambda execution will be captured by the
            // initial thread run, and won't really test lambda execution capture
            executor.prestartAllCoreThreads();
            transactionMarker();
        }

        @Override
        public void transactionMarker() throws Exception {
            MoreExecutors.directExecutor().execute(this::outerRun);
        }

        private void outerRun() {
            latch = new CountDownLatch(3);
            executor.execute(this::innerRun);
            executor.execute(this::innerRun);
            executor.execute(this::innerRun);
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            executor.shutdown();
            try {
                executor.awaitTermination(10, SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        private void innerRun() {
            LocalSpans.createTestSpan(100);
            latch.countDown();
        }
    }
}
