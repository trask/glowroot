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

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.glowroot.agent.plugin.api.Logger;

public class JdbcUrlToDest {

    private static final Logger logger = Logger.getLogger(JdbcUrlToDest.class);

    private static final Set<String> loggedMessageForUnhandledUrl =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    // visible for testing
    // IMPORTANT when a new dest is added, also update queries.js so that it is formatted as SQL
    static String getDest(String url) {
        if (!url.startsWith("jdbc:")) {
            logUnhandledUrl(url);
            return "SQL";
        }
        int from = "jdbc:".length();
        if (url.startsWith("hsqldb:", from)) {
            return getHsqldbDest(url);
        } else if (url.startsWith("h2:", from)) {
            return getH2Dest(url);
        } else if (url.startsWith("postgresql:", 5)) {
            return getPostgresqlDest(url);
        } else if (url.startsWith("oracle:", 5)) {
            return getOracleDest(url);
        } else if (url.startsWith("sqlserver:", 5)) {
            return getSqlServerDest(url);
        }
        return "SQL";
    }

    // see http://hsqldb.org/doc/apidocs/org/hsqldb/jdbc/JDBCConnection.html
    private static String getHsqldbDest(String url) {
        int from = "jdbc:hsqldb:".length();
        if (url.startsWith("hsql:", from)) {
            return getHsqldbNetworkDest(url, from + "hsql:".length());
        } else if (url.startsWith("hsqls:", from)) {
            return getHsqldbNetworkDest(url, from + "hsqls:".length());
        } else if (url.startsWith("http:", from)) {
            return getHsqldbNetworkDest(url, from + "http:".length());
        } else if (url.startsWith("https:", from)) {
            return getHsqldbNetworkDest(url, from + "https:".length());
        } else if (url.startsWith("mem:", from)) {
            return "HSQLDB [mem]";
        } else if (url.startsWith("file:", from)) {
            return "HSQLDB [file]";
        } else if (url.startsWith("res:", from)) {
            return "HSQLDB [res]";
        } else if (url.equals("jdbc:hsqldb:.") || url.startsWith("jdbc:hsqldb:.;")) {
            // legacy form
            return "HSQLDB [mem]";
        } else {
            // legacy form
            return "HSQLDB [file]";
        }
    }

    private static String getHsqldbNetworkDest(String url, int from) {
        if (url.startsWith("//", from)) {
            return getDestCommon(url, from + "//".length(), '/', ';', "HSQLDB");
        } else {
            logUnhandledUrl(url);
            return "HSQLDB";
        }
    }

    // see http://www.h2database.com/html/features.html#database_url
    private static String getH2Dest(String url) {
        int from = "jdbc:h2:".length();
        if (url.startsWith("tcp:", from)) {
            return getH2NetworkDest(url, from + "tcp:".length());
        } else if (url.startsWith("ssl:", from)) {
            return getH2NetworkDest(url, from + "ssl:".length());
        } else if (url.startsWith("mem:", from)) {
            return "H2 [mem]";
        } else if (url.startsWith("file:", from)) {
            return "H2 [file]";
        } else if (url.startsWith("zip:", from)) {
            return "H2 [file]";
        } else {
            return "H2 [file]";
        }
    }

    private static String getH2NetworkDest(String url, int from) {
        if (url.startsWith("//", from)) {
            return getDestCommon(url, from + "//".length(), '/', ';', "H2");
        } else {
            logUnhandledUrl(url);
            return "H2";
        }
    }

    // see https://jdbc.postgresql.org/documentation/head/connect.html
    private static String getPostgresqlDest(String url) {
        int from = "jdbc:postgresql:".length();
        if (url.startsWith("//", from)) {
            return getDestCommon(url, from + "//".length(), '/', '?', "PostgreSQL");
        } else {
            return "PostgreSQL [localhost]";
        }
    }

    // see https://docs.oracle.com/cd/E11882_01/appdev.112/e13995/oracle/jdbc/OracleDriver.html
    private static String getOracleDest(String url) {
        int from = "jdbc:oracle:".length();
        if (url.startsWith("thin:", from)) {
            return getOracleDest(url, from + "thin:".length());
        } else if (url.startsWith("oci:", from)) {
            return getOracleDest(url, from + "oci:".length());
        } else if (url.startsWith("oci8:", from)) {
            return getOracleDest(url, from + "oci8:".length());
        } else {
            logUnhandledUrl(url);
            return "Oracle";
        }
    }

    private static String getOracleDest(String url, int from) {
        int index = url.indexOf('@', from);
        if (index == -1) {
            logUnhandledUrl(url);
            return "Oracle";
        }
        if (index == url.length() - 1) {
            // Oracle "bequeath" connection
            return "Oracle";
        }
        int newFrom = index + 1; // skipping over the '@'
        if (url.startsWith("//", newFrom)) {
            // service name, which is case insensitive, to convert everything to lowercase
            return combineWithLowercaseNode("Oracle", url.substring(newFrom + "//".length()));
        } else if (url.startsWith("(", newFrom)) {
            // Oracle Net connection descriptor (not supported yet)
            return "Oracle";
        } else {
            int sidStartIndex = url.lastIndexOf(':');
            if (sidStartIndex < newFrom) {
                // TNS names alias, which is case sensitive
                return combine("Oracle", url.substring(newFrom));
            } else {
                // SID, which is case sensitive
                return combine("Oracle", url.substring(newFrom, sidStartIndex).toLowerCase()
                        + url.substring(sidStartIndex));
            }
        }
    }

    // see https://docs.microsoft.com/en-us/sql/connect/jdbc/building-the-connection-url
    private static String getSqlServerDest(String url) {
        int from = "jdbc:sqlserver:".length();
        if (!url.startsWith("//", from)) {
            logUnhandledUrl(url);
            return "SQL Server";
        }
        from += 2;
        if (from == url.length()) {
            return "SQL Server [localhost]";
        }
        int index = url.indexOf(';', from);
        if (index == from) {
            return "SQL Server [localhost]";
        }
        if (index == -1) {
            return combineWithLowercaseNode("SQL Server", url.substring(from));
        } else {
            return combineWithLowercaseNode("SQL Server", url.substring(from, index));
        }
    }

    // get everything before "/" or separator
    private static String getDestCommon(String url, int from, char c1, char c2, String base) {
        int end = -1;
        for (int i = from; i < url.length(); i++) {
            char c = url.charAt(i);
            if (c == c1 || c == c2) {
                end = i;
                break;
            }
        }
        if (end == -1) {
            return combineWithLowercaseNode(base, url.substring(from));
        } else {
            return combineWithLowercaseNode(base, url.substring(from, end));
        }
    }

    private static String combineWithLowercaseNode(String base, String node) {
        return combine(base, node.toLowerCase(Locale.ENGLISH));
    }

    private static String combine(String base, String node) {
        return base + " [" + node + "]";
    }

    private static void logUnhandledUrl(String url) {
        if (loggedMessageForUnhandledUrl.size() < 10 && loggedMessageForUnhandledUrl.add(url)) {
            logger.warn("parsing out the hostname from this jdbc url is not implemented, please"
                    + " report this jdbc url to the Glowroot project in order to improve the"
                    + " display under the network graph tab: {}", url);
        }
    }
}
