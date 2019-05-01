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

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.xyzzy.test.harness.AppUnderTest;
import org.glowroot.xyzzy.test.harness.Container;
import org.glowroot.xyzzy.test.harness.IncomingSpan;
import org.glowroot.xyzzy.test.harness.TestSpans;
import org.glowroot.xyzzy.test.harness.TransactionMarker;
import org.glowroot.xyzzy.test.harness.impl.JavaagentContainer;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.glowroot.xyzzy.test.harness.util.HarnessAssertions.assertSingleLocalSpanMessage;

public class LotsOfNestedAuxThreadContextsIT {

    private static Container container;

    @BeforeClass
    public static void setUp() throws Exception {
        // tests only work with javaagent container because they need to weave bootstrap classes
        // that implement Executor and ExecutorService
        //
        // restrict stack size to test for StackOverflowError
        // restrict heap size to test for OOM when lots of nested auxiliary thread contexts
        container =
                JavaagentContainer.createWithExtraJvmArgs(ImmutableList.of("-Xss256k", "-Xmx32m"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        container.close();
    }

    @After
    public void afterEachTest() throws Exception {
        container.resetAfterEachTest();
    }

    @Test
    public void shouldCaptureSubmitCallable() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(DoSubmitCallable.class);

        // then
        assertThat(incomingSpan.auxThreadRootTimer()).isNotNull();
        IncomingSpan.Timer auxThreadRootTimer = incomingSpan.auxThreadRootTimer();
        assertThat(auxThreadRootTimer.count()).isEqualTo(100000);
        assertThat(auxThreadRootTimer.childTimers().size()).isEqualTo(1);
        assertThat(auxThreadRootTimer.childTimers().get(0).name())
                .isEqualTo("test local span");

        assertSingleLocalSpanMessage(incomingSpan).isEqualTo("test local span");
    }

    public static class DoSubmitCallable implements AppUnderTest, TransactionMarker {

        private final ThreadPoolExecutor executor = new ThreadPoolExecutor(100, 100, 0,
                MILLISECONDS, new LinkedBlockingQueue<Runnable>());

        private final CountDownLatch latch = new CountDownLatch(1);

        @Override
        public void executeApp(Serializable... args) throws Exception {
            transactionMarker();
        }

        @Override
        public void transactionMarker() throws Exception {
            passOnToAnotherThread(0);
            latch.await();
            executor.shutdown();
            executor.awaitTermination(10, SECONDS);
        }

        private void passOnToAnotherThread(final int depth) throws Exception {
            if (depth == 100000) {
                TestSpans.createLocalSpan(100);
                latch.countDown();
                return;
            }
            while (executor.getQueue().size() > 1000) {
                // keep executor backlog from getting too full and adding memory pressure
                // (since restricting heap size to test for leaking aux thread contexts)
                MILLISECONDS.sleep(1);
            }
            executor.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    passOnToAnotherThread(depth + 1);
                    return null;
                }
            });
        }
    }
}
