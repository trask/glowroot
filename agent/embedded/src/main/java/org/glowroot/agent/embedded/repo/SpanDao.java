/*
 * Copyright 2018-2019 the original author or authors.
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
package org.glowroot.agent.embedded.repo;

import java.sql.SQLException;
import java.util.List;

import com.google.common.collect.ImmutableList;

import org.glowroot.agent.embedded.util.DataSource;
import org.glowroot.agent.embedded.util.ImmutableColumn;
import org.glowroot.agent.embedded.util.ImmutableIndex;
import org.glowroot.agent.embedded.util.Schemas.Column;
import org.glowroot.agent.embedded.util.Schemas.ColumnType;
import org.glowroot.agent.embedded.util.Schemas.Index;
import org.glowroot.common.util.Clock;
import org.glowroot.common2.repo.util.RollupLevelService;
import org.glowroot.wire.api.model.CollectorServiceOuterClass.SpanMessage.Span;

import static java.util.concurrent.TimeUnit.MINUTES;

public class SpanDao {

    private static final ImmutableList<Column> distributedTraceColumns = ImmutableList.<Column>of(
            ImmutableColumn.of("capture_time", ColumnType.BIGINT),
            ImmutableColumn.of("trace_id", ColumnType.VARBINARY));

    private static final ImmutableList<Column> spanColumns = ImmutableList.<Column>of(
            ImmutableColumn.of("trace_id", ColumnType.VARCHAR),
            ImmutableColumn.of("span_id", ColumnType.VARCHAR), // empty for eum span
            ImmutableColumn.of("span", ColumnType.VARBINARY));

    private static final ImmutableList<Index> distributedTraceIndexes = ImmutableList.<Index>of(
            ImmutableIndex.of("distributed_trace_idx", ImmutableList.of("capture_time")));

    private static final ImmutableList<Index> spanIndexes = ImmutableList.<Index>of(
            ImmutableIndex.of("distributed_trace_span_idx", ImmutableList.of("trace_id")));

    private final DataSource dataSource;
    private final Clock clock;

    private final Object rollupLock = new Object();

    public SpanDao(DataSource dataSource, Clock clock) throws SQLException {
        this.dataSource = dataSource;
        this.clock = clock;
        dataSource.syncTable("distributed_trace", distributedTraceColumns);
        dataSource.syncIndexes("distributed_trace", distributedTraceIndexes);
        dataSource.syncTable("distributed_trace_span", spanColumns);
        dataSource.syncIndexes("distributed_trace_span", spanIndexes);
    }

    public void store(List<Span> spans) throws SQLException {
        // lock is needed so that new (but old) records don't sneak in during the delete below
        synchronized (rollupLock) {
            for (Span span : spans) {
                dataSource.update(
                        "insert into distributed_trace_span (trace_id, span_id, span) values"
                                + " (?, ?, ?)",
                        span.getTraceId(), span.getSpanId(), span.toByteArray());
                if (span.getParentSpanId().isEmpty()) {
                    dataSource.update("merge into distributed_trace (trace_id, capture_time) values"
                            + " (?, ?)", span.getTraceId(), span.getCaptureTime());
                }
            }

            long safeCurrentTime = clock.currentTimeMillis() - 1;
            long safeRollupTime =
                    RollupLevelService.getSafeRollupTime(safeCurrentTime, MINUTES.toMillis(1));

            // FIXME select * from distributed_trace where capture_time <= safeRollupTime
            // and rollup into AggregateDao and TraceDao

            dataSource.deleteBefore("distributed_trace", safeRollupTime);
            dataSource.deleteBefore("distributed_trace_span", safeRollupTime);
        }
    }
}
