/*
 * Copyright 2018 the original author or authors.
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
package org.glowroot.agent.plugin.executor;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.glowroot.agent.bytecode.api.HasThreadContextHolder;
import org.glowroot.agent.bytecode.api.ThreadContextHolder;
import org.glowroot.agent.plugin.api.weaving.Mixin;
import org.glowroot.agent.plugin.api.weaving.MixinInit;

import static com.google.common.base.Preconditions.checkNotNull;

public class ThreadAspect {

    // the field and method names are verbose since they will be mixed in to existing classes
    @Mixin("java.lang.Thread")
    public abstract static class HasThreadContextHolderImpl implements HasThreadContextHolder {

        private @Nullable ThreadContextHolder threadContextHolder;

        @MixinInit
        private void init() {
            threadContextHolder = new ThreadContextHolder();
        }

        @Override
        public ThreadContextHolder glowroot$getThreadContextHolder() {
            return checkNotNull(threadContextHolder);
        }
    }
}
