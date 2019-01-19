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
package org.glowroot.common.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ServiceCallCollector {

    private static final String LIMIT_EXCEEDED_BUCKET = "LIMIT EXCEEDED BUCKET";

    // first key is the network node id, second key is the service call text
    private final Map<String, Map<String, MutableServiceCall>> serviceCalls = Maps.newHashMap();
    private final Map<String, MutableServiceCall> limitExceededBuckets = Maps.newHashMap();
    private final int limit;

    // this is only used by UI
    private long lastCaptureTime;

    public ServiceCallCollector(int limit) {
        this.limit = limit;
    }

    public void updateLastCaptureTime(long captureTime) {
        lastCaptureTime = Math.max(lastCaptureTime, captureTime);
    }

    public long getLastCaptureTime() {
        return lastCaptureTime;
    }

    public List<MutableServiceCall> getSortedAndTruncatedServiceCalls() {
        List<MutableServiceCall> allServiceCalls = Lists.newArrayList();
        for (Map.Entry<String, Map<String, MutableServiceCall>> outerEntry : serviceCalls
                .entrySet()) {
            allServiceCalls.addAll(outerEntry.getValue().values());
        }
        if (allServiceCalls.size() <= limit) {
            allServiceCalls.addAll(limitExceededBuckets.values());
            return MutableServiceCall.byTotalDurationDesc.sortedCopy(allServiceCalls);
        }
        allServiceCalls = MutableServiceCall.byTotalDurationDesc.sortedCopy(allServiceCalls);
        List<MutableServiceCall> exceededServiceCalls =
                allServiceCalls.subList(limit, allServiceCalls.size());
        allServiceCalls = Lists.newArrayList(allServiceCalls.subList(0, limit));
        // do not modify original limit exceeded buckets since adding exceeded service calls below
        Map<String, MutableServiceCall> limitExceededBuckets = copyLimitExceededBuckets();
        for (MutableServiceCall exceededServiceCall : exceededServiceCalls) {
            String dest = exceededServiceCall.getDest();
            MutableServiceCall limitExceededBucket = limitExceededBuckets.get(dest);
            if (limitExceededBucket == null) {
                limitExceededBucket = new MutableServiceCall(dest, LIMIT_EXCEEDED_BUCKET);
                limitExceededBuckets.put(dest, limitExceededBucket);
            }
            limitExceededBucket.add(exceededServiceCall);
        }
        allServiceCalls.addAll(limitExceededBuckets.values());
        // need to re-sort now including limit exceeded bucket
        Collections.sort(allServiceCalls, MutableServiceCall.byTotalDurationDesc);
        return allServiceCalls;
    }

    public void mergeServiceCall(String dest, String serviceCallText,
            double totalDurationNanos, long executionCount) {
        MutableServiceCall serviceCall;
        if (serviceCallText.equals(LIMIT_EXCEEDED_BUCKET)) {
            serviceCall = limitExceededBuckets.get(dest);
            if (serviceCall == null) {
                serviceCall = new MutableServiceCall(dest, LIMIT_EXCEEDED_BUCKET);
                limitExceededBuckets.put(dest, serviceCall);
            }
        } else {
            Map<String, MutableServiceCall> serviceCallsForDest = serviceCalls.get(dest);
            if (serviceCallsForDest == null) {
                serviceCallsForDest = Maps.newHashMap();
                serviceCalls.put(dest, serviceCallsForDest);
            }
            serviceCall = serviceCallsForDest.get(serviceCallText);
            if (serviceCall == null) {
                serviceCall = new MutableServiceCall(dest, serviceCallText);
                serviceCallsForDest.put(serviceCallText, serviceCall);
            }
        }
        serviceCall.addToTotalDurationNanos(totalDurationNanos);
        serviceCall.addToExecutionCount(executionCount);
    }

    private Map<String, MutableServiceCall> copyLimitExceededBuckets() {
        Map<String, MutableServiceCall> copies = Maps.newHashMap();
        for (Map.Entry<String, MutableServiceCall> entry : limitExceededBuckets.entrySet()) {
            String dest = entry.getKey();
            MutableServiceCall limitExceededBucket = entry.getValue();
            MutableServiceCall copy = new MutableServiceCall(dest, LIMIT_EXCEEDED_BUCKET);
            copy.add(limitExceededBucket);
            copies.put(dest, copy);
        }
        return copies;
    }
}
