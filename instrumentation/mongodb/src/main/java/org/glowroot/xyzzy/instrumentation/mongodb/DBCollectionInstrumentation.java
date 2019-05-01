/*
 * Copyright 2018-2019 the original author or authors.
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
package org.glowroot.xyzzy.instrumentation.mongodb;

import org.glowroot.xyzzy.instrumentation.api.Agent;
import org.glowroot.xyzzy.instrumentation.api.QueryEntry;
import org.glowroot.xyzzy.instrumentation.api.QueryMessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.ThreadContext;
import org.glowroot.xyzzy.instrumentation.api.TimerName;
import org.glowroot.xyzzy.instrumentation.api.checker.Nullable;
import org.glowroot.xyzzy.instrumentation.api.config.ConfigListener;
import org.glowroot.xyzzy.instrumentation.api.config.ConfigService;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindMethodName;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindReceiver;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindThrowable;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindTraveler;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnBefore;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnReturn;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnThrow;
import org.glowroot.xyzzy.instrumentation.api.weaving.Pointcut;
import org.glowroot.xyzzy.instrumentation.api.weaving.Shim;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

// legacy driver API (still available in latest version, just no longer preferred)
public class DBCollectionInstrumentation {

    private static final String QUERY_TYPE = "MongoDB";

    private static final ConfigService configService = Agent.getConfigService("mongodb");

    // visibility is provided by memoryBarrier in org.glowroot.config.ConfigService
    private static int stackTraceThresholdMillis;

    static {
        configService.registerConfigListener(new ConfigListener() {
            @Override
            public void onChange() {
                Double value = configService.getDoubleProperty("stackTraceThresholdMillis").value();
                stackTraceThresholdMillis = value == null ? Integer.MAX_VALUE : value.intValue();
            }
        });
    }

    @Shim("com.mongodb.DBCollection")
    public interface DBCollection {

        @Nullable
        String getFullName();
    }

    @Pointcut(className = "com.mongodb.DBCollection",
            methodName = "count|getCount|distinct|find*|aggregate|group|mapReduce|insert|remove"
                    + "|save|update*|drop*|create*|ensure*|rename*",
            methodParameterTypes = {".."}, nestingGroup = "mongodb", timerName = "mongodb query")
    public static class DBCollectionAdvice {

        private static final TimerName timerName = Agent.getTimerName(DBCollectionAdvice.class);

        @OnBefore
        public static @Nullable QueryEntry onBefore(ThreadContext context,
                @BindReceiver DBCollection collection, @BindMethodName String methodName) {
            if (methodName.equals("getCount")) {
                methodName = "count";
            }
            String queryText = methodName + " " + collection.getFullName();
            return context.startQueryEntry(QUERY_TYPE, queryText,
                    QueryMessageSupplier.create("mongodb query: "), timerName);
        }

        @OnReturn
        public static void onReturn(@BindTraveler @Nullable QueryEntry queryEntry) {
            if (queryEntry != null) {
                queryEntry.endWithLocationStackTrace(stackTraceThresholdMillis, MILLISECONDS);
            }
        }

        @OnThrow
        public static void onThrow(@BindThrowable Throwable t,
                @BindTraveler @Nullable QueryEntry queryEntry) {
            if (queryEntry != null) {
                queryEntry.endWithError(t);
            }
        }
    }
}
