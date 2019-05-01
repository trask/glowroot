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
package org.glowroot.xyzzy.instrumentation.jaxrs;

import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.xyzzy.test.harness.AppUnderTest;
import org.glowroot.xyzzy.test.harness.Container;
import org.glowroot.xyzzy.test.harness.IncomingSpan;
import org.glowroot.xyzzy.test.harness.LocalSpan;
import org.glowroot.xyzzy.test.harness.Span;
import org.glowroot.xyzzy.test.harness.TestSpans;
import org.glowroot.xyzzy.test.harness.impl.JavaagentContainer;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;

public class SuspendedResourceIT {

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
        container.resetAfterEachTest();
    }

    @Test
    public void shouldCaptureSuspendedResponse() throws Exception {
        shouldCaptureSuspendedResponse("", WithNormalServletMappingCallSuspended.class);
    }

    @Test
    public void shouldCaptureSuspendedResponseWithContextPath() throws Exception {
        shouldCaptureSuspendedResponse("/zzz",
                WithNormalServletMappingCallSuspendedWithContextPath.class);
    }

    private void shouldCaptureSuspendedResponse(String contextPath,
            Class<? extends AppUnderTest> appUnderTestClass) throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(appUnderTestClass, "Web");

        // then
        assertThat(incomingSpan.transactionName()).isEqualTo("GET " + contextPath + "/suspended/*");

        Iterator<Span> i = incomingSpan.childSpans().iterator();

        LocalSpan localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage())
                .isEqualTo("jaxrs resource: org.glowroot.xyzzy.instrumentation"
                        + ".jaxrs.SuspendedResourceIT$SuspendedResource.log()");
        assertThat(localSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo("test local span");
        assertThat(localSpan.childSpans()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    public static class WithNormalServletMappingCallSuspended extends InvokeJaxrsResourceInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp1", "", "/suspended/1");
        }
    }

    public static class WithNormalServletMappingCallSuspendedWithContextPath
            extends InvokeJaxrsResourceInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp1", "/zzz", "/suspended/1");
        }
    }

    @Path("suspended")
    public static class SuspendedResource {

        private final ExecutorService executor = Executors.newCachedThreadPool();

        @GET
        @Path("{param}")
        public void log(@Suspended final AsyncResponse asyncResponse) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        MILLISECONDS.sleep(200);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                    TestSpans.createLocalSpan();
                    asyncResponse.resume("hido");
                    executor.shutdown();
                }
            });
        }
    }
}
