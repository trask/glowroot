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
package org.glowroot.xyzzy.instrumentation.httpclient;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.HttpResponseStatus;
import org.asynchttpclient.Response;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.agent.it.harness.Container;
import org.glowroot.agent.it.harness.Containers;
import org.glowroot.agent.it.harness.model.ClientSpan;
import org.glowroot.agent.it.harness.model.ServerSpan;
import org.glowroot.agent.it.harness.model.ServerSpan.Timer;
import org.glowroot.agent.it.harness.model.Span;
import org.glowroot.agent.it.harness.util.ExecuteHttpBase;

import static org.assertj.core.api.Assertions.assertThat;

// TODO test against AsyncHttpClient providers jdk and grizzly (in addition to the default netty)
public class AsyncHttpClientIT {

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
    public void shouldCaptureHttpGet() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(ExecuteHttpGet.class);

        // then
        ServerSpan.Timer rootTimer = serverSpan.mainThreadRootTimer();
        assertThat(rootTimer.childTimers().size()).isEqualTo(1);
        assertThat(rootTimer.childTimers().get(0).name()).isEqualTo("http client request");
        assertThat(rootTimer.childTimers().get(0).count()).isEqualTo(1);

        List<Timer> asyncTimers = serverSpan.asyncTimers();
        assertThat(asyncTimers.size()).isEqualTo(1);

        ServerSpan.Timer asyncTimer = asyncTimers.get(0);
        assertThat(asyncTimer.childTimers()).isEmpty();
        assertThat(asyncTimer.name()).isEqualTo("http client request");
        assertThat(asyncTimer.count()).isEqualTo(1);

        Iterator<Span> i = serverSpan.childSpans().iterator();

        ClientSpan clientSpan = (ClientSpan) i.next();
        assertThat(clientSpan.getMessage())
                .matches("http client request: GET http://localhost:\\d+/hello1/");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldCaptureHttpPost() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(ExecuteHttpPost.class);

        // then
        ServerSpan.Timer rootTimer = serverSpan.mainThreadRootTimer();
        assertThat(rootTimer.childTimers().size()).isEqualTo(1);
        assertThat(rootTimer.childTimers().get(0).name()).isEqualTo("http client request");
        assertThat(rootTimer.childTimers().get(0).count()).isEqualTo(1);

        List<Timer> asyncTimers = serverSpan.asyncTimers();
        assertThat(asyncTimers.size()).isEqualTo(1);

        ServerSpan.Timer asyncTimer = asyncTimers.get(0);
        assertThat(asyncTimer.childTimers()).isEmpty();
        assertThat(asyncTimer.name()).isEqualTo("http client request");
        assertThat(asyncTimer.count()).isEqualTo(1);

        Iterator<Span> i = serverSpan.childSpans().iterator();

        ClientSpan clientSpan = (ClientSpan) i.next();
        assertThat(clientSpan.getMessage())
                .matches("http client request: POST http://localhost:\\d+/hello2");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldCaptureHttpGetWithAsyncHandler() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(ExecuteHttpGetWithAsyncHandler.class);

        // then
        ServerSpan.Timer rootTimer = serverSpan.mainThreadRootTimer();
        assertThat(rootTimer.childTimers().size()).isEqualTo(1);
        assertThat(rootTimer.childTimers().get(0).name()).isEqualTo("http client request");
        assertThat(rootTimer.childTimers().get(0).count()).isEqualTo(1);

        List<Timer> asyncTimers = serverSpan.asyncTimers();
        assertThat(asyncTimers.size()).isEqualTo(1);

        ServerSpan.Timer asyncTimer = asyncTimers.get(0);
        assertThat(asyncTimer.childTimers()).isEmpty();
        assertThat(asyncTimer.name()).isEqualTo("http client request");
        assertThat(asyncTimer.count()).isEqualTo(1);

        Iterator<Span> i = serverSpan.childSpans().iterator();

        ClientSpan clientSpan = (ClientSpan) i.next();
        assertThat(clientSpan.getMessage())
                .matches("http client request: GET http://localhost:\\d+/hello3/");

        assertThat(i.hasNext()).isFalse();
    }

    public static class ExecuteHttpGet extends ExecuteHttpBase {
        @Override
        public void transactionMarker() throws Exception {
            AsyncHttpClient asyncHttpClient = new DefaultAsyncHttpClient();
            int statusCode =
                    asyncHttpClient.prepareGet("http://localhost:" + getPort() + "/hello1/")
                            .execute().get().getStatusCode();
            asyncHttpClient.close();
            if (statusCode != 200) {
                throw new IllegalStateException("Unexpected status code: " + statusCode);
            }
        }
    }

    public static class ExecuteHttpPost extends ExecuteHttpBase {
        @Override
        public void transactionMarker() throws Exception {
            AsyncHttpClient asyncHttpClient = new DefaultAsyncHttpClient();
            int statusCode =
                    asyncHttpClient.preparePost("http://localhost:" + getPort() + "/hello2")
                            .execute().get().getStatusCode();
            asyncHttpClient.close();
            if (statusCode != 200) {
                throw new IllegalStateException("Unexpected status code: " + statusCode);
            }
        }
    }

    public static class ExecuteHttpGetWithAsyncHandler extends ExecuteHttpBase {
        @Override
        public void transactionMarker() throws Exception {
            AsyncHttpClient asyncHttpClient = new DefaultAsyncHttpClient();
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicInteger statusCode = new AtomicInteger();
            asyncHttpClient.prepareGet("http://localhost:" + getPort() + "/hello3/")
                    .execute(new AsyncCompletionHandler<Response>() {
                        @Override
                        public Response onCompleted(Response response) throws Exception {
                            latch.countDown();
                            return null;
                        }
                        @Override
                        public State onStatusReceived(HttpResponseStatus status) {
                            statusCode.set(status.getStatusCode());
                            return null;
                        }
                        @Override
                        public void onThrowable(Throwable t) {
                            t.printStackTrace();
                        }
                    });
            latch.await();
            asyncHttpClient.close();
            if (statusCode.get() != 200) {
                throw new IllegalStateException("Unexpected status code: " + statusCode);
            }
        }
    }
}
