/*
 * Copyright 2011-2019 the original author or authors.
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

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Strings;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableList;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.glowroot.agent.impl.Transaction.TraceEntryVisitor;
import org.glowroot.agent.model.AsyncTimer;
import org.glowroot.agent.model.DetailMapWriter;
import org.glowroot.agent.model.ErrorMessage;
import org.glowroot.agent.model.QueryData;
import org.glowroot.agent.model.QueryEntryBase;
import org.glowroot.agent.model.SharedQueryTextCollection;
import org.glowroot.agent.util.Tickers;
import org.glowroot.wire.api.model.Proto;
import org.glowroot.wire.api.model.TraceOuterClass.Trace;
import org.glowroot.xyzzy.engine.bytecode.api.BytecodeServiceHolder;
import org.glowroot.xyzzy.engine.bytecode.api.ThreadContextPlus;
import org.glowroot.xyzzy.engine.bytecode.api.ThreadContextThreadLocal;
import org.glowroot.xyzzy.engine.impl.NopTransactionService;
import org.glowroot.xyzzy.instrumentation.api.AsyncQuerySpan;
import org.glowroot.xyzzy.instrumentation.api.Getter;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.QueryMessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.Setter;
import org.glowroot.xyzzy.instrumentation.api.ThreadContext;
import org.glowroot.xyzzy.instrumentation.api.Timer;
import org.glowroot.xyzzy.instrumentation.api.internal.ReadableMessage;

import static com.google.common.base.Preconditions.checkNotNull;

// this supports updating by a single thread and reading by multiple threads
class TraceEntryImpl extends QueryEntryBase implements AsyncQuerySpan, Timer {

    private static final Logger logger = LoggerFactory.getLogger(TraceEntryImpl.class);
    private static final Ticker ticker = Tickers.getTicker();

    private final ThreadContextImpl threadContext;
    private final @Nullable TraceEntryImpl parentTraceEntry;
    private final @Nullable Object messageSupplier;

    // volatile so it can be set from another thread (needed for async trace entries)
    private volatile @Nullable ErrorMessage errorMessage;

    private final long startTick;

    // these fields are not volatile, so depends on memory barrier in Transaction for visibility
    private long revisedStartTick;
    private int selfNestingLevel;
    private long endTick;
    private boolean initialComplete;

    // this is for maintaining linear list of trace entries
    private @Nullable TraceEntryImpl nextTraceEntry;

    // only null for trace entries added using addEntryEntry()
    private final @Nullable TimerImpl syncTimer;
    private final @Nullable AsyncTimer asyncTimer;
    // not volatile, so depends on memory barrier in Transaction for visibility
    private @Nullable ImmutableList<StackTraceElement> locationStackTrace;

    // only used by transaction thread
    private long locationStackTraceThreshold;
    // only used by transaction thread
    private @Nullable TimerImpl extendedTimer;

    static TraceEntryImpl createCompletedErrorEntry(ThreadContextImpl threadContext,
            TraceEntryImpl parentTraceEntry, @Nullable Object messageSupplier,
            @Nullable QueryData queryData, ErrorMessage errorMessage, long startTick,
            long endTick) {
        // timing/etc for queryData have been captured already at this point, so passing
        // queryExecutionCount -1 because that triggers special case to bypass calling start on
        // the queryData in the constructor below
        TraceEntryImpl entry = new TraceEntryImpl(threadContext, parentTraceEntry,
                messageSupplier, queryData, -1, startTick, null, null);
        entry.errorMessage = errorMessage;
        entry.endTick = endTick;
        entry.selfNestingLevel = 0;
        entry.initialComplete = true;
        return entry;
    }

    TraceEntryImpl(ThreadContextImpl threadContext, @Nullable TraceEntryImpl parentTraceEntry,
            @Nullable Object messageSupplier, @Nullable QueryData queryData,
            long queryExecutionCount, long startTick, @Nullable TimerImpl syncTimer,
            @Nullable AsyncTimer asyncTimer) {
        super(queryData, startTick, queryExecutionCount);
        this.threadContext = threadContext;
        this.parentTraceEntry = parentTraceEntry;
        this.messageSupplier = messageSupplier;
        this.startTick = startTick;
        this.syncTimer = syncTimer;
        this.asyncTimer = asyncTimer;
        revisedStartTick = startTick;
        selfNestingLevel = 1;
    }

    @Override
    public @Nullable Object getMessageSupplier() {
        return messageSupplier;
    }

    @Nullable
    ErrorMessage getErrorMessage() {
        return errorMessage;
    }

    @Nullable
    List<StackTraceElement> getLocationStackTrace() {
        return locationStackTrace;
    }

    void accept(int depth, long transactionStartTick, long captureTick,
            TraceEntryVisitor entryVisitor, SharedQueryTextCollection sharedQueryTextCollection) {
        long offsetNanos = startTick - transactionStartTick;
        long durationNanos;
        boolean active;
        if (isCompleted() && Tickers.lessThanOrEqual(endTick, captureTick)) {
            // total time is calculated relative to revised start tick
            durationNanos = endTick - revisedStartTick;
            active = false;
        } else {
            // total time is calculated relative to revised start tick
            // max with zero to prevent negative value which is possible here under race condition
            durationNanos = Math.max(captureTick - revisedStartTick, 0);
            active = true;
        }
        Object messageSupplier = getMessageSupplier();

        Trace.Entry.Builder builder = Trace.Entry.newBuilder()
                .setDepth(depth)
                .setStartOffsetNanos(offsetNanos)
                .setDurationNanos(durationNanos)
                .setActive(active);

        // async root entry always has empty message and empty detail

        if (messageSupplier instanceof MessageSupplier) {
            ReadableMessage readableMessage =
                    (ReadableMessage) ((MessageSupplier) messageSupplier).get();
            builder.setMessage(readableMessage.getText());
            builder.addAllDetailEntry(DetailMapWriter.toProto(readableMessage.getDetail()));
        } else if (messageSupplier instanceof QueryMessageSupplier) {
            String queryText = checkNotNull(getQueryText());
            int sharedQueryTextIndex = sharedQueryTextCollection.getSharedQueryTextIndex(queryText);
            ReadableMessage readableMessage =
                    (ReadableMessage) ((QueryMessageSupplier) messageSupplier).get();
            Trace.QueryEntryMessage.Builder queryMessage = Trace.QueryEntryMessage.newBuilder()
                    .setSharedQueryTextIndex(sharedQueryTextIndex);
            // FIXME xyzzy update
            // .setPrefix(readableMessage.getPrefix());
            // FIXME xyzzy update
            // String rowCountSuffix = getRowCountSuffix();
            // if (rowCountSuffix.isEmpty()) {
            // // optimization to avoid creating new string when concatenating empty string
            // queryMessage.setSuffix(readableMessage.getSuffix());
            // } else {
            // queryMessage.setSuffix(readableMessage.getSuffix() + rowCountSuffix);
            // }
            builder.setQueryEntryMessage(queryMessage);
            builder.addAllDetailEntry(DetailMapWriter.toProto(readableMessage.getDetail()));
        }

        ErrorMessage errorMessage = this.errorMessage;
        if (errorMessage != null) {
            Trace.Error.Builder errorBuilder = builder.getErrorBuilder();
            errorBuilder.setMessage(errorMessage.message());
            Proto.Throwable throwable = errorMessage.throwable();
            if (throwable != null) {
                errorBuilder.setException(throwable);
            }
            errorBuilder.build();
        }
        if (locationStackTrace != null) {
            for (StackTraceElement stackTraceElement : locationStackTrace) {
                builder.addLocationStackTraceElementBuilder()
                        .setClassName(stackTraceElement.getClassName())
                        .setMethodName(Strings.nullToEmpty(stackTraceElement.getMethodName()))
                        .setFileName(Strings.nullToEmpty(stackTraceElement.getFileName()))
                        .setLineNumber(stackTraceElement.getLineNumber())
                        .build();
            }
        }
        entryVisitor.visitEntry(builder.build());
    }

    long getStartTick() {
        return startTick;
    }

    @Override
    public void end() {
        if (initialComplete) {
            // this guards against end*() being called multiple times on async trace entries
            return;
        }
        long endTick = ticker.read();
        endInternal(endTick, null);
    }

    @Override
    public void endWithLocationStackTrace(long threshold, TimeUnit unit) {
        if (threshold < 0) {
            logger.error("endWithLocationStackTrace(): argument 'threshold' must be non-negative");
            end();
            return;
        }
        endWithLocationStackTraceInternal(threshold, unit);
    }

    @Override
    public void endWithError(Throwable t) {
        if (initialComplete) {
            // this guards against end*() being called multiple times on async trace entries
            return;
        }
        endWithErrorInternal(null, t);
    }

    @Override
    public Timer extend() {
        if (selfNestingLevel++ == 0) {
            if (isAsync()) {
                extendAsync();
            } else {
                TimerImpl currentTimer = threadContext.getCurrentTimer();
                if (currentTimer == null) {
                    // thread context has ended, cannot extend sync timer
                    // (this is ok, see https://github.com/glowroot/glowroot/issues/418)
                    selfNestingLevel--;
                    return NopTransactionService.TIMER;
                }
                extendSync(ticker.read(), currentTimer);
            }
        }
        return this;
    }

    @Override
    @Deprecated
    public <R> void propagateToResponse(R response, Setter<R> setter) {}

    @Override
    @Deprecated
    public <R> void extractFromResponse(R response, Getter<R> getter) {}

    @Override
    public void rowNavigationAttempted() {
        // checking ThreadContext complete first because that is non-volatile read and will normally
        // shortcut the condition without checking the volatile completed field in Transaction
        if (!threadContext.isCompleted() || !threadContext.getTransaction().isCompleted()) {
            super.rowNavigationAttempted();
        }
    }

    @Override
    public void incrementCurrRow() {
        // checking ThreadContext complete first because that is non-volatile read and will normally
        // shortcut the condition without checking the volatile completed field in Transaction
        if (!threadContext.isCompleted() || !threadContext.getTransaction().isCompleted()) {
            super.incrementCurrRow();
        }
    }

    @Override
    public void setCurrRow(long row) {
        // checking ThreadContext complete first because that is non-volatile read and will normally
        // shortcut the condition without checking the volatile completed field in Transaction
        if (!threadContext.isCompleted() || !threadContext.getTransaction().isCompleted()) {
            super.setCurrRow(row);
        }
    }

    private void extendSync(long currTick, TimerImpl currentTimer) {
        // syncTimer is only null for trace entries added using addEntryEntry(), and these trace
        // entries are not returned from instrumentation api so no way for extend() to be called
        // when syncTimer is null
        checkNotNull(syncTimer);
        long priorDurationNanos = endTick - revisedStartTick;
        revisedStartTick = currTick - priorDurationNanos;
        extendedTimer = syncTimer.extend(currTick, currentTimer);
        extendQueryData(currTick);
    }

    @RequiresNonNull("asyncTimer")
    private void extendAsync() {
        ThreadContextThreadLocal.Holder holder =
                BytecodeServiceHolder.get().getCurrentThreadContextHolder();
        ThreadContextPlus currThreadContext = holder.get();
        long currTick = ticker.read();
        if (currThreadContext == threadContext) {
            // thread context was found in ThreadContextThreadLocal.Holder, so it is still
            // active, and so current timer must be non-null
            extendSync(currTick, checkNotNull(threadContext.getCurrentTimer()));
        } else {
            // set to null since its value is checked in stopAsync()
            extendedTimer = null;
            extendQueryData(currTick);
        }
        asyncTimer.extend(currTick);
    }

    // this is called for stopping an extension
    @Override
    public void stop() {
        if (--selfNestingLevel == 0) {
            if (isAsync()) {
                stopAsync();
            } else {
                stopSync(ticker.read());
            }
        }
    }

    private void stopSync(long endTick) {
        this.endTick = endTick;
        // the timer interface for this class is only expose through return value of extend()
        checkNotNull(extendedTimer).end(endTick);
        endQueryData(endTick);
        // it is not helpful to capture stack trace at end of async trace entry since it is
        // ended by a different thread (and by not capturing, it reduces thread safety needs)
        if (locationStackTrace == null && locationStackTraceThreshold != 0
                && endTick - revisedStartTick >= locationStackTraceThreshold) {
            StackTraceElement[] locationStackTrace = Thread.currentThread().getStackTrace();
            // strip up through this method, plus 1 (the instrumentation advice method)
            int index =
                    ThreadContextImpl.getNormalizedStartIndex(locationStackTrace, "stop", 1);
            setLocationStackTrace(ImmutableList.copyOf(locationStackTrace).subList(index,
                    locationStackTrace.length));
        }
    }

    @RequiresNonNull("asyncTimer")
    private void stopAsync() {
        long endTick = ticker.read();
        if (extendedTimer == null) {
            endQueryData(endTick);
        } else {
            stopSync(endTick);
        }
        asyncTimer.end(endTick);
    }

    boolean hasLocationStackTrace() {
        return locationStackTrace != null;
    }

    void setLocationStackTrace(ImmutableList<StackTraceElement> locationStackTrace) {
        this.locationStackTrace = locationStackTrace;
    }

    ThreadContextImpl getThreadContext() {
        return threadContext;
    }

    @Nullable
    TraceEntryImpl getParentTraceEntry() {
        return parentTraceEntry;
    }

    @Nullable
    TraceEntryImpl getNextTraceEntry() {
        return nextTraceEntry;
    }

    void setNextTraceEntry(TraceEntryImpl nextTraceEntry) {
        this.nextTraceEntry = nextTraceEntry;
    }

    boolean isAuxThreadRoot() {
        // TODO this is a little hacky depending on timer name
        return syncTimer != null && syncTimer.getName().equals("auxiliary thread");
    }

    private boolean isCompleted() {
        // initialComplete is needed for async trace entries which have selfNestingLevel = 0 after
        // calling stopSyncTimer(), but are not complete until end() is called
        return initialComplete && selfNestingLevel == 0;
    }

    @EnsuresNonNullIf(expression = "asyncTimer", result = true)
    private boolean isAsync() {
        return asyncTimer != null;
    }

    private void endWithLocationStackTraceInternal(long threshold, TimeUnit unit) {
        if (initialComplete) {
            // this guards against end*() being called multiple times on async trace entries
            return;
        }
        if (isAsync()) {
            // it is not helpful to capture stack trace at end of async trace entry since it is
            // ended by a different thread (and by not capturing, it reduces thread safety needs)
            endInternal(endTick, null);
            return;
        }
        long endTick = ticker.read();
        long thresholdNanos = unit.toNanos(threshold);
        if (endTick - startTick >= thresholdNanos) {
            StackTraceElement[] locationStackTrace = Thread.currentThread().getStackTrace();
            // strip up through this method, plus 2 additional methods:
            // TraceEntryImpl.endWithLocationStackTrace/endWithStackTrace() and the instrumentation
            // advice method
            int index = ThreadContextImpl.getNormalizedStartIndex(locationStackTrace,
                    "endWithLocationStackTraceInternal", 2);
            setLocationStackTrace(ImmutableList.copyOf(locationStackTrace).subList(index,
                    locationStackTrace.length));
        } else {
            // store threshold in case this trace entry is extended, see extend() below
            locationStackTraceThreshold = thresholdNanos;
        }
        endInternal(endTick, null);
    }

    private void endWithErrorInternal(@Nullable String message, @Nullable Throwable t) {
        ErrorMessage errorMessage = ErrorMessage.create(message, t,
                threadContext.getTransaction().getThrowableFrameLimitCounter());
        endInternal(ticker.read(), errorMessage);
    }

    private void endInternal(long endTick, @Nullable ErrorMessage errorMessage) {
        // syncTimer is only null for trace entries added using addEntryEntry(), and they are not
        // returned from instrumentation api so no way for end...() to be called
        checkNotNull(syncTimer);
        if (isAsync()) {
            asyncTimer.end(endTick);
        } else {
            syncTimer.end(endTick);
        }
        endQueryData(endTick);
        this.errorMessage = errorMessage;
        this.endTick = endTick;
        if (isAsync()) {
            threadContext.getTransaction().memoryBarrierWrite();
            // already popped in stopSyncTimer()
        } else {
            selfNestingLevel--;
            threadContext.popEntry(this, endTick);
        }
        initialComplete = true;
    }

    private String getRowCountSuffix() {
        if (!isRowNavigationAttempted()) {
            return "";
        }
        long rowCount = getRowCount();
        if (rowCount == 1) {
            return " => 1 row";
        } else {
            return " => " + rowCount + " rows";
        }
    }

    @Override
    public void stopSyncTimer() {
        // syncTimer is only null for trace entries added using addEntryEntry(), and they are not
        // returned from instrumentation api so no way for stopSyncTimer() to be called
        checkNotNull(syncTimer);
        syncTimer.stop();
        selfNestingLevel--;
        threadContext.popNonRootEntry(this);
    }

    @Override
    public Timer extendSyncTimer() {
        // FIXME xyzzy conversion revisit decision not to pass in ThreadContext to this method
        ThreadContext currThreadContext = threadContext.getTransaction().getTransactionRegistry()
                .getCurrentThreadContextHolder().get();
        if (currThreadContext != threadContext) {
            return NopTransactionService.TIMER;
        }
        // syncTimer is only null for trace entries added using addEntryEntry(), and they are not
        // returned from instrumentation api so no way for extendSyncTimer() to be called
        checkNotNull(syncTimer);
        // thread context was passed in from instrumentation, so it is still active, and so current
        // timer must be non-null
        return syncTimer.extend(checkNotNull(threadContext.getCurrentTimer()));
    }

    // this is used for logging, in particular in TraceEntryComponent.popEntryBailout()
    // and in ThreadContextImpl.populateParentChildMap()
    @Override
    public String toString() {
        if (messageSupplier instanceof MessageSupplier) {
            return ((ReadableMessage) ((MessageSupplier) messageSupplier).get()).getText();
        } else if (messageSupplier instanceof QueryMessageSupplier) {
            ReadableMessage readableMessage =
                    (ReadableMessage) ((QueryMessageSupplier) messageSupplier).get();
            // FIXME xyzzy update
            // return readableMessage.getPrefix() + checkNotNull(getQueryText())
            // + readableMessage.getSuffix();
            return checkNotNull(getQueryText());
        }
        if (errorMessage != null) {
            return errorMessage.message();
        }
        return checkNotNull(super.toString());
    }
}
