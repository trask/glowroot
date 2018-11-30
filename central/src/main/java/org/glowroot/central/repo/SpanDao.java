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
package org.glowroot.central.repo;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.utils.UUIDs;
import com.google.common.base.Function;
import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.Futures;
import com.google.protobuf.InvalidProtocolBufferException;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.glowroot.central.repo.Common.NeedsRollup;
import org.glowroot.central.util.ClusterManager;
import org.glowroot.central.util.MoreFutures;
import org.glowroot.central.util.Session;
import org.glowroot.common.util.CaptureTimes;
import org.glowroot.common.util.Clock;
import org.glowroot.common.util.OnlyUsedByTests;
import org.glowroot.common2.repo.ConfigRepository.RollupConfig;
import org.glowroot.wire.api.model.CollectorServiceOuterClass.SpanMessage.Span;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;

public class SpanDao {

    private final Session session;
    private final ConfigRepositoryImpl configRepository;
    private final Executor asyncExecutor;
    private final Clock clock;

    private final PreparedStatement insertSpanPS;
    private final PreparedStatement insertSpanTraceIdPS;

    private final PreparedStatement readDistributedTracePS;
    private final PreparedStatement readSpanPS;

    private final PreparedStatement insertNeedsRollup;
    private final PreparedStatement readNeedsRollup;
    private final PreparedStatement deleteNeedsRollup;

    // needs rollup caches are only to reduce pressure on the needs rollup tables by reducing
    // duplicate entries
    private final ConcurrentMap<Long, Boolean> needsRollupCache;

    public SpanDao(Session session, ConfigRepositoryImpl configRepository, Executor asyncExecutor,
            ClusterManager clusterManager, Clock clock) throws Exception {
        this.session = session;
        this.configRepository = configRepository;
        this.asyncExecutor = asyncExecutor;
        this.clock = clock;

        int rollupExpirationHours =
                configRepository.getCentralStorageConfig().traceExpirationHours();

        session.createTableWithTWCS("create table if not exists span (trace_id varchar, span_id"
                + " varchar, span blob, primary key (trace_id, span_id)", rollupExpirationHours);

        session.createTableWithTWCS("create table if not exists span_trace_id (agent_id,"
                + " capture_time, trace_id varchar, primary key (agent_id, capture_time, trace_id)",
                rollupExpirationHours);

        insertSpanTraceIdPS = session.prepare("insert into distributed_trace (capture_time,"
                + " trace_id values (?, ?) using ttl ?");

        insertSpanPS = session.prepare("insert into distributed_trace_span (trace_id, span_id,"
                + " span) values (?, ?, ?) using ttl ?");

        readDistributedTracePS = session.prepare("select trace_id from distributed_trace where"
                + " capture_time > ? and capture_time <= ?");

        readSpanPS = session.prepare("select span from distributed_trace_span where trace_id = ?");

        // since rollup operations are idempotent, any records resurrected after gc_grace_seconds
        // would just create extra work, but not have any other effect
        //
        // not using gc_grace_seconds of 0 since that disables hinted handoff
        // (http://www.uberobert.com/cassandra_gc_grace_disables_hinted_handoff)
        //
        // it seems any value over max_hint_window_in_ms (which defaults to 3 hours) is good
        long needsRollupGcGraceSeconds = HOURS.toSeconds(4);

        session.createTableWithLCS("create table if not exists distributed_trace_needs_rollup"
                + " (capture_time timestamp, uniqueness timeuuid, primary key (capture_time,"
                + " uniqueness)) with gc_grace_seconds = " + needsRollupGcGraceSeconds, true);
        // TTL is used to prevent non-idempotent rolling up of partially expired spans
        // (e.g. "needs rollup" record resurrecting due to small gc_grace_seconds)
        insertNeedsRollup = session.prepare("insert into distributed_trace_needs_rollup"
                + " (capture_time, uniqueness) values (?, ?) using TTL ?");
        readNeedsRollup = session.prepare("select capture_time, uniqueness from"
                + " distributed_trace_needs_rollup");
        deleteNeedsRollup = session.prepare("delete from distributed_trace_needs_rollup where"
                + " capture_time = ? and uniqueness = ?");

        needsRollupCache =
                clusterManager.createReplicatedMap("distributedTraceNeedsRollupCache", 5, MINUTES);
    }

    public void insertSpans(List<Span> spans) throws Exception {
        List<Future<?>> futures = new ArrayList<>();
        for (Span span : spans) {
            BoundStatement boundStatement = insertSpanPS.bind();
            int i = 0;
            boundStatement.setString(i++, span.getTraceId());
            boundStatement.setString(i++, span.getSpanId());
            boundStatement.setBytes(i++, ByteBuffer.wrap(span.toByteArray()));
            futures.add(session.writeAsync(boundStatement));

            if (span.getParentSpanId().isEmpty()) {
                boundStatement = insertSpanTraceIdPS.bind();
                boundStatement.setString(0, span.getTraceId());
                futures.add(session.writeAsync(boundStatement));
            }
        }
        MoreFutures.waitForAll(futures);

        // insert into distributed_trace_needs_rollup
        int ttl = getTTL();
        Map<Long, Boolean> updatesForNeedsRollupCache = new HashMap<>();
        Set<Long> rollupCaptureTimes = getRollupCaptureTimes(spans);
        for (long rollupCaptureTime : rollupCaptureTimes) {
            if (needsRollupCache.containsKey(rollupCaptureTime)) {
                // capture current time after getting data from cache to prevent race condition with
                // reading the data in Common.getNeedsRollupList()
                if (!Common.isOldEnoughToRollup(rollupCaptureTime, clock.currentTimeMillis(),
                        configRepository.getRollupConfigs().get(0).intervalMillis())) {
                    // covered by prior inserts that haven't been rolled up yet so no need to
                    // re-insert same data
                    continue;
                }
            } else {
                updatesForNeedsRollupCache.put(rollupCaptureTime, true);
            }
            BoundStatement boundStatement = insertNeedsRollup.bind();
            int adjustedTTL = Common.getAdjustedTTL(ttl, rollupCaptureTime, clock);
            int needsRollupAdjustedTTL = Common.getNeedsRollupAdjustedTTL(adjustedTTL,
                    configRepository.getRollupConfigs());
            int i = 0;
            boundStatement.setTimestamp(i++, new Date(rollupCaptureTime));
            boundStatement.setUUID(i++, UUIDs.timeBased());
            boundStatement.setInt(i++, needsRollupAdjustedTTL);
            futures.add(session.writeAsync(boundStatement));
        }
        MoreFutures.waitForAll(futures);

        // update the cache now that the above inserts were successful
        needsRollupCache.putAll(updatesForNeedsRollupCache);
    }

