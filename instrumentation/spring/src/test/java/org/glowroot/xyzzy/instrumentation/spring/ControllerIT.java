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
package org.glowroot.xyzzy.instrumentation.spring;

import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import org.glowroot.xyzzy.test.harness.AppUnderTest;
import org.glowroot.xyzzy.test.harness.Container;
import org.glowroot.xyzzy.test.harness.Containers;
import org.glowroot.xyzzy.test.harness.LocalSpan;
import org.glowroot.xyzzy.test.harness.IncomingSpan;
import org.glowroot.xyzzy.test.harness.Span;

import static org.assertj.core.api.Assertions.assertThat;

public class ControllerIT {

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
        container.resetInstrumentationProperties();
    }

    @Test
    public void shouldCaptureTransactionNameWithNormalServletMapping() throws Exception {
        shouldCaptureTransactionNameWithNormalServletMapping("", WithNormalServletMapping.class);
    }

    @Test
    public void shouldCaptureTransactionNameWithContextPathAndNormalServletMapping()
            throws Exception {
        shouldCaptureTransactionNameWithNormalServletMapping("/zzz",
                WithContextPathAndNormalServletMapping.class);
    }

    @Test
    public void shouldCaptureTransactionNameWithNormalServletMappingHittingRoot() throws Exception {
        shouldCaptureTransactionNameWithNormalServletMappingHittingRoot("",
                WithNormalServletMappingHittingRoot.class);
    }

    @Test
    public void shouldCaptureTransactionNameWithContextPathAndNormalServletMappingHittingRoot()
            throws Exception {
        shouldCaptureTransactionNameWithNormalServletMappingHittingRoot("/zzz",
                WithContextPathAndNormalServletMappingHittingRoot.class);
    }

    @Test
    public void shouldCaptureTransactionNameWithNestedServletMapping() throws Exception {
        shouldCaptureTransactionNameWithNestedServletMapping("", WithNestedServletMapping.class);
    }

    @Test
    public void shouldCaptureTransactionNameWithContextPathAndNestedServletMapping()
            throws Exception {
        shouldCaptureTransactionNameWithNestedServletMapping("/zzz",
                WithContextPathAndNestedServletMapping.class);
    }

    @Test
    public void shouldCaptureTransactionNameWithNestedServletMappingHittingRoot() throws Exception {
        shouldCaptureTransactionNameWithNestedServletMappingHittingRoot("",
                WithNestedServletMappingHittingRoot.class);
    }

    @Test
    public void shouldCaptureTransactionNameWithContextPathAndNestedServletMappingHittingRoot()
            throws Exception {
        shouldCaptureTransactionNameWithNestedServletMappingHittingRoot("/zzz",
                WithContextPathAndNestedServletMappingHittingRoot.class);
    }

    @Test
    public void shouldCaptureTransactionNameWithLessNormalServletMapping() throws Exception {
        shouldCaptureTransactionNameWithLessNormalServletMapping("",
                WithLessNormalServletMapping.class);
    }

    @Test
    public void shouldCaptureTransactionNameWithContextPathAndLessNormalServletMapping()
            throws Exception {
        shouldCaptureTransactionNameWithLessNormalServletMapping("/zzz",
                WithContextPathAndLessNormalServletMapping.class);
    }

    @Test
    public void shouldCaptureTransactionNameWithLessNormalServletMappingHittingRoot()
            throws Exception {
        shouldCaptureTransactionNameWithLessNormalServletMappingHittingRoot("",
                WithLessNormalServletMappingHittingRoot.class);
    }

    @Test
    public void shouldCaptureTransactionNameWithContextPathAndLessNormalServletMappingHittingRoot()
            throws Exception {
        shouldCaptureTransactionNameWithLessNormalServletMappingHittingRoot("/zzz",
                WithContextPathAndLessNormalServletMappingHittingRoot.class);
    }

    @Test
    public void shouldCaptureAltTransactionName() throws Exception {
        // given
        container.setInstrumentationProperty("spring", "useAltTransactionNaming", true);

        // when
        IncomingSpan incomingSpan = container.execute(WithNormalServletMapping.class, "Web");

        // then
        assertThat(incomingSpan.transactionName()).isEqualTo("TestController#echo");

        validateSpans(incomingSpan.childSpans(), TestController.class, "echo");
    }

    private void shouldCaptureTransactionNameWithNormalServletMapping(String contextPath,
            Class<? extends AppUnderTest> appUnderTestClass) throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(appUnderTestClass, "Web");

        // then
        assertThat(incomingSpan.transactionName()).isEqualTo(contextPath + "/hello/echo/*");

        validateSpans(incomingSpan.childSpans(), TestController.class, "echo");
    }

    private void shouldCaptureTransactionNameWithNormalServletMappingHittingRoot(String contextPath,
            Class<? extends AppUnderTest> appUnderTestClass) throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(appUnderTestClass, "Web");

        // then
        assertThat(incomingSpan.transactionName()).isEqualTo(contextPath + "/");

        validateSpans(incomingSpan.childSpans(), RootController.class, "echo");
    }

    private void shouldCaptureTransactionNameWithNestedServletMapping(String contextPath,
            Class<? extends AppUnderTest> appUnderTestClass) throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(appUnderTestClass, "Web");

        // then
        assertThat(incomingSpan.transactionName()).isEqualTo(contextPath + "/spring/hello/echo/*");

        validateSpans(incomingSpan.childSpans(), TestController.class, "echo");
    }

    private void shouldCaptureTransactionNameWithNestedServletMappingHittingRoot(String contextPath,
            Class<? extends AppUnderTest> appUnderTestClass) throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(appUnderTestClass, "Web");

        // then
        assertThat(incomingSpan.transactionName()).isEqualTo(contextPath + "/spring/");

        validateSpans(incomingSpan.childSpans(), RootController.class, "echo");
    }

    private void shouldCaptureTransactionNameWithLessNormalServletMapping(String contextPath,
            Class<? extends AppUnderTest> appUnderTestClass) throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(appUnderTestClass, "Web");

        // then
        assertThat(incomingSpan.transactionName()).isEqualTo(contextPath + "/hello/echo/*");

        validateSpans(incomingSpan.childSpans(), TestController.class, "echo");
    }

    private void shouldCaptureTransactionNameWithLessNormalServletMappingHittingRoot(
            String contextPath, Class<? extends AppUnderTest> appUnderTestClass) throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(appUnderTestClass, "Web");

        // then
        assertThat(incomingSpan.transactionName()).isEqualTo(contextPath + "/");

        validateSpans(incomingSpan.childSpans(), RootController.class, "echo");
    }

    private void validateSpans(List<Span> spans, Class<?> clazz, String methodName) {
        Iterator<Span> i = spans.iterator();

        LocalSpan localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage())
                .isEqualTo("spring controller: " + clazz.getName() + "." + methodName + "()");
        assertThat(localSpan.childSpans()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    public static class WithNormalServletMapping extends InvokeSpringControllerInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp1", "", "/hello/echo/5");
        }
    }

    public static class WithNormalServletMappingHittingRoot extends InvokeSpringControllerInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp1", "", "/");
        }
    }

    public static class WithNestedServletMapping extends InvokeSpringControllerInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp2", "", "/spring/hello/echo/5");
        }
    }

    public static class WithNestedServletMappingHittingRoot extends InvokeSpringControllerInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp2", "", "/spring/");
        }
    }

    public static class WithLessNormalServletMapping extends InvokeSpringControllerInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp3", "", "/hello/echo/5");
        }
    }

    public static class WithLessNormalServletMappingHittingRoot
            extends InvokeSpringControllerInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp3", "", "/");
        }
    }

    public static class WithContextPathAndNormalServletMapping
            extends InvokeSpringControllerInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp1", "/zzz", "/hello/echo/5");
        }
    }

    public static class WithContextPathAndNormalServletMappingHittingRoot
            extends InvokeSpringControllerInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp1", "/zzz", "/");
        }
    }

    public static class WithContextPathAndNestedServletMapping
            extends InvokeSpringControllerInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp2", "/zzz", "/spring/hello/echo/5");
        }
    }

    public static class WithContextPathAndNestedServletMappingHittingRoot
            extends InvokeSpringControllerInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp2", "/zzz", "/spring/");
        }
    }

    public static class WithContextPathAndLessNormalServletMapping
            extends InvokeSpringControllerInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp3", "/zzz", "/hello/echo/5");
        }
    }

    public static class WithContextPathAndLessNormalServletMappingHittingRoot
            extends InvokeSpringControllerInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp3", "/zzz", "/");
        }
    }

    @Controller
    @RequestMapping("hello")
    public static class TestController {
        @RequestMapping("echo/{id}")
        public @ResponseBody String echo() {
            return "";
        }
    }

    @Controller
    public static class RootController {
        @RequestMapping("")
        public @ResponseBody String echo() {
            return "";
        }
    }
}
