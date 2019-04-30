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
package org.glowroot.xyzzy.test.harness;

import java.io.Serializable;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.immutables.value.Value;

@Value.Immutable
public interface IncomingSpan extends Span, Serializable {

    String transactionType();
    String transactionName();
    String user();

    Timer mainThreadRootTimer();
    @Nullable
    Timer auxThreadRootTimer();
    List<Timer> asyncTimers();

    List<Span> childSpans();

    @Value.Immutable
    public interface Timer extends Serializable {
        String name();
        boolean extended();
        long totalNanos();
        long count();
        List<Timer> childTimers();
    }
}
