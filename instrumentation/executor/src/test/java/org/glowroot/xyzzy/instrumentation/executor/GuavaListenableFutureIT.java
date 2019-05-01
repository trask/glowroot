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
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.xyzzy.test.harness.AppUnderTest;
import org.glowroot.xyzzy.test.harness.Container;
import org.glowroot.xyzzy.test.harness.Containers;
import org.glowroot.xyzzy.test.harness.IncomingSpan;
import org.glowroot.xyzzy.test.harness.TestSpans;
import org.glowroot.xyzzy.test.harness.TransactionMarker;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.glowroot.xyzzy.test.harness.util.HarnessAssertions.assertSingleLocalSpanMessage;

public class GuavaListenableFutureIT {

    private static Container container;

    @BeforeClass
    public static void setUp() throws Exception {
        // unshaded doesn't work under javaagent because glowroot loads guava classes before the
        // Weaver is registered, so the guava classes don't have a chance to get woven
        Assume.assumeTrue(isShaded() || !Containers.useJavaagent());
        container = Containers.create();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // need null check in case assumption is false in setUp()
        if (container != null) {
            container.close();
        }
    }

    @After
    public void afterEachTest() throws Exception {
        container.resetAfterEachTest();
    }

    @Test
    public void shouldCaptureListenerAddedBeforeComplete() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(AddListenerBeforeComplete.class);

        // then
        assertSingleLocalSpanMessage(incomingSpan).isEqualTo("test local span");
    }

    @Test
    public void shouldCaptureListenerAddedAfterComplete() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(AddListenerAfterComplete.class);

        // then
        assertSingleLocalSpanMessage(incomingSpan).isEqualTo("test local span");
    }

    @Test
    public void shouldCaptureSameExecutorListenerAddedBeforeComplete() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(AddSameExecutorListenerBeforeComplete.class);

        // then
        assertSingleLocalSpanMessage(incomingSpan).isEqualTo("test local span");
    }

    @Test
    public void shouldCaptureSameExecutorListenerAddedAfterComplete() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(AddSameExecutorListenerAfterComplete.class);

        // then
        assertSingleLocalSpanMessage(incomingSpan).isEqualTo("test local span");
    }

    public static class AddListenerBeforeComplete implements AppUnderTest, TransactionMarker {

        @Override
        public void executeApp(Serializable... args) throws Exception {
            transactionMarker();
        }

        @Override
        public void transactionMarker() throws Exception {
            ListeningExecutorService executor =
                    MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
            ListenableFuture<Void> future1 = executor.submit(new Callable<Void>() {
                @Override
                public Void call() throws InterruptedException {
                    MILLISECONDS.sleep(100);
                    return null;
                }
            });
            future1.addListener(new Runnable() {
                @Override
                public void run() {
                    TestSpans.createLocalSpan();
                }
            }, executor);
            MILLISECONDS.sleep(200);
            executor.shutdown();
            executor.awaitTermination(10, SECONDS);
        }
    }

    public static class AddListenerAfterComplete implements AppUnderTest, TransactionMarker {

        @Override
        public void executeApp(Serializable... args) throws Exception {
            transactionMarker();
        }

        @Override
        public void transactionMarker() throws Exception {
            ListeningExecutorService executor =
                    MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
            ListenableFuture<Void> future1 = executor.submit(new Callable<Void>() {
                @Override
                public Void call() {
                    return null;
                }
            });
            MILLISECONDS.sleep(100);
            future1.addListener(new Runnable() {
                @Override
                public void run() {
                    TestSpans.createLocalSpan();
                }
            }, executor);
            MILLISECONDS.sleep(100);
            executor.shutdown();
            executor.awaitTermination(10, SECONDS);
        }
    }

    public static class AddSameExecutorListenerBeforeComplete
            implements AppUnderTest, TransactionMarker {

        @Override
        public void executeApp(Serializable... args) throws Exception {
            transactionMarker();
        }

        @Override
        public void transactionMarker() throws Exception {
            ListeningExecutorService executor =
                    MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
            ListenableFuture<Void> future1 = executor.submit(new Callable<Void>() {
                @Override
                public Void call() throws InterruptedException {
                    MILLISECONDS.sleep(100);
                    return null;
                }
            });
            future1.addListener(new Runnable() {
                @Override
                public void run() {
                    TestSpans.createLocalSpan();
                }
            }, executor);
            MILLISECONDS.sleep(200);
            executor.shutdown();
            executor.awaitTermination(10, SECONDS);
        }
    }

    public static class AddSameExecutorListenerAfterComplete
            implements AppUnderTest, TransactionMarker {

        @Override
        public void executeApp(Serializable... args) throws Exception {
            transactionMarker();
        }

        @Override
        public void transactionMarker() throws Exception {
            ListeningExecutorService executor =
                    MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
            ListenableFuture<Void> future1 = executor.submit(new Callable<Void>() {
                @Override
                public Void call() {
                    return null;
                }
            });
            MILLISECONDS.sleep(100);
            future1.addListener(new Runnable() {
                @Override
                public void run() {
                    TestSpans.createLocalSpan();
                }
            }, executor);
            MILLISECONDS.sleep(100);
            executor.shutdown();
            executor.awaitTermination(10, SECONDS);
        }
    }

    static boolean isShaded() {
        try {
            Class.forName("org.glowroot.agent.shaded.org.slf4j.Logger");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
