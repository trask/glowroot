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
package org.glowroot.xyzzy.instrumentation.apachehttpclient3x;

import java.util.Iterator;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.xyzzy.test.harness.OutgoingSpan;
import org.glowroot.xyzzy.test.harness.Container;
import org.glowroot.xyzzy.test.harness.Containers;
import org.glowroot.xyzzy.test.harness.IncomingSpan;
import org.glowroot.xyzzy.test.harness.Span;
import org.glowroot.xyzzy.test.harness.util.ExecuteHttpBase;

import static org.assertj.core.api.Assertions.assertThat;

public class ApacheHttpClient3xIT {

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
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan span = (OutgoingSpan) i.next();
        assertThat(span.getMessage())
                .matches("http client request: GET http://localhost:\\d+/hello1");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldCaptureHttpPost() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteHttpPost.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan span = (OutgoingSpan) i.next();
        assertThat(span.getMessage())
                .matches("http client request: POST http://localhost:\\d+/hello1");

        assertThat(i.hasNext()).isFalse();
    }

    public static class ExecuteHttpGet extends ExecuteHttpBase {
        @Override
        public void transactionMarker() throws Exception {
            HttpClient httpClient = new HttpClient();
            GetMethod httpGet = new GetMethod("http://localhost:" + getPort() + "/hello1");
            httpClient.executeMethod(httpGet);
            httpGet.releaseConnection();
            if (httpGet.getStatusCode() != 200) {
                throw new IllegalStateException(
                        "Unexpected response status code: " + httpGet.getStatusCode());
            }
        }
    }

    public static class ExecuteHttpPost extends ExecuteHttpBase {
        @Override
        public void transactionMarker() throws Exception {
            HttpClient httpClient = new HttpClient();
            PostMethod httpPost = new PostMethod("http://localhost:" + getPort() + "/hello1");
            httpClient.executeMethod(httpPost);
            httpPost.releaseConnection();
            if (httpPost.getStatusCode() != 200) {
                throw new IllegalStateException(
                        "Unexpected response status code: " + httpPost.getStatusCode());
            }
        }
    }
}
