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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

import org.apache.commons.dbcp.DelegatingConnection;
import org.apache.commons.dbcp.DelegatingStatement;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.agent.it.harness.AppUnderTest;
import org.glowroot.agent.it.harness.Container;
import org.glowroot.agent.it.harness.Containers;
import org.glowroot.agent.it.harness.TransactionMarker;
import org.glowroot.agent.it.harness.model.ClientSpan;
import org.glowroot.agent.it.harness.model.ServerSpan;
import org.glowroot.agent.it.harness.model.Span;

import static org.assertj.core.api.Assertions.assertThat;

public class StatementIT {

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
    public void testStatement() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(ExecuteStatementAndIterateOverResults.class);

        // then
        Iterator<Span> i = serverSpan.childSpans().iterator();

        ClientSpan clientSpan = (ClientSpan) i.next();
        assertThat(clientSpan.getMessage()).isEmpty();
        assertThat(clientSpan.getMessage()).isEqualTo("select * from employee");
        assertThat(clientSpan.getPrefix()).isEqualTo("jdbc query: ");
        assertThat(clientSpan.getSuffix()).isEqualTo(" => 3 rows");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testStatementQuery() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(ExecuteStatementQueryAndIterateOverResults.class);

        // then
        Iterator<Span> i = serverSpan.childSpans().iterator();

        ClientSpan clientSpan = (ClientSpan) i.next();
        assertThat(clientSpan.getMessage()).isEmpty();
        assertThat(clientSpan.getMessage()).isEqualTo("select * from employee");
        assertThat(clientSpan.getPrefix()).isEqualTo("jdbc query: ");
        assertThat(clientSpan.getSuffix()).isEqualTo(" => 3 rows");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testStatementUpdate() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(ExecuteStatementUpdate.class);

        // then
        Iterator<Span> i = serverSpan.childSpans().iterator();

