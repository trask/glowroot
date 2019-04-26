/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, @Nullable Version 2.0 (@Nullable the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, @Nullable software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, @Nullable either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glowroot.xyzzy.engine.logging;

import org.glowroot.xyzzy.instrumentation.api.Logger;
import org.glowroot.xyzzy.instrumentation.api.checker.Nullable;
import org.glowroot.xyzzy.instrumentation.api.internal.LoggerFactory;

public class LoggerFactoryImpl implements LoggerFactory {

    @Override
    public Logger getLogger(Class<?> clazz) {
        return new LoggerImpl(clazz);
    }

    private static class LoggerImpl extends Logger {

        private final org.slf4j.Logger logger;

        private LoggerImpl(Class<?> clazz) {
            this(org.slf4j.LoggerFactory.getLogger(clazz));
        }

        // visible for testing
        LoggerImpl(org.slf4j.Logger logger) {
            this.logger = logger;
        }

        @Override
        public String getName() {
            return logger.getName();
        }

        @Override
        public boolean isTraceEnabled() {
            return logger.isTraceEnabled();
        }

        @Override
        public void trace(@Nullable String msg) {
            logger.trace(msg);
        }

        @Override
        public void trace(@Nullable String format, @Nullable Object arg) {
            logger.trace(format, arg);
        }

        @Override
        public void trace(@Nullable String format, @Nullable Object arg1, @Nullable Object arg2) {
            logger.trace(format, arg1, arg2);
        }

        @Override
        public void trace(@Nullable String format, @Nullable Object... arguments) {
            logger.trace(format, arguments);
        }

        @Override
        public void trace(@Nullable String msg, @Nullable Throwable t) {
            logger.trace(msg, t);
        }

        @Override
        public boolean isDebugEnabled() {
            return logger.isDebugEnabled();
        }

        @Override
        public void debug(@Nullable String msg) {
            logger.debug(msg);
        }

        @Override
        public void debug(@Nullable String format, @Nullable Object arg) {
            logger.debug(format, arg);
        }

        @Override
        public void debug(@Nullable String format, @Nullable Object arg1, @Nullable Object arg2) {
            logger.debug(format, arg1, arg2);
        }

        @Override
        public void debug(@Nullable String format, @Nullable Object... arguments) {
            logger.debug(format, arguments);
        }

        @Override
        public void debug(@Nullable String msg, @Nullable Throwable t) {
            logger.debug(msg, t);
        }

        @Override
        public boolean isInfoEnabled() {
            return logger.isInfoEnabled();
        }

        @Override
        public void info(@Nullable String msg) {
            logger.info(msg);
        }

        @Override
        public void info(@Nullable String format, @Nullable Object arg) {
            logger.info(format, arg);
        }

        @Override
        public void info(@Nullable String format, @Nullable Object arg1, @Nullable Object arg2) {
            logger.info(format, arg1, arg2);
        }

        @Override
        public void info(@Nullable String format, @Nullable Object... arguments) {
            logger.info(format, arguments);
        }

        @Override
        public void info(@Nullable String msg, @Nullable Throwable t) {
            logger.info(msg, t);
        }

        @Override
        public boolean isWarnEnabled() {
            return logger.isWarnEnabled();
        }

        @Override
        public void warn(@Nullable String msg) {
            logger.warn(msg);
        }

        @Override
        public void warn(@Nullable String format, @Nullable Object arg) {
            logger.warn(format, arg);
        }

        @Override
        public void warn(@Nullable String format, @Nullable Object... arguments) {
            logger.warn(format, arguments);
        }

        @Override
        public void warn(@Nullable String format, @Nullable Object arg1, @Nullable Object arg2) {
            logger.warn(format, arg1, arg2);
        }

        @Override
        public void warn(@Nullable String msg, @Nullable Throwable t) {
            logger.warn(msg, t);
        }

        @Override
        public boolean isErrorEnabled() {
            return logger.isErrorEnabled();
        }

        @Override
        public void error(@Nullable String msg) {
            logger.error(msg);
        }

        @Override
        public void error(@Nullable String format, @Nullable Object arg) {
            logger.error(format, arg);
        }

        @Override
        public void error(@Nullable String format, @Nullable Object arg1, @Nullable Object arg2) {
            logger.error(format, arg1, arg2);
        }

        @Override
        public void error(@Nullable String format, @Nullable Object... arguments) {
            logger.error(format, arguments);
        }

        @Override
        public void error(@Nullable String msg, @Nullable Throwable t) {
            logger.error(msg, t);
        }
    }
}
