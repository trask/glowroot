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
package org.glowroot.agent.it.harness.agent;

import java.util.Random;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.glowroot.agent.it.harness.model.Trace;
import org.glowroot.xyzzy.engine.bytecode.api.ThreadContextThreadLocal;

import static com.google.common.base.Preconditions.checkNotNull;

// global state used instead of passing these to various classes (e.g. ThreadContextImpl) in order
// to reduce memory footprint
public class Global {

    private static final ThreadContextThreadLocal threadContextThreadLocal =
            new ThreadContextThreadLocal();

    private static volatile @Nullable TraceReporter traceReporter;

    // TODO use ThreadLocalRandom on Java 7
    private static final Random random = new Random();

    public static ThreadContextThreadLocal getThreadContextThreadLocal() {
        return threadContextThreadLocal;
    }

    public static ThreadContextThreadLocal.Holder getThreadContextHolder() {
        return threadContextThreadLocal.getHolder();
    }

    public static void report(Trace trace) throws Exception {
        checkNotNull(traceReporter).send(trace);
    }

    public static void setTraceReporter(TraceReporter traceReporter) {
        Global.traceReporter = traceReporter;
    }

    public static long currentTimeMicros() {
        return System.currentTimeMillis() * 1000;
    }

    // TODO is avoiding 0 necessary? See brave.Tracer.nextId()
    public static long nextId() {
        long nextId = random.nextLong();
        while (nextId == 0L) {
            nextId = random.nextLong();
        }
        return nextId;
    }
}
