/*
 * Copyright 2015 the original author or authors.
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
package org.glowroot.jvm;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.Ordering;
import org.immutables.value.Json;
import org.immutables.value.Value;

import static com.google.common.base.Preconditions.checkNotNull;

@Value.Immutable
@Json.Marshaled
public abstract class GcEvent {

    public abstract String action();
    public abstract String cause();
    public abstract String collectorName();
    public abstract long collectorCount();
    public abstract long startTime();
    public abstract long endTime();
    public abstract long duration(); // milliseconds
    public abstract List<GcEventMemoryPool> memoryPools();

    @Value.Immutable
    @Json.Marshaled
    public static abstract class GcEventMemoryPool {

        static final Ordering<GcEventMemoryPool> ORDERING_BY_POOL_NAME =
                new Ordering<GcEventMemoryPool>() {
                    @Override
                    public int compare(@Nullable GcEventMemoryPool left,
                            @Nullable GcEventMemoryPool right) {
                        checkNotNull(left);
                        checkNotNull(right);
                        return left.memoryPoolName().compareToIgnoreCase(
                                right.memoryPoolName());
                    }
                };

        public abstract String memoryPoolName();
        public abstract long usedBeforeGc();
        public abstract long usedAfterGc();
    }
}
