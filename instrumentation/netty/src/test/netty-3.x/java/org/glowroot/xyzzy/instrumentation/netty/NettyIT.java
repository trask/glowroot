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
package org.glowroot.xyzzy.instrumentation.netty;

import java.net.ServerSocket;

import org.apache.http.NoHttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.agent.it.harness.AppUnderTest;
import org.glowroot.agent.it.harness.Container;
import org.glowroot.agent.it.harness.Containers;
import org.glowroot.wire.api.model.TraceOuterClass.Trace;

import static org.assertj.core.api.Assertions.assertThat;

public class NettyIT {

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
        container.checkAndReset();
    }

    @Test
    public void shouldCaptureHttpGet() throws Exception {
        // when
        Trace trace = container.execute(ExecuteHttpGet.class);

        // then
        assertThat(trace.getHeader().getTransactionName()).isEqualTo("/abc");
        assertThat(trace.getHeader().getHeadline()).isEqualTo("GET /abc?xyz=123");
        assertThat(trace.getEntryCount()).isZero();
    }

    @Test
    public void shouldCaptureHttpGetWithException() throws Exception {
        // when
        Trace trace = container.execute(ExecuteHttpGetWithException.class);

        // then
        assertThat(trace.getHeader().getTransactionName()).isEqualTo("/exception");
        assertThat(trace.getEntryCount()).isZero();
        assertThat(trace.getHeader().getPartial()).isFalse();
    }

    public static class ExecuteHttpGet implements AppUnderTest {

        @Override
        public void executeApp(Serializable... args) throws Exception {
            int port = Ports.getAvailable();
            HttpServer server = new HttpServer(port);
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet("http://localhost:" + port + "/abc?xyz=123");
            int code = httpClient.execute(httpGet).getStatusLine().getStatusCode();
            if (code != 200) {
                throw new IllegalStateException("Unexpected response code: " + code);
            }
            server.close();
        }
    }

    public static class ExecuteHttpGetWithException implements AppUnderTest {

        @Override
        public void executeApp(Serializable... args) throws Exception {
            int port = Ports.getAvailable();
            HttpServer server = new HttpServer(port);
            CloseableHttpClient httpClient = HttpClientBuilder.create()
                    .disableAutomaticRetries()
                    .build();
            HttpGet httpGet = new HttpGet("http://localhost:" + port + "/exception");
            try {
                httpClient.execute(httpGet);
            } catch (NoHttpResponseException e) {
            }
            server.close();
        }
    }
}
