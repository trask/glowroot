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
package org.glowroot.xyzzy.instrumentation.mongodb;

import org.glowroot.xyzzy.instrumentation.api.QueryEntry;
import org.glowroot.xyzzy.instrumentation.api.Timer;
import org.glowroot.xyzzy.instrumentation.api.checker.Nullable;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindReceiver;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindReturn;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindTraveler;
import org.glowroot.xyzzy.instrumentation.api.weaving.Mixin;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnAfter;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnBefore;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnReturn;
import org.glowroot.xyzzy.instrumentation.api.weaving.Pointcut;

public class MongoCursorInstrumentation {

    // the field and method names are verbose since they will be mixed in to existing classes
    @Mixin({"com.mongodb.client.MongoCursor"})
    public static class MongoCursorImpl implements MongoCursorMixin {

        // does not need to be volatile, app/framework must provide visibility of MongoIterables if
        // used across threads and this can piggyback
        private transient @Nullable QueryEntry glowroot$queryEntry;

        @Override
        public @Nullable QueryEntry glowroot$getQueryEntry() {
            return glowroot$queryEntry;
        }

        @Override
        public void glowroot$setQueryEntry(@Nullable QueryEntry queryEntry) {
            glowroot$queryEntry = queryEntry;
        }
    }

    // the method names are verbose since they will be mixed in to existing classes
    public interface MongoCursorMixin {

        @Nullable
        QueryEntry glowroot$getQueryEntry();

        void glowroot$setQueryEntry(@Nullable QueryEntry queryEntry);
    }

    @Pointcut(className = "com.mongodb.client.MongoCursor", methodName = "next|tryNext",
            methodParameterTypes = {}, nestingGroup = "mongodb")
    public static class FirstAdvice {

        @OnBefore
        public static @Nullable Timer onBefore(@BindReceiver MongoCursorMixin mongoCursor) {
            QueryEntry queryEntry = mongoCursor.glowroot$getQueryEntry();
            return queryEntry == null ? null : queryEntry.extend();
        }

        @OnReturn
        public static void onReturn(@BindReturn @Nullable Object document,
                @BindReceiver MongoCursorMixin mongoIterable) {
            QueryEntry queryEntry = mongoIterable.glowroot$getQueryEntry();
            if (queryEntry == null) {
                return;
            }
            if (document != null) {
                queryEntry.incrementCurrRow();
            } else {
                queryEntry.rowNavigationAttempted();
            }
        }

        @OnAfter
        public static void onAfter(@BindTraveler @Nullable Timer timer) {
            if (timer != null) {
                timer.stop();
            }
        }
    }

    @Pointcut(className = "com.mongodb.client.MongoCursor", methodName = "hasNext",
            methodParameterTypes = {}, nestingGroup = "mongodb")
    public static class IsExhaustedAdvice {

        @OnBefore
        public static @Nullable Timer onBefore(@BindReceiver MongoCursorMixin mongoCursor) {
            QueryEntry queryEntry = mongoCursor.glowroot$getQueryEntry();
            return queryEntry == null ? null : queryEntry.extend();
        }

        @OnReturn
        public static void onReturn(@BindReceiver MongoCursorMixin mongoCursor) {
            QueryEntry queryEntry = mongoCursor.glowroot$getQueryEntry();
            if (queryEntry == null) {
                // tracing must be disabled (e.g. exceeded trace entry limit)
                return;
            }
            queryEntry.rowNavigationAttempted();
        }

        @OnAfter
        public static void onAfter(@BindTraveler @Nullable Timer timer) {
            if (timer != null) {
                timer.stop();
            }
        }
    }
}
