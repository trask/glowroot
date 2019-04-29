/*
 * Copyright 2018-2019 the original author or authors.
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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.xyzzy.test.harness.AppUnderTest;
import org.glowroot.xyzzy.test.harness.Container;
import org.glowroot.xyzzy.test.harness.ServerSpan;
import org.glowroot.xyzzy.test.harness.Span;
import org.glowroot.xyzzy.test.harness.TraceEntryMarker;
import org.glowroot.xyzzy.test.harness.TransactionMarker;
import org.glowroot.xyzzy.test.harness.impl.JavaagentContainer;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.glowroot.xyzzy.test.harness.util.HarnessAssertions.assertSingleLocalSpanMessage;

public class ThreadIT {

    private static Container container;

    @BeforeClass
    public static void setUp() throws Exception {
        // tests only work with javaagent container because they need to weave java.lang.Thread
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
    public void shouldCaptureThread() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(DoExecuteThread.class);

        // then
        checkTrace(serverSpan, false, false);
    }

    @Test
    public void shouldCaptureThreadWithName() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(DoExecuteThreadWithName.class);

        // then
        checkTrace(serverSpan, false, false);
    }

    @Test
    public void shouldCaptureThreadWithThreadGroup() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(DoExecuteThreadWithThreadGroup.class);

        // then
        checkTrace(serverSpan, false, false);
    }

    @Test
    public void shouldCaptureThreadWithThreadGroupAndName() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(DoExecuteThreadWithThreadGroupAndName.class);

        // then
        checkTrace(serverSpan, false, false);
    }

    @Test
    public void shouldCaptureThreadSubclassed() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(DoExecuteThreadSubclassed.class);

        // then
        checkTrace(serverSpan, false, false);
    }

    @Test
    public void shouldCaptureThreadSubSubclassed() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(DoExecuteThreadSubSubclassed.class);

        // then
        checkTrace(serverSpan, false, false);
    }

    @Test
    public void shouldCaptureThreadSubSubclassedWithRunnable() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(DoExecuteThreadSubSubclassedWithRunnable.class);

        // then
        checkTrace(serverSpan, false, false);
    }

    private static void checkTrace(ServerSpan serverSpan, boolean isAny, boolean withFuture) {
        if (withFuture) {
            assertThat(serverSpan.mainThreadRootTimer().childTimers().size()).isEqualTo(1);
            assertThat(serverSpan.mainThreadRootTimer().childTimers().get(0).name())
                    .isEqualTo("wait on future");
            assertThat(serverSpan.mainThreadRootTimer().childTimers().get(0).count())
                    .isGreaterThanOrEqualTo(1);
            assertThat(serverSpan.mainThreadRootTimer().childTimers().get(0).count())
                    .isLessThanOrEqualTo(3);
        }
        assertThat(serverSpan.auxThreadRootTimer()).isNotNull();
        assertThat(serverSpan.asyncTimers()).isEmpty();
        assertThat(serverSpan.auxThreadRootTimer().name()).isEqualTo("auxiliary thread");
        if (isAny) {
            assertThat(serverSpan.auxThreadRootTimer().count()).isBetween(1L, 3L);
            // should be 100-300ms, but margin of error, esp. in travis builds is high
            assertThat(serverSpan.auxThreadRootTimer().totalNanos())
                    .isGreaterThanOrEqualTo(MILLISECONDS.toNanos(50));
        } else {
            assertThat(serverSpan.auxThreadRootTimer().count()).isEqualTo(3);
            // should be 300ms, but margin of error, esp. in travis builds is high
            assertThat(serverSpan.auxThreadRootTimer().totalNanos())
                    .isGreaterThanOrEqualTo(MILLISECONDS.toNanos(250));
        }
        assertThat(serverSpan.auxThreadRootTimer().childTimers().size()).isEqualTo(1);
        assertThat(serverSpan.auxThreadRootTimer().childTimers().get(0).name())
                .isEqualTo("mock trace entry marker");

        List<Span> spans = serverSpan.childSpans();

        if (isAny) {
            assertThat(spans.size()).isBetween(1, 3);
        } else {
            assertThat(spans).hasSize(3);
        }
        for (Span span : spans) {
            assertSingleLocalSpanMessage(span).isEqualTo("trace entry marker / CreateTraceEntry");
        }
    }

    public static class DoExecuteThread implements AppUnderTest, TransactionMarker {

        @Override
        public void executeApp() throws Exception {
            transactionMarker();
        }

        @Override
        public void transactionMarker() throws Exception {
            Thread thread1 = new Thread(new Runnable() {
                @Override
                public void run() {
                    new CreateTraceEntry().traceEntryMarker();
                }
            });
            Thread thread2 = new Thread(new Runnable() {
                @Override
                public void run() {
                    new CreateTraceEntry().traceEntryMarker();
                }
            });
            Thread thread3 = new Thread(new Runnable() {
                @Override
                public void run() {
                    new CreateTraceEntry().traceEntryMarker();
                }
            });
            thread1.start();
            thread2.start();
            thread3.start();
            thread1.join();
            thread2.join();
            thread3.join();
        }
    }

    public static class DoExecuteThreadWithName implements AppUnderTest, TransactionMarker {

        @Override
        public void executeApp() throws Exception {
            transactionMarker();
        }

        @Override
        public void transactionMarker() throws Exception {
            Thread thread1 = new Thread(new Runnable() {
                @Override
                public void run() {
                    new CreateTraceEntry().traceEntryMarker();
                }
            }, "one");
            Thread thread2 = new Thread(new Runnable() {
                @Override
                public void run() {
                    new CreateTraceEntry().traceEntryMarker();
                }
            }, "two");
            Thread thread3 = new Thread(new Runnable() {
                @Override
                public void run() {
                    new CreateTraceEntry().traceEntryMarker();
                }
            }, "three");
            thread1.start();
            thread2.start();
            thread3.start();
            thread1.join();
            thread2.join();
            thread3.join();
        }
    }

    public static class DoExecuteThreadWithThreadGroup implements AppUnderTest, TransactionMarker {

        @Override
        public void executeApp() throws Exception {
            transactionMarker();
        }

        @Override
        public void transactionMarker() throws Exception {
            Thread thread1 = new Thread(Thread.currentThread().getThreadGroup(), new Runnable() {
                @Override
                public void run() {
                    new CreateTraceEntry().traceEntryMarker();
                }
            });
            Thread thread2 = new Thread(Thread.currentThread().getThreadGroup(), new Runnable() {
                @Override
                public void run() {
                    new CreateTraceEntry().traceEntryMarker();
                }
            });
            Thread thread3 = new Thread(Thread.currentThread().getThreadGroup(), new Runnable() {
                @Override
                public void run() {
                    new CreateTraceEntry().traceEntryMarker();
                }
            });
            thread1.start();
            thread2.start();
            thread3.start();
            thread1.join();
            thread2.join();
            thread3.join();
        }
    }

    public static class DoExecuteThreadWithThreadGroupAndName
            implements AppUnderTest, TransactionMarker {

        @Override
        public void executeApp() throws Exception {
            transactionMarker();
        }

        @Override
        public void transactionMarker() throws Exception {
            Thread thread1 = new Thread(Thread.currentThread().getThreadGroup(), new Runnable() {
                @Override
                public void run() {
                    new CreateTraceEntry().traceEntryMarker();
                }
            }, "one");
            Thread thread2 = new Thread(Thread.currentThread().getThreadGroup(), new Runnable() {
                @Override
                public void run() {
                    new CreateTraceEntry().traceEntryMarker();
                }
            }, "two");
            Thread thread3 = new Thread(Thread.currentThread().getThreadGroup(), new Runnable() {
                @Override
                public void run() {
                    new CreateTraceEntry().traceEntryMarker();
                }
            }, "three");
            thread1.start();
            thread2.start();
            thread3.start();
            thread1.join();
            thread2.join();
            thread3.join();
        }
    }

    public static class DoExecuteThreadSubclassed implements AppUnderTest, TransactionMarker {

        @Override
        public void executeApp() throws Exception {
            transactionMarker();
        }

        @Override
        public void transactionMarker() throws Exception {
            Thread thread1 = new Thread() {
                @Override
                public void run() {
                    new CreateTraceEntry().traceEntryMarker();
                }
            };
            Thread thread2 = new Thread() {
                @Override
                public void run() {
                    new CreateTraceEntry().traceEntryMarker();
                }
            };
            Thread thread3 = new Thread() {
                @Override
                public void run() {
                    new CreateTraceEntry().traceEntryMarker();
                }
            };
            thread1.start();
            thread2.start();
            thread3.start();
            thread1.join();
            thread2.join();
            thread3.join();
        }
    }

    public static class DoExecuteThreadSubSubclassed implements AppUnderTest, TransactionMarker {

        @Override
        public void executeApp() throws Exception {
            transactionMarker();
        }

        @Override
        public void transactionMarker() throws Exception {
            Thread thread1 = new TraceEntryMarkerThread() {
                @Override
                public void run() {
                    new CreateTraceEntry().traceEntryMarker();
                }
            };
            Thread thread2 = new TraceEntryMarkerThread() {
                @Override
                public void run() {
                    new CreateTraceEntry().traceEntryMarker();
                }
            };
            Thread thread3 = new TraceEntryMarkerThread() {
                @Override
                public void run() {
                    new CreateTraceEntry().traceEntryMarker();
                }
            };
            thread1.start();
            thread2.start();
            thread3.start();
            thread1.join();
            thread2.join();
            thread3.join();
        }
    }

    public static class DoExecuteThreadSubSubclassedWithRunnable
            implements AppUnderTest, TransactionMarker {

        @Override
        public void executeApp() throws Exception {
            transactionMarker();
        }

        @Override
        public void transactionMarker() throws Exception {
            Thread thread1 = new TraceEntryMarkerThreadWithRunnable() {
                @Override
                public void run() {
                    new CreateTraceEntry().traceEntryMarker();
                }
            };
            Thread thread2 = new TraceEntryMarkerThreadWithRunnable() {
                @Override
                public void run() {
                    new CreateTraceEntry().traceEntryMarker();
                }
            };
            Thread thread3 = new TraceEntryMarkerThreadWithRunnable() {
                @Override
                public void run() {
                    new CreateTraceEntry().traceEntryMarker();
                }
            };
            thread1.start();
            thread2.start();
            thread3.start();
            thread1.join();
            thread2.join();
            thread3.join();
        }
    }

    private static class TraceEntryMarkerThread extends Thread {}

    private static class TraceEntryMarkerThreadWithRunnable extends Thread implements Runnable {}

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
