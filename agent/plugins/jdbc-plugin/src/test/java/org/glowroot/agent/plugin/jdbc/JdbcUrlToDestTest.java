/*
 * Copyright 2019 the original author or authors.
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
package org.glowroot.agent.plugin.jdbc;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.glowroot.agent.plugin.jdbc.JdbcUrlToDest.getDest;

public class JdbcUrlToDestTest {

    @Test
    public void shouldGetDestForHsqldbNetworkUrls() {
        assertThat(getDest("jdbc:hsqldb:hsql://Host:1234/alias;key=val/ue"))
                .isEqualTo("HSQLDB [host:1234]");
        assertThat(getDest("jdbc:hsqldb:hsqls://Host:1234/alias;key=val/ue"))
                .isEqualTo("HSQLDB [host:1234]");
        assertThat(getDest("jdbc:hsqldb:http://Host:1234/alias;key=val/ue"))
                .isEqualTo("HSQLDB [host:1234]");
        assertThat(getDest("jdbc:hsqldb:https://Host:1234/alias;key=val/ue"))
                .isEqualTo("HSQLDB [host:1234]");

        assertThat(getDest("jdbc:hsqldb:hsql://Host:1234;key=val/ue"))
                .isEqualTo("HSQLDB [host:1234]");
        assertThat(getDest("jdbc:hsqldb:hsql://Host:1234/alias"))
                .isEqualTo("HSQLDB [host:1234]");
        assertThat(getDest("jdbc:hsqldb:hsql://Host:1234/alias;"))
                .isEqualTo("HSQLDB [host:1234]");
        assertThat(getDest("jdbc:hsqldb:hsql://Host:1234/;"))
                .isEqualTo("HSQLDB [host:1234]");
        assertThat(getDest("jdbc:hsqldb:hsql://Host:1234/"))
                .isEqualTo("HSQLDB [host:1234]");
        assertThat(getDest("jdbc:hsqldb:hsql://Host:1234"))
                .isEqualTo("HSQLDB [host:1234]");

        assertThat(getDest("jdbc:hsqldb:hsql://Host;key=val/ue"))
                .isEqualTo("HSQLDB [host]");
        assertThat(getDest("jdbc:hsqldb:hsql://Host/alias"))
                .isEqualTo("HSQLDB [host]");
        assertThat(getDest("jdbc:hsqldb:hsql://Host/alias;"))
                .isEqualTo("HSQLDB [host]");
        assertThat(getDest("jdbc:hsqldb:hsql://Host/;"))
                .isEqualTo("HSQLDB [host]");
        assertThat(getDest("jdbc:hsqldb:hsql://Host/"))
                .isEqualTo("HSQLDB [host]");
        assertThat(getDest("jdbc:hsqldb:hsql://Host"))
                .isEqualTo("HSQLDB [host]");
    }

    @Test
    public void shouldGetDestForHsqldbInProcessTransientUrls() {
        assertThat(getDest("jdbc:hsqldb:mem:alias;key=val/ue"))
                .isEqualTo("HSQLDB [mem]");
        assertThat(getDest("jdbc:hsqldb:mem:alias;"))
                .isEqualTo("HSQLDB [mem]");
        assertThat(getDest("jdbc:hsqldb:mem:;key=val/ue"))
                .isEqualTo("HSQLDB [mem]");
        assertThat(getDest("jdbc:hsqldb:mem:;"))
                .isEqualTo("HSQLDB [mem]");
        assertThat(getDest("jdbc:hsqldb:mem:"))
                .isEqualTo("HSQLDB [mem]");

        // legacy form
        assertThat(getDest("jdbc:hsqldb:.;key=val/ue"))
                .isEqualTo("HSQLDB [mem]");
        assertThat(getDest("jdbc:hsqldb:.;"))
                .isEqualTo("HSQLDB [mem]");
        assertThat(getDest("jdbc:hsqldb:."))
                .isEqualTo("HSQLDB [mem]");
    }

    @Test
    public void shouldGetDestForHsqldbInProcessPersistentUrls() {
        assertThat(getDest("jdbc:hsqldb:file:~/x/y;key=val/ue"))
                .isEqualTo("HSQLDB [file]");
        assertThat(getDest("jdbc:hsqldb:file:~/x/y;"))
                .isEqualTo("HSQLDB [file]");
        assertThat(getDest("jdbc:hsqldb:file:~/x/y"))
                .isEqualTo("HSQLDB [file]");

        assertThat(getDest("jdbc:hsqldb:res:~/x/y;key=val/ue"))
                .isEqualTo("HSQLDB [res]");
        assertThat(getDest("jdbc:hsqldb:res:~/x/y;"))
                .isEqualTo("HSQLDB [res]");
        assertThat(getDest("jdbc:hsqldb:res:~/x/y"))
                .isEqualTo("HSQLDB [res]");

        // legacy form
        assertThat(getDest("jdbc:hsqldb:~/x/y;key=val/ue"))
                .isEqualTo("HSQLDB [file]");
        assertThat(getDest("jdbc:hsqldb:~/x/y;"))
                .isEqualTo("HSQLDB [file]");
        assertThat(getDest("jdbc:hsqldb:~/x/y"))
                .isEqualTo("HSQLDB [file]");
    }

    @Test
    public void shouldGetDestForH2NetworkUrls() {
        assertThat(getDest("jdbc:h2:tcp://Host:1234/~/test;key=val/ue"))
                .isEqualTo("H2 [host:1234]");
        assertThat(getDest("jdbc:h2:ssl://Host:1234/~/test;key=val/ue"))
                .isEqualTo("H2 [host:1234]");

        assertThat(getDest("jdbc:h2:tcp://Host:1234;key=val/ue"))
                .isEqualTo("H2 [host:1234]");
        assertThat(getDest("jdbc:h2:tcp://Host:1234/~/test"))
                .isEqualTo("H2 [host:1234]");
        assertThat(getDest("jdbc:h2:tcp://Host:1234/~/test;"))
                .isEqualTo("H2 [host:1234]");
        assertThat(getDest("jdbc:h2:tcp://Host:1234/;"))
                .isEqualTo("H2 [host:1234]");
        assertThat(getDest("jdbc:h2:tcp://Host:1234/"))
                .isEqualTo("H2 [host:1234]");
        assertThat(getDest("jdbc:h2:tcp://Host:1234"))
                .isEqualTo("H2 [host:1234]");

        assertThat(getDest("jdbc:h2:tcp://Host;key=val/ue"))
                .isEqualTo("H2 [host]");
        assertThat(getDest("jdbc:h2:tcp://Host/~/test"))
                .isEqualTo("H2 [host]");
        assertThat(getDest("jdbc:h2:tcp://Host/~/test;"))
                .isEqualTo("H2 [host]");
        assertThat(getDest("jdbc:h2:tcp://Host/;"))
                .isEqualTo("H2 [host]");
        assertThat(getDest("jdbc:h2:tcp://Host/"))
                .isEqualTo("H2 [host]");
        assertThat(getDest("jdbc:h2:tcp://Host"))
                .isEqualTo("H2 [host]");

    }

    @Test
    public void shouldGetDestForH2InProcessTransientUrls() {
        assertThat(getDest("jdbc:h2:mem:test;key=val/ue"))
                .isEqualTo("H2 [mem]");
        assertThat(getDest("jdbc:h2:mem:test;"))
                .isEqualTo("H2 [mem]");
        assertThat(getDest("jdbc:h2:mem:;key=val/ue"))
                .isEqualTo("H2 [mem]");
        assertThat(getDest("jdbc:h2:mem:;"))
                .isEqualTo("H2 [mem]");
        assertThat(getDest("jdbc:h2:mem:"))
                .isEqualTo("H2 [mem]");
    }

    @Test
    public void shouldGetDestForH2InProcessPersistentUrls() {
        assertThat(getDest("jdbc:h2:file:~/x/y;key=val/ue"))
                .isEqualTo("H2 [file]");
        assertThat(getDest("jdbc:h2:file:~/x/y;"))
                .isEqualTo("H2 [file]");
        assertThat(getDest("jdbc:h2:file:~/x/y"))
                .isEqualTo("H2 [file]");
    }

    @Test
    public void shouldGetDestForPostgresqlUrls() {
        assertThat(getDest("jdbc:postgresql:database?key=val/ue"))
                .isEqualTo("PostgreSQL [localhost]");
        assertThat(getDest("jdbc:postgresql:database?"))
                .isEqualTo("PostgreSQL [localhost]");
        assertThat(getDest("jdbc:postgresql:database"))
                .isEqualTo("PostgreSQL [localhost]");

        assertThat(getDest("jdbc:postgresql:/?key=val/ue"))
                .isEqualTo("PostgreSQL [localhost]");
        assertThat(getDest("jdbc:postgresql:/?"))
                .isEqualTo("PostgreSQL [localhost]");
        assertThat(getDest("jdbc:postgresql:/"))
                .isEqualTo("PostgreSQL [localhost]");

        assertThat(getDest("jdbc:postgresql://Host/database?key=val/ue"))
                .isEqualTo("PostgreSQL [host]");
        assertThat(getDest("jdbc:postgresql://Host/database?"))
                .isEqualTo("PostgreSQL [host]");
        assertThat(getDest("jdbc:postgresql://Host/database"))
                .isEqualTo("PostgreSQL [host]");

        assertThat(getDest("jdbc:postgresql://Host/?key=val/ue"))
                .isEqualTo("PostgreSQL [host]");
        assertThat(getDest("jdbc:postgresql://Host/?"))
                .isEqualTo("PostgreSQL [host]");
        assertThat(getDest("jdbc:postgresql://Host/"))
                .isEqualTo("PostgreSQL [host]");

        assertThat(getDest("jdbc:postgresql://Host?key=val/ue"))
                .isEqualTo("PostgreSQL [host]");
        assertThat(getDest("jdbc:postgresql://Host?"))
                .isEqualTo("PostgreSQL [host]");
        assertThat(getDest("jdbc:postgresql://Host"))
                .isEqualTo("PostgreSQL [host]");

        assertThat(getDest("jdbc:postgresql://Host:1234/database?key=val/ue"))
                .isEqualTo("PostgreSQL [host:1234]");
        assertThat(getDest("jdbc:postgresql://Host:1234/database?"))
                .isEqualTo("PostgreSQL [host:1234]");
        assertThat(getDest("jdbc:postgresql://Host:1234/database"))
                .isEqualTo("PostgreSQL [host:1234]");

        assertThat(getDest("jdbc:postgresql://Host:1234/?key=val/ue"))
                .isEqualTo("PostgreSQL [host:1234]");
        assertThat(getDest("jdbc:postgresql://Host:1234/?"))
                .isEqualTo("PostgreSQL [host:1234]");
        assertThat(getDest("jdbc:postgresql://Host:1234/"))
                .isEqualTo("PostgreSQL [host:1234]");

        assertThat(getDest("jdbc:postgresql://Host:1234?key=val/ue"))
                .isEqualTo("PostgreSQL [host:1234]");
        assertThat(getDest("jdbc:postgresql://Host:1234?"))
                .isEqualTo("PostgreSQL [host:1234]");
        assertThat(getDest("jdbc:postgresql://Host:1234"))
                .isEqualTo("PostgreSQL [host:1234]");
    }

    @Test
    public void shouldGetDestForOracleThinUrls() {
        // with service name (note: service names are case-insensitive)
        assertThat(getDest("jdbc:oracle:thin:@//Host:1234/ServiceName"))
                .isEqualTo("Oracle [host:1234/servicename]");
        assertThat(getDest("jdbc:oracle:thin:@//Host/ServiceName"))
                .isEqualTo("Oracle [host/servicename]");

        assertThat(getDest("jdbc:oracle:thin:user/password@//Host:1234/ServiceName"))
                .isEqualTo("Oracle [host:1234/servicename]");
        assertThat(getDest("jdbc:oracle:thin:user/password@//Host/ServiceName"))
                .isEqualTo("Oracle [host/servicename]");

        // with SID (note: SIDs are case-sensitive)
        assertThat(getDest("jdbc:oracle:thin:@Host:1234:SID"))
                .isEqualTo("Oracle [host:1234:SID]");
        assertThat(getDest("jdbc:oracle:thin:@Host:SID"))
                .isEqualTo("Oracle [host:SID]");

        assertThat(getDest("jdbc:oracle:thin:user/password@Host:1234:SID"))
                .isEqualTo("Oracle [host:1234:SID]");
        assertThat(getDest("jdbc:oracle:thin:user/password@Host:SID"))
                .isEqualTo("Oracle [host:SID]");

        // with TNS names alias
        assertThat(getDest("jdbc:oracle:thin:@tns_entry"))
                .isEqualTo("Oracle [tns_entry]");

        assertThat(getDest("jdbc:oracle:thin:user/password@tns_entry"))
                .isEqualTo("Oracle [tns_entry]");

        // with Oracle Net connection descriptor
        // not supported (yet)
        assertThat(getDest("jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)"
                + "[host=host)(PORT=1521))(CONNECT_DATA=(SERVICE_NAME=Orcl)))"))
                        .isEqualTo("Oracle");
        assertThat(getDest("jdbc:oracle:thin:@(DESCRIPTION=(LOAD_BALANCE=on)"
                + "(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)[host=host1)(PORT=1521))"
                + " (ADDRESS=(PROTOCOL=TCP)[host=host2)(PORT=1521)))"
                + " (CONNECT_DATA=(SERVICE_NAME=Orcl)))"))
                        .isEqualTo("Oracle");

    }

    @Test
    public void shouldGetDestForOracleOciUrls() {
        // with service name (note: service names are case-insensitive)
        assertThat(getDest("jdbc:oracle:oci:@//Host:1234/ServiceName"))
                .isEqualTo("Oracle [host:1234/servicename]");
        assertThat(getDest("jdbc:oracle:oci:@//Host/ServiceName"))
                .isEqualTo("Oracle [host/servicename]");

        assertThat(getDest("jdbc:oracle:oci:user/password@//Host:1234/ServiceName"))
                .isEqualTo("Oracle [host:1234/servicename]");
        assertThat(getDest("jdbc:oracle:oci:user/password@//Host/ServiceName"))
                .isEqualTo("Oracle [host/servicename]");

        // with SID (note: SIDs are case-sensitive)
        assertThat(getDest("jdbc:oracle:oci:@Host:1234:SID"))
                .isEqualTo("Oracle [host:1234:SID]");
        assertThat(getDest("jdbc:oracle:oci:@Host:SID"))
                .isEqualTo("Oracle [host:SID]");

        assertThat(getDest("jdbc:oracle:oci:user/password@Host:1234:SID"))
                .isEqualTo("Oracle [host:1234:SID]");
        assertThat(getDest("jdbc:oracle:oci:user/password@Host:SID"))
                .isEqualTo("Oracle [host:SID]");

        // with TNS names alias
        assertThat(getDest("jdbc:oracle:oci:@tns_entry"))
                .isEqualTo("Oracle [tns_entry]");

        assertThat(getDest("jdbc:oracle:oci:user/password@tns_entry"))
                .isEqualTo("Oracle [tns_entry]");

        // with Oracle Net connection descriptor
        // not supported (yet)
        assertThat(getDest("jdbc:oracle:oci:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)"
                + "[host=host)(PORT=1521))(CONNECT_DATA=(SERVICE_NAME=Orcl)))"))
                        .isEqualTo("Oracle");
        assertThat(getDest("jdbc:oracle:oci:@(DESCRIPTION=(LOAD_BALANCE=on)"
                + "(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)[host=host1)(PORT=1521))"
                + " (ADDRESS=(PROTOCOL=TCP)[host=host2)(PORT=1521)))"
                + " (CONNECT_DATA=(SERVICE_NAME=service_name)))"))
                        .isEqualTo("Oracle");

        // "bequeath" connection
        assertThat(getDest("jdbc:oracle:oci:@"))
                .isEqualTo("Oracle");
    }

    @Test
    public void shouldGetDestForOracleOci8Urls() {
        // with service name (note: service names are case-insensitive)
        assertThat(getDest("jdbc:oracle:oci8:@//Host:1234/ServiceName"))
                .isEqualTo("Oracle [host:1234/servicename]");
        assertThat(getDest("jdbc:oracle:oci8:@//Host/ServiceName"))
                .isEqualTo("Oracle [host/servicename]");

        assertThat(getDest("jdbc:oracle:oci8:user/password@//Host:1234/ServiceName"))
                .isEqualTo("Oracle [host:1234/servicename]");
        assertThat(getDest("jdbc:oracle:oci8:user/password@//Host/ServiceName"))
                .isEqualTo("Oracle [host/servicename]");

        // with SID (note: SIDs are case-sensitive)
        assertThat(getDest("jdbc:oracle:oci8:@Host:1234:SID"))
                .isEqualTo("Oracle [host:1234:SID]");
        assertThat(getDest("jdbc:oracle:oci8:@Host:SID"))
                .isEqualTo("Oracle [host:SID]");

        assertThat(getDest("jdbc:oracle:oci8:user/password@Host:1234:SID"))
                .isEqualTo("Oracle [host:1234:SID]");
        assertThat(getDest("jdbc:oracle:oci8:user/password@Host:SID"))
                .isEqualTo("Oracle [host:SID]");

        // with TNS names alias
        assertThat(getDest("jdbc:oracle:oci8:@tns_entry"))
                .isEqualTo("Oracle [tns_entry]");

        assertThat(getDest("jdbc:oracle:oci8:user/password@tns_entry"))
                .isEqualTo("Oracle [tns_entry]");

        // with Oracle Net connection descriptor
        // not supported (yet)
        assertThat(getDest("jdbc:oracle:oci8:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)"
                + "[host=host)(PORT=1521))(CONNECT_DATA=(SERVICE_NAME=Orcl)))"))
                        .isEqualTo("Oracle");
        assertThat(getDest("jdbc:oracle:oci8:@(DESCRIPTION=(LOAD_BALANCE=on)"
                + "(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)[host=host1)(PORT=1521))"
                + " (ADDRESS=(PROTOCOL=TCP)[host=host2)(PORT=1521)))"
                + " (CONNECT_DATA=(SERVICE_NAME=service_name)))"))
                        .isEqualTo("Oracle");

        // "bequeath" connection
        assertThat(getDest("jdbc:oracle:oci8:@"))
                .isEqualTo("Oracle");
    }

    @Test
    public void shouldGetDestForSqlServerUrls() {
        assertThat(getDest("jdbc:sqlserver://"))
                .isEqualTo("SQL Server [localhost]");
        assertThat(getDest("jdbc:sqlserver://Host"))
                .isEqualTo("SQL Server [host]");
        assertThat(getDest("jdbc:sqlserver://Host\\Instance"))
                .isEqualTo("SQL Server [host\\instance]");
        assertThat(getDest("jdbc:sqlserver://Host:1234"))
                .isEqualTo("SQL Server [host:1234]");
        assertThat(getDest("jdbc:sqlserver://Host\\Instance:1234"))
                .isEqualTo("SQL Server [host\\instance:1234]");

        assertThat(getDest("jdbc:sqlserver://;key=val/ue"))
                .isEqualTo("SQL Server [localhost]");
        assertThat(getDest("jdbc:sqlserver://Host;key=val/ue"))
                .isEqualTo("SQL Server [host]");
        assertThat(getDest("jdbc:sqlserver://Host\\Instance;key=val/ue"))
                .isEqualTo("SQL Server [host\\instance]");
        assertThat(getDest("jdbc:sqlserver://Host:1234;key=val/ue"))
                .isEqualTo("SQL Server [host:1234]");
        assertThat(getDest("jdbc:sqlserver://Host\\Instance:1234;key=val/ue"))
                .isEqualTo("SQL Server [host\\instance:1234]");
    }
}
