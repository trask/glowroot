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
package org.glowroot.xyzzy.engine.bytecode.api;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Wrapper that implements optimized {@link ThreadLocal} access pattern ideal for heavily used
 * ThreadLocals.
 * 
 * It is faster to use a mutable holder object and always perform ThreadLocal.get() and never use
 * ThreadLocal.set(), because the value is more likely to be found in the ThreadLocalMap direct hash
 * slot and avoid the slow path ThreadLocalMap.getEntryAfterMiss().
 * 
 * Important: this thread local will live in ThreadLocalMap forever, so use with care.
 */
// NOTE this is same as org.glowroot.xyzzy.instrumentation.api.util.FastThreadLocal, but not genericized
// in order to help with stack frame maps
public class ThreadContextThreadLocal {

    @SuppressWarnings("nullness:type.argument.type.incompatible")
    private final ThreadLocal<Holder> threadLocal = new ThreadLocal<Holder>() {
        @Override
        protected Holder initialValue() {
            return new Holder();
        }
    };

    public @Nullable ThreadContextPlus get() {
        return threadLocal.get().value;
    }

    public void set(@Nullable ThreadContextPlus value) {
        threadLocal.get().value = value;
    }

    public Holder getHolder() {
        return threadLocal.get();
    }

    public static class Holder {

        private @Nullable ThreadContextPlus value;

        private Holder() {}

        public @Nullable ThreadContextPlus get() {
            return value;
        }

        public void set(@Nullable ThreadContextPlus value) {
            this.value = value;
        }
    }
}
