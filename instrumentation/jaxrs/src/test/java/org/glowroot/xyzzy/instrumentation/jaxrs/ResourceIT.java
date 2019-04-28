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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.agent.it.harness.AppUnderTest;
import org.glowroot.agent.it.harness.Container;
import org.glowroot.agent.it.harness.Containers;
import org.glowroot.agent.it.harness.model.ServerSpan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.glowroot.agent.it.harness.validation.HarnessAssertions.assertSingleLocalSpanMessage;

public class ResourceIT {

    private static Container container;

    @BeforeClass
    public static void setUp() throws Exception {
        container = Containers.create();
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
    public void shouldCaptureTransactionNameWithSimpleServletMapping() throws Exception {
        shouldCaptureTransactionNameWithSimpleServletMapping("", WithSimpleServletMapping.class);
    }

    @Test
    public void shouldCaptureTransactionNameWithSimpleServletMappingWithContextPath()
            throws Exception {
        shouldCaptureTransactionNameWithSimpleServletMapping("/zzz",
                WithSimpleServletMappingWithContextPath.class);
    }

    @Test
    public void shouldCaptureTransactionNameWithNormalServletMapping() throws Exception {
        shouldCaptureTransactionNameWithNormalServletMapping("", WithNormalServletMapping.class);
    }

    @Test
    public void shouldCaptureTransactionNameWithNormalServletMappingWithContextPath()
            throws Exception {
        shouldCaptureTransactionNameWithNormalServletMapping("/zzz",
                WithNormalServletMappingWithContextPath.class);
    }

    @Test
    public void shouldCaptureTransactionNameWithNormalServletMappingHittingRoot() throws Exception {
        shouldCaptureTransactionNameWithNormalServletMappingHittingRoot("",
                WithNormalServletMappingHittingRoot.class);
    }

    @Test
    public void shouldCaptureTransactionNameWithNormalServletMappingHittingRootWithContextPath()
            throws Exception {
        shouldCaptureTransactionNameWithNormalServletMappingHittingRoot("/zzz",
                WithNormalServletMappingHittingRootWithContextPath.class);
    }

    @Test
    public void shouldCaptureTransactionNameWithNestedServletMapping() throws Exception {
        shouldCaptureTransactionNameWithNestedServletMapping("", WithNestedServletMapping.class);
    }

    @Test
    public void shouldCaptureTransactionNameWithNestedServletMappingWithContextPath()
            throws Exception {
        shouldCaptureTransactionNameWithNestedServletMapping("/zzz",
                WithNestedServletMappingWithContextPath.class);
    }

    @Test
    public void shouldCaptureTransactionNameWithNestedServletMappingHittingRoot() throws Exception {
        shouldCaptureTransactionNameWithNestedServletMappingHittingRoot("",
                WithNestedServletMappingHittingRoot.class);
    }

    @Test
    public void shouldCaptureTransactionNameWithNestedServletMappingHittingRootWithContextPath()
            throws Exception {
        shouldCaptureTransactionNameWithNestedServletMappingHittingRoot("/zzz",
                WithNestedServletMappingHittingRootWithContextPath.class);
    }

    @Test
    public void shouldCaptureTransactionNameWithLessNormalServletMapping() throws Exception {
        shouldCaptureTransactionNameWithLessNormalServletMapping("",
                WithLessNormalServletMapping.class);
    }

    @Test
    public void shouldCaptureTransactionNameWithLessNormalServletMappingWithContextPath()
            throws Exception {
        shouldCaptureTransactionNameWithLessNormalServletMapping("/zzz",
                WithLessNormalServletMappingWithContextPath.class);
    }

    @Test
    public void shouldCaptureTransactionNameWithLessNormalServletMappingHittingRoot()
            throws Exception {
        shouldCaptureTransactionNameWithLessNormalServletMappingHittingRoot("",
                WithLessNormalServletMappingHittingRoot.class);
    }

    @Test
    public void shouldCaptureTransactionNameWithLessNormalServletMappingHittingRootWithContextPath()
            throws Exception {
        shouldCaptureTransactionNameWithLessNormalServletMappingHittingRoot("/zzz",
                WithLessNormalServletMappingHittingRootWithContextPath.class);
    }

    @Test
    public void shouldCaptureAltTransactionName() throws Exception {
        // given
        container.setInstrumentationProperty("jaxrs", "useAltTransactionNaming", true);

        // when
        ServerSpan serverSpan = container.execute(WithNormalServletMapping.class, "Web");

        // then
        assertThat(serverSpan.transactionName()).isEqualTo("HelloResource#echo");

        assertSingleLocalSpanMessage(serverSpan).isEqualTo("jaxrs resource:"
                + " org.glowroot.xyzzy.instrumentation.jaxrs.ResourceIT$HelloResource.echo()");
    }

    private void shouldCaptureTransactionNameWithSimpleServletMapping(String contextPath,
            Class<? extends AppUnderTest> appUnderTestClass) throws Exception {
        // when
        ServerSpan serverSpan = container.execute(appUnderTestClass, "Web");

        // then
        assertThat(serverSpan.transactionName()).isEqualTo("GET " + contextPath + "/simple");

        assertSingleLocalSpanMessage(serverSpan).isEqualTo("jaxrs resource:"
                + " org.glowroot.xyzzy.instrumentation.jaxrs.ResourceIT$SimpleResource.echo()");
    }

    private void shouldCaptureTransactionNameWithNormalServletMapping(String contextPath,
            Class<? extends AppUnderTest> appUnderTestClass) throws Exception {
        // when
        ServerSpan serverSpan = container.execute(appUnderTestClass, "Web");

        // then
        assertThat(serverSpan.transactionName()).isEqualTo("GET " + contextPath + "/hello/*");

        assertSingleLocalSpanMessage(serverSpan).isEqualTo("jaxrs resource:"
                + " org.glowroot.xyzzy.instrumentation.jaxrs.ResourceIT$HelloResource.echo()");
    }

    private void shouldCaptureTransactionNameWithNormalServletMappingHittingRoot(String contextPath,
            Class<? extends AppUnderTest> appUnderTestClass) throws Exception {
        // when
        ServerSpan serverSpan = container.execute(appUnderTestClass, "Web");

        // then
        assertThat(serverSpan.transactionName()).isEqualTo("GET " + contextPath + "/");

        assertSingleLocalSpanMessage(serverSpan).isEqualTo("jaxrs resource:"
                + " org.glowroot.xyzzy.instrumentation.jaxrs.ResourceIT$RootResource.echo()");
    }

    private void shouldCaptureTransactionNameWithNestedServletMapping(String contextPath,
            Class<? extends AppUnderTest> appUnderTestClass) throws Exception {
        // when
        ServerSpan serverSpan = container.execute(appUnderTestClass, "Web");

        // then
        assertThat(serverSpan.transactionName()).isEqualTo("GET " + contextPath + "/rest/hello/*");

        assertSingleLocalSpanMessage(serverSpan).isEqualTo("jaxrs resource:"
                + " org.glowroot.xyzzy.instrumentation.jaxrs.ResourceIT$HelloResource.echo()");
    }

    private void shouldCaptureTransactionNameWithNestedServletMappingHittingRoot(String contextPath,
            Class<? extends AppUnderTest> appUnderTestClass) throws Exception {
        // when
        ServerSpan serverSpan = container.execute(appUnderTestClass, "Web");

        // then
        assertThat(serverSpan.transactionName()).isEqualTo("GET " + contextPath + "/rest/");

        assertSingleLocalSpanMessage(serverSpan).isEqualTo("jaxrs resource:"
                + " org.glowroot.xyzzy.instrumentation.jaxrs.ResourceIT$RootResource.echo()");
    }

    private void shouldCaptureTransactionNameWithLessNormalServletMapping(String contextPath,
            Class<? extends AppUnderTest> appUnderTestClass) throws Exception {
        // when
        ServerSpan serverSpan = container.execute(appUnderTestClass, "Web");

        // then
        if (!serverSpan.transactionName().equals("GET " + contextPath + "/hello/*")) {
            // Jersey (2.5 and above) doesn't like this "less than normal" servlet mapping, and ends
            // up mapping everything to RootResource
            assertThat(serverSpan.transactionName())
                    .isEqualTo("GET " + contextPath + "/");
        }

        assertSingleLocalSpanMessage(serverSpan).isEqualTo("jaxrs resource:"
                + " org.glowroot.xyzzy.instrumentation.jaxrs.ResourceIT$RootResource.echo()");
    }

    private void shouldCaptureTransactionNameWithLessNormalServletMappingHittingRoot(
            String contextPath, Class<? extends AppUnderTest> appUnderTestClass) throws Exception {
        // when
        ServerSpan serverSpan = container.execute(appUnderTestClass, "Web");

        // then
        assertThat(serverSpan.transactionName()).isEqualTo("GET " + contextPath + "/");

        assertSingleLocalSpanMessage(serverSpan).isEqualTo("jaxrs resource:"
                + " org.glowroot.xyzzy.instrumentation.jaxrs.ResourceIT$RootResource.echo()");
    }

    @Test
    public void shouldCaptureWhenInterfaceAnnotated() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(WithInterfaceAnnotated.class, "Web");

        // then
        assertThat(serverSpan.transactionName()).isEqualTo("GET /another/*");

        assertSingleLocalSpanMessage(serverSpan).isEqualTo("jaxrs resource:"
                + " org.glowroot.xyzzy.instrumentation.jaxrs.ResourceIT$AnotherResourceImpl.echo()");
    }

