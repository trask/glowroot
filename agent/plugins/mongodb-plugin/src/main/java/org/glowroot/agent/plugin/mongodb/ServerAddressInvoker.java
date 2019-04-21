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

import java.lang.reflect.Method;

import org.glowroot.agent.plugin.api.ClassInfo;
import org.glowroot.agent.plugin.api.checker.Nullable;
import org.glowroot.agent.plugin.api.util.Reflection;

public class ServerAddressInvoker {

    private final @Nullable Method getHostMethod;

    public ServerAddressInvoker(ClassInfo classInfo) {
        Class<?> servletResponseClass = Reflection
                .getClassWithWarnIfNotFound("com.mongodb.ServerAddress", classInfo.getLoader());
        Method method = Reflection.getMethod(servletResponseClass, "getHost");
        if (method == null) {
            method = Reflection.getMethod(servletResponseClass, "geHost"); // mongo driver 1.4
        }
        getHostMethod = method;
    }

    String getHost(Object address) {
        return Reflection.invokeWithDefault(getHostMethod, address, "");
    }
}
