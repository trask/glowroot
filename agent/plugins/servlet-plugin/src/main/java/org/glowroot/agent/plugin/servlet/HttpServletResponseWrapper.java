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
package org.glowroot.agent.plugin.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.glowroot.agent.plugin.api.ThreadContext;
import org.glowroot.agent.plugin.api.checker.Nullable;
import org.glowroot.agent.plugin.servlet._.EumPrintWriter;
import org.glowroot.agent.plugin.servlet._.EumWeaselPrintWriter;

public class HttpServletResponseWrapper extends javax.servlet.http.HttpServletResponseWrapper {

    private final String contextPath;
    private final ThreadContext context;

    private @Nullable EumPrintWriter eumWriter;

    public HttpServletResponseWrapper(HttpServletRequest req, HttpServletResponse delegate,
            ThreadContext context) {
        super(delegate);
        String reqContextPath = req.getContextPath();
        contextPath = reqContextPath == null ? "" : reqContextPath;
        this.context = context;
    }

    @Override
    @SuppressWarnings("override.return.invalid") // only returns null if super returns null
    public @Nullable PrintWriter getWriter() throws IOException {
        if (eumWriter == null) {
            PrintWriter writer = super.getWriter();
            if (writer == null) {
                return null;
            }
            eumWriter = new EumWeaselPrintWriter(writer, contextPath, context);
        }
        return eumWriter;
    }
}
