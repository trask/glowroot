/*
 * Copyright 2017-2019 the original author or authors.
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

import java.util.List;

import org.glowroot.agent.api.Instrumentation;
import org.glowroot.agent.api.Instrumentation.AlreadyInTransactionBehavior;
import org.glowroot.common2.repo.GaugeValueRepository;
import org.glowroot.wire.api.model.CollectorServiceOuterClass.GaugeValueMessage.GaugeValue;

public interface GaugeValueDao extends GaugeValueRepository {

    void store(String agentId, List<GaugeValue> gaugeValues) throws Exception;

    @Instrumentation.Transaction(transactionType = "Background", transactionName = "Rollup gauges",
            traceHeadline = "Rollup gauges: {{0}}", timer = "rollup gauges",
            alreadyInTransactionBehavior = AlreadyInTransactionBehavior.CAPTURE_NEW_TRANSACTION)
    void rollup(String agentRollupId) throws Exception;

    void truncateAll() throws Exception;
}
