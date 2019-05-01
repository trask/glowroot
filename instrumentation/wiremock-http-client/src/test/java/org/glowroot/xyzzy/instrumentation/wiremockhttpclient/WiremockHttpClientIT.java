/**
 * Copyright 2015-2019 the original author or authors.
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
package org.glowroot.xyzzy.instrumentation.wiremockhttpclient;

import java.io.InputStream;

import com.google.common.io.ByteStreams;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import wiremock.org.apache.http.HttpHost;
import wiremock.org.apache.http.HttpResponse;
import wiremock.org.apache.http.client.methods.HttpGet;
import wiremock.org.apache.http.client.methods.HttpPost;
import wiremock.org.apache.http.impl.client.CloseableHttpClient;
import wiremock.org.apache.http.impl.client.HttpClients;

import org.glowroot.xyzzy.test.harness.Container;
import org.glowroot.xyzzy.test.harness.Containers;
import org.glowroot.xyzzy.test.harness.IncomingSpan;
import org.glowroot.xyzzy.test.harness.util.ExecuteHttpBase;

import static org.glowroot.xyzzy.test.harness.util.HarnessAssertions.assertSingleOutgoingSpanMessage;

public class WiremockHttpClientIT {

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
        container.resetAfterEachTest();
    }

    @Test
    public void shouldCaptureHttpGet() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteHttpGet.class);

        // then
        assertSingleOutgoingSpanMessage(incomingSpan)
                .matches("http client request: GET http://localhost:\\d+/hello1");
    }

    @Test
    public void shouldCaptureHttpGetUsingHttpHostArg() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteHttpGetUsingHttpHostArg.class);

        // then
        assertSingleOutgoingSpanMessage(incomingSpan)
                .matches("http client request: GET http://localhost:\\d+/hello2");
    }

    @Test
    public void shouldCaptureHttpPost() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteHttpPost.class);

        // then
        assertSingleOutgoingSpanMessage(incomingSpan)
                .matches("http client request: POST http://localhost:\\d+/hello3");
    }

    @Test
    public void shouldCaptureHttpPostUsingHttpHostArg() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteHttpPostUsingHttpHostArg.class);

        // then
        assertSingleOutgoingSpanMessage(incomingSpan)
                .matches("http client request: POST http://localhost:\\d+/hello4");
    }

    public static class ExecuteHttpGet extends ExecuteHttpBase {
        @Override
        public void transactionMarker() throws Exception {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet("http://localhost:" + getPort() + "/hello1");
            HttpResponse response = httpClient.execute(httpGet);
            int responseStatusCode = response.getStatusLine().getStatusCode();
            if (responseStatusCode != 200) {
                throw new IllegalStateException(
                        "Unexpected response status code: " + responseStatusCode);
            }
            InputStream content = response.getEntity().getContent();
            ByteStreams.exhaust(content);
            content.close();
            httpClient.close();
        }
    }

    public static class ExecuteHttpGetUsingHttpHostArg extends ExecuteHttpBase {
        @Override
        public void transactionMarker() throws Exception {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpHost httpHost = new HttpHost("localhost", getPort());
            HttpGet httpGet = new HttpGet("/hello2");
            HttpResponse response = httpClient.execute(httpHost, httpGet);
            int responseStatusCode = response.getStatusLine().getStatusCode();
            if (responseStatusCode != 200) {
                throw new IllegalStateException(
                        "Unexpected response status code: " + responseStatusCode);
            }
            InputStream content = response.getEntity().getContent();
            ByteStreams.exhaust(content);
            content.close();
            httpClient.close();
        }
    }

    public static class ExecuteHttpPost extends ExecuteHttpBase {
        @Override
        public void transactionMarker() throws Exception {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost("http://localhost:" + getPort() + "/hello3");
            HttpResponse response = httpClient.execute(httpPost);
            int responseStatusCode = response.getStatusLine().getStatusCode();
            if (responseStatusCode != 200) {
                throw new IllegalStateException(
                        "Unexpected response status code: " + responseStatusCode);
            }
            InputStream content = response.getEntity().getContent();
            ByteStreams.exhaust(content);
            content.close();
            httpClient.close();
        }
    }

    public static class ExecuteHttpPostUsingHttpHostArg extends ExecuteHttpBase {
        @Override
        public void transactionMarker() throws Exception {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpHost httpHost = new HttpHost("localhost", getPort());
            HttpPost httpPost = new HttpPost("/hello4");
            HttpResponse response = httpClient.execute(httpHost, httpPost);
            int responseStatusCode = response.getStatusLine().getStatusCode();
            if (responseStatusCode != 200) {
                throw new IllegalStateException(
                        "Unexpected response status code: " + responseStatusCode);
            }
            InputStream content = response.getEntity().getContent();
            ByteStreams.exhaust(content);
            content.close();
            httpClient.close();
        }
    }
}