        ClientSpan clientSpan = (ClientSpan) i.next();
        assertThat(clientSpan.getMessage()).isEmpty();
        assertThat(clientSpan.getMessage())
                .isEqualTo("update employee set name = 'nobody'");
        assertThat(clientSpan.getPrefix()).isEqualTo("jdbc query: ");
        assertThat(clientSpan.getSuffix()).isEqualTo(" => 3 rows");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testNullStatement() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(ExecuteNullStatement.class);
        // then
        assertThat(serverSpan.childSpans()).isEmpty();
    }

    @Test
    public void testStatementThrowing() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(ExecuteStatementThrowing.class);

        // then
        Iterator<Span> i = serverSpan.childSpans().iterator();

        for (int j = 0; j < 2000; j++) {
            i.next();
        }

        ClientSpan clientSpan = (ClientSpan) i.next();
        assertThat(clientSpan.getMessage()).isEmpty();
        assertThat(clientSpan.getMessage()).isEqualTo("select * from employee");
        assertThat(clientSpan.getPrefix()).isEqualTo("jdbc query: ");
        assertThat(clientSpan.getSuffix()).isEmpty();

        assertThat(clientSpan.getError().message())
                .isEqualTo("java.sql.SQLException: An execute failure");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testStatementUsingPrevious() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(ExecuteStatementAndUsePrevious.class);

        // then
        Iterator<Span> i = serverSpan.childSpans().iterator();

        ClientSpan clientSpan = (ClientSpan) i.next();
        assertThat(clientSpan.getMessage()).isEmpty();
        assertThat(clientSpan.getMessage()).isEqualTo("select * from employee");
        assertThat(clientSpan.getPrefix()).isEqualTo("jdbc query: ");
        assertThat(clientSpan.getSuffix()).isEqualTo(" => 3 rows");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testStatementUsingRelativeForward() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(ExecuteStatementAndUseRelativeForward.class);

        // then
        Iterator<Span> i = serverSpan.childSpans().iterator();

        ClientSpan clientSpan = (ClientSpan) i.next();
        assertThat(clientSpan.getMessage()).isEmpty();
        assertThat(clientSpan.getMessage()).isEqualTo("select * from employee");
        assertThat(clientSpan.getPrefix()).isEqualTo("jdbc query: ");
        assertThat(clientSpan.getSuffix()).isEqualTo(" => 3 rows");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testStatementUsingRelativeBackward() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(ExecuteStatementAndUseRelativeBackward.class);

        // then
        Iterator<Span> i = serverSpan.childSpans().iterator();

        ClientSpan clientSpan = (ClientSpan) i.next();
        assertThat(clientSpan.getMessage()).isEmpty();
        assertThat(clientSpan.getMessage()).isEqualTo("select * from employee");
        assertThat(clientSpan.getPrefix()).isEqualTo("jdbc query: ");
        assertThat(clientSpan.getSuffix()).isEqualTo(" => 3 rows");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testStatementUsingAbsolute() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(ExecuteStatementAndUseAbsolute.class);

        // then
        Iterator<Span> i = serverSpan.childSpans().iterator();

        ClientSpan clientSpan = (ClientSpan) i.next();
        assertThat(clientSpan.getMessage()).isEmpty();
        assertThat(clientSpan.getMessage()).isEqualTo("select * from employee");
        assertThat(clientSpan.getPrefix()).isEqualTo("jdbc query: ");
        assertThat(clientSpan.getSuffix()).isEqualTo(" => 2 rows");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testStatementUsingFirst() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(ExecuteStatementAndUseFirst.class);

        // then
        Iterator<Span> i = serverSpan.childSpans().iterator();

        ClientSpan clientSpan = (ClientSpan) i.next();
        assertThat(clientSpan.getMessage()).isEmpty();
        assertThat(clientSpan.getMessage()).isEqualTo("select * from employee");
        assertThat(clientSpan.getPrefix()).isEqualTo("jdbc query: ");
        assertThat(clientSpan.getSuffix()).isEqualTo(" => 1 row");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testStatementUsingLast() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(ExecuteStatementAndUseLast.class);

        // then
        Iterator<Span> i = serverSpan.childSpans().iterator();

        ClientSpan clientSpan = (ClientSpan) i.next();
        assertThat(clientSpan.getMessage()).isEmpty();
        assertThat(clientSpan.getMessage()).isEqualTo("select * from employee");
        assertThat(clientSpan.getPrefix()).isEqualTo("jdbc query: ");
        assertThat(clientSpan.getSuffix()).isEqualTo(" => 3 rows");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testIteratingOverResultsInAnotherThread() throws Exception {
        // when
        ServerSpan serverSpan =
                container.execute(ExecuteStatementQueryAndIterateOverResultsAfterTransaction.class);

        // then
        Iterator<Span> i = serverSpan.childSpans().iterator();

        ClientSpan clientSpan = (ClientSpan) i.next();
        assertThat(clientSpan.getMessage()).isEmpty();
        assertThat(clientSpan.getMessage()).isEqualTo("select * from employee");
        assertThat(clientSpan.getPrefix()).isEqualTo("jdbc query: ");
        assertThat(clientSpan.getSuffix()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    public static class ExecuteStatementAndIterateOverResults
            implements AppUnderTest, TransactionMarker {
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
            Statement statement = connection.createStatement();
            try {
                statement.execute("select * from employee");
                ResultSet rs = statement.getResultSet();
                while (rs.next()) {
                    rs.getString(1);
                }
            } finally {
                statement.close();
            }
        }
    }

    public static class ExecuteStatementQueryAndIterateOverResults
            implements AppUnderTest, TransactionMarker {
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
            Statement statement = connection.createStatement();
            try {
                ResultSet rs = statement.executeQuery("select * from employee");
                while (rs.next()) {
                    rs.getString(1);
                }
            } finally {
                statement.close();
            }
        }
    }

    public static class ExecuteStatementUpdate implements AppUnderTest, TransactionMarker {
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
            Statement statement = connection.createStatement();
            try {
                statement.executeUpdate("update employee set name = 'nobody'");
            } finally {
                statement.close();
            }
        }
    }

    public static class ExecuteNullStatement implements AppUnderTest, TransactionMarker {
        private Connection delegatingConnection;
        @Override
        public void executeApp() throws Exception {
            Connection connection = Connections.createConnection();
            delegatingConnection = new DelegatingConnection(connection) {
                @Override
                public Statement createStatement() throws SQLException {
                    return new DelegatingStatement(this, super.createStatement()) {
                        @Override
                        public boolean execute(String sql) throws SQLException {
                            return super.execute("select 1 from employee");
                        }
                    };
                }
            };
            try {
                transactionMarker();
            } finally {
                Connections.closeConnection(connection);
            }
        }
        @Override
        public void transactionMarker() throws Exception {
            Statement statement = delegatingConnection.createStatement();
            try {
                statement.execute(null);
            } finally {
                statement.close();
            }
        }
    }

    public static class ExecuteStatementThrowing implements AppUnderTest, TransactionMarker {
        private Connection connection;
        private Connection delegatingConnection;
        @Override
        public void executeApp() throws Exception {
            connection = Connections.createConnection();
            delegatingConnection = new DelegatingConnection(connection) {
                @Override
                public Statement createStatement() throws SQLException {
                    return new DelegatingStatement(this, super.createStatement()) {
                        @Override
                        public boolean execute(String sql) throws SQLException {
                            throw new SQLException("An execute failure");
                        }
                    };
                }
            };
            try {
                transactionMarker();
            } finally {
                Connections.closeConnection(connection);
            }
        }
        @Override
        public void transactionMarker() throws Exception {
            // exceed the limit for distinct aggregated queries in a single serverSpan
            for (int i = 0; i < 5000; i++) {
                Statement statement = connection.createStatement();
                try {
                    statement.execute("select " + i + " from employee");
                } finally {
                    statement.close();
                }
            }
            Statement statement = delegatingConnection.createStatement();
            try {
                statement.execute("select * from employee");
            } catch (SQLException e) {
            } finally {
                statement.close();
            }
        }
    }

    public static class ExecuteStatementAndUsePrevious implements AppUnderTest, TransactionMarker {
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
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
            try {
                statement.execute("select * from employee");
                ResultSet rs = statement.getResultSet();
                rs.afterLast();
                while (rs.previous()) {
                    rs.getString(1);
                }
            } finally {
                statement.close();
            }
        }
    }

    public static class ExecuteStatementAndUseRelativeForward
            implements AppUnderTest, TransactionMarker {
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
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
            try {
                statement.execute("select * from employee");
                ResultSet rs = statement.getResultSet();
                // need to position cursor on a valid row before calling relative(), at least for
                // sqlserver jdbc driver
                rs.next();
                rs.getString(1);
                while (rs.relative(1)) {
                    rs.getString(1);
                }
            } finally {
                statement.close();
            }
        }
    }

    public static class ExecuteStatementAndUseRelativeBackward
            implements AppUnderTest, TransactionMarker {
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
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
            try {
                statement.execute("select * from employee");
                ResultSet rs = statement.getResultSet();
                rs.afterLast();
                // need to position cursor on a valid row before calling relative(), at least for
                // sqlserver jdbc driver
                rs.previous();
                rs.getString(1);
                while (rs.relative(-1)) {
                    rs.getString(1);
                }
            } finally {
                statement.close();
            }
        }
    }

    public static class ExecuteStatementAndUseAbsolute implements AppUnderTest, TransactionMarker {
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
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
            try {
                statement.execute("select * from employee");
                ResultSet rs = statement.getResultSet();
                rs.absolute(2);
            } finally {
                statement.close();
            }
        }
    }

    public static class ExecuteStatementAndUseFirst implements AppUnderTest, TransactionMarker {
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
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
            try {
                statement.execute("select * from employee");
                ResultSet rs = statement.getResultSet();
                rs.first();
            } finally {
                statement.close();
            }
        }
    }

    public static class ExecuteStatementAndUseLast implements AppUnderTest, TransactionMarker {
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
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
            try {
                statement.execute("select * from employee");
                ResultSet rs = statement.getResultSet();
                rs.last();
            } finally {
                statement.close();
            }
        }
    }

    public static class ExecuteStatementQueryAndIterateOverResultsAfterTransaction
            implements AppUnderTest, TransactionMarker {

        private Connection connection;
        private Statement statement;
        private ResultSet rs;

        @Override
        public void executeApp() throws Exception {
            connection = Connections.createConnection();
            statement = connection.createStatement();
            try {
                transactionMarker();
                while (rs.next()) {
                    rs.getString(1);
                }
            } finally {
                statement.close();
                Connections.closeConnection(connection);
            }
        }

        @Override
        public void transactionMarker() throws Exception {
            rs = statement.executeQuery("select * from employee");
        }
    }
}
