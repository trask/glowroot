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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.agent.it.harness.AppUnderTest;
import org.glowroot.agent.it.harness.Container;
import org.glowroot.agent.it.harness.TraceEntryMarker;
import org.glowroot.agent.it.harness.TransactionMarker;
import org.glowroot.agent.it.harness.impl.JavaagentContainer;
import org.glowroot.agent.it.harness.model.ServerSpan;
import org.glowroot.agent.it.harness.model.Span;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.glowroot.agent.it.harness.validation.HarnessAssertions.assertSingleLocalSpanMessage;

// see https://github.com/glowroot/glowroot/issues/564
public class ProblemExecutorIT {

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
        container.resetConfig();
    }

    @Test
    public void shouldCaptureSubmit() throws Exception {
        // when
        ServerSpan trace = container.execute(DoSubmitRunnable.class);

        // then
        assertThat(trace.auxThreadRootTimer()).isNotNull();
        assertThat(trace.asyncTimers()).isEmpty();
        assertThat(trace.auxThreadRootTimer().name()).isEqualTo("auxiliary thread");
        assertThat(trace.auxThreadRootTimer().count()).isEqualTo(3);
        // should be 300ms, but margin of error, esp. in travis builds is high
        assertThat(trace.auxThreadRootTimer().totalNanos())
                .isGreaterThanOrEqualTo(MILLISECONDS.toNanos(250));
        assertThat(trace.auxThreadRootTimer().childTimers().size()).isEqualTo(1);
        assertThat(trace.auxThreadRootTimer().childTimers().get(0).name())
                .isEqualTo("mock trace entry marker");

        List<Span> spans = trace.childSpans();
        assertThat(spans).hasSize(3);
        for (Span span : spans) {
            assertSingleLocalSpanMessage(span).isEqualTo("trace entry marker / CreateTraceEntry");
        }
    }

    private static ExecutorService createExecutorService() {
        return new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, SECONDS,
                new SynchronousQueue<Runnable>()) {
            @Override
            protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
                return new ProblemFutureTask<T>((ProblemRunnable) runnable, value);
            }
        };
    }

    public static class DoSubmitRunnable implements AppUnderTest, TransactionMarker {

        @Override
        public void executeApp() throws Exception {
            transactionMarker();
        }

        @Override
        public void transactionMarker() throws Exception {
            ExecutorService executor = createExecutorService();
            final CountDownLatch latch = new CountDownLatch(3);
            executor.submit(new ProblemRunnable() {
                @Override
                public void run() {
                    new CreateTraceEntry().traceEntryMarker();
                    latch.countDown();
                }
            });
            executor.submit(new ProblemRunnable() {
                @Override
                public void run() {
                    new CreateTraceEntry().traceEntryMarker();
                    latch.countDown();
                }
            });
            executor.submit(new ProblemRunnable() {
                @Override
                public void run() {
                    new CreateTraceEntry().traceEntryMarker();
                    latch.countDown();
                }
            });
            latch.await();
            executor.shutdown();
            executor.awaitTermination(10, SECONDS);
        }
    }

    private static class ProblemRunnable implements Runnable {

        @Override
        public void run() {}
    }

    private static class ProblemFutureTask<V> extends FutureTask<V> {

        public ProblemFutureTask(ProblemRunnable runnable, V result) {
            super(runnable, result);
        }
    }

    private static class CreateTraceEntry implements TraceEntryMarker {

        @Override
        public void traceEntryMarker() {
            try {
                MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
            }
        }
    }
}
