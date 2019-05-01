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
import java.sql.Statement;
import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.Sets;
import org.apache.commons.dbcp.DelegatingConnection;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.xyzzy.test.harness.AppUnderTest;
import org.glowroot.xyzzy.test.harness.OutgoingSpan;
import org.glowroot.xyzzy.test.harness.Container;
import org.glowroot.xyzzy.test.harness.Containers;
import org.glowroot.xyzzy.test.harness.LocalSpan;
import org.glowroot.xyzzy.test.harness.IncomingSpan;
import org.glowroot.xyzzy.test.harness.Span;
import org.glowroot.xyzzy.test.harness.TransactionMarker;

import static org.assertj.core.api.Assertions.assertThat;

public class CommitRollbackIT {

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
    public void testCommit() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteJdbcCommit.class);

        // then
        IncomingSpan.Timer rootTimer = incomingSpan.mainThreadRootTimer();
        assertThat(rootTimer.name()).isEqualTo("mock trace marker");
        assertThat(rootTimer.childTimers()).hasSize(2);
        // ordering is by total desc, so order is not fixed
        Set<String> childTimerNames = Sets.newHashSet();
        childTimerNames.add(rootTimer.childTimers().get(0).name());
        childTimerNames.add(rootTimer.childTimers().get(1).name());
        assertThat(childTimerNames).containsOnly("jdbc query", "jdbc commit");

        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage()).isEmpty();
        assertThat(outgoingSpan.getMessage())
                .isEqualTo("insert into employee (name) values ('john doe')");
        assertThat(outgoingSpan.getPrefix()).isEqualTo("jdbc query: ");
        assertThat(outgoingSpan.getSuffix()).isEmpty();

        LocalSpan localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo("jdbc commit");
        assertThat(localSpan.childSpans()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testCommitThrowing() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteJdbcCommitThrowing.class);

        // then
        IncomingSpan.Timer rootTimer = incomingSpan.mainThreadRootTimer();
        assertThat(rootTimer.name()).isEqualTo("mock trace marker");
        assertThat(rootTimer.childTimers()).hasSize(2);
        // ordering is by total desc, so order is not fixed
        Set<String> childTimerNames = Sets.newHashSet();
        childTimerNames.add(rootTimer.childTimers().get(0).name());
        childTimerNames.add(rootTimer.childTimers().get(1).name());
        assertThat(childTimerNames).containsOnly("jdbc query", "jdbc commit");

        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage()).isEmpty();
        assertThat(outgoingSpan.getMessage())
                .isEqualTo("insert into employee (name) values ('john doe')");
        assertThat(outgoingSpan.getPrefix()).isEqualTo("jdbc query: ");
        assertThat(outgoingSpan.getSuffix()).isEmpty();

        LocalSpan localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo("jdbc commit");
        assertThat(localSpan.childSpans()).isEmpty();

        assertThat(outgoingSpan.getError().message())
                .isEqualTo("java.sql.SQLException: A commit failure");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testRollback() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteJdbcRollback.class);

        // then
        IncomingSpan.Timer rootTimer = incomingSpan.mainThreadRootTimer();
        assertThat(rootTimer.name()).isEqualTo("mock trace marker");
        assertThat(rootTimer.childTimers()).hasSize(2);
        // ordering is by total desc, so order is not fixed
        Set<String> childTimerNames = Sets.newHashSet();
        childTimerNames.add(rootTimer.childTimers().get(0).name());
        childTimerNames.add(rootTimer.childTimers().get(1).name());
        assertThat(childTimerNames).containsOnly("jdbc query", "jdbc rollback");

        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage()).isEmpty();
        assertThat(outgoingSpan.getMessage())
                .isEqualTo("insert into employee (name) values ('john doe')");
        assertThat(outgoingSpan.getPrefix()).isEqualTo("jdbc query: ");
        assertThat(outgoingSpan.getSuffix()).isEmpty();

        LocalSpan localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo("jdbc rollback");
        assertThat(localSpan.childSpans()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testRollbackThrowing() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteJdbcRollbackThrowing.class);

        // then
        IncomingSpan.Timer rootTimer = incomingSpan.mainThreadRootTimer();
        assertThat(rootTimer.childTimers()).hasSize(2);
        // ordering is by total desc, so order is not fixed
        Set<String> childTimerNames = Sets.newHashSet();
        childTimerNames.add(rootTimer.childTimers().get(0).name());
        childTimerNames.add(rootTimer.childTimers().get(1).name());
        assertThat(childTimerNames).containsOnly("jdbc query", "jdbc rollback");

        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage()).isEmpty();
        assertThat(outgoingSpan.getMessage())
                .isEqualTo("insert into employee (name) values ('john doe')");
        assertThat(outgoingSpan.getPrefix()).isEqualTo("jdbc query: ");
        assertThat(outgoingSpan.getSuffix()).isEmpty();

        LocalSpan localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo("jdbc rollback");
        assertThat(localSpan.childSpans()).isEmpty();
        assertThat(localSpan.getError().message())
                .isEqualTo("java.sql.SQLException: A rollback failure");

        assertThat(i.hasNext()).isFalse();
    }

    public abstract static class ExecuteJdbcCommitBase implements AppUnderTest, TransactionMarker {
        protected Connection connection;
        @Override
        public void executeApp() throws Exception {
            connection = Connections.createConnection();
            connection.setAutoCommit(false);
            try {
                transactionMarker();
            } finally {
                Connections.closeConnection(connection);
            }
        }
        void executeInsert() throws Exception {
            Statement statement = connection.createStatement();
            try {
                statement.execute("insert into employee (name) values ('john doe')");
            } finally {
                statement.close();
            }
        }
    }

    public abstract static class ExecuteJdbcCommitThrowingBase
            implements AppUnderTest, TransactionMarker {
        protected Connection connection;
        @Override
        public void executeApp() throws Exception {
            connection = new DelegatingConnection(Connections.createConnection()) {
                @Override
                public void commit() throws SQLException {
                    throw new SQLException("A commit failure");
                }
                @Override
                public void rollback() throws SQLException {
                    throw new SQLException("A rollback failure");
                }
            };
            connection.setAutoCommit(false);
            try {
                transactionMarker();
            } finally {
                Connections.closeConnection(connection);
            }
        }
        void executeInsert() throws Exception {
            Statement statement = connection.createStatement();
            try {
                statement.execute("insert into employee (name) values ('john doe')");
            } finally {
                statement.close();
            }
        }
    }

    public static class ExecuteJdbcCommit extends ExecuteJdbcCommitBase {
        @Override
        public void transactionMarker() throws Exception {
            executeInsert();
            connection.commit();
        }
    }

    public static class ExecuteJdbcRollback extends ExecuteJdbcCommitBase {
        @Override
        public void transactionMarker() throws Exception {
            executeInsert();
            connection.rollback();
        }
    }

    public static class ExecuteJdbcCommitThrowing extends ExecuteJdbcCommitThrowingBase {
        @Override
        public void transactionMarker() throws Exception {
            executeInsert();
            try {
                connection.commit();
            } catch (SQLException e) {
            }
        }
    }

    public static class ExecuteJdbcRollbackThrowing extends ExecuteJdbcCommitThrowingBase {
        @Override
        public void transactionMarker() throws Exception {
            executeInsert();
            try {
                connection.rollback();
            } catch (SQLException e) {
            }
        }
    }
}
