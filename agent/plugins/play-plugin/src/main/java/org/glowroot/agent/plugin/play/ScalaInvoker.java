/*
 * Copyright 2017-2018 the original author or authors.
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
package org.glowroot.agent.plugin.play;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import org.glowroot.agent.plugin.api.Logger;
import org.glowroot.agent.plugin.api.checker.Nullable;
import org.glowroot.agent.plugin.api.util.Reflection;
import org.glowroot.agent.plugin.play.interop.Converter;
import org.glowroot.agent.plugin.play.interop.Function1;

public class ScalaInvoker {

    private static final Logger logger = Logger.getLogger(ScalaInvoker.class);

    private final @Nullable Converter converter;

    private final @Nullable Object directExecutor;

    public ScalaInvoker(Class<?> clazz) {
        converter = Converter.create(clazz.getClassLoader());
        Class<?> executorContextObjectClass = getExecutorContextObjectClass(clazz);
        Field field = Reflection.getDeclaredField(executorContextObjectClass, "MODULE$");
        Object executorContextObject = Reflection.getFieldValue(field, null);
        Method fromExecutorMethod = null;
        if (executorContextObjectClass != null) {
            try {
                fromExecutorMethod =
                        executorContextObjectClass.getMethod("fromExecutor", Executor.class);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        Object directExecutor = null;
        if (fromExecutorMethod != null) {
            try {
                directExecutor =
                        fromExecutorMethod.invoke(executorContextObject, DirectExecutor.INSTANCE);
            } catch (Throwable t) {
                logger.warn(t.getMessage(), t);
            }
        }
        this.directExecutor = directExecutor;
    }

    @Nullable
    Object toScala(Function1<?, ?> f) {
        return converter == null ? null : converter.toScalaFunction1(f);
    }

    @Nullable
    Object getDirectExecutor() {
        return directExecutor;
    }

    private static @Nullable Class<?> getExecutorContextObjectClass(Class<?> clazz) {
        try {
            return Class.forName("scala.concurrent.ExecutionContext$", false,
                    clazz.getClassLoader());
        } catch (ClassNotFoundException e) {
            logger.debug(e.getMessage(), e);
        }
        return null;
    }

    private static class DirectExecutor implements Executor {

        private static final DirectExecutor INSTANCE = new DirectExecutor();

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }
}
