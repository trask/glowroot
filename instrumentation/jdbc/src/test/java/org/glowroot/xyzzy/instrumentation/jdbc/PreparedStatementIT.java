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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.dbcp.DelegatingConnection;
import org.apache.commons.dbcp.DelegatingPreparedStatement;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.agent.it.harness.AppUnderTest;
import org.glowroot.agent.it.harness.Container;
import org.glowroot.agent.it.harness.Containers;
import org.glowroot.agent.it.harness.TransactionMarker;
import org.glowroot.agent.it.harness.model.Trace;
import org.glowroot.xyzzy.instrumentation.jdbc.Connections.ConnectionType;

import static org.assertj.core.api.Assertions.assertThat;

public class PreparedStatementIT {

    private static final String INSTRUMENTATION_ID = "jdbc";

    private static final List<String> H2_EXTRA_LOB_QUERIES =
            ImmutableList.of("SELECT MAX(LOB) FROM INFORMATION_SCHEMA.LOB_MAP",
                    "SELECT MAX(ID) FROM INFORMATION_SCHEMA.LOBS");

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
    public void testPreparedStatement() throws Exception {
        // when
        Trace trace = container.execute(ExecutePreparedStatementAndIterateOverResults.class);

        // then
        Iterator<Trace.Entry> i = trace.entries().iterator();

        Trace.Entry entry = i.next();
        assertThat(entry.depth()).isEqualTo(0);
        assertThat(entry.message()).isEmpty();
        assertThat(entry.queryEntryMessage().queryText())
                .isEqualTo("select * from employee where name like ?");
        assertThat(entry.queryEntryMessage().prefix()).isEqualTo("jdbc query: ");
        assertThat(entry.queryEntryMessage().suffix()).isEqualTo(" ['john%'] => 1 row");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testPreparedStatementQuery() throws Exception {
        // when
        Trace trace = container.execute(ExecutePreparedStatementQueryAndIterateOverResults.class);

        // then
        Iterator<Trace.Entry> i = trace.entries().iterator();

        Trace.Entry entry = i.next();
        assertThat(entry.depth()).isEqualTo(0);
        assertThat(entry.message()).isEmpty();
        assertThat(entry.queryEntryMessage().queryText())
                .isEqualTo("select * from employee where name like ?");
        assertThat(entry.queryEntryMessage().prefix()).isEqualTo("jdbc query: ");
        assertThat(entry.queryEntryMessage().suffix()).isEqualTo(" ['john%'] => 1 row");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testPreparedStatementUpdate() throws Exception {
        // when
        Trace trace = container.execute(ExecutePreparedStatementUpdate.class);

        // then
        Iterator<Trace.Entry> i = trace.entries().iterator();

        Trace.Entry entry = i.next();
        assertThat(entry.depth()).isEqualTo(0);
        assertThat(entry.message()).isEmpty();
        assertThat(entry.queryEntryMessage().queryText())
                .isEqualTo("update employee set name = ?");
        assertThat(entry.queryEntryMessage().prefix()).isEqualTo("jdbc query: ");
        assertThat(entry.queryEntryMessage().suffix()).isEqualTo(" ['nobody'] => 3 rows");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testPreparedStatementInsertWithGeneratedKeys() throws Exception {
        // when
        Trace trace = container.execute(ExecutePreparedStatementInsertWithGeneratedKeys.class);

        // then
        Iterator<Trace.Entry> i = trace.entries().iterator();

        Trace.Entry entry = i.next();
        assertThat(entry.depth()).isEqualTo(0);
        assertThat(entry.message()).isEmpty();
        assertThat(entry.queryEntryMessage().queryText())
                .isEqualTo("insert into employee (name) values (?)");
        assertThat(entry.queryEntryMessage().prefix()).isEqualTo("jdbc query: ");
        assertThat(entry.queryEntryMessage().suffix()).isEqualTo(" ['nobody'] => 1 row");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testPreparedStatementLargeParamSetFirst() throws Exception {
        // when
        Trace trace = container.execute(ExecutePreparedStatementLargeParamSetFirst.class);

        // then
        Iterator<Trace.Entry> i = trace.entries().iterator();

        Trace.Entry entry = i.next();
        assertThat(entry.depth()).isEqualTo(0);
        assertThat(entry.message()).isEmpty();
        assertThat(entry.queryEntryMessage().queryText())
                .startsWith("select * from employee where name like ? and name like ? ");
        assertThat(entry.queryEntryMessage().prefix()).isEqualTo("jdbc query: ");
        assertThat(entry.queryEntryMessage().suffix()).startsWith(" ['john%', 'john%', ");
        assertThat(entry.queryEntryMessage().suffix())
                .endsWith(", 'john%', 'john%'] => 1 row");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testPreparedStatementNullSql() throws Exception {
        // when
        Trace trace = container.execute(PreparedStatementNullSql.class);
        // then
        assertThat(trace.entries()).isEmpty();
    }

    @Test
    public void testPreparedStatementThrowing() throws Exception {
        // when
        Trace trace = container.execute(ExecutePreparedStatementThrowing.class);

        // then
        Iterator<Trace.Entry> i = trace.entries().iterator();

        Trace.Entry entry = i.next();
        assertThat(entry.depth()).isEqualTo(0);
        assertThat(entry.message()).isEmpty();
        assertThat(entry.queryEntryMessage().queryText())
                .isEqualTo("select * from employee where name like ?");
        assertThat(entry.queryEntryMessage().prefix()).isEqualTo("jdbc query: ");
        assertThat(entry.queryEntryMessage().suffix()).isEqualTo(" ['john%']");
        assertThat(entry.error().message())
                .isEqualTo("java.sql.SQLException: An execute failure");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testPreparedStatementWithTonsOfBindParameters() throws Exception {
        // when
        Trace trace = container.execute(
                ExecutePreparedStatementWithTonsOfBindParametersAndIterateOverResults.class);

        // then
        Iterator<Trace.Entry> i = trace.entries().iterator();

        Trace.Entry entry = i.next();
        StringBuilder sql = new StringBuilder("select * from employee where name like ?");
        for (int j = 0; j < 200; j++) {
            sql.append(" and name like ?");
        }
        StringBuilder suffix = new StringBuilder(" ['john%'");
        for (int j = 0; j < 200; j++) {
            suffix.append(", 'john%'");
        }
        suffix.append("] => 1 row");

        assertThat(entry.depth()).isEqualTo(0);
        assertThat(entry.message()).isEmpty();
        assertThat(entry.queryEntryMessage().queryText()).isEqualTo(sql.toString());
        assertThat(entry.queryEntryMessage().prefix()).isEqualTo("jdbc query: ");
        assertThat(entry.queryEntryMessage().suffix()).isEqualTo(suffix.toString());

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testPreparedStatementWithoutBindParameters() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID, "captureBindParametersIncludes",
                ImmutableList.<String>of());

        // when
        Trace trace = container.execute(ExecutePreparedStatementAndIterateOverResults.class);

        // then
        Iterator<Trace.Entry> i = trace.entries().iterator();

        Trace.Entry entry = i.next();
        assertThat(entry.depth()).isEqualTo(0);
        assertThat(entry.message()).isEmpty();
        assertThat(entry.queryEntryMessage().queryText())
                .isEqualTo("select * from employee where name like ?");
        assertThat(entry.queryEntryMessage().prefix()).isEqualTo("jdbc query: ");
        assertThat(entry.queryEntryMessage().suffix()).isEqualTo(" => 1 row");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testPreparedStatementWithSetNull() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID, "captureBindParametersIncludes",
                ImmutableList.of(".*"));

        // whens
        Trace trace = container.execute(ExecutePreparedStatementWithSetNull.class);

        // then
        Iterator<Trace.Entry> i = trace.entries().iterator();

        Trace.Entry entry = i.next();
        assertThat(entry.depth()).isEqualTo(0);
        assertThat(entry.message()).isEmpty();
        assertThat(entry.queryEntryMessage().queryText())
                .isEqualTo("insert into employee (name, misc) values (?, ?)");
        assertThat(entry.queryEntryMessage().prefix()).isEqualTo("jdbc query: ");
        assertThat(entry.queryEntryMessage().suffix()).isEqualTo(" [NULL, NULL]");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testPreparedStatementWithBinary() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID, "captureBindParametersIncludes",
                ImmutableList.of(".*"));

        // when
        Trace trace = container.execute(ExecutePreparedStatementWithBinary.class);

        // then
        Iterator<Trace.Entry> i = trace.entries().iterator();

        Trace.Entry entry = i.next();
        assertThat(entry.depth()).isEqualTo(0);
        assertThat(entry.message()).isEmpty();
        assertThat(entry.queryEntryMessage().queryText())
                .isEqualTo("insert into employee (name, misc) values (?, ?)");
        assertThat(entry.queryEntryMessage().prefix()).isEqualTo("jdbc query: ");
        assertThat(entry.queryEntryMessage().suffix())
                .isEqualTo(" ['jane', 0x00010203040506070809]");

        entry = i.next();
        assertThat(entry.depth()).isEqualTo(0);
        assertThat(entry.message()).isEmpty();
        assertThat(entry.queryEntryMessage().queryText())
                .isEqualTo("insert /**/ into employee (name, misc) values (?, ?)");
        assertThat(entry.queryEntryMessage().prefix()).isEqualTo("jdbc query: ");
        assertThat(entry.queryEntryMessage().suffix()).isEqualTo(" ['jane', {10 bytes}]");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testPreparedStatementWithBinaryUsingSetObject() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID, "captureBindParametersIncludes",
                ImmutableList.of(".*"));

        // when
        Trace trace = container.execute(ExecutePreparedStatementWithBinaryUsingSetObject.class);

        // then
        Iterator<Trace.Entry> i = trace.entries().iterator();

        Trace.Entry entry = i.next();
        assertThat(entry.depth()).isEqualTo(0);
        assertThat(entry.message()).isEmpty();
        assertThat(entry.queryEntryMessage().queryText())
                .isEqualTo("insert into employee (name, misc) values (?, ?)");
        assertThat(entry.queryEntryMessage().prefix()).isEqualTo("jdbc query: ");
        assertThat(entry.queryEntryMessage().suffix())
                .isEqualTo(" ['jane', 0x00010203040506070809]");

        entry = i.next();
        assertThat(entry.depth()).isEqualTo(0);
        assertThat(entry.message()).isEmpty();
        assertThat(entry.queryEntryMessage().queryText())
                .isEqualTo("insert /**/ into employee (name, misc) values (?, ?)");
        assertThat(entry.queryEntryMessage().prefix()).isEqualTo("jdbc query: ");
        assertThat(entry.queryEntryMessage().suffix()).isEqualTo(" ['jane', {10 bytes}]");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testPreparedStatementWithBinaryStream() throws Exception {

        if (Connections.getConnectionType() == ConnectionType.COMMONS_DBCP_WRAPPED) {
            NoSuchMethodException exception = null;
            try {
                org.apache.commons.dbcp.DelegatingStatement.class.getMethod("setBinaryStream",
                        InputStream.class);
            } catch (NoSuchMethodException e) {
                exception = e;
            }
            Assume.assumeNoException(exception);
        }

        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID, "captureBindParametersIncludes",
                ImmutableList.of(".*"));

        // when
        Trace trace = container.execute(ExecutePreparedStatementWithBinaryStream.class);

        // then
        Iterator<Trace.Entry> i = getTraceEntriesWithoutH2ExtraLobQueries(trace).iterator();

        Trace.Entry entry = i.next();
        assertThat(entry.depth()).isEqualTo(0);
        assertThat(entry.message()).isEmpty();
        assertThat(entry.queryEntryMessage().queryText())
                .isEqualTo("insert into employee (name, misc) values (?, ?)");
        assertThat(entry.queryEntryMessage().prefix()).isEqualTo("jdbc query: ");
        assertThat(entry.queryEntryMessage().suffix())
                .isEqualTo(" ['jane', {stream:ByteArrayInputStream}]");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testPreparedStatementWithCharacterStream() throws Exception {

        if (Connections.getConnectionType() == ConnectionType.COMMONS_DBCP_WRAPPED) {
            NoSuchMethodException exception = null;
            try {
                org.apache.commons.dbcp.DelegatingStatement.class.getMethod("setCharacterStream",
                        Reader.class);
            } catch (NoSuchMethodException e) {
                exception = e;
            }
            Assume.assumeNoException(exception);
        }

        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID, "captureBindParametersIncludes",
                ImmutableList.of(".*"));

        // when
        Trace trace = container.execute(ExecutePreparedStatementWithCharacterStream.class);

        // then
        Iterator<Trace.Entry> i = getTraceEntriesWithoutH2ExtraLobQueries(trace).iterator();

        Trace.Entry entry = i.next();
        assertThat(entry.depth()).isEqualTo(0);
        assertThat(entry.message()).isEmpty();
        assertThat(entry.queryEntryMessage().queryText())
                .isEqualTo("insert into employee (name, misc2) values (?, ?)");
        assertThat(entry.queryEntryMessage().prefix()).isEqualTo("jdbc query: ");
        assertThat(entry.queryEntryMessage().suffix())
                .isEqualTo(" ['jane', {stream:StringReader}]");

        assertThat(i.hasNext()).isFalse();
    }

    private List<Trace.Entry> getTraceEntriesWithoutH2ExtraLobQueries(Trace trace) {
        List<Trace.Entry> filtered = Lists.newArrayList();
        for (Trace.Entry entry : trace.entries()) {
            if (!H2_EXTRA_LOB_QUERIES.contains(entry.queryEntryMessage().queryText())) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    @Test
    public void testPreparedStatementWithClear() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID, "captureBindParametersIncludes",
                ImmutableList.of(".*"));

        // when
        Trace trace = container.execute(ExecutePreparedStatementWithClear.class);

        // then
        Iterator<Trace.Entry> i = trace.entries().iterator();

        Trace.Entry entry = i.next();
        assertThat(entry.depth()).isEqualTo(0);
        assertThat(entry.message()).isEmpty();
        assertThat(entry.queryEntryMessage().queryText())
                .isEqualTo("select * from employee where name like ?");
        assertThat(entry.queryEntryMessage().prefix()).isEqualTo("jdbc query: ");
        assertThat(entry.queryEntryMessage().suffix()).isEqualTo(" ['john%'] => 1 row");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testPreparedStatementThatHasInternalGlowrootToken() throws Exception {
        // when
        Trace trace = container.execute(ExecutePreparedStatementThatHasInternalGlowrootToken.class);

        // then
        Iterator<Trace.Entry> i = trace.entries().iterator();

        Trace.Entry entry = i.next();
        assertThat(entry.depth()).isEqualTo(0);
        assertThat(entry.message()).isEmpty();
        assertThat(entry.queryEntryMessage().queryText())
                .isEqualTo("select * from employee where name like ?");
        assertThat(entry.queryEntryMessage().prefix()).isEqualTo("jdbc query: ");
        assertThat(entry.queryEntryMessage().suffix()).isEqualTo(" ['{}'] => 0 rows");

        assertThat(i.hasNext()).isFalse();
    }

    public static class ExecutePreparedStatementAndIterateOverResults
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
            PreparedStatement preparedStatement =
                    connection.prepareStatement("select * from employee where name like ?");
            try {
                preparedStatement.setString(1, "john%");
                preparedStatement.execute();
                ResultSet rs = preparedStatement.getResultSet();
                while (rs.next()) {
                    rs.getString(1);
                }
            } finally {
                preparedStatement.close();
            }
        }
    }

    public static class ExecutePreparedStatementQueryAndIterateOverResults
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
            PreparedStatement preparedStatement =
                    connection.prepareStatement("select * from employee where name like ?");
            try {
                preparedStatement.setString(1, "john%");
                ResultSet rs = preparedStatement.executeQuery();
                while (rs.next()) {
                    rs.getString(1);
                }
            } finally {
                preparedStatement.close();
            }
        }
    }

    public static class ExecutePreparedStatementUpdate implements AppUnderTest, TransactionMarker {
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
            PreparedStatement preparedStatement =
                    connection.prepareStatement("update employee set name = ?");
            try {
                preparedStatement.setString(1, "nobody");
                preparedStatement.executeUpdate();
            } finally {
                preparedStatement.close();
            }
        }
    }

    public static class ExecutePreparedStatementInsertWithGeneratedKeys
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
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "insert into employee (name) values (?)", Statement.RETURN_GENERATED_KEYS);
            try {
                preparedStatement.setString(1, "nobody");
                preparedStatement.executeUpdate();
                ResultSet rs = preparedStatement.getGeneratedKeys();
                while (rs.next()) {
                    rs.getString(1);
                }
            } finally {
                preparedStatement.close();
            }
        }
    }

    public static class ExecutePreparedStatementLargeParamSetFirst
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
            String sql = "select * from employee where name like ?";
            for (int i = 0; i < 99; i++) {
                sql += " and name like ?";
            }
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            try {
                preparedStatement.setString(100, "john%");
                for (int i = 0; i < 99; i++) {
                    preparedStatement.setString(i + 1, "john%");
                }
                preparedStatement.execute();
                ResultSet rs = preparedStatement.getResultSet();
                while (rs.next()) {
                    rs.getString(1);
                }
            } finally {
                preparedStatement.close();
            }
        }
    }

    public static class PreparedStatementNullSql implements AppUnderTest, TransactionMarker {
        private Connection delegatingConnection;
        @Override
        public void executeApp() throws Exception {
            Connection connection = Connections.createConnection();
            delegatingConnection = new DelegatingConnection(connection) {
                @Override
                public PreparedStatement prepareStatement(String sql) throws SQLException {
                    return super.prepareStatement("select 1 from employee");
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
            delegatingConnection.prepareStatement(null);
        }
    }

    public static class ExecutePreparedStatementThrowing
            implements AppUnderTest, TransactionMarker {
        private Connection delegatingConnection;
        @Override
        public void executeApp() throws Exception {
            Connection connection = Connections.createConnection();
            delegatingConnection = new DelegatingConnection(connection) {
                @Override
                public PreparedStatement prepareStatement(String sql) throws SQLException {
                    return new DelegatingPreparedStatement(this, super.prepareStatement(sql)) {
                        @Override
                        public boolean execute() throws SQLException {
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
            PreparedStatement preparedStatement = delegatingConnection
                    .prepareStatement("select * from employee where name like ?");
            try {
                preparedStatement.setString(1, "john%");
                preparedStatement.execute();
            } catch (SQLException e) {
            } finally {
                preparedStatement.close();
            }
        }
    }

    public static class ExecutePreparedStatementWithTonsOfBindParametersAndIterateOverResults
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
            StringBuilder sql = new StringBuilder("select * from employee where name like ?");
            for (int i = 0; i < 200; i++) {
                sql.append(" and name like ?");
            }
            PreparedStatement preparedStatement = connection.prepareStatement(sql.toString());
            try {
                for (int i = 1; i < 202; i++) {
                    preparedStatement.setString(i, "john%");
                }
                preparedStatement.execute();
                ResultSet rs = preparedStatement.getResultSet();
                while (rs.next()) {
                    rs.getString(1);
                }
            } finally {
                preparedStatement.close();
            }
        }
    }

    public static class ExecutePreparedStatementWithSetNull
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
            PreparedStatement preparedStatement =
                    connection.prepareStatement("insert into employee (name, misc) values (?, ?)");
            try {
                preparedStatement.setNull(1, Types.VARCHAR);
                preparedStatement.setNull(2, Types.BINARY);
                preparedStatement.execute();
            } finally {
                preparedStatement.close();
            }
        }
    }

    public static class ExecutePreparedStatementWithBinary
            implements AppUnderTest, TransactionMarker {
        static {
            JdbcInstrumentationProperties.setDisplayBinaryParameterAsHex(
                    "insert into employee (name, misc) values (?, ?)", 2);
        }
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
            byte[] bytes = new byte[10];
            for (int i = 0; i < 10; i++) {
                bytes[i] = (byte) i;
            }
            PreparedStatement preparedStatement =
                    connection.prepareStatement("insert into employee (name, misc) values (?, ?)");
            PreparedStatement preparedStatement2 = connection
                    .prepareStatement("insert /**/ into employee (name, misc) values (?, ?)");
            try {
                preparedStatement.setString(1, "jane");
                preparedStatement.setBytes(2, bytes);
                preparedStatement.execute();
                preparedStatement2.setString(1, "jane");
                preparedStatement2.setBytes(2, bytes);
                preparedStatement2.execute();
            } finally {
                preparedStatement.close();
                preparedStatement2.close();
            }
        }
    }

    public static class ExecutePreparedStatementWithBinaryUsingSetObject
            implements AppUnderTest, TransactionMarker {
        static {
            JdbcInstrumentationProperties.setDisplayBinaryParameterAsHex(
                    "insert into employee (name, misc) values (?, ?)", 2);
        }
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
            byte[] bytes = new byte[10];
            for (int i = 0; i < 10; i++) {
                bytes[i] = (byte) i;
            }
            PreparedStatement preparedStatement =
                    connection.prepareStatement("insert into employee (name, misc) values (?, ?)");
            PreparedStatement preparedStatement2 = connection
                    .prepareStatement("insert /**/ into employee (name, misc) values (?, ?)");
            try {
                preparedStatement.setString(1, "jane");
                preparedStatement.setObject(2, bytes);
                preparedStatement.execute();
                preparedStatement2.setString(1, "jane");
                preparedStatement2.setObject(2, bytes);
                preparedStatement2.execute();
            } finally {
                preparedStatement.close();
                preparedStatement2.close();
            }
        }
    }

    public static class ExecutePreparedStatementWithBinaryStream
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
            byte[] bytes = new byte[10];
            for (int i = 0; i < 10; i++) {
                bytes[i] = (byte) i;
            }
            PreparedStatement preparedStatement =
                    connection.prepareStatement("insert into employee (name, misc) values (?, ?)");
            try {
                preparedStatement.setString(1, "jane");
                preparedStatement.setBinaryStream(2, new ByteArrayInputStream(bytes));
                preparedStatement.execute();
            } finally {
                preparedStatement.close();
            }
        }
    }

    public static class ExecutePreparedStatementWithCharacterStream
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
            PreparedStatement preparedStatement =
                    connection.prepareStatement("insert into employee (name, misc2) values (?, ?)");
            try {
                preparedStatement.setString(1, "jane");
                preparedStatement.setCharacterStream(2, new StringReader("abc"));
                preparedStatement.execute();
            } finally {
                preparedStatement.close();
            }
        }
    }

    public static class ExecutePreparedStatementWithClear
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
            PreparedStatement preparedStatement =
                    connection.prepareStatement("select * from employee where name like ?");
            try {
                preparedStatement.setString(1, "na%");
                preparedStatement.clearParameters();
                preparedStatement.setString(1, "john%");
                preparedStatement.execute();
                ResultSet rs = preparedStatement.getResultSet();
                while (rs.next()) {
                    rs.getString(1);
                }
            } finally {
                preparedStatement.close();
            }
        }
    }

    public static class ExecutePreparedStatementThatHasInternalGlowrootToken
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
            PreparedStatement preparedStatement =
                    connection.prepareStatement("select * from employee where name like ?");
            try {
                preparedStatement.setString(1, "{}");
                preparedStatement.execute();
                ResultSet rs = preparedStatement.getResultSet();
                while (rs.next()) {
                    rs.getString(1);
                }
            } finally {
                preparedStatement.close();
            }
        }
    }

}
