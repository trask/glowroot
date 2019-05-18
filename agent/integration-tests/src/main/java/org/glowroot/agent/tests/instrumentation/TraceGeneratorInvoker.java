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
package org.glowroot.agent.tests.instrumentation;

import java.lang.reflect.Method;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.glowroot.xyzzy.instrumentation.api.ClassInfo;
import org.glowroot.xyzzy.instrumentation.api.util.Reflection;

public class TraceGeneratorInvoker {

    private final @Nullable Method transactionTypeMethod;
    private final @Nullable Method transactionNameMethod;
    private final @Nullable Method headlineMethod;
    private final @Nullable Method attributesMethod;
    private final @Nullable Method errorMethod;

    public TraceGeneratorInvoker(ClassInfo classInfo) {
        Class<?> clazz = Reflection.getClass("org.glowroot.agent.tests.app.TraceGenerator",
                classInfo.getLoader());
        transactionTypeMethod = Reflection.getMethod(clazz, "transactionType");
        transactionNameMethod = Reflection.getMethod(clazz, "transactionName");
        headlineMethod = Reflection.getMethod(clazz, "headline");
        attributesMethod = Reflection.getMethod(clazz, "attributes");
        errorMethod = Reflection.getMethod(clazz, "error");
    }

    String transactionType(Object request) {
        return Reflection.invokeWithDefault(transactionTypeMethod, request, "");
    }

    String transactionName(Object request) {
        return Reflection.invokeWithDefault(transactionNameMethod, request, "");
    }

    String headline(Object request) {
        return Reflection.invokeWithDefault(headlineMethod, request, "");
    }

    // TODO report checker framework issue that occurs without this suppression
    @SuppressWarnings("return.type.incompatible")
    @Nullable
    Map<String, String> attributes(Object request) {
        return Reflection.invoke(attributesMethod, request);
    }

    @Nullable
    String error(Object request) {
        return Reflection.invoke(errorMethod, request);
    }
}
