/*
 * Copyright 2017-2019 the original author or authors.
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
package org.glowroot.xyzzy.instrumentation.javahttpserver;

import java.util.HashMap;
import java.util.Map;

import org.glowroot.xyzzy.instrumentation.api.Message;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.checker.Nullable;

class HttpHandlerMessageSupplier extends MessageSupplier {

    private final String requestMethod;
    private final String requestUri;
    private final @Nullable String requestQueryString;

    private final Map<String, Object> requestHeaders;
    private @Nullable Map<String, Object> responseHeaders;

    private final @Nullable String requestRemoteAddr;
    private final @Nullable String requestRemoteHost;

    HttpHandlerMessageSupplier(String requestMethod, String requestUri,
            @Nullable String requestQueryString, Map<String, Object> requestHeaders,
            @Nullable String requestRemoteAddr, @Nullable String requestRemoteHost) {
        this.requestMethod = requestMethod;
        this.requestUri = requestUri;
        this.requestQueryString = requestQueryString;
        this.requestHeaders = requestHeaders;
        this.requestRemoteAddr = requestRemoteAddr;
        this.requestRemoteHost = requestRemoteHost;
    }

    @Override
    public Message get() {
        Map<String, Object> detail = new HashMap<String, Object>();
        detail.put("Request http method", requestMethod);
        if (requestQueryString != null) {
            // including empty query string since that means request ended with ?
            detail.put("Request query string", requestQueryString);
        }
        if (!requestHeaders.isEmpty()) {
            detail.put("Request headers", requestHeaders);
        }
        if (requestRemoteAddr != null) {
            detail.put("Request remote address", requestRemoteAddr);
        }
        if (requestRemoteHost != null) {
            detail.put("Request remote host", requestRemoteHost);
        }
        if (responseHeaders != null && !responseHeaders.isEmpty()) {
            detail.put("Response headers", responseHeaders);
        }
        return Message.create(requestUri, detail);
    }

    void setResponseHeaders(Map<String, Object> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }
}
