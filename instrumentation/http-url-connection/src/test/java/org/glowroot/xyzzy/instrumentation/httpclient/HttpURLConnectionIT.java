/**
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
package org.glowroot.xyzzy.instrumentation.httpclient;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.common.io.ByteStreams;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.agent.it.harness.AppUnderTest;
import org.glowroot.agent.it.harness.Container;
import org.glowroot.agent.it.harness.impl.JavaagentContainer;
import org.glowroot.agent.it.harness.model.ServerSpan;
import org.glowroot.agent.it.harness.util.ExecuteHttpBase;

import static org.glowroot.agent.it.harness.validation.HarnessAssertions.assertSingleClientSpanMessage;

public class HttpURLConnectionIT {

    private static Container container;

    @BeforeClass
    public static void setUp() throws Exception {
        // need to use javaagent container since HttpURLConnection is in the bootstrap class loader
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
    public void shouldCaptureHttpGet() throws Exception {
        shouldCaptureHttpGet(ExecuteHttpGet.class, "http");
    }

    @Test
    public void shouldCaptureHttpGetWithQueryString() throws Exception {
        shouldCaptureHttpGetWithQueryString(ExecuteHttpGetWithQueryString.class, "http");
    }

    @Test
    public void shouldCaptureHttpPost() throws Exception {
        shouldCaptureHttpPost(ExecuteHttpPost.class, "http");
    }

    @Test
    public void shouldCaptureHttpGetHTTPS() throws Exception {
        shouldCaptureHttpGet(ExecuteHttpGetHTTPS.class, "https");
    }

    @Test
    public void shouldCaptureHttpGetWithQueryStringHTTPS() throws Exception {
        shouldCaptureHttpGetWithQueryString(ExecuteHttpGetWithQueryStringHTTPS.class, "https");
    }

    @Test
    public void shouldCaptureHttpPostHTTPS() throws Exception {
        shouldCaptureHttpPost(ExecuteHttpPostHTTPS.class, "https");
    }

    private void shouldCaptureHttpGet(Class<? extends AppUnderTest> appUnderTestClass,
            String protocol) throws Exception {
        // when
        ServerSpan serverSpan = container.execute(appUnderTestClass);

        // then
        assertSingleClientSpanMessage(serverSpan)
                .matches("http client request: GET " + protocol + "://localhost:\\d+/hello1/");
    }

    private void shouldCaptureHttpGetWithQueryString(
            Class<? extends AppUnderTest> appUnderTestClass, String protocol) throws Exception {
        // when
        ServerSpan serverSpan = container.execute(appUnderTestClass);

        // then
        assertSingleClientSpanMessage(serverSpan).matches("http client request: GET " + protocol
                + "http client request: GET " + protocol + "://localhost:\\d+/hello1\\?abc=xyz");
    }

    private void shouldCaptureHttpPost(Class<? extends AppUnderTest> appUnderTestClass,
            String protocol) throws Exception {
        // when
        ServerSpan serverSpan = container.execute(appUnderTestClass);

        // then
        assertSingleClientSpanMessage(serverSpan)
                .matches("http client request: POST " + protocol + "://localhost:\\d+/hello1/");
    }

    public static class ExecuteHttpGet extends ExecuteHttpBase {
        @Override
        public void transactionMarker() throws Exception {
            String protocol = getClass().getName().endsWith("HTTPS") ? "https" : "http";
            URL obj = new URL(protocol + "://localhost:" + getPort() + "/hello1/");
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            InputStream content = connection.getInputStream();
            ByteStreams.exhaust(content);
            content.close();
        }
    }

    public static class ExecuteHttpGetWithQueryString extends ExecuteHttpBase {
        @Override
        public void transactionMarker() throws Exception {
            String protocol = getClass().getName().endsWith("HTTPS") ? "https" : "http";
            URL obj = new URL(protocol + "://localhost:" + getPort() + "/hello1?abc=xyz");
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            InputStream content = connection.getInputStream();
            ByteStreams.exhaust(content);
            content.close();
        }
    }

    public static class ExecuteHttpPost extends ExecuteHttpBase {
        @Override
        public void transactionMarker() throws Exception {
            String protocol = getClass().getName().endsWith("HTTPS") ? "https" : "http";
            URL obj = new URL(protocol + "://localhost:" + getPort() + "/hello1/");
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setDoOutput(true);
            connection.getOutputStream().write("some data".getBytes());
            connection.getOutputStream().close();
            InputStream content = connection.getInputStream();
            ByteStreams.exhaust(content);
            content.close();
        }
    }

    public static class ExecuteHttpGetHTTPS extends ExecuteHttpGet {}

    public static class ExecuteHttpGetWithQueryStringHTTPS extends ExecuteHttpGetWithQueryString {}

    public static class ExecuteHttpPostHTTPS extends ExecuteHttpPost {}
}
