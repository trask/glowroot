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
package org.glowroot.xyzzy.instrumentation.httpclient;

import java.net.URI;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.RequestLine;
import org.apache.http.client.methods.HttpUriRequest;

import org.glowroot.xyzzy.instrumentation.api.Agent;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.ThreadContext;
import org.glowroot.xyzzy.instrumentation.api.ThreadContext.Propagator;
import org.glowroot.xyzzy.instrumentation.api.TimerName;
import org.glowroot.xyzzy.instrumentation.api.TraceEntry;
import org.glowroot.xyzzy.instrumentation.api.checker.Nullable;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindParameter;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindThrowable;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindTraveler;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnBefore;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnReturn;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnThrow;
import org.glowroot.xyzzy.instrumentation.api.weaving.Pointcut;
import org.glowroot.xyzzy.instrumentation.httpclient._.Uris;

// see nearly identical copy of this in WiremockApacheHttpClientInstrumentation
public class ApacheHttpClientInstrumentation {

    private static final Propagator<HttpUriRequest> PROPAGATOR = new Propagator<HttpUriRequest>() {
        @Override
        public void propagate(HttpUriRequest request, String name, String value) {
            request.setHeader(name, value);
        }
    };

    @Pointcut(className = "org.apache.http.client.HttpClient", methodName = "execute",
            methodParameterTypes = {"org.apache.http.client.methods.HttpUriRequest", ".."},
            nestingGroup = "http-client", timerName = "http client request")
    public static class ExecuteAdvice {
        private static final TimerName timerName = Agent.getTimerName(ExecuteAdvice.class);
        @OnBefore
        public static @Nullable TraceEntry onBefore(ThreadContext context,
                @BindParameter @Nullable HttpUriRequest request) {
            if (request == null) {
                return null;
            }
            String method = request.getMethod();
            if (method == null) {
                method = "";
            } else {
                method += " ";
            }
            URI uriObj = request.getURI();
            String uri;
            if (uriObj == null) {
                uri = "";
            } else {
                uri = uriObj.toString();
            }
            context.propagateTrace(request, PROPAGATOR);
            return context.startServiceCallEntry("HTTP", method + Uris.stripQueryString(uri),
                    MessageSupplier.create("http client request: {}{}", method, uri),
                    timerName);
        }
        @OnReturn
        public static void onReturn(@BindTraveler @Nullable TraceEntry traceEntry) {
            if (traceEntry != null) {
                traceEntry.end();
            }
        }
        @OnThrow
        public static void onThrow(@BindThrowable Throwable t,
                @BindTraveler @Nullable TraceEntry traceEntry) {
            if (traceEntry != null) {
                traceEntry.endWithError(t);
            }
        }
    }

    @Pointcut(className = "org.apache.http.client.HttpClient", methodName = "execute",
            methodParameterTypes = {"org.apache.http.HttpHost", "org.apache.http.HttpRequest",
                    ".."},
            nestingGroup = "http-client", timerName = "http client request")
    public static class ExecuteWithHostAdvice {
        private static final TimerName timerName = Agent.getTimerName(ExecuteWithHostAdvice.class);
        @OnBefore
        public static @Nullable TraceEntry onBefore(ThreadContext context,
                @BindParameter @Nullable HttpHost hostObj,
                @BindParameter @Nullable HttpRequest request) {
            if (request == null) {
                return null;
            }
            RequestLine requestLine = request.getRequestLine();
            if (requestLine == null) {
                return null;
            }
            String method = requestLine.getMethod();
            if (method == null) {
                method = "";
            } else {
                method += " ";
            }
            String host = hostObj == null ? "" : hostObj.toURI();
            String uri = requestLine.getUri();
            if (uri == null) {
                uri = "";
            }
            return context.startServiceCallEntry("HTTP", method + Uris.stripQueryString(uri),
                    MessageSupplier.create("http client request: {}{}{}", method, host, uri),
                    timerName);
        }
        @OnReturn
        public static void onReturn(@BindTraveler @Nullable TraceEntry traceEntry) {
            if (traceEntry != null) {
                traceEntry.end();
            }
        }
        @OnThrow
        public static void onThrow(@BindThrowable Throwable t,
                @BindTraveler TraceEntry traceEntry) {
            traceEntry.endWithError(t);
        }
    }
}
