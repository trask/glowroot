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
package org.glowroot.agent.plugin.mongodb;

import java.net.InetSocketAddress;
import java.util.List;

import org.glowroot.agent.plugin.api.checker.Nullable;
import org.glowroot.agent.plugin.api.weaving.BindClassMeta;
import org.glowroot.agent.plugin.api.weaving.BindParameter;
import org.glowroot.agent.plugin.api.weaving.BindReceiver;
import org.glowroot.agent.plugin.api.weaving.BindReturn;
import org.glowroot.agent.plugin.api.weaving.Mixin;
import org.glowroot.agent.plugin.api.weaving.OnReturn;
import org.glowroot.agent.plugin.api.weaving.Pointcut;
import org.glowroot.agent.plugin.api.weaving.Shim;

public class ConnectionStringAspect {

    static final String DEST_BASE = "MongoDB";

    @Shim("com.mongodb.connection.Cluster")
    public interface Cluster {
        @Shim("com.mongodb.connection.ClusterSettings getSettings()")
        @Nullable
        ClusterSettings glowroot$getSettings();
    }

    @Shim("com.mongodb.connection.ClusterSettings")
    public interface ClusterSettings {
        @Nullable
        List<? extends ServerAddress> getHosts();
    }

    @Shim("com.mongodb.MongoAuthority")
    public interface MongoAuthority {
        @Nullable
        List<? extends ServerAddress> getServerAddresses();
    }

    @Shim("com.mongodb.ServerAddress")
    public interface ServerAddress {
        // unfortunately, "getHost" method was typo'd as "geHost" in mongo driver 1.4
        int getPort();
    }

    @Shim("com.mongodb.DBAddress")
    public interface DBAddress {
        @Nullable
        InetSocketAddress getSocketAddress();
    }

    // the field and method names are verbose since they will be mixed in to existing classes
    @Mixin({"com.mongodb.MongoClient", "com.mongodb.client.MongoClient",
            "com.mongodb.client.MongoDatabase", "com.mongodb.client.MongoCollection",
            "com.mongodb.Mongo", "com.mongodb.DB", "com.mongodb.DBCollection"})
    public static class HasConnectionStringImpl implements HasConnectionStringMixin {

        private transient volatile @Nullable String glowroot$connectionString;

        @Override
        public @Nullable String glowroot$getConnectionString() {
            return glowroot$connectionString;
        }

        @Override
        public void glowroot$setConnectionString(@Nullable String connectionString) {
            glowroot$connectionString = connectionString;
        }
    }

    // the method names are verbose since they will be mixed in to existing classes
    public interface HasConnectionStringMixin {

        @Nullable
        String glowroot$getConnectionString();

        void glowroot$setConnectionString(@Nullable String connectionString);
    }

    @Pointcut(className = "com.mongodb.client.internal.MongoClientImpl"
            + "|com.mongodb.client.MongoClientImpl|com.mongodb.Mongo",
            methodName = "<init>", methodParameterTypes = {"com.mongodb.connection.Cluster", ".."})
    public static class MongoClientsAdvice {

        @OnReturn
        public static void onReturn(@BindReceiver HasConnectionStringMixin client,
                @BindParameter @Nullable Cluster cluster,
                @BindClassMeta ServerAddressInvoker serverAddressInvoker) {
            if (cluster == null) {
                return;
            }
            ClusterSettings settings = cluster.glowroot$getSettings();
            if (settings == null) {
                return;
            }
            List<? extends ServerAddress> hosts = settings.getHosts();
            if (hosts == null) {
                return;
            }
            StringBuilder sb = new StringBuilder(DEST_BASE);
            boolean first = true;
            for (ServerAddress host : hosts) {
                if (host != null) {
                    appendAddress(sb, host, serverAddressInvoker, first);
                    first = false;
                }
            }
            if (!first) {
                sb.append(']');
            }
            client.glowroot$setConnectionString(sb.toString());
        }
    }

