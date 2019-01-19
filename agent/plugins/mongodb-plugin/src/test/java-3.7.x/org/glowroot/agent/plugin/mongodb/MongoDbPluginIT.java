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
package org.glowroot.agent.plugin.mongodb;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
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

import org.glowroot.agent.it.harness.AppUnderTest;
import org.glowroot.agent.it.harness.Container;
import org.glowroot.agent.it.harness.Containers;
import org.glowroot.agent.it.harness.TransactionMarker;
import org.glowroot.wire.api.model.AggregateOuterClass.Aggregate;
import org.glowroot.wire.api.model.TraceOuterClass.Trace;

import static org.assertj.core.api.Assertions.assertThat;

public class MongoDbPluginIT {

    private static final Pattern DEST_PATTERN =
            Pattern.compile("MongoDB \\[localhost:[0-9]+\\]");

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
    public void shouldCaptureCount() throws Exception {
        shouldCaptureCount(ExecuteCount.class);
    }

    @Test
    public void shouldCaptureDistinct() throws Exception {
        shouldCaptureDistinct(ExecuteDistinct.class);
    }

    @Test
    public void shouldCaptureFind() throws Exception {
        shouldCaptureFind(ExecuteFind.class);
    }

    @Test
    public void shouldCaptureAggregate() throws Exception {
        shouldCaptureAggregate(ExecuteAggregate.class);
    }

    @Test
    public void shouldCaptureInsert() throws Exception {
        shouldCaptureInsert(ExecuteInsert.class, "insertOne");
    }

    @Test
    public void shouldCaptureCountAlt() throws Exception {
        shouldCaptureCount(ExecuteCountAlt.class);
    }

    @Test
    public void shouldCaptureDistinctAlt() throws Exception {
        shouldCaptureDistinct(ExecuteDistinctAlt.class);
    }

    @Test
    public void shouldCaptureFindAlt() throws Exception {
        shouldCaptureFind(ExecuteFindAlt.class);
    }

    @Test
    public void shouldCaptureAggregateAlt() throws Exception {
        shouldCaptureAggregate(ExecuteAggregateAlt.class);
    }

    @Test
    public void shouldCaptureInsertAlt() throws Exception {
        shouldCaptureInsert(ExecuteInsertAlt.class, "insertOne");
    }

    @Test
    public void shouldCaptureCountOld() throws Exception {
        shouldCaptureCount(ExecuteCountOld.class);
    }

    @Test
    public void shouldCaptureDistinctOld() throws Exception {
        shouldCaptureDistinct(ExecuteDistinctOld.class);
    }

    @Test
    public void shouldCaptureFindOld() throws Exception {
        shouldCaptureFind(ExecuteFindOld.class);
    }

    @Test
    public void shouldCaptureInsertOld() throws Exception {
        shouldCaptureInsert(ExecuteInsertOld.class, "insert");
    }

    private void shouldCaptureCount(Class<? extends AppUnderTest> appUnderTestClass)
            throws Exception {
        // when
        Trace trace = container.execute(appUnderTestClass);

        // then
        checkTimers(trace);

        Iterator<Trace.Entry> i = trace.getEntryList().iterator();
        List<Trace.SharedQueryText> sharedQueryTexts = trace.getSharedQueryTextList();

        Trace.Entry entry = i.next();
        assertThat(entry.getDepth()).isEqualTo(0);
        assertThat(entry.getMessage()).isEmpty();
        assertThat(sharedQueryTexts.get(entry.getQueryEntryMessage().getSharedQueryTextIndex())
                .getFullText()).isEqualTo("count testdb.test");
        assertThat(entry.getQueryEntryMessage().getPrefix()).isEqualTo("mongodb query: ");
        assertThat(entry.getQueryEntryMessage().getSuffix()).isEmpty();

        assertThat(i.hasNext()).isFalse();

        Iterator<Aggregate.Query> j = trace.getQueryList().iterator();

        Aggregate.Query query = j.next();
        assertThat(query.getDest()).matches(DEST_PATTERN);
        assertThat(sharedQueryTexts.get(query.getSharedQueryTextIndex()).getFullText())
                .isEqualTo("count testdb.test");
        assertThat(query.getExecutionCount()).isEqualTo(1);
        assertThat(query.hasTotalRows()).isFalse();

        assertThat(j.hasNext()).isFalse();
    }

