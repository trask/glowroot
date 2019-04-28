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
package org.glowroot.xyzzy.instrumentation.javahttpserver;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.agent.it.harness.Container;
import org.glowroot.agent.it.harness.impl.JavaagentContainer;
import org.glowroot.agent.it.harness.model.Trace;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("restriction")
public class JavaHttpServerIT {

    private static final String INSTRUMENTATION_ID = "java-http-server";

    private static Container container;

    @BeforeClass
    public static void setUp() throws Exception {
        // tests only work with javaagent container because they need to weave bootstrap classes
        // that implement com.sun.net.httpserver.HttpExchange
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
    public void testHandler() throws Exception {
        // when
        Trace trace = container.execute(ExecuteHandler.class, "Web");

        // then
        assertThat(trace.headline()).isEqualTo("/testhandler");
        assertThat(trace.transactionName()).isEqualTo("/testhandler");
        assertThat(getDetailValue(trace, "Request http method")).isEqualTo("GET");
        assertThat(trace.entries()).isEmpty();
    }

    @Test
    public void testFilter() throws Exception {
        // when
        Trace trace = container.execute(ExecuteFilter.class, "Web");

        // then
        assertThat(trace.headline()).isEqualTo("/testfilter");
        assertThat(trace.transactionName()).isEqualTo("/testfilter");
        assertThat(getDetailValue(trace, "Request http method")).isEqualTo("GET");
        assertThat(trace.entries()).isEmpty();
    }

    @Test
    public void testCombination() throws Exception {
        // when
        Trace trace = container.execute(ExecuteFilterWithNestedHandler.class, "Web");

        // then
        assertThat(trace.headline()).isEqualTo("/testfilter");
        assertThat(trace.transactionName()).isEqualTo("/testfilter");
        assertThat(getDetailValue(trace, "Request http method")).isEqualTo("GET");
        assertThat(trace.entries()).isEmpty();
    }

    @Test
    public void testNoQueryString() throws Exception {
        // when
        Trace trace = container.execute(TestNoQueryString.class, "Web");
        // then
        assertThat(getDetailValue(trace, "Request query string")).isNull();
        assertThat(trace.entries()).isEmpty();
    }

    @Test
    public void testEmptyQueryString() throws Exception {
        // when
        Trace trace = container.execute(TestEmptyQueryString.class, "Web");
        // then
        assertThat(getDetailValue(trace, "Request query string")).isEqualTo("");
        assertThat(trace.entries()).isEmpty();
    }

    @Test
    public void testNonEmptyQueryString() throws Exception {
        // when
        Trace trace = container.execute(TestNonEmptyQueryString.class, "Web");
        // then
        assertThat(getDetailValue(trace, "Request query string")).isEqualTo("a=b&c=d");
        assertThat(trace.entries()).isEmpty();
    }

    @Test
    public void testHandlerThrowsException() throws Exception {
        // when
        Trace trace = container.execute(HandlerThrowsException.class, "Web");

        // then
        assertThat(trace.error().message()).isNotEmpty();
        assertThat(trace.error().exception()).isNotNull();
        assertThat(trace.entries()).isEmpty();
    }

    @Test
    public void testFilterThrowsException() throws Exception {
        // when
        Trace trace = container.execute(FilterThrowsException.class, "Web");

        // then
        assertThat(trace.error().message()).isNotEmpty();
        assertThat(trace.error().exception()).isNotNull();
        assertThat(trace.entries()).isEmpty();
    }

    @Test
    public void testSend500Error() throws Exception {
        // when
        Trace trace = container.execute(Send500Error.class, "Web");

        // then
        assertThat(trace.error().message())
                .isEqualTo("sendResponseHeaders, HTTP status code 500");
        assertThat(trace.error().exception()).isNull();

        Iterator<Trace.Entry> i = trace.entries().iterator();

        Trace.Entry entry = i.next();
        assertThat(entry.error().message())
                .isEqualTo("sendResponseHeaders, HTTP status code 500");
        assertThat(entry.error().exception()).isNull();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testSend400Error() throws Exception {
        // when
        Trace trace = container.execute(Send400Error.class, "Web");

        // then
        assertThat(trace.error()).isNull();
        assertThat(trace.entries()).isEmpty();
    }

    @Test
    public void testSend400ErrorWithCaptureOn() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID, "traceErrorOn4xxResponseCode",
                true);

        // when
        Trace trace = container.execute(Send400Error.class, "Web");

        // then
        assertThat(trace.error().message())
                .isEqualTo("sendResponseHeaders, HTTP status code 400");
        assertThat(trace.error().exception()).isNull();

        Iterator<Trace.Entry> i = trace.entries().iterator();

        Trace.Entry entry = i.next();
        assertThat(entry.error().message())
                .isEqualTo("sendResponseHeaders, HTTP status code 400");
        assertThat(entry.error().exception()).isNull();

        assertThat(i.hasNext()).isFalse();
    }

    private static String getDetailValue(Trace trace, String name) {
        for (Map.Entry<String, ?> entry : trace.details().entrySet()) {
            if (entry.getKey().equals(name)) {
                return (String) entry.getValue();
            }
        }
        return null;
    }

    public static class ExecuteHandler extends TestHandler {}

    public static class ExecuteFilter extends TestFilter {}

    public static class ExecuteFilterWithNestedHandler extends TestFilter {
        @Override
        public void doFilter(HttpExchange exchange, Chain chain)
                throws IOException {
            new TestFilter().doFilter(exchange, chain);
        }
    }

    public static class TestNoQueryString extends TestHandler {
        @Override
        protected void before(HttpExchange exchange) {
            ((MockHttpExchange) exchange).setQueryString(null);
        }
    }

    public static class TestEmptyQueryString extends TestHandler {
        @Override
        protected void before(HttpExchange exchange) {
            ((MockHttpExchange) exchange).setQueryString("");
        }
    }

    public static class TestNonEmptyQueryString extends TestHandler {
        @Override
        protected void before(HttpExchange exchange) {
            ((MockHttpExchange) exchange).setQueryString("a=b&c=d");
        }
    }

    public static class HandlerThrowsException extends TestHandler {
        private final RuntimeException exception = new RuntimeException("Something happened");
        @Override
        public void executeApp() throws Exception {
            try {
                super.executeApp();
            } catch (RuntimeException e) {
                // only suppress expected exception
                if (e != exception) {
                    throw e;
                }
            }
        }
        @Override
        public void handle(HttpExchange exchange) {
            throw exception;
        }
    }

    public static class FilterThrowsException extends TestFilter {
        private final RuntimeException exception = new RuntimeException("Something happened");
        @Override
        public void executeApp() throws Exception {
            try {
                super.executeApp();
            } catch (RuntimeException e) {
                // only suppress expected exception
                if (e != exception) {
                    throw e;
                }
            }
        }
        @Override
        public void doFilter(HttpExchange exchange, Chain chain) {
            throw exception;
        }
    }

    public static class Send500Error extends TestHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.sendResponseHeaders(500, 0);
        }
    }

    public static class Send400Error extends TestHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.sendResponseHeaders(400, 0);
        }
    }

    public static class NestedTwo {
        private final String two;
        public NestedTwo(String two) {
            this.two = two;
        }
        public String getTwo() {
            return two;
        }
    }
}