    @Pointcut(className = "com.mongodb.Mongo", methodName = "<init>",
            methodParameterTypes = {"com.mongodb.MongoAuthority", ".."})
    public static class OldMongoAdvice {

        @OnReturn
        public static void onReturn(@BindReceiver HasConnectionStringMixin client,
                @BindParameter @Nullable MongoAuthority authority,
                @BindClassMeta ServerAddressInvoker serverAddressInvoker) {
            if (authority == null) {
                return;
            }
            List<? extends ServerAddress> addresses = authority.getServerAddresses();
            if (addresses == null) {
                return;
            }
            StringBuilder sb = new StringBuilder(DEST_BASE);
            boolean first = true;
            for (ServerAddress address : addresses) {
                if (address != null) {
                    appendAddress(sb, address, serverAddressInvoker, first);
                    first = false;
                }
            }
            if (!first) {
                sb.append(']');
            }
            client.glowroot$setConnectionString(sb.toString());
        }
    }

    @Pointcut(className = "com.mongodb.Mongo", methodName = "<init>",
            methodParameterTypes = {"com.mongodb.ServerAddress"})
    public static class PrettyOldMongoOneArgAdvice {

        @OnReturn
        public static void onReturn(@BindReceiver HasConnectionStringMixin client,
                @BindParameter @Nullable ServerAddress address,
                @BindClassMeta ServerAddressInvoker serverAddressInvoker) {
            if (address == null) {
                return;
            }
            StringBuilder sb = new StringBuilder(DEST_BASE);
            appendAddress(sb, address, serverAddressInvoker, true);
            sb.append(']');
            client.glowroot$setConnectionString(sb.toString());
        }
    }

    @Pointcut(className = "com.mongodb.Mongo", methodName = "<init>",
            methodParameterTypes = {"com.mongodb.ServerAddress", "*", ".."})
    public static class PrettyOldMongoTwoArgAdvice {

        @OnReturn
        public static void onReturn(@BindReceiver HasConnectionStringMixin client,
                @BindParameter @Nullable ServerAddress address,
                @BindParameter @Nullable Object maybeAddress,
                @BindClassMeta ServerAddressInvoker serverAddressInvoker) {
            if (address == null) {
                return;
            }
            StringBuilder sb = new StringBuilder(DEST_BASE);
            appendAddress(sb, address, serverAddressInvoker, true);
            if (maybeAddress instanceof ServerAddress) {
                appendAddress(sb, (ServerAddress) maybeAddress, serverAddressInvoker, false);
            }
            sb.append(']');
            client.glowroot$setConnectionString(sb.toString());
        }
    }

    @Pointcut(className = "com.mongodb.Mongo", methodName = "<init>",
            methodParameterTypes = {"java.util.List", ".."})
    public static class PrettyOldMongoListAdvice {

        @OnReturn
        public static void onReturn(@BindReceiver HasConnectionStringMixin client,
                @BindParameter @Nullable List<?> seeds,
                @BindClassMeta ServerAddressInvoker serverAddressInvoker) {
            if (seeds == null) {
                return;
            }
            StringBuilder sb = new StringBuilder(DEST_BASE);
            boolean first = true;
            for (Object seed : seeds) {
                if (seed instanceof ServerAddress) {
                    appendAddress(sb, (ServerAddress) seed, serverAddressInvoker, first);
                    first = false;
                }
            }
            if (!first) {
                sb.append(']');
            }
            client.glowroot$setConnectionString(sb.toString());
        }
    }

    @Pointcut(className = "com.mongodb.Mongo", methodName = "<init>",
            methodParameterTypes = {"com.mongodb.DBAddress"})
    public static class ReallyOldMongoOneArgAdvice {

        @OnReturn
        public static void onReturn(@BindReceiver HasConnectionStringMixin client,
                @BindParameter @Nullable DBAddress address) {
            if (address == null) {
                return;
            }
            InetSocketAddress socketAddress = address.getSocketAddress();
            if (socketAddress == null) {
                return;
            }
            StringBuilder sb = new StringBuilder(DEST_BASE);
            appendAddress(sb, socketAddress, true);
            sb.append(']');
            client.glowroot$setConnectionString(sb.toString());
        }
    }

