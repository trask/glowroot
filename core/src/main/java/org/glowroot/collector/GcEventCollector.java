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
package org.glowroot.collector;

import org.glowroot.common.ScheduledRunnable;
import org.glowroot.jvm.GcEvent;
import org.glowroot.jvm.GcEvents;
import org.glowroot.jvm.GcEvents.GcEventListener;

class GcEventCollector extends ScheduledRunnable implements GcEventListener {

    private final GcEventRepository gcEventRepository;
    private final GcEvents gcEvents;

    static GcEventCollector create(GcEventRepository gcEventRepository, GcEvents gcEvents) {
        GcEventCollector gcEventCollector = new GcEventCollector(gcEventRepository, gcEvents);
        gcEvents.addListener(gcEventCollector);
        return gcEventCollector;
    }

    private GcEventCollector(GcEventRepository gcEventRepository, GcEvents gcEvents) {
        this.gcEventRepository = gcEventRepository;
        this.gcEvents = gcEvents;
    }

    @Override
    protected void runInternal() {
        gcEvents.checkForNewMXBeans();
    }

    @Override
    public void onGcEvent(GcEvent gcEvent) {
        gcEventRepository.store(gcEvent);
    }
}
