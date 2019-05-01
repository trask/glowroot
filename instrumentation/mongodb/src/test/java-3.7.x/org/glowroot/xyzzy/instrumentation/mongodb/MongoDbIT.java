/**
 * Copyright 2018-2019 the original author or authors.
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
package org.glowroot.xyzzy.instrumentation.mongodb;

import java.util.Arrays;
import java.util.Iterator;

import com.google.common.collect.ImmutableList;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import org.glowroot.xyzzy.test.harness.AppUnderTest;
import org.glowroot.xyzzy.test.harness.Container;
import org.glowroot.xyzzy.test.harness.Containers;
import org.glowroot.xyzzy.test.harness.IncomingSpan;
import org.glowroot.xyzzy.test.harness.OutgoingSpan;
import org.glowroot.xyzzy.test.harness.Span;
import org.glowroot.xyzzy.test.harness.TransactionMarker;

import static org.assertj.core.api.Assertions.assertThat;

public class MongoDbIT {

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
    public void shouldCaptureCount() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteCount.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan span = (OutgoingSpan) i.next();
        assertThat(span.getMessage()).isEqualTo("count testdb.test");
        assertThat(span.getPrefix()).isEqualTo("mongodb query: ");
        assertThat(span.getSuffix()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldCaptureDistinct() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteDistinct.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan span = (OutgoingSpan) i.next();
        assertThat(span.getMessage()).isEqualTo("distinct testdb.test");
        assertThat(span.getPrefix()).isEqualTo("mongodb query: ");
        assertThat(span.getSuffix()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldCaptureFindZeroRecords() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteFindZeroRecords.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan span = (OutgoingSpan) i.next();
        assertThat(span.getMessage()).isEqualTo("find testdb.test");
        assertThat(span.getPrefix()).isEqualTo("mongodb query: ");
        assertThat(span.getSuffix()).isEqualTo(" => 0 rows");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldCaptureFindOneRecord() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteFindOneRecord.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan span = (OutgoingSpan) i.next();
        assertThat(span.getMessage()).isEqualTo("find testdb.test");
        assertThat(span.getPrefix()).isEqualTo("mongodb query: ");
        assertThat(span.getSuffix()).isEqualTo(" => 1 row");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldCaptureAggregate() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteAggregate.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan span = (OutgoingSpan) i.next();
        assertThat(span.getMessage()).isEqualTo("aggregate testdb.test");
        assertThat(span.getPrefix()).isEqualTo("mongodb query: ");
        assertThat(span.getSuffix()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    public static class ExecuteCount extends DoMongoDB {
        @Override
        @SuppressWarnings("deprecation")
        public void transactionMarker() {
            MongoDatabase database = mongoClient.getDatabase("testdb");
            MongoCollection<Document> collection = database.getCollection("test");
            // intentionally using deprecated method here so it will work with mongo driver 3.7.0
            collection.count();
        }
    }

    public static class ExecuteDistinct extends DoMongoDB {
        @Override
        public void transactionMarker() {
            MongoDatabase database = mongoClient.getDatabase("testdb");
            MongoCollection<Document> collection = database.getCollection("test");
            collection.distinct("abc", String.class);
        }
    }

    public static class ExecuteFindZeroRecords extends DoMongoDB {
        @Override
        public void transactionMarker() throws InterruptedException {
            MongoDatabase database = mongoClient.getDatabase("testdb");
            MongoCollection<Document> collection = database.getCollection("test");
            MongoCursor<Document> i = collection.find().iterator();
            while (i.hasNext()) {
            }
        }
    }

    public static class ExecuteFindOneRecord extends DoMongoDB {

        @Override
        protected void beforeTransactionMarker() {
            MongoDatabase database = mongoClient.getDatabase("testdb");
            MongoCollection<Document> collection = database.getCollection("test");
            Document document = new Document("test1", "test2")
                    .append("test3", "test4");
            collection.insertOne(document);
        }

        @Override
        public void transactionMarker() throws InterruptedException {
            MongoDatabase database = mongoClient.getDatabase("testdb");
            MongoCollection<Document> collection = database.getCollection("test");
            MongoCursor<Document> i = collection.find().iterator();
            while (i.hasNext()) {
                i.next();
            }
        }
    }

    public static class ExecuteAggregate extends DoMongoDB {
        @Override
        public void transactionMarker() {
            MongoDatabase database = mongoClient.getDatabase("testdb");
            MongoCollection<Document> collection = database.getCollection("test");
            collection.aggregate(ImmutableList.of());
        }
    }

    public static class ExecuteInsert extends DoMongoDB {
        @Override
        public void transactionMarker() {
            MongoDatabase database = mongoClient.getDatabase("testdb");
            MongoCollection<Document> collection = database.getCollection("test");
            Document document = new Document("test1", "test2")
                    .append("test3", "test4");
            collection.insertOne(document);
        }
    }

    private abstract static class DoMongoDB implements AppUnderTest, TransactionMarker {

        protected MongoClient mongoClient;

        @Override
        public void executeApp() throws Exception {
            GenericContainer<?> mongo = new GenericContainer<>("mongo:4.0.3");
            mongo.setExposedPorts(Arrays.asList(27017));
            mongo.start();
            try {
                mongoClient = MongoClients.create("mongodb://" + mongo.getContainerIpAddress() + ":"
                        + mongo.getMappedPort(27017));
                beforeTransactionMarker();
                transactionMarker();
            } finally {
                mongo.close();
            }
        }

        protected void beforeTransactionMarker() {}
    }
}
