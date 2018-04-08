/*
 * Copyright 2018 the original author or authors.
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

import com.google.common.base.Ticker;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.immutables.value.Value;

import org.glowroot.agent.config.ConfigService;
import org.glowroot.agent.util.ThreadAllocatedBytes;

@Value.Immutable
interface Glob {

    int maxTraceEntries();
    int maxQueryAggregates();
    int maxServiceCallAggregates();
    int maxProfileSamples();

    @Nullable
    ThreadAllocatedBytes threadAllocatedBytes(); // non-null means to captureThreadStats
    TransactionRegistry transactionRegistry();
    TransactionService transactionService();
    ConfigService configService();
    UserProfileScheduler userProfileScheduler();

    Ticker ticker();
}
