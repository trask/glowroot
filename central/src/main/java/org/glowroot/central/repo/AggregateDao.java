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

import org.glowroot.common.util.OnlyUsedByTests;
import org.glowroot.common2.repo.AggregateRepository;
import org.glowroot.wire.api.model.AggregateOuterClass.Aggregate;
import org.glowroot.wire.api.model.AggregateOuterClass.OldAggregatesByType;
import org.glowroot.xyzzy.annotation.api.Instrumentation;
import org.glowroot.xyzzy.annotation.api.Instrumentation.AlreadyInTransactionBehavior;

public interface AggregateDao extends AggregateRepository {

    void store(String agentId, long captureTime, List<OldAggregatesByType> aggregatesByTypeList,
            List<Aggregate.SharedQueryText> initialSharedQueryTexts) throws Exception;

    @Instrumentation.Transaction(transactionType = "Background",
            transactionName = "Rollup aggregates", traceHeadline = "Rollup aggregates: {{0}}",
            timer = "rollup aggregates",
            alreadyInTransactionBehavior = AlreadyInTransactionBehavior.CAPTURE_NEW_TRANSACTION)
    void rollup(String agentRollupId) throws Exception;

    @OnlyUsedByTests
    void truncateAll() throws Exception;
}
