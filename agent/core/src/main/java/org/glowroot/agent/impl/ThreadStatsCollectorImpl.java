/*
 * Copyright 2016-2018 the original author or authors.
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
package org.glowroot.agent.impl;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.glowroot.agent.impl.Transaction.ThreadStatsCollector;
import org.glowroot.agent.model.ThreadStats;
import org.glowroot.common.util.NotAvailableAware;
import org.glowroot.wire.api.model.Proto.OptionalInt64;
import org.glowroot.wire.api.model.TraceOuterClass.Trace;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

class ThreadStatsCollectorImpl implements ThreadStatsCollector {

    private static final Trace.ThreadStats NA_THREAD_STATS = Trace.ThreadStats.newBuilder()
            .setTotalCpuNanos(toProtoInt(NotAvailableAware.NA))
            .setTotalBlockedNanos(toProtoInt(NotAvailableAware.NA))
            .setTotalWaitedNanos(toProtoInt(NotAvailableAware.NA))
            .setTotalAllocatedBytes(toProtoInt(NotAvailableAware.NA))
            .build();

    private long totalCpuNanos;
    private long totalBlockedMillis;
    private long totalWaitedMillis;
    private long totalAllocatedBytes;

    private boolean empty = true;
    private boolean na;

    @Override
    public void mergeThreadStats(@Nullable ThreadStats threadStats) {
        if (threadStats == null) {
            na = true;
        } else if (!na) {
            totalCpuNanos = NotAvailableAware.add(totalCpuNanos, threadStats.getTotalCpuNanos());
            totalBlockedMillis =
                    NotAvailableAware.add(totalBlockedMillis, threadStats.getTotalBlockedMillis());
            totalWaitedMillis =
                    NotAvailableAware.add(totalWaitedMillis, threadStats.getTotalWaitedMillis());
            totalAllocatedBytes = NotAvailableAware.add(totalAllocatedBytes,
                    threadStats.getTotalAllocatedBytes());
        }
        empty = false;
    }

    @Nullable
    ThreadStats getMergedThreadStats() {
        if (empty) {
            return null;
        } else if (na) {
            return ThreadStats.NA;
        } else {
            return new ThreadStats(totalCpuNanos, totalBlockedMillis, totalWaitedMillis,
                    totalAllocatedBytes);
        }
    }

    long getTotalCpuNanos() {
        return totalCpuNanos;
    }

    public Trace. /*@Nullable*/ ThreadStats toProto() {
        if (empty) {
            return null;
        } else if (na) {
            return NA_THREAD_STATS;
        } else {
            Trace.ThreadStats.Builder builder = Trace.ThreadStats.newBuilder();
            if (!NotAvailableAware.isNA(totalCpuNanos)) {
                builder.setTotalCpuNanos(toProtoInt(totalCpuNanos));
            }
            if (!NotAvailableAware.isNA(totalBlockedMillis)) {
                builder.setTotalBlockedNanos(toProtoInt(MILLISECONDS.toNanos(totalBlockedMillis)));
            }
            if (!NotAvailableAware.isNA(totalWaitedMillis)) {
                builder.setTotalWaitedNanos(toProtoInt(MILLISECONDS.toNanos(totalWaitedMillis)));
            }
            if (!NotAvailableAware.isNA(totalAllocatedBytes)) {
                builder.setTotalAllocatedBytes(toProtoInt(totalAllocatedBytes));
            }
            return builder.build();
        }
    }

    private static OptionalInt64 toProtoInt(long value) {
        return OptionalInt64.newBuilder().setValue(value).build();
    }
}
