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

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.glowroot.jvm.GcEvent.GcEventMemoryPool;

public class GcEvents {

    private static final Logger logger = LoggerFactory.getLogger(GcEvents.class);

    // the JVM "may add or remove GarbageCollectorMXBean during execution"
    // (see javadoc for ManagementFactory.getGarbageCollectorMXBeans())
    // so need to track MXBeans that the internal listener has been registered with
    private final Set<GarbageCollectorMXBean> knownMXBeans = Sets.newCopyOnWriteArraySet();

    private final MXBeanListener internalListener = new MXBeanListener();

    static OptionalService<GcEvents> create() {
        List<GarbageCollectorMXBean> garbageCollectorMXBeans =
                ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean garbageCollectorMXBean : garbageCollectorMXBeans) {
            if (!(garbageCollectorMXBean instanceof NotificationEmitter)) {
                return OptionalService.unavailable("Garbage collector notifications are"
                        + " not available (introduced in Oracle Java SE 7u4)");
            }
        }
        GcEvents gcEvents = new GcEvents();
        gcEvents.checkForNewMXBeans();
        return OptionalService.available(gcEvents);
    }

    private GcEvents() {}

    public void addListener(GcEventListener listener) {
        internalListener.addListener(listener);
    }

    public void removeListener(GcEventListener listener) {
        internalListener.removeListener(listener);
    }

    // need to call this method from time to time to check for new GarbageCollectorMXBeans
    public void checkForNewMXBeans() {
        boolean firstCheck = knownMXBeans.isEmpty();
        List<GarbageCollectorMXBean> garbageCollectorMXBeans =
                ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean garbageCollectorMXBean : garbageCollectorMXBeans) {
            if (knownMXBeans.add(garbageCollectorMXBean)) {
                if (garbageCollectorMXBean instanceof NotificationEmitter) {
                    ((NotificationEmitter) garbageCollectorMXBean).addNotificationListener(
                            internalListener, null, null);
                    if (!firstCheck) {
                        // curious if/how this ever occurs
                        logger.info("new garbageCollectorMXBean found during execution: {}",
                                garbageCollectorMXBean.getName());
                    }
                } else {
                    logger.error("garbageCollectorMXBean does not implement"
                            + " NotificationEmitter: {}", garbageCollectorMXBean.getName());
                }
            }
        }
    }

    public interface GcEventListener {
        void onGcEvent(GcEvent gcEvent);
    }

    // using nested class instead of having GcEvents implement NotificationListener directly
    // in order to keep GcEvents contract clean
    private static class MXBeanListener implements NotificationListener {

        private final Set<GcEventListener> externalListeners = Sets.newCopyOnWriteArraySet();

        @Override
        public void handleNotification(Notification notification, Object handback) {
            try {
                handleNotificationInternal(notification);
            } catch (Throwable t) {
                // don't propagate exceptions back to caller
                logger.error(t.getMessage(), t);
            }
        }

        private void handleNotificationInternal(Notification notification) {
            CompositeData userData = (CompositeData) notification.getUserData();
            String action = (String) userData.get("gcAction");
            CompositeData gcInfo = (CompositeData) userData.get("gcInfo");
            long duration = (Long) gcInfo.get("duration");
            if (!action.equals("end of major GC") && duration < 1000) {
                // ignore minor collections of less than 1 second
                return;
            }
            String cause = (String) userData.get("gcCause");
            String collectorName = (String) userData.get("gcName");
            long collectorCount = (Long) gcInfo.get("id");
            long startTime = (Long) gcInfo.get("startTime");
            long endTime = (Long) gcInfo.get("endTime");
            TabularData memoryUsageBeforeGcTabularData =
                    (TabularData) gcInfo.get("memoryUsageBeforeGc");
            TabularData memoryUsageAfterGcTabularData =
                    (TabularData) gcInfo.get("memoryUsageAfterGc");

            Map<String, MemoryUsage> memoryUsageBeforeGcMap =
                    extractMap(memoryUsageBeforeGcTabularData);
            Map<String, MemoryUsage> memoryUsageAfterGcMap =
                    extractMap(memoryUsageAfterGcTabularData);
            List<GcEventMemoryPool> memoryPools = Lists.newArrayList();
            for (Entry<String, MemoryUsage> entry : memoryUsageAfterGcMap.entrySet()) {
                String memoryPoolName = entry.getKey();
                MemoryUsage memoryUsageAfterGc = entry.getValue();
                MemoryUsage memoryUsageBeforeGc = memoryUsageBeforeGcMap.get(memoryPoolName);
                if (memoryUsageBeforeGc == null) {
                    logger.error("corresponding 'before' memory usage entry not found for"
                            + " collector {} and pool {}", collectorName, memoryPoolName);
                    continue;
                }
                memoryPools.add(ImmutableGcEventMemoryPool.builder()
                        .memoryPoolName(memoryPoolName)
                        .usedBeforeGc(memoryUsageBeforeGc.getUsed())
                        .usedAfterGc(memoryUsageAfterGc.getUsed())
                        .build());
            }
            RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
            long jvmStartTime = runtimeMXBean.getStartTime();
            GcEvent gcEvent = ImmutableGcEvent.builder()
                    .action(action)
                    .cause(cause)
                    .collectorName(collectorName)
                    .collectorCount(collectorCount)
                    .startTime(jvmStartTime + startTime)
                    .endTime(jvmStartTime + endTime)
                    .duration(duration)
                    .addAllMemoryPools(GcEventMemoryPool.ORDERING_BY_POOL_NAME
                            .immutableSortedCopy(memoryPools))
                    .build();
            for (GcEventListener externalListener : externalListeners) {
                externalListener.onGcEvent(gcEvent);
            }
        }
        private Map<String, MemoryUsage> extractMap(TabularData memoryUsageBeforeGc) {
            Map<String, MemoryUsage> memoryUsageBeforeGcMap = Maps.newHashMap();
            for (Object key : memoryUsageBeforeGc.keySet()) {
                // TabularData.keySet() returns "Set<List<?>> but is declared Set<?> for
                // compatibility reasons" (see javadocs) so safe to cast to List<?>
                List<?> keyList = (List<?>) key;
                CompositeData compositeData = memoryUsageBeforeGc.get(keyList.toArray());
                String memoryPoolName = (String) compositeData.get("key");
                MemoryUsage memoryUsage =
                        MemoryUsage.from((CompositeData) compositeData.get("value"));
                memoryUsageBeforeGcMap.put(memoryPoolName, memoryUsage);
            }
            return memoryUsageBeforeGcMap;
        }

        public void addListener(GcEventListener listener) {
            externalListeners.add(listener);
        }

        public void removeListener(GcEventListener listener) {
            externalListeners.remove(listener);
        }
    }
}
