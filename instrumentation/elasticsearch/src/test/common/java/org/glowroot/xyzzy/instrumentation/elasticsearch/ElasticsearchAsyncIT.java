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
package org.glowroot.xyzzy.instrumentation.elasticsearch;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.xyzzy.test.harness.AppUnderTest;
import org.glowroot.xyzzy.test.harness.ClientSpan;
import org.glowroot.xyzzy.test.harness.Container;
import org.glowroot.xyzzy.test.harness.ServerSpan;
import org.glowroot.xyzzy.test.harness.Span;
import org.glowroot.xyzzy.test.harness.TransactionMarker;

import static org.assertj.core.api.Assertions.assertThat;

public class ElasticsearchAsyncIT {

    private static Container container;

    @BeforeClass
    public static void setUp() throws Exception {
        container = SharedSetupRunListener.getContainer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        SharedSetupRunListener.close(container);
    }

    @After
    public void afterEachTest() throws Exception {
        container.resetInstrumentationConfig();
    }

    @Test
    public void shouldCaptureDocumentPut() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(ExecuteDocumentPut.class);

        // then
        checkTimers(serverSpan, 1);

        Iterator<Span> i = serverSpan.childSpans().iterator();

        ClientSpan clientSpan = (ClientSpan) i.next();
        assertThat(clientSpan.getMessage()).isEmpty();
        assertThat(clientSpan.getMessage()).isEqualTo("PUT testindex/testtype");
        assertThat(clientSpan.getPrefix()).isEqualTo("elasticsearch query: ");
        assertThat(clientSpan.getSuffix()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldCaptureDocumentGet() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(ExecuteDocumentGet.class);

        // then
        checkTimers(serverSpan, 1);

        Iterator<Span> i = serverSpan.childSpans().iterator();

        ClientSpan clientSpan = (ClientSpan) i.next();
        assertThat(clientSpan.getMessage()).isEmpty();
        assertThat(clientSpan.getMessage()).isEqualTo("GET testindex/testtype");
        assertThat(clientSpan.getPrefix()).isEqualTo("elasticsearch query: ");
        assertThat(clientSpan.getSuffix()).startsWith(" [");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldConcurrentlyAsyncExecuteSameStatement() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(ConcurrentlyExecuteSameAsyncStatement.class);

        // then
        checkTimers(serverSpan, 100);

        Iterator<Span> i = serverSpan.childSpans().iterator();

        for (int j = 0; j < 100; j++) {
            ClientSpan clientSpan = (ClientSpan) i.next();
            assertThat(clientSpan.getMessage()).isEmpty();
            assertThat(clientSpan.getMessage())
                    .isEqualTo("GET testindex/testtype");
            assertThat(clientSpan.getPrefix())
                    .isEqualTo("elasticsearch query: ");
            assertThat(clientSpan.getSuffix()).startsWith(" [");
        }
        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldCaptureDocumentUpdate() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(ExecuteDocumentUpdate.class);

        // then
        checkTimers(serverSpan, 1);

        Iterator<Span> i = serverSpan.childSpans().iterator();

        ClientSpan clientSpan = (ClientSpan) i.next();
        assertThat(clientSpan.getMessage()).isEmpty();
        assertThat(clientSpan.getMessage()).isEqualTo("PUT testindex/testtype");
        assertThat(clientSpan.getPrefix()).isEqualTo("elasticsearch query: ");
        assertThat(clientSpan.getSuffix()).startsWith(" [");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldCaptureDocumentDelete() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(ExecuteDocumentDelete.class);

        // then
        checkTimers(serverSpan, 1);

        Iterator<Span> i = serverSpan.childSpans().iterator();

        ClientSpan clientSpan = (ClientSpan) i.next();
        assertThat(clientSpan.getMessage()).isEmpty();
        assertThat(clientSpan.getMessage())
                .isEqualTo("DELETE testindex/testtype");
        assertThat(clientSpan.getPrefix()).isEqualTo("elasticsearch query: ");
        assertThat(clientSpan.getSuffix()).startsWith(" [");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldCaptureDocumentSearchWithoutSource() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(ExecuteDocumentSearchWithoutSource.class);

        // then
        checkTimers(serverSpan, 1);

        Iterator<Span> i = serverSpan.childSpans().iterator();

        ClientSpan clientSpan = (ClientSpan) i.next();
        assertThat(clientSpan.getMessage()).isEmpty();
        assertThat(clientSpan.getMessage())
                .startsWith("SEARCH testindex/testtype {");
        assertThat(clientSpan.getPrefix()).isEqualTo("elasticsearch query: ");
        assertThat(clientSpan.getSuffix()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldCaptureDocumentSearchWithoutIndexesWithoutSource() throws Exception {
        // when
        ServerSpan serverSpan =
                container.execute(ExecuteDocumentSearchWithoutIndexesWithoutSource.class);

        // then
        checkTimers(serverSpan, 1);

        Iterator<Span> i = serverSpan.childSpans().iterator();

        ClientSpan clientSpan = (ClientSpan) i.next();
        assertThat(clientSpan.getMessage()).isEmpty();
        assertThat(clientSpan.getMessage())
                .startsWith("SEARCH _any/testtype {");
        assertThat(clientSpan.getPrefix()).isEqualTo("elasticsearch query: ");
        assertThat(clientSpan.getSuffix()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldCaptureDocumentSearchWithoutIndexesWithoutTypesWithoutSource()
            throws Exception {
        // when
        ServerSpan serverSpan = container
                .execute(ExecuteDocumentSearchWithoutIndexesWithoutTypesWithoutSource.class);

        // then
        checkTimers(serverSpan, 1);

        Iterator<Span> i = serverSpan.childSpans().iterator();

        ClientSpan clientSpan = (ClientSpan) i.next();
        assertThat(clientSpan.getMessage()).isEmpty();
        assertThat(clientSpan.getMessage()).startsWith("SEARCH / {");
        assertThat(clientSpan.getPrefix()).isEqualTo("elasticsearch query: ");
        assertThat(clientSpan.getSuffix()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldCaptureDocumentSearchWithMultipleIndexesWithMultipleTypesWithoutSource()
            throws Exception {
        // when
        ServerSpan serverSpan = container.execute(
                ExecuteDocumentSearchWithMultipleIndexesWithMultipleTypesWithoutSource.class);

        // then
        checkTimers(serverSpan, 1);

        Iterator<Span> i = serverSpan.childSpans().iterator();

        ClientSpan clientSpan = (ClientSpan) i.next();
        assertThat(clientSpan.getMessage())
                .startsWith("SEARCH testindex,testindex2/testtype,testtype2 {");
        assertThat(clientSpan.getPrefix()).isEqualTo("elasticsearch query: ");
        assertThat(clientSpan.getSuffix()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldCaptureDocumentSearchWithSource() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(ExecuteDocumentSearchWithSource.class);

        // then
        checkTimers(serverSpan, 1);

        Iterator<Span> i = serverSpan.childSpans().iterator();

        ClientSpan clientSpan = (ClientSpan) i.next();
        assertThat(clientSpan.getMessage()).startsWith("SEARCH testindex/testtype {");
        assertThat(clientSpan.getPrefix()).isEqualTo("elasticsearch query: ");
        assertThat(clientSpan.getSuffix()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    private static void checkTimers(ServerSpan serverSpan, int count) {
        ServerSpan.Timer rootTimer = serverSpan.mainThreadRootTimer();
        List<String> timerNames = Lists.newArrayList();
        for (ServerSpan.Timer timer : rootTimer.childTimers()) {
            timerNames.add(timer.name());
        }
        Collections.sort(timerNames);
        assertThat(timerNames).containsExactly("elasticsearch query");
        for (ServerSpan.Timer timer : rootTimer.childTimers()) {
            assertThat(timer.childTimers()).isEmpty();
        }
        assertThat(serverSpan.asyncTimers().size()).isEqualTo(1);
        ServerSpan.Timer asyncTimer = serverSpan.asyncTimers().get(0);
        assertThat(asyncTimer.childTimers()).isEmpty();
        assertThat(asyncTimer.name()).isEqualTo("elasticsearch query");
        assertThat(asyncTimer.count()).isEqualTo(count);
    }

    public static class ExecuteDocumentPut implements AppUnderTest, TransactionMarker {

        private TransportClient client;

        @Override
        public void executeApp() throws Exception {
            client = Util.client(new InetSocketAddress("127.0.0.1", 9300));
            transactionMarker();
            client.close();
        }

        @Override
        public void transactionMarker() throws Exception {
            client.prepareIndex("testindex", "testtype")
                    .setSource("abc", 11, "xyz", "some text")
                    .execute()
                    .get();
        }
    }

    public static class ExecuteDocumentGet implements AppUnderTest, TransactionMarker {

        private TransportClient client;
        private String documentId;

        @Override
        public void executeApp() throws Exception {
            client = Util.client(new InetSocketAddress("127.0.0.1", 9300));
            IndexResponse response = client.prepareIndex("testindex", "testtype")
                    .setSource("abc", 11, "xyz", "some text")
                    .get();
            documentId = response.getId();
            transactionMarker();
            client.close();
        }

        @Override
        public void transactionMarker() throws Exception {
            client.prepareGet("testindex", "testtype", documentId)
                    .execute()
                    .get();
        }
    }

    public static class ConcurrentlyExecuteSameAsyncStatement
            implements AppUnderTest, TransactionMarker {

        private TransportClient client;
        private String documentId;

        @Override
        public void executeApp() throws Exception {
            client = Util.client(new InetSocketAddress("127.0.0.1", 9300));
            IndexResponse response = client.prepareIndex("testindex", "testtype")
                    .setSource("abc", 11, "xyz", "some text")
                    .get();
            documentId = response.getId();
            transactionMarker();
            client.close();
        }

        @Override
        public void transactionMarker() throws Exception {
            List<ActionFuture<GetResponse>> futures = Lists.newArrayList();
            for (int i = 0; i < 100; i++) {
                futures.add(client.prepareGet("testindex", "testtype", documentId)
                        .execute());
            }
            for (ActionFuture<GetResponse> future : futures) {
                future.get();
            }
        }
    }

    public static class ExecuteDocumentUpdate implements AppUnderTest, TransactionMarker {

        private TransportClient client;
        private String documentId;

        @Override
        public void executeApp() throws Exception {
            client = Util.client(new InetSocketAddress("127.0.0.1", 9300));
            IndexResponse response = client.prepareIndex("testindex", "testtype")
                    .setSource("abc", 11, "xyz", "some text")
                    .get();
            documentId = response.getId();
            transactionMarker();
            client.close();
        }

        @Override
        public void transactionMarker() throws Exception {
            client.prepareUpdate("testindex", "testtype", documentId)
                    .setDoc("xyz", "some updated text")
                    .execute()
                    .get();
        }
    }

    public static class ExecuteDocumentDelete implements AppUnderTest, TransactionMarker {

        private TransportClient client;
        private String documentId;

        @Override
        public void executeApp() throws Exception {
            client = Util.client(new InetSocketAddress("127.0.0.1", 9300));
            IndexResponse response = client.prepareIndex("testindex", "testtype")
                    .setSource("abc", 11, "xyz", "some text")
                    .get();
            documentId = response.getId();
            transactionMarker();
            client.close();
        }

        @Override
        public void transactionMarker() throws Exception {
            client.prepareDelete("testindex", "testtype", documentId)
                    .execute()
                    .get();
        }
    }

    public static class ExecuteDocumentSearchWithoutSource
            implements AppUnderTest, TransactionMarker {

        private TransportClient client;

        @Override
        public void executeApp() throws Exception {
            client = Util.client(new InetSocketAddress("127.0.0.1", 9300));
            client.prepareIndex("testindex", "testtype")
                    .setSource("abc", 11, "xyz", "some text")
                    .get();
            transactionMarker();
            client.close();
        }

        @Override
        public void transactionMarker() throws Exception {
            client.prepareSearch("testindex")
                    .setTypes("testtype")
                    .execute()
                    .get();
        }
    }

    public static class ExecuteDocumentSearchWithoutIndexesWithoutSource
            implements AppUnderTest, TransactionMarker {

        private TransportClient client;

        @Override
        public void executeApp() throws Exception {
            client = Util.client(new InetSocketAddress("127.0.0.1", 9300));
            client.prepareIndex("testindex", "testtype")
                    .setSource("abc", 11, "xyz", "some text")
                    .get();
            transactionMarker();
            client.close();
        }

        @Override
        public void transactionMarker() throws Exception {
            client.prepareSearch()
                    .setTypes("testtype")
                    .execute()
                    .get();
        }
    }

    public static class ExecuteDocumentSearchWithoutTypesWithoutSource
            implements AppUnderTest, TransactionMarker {

        private TransportClient client;

        @Override
        public void executeApp() throws Exception {
            client = Util.client(new InetSocketAddress("127.0.0.1", 9300));
            client.prepareIndex("testindex", "testtype")
                    .setSource("abc", 11, "xyz", "some text")
                    .get();
            transactionMarker();
            client.close();
        }

        @Override
        public void transactionMarker() throws Exception {
            client.prepareSearch("testindex")
                    .execute()
                    .get();
        }
    }

    public static class ExecuteDocumentSearchWithoutIndexesWithoutTypesWithoutSource
            implements AppUnderTest, TransactionMarker {

        private TransportClient client;

        @Override
        public void executeApp() throws Exception {
            client = Util.client(new InetSocketAddress("127.0.0.1", 9300));
            client.prepareIndex("testindex", "testtype")
                    .setSource("abc", 11, "xyz", "some text")
                    .get();
            transactionMarker();
            client.close();
        }

        @Override
        public void transactionMarker() throws Exception {
            client.prepareSearch()
                    .execute()
                    .get();
        }
    }

    public static class ExecuteDocumentSearchWithMultipleIndexesWithMultipleTypesWithoutSource
            implements AppUnderTest, TransactionMarker {

        private TransportClient client;

        @Override
        public void executeApp() throws Exception {
            client = Util.client(new InetSocketAddress("127.0.0.1", 9300));
            client.prepareIndex("testindex", "testtype")
                    .setSource("abc", 11, "xyz", "some text")
                    .get();
            client.prepareIndex("testindex2", "testtype2")
                    .setSource("abc2", 11, "xyz2", "some text")
                    .get();
            transactionMarker();
            client.close();
        }

        @Override
        public void transactionMarker() throws Exception {
            client.prepareSearch("testindex", "testindex2")
                    .setTypes("testtype", "testtype2")
                    .execute()
                    .get();
        }
    }

    public static class ExecuteDocumentSearchWithSource implements AppUnderTest, TransactionMarker {

        private TransportClient client;

        @Override
        public void executeApp() throws Exception {
            client = Util.client(new InetSocketAddress("127.0.0.1", 9300));
            client.prepareIndex("testindex", "testtype")
                    .setSource("abc", 11, "xyz", "some text")
                    .get();
            transactionMarker();
            client.close();
        }

        @Override
        public void transactionMarker() throws Exception {
            client.prepareSearch("testindex")
                    .setTypes("testtype")
                    .setQuery(QueryBuilders.termQuery("xyz", "text"))
                    .execute()
                    .get();
        }
    }
}