    private void shouldCaptureDistinct(Class<? extends AppUnderTest> appUnderTestClass)
            throws Exception {
        // when
        Trace trace = container.execute(appUnderTestClass);

        // then
        checkTimers(trace);

        Iterator<Trace.Entry> i = trace.getEntryList().iterator();
        List<Trace.SharedQueryText> sharedQueryTexts = trace.getSharedQueryTextList();

        Trace.Entry entry = i.next();
        assertThat(entry.getDepth()).isEqualTo(0);
        assertThat(entry.getMessage()).isEmpty();
        assertThat(sharedQueryTexts.get(entry.getQueryEntryMessage().getSharedQueryTextIndex())
                .getFullText()).isEqualTo("distinct testdb.test");
        assertThat(entry.getQueryEntryMessage().getPrefix()).isEqualTo("mongodb query: ");
        assertThat(entry.getQueryEntryMessage().getSuffix()).isEmpty();

        assertThat(i.hasNext()).isFalse();

        Iterator<Aggregate.Query> j = trace.getQueryList().iterator();

        Aggregate.Query query = j.next();
        assertThat(query.getDest()).matches(DEST_PATTERN);
        assertThat(sharedQueryTexts.get(query.getSharedQueryTextIndex()).getFullText())
                .isEqualTo("distinct testdb.test");
        assertThat(query.getExecutionCount()).isEqualTo(1);
        assertThat(query.hasTotalRows()).isFalse();

        assertThat(j.hasNext()).isFalse();
    }

<<<<<<< HEAD
    @Test
    public void shouldCaptureFindZeroRecords() throws Exception {
        // when
        Trace trace = container.execute(ExecuteFindZeroRecords.class);
=======
    private void shouldCaptureFind(Class<? extends AppUnderTest> appUnderTestClass)
            throws Exception {
        // when
        Trace trace = container.execute(appUnderTestClass);
>>>>>>> 4f2e7d59be... Change query and service call type to dest

        // then
        checkTimers(trace);

        Iterator<Trace.Entry> i = trace.getEntryList().iterator();
        List<Trace.SharedQueryText> sharedQueryTexts = trace.getSharedQueryTextList();

        Trace.Entry entry = i.next();
        assertThat(entry.getDepth()).isEqualTo(0);
        assertThat(entry.getMessage()).isEmpty();
        assertThat(sharedQueryTexts.get(entry.getQueryEntryMessage().getSharedQueryTextIndex())
                .getFullText()).isEqualTo("find testdb.test");
        assertThat(entry.getQueryEntryMessage().getPrefix()).isEqualTo("mongodb query: ");
        assertThat(entry.getQueryEntryMessage().getSuffix()).isEqualTo(" => 0 rows");

        assertThat(i.hasNext()).isFalse();

        Iterator<Aggregate.Query> j = trace.getQueryList().iterator();

        Aggregate.Query query = j.next();
        assertThat(query.getDest()).matches(DEST_PATTERN);
        assertThat(sharedQueryTexts.get(query.getSharedQueryTextIndex()).getFullText())
                .isEqualTo("find testdb.test");
        assertThat(query.getExecutionCount()).isEqualTo(1);
        assertThat(query.hasTotalRows()).isTrue();
        assertThat(query.getTotalRows().getValue()).isEqualTo(0);

        assertThat(j.hasNext()).isFalse();
    }

    @Test
    public void shouldCaptureFindOneRecord() throws Exception {
        // when
        Trace trace = container.execute(ExecuteFindOneRecord.class);

        // then
        checkTimers(trace);

        Iterator<Trace.Entry> i = trace.getEntryList().iterator();
        List<Trace.SharedQueryText> sharedQueryTexts = trace.getSharedQueryTextList();

        Trace.Entry entry = i.next();
        assertThat(entry.getDepth()).isEqualTo(0);
        assertThat(entry.getMessage()).isEmpty();
        assertThat(sharedQueryTexts.get(entry.getQueryEntryMessage().getSharedQueryTextIndex())
                .getFullText()).isEqualTo("find testdb.test");
        assertThat(entry.getQueryEntryMessage().getPrefix()).isEqualTo("mongodb query: ");
        assertThat(entry.getQueryEntryMessage().getSuffix()).isEqualTo(" => 1 row");

        assertThat(i.hasNext()).isFalse();

        Iterator<Aggregate.Query> j = trace.getQueryList().iterator();

        Aggregate.Query query = j.next();
        assertThat(query.getType()).isEqualTo("MongoDB");
        assertThat(sharedQueryTexts.get(query.getSharedQueryTextIndex()).getFullText())
                .isEqualTo("find testdb.test");
        assertThat(query.getExecutionCount()).isEqualTo(1);
        assertThat(query.hasTotalRows()).isTrue();
        assertThat(query.getTotalRows().getValue()).isEqualTo(1);

        assertThat(j.hasNext()).isFalse();
    }

    private void shouldCaptureAggregate(Class<? extends AppUnderTest> appUnderTestClass)
            throws Exception {
        // when
        Trace trace = container.execute(appUnderTestClass);

        // then
        checkTimers(trace);

        Iterator<Trace.Entry> i = trace.getEntryList().iterator();
        List<Trace.SharedQueryText> sharedQueryTexts = trace.getSharedQueryTextList();

        Trace.Entry entry = i.next();
        assertThat(entry.getDepth()).isEqualTo(0);
        assertThat(entry.getMessage()).isEmpty();
        assertThat(sharedQueryTexts.get(entry.getQueryEntryMessage().getSharedQueryTextIndex())
                .getFullText()).isEqualTo("aggregate testdb.test");
        assertThat(entry.getQueryEntryMessage().getPrefix()).isEqualTo("mongodb query: ");
        assertThat(entry.getQueryEntryMessage().getSuffix()).isEmpty();

        assertThat(i.hasNext()).isFalse();

        Iterator<Aggregate.Query> j = trace.getQueryList().iterator();

        Aggregate.Query query = j.next();
        assertThat(query.getDest()).matches(DEST_PATTERN);
        assertThat(sharedQueryTexts.get(query.getSharedQueryTextIndex()).getFullText())
                .isEqualTo("aggregate testdb.test");
        assertThat(query.getExecutionCount()).isEqualTo(1);
        assertThat(query.hasTotalRows()).isFalse();

        assertThat(j.hasNext()).isFalse();
    }

    private void shouldCaptureInsert(Class<? extends AppUnderTest> appUnderTestClass,
            String methodName) throws Exception {
        // when
        Trace trace = container.execute(appUnderTestClass);

        // then
        checkTimers(trace);

        Iterator<Trace.Entry> i = trace.getEntryList().iterator();
        List<Trace.SharedQueryText> sharedQueryTexts = trace.getSharedQueryTextList();

        Trace.Entry entry = i.next();
        assertThat(entry.getDepth()).isEqualTo(0);
        assertThat(entry.getMessage()).isEmpty();
        assertThat(sharedQueryTexts.get(entry.getQueryEntryMessage().getSharedQueryTextIndex())
                .getFullText()).isEqualTo(methodName + " testdb.test");
        assertThat(entry.getQueryEntryMessage().getPrefix()).isEqualTo("mongodb query: ");
        assertThat(entry.getQueryEntryMessage().getSuffix()).isEmpty();

        assertThat(i.hasNext()).isFalse();

        Iterator<Aggregate.Query> j = trace.getQueryList().iterator();

        Aggregate.Query query = j.next();
        assertThat(query.getDest()).matches(DEST_PATTERN);
        assertThat(sharedQueryTexts.get(query.getSharedQueryTextIndex()).getFullText())
                .isEqualTo(methodName + " testdb.test");
        assertThat(query.getExecutionCount()).isEqualTo(1);
        assertThat(query.hasTotalRows()).isFalse();

        assertThat(j.hasNext()).isFalse();
    }

    private static void checkTimers(Trace trace) {
        Trace.Timer rootTimer = trace.getHeader().getMainThreadRootTimer();
        List<String> timerNames = Lists.newArrayList();
        for (Trace.Timer timer : rootTimer.getChildTimerList()) {
            timerNames.add(timer.getName());
        }
        Collections.sort(timerNames);
        assertThat(timerNames).containsExactly("mongodb query");
        for (Trace.Timer timer : rootTimer.getChildTimerList()) {
            assertThat(timer.getChildTimerList()).isEmpty();
        }
        assertThat(trace.getHeader().getAsyncTimerCount()).isZero();
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

    public static class ExecuteCountAlt extends DoMongoDBAlt {
        @Override
        @SuppressWarnings("deprecation")
        public void transactionMarker() {
            MongoDatabase database = mongoClient.getDatabase("testdb");
            MongoCollection<Document> collection = database.getCollection("test");
            // intentionally using deprecated method here so it will work with mongo driver 3.7.0
            collection.count();
        }
    }

    public static class ExecuteDistinctAlt extends DoMongoDBAlt {
        @Override
        public void transactionMarker() {
            MongoDatabase database = mongoClient.getDatabase("testdb");
            MongoCollection<Document> collection = database.getCollection("test");
            collection.distinct("abc", String.class);
        }
    }

    public static class ExecuteFindAlt extends DoMongoDBAlt {
        @Override
        public void transactionMarker() {
            MongoDatabase database = mongoClient.getDatabase("testdb");
            MongoCollection<Document> collection = database.getCollection("test");
            collection.find();
        }
    }

    public static class ExecuteAggregateAlt extends DoMongoDBAlt {
        @Override
        public void transactionMarker() {
            MongoDatabase database = mongoClient.getDatabase("testdb");
            MongoCollection<Document> collection = database.getCollection("test");
            collection.aggregate(ImmutableList.of());
        }
    }

    public static class ExecuteInsertAlt extends DoMongoDBAlt {
        @Override
        public void transactionMarker() {
            MongoDatabase database = mongoClient.getDatabase("testdb");
            MongoCollection<Document> collection = database.getCollection("test");
            Document document = new Document("test1", "test2")
                    .append("test3", "test4");
            collection.insertOne(document);
        }
    }

    public static class ExecuteCountOld extends DoMongoDBOld {
        @Override
        @SuppressWarnings("deprecation")
        public void transactionMarker() {
            DB db = mongoClient.getDB("testdb");
            DBCollection collection = db.getCollection("test");
            collection.count();
        }
    }

    @SuppressWarnings("deprecation")
    public static class ExecuteDistinctOld extends DoMongoDBOld {
        @Override
        public void transactionMarker() {
            DB db = mongoClient.getDB("testdb");
            DBCollection collection = db.getCollection("test");
            collection.distinct("abc");
        }
    }

    @SuppressWarnings("deprecation")
    public static class ExecuteFindOld extends DoMongoDBOld {
        @Override
        public void transactionMarker() {
            DB db = mongoClient.getDB("testdb");
            DBCollection collection = db.getCollection("test");
            collection.find();
        }
    }

    @SuppressWarnings("deprecation")
    public static class ExecuteInsertOld extends DoMongoDBOld {
        @Override
        public void transactionMarker() {
            DB db = mongoClient.getDB("testdb");
            DBCollection collection = db.getCollection("test");
            BasicDBObject document = new BasicDBObject("test1", "test2")
                    .append("test3", "test4");
            collection.insert(document);
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

    private abstract static class DoMongoDBAlt implements AppUnderTest, TransactionMarker {

        protected com.mongodb.MongoClient mongoClient;

        @Override
        public void executeApp() throws Exception {
            GenericContainer<?> mongo = new GenericContainer<>("mongo:4.0.3");
            mongo.setExposedPorts(Arrays.asList(27017));
            mongo.start();
            try {
                mongoClient = new com.mongodb.MongoClient(mongo.getContainerIpAddress(),
                        +mongo.getMappedPort(27017));
                transactionMarker();
            } finally {
                mongo.close();
            }
        }
    }

    @SuppressWarnings("deprecation")
    private abstract static class DoMongoDBOld implements AppUnderTest, TransactionMarker {

        protected Mongo mongoClient;

        @Override
        public void executeApp() throws Exception {
            GenericContainer<?> mongo = new GenericContainer<>("mongo:4.0.3");
            mongo.setExposedPorts(Arrays.asList(27017));
            mongo.start();
            try {
                mongoClient = new Mongo(mongo.getContainerIpAddress(), mongo.getMappedPort(27017));
                transactionMarker();
            } finally {
                mongo.close();
            }
        }
    }
}
