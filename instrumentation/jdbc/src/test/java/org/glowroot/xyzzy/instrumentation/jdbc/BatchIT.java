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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

import com.google.common.collect.ImmutableList;
import org.apache.commons.dbcp.DelegatingConnection;
import org.apache.commons.dbcp.DelegatingStatement;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.xyzzy.instrumentation.jdbc.Connections.ConnectionType;
import org.glowroot.xyzzy.test.harness.AppUnderTest;
import org.glowroot.xyzzy.test.harness.OutgoingSpan;
import org.glowroot.xyzzy.test.harness.Container;
import org.glowroot.xyzzy.test.harness.Containers;
import org.glowroot.xyzzy.test.harness.IncomingSpan;
import org.glowroot.xyzzy.test.harness.Span;
import org.glowroot.xyzzy.test.harness.TransactionMarker;

import static org.assertj.core.api.Assertions.assertThat;

public class BatchIT {

    private static final String INSTRUMENTATION_ID = "jdbc";

    private static Container container;
    private static boolean driverCapturesBatchRows =
            Connections.getConnectionType() != ConnectionType.ORACLE;

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
        container.resetInstrumentationProperties();
    }

    @Test
    public void testBatchPreparedStatement() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteBatchPreparedStatement.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage()).isEmpty();
        assertThat(outgoingSpan.getMessage()).isEqualTo("insert into employee (name) values (?)");
        assertThat(outgoingSpan.getPrefix()).isEqualTo("jdbc query: 3 x ");
        if (driverCapturesBatchRows) {
            assertThat(outgoingSpan.getSuffix()).isEqualTo(" ['huckle'] ['sally'] ['sally'] => 3 rows");
        } else {
            assertThat(outgoingSpan.getSuffix()).isEqualTo(" ['huckle'] ['sally'] ['sally']");
        }

        outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage()).isEmpty();
        assertThat(outgoingSpan.getMessage()).isEqualTo("insert into employee (name) values (?)");
        assertThat(outgoingSpan.getPrefix()).isEqualTo("jdbc query: 2 x ");
        if (driverCapturesBatchRows) {
            assertThat(outgoingSpan.getSuffix()).isEqualTo(" ['lowly'] ['pig will'] => 2 rows");
        } else {
            assertThat(outgoingSpan.getSuffix()).isEqualTo(" ['lowly'] ['pig will']");
        }

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testBatchPreparedExceedingLimitStatement() throws Exception {
        // when
        IncomingSpan incomingSpan =
                container.execute(ExecuteBatchExceedingLimitPreparedStatement.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage()).isEmpty();
        assertThat(outgoingSpan.getMessage()).isEqualTo("insert into employee (name) values (?)");
        assertThat(outgoingSpan.getPrefix()).isEqualTo("jdbc query: 2002 x ");
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < 1000; j++) {
            sb.append(" ['name");
            sb.append(j);
            sb.append("']");
        }
        if (driverCapturesBatchRows) {
            sb.append(" ... => 2002 rows");
        } else {
            sb.append(" ...");
        }
        assertThat(outgoingSpan.getSuffix()).isEqualTo(sb.toString());

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testBatchPreparedStatementWithoutCaptureBindParams() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID, "captureBindParametersIncludes",
                ImmutableList.<String>of());

        // when
        IncomingSpan incomingSpan = container.execute(ExecuteBatchPreparedStatement.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage()).isEmpty();
        assertThat(outgoingSpan.getMessage()).isEqualTo("insert into employee (name) values (?)");
        assertThat(outgoingSpan.getPrefix()).isEqualTo("jdbc query: 3 x ");
        if (driverCapturesBatchRows) {
            assertThat(outgoingSpan.getSuffix()).isEqualTo(" => 3 rows");
        } else {
            assertThat(outgoingSpan.getSuffix()).isEmpty();
        }

        outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage()).isEmpty();
        assertThat(outgoingSpan.getMessage()).isEqualTo("insert into employee (name) values (?)");
        assertThat(outgoingSpan.getPrefix()).isEqualTo("jdbc query: 2 x ");
        if (driverCapturesBatchRows) {
            assertThat(outgoingSpan.getSuffix()).isEqualTo(" => 2 rows");
        } else {
            assertThat(outgoingSpan.getSuffix()).isEmpty();
        }

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testBatchPreparedStatementWithoutClear() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteBatchPreparedStatementWithoutClear.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage()).isEmpty();
        assertThat(outgoingSpan.getMessage()).isEqualTo("insert into employee (name) values (?)");
        assertThat(outgoingSpan.getPrefix()).isEqualTo("jdbc query: 2 x ");
        if (driverCapturesBatchRows) {
            assertThat(outgoingSpan.getSuffix()).isEqualTo(" ['huckle'] ['sally'] => 2 rows");
        } else {
            assertThat(outgoingSpan.getSuffix()).isEqualTo(" ['huckle'] ['sally']");
        }

        outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage()).isEmpty();
        assertThat(outgoingSpan.getMessage()).isEqualTo("insert into employee (name) values (?)");
        assertThat(outgoingSpan.getPrefix()).isEqualTo("jdbc query: 2 x ");
        if (driverCapturesBatchRows) {
            assertThat(outgoingSpan.getSuffix()).isEqualTo(" ['lowly'] ['pig will'] => 2 rows");
        } else {
            assertThat(outgoingSpan.getSuffix()).isEqualTo(" ['lowly'] ['pig will']");
        }

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testBatchPreparedStatementWithoutClearWithoutCaptureBindParams() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID, "captureBindParametersIncludes",
                ImmutableList.<String>of());

        // when
        IncomingSpan incomingSpan = container.execute(ExecuteBatchPreparedStatementWithoutClear.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage()).isEmpty();
        assertThat(outgoingSpan.getMessage()).isEqualTo("insert into employee (name) values (?)");
        assertThat(outgoingSpan.getPrefix()).isEqualTo("jdbc query: 2 x ");
        if (driverCapturesBatchRows) {
            assertThat(outgoingSpan.getSuffix()).isEqualTo(" => 2 rows");
        } else {
            assertThat(outgoingSpan.getSuffix()).isEmpty();
        }

        outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage()).isEmpty();
        assertThat(outgoingSpan.getMessage()).isEqualTo("insert into employee (name) values (?)");
        assertThat(outgoingSpan.getPrefix()).isEqualTo("jdbc query: 2 x ");
        if (driverCapturesBatchRows) {
            assertThat(outgoingSpan.getSuffix()).isEqualTo(" => 2 rows");
        } else {
            assertThat(outgoingSpan.getSuffix()).isEmpty();
        }

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testBatchStatement() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteBatchStatement.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage()).isEmpty();
        assertThat(outgoingSpan.getMessage())
                .isEqualTo("[batch] insert into employee (name) values ('huckle'),"
                        + " insert into employee (name) values ('sally')");
        assertThat(outgoingSpan.getPrefix()).isEqualTo("jdbc query: ");
        assertThat(outgoingSpan.getSuffix()).isEqualTo(" => 2 rows");

        outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage()).isEmpty();
        assertThat(outgoingSpan.getMessage())
                .isEqualTo("[batch] insert into employee (name) values ('lowly'),"
                        + " insert into employee (name) values ('pig will')");
        assertThat(outgoingSpan.getPrefix()).isEqualTo("jdbc query: ");
        assertThat(outgoingSpan.getSuffix()).isEqualTo(" => 2 rows");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testBatchStatementNull() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(BatchStatementNull.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage()).isEmpty();
        assertThat(outgoingSpan.getMessage())
                .isEqualTo("[batch] insert into employee (name) values ('1')");
        assertThat(outgoingSpan.getPrefix()).isEqualTo("jdbc query: ");
        assertThat(outgoingSpan.getSuffix()).isEqualTo(" => 1 row");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testBatchStatementWithNoBatches() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteBatchStatementWithNoBatches.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage()).isEmpty();
        assertThat(outgoingSpan.getMessage()).isEqualTo("[empty batch]");
        assertThat(outgoingSpan.getPrefix()).isEqualTo("jdbc query: ");
        assertThat(outgoingSpan.getSuffix()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testBatchPreparedStatementWithNoBatches() throws Exception {
        // hsqldb driver (and maybe some others) throw error when executing a batch with no batches
        Assume.assumeTrue(Connections.getConnectionType() == ConnectionType.H2);

        // when
        IncomingSpan incomingSpan = container.execute(ExecuteBatchPreparedStatementWithNoBatches.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage()).isEmpty();
        assertThat(outgoingSpan.getMessage())
                .isEqualTo("[empty batch] insert into employee (name) values (?)");
        assertThat(outgoingSpan.getPrefix()).isEqualTo("jdbc query: ");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testBatchPreparedStatementWithSingleBatch() throws Exception {
        // when
        IncomingSpan incomingSpan =
                container.execute(ExecuteBatchPreparedStatementWithSingleBatch.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage()).isEmpty();
        assertThat(outgoingSpan.getMessage()).isEqualTo("insert into employee (name) values (?)");
        assertThat(outgoingSpan.getPrefix()).isEqualTo("jdbc query: ");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testBatchStatementWithoutClear() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteBatchStatementWithoutClear.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage()).isEmpty();
        assertThat(outgoingSpan.getMessage())
                .isEqualTo("[batch] insert into employee (name) values ('huckle'),"
                        + " insert into employee (name) values ('sally')");
        assertThat(outgoingSpan.getPrefix()).isEqualTo("jdbc query: ");
        assertThat(outgoingSpan.getSuffix()).isEqualTo(" => 2 rows");

        outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage()).isEmpty();
        assertThat(outgoingSpan.getMessage())
                .isEqualTo("[batch] insert into employee (name) values ('lowly'),"
                        + " insert into employee (name) values ('pig will')");
        assertThat(outgoingSpan.getPrefix()).isEqualTo("jdbc query: ");
        assertThat(outgoingSpan.getSuffix()).isEqualTo(" => 2 rows");

        assertThat(i.hasNext()).isFalse();
    }

    public static class ExecuteBatchPreparedStatement implements AppUnderTest, TransactionMarker {
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
            PreparedStatement preparedStatement =
                    connection.prepareStatement("insert into employee (name) values (?)");
            try {
                preparedStatement.setString(1, "huckle");
                preparedStatement.addBatch();
                preparedStatement.setString(1, "sally");
                preparedStatement.addBatch();
                // add batch without re-setting params
                preparedStatement.addBatch();
                preparedStatement.executeBatch();
                preparedStatement.clearBatch();
                preparedStatement.clearBatch();
                preparedStatement.setString(1, "lowly");
                preparedStatement.addBatch();
                preparedStatement.setString(1, "pig will");
                preparedStatement.addBatch();
                preparedStatement.executeBatch();
            } finally {
                preparedStatement.close();
            }
        }
    }

    public static class ExecuteBatchExceedingLimitPreparedStatement
            implements AppUnderTest, TransactionMarker {
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
            PreparedStatement preparedStatement =
                    connection.prepareStatement("insert into employee (name) values (?)");
            try {
                for (int i = 0; i < 2002; i++) {
                    preparedStatement.setString(1, "name" + i);
                    preparedStatement.addBatch();
                }
                preparedStatement.executeBatch();
            } finally {
                preparedStatement.close();
            }
        }
    }

    public static class ExecuteBatchPreparedStatementWithoutClear
            implements AppUnderTest, TransactionMarker {
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
            PreparedStatement preparedStatement =
                    connection.prepareStatement("insert into employee (name) values (?)");
            try {
                preparedStatement.setString(1, "huckle");
                preparedStatement.addBatch();
                preparedStatement.setString(1, "sally");
                preparedStatement.addBatch();
                preparedStatement.executeBatch();
                // intentionally not calling preparedStatement.clearBatch()
                preparedStatement.setString(1, "lowly");
                preparedStatement.addBatch();
                preparedStatement.setString(1, "pig will");
                preparedStatement.addBatch();
                preparedStatement.executeBatch();
            } finally {
                preparedStatement.close();
            }
        }
    }

    public static class ExecuteBatchStatement implements AppUnderTest, TransactionMarker {
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
            Statement statement = connection.createStatement();
            try {
                statement.addBatch("insert into employee (name) values ('huckle')");
                statement.addBatch("insert into employee (name) values ('sally')");
                statement.executeBatch();
                statement.clearBatch();
                statement.addBatch("insert into employee (name) values ('lowly')");
                statement.addBatch("insert into employee (name) values ('pig will')");
                statement.executeBatch();
            } finally {
                statement.close();
            }
        }
    }

    public static class BatchStatementNull implements AppUnderTest, TransactionMarker {
        private Connection delegatingConnection;
        @Override
        public void executeApp() throws Exception {
            Connection connection = Connections.createConnection();
            delegatingConnection = new DelegatingConnection(connection) {
                @Override
                public Statement createStatement() throws SQLException {
                    return new DelegatingStatement(this, super.createStatement()) {
                        @Override
                        public void addBatch(String sql) throws SQLException {
                            super.addBatch("insert into employee (name) values ('1')");
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
                statement.addBatch(null);
                statement.executeBatch();
            } finally {
                statement.close();
            }
        }
    }

    public static class ExecuteBatchStatementWithNoBatches
            implements AppUnderTest, TransactionMarker {
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
            Statement statement = connection.createStatement();
            try {
                statement.executeBatch();
            } finally {
                statement.close();
            }
        }
    }

    public static class ExecuteBatchPreparedStatementWithNoBatches
            implements AppUnderTest, TransactionMarker {
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
            PreparedStatement preparedStatement =
                    connection.prepareStatement("insert into employee (name) values (?)");
            try {
                preparedStatement.executeBatch();
            } finally {
                preparedStatement.close();
            }
        }
    }

    public static class ExecuteBatchPreparedStatementWithSingleBatch
            implements AppUnderTest, TransactionMarker {
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
            PreparedStatement preparedStatement =
                    connection.prepareStatement("insert into employee (name) values (?)");
            try {
                preparedStatement.setString(1, "huckle");
                preparedStatement.addBatch();
                preparedStatement.executeBatch();
            } finally {
                preparedStatement.close();
            }
        }
    }

    public static class ExecuteBatchStatementWithoutClear
            implements AppUnderTest, TransactionMarker {
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
            Statement statement = connection.createStatement();
            try {
                statement.addBatch("insert into employee (name) values ('huckle')");
                statement.addBatch("insert into employee (name) values ('sally')");
                statement.executeBatch();
                // intentionally not calling statement.clearBatch()
                statement.addBatch("insert into employee (name) values ('lowly')");
                statement.addBatch("insert into employee (name) values ('pig will')");
                statement.executeBatch();
            } finally {
                statement.close();
            }
        }
    }
}
