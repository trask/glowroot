/*
 * Copyright 2013-2019 the original author or authors.
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

import org.glowroot.xyzzy.instrumentation.api.weaving.OptionalReturn;

public class NonVoidReturn implements OptionalReturn {

    private final @Nullable Object returnValue;

    public static OptionalReturn create(@Nullable Object returnValue) {
        return new NonVoidReturn(returnValue);
    }

    private NonVoidReturn(@Nullable Object returnValue) {
        this.returnValue = returnValue;
    }

    @Override
    public @Nullable Object getValue() {
        return returnValue;
    }

    @Override
    public boolean isVoid() {
        return false;
    }
}
