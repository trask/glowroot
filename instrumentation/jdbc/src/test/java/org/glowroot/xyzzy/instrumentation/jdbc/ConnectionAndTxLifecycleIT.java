/*
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
package org.glowroot.xyzzy.instrumentation.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.Sets;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbcp.DelegatingConnection;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.agent.it.harness.AppUnderTest;
import org.glowroot.agent.it.harness.Container;
import org.glowroot.agent.it.harness.Containers;
import org.glowroot.agent.it.harness.TransactionMarker;
import org.glowroot.agent.it.harness.model.Trace;

import static org.assertj.core.api.Assertions.assertThat;

public class ConnectionAndTxLifecycleIT {

    private static final String INSTRUMENTATION_ID = "jdbc";

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
    public void testConnectionLifecycle() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID,
                "captureConnectionLifecycleTraceEntries", true);

        // when
        Trace trace = container.execute(ExecuteGetConnectionAndConnectionClose.class);

        // then
        Iterator<Trace.Entry> i = trace.entries().iterator();

        Trace.Entry entry = i.next();
        assertThat(entry.depth()).isEqualTo(0);
        assertThat(entry.message()).isEqualTo("jdbc get connection");

        entry = i.next();
        assertThat(entry.depth()).isEqualTo(0);
        assertThat(entry.message()).isEqualTo("jdbc connection close");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testConnectionLifecycleDisabled() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID, "captureGetConnection", false);
        container.setInstrumentationProperty(INSTRUMENTATION_ID, "captureConnectionClose", false);

        // when
        Trace trace = container.execute(ExecuteGetConnectionAndConnectionClose.class);

        // then
        Trace.Timer rootTimer = trace.mainThreadRootTimer();
        assertThat(rootTimer.childTimers()).isEmpty();
        assertThat(trace.entries()).isEmpty();
    }

    @Test
    public void testConnectionLifecyclePartiallyDisabled() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID, "captureConnectionClose", true);

        // when
        Trace trace = container.execute(ExecuteGetConnectionAndConnectionClose.class);

        // then
        Trace.Timer rootTimer = trace.mainThreadRootTimer();
        assertThat(rootTimer.childTimers()).hasSize(2);
        // ordering is by total desc, so order is not fixed
        Set<String> childTimerNames = Sets.newHashSet();
        childTimerNames.add(rootTimer.childTimers().get(0).name());
        childTimerNames.add(rootTimer.childTimers().get(1).name());
        assertThat(childTimerNames).containsOnly("jdbc get connection", "jdbc connection close");
        assertThat(trace.entries()).isEmpty();
    }

    @Test
    public void testConnectionLifecycleGetConnectionThrows() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID,
                "captureConnectionLifecycleTraceEntries", true);

        // when
        Trace trace = container.execute(ExecuteGetConnectionOnThrowingDataSource.class);

        // then
        Iterator<Trace.Entry> i = trace.entries().iterator();

        Trace.Entry entry = i.next();
        assertThat(entry.depth()).isEqualTo(0);
        assertThat(entry.message()).isEqualTo("jdbc get connection");

        assertThat(entry.error().message())
                .isEqualTo("java.sql.SQLException: A getconnection failure");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testConnectionLifecycleGetConnectionThrowsDisabled() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID, "captureGetConnection", false);
        container.setInstrumentationProperty(INSTRUMENTATION_ID, "captureConnectionClose", false);

        // when
        Trace trace = container.execute(ExecuteGetConnectionOnThrowingDataSource.class);

        // then
        Trace.Timer rootTimer = trace.mainThreadRootTimer();
        assertThat(rootTimer.childTimers()).isEmpty();
        assertThat(trace.entries()).isEmpty();
    }

    @Test
    public void testConnectionLifecycleGetConnectionThrowsPartiallyDisabled() throws Exception {
        // when
        Trace trace = container.execute(ExecuteGetConnectionOnThrowingDataSource.class);

        // then
        Trace.Timer rootTimer = trace.mainThreadRootTimer();
        assertThat(rootTimer.childTimers()).hasSize(1);
        assertThat(rootTimer.childTimers().get(0).name()).isEqualTo("jdbc get connection");
        assertThat(trace.entries()).isEmpty();
    }

    @Test
    public void testConnectionLifecycleCloseConnectionThrows() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID,
                "captureConnectionLifecycleTraceEntries", true);

        // when
        Trace trace = container.execute(ExecuteCloseConnectionOnThrowingDataSource.class);

        // then
        Iterator<Trace.Entry> i = trace.entries().iterator();

        Trace.Entry entry = i.next();
        assertThat(entry.depth()).isEqualTo(0);
        assertThat(entry.message()).isEqualTo("jdbc get connection");

        entry = i.next();
        assertThat(entry.depth()).isEqualTo(0);
        assertThat(entry.message()).isEqualTo("jdbc connection close");

        assertThat(entry.error().message())
                .isEqualTo("java.sql.SQLException: A close failure");

        entry = i.next();
        assertThat(entry.message()).startsWith("Resource leak");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testConnectionLifecycleCloseConnectionThrowsDisabled() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID, "captureGetConnection", false);
        container.setInstrumentationProperty(INSTRUMENTATION_ID, "captureConnectionClose", false);

        // when
        Trace trace = container.execute(ExecuteCloseConnectionOnThrowingDataSource.class);

        // then
        Trace.Timer rootTimer = trace.mainThreadRootTimer();
        assertThat(rootTimer.childTimers()).isEmpty();

        Iterator<Trace.Entry> i = trace.entries().iterator();

        Trace.Entry entry = i.next();
        assertThat(entry.message()).startsWith("Resource leak");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testConnectionLifecycleCloseConnectionThrowsPartiallyDisabled() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID, "captureConnectionClose", true);

        // when
        Trace trace = container.execute(ExecuteCloseConnectionOnThrowingDataSource.class);

        // then
        Trace.Timer rootTimer = trace.mainThreadRootTimer();
        assertThat(rootTimer.childTimers()).hasSize(2);
        // ordering is by total desc, so order is not fixed
        Set<String> childTimerNames = Sets.newHashSet();
        childTimerNames.add(rootTimer.childTimers().get(0).name());
        childTimerNames.add(rootTimer.childTimers().get(1).name());
        assertThat(childTimerNames).containsOnly("jdbc get connection", "jdbc connection close");

        Iterator<Trace.Entry> i = trace.entries().iterator();

        Trace.Entry entry = i.next();
        assertThat(entry.message()).startsWith("Resource leak");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testTransactionLifecycle() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID,
                "captureTransactionLifecycleTraceEntries", true);

        // when
        Trace trace = container.execute(ExecuteSetAutoCommit.class);

        // then
        Iterator<Trace.Entry> i = trace.entries().iterator();

        Trace.Entry entry = i.next();
        assertThat(entry.depth()).isEqualTo(0);
        assertThat(entry.message()).isEqualTo("jdbc set autocommit: false");

        entry = i.next();
        assertThat(entry.depth()).isEqualTo(0);
        assertThat(entry.message()).isEqualTo("jdbc set autocommit: true");

        if (i.hasNext()) {
            entry = i.next();
            assertThat(entry.depth()).isEqualTo(0);
            assertThat(entry.message()).isEqualTo("jdbc commit");

        }

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testTransactionLifecycleThrowing() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID,
                "captureTransactionLifecycleTraceEntries", true);

        // when
        Trace trace = container.execute(ExecuteSetAutoCommitThrowing.class);

        // then
        Iterator<Trace.Entry> i = trace.entries().iterator();

        Trace.Entry entry = i.next();
        assertThat(entry.depth()).isEqualTo(0);
        assertThat(entry.message()).isEqualTo("jdbc set autocommit: false");

        assertThat(entry.error().message())
                .isEqualTo("java.sql.SQLException: A setautocommit failure");

        entry = i.next();
        assertThat(entry.depth()).isEqualTo(0);
        assertThat(entry.message()).isEqualTo("jdbc set autocommit: true");

        assertThat(entry.error().message())
                .isEqualTo("java.sql.SQLException: A setautocommit failure");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testConnectionLifecycleAndTransactionLifecycleTogether() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID,
                "captureConnectionLifecycleTraceEntries", true);
        container.setInstrumentationProperty(INSTRUMENTATION_ID,
                "captureTransactionLifecycleTraceEntries", true);

        // when
        Trace trace = container.execute(ExecuteGetConnectionAndConnectionClose.class);

        // then
        Iterator<Trace.Entry> i = trace.entries().iterator();

        Trace.Entry entry = i.next();
        assertThat(entry.depth()).isEqualTo(0);
        assertThat(entry.message()).isEqualTo("jdbc get connection (autocommit: true)");

        entry = i.next();
        assertThat(entry.depth()).isEqualTo(0);
        assertThat(entry.message()).isEqualTo("jdbc connection close");

        assertThat(i.hasNext()).isFalse();
    }

    public static class ExecuteGetConnectionAndConnectionClose
            implements AppUnderTest, TransactionMarker {
        private BasicDataSource dataSource;
        @Override
        public void executeApp() throws Exception {
            dataSource = new BasicDataSource();
            dataSource.setDriverClassName("org.hsqldb.jdbc.JDBCDriver");
            dataSource.setUrl("jdbc:hsqldb:mem:test");
            // BasicDataSource opens and closes a test connection on first getConnection(),
            // so just getting that out of the way before starting transaction
            dataSource.getConnection().close();
            transactionMarker();
        }
        @Override
        public void transactionMarker() throws Exception {
            dataSource.getConnection().close();
        }
    }

    public static class ExecuteGetConnectionOnThrowingDataSource
            implements AppUnderTest, TransactionMarker {
        private BasicDataSource dataSource;
        @Override
        public void executeApp() throws Exception {
            dataSource = new BasicDataSource() {
                @Override
                public Connection getConnection() throws SQLException {
                    throw new SQLException("A getconnection failure");
                }
            };
            dataSource.setDriverClassName("org.hsqldb.jdbc.JDBCDriver");
            dataSource.setUrl("jdbc:hsqldb:mem:test");
            transactionMarker();
        }
        @Override
        public void transactionMarker() throws Exception {
            try {
                dataSource.getConnection();
            } catch (SQLException e) {
            }
        }
    }

    public static class ExecuteCloseConnectionOnThrowingDataSource
            implements AppUnderTest, TransactionMarker {
        private BasicDataSource dataSource;
        @Override
        public void executeApp() throws Exception {
            dataSource = new BasicDataSource() {
                private boolean first = true;
                @Override
                public Connection getConnection() throws SQLException {
                    if (first) {
                        // BasicDataSource opens and closes a test connection on first
                        // getConnection()
                        first = false;
                        return super.getConnection();
                    }
                    return new DelegatingConnection(super.getConnection()) {
                        @Override
                        public void close() throws SQLException {
                            throw new SQLException("A close failure");
                        }
                    };
                }
            };
            dataSource.setDriverClassName("org.hsqldb.jdbc.JDBCDriver");
            dataSource.setUrl("jdbc:hsqldb:mem:test");
            // BasicDataSource opens and closes a test connection on first getConnection(),
            // so just getting that out of the way before starting transaction
            dataSource.getConnection().close();
            transactionMarker();
        }
        @Override
        public void transactionMarker() throws Exception {
            try {
                dataSource.getConnection().close();
            } catch (SQLException e) {
            }
        }
    }

    public static class ExecuteSetAutoCommit implements AppUnderTest, TransactionMarker {
        private Connection connection;
        @Override
        public void executeApp() throws Exception {
            connection = Connections.createConnection();
            try {
                transactionMarker();
            } finally {
                Connections.closeConnection(connection);
            }
        }
        @Override
        public void transactionMarker() throws Exception {
            connection.setAutoCommit(false);
            connection.setAutoCommit(true);
        }
    }

    public static class ExecuteSetAutoCommitThrowing implements AppUnderTest, TransactionMarker {
        private Connection connection;
        @Override
        public void executeApp() throws Exception {
            connection = new DelegatingConnection(Connections.createConnection()) {
                @Override
                public void setAutoCommit(boolean autoCommit) throws SQLException {
                    throw new SQLException("A setautocommit failure");
                }
            };
            try {
                transactionMarker();
            } finally {
                Connections.closeConnection(connection);
            }
        }
        @Override
        public void transactionMarker() {
            try {
                connection.setAutoCommit(false);
            } catch (SQLException e) {
            }
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
            }
        }
    }
}
