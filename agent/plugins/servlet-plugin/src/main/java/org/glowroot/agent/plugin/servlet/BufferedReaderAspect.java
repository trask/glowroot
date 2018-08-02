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
package org.glowroot.agent.plugin.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.CharBuffer;

import org.glowroot.agent.plugin.api.ThreadContext;
import org.glowroot.agent.plugin.api.checker.EnsuresNonNullIf;
import org.glowroot.agent.plugin.api.checker.Nullable;
import org.glowroot.agent.plugin.api.weaving.BindReturn;
import org.glowroot.agent.plugin.api.weaving.IsEnabled;
import org.glowroot.agent.plugin.api.weaving.OnReturn;
import org.glowroot.agent.plugin.api.weaving.Pointcut;

public class BufferedReaderAspect {

    @Pointcut(className = "javax.servlet.ServletRequest", methodName = "getReader",
            methodParameterTypes = {".."}, nestingGroup = "servlet-inner-call")
    public static class GetReaderAdvice {
        @IsEnabled
        public static boolean isEnabled() {
            return ServletPluginProperties.captureRequestBodyNumChars() != 0;
        }
        @OnReturn
        public static @Nullable BufferedReader onReturn(@BindReturn @Nullable BufferedReader reader,
                ThreadContext context) {
            if (reader == null) {
                return null;
            }
            ServletMessageSupplier messageSupplier =
                    (ServletMessageSupplier) context.getServletRequestInfo();
            return messageSupplier == null ? reader
                    : new CapturingBufferedReader(reader, messageSupplier);
        }
    }

    private static class CapturingBufferedReader extends BufferedReader {

        private final BufferedReader delegate;
        private final ServletMessageSupplier messageSupplier;

        private CapturingBufferedReader(BufferedReader delegate,
                ServletMessageSupplier messageSupplier) {
            super(delegate, 1);
            this.delegate = delegate;
            this.messageSupplier = messageSupplier;
        }

        @Override
        public int read(CharBuffer cbuf) throws IOException {
            int startingPosition = cbuf.position();
            int numRead = delegate.read(cbuf);
            messageSupplier.appendRequestBodyText(cbuf, startingPosition, numRead);
            return numRead;
        }

        @Override
        public @Nullable String readLine() throws IOException {
            String line = delegate.readLine();
            if (line != null) {
                messageSupplier.appendRequestBodyText(line);
                messageSupplier.appendRequestBodyText('\n');
            }
            return line;
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            int numRead = delegate.read(cbuf, off, len);
            if (numRead != -1 && cbuf != null) {
                messageSupplier.appendRequestBodyText(cbuf, off, numRead);
            }
            return numRead;
        }

        @Override
        public int read(char[] cbuf) throws IOException {
            int numRead = delegate.read(cbuf);
            if (numRead != -1 && cbuf != null) {
                messageSupplier.appendRequestBodyText(cbuf, 0, numRead);
            }
            return numRead;
        }

        @Override
        public int read() throws IOException {
            int c = delegate.read();
            if (c != -1) {
                messageSupplier.appendRequestBodyText((char) c);
            }
            return c;
        }

        @Override
        public long skip(long n) throws IOException {
            return delegate.skip(n);
        }

        @Override
        @EnsuresNonNullIf(expression = "readLine()", result = true)
        public boolean ready() throws IOException {
            return delegate.ready();
        }

        @Override
        public boolean markSupported() {
            return delegate.markSupported();
        }

        @Override
        public void mark(int readAheadLimit) throws IOException {
            delegate.mark(readAheadLimit);
        }

        @Override
        public void reset() throws IOException {
            delegate.reset();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }

        // don't need to override Java 8 lines() since its implementation delegates to readLine()
    }
}
