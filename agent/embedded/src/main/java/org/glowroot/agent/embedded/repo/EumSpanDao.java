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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import org.glowroot.agent.embedded.util.DataSource;
import org.glowroot.agent.embedded.util.ImmutableColumn;
import org.glowroot.agent.embedded.util.ImmutableIndex;
import org.glowroot.agent.embedded.util.Schemas.Column;
import org.glowroot.agent.embedded.util.Schemas.ColumnType;
import org.glowroot.agent.embedded.util.Schemas.Index;
import org.glowroot.wire.api.model.CollectorServiceOuterClass.EumClientSpanMessage.EumClientSpan;
import org.glowroot.wire.api.model.CollectorServiceOuterClass.EumServerSpanMessage.EumServerSpan;

public class EumSpanDao {

    private static final ImmutableList<Column> eumServerSpanColumns = ImmutableList.<Column>of(
            ImmutableColumn.of("trace_id", ColumnType.VARCHAR),
            ImmutableColumn.of("span_id", ColumnType.VARCHAR),
            ImmutableColumn.of("capture_time", ColumnType.VARCHAR),
            ImmutableColumn.of("data", ColumnType.VARBINARY));

    private static final ImmutableList<Column> eumClientSpanColumns = ImmutableList.<Column>of(
            ImmutableColumn.of("trace_id", ColumnType.VARCHAR),
            ImmutableColumn.of("span_id", ColumnType.VARCHAR),
            ImmutableColumn.of("capture_time", ColumnType.VARCHAR),
            ImmutableColumn.of("data", ColumnType.VARBINARY));

    private static final ImmutableList<Index> eumServerSpanIndexes = ImmutableList.<Index>of(
            ImmutableIndex.of("eum_server_span_idx", ImmutableList.of("trace_id", "span_id")),
            ImmutableIndex.of("eum_server_span_capture_time_idx",
                    ImmutableList.of("capture_time")));

    private static final ImmutableList<Index> eumClientSpanIndexes = ImmutableList.<Index>of(
            ImmutableIndex.of("eum_client_span_idx", ImmutableList.of("trace_id", "span_id")));

    private final DataSource dataSource;

    public EumSpanDao(DataSource dataSource) throws SQLException {
        this.dataSource = dataSource;
        dataSource.syncTable("eum_server_span", eumServerSpanColumns);
        dataSource.syncIndexes("eum_server_span", eumServerSpanIndexes);
        dataSource.syncTable("eum_client_span", eumClientSpanColumns);
        dataSource.syncIndexes("eum_client_span", eumClientSpanIndexes);
    }

    public void storeEumServerSpans(List<EumServerSpan> eumServerSpans) throws SQLException {
        for (EumServerSpan eumServerSpan : eumServerSpans) {
            dataSource.update(
                    "insert into eum_server_span (trace_id, span_id, data) values (?, ?, ?)",
                    eumServerSpan.getTraceId(), Strings.emptyToNull(eumServerSpan.getSpanId()),
                    eumServerSpan.toByteArray());
        }
    }

    public void storeEumClientSpans(List<EumClientSpan> eumClientSpans) throws SQLException {
        for (EumClientSpan eumClientSpan : eumClientSpans) {
            dataSource.update(
                    "insert into eum_client_span (trace_id, span_id, data) values (?, ?, ?)",
                    eumClientSpan.getTraceId(), Strings.emptyToNull(eumClientSpan.getSpanId()),
                    eumClientSpan.toByteArray());
        }
    }

    public void deleteBefore(long captureTime) throws SQLException {
        dataSource.deleteBefore("eum_server_span", captureTime);
        dataSource.deleteBefore("eum_client_span", captureTime);
    }
}