    @Test
    public void shouldCaptureSubResource() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(WithSubResource.class, "Web");

        // then
        assertThat(serverSpan.transactionName()).isEqualTo("GET /parent/child/grandchild/*");

        assertSingleLocalSpanMessage(serverSpan).isEqualTo("jaxrs resource:"
                + " org.glowroot.xyzzy.instrumentation.jaxrs.ResourceIT$GrandchildResourceImpl"
                + ".echo()");
    }

    public static class WithSimpleServletMapping extends InvokeJaxrsResourceInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp1", "", "/simple");
        }
    }

    public static class WithSimpleServletMappingWithContextPath
            extends InvokeJaxrsResourceInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp1", "/zzz", "/simple");
        }
    }

    public static class WithNormalServletMapping extends InvokeJaxrsResourceInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp1", "", "/hello/1");
        }
    }

    public static class WithNormalServletMappingWithContextPath
            extends InvokeJaxrsResourceInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp1", "/zzz", "/hello/1");
        }
    }

    public static class WithNormalServletMappingHittingRoot extends InvokeJaxrsResourceInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp1", "", "/");
        }
    }

    public static class WithNormalServletMappingHittingRootWithContextPath
            extends InvokeJaxrsResourceInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp1", "/zzz", "/");
        }
    }

    public static class WithNestedServletMapping extends InvokeJaxrsResourceInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp2", "", "/rest/hello/1");
        }
    }

    public static class WithNestedServletMappingWithContextPath
            extends InvokeJaxrsResourceInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp2", "/zzz", "/rest/hello/1");
        }
    }

    public static class WithNestedServletMappingHittingRoot extends InvokeJaxrsResourceInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp2", "", "/rest/");
        }
    }

    public static class WithNestedServletMappingHittingRootWithContextPath
            extends InvokeJaxrsResourceInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp2", "/zzz", "/rest/");
        }
    }

    public static class WithLessNormalServletMapping extends InvokeJaxrsResourceInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp3", "", "/hello/1");
        }
    }

    public static class WithLessNormalServletMappingWithContextPath
            extends InvokeJaxrsResourceInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp3", "/zzz", "/hello/1");
        }
    }

    public static class WithLessNormalServletMappingHittingRoot
            extends InvokeJaxrsResourceInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp3", "", "/");
        }
    }

    public static class WithLessNormalServletMappingHittingRootWithContextPath
            extends InvokeJaxrsResourceInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp3", "/zzz", "/");
        }
    }

    public static class WithInterfaceAnnotated extends InvokeJaxrsResourceInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp1", "", "/another/1");
        }
    }

    public static class WithSubResource extends InvokeJaxrsResourceInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp1", "", "/parent/child/grandchild/1");
        }
    }

    @Path("simple")
    public static class SimpleResource {
        @GET
        public Response echo() {
            return Response.status(200).entity("hi").build();
        }
    }

    @Path("hello")
    public static class HelloResource {
        @GET
        @Path("{param}")
        public Response echo(@PathParam("param") String msg) {
            return Response.status(200).entity(msg).build();
        }
    }

    @Path("/")
    public static class RootResource {
        @GET
        public Response echo() {
            return Response.status(200).build();
        }
    }

    @Path("another")
    public static class AnotherResourceImpl implements AnotherResource {
        private final AnotherResource delegate = new DelegateAnotherResource();
        @Override
        public Response echo(String msg) {
            return delegate.echo(msg);
        }
    }

    @Path("parent")
    public static class ParentResourceImpl implements ParentResource {
        private final ParentResource delegate = new DelegateParentResource();
        @Override
        public ChildResource getChildResource() {
            return delegate.getChildResource();
        }
    }

    public static class DelegateAnotherResource implements AnotherResource {
        @Override
        public Response echo(String msg) {
            return Response.status(200).entity(msg).build();
        }
    }

    public static class DelegateParentResource implements ParentResource {
        @Override
        public ChildResource getChildResource() {
            return new ChildResourceImpl();
        }
    }

    public static class ChildResourceImpl implements ChildResource {
        @Override
        public GrandchildResource getGrandchildResource() {
            return new GrandchildResourceImpl();
        }
    }

    public static class GrandchildResourceImpl implements GrandchildResource {
        @Override
        public Response echo(String msg) {
            return Response.status(200).entity(msg).build();
        }
    }

    public interface AnotherResource {
        @GET
        @Path("{param}")
        Response echo(@PathParam("param") String msg);
    }

    public interface ParentResource {
        @Path("child")
        ChildResource getChildResource();
    }

    public interface ChildResource {
        @Path("grandchild")
        GrandchildResource getGrandchildResource();
    }

    public interface GrandchildResource {
        @GET
        @Path("{param}")
        Response echo(@PathParam("param") String msg);
    }
}