    @Pointcut(className = "com.mongodb.Mongo", methodName = "<init>",
            methodParameterTypes = {"com.mongodb.DBAddress", "*", ".."})
    public static class ReallyOldMongoTwoArgAdvice {

        @OnReturn
        public static void onReturn(@BindReceiver HasConnectionStringMixin client,
                @BindParameter @Nullable DBAddress address,
                @BindParameter @Nullable Object maybeAddress) {
            if (address == null) {
                return;
            }
            InetSocketAddress socketAddress = address.getSocketAddress();
            if (socketAddress == null) {
                return;
            }
            StringBuilder sb = new StringBuilder(DEST_BASE);
            appendAddress(sb, socketAddress, true);
            if (maybeAddress instanceof DBAddress) {
                InetSocketAddress rightSocketAddress =
                        ((DBAddress) maybeAddress).getSocketAddress();
                if (rightSocketAddress != null) {
                    appendAddress(sb, rightSocketAddress, false);
                }
            }
            sb.append(']');
            client.glowroot$setConnectionString(sb.toString());
        }
    }

    @Pointcut(className = "com.mongodb.client.MongoClient|com.mongodb.MongoClient",
            methodName = "*", methodParameterTypes = {".."},
            methodReturnType = "com.mongodb.client.MongoDatabase")
    public static class GetDatabaseAdvice {

        @OnReturn
        public static void onReturn(@BindReturn @Nullable HasConnectionStringMixin database,
                @BindReceiver HasConnectionStringMixin client) {
            if (database != null) {
                database.glowroot$setConnectionString(client.glowroot$getConnectionString());
            }
        }
    }

    @Pointcut(className = "com.mongodb.client.MongoDatabase", methodName = "*",
            methodParameterTypes = {".."}, methodReturnType = "com.mongodb.client.MongoCollection")
    public static class GetCollectionAdvice {

        @OnReturn
        public static void onReturn(@BindReturn @Nullable HasConnectionStringMixin collection,
                @BindReceiver HasConnectionStringMixin database) {
            if (collection != null) {
                collection.glowroot$setConnectionString(database.glowroot$getConnectionString());
            }
        }
    }

    // mongodb driver legacy API (prior to 3.7.0)

    @Pointcut(className = "com.mongodb.Mongo", methodName = "*",
            methodParameterTypes = {".."}, methodReturnType = "com.mongodb.DB")
    public static class GetDBAdvice {

        @OnReturn
        public static void onReturn(@BindReturn @Nullable HasConnectionStringMixin database,
                @BindReceiver HasConnectionStringMixin client) {
            if (database != null) {
                database.glowroot$setConnectionString(client.glowroot$getConnectionString());
            }
        }
    }

    @Pointcut(className = "com.mongodb.DB", methodName = "*", methodParameterTypes = {".."},
            methodReturnType = "com.mongodb.DBCollection")
    public static class GetDBCollectionAdvice {

        @OnReturn
        public static void onReturn(@BindReturn @Nullable HasConnectionStringMixin collection,
                @BindReceiver HasConnectionStringMixin database) {
            if (collection != null) {
                collection.glowroot$setConnectionString(database.glowroot$getConnectionString());
            }
        }
    }

    private static void appendAddress(StringBuilder sb, ServerAddress address,
            ServerAddressInvoker serverAddressInvoker, boolean first) {
        if (first) {
            sb.append(" [");
        } else {
            sb.append(',');
        }
        sb.append(serverAddressInvoker.getHost(address));
        sb.append(':');
        sb.append(address.getPort());
    }

    private static void appendAddress(StringBuilder sb, InetSocketAddress address, boolean first) {
        if (first) {
            sb.append(" [");
        } else {
            sb.append(',');
        }
        // this is not ideal
        sb.append(address.getAddress().getHostName());
        sb.append(':');
        sb.append(address.getPort());
    }
}
