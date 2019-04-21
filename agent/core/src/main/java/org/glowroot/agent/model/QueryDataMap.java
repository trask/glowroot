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

import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

// micro-optimized map for query data
public class QueryDataMap {

    private static final Object CHAINED_KEY = new Object();

    private final String dest;

    // capacity must always be a power of 2, see comments in get() and put()
    private int capacity = 4;
    private @Nullable Object[] table = new Object[capacity << 1];

    private int size = 0;
    private int threshold = 3; // 0.75 * capacity

    public QueryDataMap(String dest) {
        this.dest = dest;
    }

    public String getDest() {
        return dest;
    }

    public @Nullable SyncQueryData get(String key) {
        // this mask requires capacity to be a power of 2
        int bucket = (key.hashCode() & (capacity - 1)) << 1;
        Object keyAtBucket = table[bucket];
        Object value = table[bucket + 1];
        if (key.equals(keyAtBucket)) {
            return (SyncQueryData) value;
        }
        if (keyAtBucket == CHAINED_KEY) {
            return getChained(key, checkNotNull(value));
        }
        return null;
    }

    // IMPORTANT put assumes get was already called and key is not present in this map
    public void put(String key, SyncQueryData value) {
        if (size++ > threshold) {
            rehash();
        }
        putWithoutRehashCheck(key, value);
    }

    private void putWithoutRehashCheck(Object key, @Nullable Object value) {
        // this mask requires capacity to be a power of 2
        int bucket = (key.hashCode() & (capacity - 1)) << 1;
        Object keyAtBucket = table[bucket];
        if (keyAtBucket == null) {
            table[bucket] = key;
            table[bucket + 1] = value;
            return;
        }
        putChained(key, value, bucket, keyAtBucket);
    }

    private void putChained(Object key, @Nullable Object value, int bucket, Object keyAtBucket) {
        if (keyAtBucket == CHAINED_KEY) {
            @Nullable
            Object[] chain = (/*@Nullable*/ Object[]) checkNotNull(table[bucket + 1]);
            int chainLength = chain.length;
            for (int i = 0; i < chainLength; i += 2) {
                if (chain[i] == null) {
                    chain[i] = key;
                    chain[i + 1] = value;
                    return;
                }
            }
            @Nullable
            Object[] newChain = new Object[chainLength << 1];
            System.arraycopy(chain, 0, newChain, 0, chainLength);
            newChain[chainLength] = key;
            newChain[chainLength + 1] = value;
            table[bucket + 1] = newChain;
            return;
        }
        @Nullable
        Object[] chain = new Object[4];
        chain[0] = keyAtBucket;
        chain[1] = table[bucket + 1];
        chain[2] = key;
        chain[3] = value;
        table[bucket] = CHAINED_KEY;
        table[bucket + 1] = chain;
    }

    private void rehash() {
        @Nullable
        Object[] existingTable = table;
        capacity <<= 1;
        threshold <<= 1;
        table = new Object[capacity << 1];

        for (int i = 0; i < existingTable.length; i += 2) {
            Object key = existingTable[i];
            if (key == null) {
                continue;
            }
            if (key == CHAINED_KEY) {
                @Nullable
                Object[] values = (/*@Nullable*/ Object[]) checkNotNull(existingTable[i + 1]);
                putChainedValues(values);
            } else {
                putWithoutRehashCheck(key, existingTable[i + 1]);
            }
        }
    }

    private void putChainedValues(/*@Nullable*/ Object[] values) {
        for (int j = 0; j < values.length; j += 2) {
            Object chainedKey = values[j];
            if (chainedKey == null) {
                break;
            }
            putWithoutRehashCheck(chainedKey, values[j + 1]);
        }
    }

    private static @Nullable SyncQueryData getChained(String key, Object value) {
        @Nullable
        Object[] chainedTable = (/*@Nullable*/ Object[]) value;
        for (int i = 0; i < chainedTable.length; i += 2) {
            if (key.equals(chainedTable[i])) {
                return (SyncQueryData) chainedTable[i + 1];
            }
        }
        return null;
    }
}
