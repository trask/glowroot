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
import java.util.Iterator;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.xyzzy.test.harness.AppUnderTest;
import org.glowroot.xyzzy.test.harness.Container;
import org.glowroot.xyzzy.test.harness.IncomingSpan;
import org.glowroot.xyzzy.test.harness.OutgoingSpan;
import org.glowroot.xyzzy.test.harness.Span;
import org.glowroot.xyzzy.test.harness.TransactionMarker;

import static org.assertj.core.api.Assertions.assertThat;

public class ElasticsearchSyncIT {

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
        container.resetInstrumentationProperties();
    }

    @Test
    public void shouldCaptureDocumentPut() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteDocumentPut.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage()).isEqualTo("PUT testindex/testtype");
        assertThat(outgoingSpan.getPrefix()).isEqualTo("elasticsearch query: ");
        assertThat(outgoingSpan.getSuffix()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldCaptureDocumentGet() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteDocumentGet.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage()).isEqualTo("GET testindex/testtype");
        assertThat(outgoingSpan.getPrefix()).isEqualTo("elasticsearch query: ");
        assertThat(outgoingSpan.getSuffix()).startsWith(" [");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldCaptureDocumentUpdate() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteDocumentUpdate.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage()).isEqualTo("PUT testindex/testtype");
        assertThat(outgoingSpan.getPrefix()).isEqualTo("elasticsearch query: ");
        assertThat(outgoingSpan.getSuffix()).startsWith(" [");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldCaptureDocumentDelete() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteDocumentDelete.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage())
                .isEqualTo("DELETE testindex/testtype");
        assertThat(outgoingSpan.getPrefix()).isEqualTo("elasticsearch query: ");
        assertThat(outgoingSpan.getSuffix()).startsWith(" [");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldCaptureDocumentSearchWithoutSource() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteDocumentSearchWithoutSource.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage())
                .startsWith("SEARCH testindex/testtype {");
        assertThat(outgoingSpan.getPrefix()).isEqualTo("elasticsearch query: ");
        assertThat(outgoingSpan.getSuffix()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldCaptureDocumentSearchWithoutIndexesWithoutSource() throws Exception {
        // when
        IncomingSpan incomingSpan =
                container.execute(ExecuteDocumentSearchWithoutIndexesWithoutSource.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage())
                .startsWith("SEARCH _any/testtype {");
        assertThat(outgoingSpan.getPrefix()).isEqualTo("elasticsearch query: ");
        assertThat(outgoingSpan.getSuffix()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldCaptureDocumentSearchWithoutIndexesWithoutTypesWithoutSource()
            throws Exception {
        // when
        IncomingSpan incomingSpan = container
                .execute(ExecuteDocumentSearchWithoutIndexesWithoutTypesWithoutSource.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage()).startsWith("SEARCH / {");
        assertThat(outgoingSpan.getPrefix()).isEqualTo("elasticsearch query: ");
        assertThat(outgoingSpan.getSuffix()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldCaptureDocumentSearchWithMultipleIndexesWithMultipleTypesWithoutSource()
            throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(
                ExecuteDocumentSearchWithMultipleIndexesWithMultipleTypesWithoutSource.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage())
                .startsWith("SEARCH testindex,testindex2/testtype,testtype2 {");
        assertThat(outgoingSpan.getPrefix()).isEqualTo("elasticsearch query: ");
        assertThat(outgoingSpan.getSuffix()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldCaptureDocumentSearchWithQuery() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteDocumentSearchWithQuery.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage())
                .startsWith("SEARCH testindex/testtype {");
        assertThat(outgoingSpan.getPrefix()).isEqualTo("elasticsearch query: ");
        assertThat(outgoingSpan.getSuffix()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldCaptureDocumentSearchWithQueryAndSort() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteDocumentSearchWithQueryAndSort.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage())
                .startsWith("SEARCH testindex/testtype {");
        assertThat(outgoingSpan.getPrefix()).isEqualTo("elasticsearch query: ");
        assertThat(outgoingSpan.getSuffix()).isEmpty();

        assertThat(i.hasNext()).isFalse();
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
                    .get();
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
                    .get();
        }
    }

    public static class ExecuteDocumentSearchWithQuery implements AppUnderTest, TransactionMarker {

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
                    .get();
        }
    }

    public static class ExecuteDocumentSearchWithQueryAndSort
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
                    .setQuery(QueryBuilders.termQuery("xyz", "text"))
                    .addSort("abc", SortOrder.ASC)
                    .get();
        }
    }
}
