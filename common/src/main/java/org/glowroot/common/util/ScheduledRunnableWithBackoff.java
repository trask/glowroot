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
package org.glowroot.common.util;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

public abstract class ScheduledRunnableWithBackoff implements Runnable, Cancellable {

    private static final Logger logger =
            LoggerFactory.getLogger(ScheduledRunnableWithBackoff.class);

    private final ScheduledExecutorService scheduledExecutor;
    private final AtomicBoolean cancelled = new AtomicBoolean();

    private volatile long nextDelayMillis;
    private volatile @MonotonicNonNull ScheduledFuture<?> future;

    public ScheduledRunnableWithBackoff(ScheduledExecutorService scheduledExecutor) {
        this.scheduledExecutor = scheduledExecutor;
    }

    public void scheduleWithBackoff(long initialDelay, long period, TimeUnit unit) {
        if (future != null) {
            logger.error("command has already been scheduled: {}", this);
            return;
        }
        nextDelayMillis = unit.toMillis(period);
        future = scheduledExecutor.schedule(this, initialDelay, unit);
    }

    @Override
    public void run() {
        try {
            runInternal();
        } catch (TerminateSubsequentExecutionsException e) {
            // cancel this scheduled runnable
            // log exception at debug level
            logger.debug(e.getMessage(), e);
            return;
        } catch (Throwable t) {
            // log and continue so it will continue to run
            logger.error(t.getMessage(), t);
        }
        long delayMillis = nextDelayMillis;
        nextDelayMillis = Math.min(nextDelayMillis * 2, HOURS.toMillis(1));
        if (nextDelayMillis == MINUTES.toMillis(16)) {
            // special case to make nicer delays
            nextDelayMillis = MINUTES.toMillis(15);
        }
        synchronized (cancelled) {
            if (!cancelled.get()) {
                future = scheduledExecutor.schedule(this, delayMillis, MILLISECONDS);
            }
        }
    }

    @Override
    public void cancel() {
        synchronized (cancelled) {
            cancelled.set(true);
        }
        checkNotNull(future).cancel(false);
    }

    protected abstract void runInternal() throws Exception;

    // marker exception used to terminate subsequent scheduled executions
    // (see ScheduledExecutorService.scheduleWithFixedDelay())
    @SuppressWarnings("serial")
    public static class TerminateSubsequentExecutionsException extends RuntimeException {}
}
