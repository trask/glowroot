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

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
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
    public void shouldCaptureInsert() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteInsert.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan span = (OutgoingSpan) i.next();
        assertThat(span.getMessage()).isEqualTo("insert testdb.test");
        assertThat(span.getPrefix()).isEqualTo("mongodb query: ");
        assertThat(span.getSuffix()).isEmpty();

        assertThat(i.hasNext()).isFalse();
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
    public void shouldCaptureFind() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteFind.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan span = (OutgoingSpan) i.next();
        assertThat(span.getMessage()).isEqualTo("find testdb.test");
        assertThat(span.getPrefix()).isEqualTo("mongodb query: ");
        assertThat(span.getSuffix()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    public static class ExecuteInsert extends DoMongoDB {
        @Override
        public void transactionMarker() {
            @SuppressWarnings("deprecation")
            DB database = mongoClient.getDB("testdb");
            DBCollection collection = database.getCollection("test");
            BasicDBObject document = new BasicDBObject("test1", "test2")
                    .append("test3", "test4");
            collection.insert(document);
        }
    }

    public static class ExecuteCount extends DoMongoDB {
        @Override
        public void transactionMarker() {
            @SuppressWarnings("deprecation")
            DB database = mongoClient.getDB("testdb");
            DBCollection collection = database.getCollection("test");
            collection.getCount();
        }
    }

    public static class ExecuteFind extends DoMongoDB {
        @Override
        public void transactionMarker() {
            @SuppressWarnings("deprecation")
            DB database = mongoClient.getDB("testdb");
            DBCollection collection = database.getCollection("test");
            collection.find();
        }
    }

    private abstract static class DoMongoDB implements AppUnderTest, TransactionMarker {

        protected Mongo mongoClient;

        @Override
        @SuppressWarnings("deprecation")
        public void executeApp(Serializable... args) throws Exception {
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
