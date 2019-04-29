/*
 * Copyright 2011-2019 the original author or authors.
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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Iterator;

import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.xyzzy.test.harness.AppUnderTest;
import org.glowroot.xyzzy.test.harness.ClientSpan;
import org.glowroot.xyzzy.test.harness.Container;
import org.glowroot.xyzzy.test.harness.Containers;
import org.glowroot.xyzzy.test.harness.ServerSpan;
import org.glowroot.xyzzy.test.harness.Span;
import org.glowroot.xyzzy.test.harness.TraceEntryMarker;
import org.glowroot.xyzzy.test.harness.TransactionMarker;

import static org.assertj.core.api.Assertions.assertThat;

public class OtherIT {

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
        container.resetInstrumentationConfig();
    }

    @Test
    public void testCallableStatement() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID, "captureBindParametersIncludes",
                ImmutableList.of(".*"));

        // when
        ServerSpan serverSpan = container.execute(ExecuteCallableStatement.class);

        // then
        Iterator<Span> i = serverSpan.childSpans().iterator();

        ClientSpan entry = (ClientSpan) i.next();
        assertThat(entry.getMessage()).isEmpty();
        assertThat(entry.getMessage()).isEqualTo("insert into employee (name, misc) values (?, ?)");
        assertThat(entry.getPrefix()).isEqualTo("jdbc query: ");
        assertThat(entry.getSuffix()).isEqualTo(" ['jane', NULL]");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testWithoutResultSetValueTimerNormal() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(ExecuteStatementAndIterateOverResults.class);

        // then
        boolean found = findExtendedTimerName(serverSpan.mainThreadRootTimer(), "jdbc query");
        assertThat(found).isFalse();
    }

    @Test
    public void testWithResultSetValueTimerNormal() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID, "captureResultSetGet", true);

        // when
        ServerSpan serverSpan = container.execute(ExecuteStatementAndIterateOverResults.class);

        // then
        boolean found = findExtendedTimerName(serverSpan, "jdbc query");
        assertThat(found).isFalse();
    }

    @Test
    public void testWithoutResultSetValueTimerUnderSeparateTraceEntry() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(GetResultSetValueUnderSeparateTraceEntry.class);

        // then
        boolean found = findExtendedTimerName(serverSpan, "jdbc query");
        assertThat(found).isFalse();
    }

    @Test
    public void testWithResultSetValueTimerUnderSeparateTraceEntry() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID, "captureResultSetGet", true);

        // when
        ServerSpan serverSpan = container.execute(GetResultSetValueUnderSeparateTraceEntry.class);

        // then
        boolean found = findExtendedTimerName(serverSpan, "jdbc query");
        assertThat(found).isTrue();
    }

    @Test
    public void testResultSetValueTimerUsingColumnName() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID, "captureResultSetGet", true);

        // when
        ServerSpan serverSpan =
                container.execute(ExecuteStatementAndIterateOverResultsUsingColumnName.class);

        // then
        boolean found = findExtendedTimerName(serverSpan, "jdbc query");
        assertThat(found).isFalse();
    }

    @Test
    public void testResultSetValueTimerUsingColumnNameUnderSeparateTraceEntry() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID, "captureResultSetGet", true);

        // when
        ServerSpan serverSpan = container.execute(
                ExecuteStatementAndIterateOverResultsUsingColumnNameUnderSeparateTraceEntry.class);

        // then
        boolean found = findExtendedTimerName(serverSpan, "jdbc query");
        assertThat(found).isTrue();
    }

    @Test
    public void testWithResultSetNavigateTimerNormal() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(ExecuteStatementAndIterateOverResults.class);

        // then
        boolean found = findExtendedTimerName(serverSpan, "jdbc query");
        assertThat(found).isFalse();
    }

    @Test
    public void testWithResultSetNavigateTimerUnderSeparateTraceEntry() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(IterateOverResultsUnderSeparateTraceEntry.class);

        // then
        boolean found = findExtendedTimerName(serverSpan, "jdbc query");
        assertThat(found).isTrue();
    }

    @Test
    public void testWithoutResultSetNavigateTimerUnderSeparateTraceEntry() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID, "captureResultSetNavigate", false);

        // when
        ServerSpan serverSpan = container.execute(IterateOverResultsUnderSeparateTraceEntry.class);

        // then
        boolean found = findExtendedTimerName(serverSpan, "jdbc query");
        assertThat(found).isFalse();
    }

    @Test
    public void testDefaultStackTraceThreshold() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(ExecuteStatementAndIterateOverResults.class);

        // then
        Iterator<Span> i = serverSpan.childSpans().iterator();

        ClientSpan clientSpan = (ClientSpan) i.next();
        assertThat(clientSpan.getMessage()).isEmpty();
        assertThat(clientSpan.getMessage()).isEqualTo("select * from employee");
        assertThat(clientSpan.getPrefix()).isEqualTo("jdbc query: ");
        assertThat(clientSpan.getSuffix()).isEqualTo(" => 3 rows");
        assertThat(clientSpan.getLocationStackTraceMillis()).isNull();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testZeroStackTraceThreshold() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID, "stackTraceThresholdMillis", 0.0);

        // when
        ServerSpan serverSpan = container.execute(ExecuteStatementAndIterateOverResults.class);

        // then
        Iterator<Span> i = serverSpan.childSpans().iterator();

        ClientSpan clientSpan = (ClientSpan) i.next();
        assertThat(clientSpan.getMessage()).isEmpty();
        assertThat(clientSpan.getMessage()).isEqualTo("select * from employee");
        assertThat(clientSpan.getPrefix()).isEqualTo("jdbc query: ");
        assertThat(clientSpan.getSuffix()).isEqualTo(" => 3 rows");
        assertThat(clientSpan.getLocationStackTraceMillis()).isZero();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testNullStackTraceThreshold() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID, "stackTraceThresholdMillis",
                (Double) null);

        // when
        ServerSpan serverSpan = container.execute(ExecuteStatementAndIterateOverResults.class);

        // then
        Iterator<Span> i = serverSpan.childSpans().iterator();

        ClientSpan clientSpan = (ClientSpan) i.next();
        assertThat(clientSpan.getMessage()).isEmpty();
        assertThat(clientSpan.getMessage()).isEqualTo("select * from employee");
        assertThat(clientSpan.getPrefix()).isEqualTo("jdbc query: ");
        assertThat(clientSpan.getSuffix()).isEqualTo(" => 3 rows");
        assertThat(clientSpan.getLocationStackTraceMillis()).isNull();

        assertThat(i.hasNext()).isFalse();
    }

    private static boolean findExtendedTimerName(ServerSpan serverSpan, String timerName) {
        return findExtendedTimerName(serverSpan.mainThreadRootTimer(), timerName);
    }

    private static boolean findExtendedTimerName(ServerSpan.Timer timer, String timerName) {
        if (timer.name().equals(timerName) && timer.extended()) {
            return true;
        }
        for (ServerSpan.Timer nestedTimer : timer.childTimers()) {
            if (findExtendedTimerName(nestedTimer, timerName)) {
                return true;
            }
        }
        return false;
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

    public static class ExecuteLotsOfStatementAndIterateOverResults
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
                for (int i = 0; i < 4000; i++) {
                    statement.execute("select * from employee");
                    ResultSet rs = statement.getResultSet();
                    while (rs.next()) {
                        rs.getString(1);
                    }
                }
            } finally {
                statement.close();
            }
        }
    }

    public static class IterateOverResultsUnderSeparateTraceEntry
            implements AppUnderTest, TransactionMarker, TraceEntryMarker {
        private Connection connection;
        private Statement statement;
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
            statement = connection.createStatement();
            try {
                statement.execute("select * from employee");
                traceEntryMarker();
            } finally {
                statement.close();
            }
        }
        @Override
        public void traceEntryMarker() throws SQLException {
            ResultSet rs = statement.getResultSet();
            while (rs.next()) {
                rs.getString(1);
            }
        }
    }

    public static class GetResultSetValueUnderSeparateTraceEntry
            implements AppUnderTest, TransactionMarker, TraceEntryMarker {
        private Connection connection;
        private ResultSet rs;
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
                rs = statement.getResultSet();
                while (rs.next()) {
                    traceEntryMarker();
                }
            } finally {
                statement.close();
            }
        }
        @Override
        public void traceEntryMarker() throws SQLException {
            rs.getString(1);
        }
    }

    public static class ExecuteStatementAndIterateOverResultsUsingColumnName
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
                    rs.getString("name");
                }
            } finally {
                statement.close();
            }
        }
    }

    public static class ExecuteStatementAndIterateOverResultsUsingColumnNameUnderSeparateTraceEntry
            implements AppUnderTest, TransactionMarker, TraceEntryMarker {
        private Connection connection;
        private Statement statement;
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
            statement = connection.createStatement();
            try {
                statement.execute("select * from employee");
                traceEntryMarker();
            } finally {
                statement.close();
            }
        }
        @Override
        public void traceEntryMarker() throws SQLException {
            ResultSet rs = statement.getResultSet();
            while (rs.next()) {
                rs.getString("name");
            }
        }
    }

    public static class ExecuteCallableStatement implements AppUnderTest, TransactionMarker {
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
            CallableStatement callableStatement =
                    connection.prepareCall("insert into employee (name, misc) values (?, ?)");
            try {
                callableStatement.setString(1, "jane");
                callableStatement.setNull(2, Types.BINARY);
                callableStatement.execute();
            } finally {
                callableStatement.close();
            }
        }
    }

    public static class AccessMetaData implements AppUnderTest, TransactionMarker {
        private Connection connection;
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
        @Override
        public void transactionMarker() throws Exception {
            connection.getMetaData().getTables(null, null, null, null);
        }
    }
}
