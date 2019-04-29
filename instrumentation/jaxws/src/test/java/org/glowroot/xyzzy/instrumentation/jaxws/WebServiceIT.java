/*
 * Copyright 2017-2019 the original author or authors.
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
package org.glowroot.xyzzy.instrumentation.jaxws;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.xyzzy.test.harness.AppUnderTest;
import org.glowroot.xyzzy.test.harness.Container;
import org.glowroot.xyzzy.test.harness.Containers;
import org.glowroot.xyzzy.test.harness.ServerSpan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.glowroot.xyzzy.test.harness.util.HarnessAssertions.assertSingleLocalSpanMessage;

public class WebServiceIT {

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
        container.resetInstrumentationConfig();
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
        container.setInstrumentationProperty("jaxws", "useAltTransactionNaming", true);

        // when
        ServerSpan serverSpan = container.execute(WithNormalServletMapping.class, "Web");

        // then
        assertThat(serverSpan.transactionName()).isEqualTo("HelloService#echo");

        assertSingleLocalSpanMessage(serverSpan).isEqualTo("jaxws service:"
                + " org.glowroot.xyzzy.instrumentation.jaxws.WebServiceIT$HelloService.echo()");
    }

    private void shouldCaptureTransactionNameWithNormalServletMapping(String contextPath,
            Class<? extends AppUnderTest> appUnderTestClass) throws Exception {
        // when
        ServerSpan serverSpan = container.execute(appUnderTestClass, "Web");

        // then
        assertThat(serverSpan.transactionName()).isEqualTo("POST " + contextPath + "/hello#echo");

        assertSingleLocalSpanMessage(serverSpan).isEqualTo("jaxws service:"
                + " org.glowroot.xyzzy.instrumentation.jaxws.WebServiceIT$HelloService.echo()");
    }

    private void shouldCaptureTransactionNameWithNormalServletMappingHittingRoot(String contextPath,
            Class<? extends AppUnderTest> appUnderTestClass) throws Exception {
        // when
        ServerSpan serverSpan = container.execute(appUnderTestClass, "Web");

        // then
        assertThat(serverSpan.transactionName()).isEqualTo("POST " + contextPath + "/#echo");

        assertSingleLocalSpanMessage(serverSpan).isEqualTo("jaxws service:"
                + " org.glowroot.xyzzy.instrumentation.jaxws.WebServiceIT$RootService.echo()");
    }

    private void shouldCaptureTransactionNameWithNestedServletMapping(String contextPath,
            Class<? extends AppUnderTest> appUnderTestClass) throws Exception {
        // when
        ServerSpan serverSpan = container.execute(appUnderTestClass, "Web");

        // then
        assertThat(serverSpan.transactionName())
                .isEqualTo("POST " + contextPath + "/service/hello#echo");

        assertSingleLocalSpanMessage(serverSpan).isEqualTo("jaxws service:"
                + " org.glowroot.xyzzy.instrumentation.jaxws.WebServiceIT$HelloService.echo()");
    }

    private void shouldCaptureTransactionNameWithNestedServletMappingHittingRoot(String contextPath,
            Class<? extends AppUnderTest> appUnderTestClass) throws Exception {
        // when
        ServerSpan serverSpan = container.execute(appUnderTestClass, "Web");

        // then
        assertThat(serverSpan.transactionName())
                .isEqualTo("POST " + contextPath + "/service/#echo");

        assertSingleLocalSpanMessage(serverSpan).isEqualTo("jaxws service:"
                + " org.glowroot.xyzzy.instrumentation.jaxws.WebServiceIT$RootService.echo()");
    }

    private void shouldCaptureTransactionNameWithLessNormalServletMapping(String contextPath,
            Class<? extends AppUnderTest> appUnderTestClass) throws Exception {
        // when
        ServerSpan serverSpan = container.execute(appUnderTestClass, "Web");

        // then
        if (!serverSpan.transactionName().equals("POST " + contextPath + "/hello#echo")) {
            // Jersey (2.5 and above) doesn't like this "less than normal" servlet mapping, and ends
            // up mapping everything to RootService
            assertThat(serverSpan.transactionName()).isEqualTo("POST " + contextPath + "#echo");
        }

        assertSingleLocalSpanMessage(serverSpan).isEqualTo("jaxws service:"
                + " org.glowroot.xyzzy.instrumentation.jaxws.WebServiceIT$RootService.echo()");
    }

    private void shouldCaptureTransactionNameWithLessNormalServletMappingHittingRoot(
            String contextPath, Class<? extends AppUnderTest> appUnderTestClass) throws Exception {
        // when
        ServerSpan serverSpan = container.execute(appUnderTestClass, "Web");

        // then
        assertThat(serverSpan.transactionName()).isEqualTo("POST " + contextPath + "/#echo");

        assertSingleLocalSpanMessage(serverSpan).isEqualTo("jaxws service:"
                + " org.glowroot.xyzzy.instrumentation.jaxws.WebServiceIT$RootService.echo()");
    }

    public static class WithNormalServletMapping extends InvokeJaxwsWebServiceInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp1", "", "/hello");
        }
    }

    public static class WithNormalServletMappingWithContextPath
            extends InvokeJaxwsWebServiceInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp1", "/zzz", "/hello");
        }
    }

    public static class WithNormalServletMappingHittingRoot extends InvokeJaxwsWebServiceInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp1", "", "/");
        }
    }

    public static class WithNormalServletMappingHittingRootWithContextPath
            extends InvokeJaxwsWebServiceInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp1", "/zzz", "/");
        }
    }

    public static class WithNestedServletMapping extends InvokeJaxwsWebServiceInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp2", "", "/service/hello");
        }
    }

    public static class WithNestedServletMappingWithContextPath
            extends InvokeJaxwsWebServiceInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp2", "/zzz", "/service/hello");
        }
    }

    public static class WithNestedServletMappingHittingRoot extends InvokeJaxwsWebServiceInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp2", "", "/service/");
        }
    }

    public static class WithNestedServletMappingHittingRootWithContextPath
            extends InvokeJaxwsWebServiceInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp2", "/zzz", "/service/");
        }
    }

    public static class WithLessNormalServletMapping extends InvokeJaxwsWebServiceInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp3", "", "/hello");
        }
    }

    public static class WithLessNormalServletMappingWithContextPath
            extends InvokeJaxwsWebServiceInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp3", "/zzz", "/hello");
        }
    }

    public static class WithLessNormalServletMappingHittingRoot
            extends InvokeJaxwsWebServiceInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp3", "", "/");
        }
    }

    public static class WithLessNormalServletMappingHittingRootWithContextPath
            extends InvokeJaxwsWebServiceInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp3", "/zzz", "/");
        }
    }

    @WebService
    public static class HelloService {
        @WebMethod
        public String echo(@WebParam(name = "param") String msg) {
            return msg;
        }
    }

    @WebService
    public static class RootService {
        @WebMethod
        public String echo(@WebParam(name = "param") String msg) {
            return msg;
        }
    }
}
