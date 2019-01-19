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
package org.glowroot.agent.plugin.jdbc;

import org.glowroot.agent.plugin.api.Agent;
import org.glowroot.agent.plugin.api.Logger;
import org.glowroot.agent.plugin.api.MessageSupplier;
import org.glowroot.agent.plugin.api.ThreadContext;
import org.glowroot.agent.plugin.api.Timer;
import org.glowroot.agent.plugin.api.TimerName;
import org.glowroot.agent.plugin.api.TraceEntry;
import org.glowroot.agent.plugin.api.checker.Nullable;
import org.glowroot.agent.plugin.api.config.BooleanProperty;
import org.glowroot.agent.plugin.api.config.ConfigService;
import org.glowroot.agent.plugin.api.weaving.BindParameter;
import org.glowroot.agent.plugin.api.weaving.BindReceiver;
import org.glowroot.agent.plugin.api.weaving.BindReturn;
import org.glowroot.agent.plugin.api.weaving.BindThrowable;
import org.glowroot.agent.plugin.api.weaving.BindTraveler;
import org.glowroot.agent.plugin.api.weaving.IsEnabled;
import org.glowroot.agent.plugin.api.weaving.Mixin;
import org.glowroot.agent.plugin.api.weaving.OnAfter;
import org.glowroot.agent.plugin.api.weaving.OnBefore;
import org.glowroot.agent.plugin.api.weaving.OnReturn;
import org.glowroot.agent.plugin.api.weaving.OnThrow;
import org.glowroot.agent.plugin.api.weaving.Pointcut;
import org.glowroot.agent.plugin.api.weaving.Shim;
import org.glowroot.agent.plugin.jdbc.StatementAspect.HasStatementMirrorMixin;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ConnectionAspect {

    private static final Logger logger = Logger.getLogger(ConnectionAspect.class);

    private static final ConfigService configService = Agent.getConfigService("jdbc");

    private static final BooleanProperty capturePreparedStatementCreation =
            configService.getBooleanProperty("capturePreparedStatementCreation");
    private static final BooleanProperty captureConnectionClose =
            configService.getBooleanProperty("captureConnectionClose");
    private static final BooleanProperty captureConnectionLifecycleTraceEntries =
            configService.getBooleanProperty("captureConnectionLifecycleTraceEntries");
    private static final BooleanProperty captureTransactionLifecycleTraceEntries =
            configService.getBooleanProperty("captureTransactionLifecycleTraceEntries");

    private static volatile boolean loggedExceptionInGetUrl;

    @Shim("java.sql.Connection")
    public interface Connection {
        @Shim("java.sql.DatabaseMetaData getMetaData()")
        @Nullable
        DatabaseMetaData glowroot$getMetaData();
    }

    @Shim("java.sql.DatabaseMetaData")
    public interface DatabaseMetaData {
        @Nullable
        String getURL();
    }

    // ===================== Mixin =====================

    // the field and method names are verbose since they will be mixed in to existing classes
    @Mixin("java.sql.Connection")
    public static class ConnectionImpl implements ConnectionMixin {

        // does not need to be volatile, app/framework must provide visibility of Connections if
        // used across threads and this can piggyback
        private transient @Nullable String glowroot$dest;

        @Override
        public @Nullable String glowroot$getDest() {
            return glowroot$dest;
        }

        @Override
        public void glowroot$setDest(@Nullable String dest) {
            glowroot$dest = dest;
        }
    }

    // the method names are verbose since they will be mixed in to existing classes
    public interface ConnectionMixin {

        @Nullable
        String glowroot$getDest();

        void glowroot$setDest(@Nullable String dest);
    }

    // ===================== Statement Preparation =====================

    // capture the sql used to create the PreparedStatement
    @Pointcut(className = "java.sql.Connection", methodName = "prepare*",
            methodParameterTypes = {"java.lang.String", ".."}, nestingGroup = "jdbc",
            timerName = "jdbc prepare")
    public static class PrepareAdvice {
        private static final TimerName timerName = Agent.getTimerName(PrepareAdvice.class);
        @OnBefore
        public static @Nullable Timer onBefore(ThreadContext context) {
            if (capturePreparedStatementCreation.value()) {
                return context.startTimer(timerName);
            } else {
                return null;
            }
        }
        @OnReturn
        public static <T extends Connection & ConnectionMixin> void onReturn(
                @BindReturn @Nullable HasStatementMirrorMixin preparedStatement,
                @BindReceiver T connection, @BindParameter @Nullable String sql) {
            if (preparedStatement == null || sql == null) {
                // seems nothing sensible to do here other than ignore
                return;
            }
            preparedStatement.glowroot$setStatementMirror(
                    new PreparedStatementMirror(getDest(connection), sql));
        }
        @OnAfter
        public static void onAfter(@BindTraveler @Nullable Timer timer) {
            if (timer != null) {
                timer.stop();
            }
        }
    }

    @Pointcut(className = "java.sql.Connection", methodName = "createStatement",
            methodParameterTypes = {".."}, nestingGroup = "jdbc")
    public static class CreateStatementAdvice {
        @OnReturn
        public static <T extends Connection & ConnectionMixin> void onReturn(
                @BindReturn @Nullable HasStatementMirrorMixin statement,
                @BindReceiver T connection) {
            if (statement == null) {
                // seems nothing sensible to do here other than ignore
                return;
            }
            statement.glowroot$setStatementMirror(new StatementMirror(getDest(connection)));
        }
    }

    @Pointcut(className = "java.sql.Connection", methodName = "commit", methodParameterTypes = {},
            nestingGroup = "jdbc", timerName = "jdbc commit")
    public static class CommitAdvice {
        private static final TimerName timerName = Agent.getTimerName(CommitAdvice.class);
        @OnBefore
        public static TraceEntry onBefore(ThreadContext context) {
            return context.startTraceEntry(MessageSupplier.create("jdbc commit"), timerName);
        }
        @OnReturn
        public static void onReturn(@BindTraveler TraceEntry traceEntry) {
            traceEntry.endWithLocationStackTrace(JdbcPluginProperties.stackTraceThresholdMillis(),
                    MILLISECONDS);
        }
        @OnThrow
        public static void onThrow(@BindThrowable Throwable t,
                @BindTraveler TraceEntry traceEntry) {
            traceEntry.endWithError(t);
        }
    }

    @Pointcut(className = "java.sql.Connection", methodName = "rollback", methodParameterTypes = {},
            nestingGroup = "jdbc", timerName = "jdbc rollback")
    public static class RollbackAdvice {
        private static final TimerName timerName = Agent.getTimerName(RollbackAdvice.class);
        @OnBefore
        public static TraceEntry onBefore(ThreadContext context) {
            return context.startTraceEntry(MessageSupplier.create("jdbc rollback"), timerName);
        }
        @OnReturn
        public static void onReturn(@BindTraveler TraceEntry traceEntry) {
            traceEntry.endWithLocationStackTrace(JdbcPluginProperties.stackTraceThresholdMillis(),
                    MILLISECONDS);
        }
        @OnThrow
        public static void onThrow(@BindThrowable Throwable t,
                @BindTraveler TraceEntry traceEntry) {
            traceEntry.endWithError(t);
        }
    }

    @Pointcut(className = "java.sql.Connection", methodName = "close", methodParameterTypes = {},
            nestingGroup = "jdbc", timerName = "jdbc connection close")
    public static class CloseAdvice {
        private static final TimerName timerName = Agent.getTimerName(CloseAdvice.class);
        @IsEnabled
        public static boolean isEnabled() {
            return captureConnectionClose.value() || captureConnectionLifecycleTraceEntries.value();
        }
        @OnBefore
        public static Object onBefore(ThreadContext context) {
            if (captureConnectionLifecycleTraceEntries.value()) {
                return context.startTraceEntry(MessageSupplier.create("jdbc connection close"),
                        timerName);
            } else {
                return context.startTimer(timerName);
            }
        }
        @OnReturn
        public static void onReturn(@BindTraveler Object entryOrTimer) {
            if (entryOrTimer instanceof TraceEntry) {
                ((TraceEntry) entryOrTimer).endWithLocationStackTrace(
                        JdbcPluginProperties.stackTraceThresholdMillis(), MILLISECONDS);
            } else {
                ((Timer) entryOrTimer).stop();
            }
        }
        @OnThrow
        public static void onThrow(@BindThrowable Throwable t, @BindTraveler Object entryOrTimer) {
            if (entryOrTimer instanceof TraceEntry) {
                ((TraceEntry) entryOrTimer).endWithError(t);
            } else {
                ((Timer) entryOrTimer).stop();
            }
        }
    }

    @Pointcut(className = "java.sql.Connection", methodName = "setAutoCommit",
            methodParameterTypes = {"boolean"}, nestingGroup = "jdbc",
            timerName = "jdbc set autocommit")
    public static class SetAutoCommitAdvice {
        private static final TimerName timerName = Agent.getTimerName(SetAutoCommitAdvice.class);
        @IsEnabled
        public static boolean isEnabled() {
            return captureTransactionLifecycleTraceEntries.value();
        }
        @OnBefore
        public static TraceEntry onBefore(ThreadContext context,
                @BindParameter boolean autoCommit) {
            return context.startTraceEntry(
                    MessageSupplier.create("jdbc set autocommit: {}", Boolean.toString(autoCommit)),
                    timerName);
        }
        @OnReturn
        public static void onReturn(@BindTraveler TraceEntry traceEntry) {
            traceEntry.endWithLocationStackTrace(JdbcPluginProperties.stackTraceThresholdMillis(),
                    MILLISECONDS);
        }
        @OnThrow
        public static void onThrow(@BindThrowable Throwable t,
                @BindTraveler TraceEntry traceEntry) {
            traceEntry.endWithError(t);
        }
    }

    private static <T extends Connection & ConnectionMixin> String getDest(T connection) {
        String dest = connection.glowroot$getDest();
        if (dest != null) {
            return dest;
        }
        dest = JdbcUrlToDest.getDest(getUrl(connection));
        connection.glowroot$setDest(dest);
        return dest;
    }

    private static String getUrl(Connection connection) {
        try {
            DatabaseMetaData metaData = connection.glowroot$getMetaData();
            if (metaData == null) {
                return "jdbc:";
            }
            String url = metaData.getURL();
            if (url == null) {
                return "jdbc:";
            }
            return url;
        } catch (Exception e) {
            // getMetaData and getURL can throw SQLException
            if (loggedExceptionInGetUrl) {
                logger.debug(e.getMessage(), e);
            } else {
                logger.warn(e.getMessage(), e);
                loggedExceptionInGetUrl = true;
            }
            return "jdbc:";
        }
    }
}
