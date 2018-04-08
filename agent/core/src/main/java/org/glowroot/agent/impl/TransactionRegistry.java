/*
 * Copyright 2011-2018 the original author or authors.
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

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.glowroot.agent.bytecode.api.ThreadContextThreadLocal;
import org.glowroot.agent.util.IterableWithSelfRemovableEntries;

public class TransactionRegistry implements Iterable<Transaction> {

    // active thread context being executed by the current thread
    private final ThreadContextThreadLocal currentThreadContext = new ThreadContextThreadLocal();

    // head and tail are non-volatile since only accessed under lock
    private @Nullable Transaction head;
    private @Nullable Transaction tail;

    // all structural changes are made under lock for simplicity
    // TODO implement lock free structure
    private final Object lock = new Object();

    // weak references are expensive, so only use them for long running transactions that may have
    // missed their ending due to buggy glowroot plugin
    private final IterableWithSelfRemovableEntries<Transaction> longRunningTransactions =
            new IterableWithSelfRemovableEntries<Transaction>();

    @Nullable
    Transaction getCurrentTransaction() {
        ThreadContextImpl threadContext = (ThreadContextImpl) currentThreadContext.get();
        if (threadContext == null) {
            return null;
        }
        return threadContext.getTransaction();
    }

    public ThreadContextThreadLocal.Holder getCurrentThreadContextHolder() {
        return currentThreadContext.getHolder();
    }

    void addTransaction(Transaction transaction) {
        synchronized (lock) {
            if (tail == null) {
                head = transaction;
                tail = transaction;
            } else {
                tail.setLinkedNext(transaction);
                transaction.setLinkedPrevious(tail);
                tail = transaction;
            }
        }
    }

    void removeTransaction(Transaction transaction) {
        synchronized (lock) {
            Transaction previous = transaction.getLinkedPrevious();
            Transaction next = transaction.getLinkedNext();
            if (previous != null) {
                previous.setLinkedNext(next);
            }
            if (next != null) {
                next.setLinkedPrevious(previous);
            }
            if (transaction == head) {
                head = next;
            }
            if (transaction == tail) {
                tail = previous;
            }
        }
    }

    public Iterable<Transaction> getTransactions() {
        return this;
    }

    @Override
    public Iterator<Transaction> iterator() {
        synchronized (lock) {
            return new ElementIterator(head);
        }
    }

    private class ElementIterator implements Iterator<Transaction> {

        private @Nullable Transaction next;

        private ElementIterator(@Nullable Transaction head) {
            next = head;
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public Transaction next() {
            if (next == null) {
                throw new NoSuchElementException();
            }
            Transaction curr = next;
            next = next.getLinkedNext();
            return curr;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
