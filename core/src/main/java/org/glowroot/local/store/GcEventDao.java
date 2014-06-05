/*
 * Copyright 2015 the original author or authors.
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
package org.glowroot.local.store;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.glowroot.collector.GcEventRepository;
import org.glowroot.jvm.GcEvent;
import org.glowroot.jvm.GcEvent.GcEventMemoryPool;
import org.glowroot.jvm.ImmutableGcEvent;
import org.glowroot.local.store.DataSource.RowMapper;
import org.glowroot.local.store.Schemas.Column;

import static com.google.common.base.Preconditions.checkNotNull;

public class GcEventDao implements GcEventRepository {

    private static final Logger logger = LoggerFactory.getLogger(GcEventDao.class);

    private static final ImmutableList<Column> gcEventColumns = ImmutableList.<Column>of(
            ImmutableColumn.of("id", Types.BIGINT),
            ImmutableColumn.of("action", Types.VARCHAR),
            ImmutableColumn.of("cause", Types.VARCHAR),
            ImmutableColumn.of("collector_name", Types.VARCHAR),
            ImmutableColumn.of("collector_count", Types.BIGINT),
            ImmutableColumn.of("start_time", Types.BIGINT),
            ImmutableColumn.of("end_time", Types.BIGINT),
            ImmutableColumn.of("duration", Types.BIGINT), // milliseconds
            ImmutableColumn.of("memory_pools", Types.VARCHAR)); // json data

    // private static final ImmutableList<Column> gcEventMemoryPoolColumns = ImmutableList.of(
    // new Column("event_id", Types.VARCHAR),
    // new Column("memory_pool_name", Types.VARCHAR),
    // new Column("init_before_gc", Types.BIGINT),
    // new Column("used_before_gc", Types.BIGINT),
    // new Column("committed_before_gc", Types.BIGINT),
    // new Column("max_before_gc", Types.BIGINT),
    // new Column("init_after_gc", Types.BIGINT),
    // new Column("used_after_gc", Types.BIGINT),
    // new Column("committed_after_gc", Types.BIGINT),
    // new Column("max_after_gc", Types.BIGINT));

    private final DataSource dataSource;

    GcEventDao(DataSource dataSource) throws SQLException {
        this.dataSource = dataSource;
        dataSource.syncTable("gc_event", gcEventColumns);
    }

    @Override
    public void store(GcEvent gcEvent) {
        try {
            String memoryPoolJson = mapper.writeValueAsString(gcEvent.memoryPools());
            dataSource.update("insert into gc_event (action, cause, collector_name,"
                    + " collector_count, start_time, end_time, duration, memory_pools) values"
                    + " (?, ?, ?, ?, ?, ?, ?, ?)", gcEvent.action(), gcEvent.cause(),
                    gcEvent.collectorName(), gcEvent.collectorCount(),
                    gcEvent.startTime(), gcEvent.endTime(), gcEvent.duration(),
                    memoryPoolJson);
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage(), e);
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void deleteAll() {
        try {
            dataSource.execute("truncate table gc_event");
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
    }

    // TODO delete 100 at a time similar to SnapshotDao.deleteBefore()
    void deleteBefore(long time) {
        try {
            dataSource.update("delete from gc_event where end_time < ?", time);
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static class GcEventRowMapper implements RowMapper<GcEvent> {
        @Override
        public GcEvent mapRow(ResultSet resultSet) throws SQLException {
            String action = checkNotNull(resultSet.getString(1));
            String cause = checkNotNull(resultSet.getString(2));
            String collectorName = checkNotNull(resultSet.getString(3));
            long collectorCount = resultSet.getLong(4);
            long startTime = resultSet.getLong(5);
            long endTime = resultSet.getLong(6);
            long duration = resultSet.getLong(7);
            String memoryPoolsJson = resultSet.getString(8);
            if (memoryPoolsJson == null) {
                // TODO provide better fallback
                throw new SQLException("Found null memory_pools in gc_event");
            }
            List<GcEventMemoryPool> memoryPools;
            try {
                memoryPools = mapper.readValue(memoryPoolsJson,
                        new TypeReference<List<GcEventMemoryPool>>() {});
            } catch (JsonParseException e) {
                throw new SQLException(e);
            } catch (JsonMappingException e) {
                throw new SQLException(e);
            } catch (IOException e) {
                throw new SQLException(e);
            }
            return ImmutableGcEvent.builder()
                    .action(action)
                    .cause(cause)
                    .collectorName(collectorName)
                    .collectorCount(collectorCount)
                    .startTime(startTime)
                    .endTime(endTime)
                    .duration(duration)
                    .addAllMemoryPools(memoryPools)
                    .build();
        }
    }
}
