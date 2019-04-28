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

import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;
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
import static org.assertj.core.api.Assertions.assertThat;

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
        container.resetConfig();
    }

    @Test
    public void shouldCaptureThread() throws Exception {
        // when
        Trace trace = container.execute(DoExecuteThread.class);
        // then
        checkTrace(trace, false, false);
    }

    @Test
    public void shouldCaptureThreadWithName() throws Exception {
        // when
        Trace trace = container.execute(DoExecuteThreadWithName.class);
        // then
        checkTrace(trace, false, false);
    }

    @Test
    public void shouldCaptureThreadWithThreadGroup() throws Exception {
        // when
        Trace trace = container.execute(DoExecuteThreadWithThreadGroup.class);
        // then
        checkTrace(trace, false, false);
    }

    @Test
    public void shouldCaptureThreadWithThreadGroupAndName() throws Exception {
        // when
        Trace trace = container.execute(DoExecuteThreadWithThreadGroupAndName.class);
        // then
        checkTrace(trace, false, false);
    }

    @Test
    public void shouldCaptureThreadSubclassed() throws Exception {
        // when
        Trace trace = container.execute(DoExecuteThreadSubclassed.class);
        // then
        checkTrace(trace, false, false);
    }

    @Test
    public void shouldCaptureThreadSubSubclassed() throws Exception {
        // when
        Trace trace = container.execute(DoExecuteThreadSubSubclassed.class);
        // then
        checkTrace(trace, false, false);
    }

    @Test
    public void shouldCaptureThreadSubSubclassedWithRunnable() throws Exception {
        // when
        Trace trace = container.execute(DoExecuteThreadSubSubclassedWithRunnable.class);
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
