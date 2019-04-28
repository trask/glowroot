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
package org.glowroot.xyzzy.instrumentation.executor;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.agent.it.harness.AppUnderTest;
import org.glowroot.agent.it.harness.Container;
import org.glowroot.agent.it.harness.TraceEntryMarker;
import org.glowroot.agent.it.harness.TransactionMarker;
import org.glowroot.agent.it.harness.impl.JavaagentContainer;
import org.glowroot.agent.it.harness.model.Trace;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

public class ExecutorIT {

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
    public void shouldCaptureExecute() throws Exception {
        // when
        Trace trace = container.execute(DoExecuteRunnable.class);
        // then
        checkTrace(trace, false, false);
    }

    @Test
    public void shouldCaptureExecuteFutureTask() throws Exception {
        // when
        Trace trace = container.execute(DoExecuteFutureTask.class);
        // then
        checkTrace(trace, false, false);
    }

    @Test
    public void shouldCaptureSubmitCallable() throws Exception {
        // when
        Trace trace = container.execute(DoSubmitCallable.class);
        // then
        checkTrace(trace, false, true);
    }

    @Test
    public void shouldCaptureSubmitRunnableAndCallable() throws Exception {
        // when
        Trace trace = container.execute(DoSubmitRunnableAndCallable.class);
        // then
        checkTrace(trace, false, true);
    }

    @Test
    public void shouldNotCaptureTraceEntryForEmptyAuxThread() throws Exception {
        // when
        Trace trace = container.execute(DoSimpleSubmitRunnableWork.class);

        // then
        assertThat(trace.auxThreadRootTimer()).isNotNull();
        assertThat(trace.asyncTimers()).isEmpty();
        assertThat(trace.auxThreadRootTimer().name()).isEqualTo("auxiliary thread");
        assertThat(trace.auxThreadRootTimer().count()).isEqualTo(3);
        assertThat(trace.auxThreadRootTimer().totalNanos())
                .isGreaterThanOrEqualTo(MILLISECONDS.toNanos(500));
        assertThat(trace.auxThreadRootTimer().childTimers()).isEmpty();
        assertThat(trace.entries()).isEmpty();
    }

    @Test
    public void shouldNotCaptureAlreadyCompletedFutureGet() throws Exception {
        // when
        Trace trace = container.execute(CallFutureGetOnAlreadyCompletedFuture.class);

        // then
        assertThat(trace.mainThreadRootTimer().childTimers()).isEmpty();
    }

