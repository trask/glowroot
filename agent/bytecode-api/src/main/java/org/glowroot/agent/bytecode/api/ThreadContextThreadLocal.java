/*
 * Copyright 2015-2018 the original author or authors.
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
package org.glowroot.agent.bytecode.api;

import org.checkerframework.checker.nullness.qual.Nullable;

public class ThreadContextThreadLocal {

    @SuppressWarnings("nullness:type.argument.type.incompatible")
    private final ThreadLocal<ThreadContextHolder> threadLocal =
            new ThreadLocal<ThreadContextHolder>() {
                @Override
                protected ThreadContextHolder initialValue() {
                    return new ThreadContextHolder();
                }
            };

    public @Nullable ThreadContextPlus get() {
        return threadLocal.get().get();
    }

    public void set(@Nullable ThreadContextPlus value) {
        threadLocal.get().set(value);
    }

    public ThreadContextHolder getHolder() {
        Thread thread = Thread.currentThread();
        if (thread instanceof FastGlowrootThread) {
            return ((FastGlowrootThread) thread).getThreadContextHolder();
        } else {
            return threadLocal.get();
        }
    }
}
