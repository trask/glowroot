/*
 * Copyright 2016-2019 the original author or authors.
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
package org.glowroot.agent.model;

import com.google.common.base.Ticker;

import org.glowroot.agent.util.Tickers;
import org.glowroot.xyzzy.engine.impl.TimerNameImpl;

public class AsyncTimer implements TransactionTimer {

    private static final Ticker ticker = Tickers.getTicker();

    private final TimerNameImpl timerName;

    private volatile long startTick;
    private volatile boolean active;

    private volatile long totalNanos = 0;

    public AsyncTimer(TimerNameImpl timerName, long startTick) {
        this.timerName = timerName;
        this.startTick = startTick;
        active = true;
    }

    public void end(long endTick) {
        totalNanos += endTick - startTick;
        active = false;
    }

    public void extend(long startTick) {
        this.startTick = startTick;
        active = true;
    }

    @Override
    public String getName() {
        return timerName.name();
    }

    @Override
    public boolean isExtended() {
        return false;
    }

    @Override
    public long getTotalNanos() {
        long totalNanos = this.totalNanos;
        if (active) {
            totalNanos += ticker.read() - startTick;
        }
        return totalNanos;
    }

    @Override
    public long getCount() {
        return 1;
    }

    @Override
    public void mergeChildTimersInto(AggregatedTimer timer) {
        // async timers have no child timers
    }

    public boolean active() {
        return active;
    }

    @Override
    public TransactionTimerSnapshot getSnapshot() {
        return ImmutableTransactionTimerSnapshot.builder()
                .totalNanos(getTotalNanos())
                .count(1)
                .active(active)
                .build();
    }
}
