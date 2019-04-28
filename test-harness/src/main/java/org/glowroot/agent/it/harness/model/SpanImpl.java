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
package org.glowroot.agent.it.harness.model;

import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class SpanImpl {

    private volatile String message;
    private volatile Map<String, Object> details;
    private volatile @Nullable Span.Error error;
    private volatile @Nullable Long locationStackTraceMillis;

    public void setMessage(String message) {
        this.message = message;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public void setError(Span.Error error) {
        this.error = error;
    }

    public void setLocationStackTraceMillis(Long getLocationStackTraceMillis) {
        this.locationStackTraceMillis = getLocationStackTraceMillis;
    }

    String getMessage() {
        return message;
    }

    Map<String, Object> getDetails() {
        return details;
    }

    @Nullable
    Span.Error getError() {
        return error;
    }

    @Nullable
    Long getLocationStackTraceMillis() {
        return locationStackTraceMillis;
    }

    protected abstract Span toImmutable();
}
