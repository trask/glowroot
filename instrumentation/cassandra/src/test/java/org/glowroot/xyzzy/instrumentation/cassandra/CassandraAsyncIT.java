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
package org.glowroot.xyzzy.instrumentation.cassandra;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.google.common.collect.Lists;
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

public class CassandraAsyncIT {

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
    public void shouldAsyncExecuteStatement() throws Exception {
        // when
        ServerSpan trace = container.execute(ExecuteAsyncStatement.class);

        // then
        checkTimers(trace, false, 1);

        Iterator<Span> i = trace.childSpans().iterator();

        ClientSpan clientSpan = (ClientSpan) i.next();
        assertThat(clientSpan.getMessage()).isEmpty();
        assertThat(clientSpan.getMessage()).isEqualTo("SELECT * FROM test.users");
        assertThat(clientSpan.getPrefix()).isEqualTo("cassandra query: ");
        assertThat(clientSpan.getSuffix()).isEqualTo(" => 10 rows");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldConcurrentlyAsyncExecuteSameStatement() throws Exception {
        // when
        ServerSpan trace = container.execute(ConcurrentlyExecuteSameAsyncStatement.class);

        // then
        checkTimers(trace, false, 100);

        Iterator<Span> i = trace.childSpans().iterator();

        for (int j = 0; j < 100; j++) {
            ClientSpan clientSpan = (ClientSpan) i.next();
            assertThat(clientSpan.getMessage()).isEmpty();
            assertThat(clientSpan.getMessage()).isEqualTo("SELECT * FROM test.users");
            assertThat(clientSpan.getPrefix()).isEqualTo("cassandra query: ");
            assertThat(clientSpan.getSuffix()).isEqualTo(" => 10 rows");
        }
        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldAsyncExecuteStatementReturningNoRecords() throws Exception {
        // when
        ServerSpan trace = container.execute(ExecuteAsyncStatementReturningNoRecords.class);

        // then
        checkTimers(trace, false, 1);

        Iterator<Span> i = trace.childSpans().iterator();

        ClientSpan clientSpan = (ClientSpan) i.next();
        assertThat(clientSpan.getMessage()).isEmpty();
        assertThat(clientSpan.getMessage()).isEqualTo("SELECT * FROM test.users where id = 12345");
        assertThat(clientSpan.getPrefix()).isEqualTo("cassandra query: ");
        assertThat(clientSpan.getSuffix()).isEqualTo(" => 0 rows");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldAsyncIterateUsingOneAndAll() throws Exception {
        // when
        ServerSpan trace = container.execute(AsyncIterateUsingOneAndAll.class);

        // then
        checkTimers(trace, false, 1);

        Iterator<Span> i = trace.childSpans().iterator();

        ClientSpan clientSpan = (ClientSpan) i.next();
        assertThat(clientSpan.getMessage()).isEmpty();
        assertThat(clientSpan.getMessage()).isEqualTo("SELECT * FROM test.users");
        assertThat(clientSpan.getPrefix()).isEqualTo("cassandra query: ");
        assertThat(clientSpan.getSuffix()).isEqualTo(" => 10 rows");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldAsyncExecuteBoundStatement() throws Exception {
        // when
        ServerSpan trace = container.execute(AsyncExecuteBoundStatement.class);

        // then
        checkTimers(trace, true, 1);

        Iterator<Span> i = trace.childSpans().iterator();

        ClientSpan clientSpan = (ClientSpan) i.next();
        assertThat(clientSpan.getMessage()).isEmpty();
        assertThat(clientSpan.getMessage())
                .isEqualTo("INSERT INTO test.users (id,  fname, lname) VALUES (?, ?, ?)");
        assertThat(clientSpan.getPrefix()).isEqualTo("cassandra query: ");
        assertThat(clientSpan.getSuffix()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldAsyncExecuteBatchStatement() throws Exception {
        // when
        ServerSpan trace = container.execute(AsyncExecuteBatchStatement.class);

        // then
        checkTimers(trace, true, 1);

        Iterator<Span> i = trace.childSpans().iterator();

        ClientSpan clientSpan = (ClientSpan) i.next();
        assertThat(clientSpan.getMessage()).isEmpty();
        assertThat(clientSpan.getMessage())
                .isEqualTo("[batch] INSERT INTO test.users (id,  fname, lname)"
                        + " VALUES (100, 'f100', 'l100'),"
                        + " INSERT INTO test.users (id,  fname, lname)"
                        + " VALUES (101, 'f101', 'l101'),"
                        + " 10 x INSERT INTO test.users (id,  fname, lname) VALUES (?, ?, ?),"
                        + " INSERT INTO test.users (id,  fname, lname)"
                        + " VALUES (300, 'f300', 'l300')");
        assertThat(clientSpan.getPrefix()).isEqualTo("cassandra query: ");
        assertThat(clientSpan.getSuffix()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    private static void checkTimers(ServerSpan trace, boolean prepared, int count) {
        ServerSpan.Timer rootTimer = trace.mainThreadRootTimer();
        List<String> timerNames = Lists.newArrayList();
        for (ServerSpan.Timer timer : rootTimer.childTimers()) {
            timerNames.add(timer.name());
        }
        Collections.sort(timerNames);
        if (prepared) {
            assertThat(timerNames).containsExactly("cassandra query", "cql prepare");
        } else {
            assertThat(timerNames).containsExactly("cassandra query");
        }
        for (ServerSpan.Timer timer : rootTimer.childTimers()) {
            assertThat(timer.childTimers()).isEmpty();
        }
        assertThat(trace.asyncTimers().size()).isEqualTo(1);
        ServerSpan.Timer asyncTimer = trace.asyncTimers().get(0);
        assertThat(asyncTimer.childTimers()).isEmpty();
        assertThat(asyncTimer.name()).isEqualTo("cassandra query");
        assertThat(asyncTimer.count()).isEqualTo(count);
    }

    public static class ExecuteAsyncStatement implements AppUnderTest, TransactionMarker {

        private Session session;

        @Override
        public void executeApp() throws Exception {
            session = Sessions.createSession();
            transactionMarker();
            Sessions.closeSession(session);
        }

        @Override
        public void transactionMarker() throws Exception {
            ResultSetFuture future = session.executeAsync("SELECT * FROM test.users");
            ResultSet results = future.get();
            for (Row row : results) {
                row.getInt("id");
            }
        }
    }

    public static class ConcurrentlyExecuteSameAsyncStatement
            implements AppUnderTest, TransactionMarker {

        private Session session;

        @Override
        public void executeApp() throws Exception {
            session = Sessions.createSession();
            transactionMarker();
            Sessions.closeSession(session);
        }

        @Override
        public void transactionMarker() throws Exception {
            List<ResultSetFuture> futures = Lists.newArrayList();
            for (int i = 0; i < 100; i++) {
                futures.add(session.executeAsync("SELECT * FROM test.users"));
            }
            for (ResultSetFuture future : futures) {
                ResultSet results = future.get();
                for (Row row : results) {
                    row.getInt("id");
                }
            }
        }
    }

    public static class ExecuteAsyncStatementReturningNoRecords
            implements AppUnderTest, TransactionMarker {

        private Session session;

        @Override
        public void executeApp() throws Exception {
            session = Sessions.createSession();
            transactionMarker();
            Sessions.closeSession(session);
        }

        @Override
        public void transactionMarker() throws Exception {
            ResultSet results =
                    session.executeAsync("SELECT * FROM test.users where id = 12345").get();
            for (Row row : results) {
                row.getInt("id");
            }
        }
    }

    public static class AsyncIterateUsingOneAndAll implements AppUnderTest, TransactionMarker {

        private Session session;

        @Override
        public void executeApp() throws Exception {
            session = Sessions.createSession();
            transactionMarker();
            Sessions.closeSession(session);
        }

        @Override
        public void transactionMarker() throws Exception {
            ResultSet results = session.executeAsync("SELECT * FROM test.users").get();
            results.one();
            results.one();
            results.one();
            results.all();
        }
    }

    public static class AsyncExecuteBoundStatement implements AppUnderTest, TransactionMarker {

        private Session session;

        @Override
        public void executeApp() throws Exception {
            session = Sessions.createSession();
            transactionMarker();
            Sessions.closeSession(session);
        }

        @Override
        public void transactionMarker() throws Exception {
            PreparedStatement preparedStatement =
                    session.prepare("INSERT INTO test.users (id,  fname, lname) VALUES (?, ?, ?)");
            BoundStatement boundStatement = new BoundStatement(preparedStatement);
            boundStatement.bind(100, "f100", "l100");
            session.executeAsync(boundStatement).get();
        }
    }

    public static class AsyncExecuteBatchStatement implements AppUnderTest, TransactionMarker {

        private Session session;

        @Override
        public void executeApp() throws Exception {
            session = Sessions.createSession();
            transactionMarker();
            Sessions.closeSession(session);
        }

        @Override
        public void transactionMarker() throws Exception {
            BatchStatement batchStatement = new BatchStatement();
            batchStatement.add(new SimpleStatement(
                    "INSERT INTO test.users (id,  fname, lname) VALUES (100, 'f100', 'l100')"));
            batchStatement.add(new SimpleStatement(
                    "INSERT INTO test.users (id,  fname, lname) VALUES (101, 'f101', 'l101')"));
            PreparedStatement preparedStatement =
                    session.prepare("INSERT INTO test.users (id,  fname, lname) VALUES (?, ?, ?)");
            for (int i = 200; i < 210; i++) {
                BoundStatement boundStatement = new BoundStatement(preparedStatement);
                boundStatement.bind(i, "f" + i, "l" + i);
                batchStatement.add(boundStatement);
            }
            batchStatement.add(new SimpleStatement(
                    "INSERT INTO test.users (id,  fname, lname) VALUES (300, 'f300', 'l300')"));
            session.executeAsync(batchStatement).get();
        }
    }
}
