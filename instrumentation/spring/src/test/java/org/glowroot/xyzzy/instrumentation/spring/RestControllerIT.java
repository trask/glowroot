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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.glowroot.xyzzy.test.harness.AppUnderTest;
import org.glowroot.xyzzy.test.harness.Container;
import org.glowroot.xyzzy.test.harness.Containers;
import org.glowroot.xyzzy.test.harness.LocalSpan;
import org.glowroot.xyzzy.test.harness.ServerSpan;
import org.glowroot.xyzzy.test.harness.Span;

import static org.assertj.core.api.Assertions.assertThat;

public class RestControllerIT {

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
    public void shouldCaptureTransactionNameWithNormalServletMappingHittingRest() throws Exception {
        shouldCaptureTransactionNameWithNormalServletMappingHittingRest("",
                WithNormalServletMappingHittingRest.class);
    }

    @Test
    public void shouldCaptureTransactionNameWithNormalServletMappingHittingAbc() throws Exception {
        shouldCaptureTransactionNameWithNormalServletMappingHittingAbc("",
                WithNormalServletMappingHittingAbc.class);
    }

    @Test
    public void shouldCaptureTransactionNameWithContextPathAndNormalServletMappingHittingRest()
            throws Exception {
        shouldCaptureTransactionNameWithNormalServletMappingHittingRest("/zzz",
                WithContextPathAndNormalServletMappingHittingRest.class);
    }

    @Test
    public void shouldCaptureTransactionNameWithContextPathAndNormalServletMappingHittingAbc()
            throws Exception {
        shouldCaptureTransactionNameWithNormalServletMappingHittingAbc("/zzz",
                WithContextPathAndNormalServletMappingHittingAbc.class);
    }

    private void shouldCaptureTransactionNameWithNormalServletMappingHittingRest(String contextPath,
            Class<? extends AppUnderTest> appUnderTestClass) throws Exception {
        // when
        ServerSpan serverSpan = container.execute(appUnderTestClass, "Web");

        // then
        assertThat(serverSpan.transactionName()).isEqualTo(contextPath + "/rest");

        validateSpans(serverSpan.childSpans(), TestRestController.class, "rest");
    }

    private void shouldCaptureTransactionNameWithNormalServletMappingHittingAbc(String contextPath,
            Class<? extends AppUnderTest> appUnderTestClass) throws Exception {
        // when
        ServerSpan serverSpan = container.execute(appUnderTestClass, "Web");

        // then
        assertThat(serverSpan.transactionName()).isEqualTo(contextPath + "/abc");

        validateSpans(serverSpan.childSpans(), TestRestWithPropertyController.class, "abc");
    }

    private void validateSpans(List<Span> spans, Class<?> clazz, String methodName) {
        Iterator<Span> i = spans.iterator();

        LocalSpan localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage())
                .isEqualTo("spring controller: " + clazz.getName() + "." + methodName + "()");
        assertThat(localSpan.childSpans()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    public static class WithNormalServletMappingHittingRest extends InvokeSpringControllerInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp1", "", "/rest");
        }
    }

    public static class WithNormalServletMappingHittingAbc extends InvokeSpringControllerInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp1", "", "/abc");
        }
    }

    public static class WithContextPathAndNormalServletMappingHittingRest
            extends InvokeSpringControllerInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp1", "/zzz", "/rest");
        }
    }

    public static class WithContextPathAndNormalServletMappingHittingAbc
            extends InvokeSpringControllerInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp1", "/zzz", "/abc");
        }
    }

    @RestController
    public static class TestRestController {
        @RequestMapping("rest")
        public String rest() {
            return "";
        }
    }

    @RestController
    public static class TestRestWithPropertyController {
        @RequestMapping("${abc.path:abc}")
        public String abc() {
            return "";
        }
    }
}
