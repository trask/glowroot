/*
 * Copyright 2016-2019 the original author or authors.
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
package org.glowroot.xyzzy.instrumentation.logback;

import java.lang.reflect.Method;

import org.glowroot.xyzzy.instrumentation.api.ClassInfo;
import org.glowroot.xyzzy.instrumentation.api.Logger;
import org.glowroot.xyzzy.instrumentation.api.checker.Nullable;
import org.glowroot.xyzzy.instrumentation.api.util.Reflection;

public class LoggingEventInvoker {

    private static final Logger logger = Logger.getLogger(LoggingEventInvoker.class);

    private final @Nullable Method getLoggerNameMethod;

    private final @Nullable Method getFormattedMessageMethod;
    private final @Nullable Method getLevelMethod;

    private final @Nullable Method getThrowableProxyMethod;
    private final @Nullable Method getThrowableMethod;

    private final @Nullable Method toIntMethod;

    public LoggingEventInvoker(ClassInfo classInfo) {
        Class<?> loggerClass = Reflection
                .getClassWithWarnIfNotFound("ch.qos.logback.classic.Logger", classInfo.getLoader());
        getLoggerNameMethod = Reflection.getMethod(loggerClass, "getName");
        Class<?> loggingEventClass = Reflection.getClassWithWarnIfNotFound(
                "ch.qos.logback.classic.spi.LoggingEvent", classInfo.getLoader());
        getFormattedMessageMethod = Reflection.getMethod(loggingEventClass, "getFormattedMessage");
        getLevelMethod = Reflection.getMethod(loggingEventClass, "getLevel");
        if (loggingEventClass == null) {
            getThrowableProxyMethod = null;
            getThrowableMethod = null;
        } else {
            Method localGetThrowableProxyMethod = null;
            Method localGetThrowableMethod = null;
            try {
                localGetThrowableProxyMethod = loggingEventClass.getMethod("getThrowableProxy");
                Class<?> throwableProxyClass = Class.forName(
                        "ch.qos.logback.classic.spi.ThrowableProxy", false, classInfo.getLoader());
                localGetThrowableMethod = throwableProxyClass.getMethod("getThrowable");
            } catch (Throwable t) {
                logger.debug(t.getMessage(), t);
                try {
                    localGetThrowableProxyMethod =
                            loggingEventClass.getMethod("getThrowableInformation");
                    Class<?> throwableInformationClass =
                            Class.forName("ch.qos.logback.classic.spi.ThrowableInformation", false,
                                    classInfo.getLoader());
                    localGetThrowableMethod =
                            Reflection.getMethod(throwableInformationClass, "getThrowable");
                } catch (Throwable tt) {
                    // log at debug
                    logger.debug(tt.getMessage(), tt);
                    // log original at warn
                    logger.warn(t.getMessage(), t);
                }
            }
            getThrowableProxyMethod = localGetThrowableProxyMethod;
            getThrowableMethod = localGetThrowableMethod;
        }
        Class<?> levelClass = Reflection.getClassWithWarnIfNotFound("ch.qos.logback.classic.Level",
                classInfo.getLoader());
        toIntMethod = Reflection.getMethod(levelClass, "toInt");
    }

    String getFormattedMessage(Object loggingEvent) {
        return Reflection.invokeWithDefault(getFormattedMessageMethod, loggingEvent, "");
    }

    int getLevel(Object loggingEvent) {
        Object level = Reflection.invoke(getLevelMethod, loggingEvent);
        if (level == null) {
            return 0;
        }
        return Reflection.invokeWithDefault(toIntMethod, level, 0);
    }

    @Nullable
    Throwable getThrowable(Object loggingEvent) {
        Object throwableInformation =
                Reflection.invoke(getThrowableProxyMethod, loggingEvent);
        if (throwableInformation == null) {
            return null;
        }
        return Reflection.</*@Nullable*/ Throwable>invoke(getThrowableMethod, throwableInformation);
    }

    String getLoggerName(Object logger) {
        return Reflection.invokeWithDefault(getLoggerNameMethod, logger, "");
    }
}
