/**
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
package org.glowroot.xyzzy.instrumentation.apachehttpasyncclient;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.xyzzy.test.harness.Container;
import org.glowroot.xyzzy.test.harness.Containers;
import org.glowroot.xyzzy.test.harness.IncomingSpan;
import org.glowroot.xyzzy.test.harness.LocalSpan;
import org.glowroot.xyzzy.test.harness.OutgoingSpan;
import org.glowroot.xyzzy.test.harness.Span;
import org.glowroot.xyzzy.test.harness.TestSpans;
import org.glowroot.xyzzy.test.harness.util.ExecuteHttpBase;

import static org.assertj.core.api.Assertions.assertThat;

public class ApacheHttpAsyncClientIT {

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
    public void shouldCaptureAsyncHttpGet() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteAsyncHttpGet.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage())
                .matches("http client request: GET http://localhost:\\d+/hello1");

        LocalSpan localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).matches("test local span");
        assertThat(localSpan.childSpans()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldCaptureAsyncHttpGetUsingHttpHostArg() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteAsyncHttpGetUsingHttpHostArg.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage())
                .matches("http client request: GET http://localhost:\\d+/hello2");

        LocalSpan localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).matches("test local span");
        assertThat(localSpan.childSpans()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldCaptureAsyncHttpPost() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteAsyncHttpPost.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage())
                .matches("http client request: POST http://localhost:\\d+/hello3");

        LocalSpan localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).matches("test local span");
        assertThat(localSpan.childSpans()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldCaptureAsyncHttpPostUsingHttpHostArg() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteAsyncHttpPostUsingHttpHostArg.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage())
                .matches("http client request: POST http://localhost:\\d+/hello4");

        LocalSpan localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).matches("test local span");
        assertThat(localSpan.childSpans()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    public static class ExecuteAsyncHttpGet extends ExecuteHttpBase {
        @Override
        public void transactionMarker() throws Exception {
            CloseableHttpAsyncClient httpClient = HttpAsyncClients.createDefault();
            httpClient.start();
            HttpGet httpGet = new HttpGet("http://localhost:" + getPort() + "/hello1");
            SimpleFutureCallback callback = new SimpleFutureCallback();
            Future<HttpResponse> future = httpClient.execute(httpGet, callback);
            callback.latch.await();
            httpClient.close();
            int responseStatusCode = future.get().getStatusLine().getStatusCode();
            if (responseStatusCode != 200) {
                throw new IllegalStateException(
                        "Unexpected response status code: " + responseStatusCode);
            }
        }
    }

    public static class ExecuteAsyncHttpGetUsingHttpHostArg extends ExecuteHttpBase {
        @Override
        public void transactionMarker() throws Exception {
            CloseableHttpAsyncClient httpClient = HttpAsyncClients.createDefault();
            httpClient.start();
            HttpHost httpHost = new HttpHost("localhost", getPort());
            HttpGet httpGet = new HttpGet("/hello2");
            SimpleFutureCallback callback = new SimpleFutureCallback();
            Future<HttpResponse> future = httpClient.execute(httpHost, httpGet, callback);
            callback.latch.await();
            httpClient.close();
            int responseStatusCode = future.get().getStatusLine().getStatusCode();
            if (responseStatusCode != 200) {
                throw new IllegalStateException(
                        "Unexpected response status code: " + responseStatusCode);
            }
        }
    }

    public static class ExecuteAsyncHttpPost extends ExecuteHttpBase {
        @Override
        public void transactionMarker() throws Exception {
            CloseableHttpAsyncClient httpClient = HttpAsyncClients.createDefault();
            httpClient.start();
            HttpPost httpPost = new HttpPost("http://localhost:" + getPort() + "/hello3");
            SimpleFutureCallback callback = new SimpleFutureCallback();
            Future<HttpResponse> future = httpClient.execute(httpPost, callback);
            callback.latch.await();
            httpClient.close();
            int responseStatusCode = future.get().getStatusLine().getStatusCode();
            if (responseStatusCode != 200) {
                throw new IllegalStateException(
                        "Unexpected response status code: " + responseStatusCode);
            }
        }
    }

    public static class ExecuteAsyncHttpPostUsingHttpHostArg extends ExecuteHttpBase {
        @Override
        public void transactionMarker() throws Exception {
            CloseableHttpAsyncClient httpClient = HttpAsyncClients.createDefault();
            httpClient.start();
            HttpHost httpHost = new HttpHost("localhost", getPort());
            HttpPost httpPost = new HttpPost("/hello4");
            SimpleFutureCallback callback = new SimpleFutureCallback();
            Future<HttpResponse> future = httpClient.execute(httpHost, httpPost, callback);
            callback.latch.await();
            httpClient.close();
            int responseStatusCode = future.get().getStatusLine().getStatusCode();
            if (responseStatusCode != 200) {
                throw new IllegalStateException(
                        "Unexpected response status code: " + responseStatusCode);
            }
        }
    }

    private static class SimpleFutureCallback implements FutureCallback<HttpResponse> {

        private final CountDownLatch latch = new CountDownLatch(1);

        @Override
        public void cancelled() {
            latch.countDown();
        }

        @Override
        public void completed(HttpResponse response) {
            TestSpans.createLocalSpan();
            latch.countDown();
        }

        @Override
        public void failed(Exception e) {
            latch.countDown();
        }
    }
}