    public void rollup() throws Exception {
        List<RollupConfig> rollupConfigs = configRepository.getRollupConfigs();
        long rollupIntervalMillis = rollupConfigs.get(0).intervalMillis();
        Collection<NeedsRollup> needsRollupList = Common.getNeedsRollupList(readNeedsRollup.bind(),
                rollupIntervalMillis, session, clock, false);
        for (NeedsRollup needsRollup : needsRollupList) {
            BoundStatement boundStatement = readDistributedTracePS.bind();
            boundStatement.setTimestamp(0,
                    new Date(needsRollup.getCaptureTime() - rollupIntervalMillis));
            boundStatement.setTimestamp(0, new Date(needsRollup.getCaptureTime()));
            ResultSet results = session.read(boundStatement);
            List<Future<List<Span>>> futures = new ArrayList<>();
            for (Row row : results) {
                String traceId = checkNotNull(row.getString(0));
                boundStatement = readSpanPS.bind();
                boundStatement.setString(0, traceId);
                Futures.transform(session.readAsync(boundStatement),
                        new Function<ResultSet, List<Span>>() {
                            @Override
                            public List<Span> apply(@Nullable ResultSet results) {
                                List<Span> spans = new ArrayList<>();
                                for (Row row : results) {
                                    ByteBuffer bytes = checkNotNull(row.getBytes(0));
                                    try {
                                        spans.add(Span.parseFrom(bytes));
                                    } catch (InvalidProtocolBufferException e) {
                                        throw new IllegalStateException(e);
                                    }
                                }
                                return spans;
                            }
                        }, asyncExecutor);
                // TODO merge span into NetworkGraph
            }
            for (Future<List<Span>> future : futures) {

            }
            // TODO merge span into NetworkGraph

            // waitForAll above, then proceed

            List<Future<?>> futures = new ArrayList<>();
            for (UUID uniqueness : needsRollup.getUniquenessKeysForDeletion()) {
                boundStatement = deleteNeedsRollup.bind();
                int i = 0;
                boundStatement.setTimestamp(i++, new Date(needsRollup.getCaptureTime()));
                boundStatement.setUUID(i++, uniqueness);
                futures.add(session.writeAsync(boundStatement));
            }
            MoreFutures.waitForAll(futures);
        }
    }

    private Set<Long> getRollupCaptureTimes(List<Span> spans) {
        Set<Long> rollupCaptureTimes = new HashSet<>();
        long intervalMillis = configRepository.getRollupConfigs().get(0).intervalMillis();
        for (Span span : spans) {
            long captureTime = span.getCaptureTime();
            long rollupCaptureTime = CaptureTimes.getRollup(captureTime, intervalMillis);
            rollupCaptureTimes.add(rollupCaptureTime);
        }
        return rollupCaptureTimes;
    }

    private int getTTL() throws Exception {
        return Ints.saturatedCast(HOURS.toSeconds(
                configRepository.getCentralStorageConfig().rollupExpirationHours().get(0)));
    }

    @OnlyUsedByTests
    public void truncateAll() throws Exception {
        session.updateSchemaWithRetry("truncate distributed_trace");
        session.updateSchemaWithRetry("truncate distributed_trace_span");
        session.updateSchemaWithRetry("truncate distributed_trace_needs_rollup");
    }

    private static class EntireNetworkGraph {

        private final Map<String, NetworkNode> networkNodesByAgentId = new HashMap<>();

    }

    private static class EumNode {

    }

    private static class NetworkNode {

        private final String agentId;
        private final String transactionType;
        private final String transactionName;

        // at most one of upstreamNode and eumNode can be null
        private final @Nullable NetworkNode upstreamNode;
        private final @Nullable EumNode eumNode;

        private double totalDurationNanos;
        private int executionCount;

        private final List<NetworkNode> downstreamNodes = new ArrayList<>();
        private final List<ExternalNode> externalNodes = new ArrayList<>();

        private NetworkNode(String agentId, String transactionType, String transactionName,
                @Nullable NetworkNode upstreamNode) {
            this.agentId = agentId;
            this.transactionType = transactionType;
            this.transactionName = transactionName;
            this.upstreamNode = upstreamNode;
        }
    }

    private static class ExternalNode {

        private final String dest;

        private double totalDurationNanos;
        private int executionCount;

        private ExternalNode(String dest) {
            this.dest = dest;
        }
    }
}
