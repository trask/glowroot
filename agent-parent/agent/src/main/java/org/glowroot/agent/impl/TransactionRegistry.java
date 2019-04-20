/*
 * Copyright 2011-2016 the original author or authors.
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

import javax.annotation.Nullable;

import org.glowroot.agent.impl.TransactionCollection.TransactionEntry;
import org.glowroot.agent.model.ThreadContextImpl;
import org.glowroot.agent.model.Transaction;
import org.glowroot.agent.plugin.api.util.FastThreadLocal;
import org.glowroot.agent.plugin.api.util.FastThreadLocal.Holder;
import org.glowroot.common.util.UsedByGeneratedBytecode;

import static org.glowroot.agent.util.Checkers.castInitialized;

public class TransactionRegistry {

    private static final boolean disableThreadLocalOptimization =
            Boolean.getBoolean("glowroot.disable.thread.local.optimization");

    private static final Holder</*@Nullable*/ ThreadContextImpl> HOLDER =
            new Holder</*@Nullable*/ ThreadContextImpl>(null);

    // collection of active running transactions
    private final TransactionCollection transactions = new TransactionCollection();

    // active thread context being executed by the current thread
    private final FastThreadLocal</*@Nullable*/ ThreadContextImpl> currentThreadContext =
            new FastThreadLocal</*@Nullable*/ ThreadContextImpl>();

    public TransactionRegistry() {
        TransactionRegistryHolder.transactionRegistry = castInitialized(this);
    }

    @Nullable
    Transaction getCurrentTransaction() {
        ThreadContextImpl threadContext = getCurrentThreadContext();
        if (threadContext == null) {
            return null;
        }
        return threadContext.getTransaction();
    }

    private @Nullable ThreadContextImpl getCurrentThreadContext() {
        Thread thread = Thread.currentThread();
        if (!disableThreadLocalOptimization && thread instanceof FastThreadLocalThread) {
            return ((FastThreadLocalThread) thread).getCurrentThreadContext();
        }
        return null;
    }

    @UsedByGeneratedBytecode
    public Holder</*@Nullable*/ ThreadContextImpl> getCurrentThreadContextHolder() {
        Thread thread = Thread.currentThread();
        if (!disableThreadLocalOptimization && thread instanceof FastThreadLocalThread) {
            return ((FastThreadLocalThread) thread).getCurrentThreadContextHolder();
        }
        return HOLDER;
    }

    TransactionEntry addTransaction(Transaction transaction) {
        return transactions.add(transaction);
    }

    public Iterable<Transaction> getTransactions() {
        return transactions;
    }

    @UsedByGeneratedBytecode
    public static class TransactionRegistryHolder {

        private static @Nullable TransactionRegistry transactionRegistry;

        private TransactionRegistryHolder() {}

        public static @Nullable TransactionRegistry getTransactionRegistry() {
            return transactionRegistry;
        }
    }
}