    @Test
    public void shouldCaptureNestedFutureGet() throws Exception {
        // when
        Trace trace = container.execute(CallFutureGetOnNestedFuture.class);

        // then
        Iterator<Trace.Entry> i = trace.entries().iterator();

        Trace.Entry entry = i.next();
        assertThat(entry.depth()).isEqualTo(0);
        assertThat(entry.message()).isEqualTo("auxiliary thread");

        entry = i.next();
        assertThat(entry.depth()).isEqualTo(1);
        assertThat(entry.message()).isEqualTo("trace entry marker / CreateTraceEntry");

        entry = i.next();
        assertThat(entry.depth()).isEqualTo(1);
        assertThat(entry.message()).isEqualTo("auxiliary thread");

        entry = i.next();
        assertThat(entry.depth()).isEqualTo(2);
        assertThat(entry.message()).isEqualTo("trace entry marker / CreateTraceEntry");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldCaptureInvokeAll() throws Exception {
        // when
        Trace trace = container.execute(DoInvokeAll.class);
        // then
        checkTrace(trace, false, true);
    }

    @Test
    public void shouldCaptureInvokeAllWithTimeout() throws Exception {
        // when
        Trace trace = container.execute(DoInvokeAllWithTimeout.class);
        // then
        checkTrace(trace, false, true);
    }

    @Test
    public void shouldCaptureInvokeAny() throws Exception {
        // when
        Trace trace = container.execute(DoInvokeAny.class);
        // then
        checkTrace(trace, true, false);
    }

    @Test
    public void shouldCaptureInvokeAnyWithTimeout() throws Exception {
        // when
        Trace trace = container.execute(DoInvokeAnyWithTimeout.class);
        // then
        checkTrace(trace, true, false);
    }

    @Test
    public void shouldCaptureNestedExecute() throws Exception {
        // when
        Trace trace = container.execute(DoNestedExecuteRunnable.class);
        // then
        checkTrace(trace, false, false);
    }

    @Test
    public void shouldCaptureNestedSubmit() throws Exception {
        // when
        Trace trace = container.execute(DoNestedSubmitCallable.class);
        // then
        checkTrace(trace, false, false);
    }

    @Test
    public void shouldCaptureDelegatingExecutor() throws Exception {
        // when
        Trace trace = container.execute(DoDelegatingExecutor.class);
        // then
        checkTrace(trace, false, false);
    }

    private static void checkTrace(Trace trace, boolean isAny, boolean withFuture) {
        if (withFuture) {
            assertThat(trace.mainThreadRootTimer().childTimers().size()).isEqualTo(1);
            assertThat(trace.mainThreadRootTimer().childTimers().get(0).name())
                    .isEqualTo("wait on future");
            assertThat(trace.mainThreadRootTimer().childTimers().get(0).count())
                    .isGreaterThanOrEqualTo(1);
            assertThat(trace.mainThreadRootTimer().childTimers().get(0).count())
                    .isLessThanOrEqualTo(3);
        }
        assertThat(trace.auxThreadRootTimer()).isNotNull();
        assertThat(trace.asyncTimers()).isEmpty();
        assertThat(trace.auxThreadRootTimer().name()).isEqualTo("auxiliary thread");
        if (isAny) {
            assertThat(trace.auxThreadRootTimer().count()).isBetween(1L, 3L);
            // should be 100-300ms, but margin of error, esp. in travis builds is high
            assertThat(trace.auxThreadRootTimer().totalNanos())
                    .isGreaterThanOrEqualTo(MILLISECONDS.toNanos(50));
        } else {
            assertThat(trace.auxThreadRootTimer().count()).isEqualTo(3);
            // should be 300ms, but margin of error, esp. in travis builds is high
            assertThat(trace.auxThreadRootTimer().totalNanos())
                    .isGreaterThanOrEqualTo(MILLISECONDS.toNanos(250));
        }
        assertThat(trace.auxThreadRootTimer().childTimers().size()).isEqualTo(1);
        assertThat(trace.auxThreadRootTimer().childTimers().get(0).name())
                .isEqualTo("mock trace entry marker");
        List<Trace.Entry> entries = trace.entries();

        if (isAny) {
            entries = Lists.newArrayList(entries);
            for (Iterator<Trace.Entry> i = entries.iterator(); i.hasNext();) {
                if (i.next().message().equals(
                        "this auxiliary thread was still running when the transaction ended")) {
                    i.remove();
                }
            }
        }
        if (isAny) {
            assertThat(entries.size()).isBetween(2, 6);
        } else {
            assertThat(entries).hasSize(6);
        }
        for (int i = 0; i < entries.size(); i += 2) {
            assertThat(entries.get(i).depth()).isEqualTo(0);
            assertThat(entries.get(i).message()).isEqualTo("auxiliary thread");

            assertThat(entries.get(i + 1).depth()).isEqualTo(1);
            assertThat(entries.get(i + 1).message())
                    .isEqualTo("trace entry marker / CreateTraceEntry");
        }
    }

    private static ExecutorService createExecutorService() {
        return Executors.newCachedThreadPool();
    }

    public static class DoExecuteRunnable implements AppUnderTest, TransactionMarker {

        @Override
        public void executeApp() throws Exception {
            transactionMarker();
        }

        @Override
        public void transactionMarker() throws Exception {
            ExecutorService executor = createExecutorService();
            final CountDownLatch latch = new CountDownLatch(3);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    new CreateTraceEntry().traceEntryMarker();
                    latch.countDown();
                }
            });
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    new CreateTraceEntry().traceEntryMarker();
                    latch.countDown();
                }
            });
            executor.execute(new Runnable() {
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

    public static class DoExecuteFutureTask implements AppUnderTest, TransactionMarker {

        @Override
        public void executeApp() throws Exception {
            transactionMarker();
        }

        @Override
        public void transactionMarker() throws Exception {
            ExecutorService executor = createExecutorService();
            final CountDownLatch latch = new CountDownLatch(3);
            executor.execute(new FutureTask<Void>(new Callable<Void>() {
                @Override
                public Void call() {
                    new CreateTraceEntry().traceEntryMarker();
                    latch.countDown();
                    return null;
                }
            }));
            executor.submit(new FutureTask<Void>(new Callable<Void>() {
                @Override
                public Void call() {
                    new CreateTraceEntry().traceEntryMarker();
                    latch.countDown();
                    return null;
                }
            }));
            executor.submit(new FutureTask<Void>(new Callable<Void>() {
                @Override
                public Void call() {
                    new CreateTraceEntry().traceEntryMarker();
                    latch.countDown();
                    return null;
                }
            }));
            latch.await();
            executor.shutdown();
            executor.awaitTermination(10, SECONDS);
        }
    }

    public static class DoSubmitCallable implements AppUnderTest, TransactionMarker {

        @Override
        public void executeApp() throws Exception {
            transactionMarker();
        }

        @Override
        public void transactionMarker() throws Exception {
            ExecutorService executor = createExecutorService();
            Future<Void> future1 = executor.submit(new Callable<Void>() {
                @Override
                public Void call() {
                    new CreateTraceEntry().traceEntryMarker();
                    return null;
                }
            });
            Future<Void> future2 = executor.submit(new Callable<Void>() {
                @Override
                public Void call() {
                    new CreateTraceEntry().traceEntryMarker();
                    return null;
                }
            });
            Future<Void> future3 = executor.submit(new Callable<Void>() {
                @Override
                public Void call() {
                    new CreateTraceEntry().traceEntryMarker();
                    return null;
                }
            });
            future1.get();
            future2.get();
            future3.get();
        }
    }

    public static class DoSubmitRunnableAndCallable implements AppUnderTest, TransactionMarker {

        @Override
        public void executeApp() throws Exception {
            transactionMarker();
        }

        @Override
        public void transactionMarker() throws Exception {
            ExecutorService executor = createExecutorService();
            Future<Void> future1 = executor.submit((Callable<Void>) new RunnableAndCallableWork());
            Future<Void> future2 = executor.submit((Callable<Void>) new RunnableAndCallableWork());
            Future<Void> future3 = executor.submit((Callable<Void>) new RunnableAndCallableWork());
            future1.get();
            future2.get();
            future3.get();
        }
    }

    public static class DoSimpleSubmitRunnableWork implements AppUnderTest, TransactionMarker {

        @Override
        public void executeApp() throws Exception {
            transactionMarker();
        }

        @Override
        public void transactionMarker() throws Exception {
            ExecutorService executor = createExecutorService();
            Future<?> future1 = executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        MILLISECONDS.sleep(200);
                    } catch (InterruptedException e) {
                    }
                }
            });
            Future<?> future2 = executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        MILLISECONDS.sleep(200);
                    } catch (InterruptedException e) {
                    }
                }
            });
            Future<?> future3 = executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        MILLISECONDS.sleep(200);
                    } catch (InterruptedException e) {
                    }
                }
            });
            future1.get();
            future2.get();
            future3.get();
        }
    }

    public static class CallFutureGetOnAlreadyCompletedFuture
            implements AppUnderTest, TransactionMarker {

        @Override
        public void executeApp() throws Exception {
            transactionMarker();
        }

        @Override
        public void transactionMarker() throws Exception {
            ExecutorService executor = createExecutorService();
            Future<Void> future = executor.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    return null;
                }
            });
            while (!future.isDone()) {
                MILLISECONDS.sleep(1);
            }
            future.get();
        }
    }

    public static class CallFutureGetOnNestedFuture implements AppUnderTest, TransactionMarker {

        @Override
        public void executeApp() throws Exception {
            transactionMarker();
        }

        @Override
        public void transactionMarker() throws Exception {
            final ExecutorService executor = createExecutorService();
            Future<Void> future = executor.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    new CreateTraceEntry().traceEntryMarker();
                    Future<Void> future = executor.submit(new Callable<Void>() {
                        @Override
                        public Void call() {
                            new CreateTraceEntry().traceEntryMarker();
                            return null;
                        }
                    });
                    future.get();
                    return null;
                }
            });
            future.get();
        }
    }

    public static class DoInvokeAll implements AppUnderTest, TransactionMarker {

        @Override
        public void executeApp() throws Exception {
            transactionMarker();
        }

        @Override
        public void transactionMarker() throws Exception {
            ExecutorService executor = createExecutorService();
            List<Callable<Void>> callables = Lists.newArrayList();
            callables.add(new Callable<Void>() {
                @Override
                public Void call() {
                    new CreateTraceEntry().traceEntryMarker();
                    return null;
                }
            });
            callables.add(new Callable<Void>() {
                @Override
                public Void call() {
                    new CreateTraceEntry().traceEntryMarker();
                    return null;
                }
            });
            callables.add(new Callable<Void>() {
                @Override
                public Void call() {
                    new CreateTraceEntry().traceEntryMarker();
                    return null;
                }
            });
            for (Future<Void> future : executor.invokeAll(callables)) {
                future.get();
            }
        }
    }

    public static class DoInvokeAllWithTimeout implements AppUnderTest, TransactionMarker {

        @Override
        public void executeApp() throws Exception {
            transactionMarker();
        }

        @Override
        public void transactionMarker() throws Exception {
            ExecutorService executor = createExecutorService();
            List<Callable<Void>> callables = Lists.newArrayList();
            callables.add(new Callable<Void>() {
                @Override
                public Void call() {
                    new CreateTraceEntry().traceEntryMarker();
                    return null;
                }
            });
            callables.add(new Callable<Void>() {
                @Override
                public Void call() {
                    new CreateTraceEntry().traceEntryMarker();
                    return null;
                }
            });
            callables.add(new Callable<Void>() {
                @Override
                public Void call() {
                    new CreateTraceEntry().traceEntryMarker();
                    return null;
                }
            });
            for (Future<Void> future : executor.invokeAll(callables, 10, SECONDS)) {
                future.get();
            }
        }
    }

    public static class DoInvokeAny implements AppUnderTest, TransactionMarker {

        @Override
        public void executeApp() throws Exception {
            transactionMarker();
        }

        @Override
        public void transactionMarker() throws Exception {
            ExecutorService executor = createExecutorService();
            List<Callable<Void>> callables = Lists.newArrayList();
            callables.add(new Callable<Void>() {
                @Override
                public Void call() {
                    new CreateTraceEntry().traceEntryMarker();
                    return null;
                }
            });
            callables.add(new Callable<Void>() {
                @Override
                public Void call() {
                    new CreateTraceEntry().traceEntryMarker();
                    return null;
                }
            });
            callables.add(new Callable<Void>() {
                @Override
                public Void call() {
                    new CreateTraceEntry().traceEntryMarker();
                    return null;
                }
            });
            executor.invokeAny(callables);
        }
    }

    public static class DoInvokeAnyWithTimeout implements AppUnderTest, TransactionMarker {

        @Override
        public void executeApp() throws Exception {
            transactionMarker();
        }

        @Override
        public void transactionMarker() throws Exception {
            ExecutorService executor = createExecutorService();
            List<Callable<Void>> callables = Lists.newArrayList();
            callables.add(new Callable<Void>() {
                @Override
                public Void call() {
                    new CreateTraceEntry().traceEntryMarker();
                    return null;
                }
            });
            callables.add(new Callable<Void>() {
                @Override
                public Void call() {
                    new CreateTraceEntry().traceEntryMarker();
                    return null;
                }
            });
            callables.add(new Callable<Void>() {
                @Override
                public Void call() {
                    new CreateTraceEntry().traceEntryMarker();
                    return null;
                }
            });
            executor.invokeAny(callables, 10, SECONDS);
        }
    }

    public static class DoNestedExecuteRunnable implements AppUnderTest, TransactionMarker {

        @Override
        public void executeApp() throws Exception {
            transactionMarker();
        }

        @Override
        public void transactionMarker() throws Exception {
            MoreExecutors.directExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    ExecutorService executor = createExecutorService();
                    final CountDownLatch latch = new CountDownLatch(3);
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            new CreateTraceEntry().traceEntryMarker();
                            latch.countDown();
                        }
                    });
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            new CreateTraceEntry().traceEntryMarker();
                            latch.countDown();
                        }
                    });
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            new CreateTraceEntry().traceEntryMarker();
                            latch.countDown();
                        }
                    });
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
            });
        }
    }

    public static class DoNestedSubmitCallable implements AppUnderTest, TransactionMarker {

        @Override
        public void executeApp() throws Exception {
            transactionMarker();
        }

        @Override
        public void transactionMarker() throws Exception {
            MoreExecutors.newDirectExecutorService().submit(new Callable<Void>() {
                @Override
                public Void call() throws InterruptedException {
                    ExecutorService executor = createExecutorService();
                    final CountDownLatch latch = new CountDownLatch(3);
                    executor.submit(new Callable<Void>() {
                        @Override
                        public Void call() {
                            new CreateTraceEntry().traceEntryMarker();
                            latch.countDown();
                            return null;
                        }
                    });
                    executor.submit(new Callable<Void>() {
                        @Override
                        public Void call() {
                            new CreateTraceEntry().traceEntryMarker();
                            latch.countDown();
                            return null;
                        }
                    });
                    executor.submit(new Callable<Void>() {
                        @Override
                        public Void call() {
                            new CreateTraceEntry().traceEntryMarker();
                            latch.countDown();
                            return null;
                        }
                    });
                    latch.await();
                    executor.shutdown();
                    executor.awaitTermination(10, SECONDS);
                    return null;
                }
            });
        }
    }

    public static class DoDelegatingExecutor implements AppUnderTest, TransactionMarker {

        @Override
        public void executeApp() throws Exception {
            transactionMarker();
        }

        @Override
        public void transactionMarker() throws Exception {
            ExecutorService executor = createExecutorService();
            DelegatingExecutor delegatingExecutor = new DelegatingExecutor(executor);
            final CountDownLatch latch = new CountDownLatch(3);
            delegatingExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    new CreateTraceEntry().traceEntryMarker();
                    latch.countDown();
                }
            });
            delegatingExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    new CreateTraceEntry().traceEntryMarker();
                    latch.countDown();
                }
            });
            delegatingExecutor.execute(new Runnable() {
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

    private static class RunnableAndCallableWork implements Runnable, Callable<Void> {

        @Override
        public Void call() {
            new CreateTraceEntry().traceEntryMarker();
            return null;
        }

        @Override
        public void run() {
            new CreateTraceEntry().traceEntryMarker();
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

    private static class DelegatingExecutor implements Executor {

        private final Executor executor;

        private DelegatingExecutor(Executor executor) {
            this.executor = executor;
        }

        @Override
        public void execute(Runnable command) {
            executor.execute(new DelegatingRunnable(command));
        }
    }

    private static class DelegatingRunnable implements Runnable {

        private final Runnable runnable;

        private DelegatingRunnable(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void run() {
            runnable.run();
        }
    }
}
