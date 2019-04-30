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
package org.glowroot.xyzzy.test.harness.agent.spans;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.glowroot.xyzzy.test.harness.ImmutableError;
import org.glowroot.xyzzy.test.harness.ImmutableLocalSpan;

public class ErrorSpanImpl implements SpanImpl {

    private final @Nullable Throwable exception;
    private final @Nullable String message;

    public ErrorSpanImpl(@Nullable String message, @Nullable Throwable exception) {
        this.exception = exception;
        this.message = message;
    }

    @Override
    public ImmutableLocalSpan toImmutable() {
        return ImmutableLocalSpan.builder()
                .totalNanos(0)
                .message("")
                .error(ImmutableError.builder()
                        .message(message)
                        .exception(exception)
                        .build())
                .build();
    }
}
