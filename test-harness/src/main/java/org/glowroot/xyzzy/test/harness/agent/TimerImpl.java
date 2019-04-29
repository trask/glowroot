/*
 * Copyright 2019 the original author or authors.
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
package org.glowroot.xyzzy.test.harness.agent;

import org.glowroot.xyzzy.engine.impl.TimerNameImpl;
import org.glowroot.xyzzy.instrumentation.api.Timer;
import org.glowroot.xyzzy.test.harness.ImmutableTimer;

public class TimerImpl implements Timer {

    private final TimerNameImpl timerName;
    private final long startNanoTime;
    private volatile long totalNanos;

    public TimerImpl(TimerNameImpl timerName, long startNanoTime) {
        this.timerName = timerName;
        this.startNanoTime = startNanoTime;
    }

    @Override
    public void stop() {
        stop(System.nanoTime());
    }

    public void stop(long endNanoTime) {
        totalNanos = endNanoTime - startNanoTime;
    }

    public ImmutableTimer toImmutable() {
        return ImmutableTimer.builder()
                .name(timerName.name())
                .extended(false)
                .totalNanos(totalNanos)
                .count(1)
                // childTimers
                .build();
    }
}
