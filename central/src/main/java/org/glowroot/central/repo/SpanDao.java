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
import java.util.List;
import java.util.concurrent.Future;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;

import org.glowroot.central.util.MoreFutures;
import org.glowroot.central.util.Session;
import org.glowroot.wire.api.model.CollectorServiceOuterClass.SpanMessage.Span;

public class SpanDao {

    private final Session session;

    private final PreparedStatement insertSpanPS;
    private final PreparedStatement readSpanPS;

    public SpanDao(Session session, ConfigRepositoryImpl configRepository) throws Exception {
        this.session = session;

        int rollupExpirationHours =
                configRepository.getCentralStorageConfig().traceExpirationHours();

        session.createTableWithTWCS("create table if not exists span (trace_id varchar, span_id"
                + " varchar, span blob, primary key (trace_id, span_id)", rollupExpirationHours);

        session.createTableWithTWCS("create table if not exists span_trace_id (agent_id,"
                + " capture_time, trace_id varchar, primary key (agent_id, capture_time, trace_id)",
                rollupExpirationHours);

        insertSpanPS = session
                .prepare("insert into span (trace_id, span_id, span) values (?, ?, ?) using ttl ?");

        readSpanPS = session.prepare("select span from span where trace_id = ?");
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
        }
        MoreFutures.waitForAll(futures);
    }

    public void readSpans(String distributedTraceId) {

    }
}
