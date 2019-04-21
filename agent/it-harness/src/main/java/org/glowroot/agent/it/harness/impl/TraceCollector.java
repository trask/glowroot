/*
 * Copyright 2015-2019 the original author or authors.
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
package org.glowroot.agent.it.harness.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.immutables.value.Value;

import org.glowroot.wire.api.model.CollectorServiceOuterClass.LogMessage.LogEvent;
import org.glowroot.wire.api.model.TraceOuterClass.Trace;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

class TraceCollector {

    private final List<Trace> traces = Lists.newCopyOnWriteArrayList();

    private final List<ExpectedLogMessage> expectedMessages = Lists.newCopyOnWriteArrayList();
    private final List<LogEvent> unexpectedMessages = Lists.newCopyOnWriteArrayList();

    Trace getCompletedTrace(@Nullable String transactionType, @Nullable String transactionName,
            int timeout, TimeUnit unit) throws InterruptedException {
        if (transactionName != null) {
            checkNotNull(transactionType);
        }
        Stopwatch stopwatch = Stopwatch.createStarted();
        while (stopwatch.elapsed(unit) < timeout) {
            for (Trace trace : traces) {
                if (!trace.getHeader().getPartial()
                        && (transactionType == null
                                || trace.getHeader().getTransactionType().equals(transactionType))
                        && (transactionName == null || trace.getHeader().getTransactionName()
                                .equals(transactionName))) {
                    return trace;
                }
            }
            MILLISECONDS.sleep(10);
        }
        if (transactionName != null) {
            throw new IllegalStateException("No trace was collected for transaction type \""
                    + transactionType + "\" and transaction name \"" + transactionName + "\"");
        } else if (transactionType != null) {
            throw new IllegalStateException(
                    "No trace was collected for transaction type: " + transactionType);
        } else {
            throw new IllegalStateException("No trace was collected");
        }
    }

    Trace getPartialTrace(int timeout, TimeUnit unit) throws InterruptedException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        while (stopwatch.elapsed(unit) < timeout) {
            for (Trace trace : traces) {
                if (trace.getHeader().getPartial()) {
                    return trace;
                }
            }
            MILLISECONDS.sleep(10);
        }
        if (traces.isEmpty()) {
            throw new IllegalStateException("No trace was collected");
        } else {
            throw new IllegalStateException("Trace was collected but is not partial");
        }
    }

    boolean hasTrace() {
        return !traces.isEmpty();
    }

    void clearTrace() {
        traces.clear();
    }

    void addExpectedLogMessage(String loggerName, String partialMessage) {
        expectedMessages.add(ImmutableExpectedLogMessage.of(loggerName, partialMessage));
    }

    public void checkAndResetLogMessages() throws InterruptedException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        while (stopwatch.elapsed(SECONDS) < 10 && !expectedMessages.isEmpty()
                && unexpectedMessages.isEmpty()) {
            MILLISECONDS.sleep(10);
        }
        try {
            if (!unexpectedMessages.isEmpty()) {
                throw new AssertionError("Unexpected messages were logged:\n\n"
                        + Joiner.on("\n").join(unexpectedMessages));
            }
            if (!expectedMessages.isEmpty()) {
                throw new AssertionError("One or more expected messages were not logged");
            }
        } finally {
            expectedMessages.clear();
            unexpectedMessages.clear();
        }
    }

    public void collectTrace(Trace trace) {
        for (int i = 0; i < traces.size(); i++) {
            Trace loopTrace = traces.get(i);
            if (loopTrace.getTraceId().equals(trace.getTraceId())
                    && loopTrace.getSpanId().equals(trace.getSpanId())) {
                if (trace.getHeader().getDurationNanos() >= loopTrace.getHeader()
                        .getDurationNanos()) {
                    traces.set(i, trace);
                }
                return;
            }
        }
        traces.add(trace);
    }

    public void log(LogEvent logEvent) {
        if (isExpected(logEvent)) {
            return;
        }
        if (logEvent.getLoggerName().equals("org.apache.catalina.loader.WebappClassLoaderBase")
                && logEvent.getMessage().matches(
                        "The web application \\[.*\\] appears to have started a thread named"
                                + " \\[.*\\] but has failed to stop it\\. This is very likely to"
                                + " create a memory leak\\.")) {
            return;
        }
        if (logEvent.getLevel() == LogEvent.Level.WARN
                || logEvent.getLevel() == LogEvent.Level.ERROR) {
            unexpectedMessages.add(logEvent);
        }
    }

    private boolean isExpected(LogEvent logEvent) {
        if (expectedMessages.isEmpty()) {
            return false;
        }
        ExpectedLogMessage expectedMessage = expectedMessages.get(0);
        if (expectedMessage.loggerName().equals(logEvent.getLoggerName())
                && logEvent.getMessage().contains(expectedMessage.partialMessage())) {
            expectedMessages.remove(0);
            return true;
        }
        return false;
    }

    @Value.Immutable
    @Value.Style(allParameters = true)
    interface ExpectedLogMessage {
        String loggerName();
        String partialMessage();
    }
}
