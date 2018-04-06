/*
 * Copyright 2014-2018 the original author or authors.
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

import java.io.File;
import java.util.List;

import com.google.common.base.Ticker;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Test;

import org.glowroot.agent.bytecode.api.ThreadContextHolder;
import org.glowroot.agent.collector.Collector;
import org.glowroot.agent.config.ConfigService;
import org.glowroot.agent.config.ImmutableAdvancedConfig;
import org.glowroot.agent.impl.Transaction.CompletionCallback;
import org.glowroot.agent.model.ImmutableTimerNameImpl;
import org.glowroot.agent.plugin.api.MessageSupplier;
import org.glowroot.agent.util.IterableWithSelfRemovableEntries.SelfRemovableEntry;
import org.glowroot.common.util.Clock;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig;
import org.glowroot.wire.api.model.AggregateOuterClass.Aggregate;
import org.glowroot.wire.api.model.CollectorServiceOuterClass.Environment;
import org.glowroot.wire.api.model.CollectorServiceOuterClass.GaugeValue;
import org.glowroot.wire.api.model.CollectorServiceOuterClass.LogEvent;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AggregatorTest {

    @Test
    public void shouldFlush() throws InterruptedException {
        // given
        MockCollector aggregateCollector = new MockCollector();
        ConfigService configService = mock(ConfigService.class);
        when(configService.getAdvancedConfig())
                .thenReturn(ImmutableAdvancedConfig.builder().build());
        Aggregator aggregator =
                new Aggregator(aggregateCollector, configService, 1000, Clock.systemClock());

        // when
        int count = 0;
        Transaction transaction = buildTransaction();
        aggregator.add(transaction);
        long firstCaptureTime = transaction.getCaptureTime();
        long aggregateCaptureTime = (long) Math.ceil(firstCaptureTime / 1000.0) * 1000;
        while (true) {
            transaction = buildTransaction();
            aggregator.add(transaction);
            long captureTime = transaction.getCaptureTime();
            count++;
            if (captureTime > aggregateCaptureTime) {
                break;
            }
            Thread.sleep(1);
        }

        // then
        // aggregation is done in a separate thread, so give it a little time to complete
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 5000) {
            if (aggregateCollector.getTotalDurationNanos() > 0) {
                break;
            }
        }
        assertThat(aggregateCollector.getTotalDurationNanos()).isEqualTo(count * 123 * 1000000.0);
        aggregator.close();
    }

    private static Transaction buildTransaction() {
        Transaction transaction = new Transaction(Clock.systemClock().currentTimeMillis(),
                0, "W", "A", MessageSupplier.create("M"),
                ImmutableTimerNameImpl.of("T", false), false, 100, 100, 100, 100, null,
                mock(CompletionCallback.class), Ticker.systemTicker(),
                mock(TransactionRegistry.class), mock(TransactionService.class),
                mock(ConfigService.class), mock(UserProfileScheduler.class),
                mock(ThreadContextHolder.class));
        transaction.setTransactionEntry(mock(SelfRemovableEntry.class));
        transaction.end(MILLISECONDS.toNanos(123), false);
        return transaction;
    }

    private static class MockCollector implements Collector {

        // volatile needed for visibility from other thread
        private volatile double totalDurationNanos;

        private double getTotalDurationNanos() {
            return totalDurationNanos;
        }

        @Override
        public void init(File confDir, @Nullable File sharedConfDir, Environment environment,
                AgentConfig agentConfig, AgentConfigUpdater agentConfigUpdater) {}

        @Override
        public void collectAggregates(AggregateReader aggregateReader) throws Exception {
            aggregateReader.accept(new AggregateVisitor() {
                @Override
                public void visitOverallAggregate(String transactionType,
                        List<String> sharedQueryTexts, Aggregate overallAggregate) {
                    // only capture first non-zero value
                    if (totalDurationNanos == 0) {
                        totalDurationNanos = overallAggregate.getTotalDurationNanos();
                    }
                }
                @Override
                public void visitTransactionAggregate(String transactionType,
                        String transactionName, List<String> sharedQueryTexts,
                        Aggregate transactionAggregate) {}
            });
        }

        @Override
        public void collectGaugeValues(List<GaugeValue> gaugeValues) {}

        @Override
        public void collectTrace(TraceReader traceReader) {}

        @Override
        public void log(LogEvent logEvent) {}
    }
}
