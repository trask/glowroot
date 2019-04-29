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
package org.glowroot.xyzzy.instrumentation.spring;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import org.glowroot.xyzzy.test.harness.AppUnderTest;
import org.glowroot.xyzzy.test.harness.Container;
import org.glowroot.xyzzy.test.harness.LocalSpan;
import org.glowroot.xyzzy.test.harness.ServerSpan;
import org.glowroot.xyzzy.test.harness.Span;
import org.glowroot.xyzzy.test.harness.TraceEntryMarker;
import org.glowroot.xyzzy.test.harness.impl.JavaagentContainer;

import static org.assertj.core.api.Assertions.assertThat;

public class AsyncRestControllerIT {

    private static Container container;

    @BeforeClass
    public static void setUp() throws Exception {
        // javaagent is required for Executor.execute() weaving
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
    public void shouldCaptureCallableAsyncController() throws Exception {
        shouldCaptureCallableAsyncController("", InvokeCallableAsyncController.class);
    }

    @Test
    public void shouldCaptureDeferredResultAsyncController() throws Exception {
        shouldCaptureDeferredResultAsyncController("", InvokeDeferredResultAsyncController.class);
    }

    @Test
    public void shouldCaptureCallableAsyncControllerWithContextPath() throws Exception {
        shouldCaptureCallableAsyncController("/zzz",
                InvokeCallableAsyncControllerWithContextPath.class);
    }

    @Test
    public void shouldCaptureDeferredResultAsyncControllerWithContextPath() throws Exception {
        shouldCaptureDeferredResultAsyncController("/zzz",
                InvokeDeferredResultAsyncControllerWithContextPath.class);
    }

    private void shouldCaptureCallableAsyncController(String contextPath,
            Class<? extends AppUnderTest> appUnderTestClass) throws Exception {
        // when
        ServerSpan serverSpan = container.execute(appUnderTestClass, "Web");

        // then
        assertThat(serverSpan.async()).isTrue();
        assertThat(serverSpan.transactionName()).isEqualTo(contextPath + "/rest-async");

        Iterator<Span> i = serverSpan.childSpans().iterator();

        LocalSpan localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo("spring controller: org.glowroot.xyzzy"
                + ".instrumentation.spring.AsyncRestControllerIT$CallableAsyncController.test()");

        List<Span> nestedSpans = localSpan.childSpans();
        assertThat(nestedSpans).hasSize(1);

        LocalSpan nestedLocalSpan = (LocalSpan) nestedSpans.get(0);
        assertThat(nestedLocalSpan.getMessage()).isEqualTo("trace entry marker / CreateTraceEntry");
        assertThat(nestedLocalSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo("trace entry marker / CreateTraceEntry");

        assertThat(i.hasNext()).isFalse();
    }

    private void shouldCaptureDeferredResultAsyncController(String contextPath,
            Class<? extends AppUnderTest> appUnderTestClass) throws Exception {
        // when
        ServerSpan serverSpan = container.execute(appUnderTestClass, "Web");

        // then
        assertThat(serverSpan.async()).isTrue();
        assertThat(serverSpan.transactionName()).isEqualTo(contextPath + "/rest-async2");

        Iterator<Span> i = serverSpan.childSpans().iterator();

        LocalSpan localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo("spring controller: org.glowroot.xyzzy"
                + ".instrumentation.spring.AsyncRestControllerIT$DeferredResultAsyncController"
                + ".test()");

        List<Span> nestedSpans = localSpan.childSpans();
        assertThat(nestedSpans).hasSize(1);

        LocalSpan nestedLocalSpan = (LocalSpan) nestedSpans.get(0);
        assertThat(nestedLocalSpan.getMessage()).isEqualTo("trace entry marker / CreateTraceEntry");
        assertThat(nestedLocalSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo("trace entry marker / CreateTraceEntry");

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo("trace entry marker / CreateTraceEntry");

        assertThat(i.hasNext()).isFalse();
    }

    public static class InvokeCallableAsyncController extends InvokeSpringControllerInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp1", "", "/rest-async");
        }
    }

    public static class InvokeDeferredResultAsyncController extends InvokeSpringControllerInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp1", "", "/rest-async2");
        }
    }

    public static class InvokeCallableAsyncControllerWithContextPath
            extends InvokeSpringControllerInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp1", "/zzz", "/rest-async");
        }
    }

    public static class InvokeDeferredResultAsyncControllerWithContextPath
            extends InvokeSpringControllerInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp1", "/zzz", "/rest-async2");
        }
    }

    @RestController
    public static class CallableAsyncController {

        @RequestMapping("rest-async")
        public @ResponseBody Callable<String> test() throws InterruptedException {
            new CreateTraceEntry().traceEntryMarker();
            return new Callable<String>() {
                @Override
                public String call() throws Exception {
                    new CreateTraceEntry().traceEntryMarker();
                    return "async world";
                }
            };
        }
    }

    @RestController
    public static class DeferredResultAsyncController {

        @RequestMapping("rest-async2")
        public @ResponseBody DeferredResult<String> test() throws InterruptedException {
            new CreateTraceEntry().traceEntryMarker();
            final DeferredResult<String> result = new DeferredResult<String>();
            final ExecutorService executor = Executors.newCachedThreadPool();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    new CreateTraceEntry().traceEntryMarker();
                    result.setResult("async2 world");
                    executor.shutdown();
                }
            });
            return result;
        }
    }

    private static class CreateTraceEntry implements TraceEntryMarker {
        @Override
        public void traceEntryMarker() {}
    }
}
