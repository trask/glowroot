/*
 * Copyright 2017-2019 the original author or authors.
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
package org.glowroot.xyzzy.instrumentation.elasticsearch;

import org.glowroot.xyzzy.instrumentation.api.AsyncQueryEntry;
import org.glowroot.xyzzy.instrumentation.api.ThreadContext;
import org.glowroot.xyzzy.instrumentation.api.Timer;
import org.glowroot.xyzzy.instrumentation.api.checker.NonNull;
import org.glowroot.xyzzy.instrumentation.api.checker.Nullable;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindParameter;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindReceiver;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindTraveler;
import org.glowroot.xyzzy.instrumentation.api.weaving.IsEnabled;
import org.glowroot.xyzzy.instrumentation.api.weaving.Mixin;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnAfter;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnBefore;
import org.glowroot.xyzzy.instrumentation.api.weaving.Pointcut;

public class ActionFutureInstrumentation {

    // the field and method names are verbose since they will be mixed in to existing classes
    @Mixin("org.elasticsearch.action.ActionFuture")
    public static class ActionFutureImpl implements ActionFutureMixin {

        private transient volatile boolean glowroot$completed;
        private transient volatile @Nullable Throwable glowroot$exception;
        private transient volatile @Nullable AsyncQueryEntry glowroot$asyncQueryEntry;

        @Override
        public void glowroot$setCompleted() {
            glowroot$completed = true;
        }

        @Override
        public boolean glowroot$isCompleted() {
            return glowroot$completed;
        }

        @Override
        public void glowroot$setException(Throwable exception) {
            glowroot$exception = exception;
        }

        @Override
        public @Nullable Throwable glowroot$getException() {
            return glowroot$exception;
        }

        @Override
        public @Nullable AsyncQueryEntry glowroot$getAsyncQueryEntry() {
            return glowroot$asyncQueryEntry;
        }

        @Override
        public void glowroot$setAsyncQueryEntry(@Nullable AsyncQueryEntry asyncQueryEntry) {
            glowroot$asyncQueryEntry = asyncQueryEntry;
        }
    }

    // the method names are verbose since they will be mixed in to existing classes
    public interface ActionFutureMixin {

        void glowroot$setCompleted();

        boolean glowroot$isCompleted();

        void glowroot$setException(Throwable t);

        @Nullable
        Throwable glowroot$getException();

        @Nullable
        AsyncQueryEntry glowroot$getAsyncQueryEntry();

        void glowroot$setAsyncQueryEntry(@Nullable AsyncQueryEntry asyncQueryEntry);
    }

    @Pointcut(className = "java.util.concurrent.Future",
            subTypeRestriction = "org.elasticsearch.action.ActionFuture",
            methodName = "get", methodParameterTypes = {".."}, suppressionKey = "wait-on-future")
    public static class FutureGetAdvice {
        @IsEnabled
        public static boolean isEnabled(@BindReceiver ActionFutureMixin actionFuture) {
            return actionFuture.glowroot$getAsyncQueryEntry() != null;
        }
        @OnBefore
        public static Timer onBefore(ThreadContext threadContext,
                @BindReceiver ActionFutureMixin actionFuture) {
            @SuppressWarnings("nullness") // just checked above in isEnabled()
            @NonNull
            AsyncQueryEntry asyncQueryEntry = actionFuture.glowroot$getAsyncQueryEntry();
            return asyncQueryEntry.extendSyncTimer(threadContext);
        }
        @OnAfter
        public static void onAfter(@BindTraveler Timer timer) {
            timer.stop();
        }
    }

    @Pointcut(className = "org.elasticsearch.common.util.concurrent.BaseFuture",
            subTypeRestriction = "org.elasticsearch.action.ActionFuture",
            methodName = "setException", methodParameterTypes = {"java.lang.Throwable"})
    public static class FutureSetExceptionAdvice {
        // using @OnBefore instead of @OnReturn to ensure that async trace entry is ended prior to
        // an overall transaction that may be waiting on this future has a chance to end
        @OnBefore
        public static void onBefore(@BindReceiver ActionFutureMixin actionFuture,
                @BindParameter @Nullable Throwable t) {
            if (t == null) {
                return;
            }
            // to prevent race condition, setting completed/exception status before getting async
            // query entry, and the converse is done when setting async query entry
            // ok if end() happens to get called twice
            actionFuture.glowroot$setCompleted();
            actionFuture.glowroot$setException(t);
            AsyncQueryEntry asyncQueryEntry = actionFuture.glowroot$getAsyncQueryEntry();
            if (asyncQueryEntry != null) {
                asyncQueryEntry.endWithError(t);
            }
        }
    }

    @Pointcut(className = "org.elasticsearch.common.util.concurrent.BaseFuture",
            subTypeRestriction = "org.elasticsearch.action.ActionFuture",
            methodName = "set", methodParameterTypes = {"java.lang.Object"})
    public static class FutureSetAdvice {
        // using @OnBefore instead of @OnReturn to ensure that async trace entry is ended prior to
        // an overall transaction that may be waiting on this future has a chance to end
        @OnBefore
        public static void onBefore(@BindReceiver ActionFutureMixin actionFuture) {
            // to prevent race condition, setting completed status before getting async query entry,
            // and the converse is done when setting async query entry
            // ok if end() happens to get called twice
            actionFuture.glowroot$setCompleted();
            AsyncQueryEntry asyncQueryEntry = actionFuture.glowroot$getAsyncQueryEntry();
            if (asyncQueryEntry != null) {
                asyncQueryEntry.end();
            }
        }
    }
}
