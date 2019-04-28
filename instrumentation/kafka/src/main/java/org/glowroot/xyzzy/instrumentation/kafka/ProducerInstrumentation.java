/*
 * Copyright 2018-2019 the original author or authors.
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
package org.glowroot.xyzzy.instrumentation.kafka;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;

import org.glowroot.xyzzy.instrumentation.api.Agent;
import org.glowroot.xyzzy.instrumentation.api.AsyncTraceEntry;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.ParameterHolder;
import org.glowroot.xyzzy.instrumentation.api.ThreadContext;
import org.glowroot.xyzzy.instrumentation.api.TimerName;
import org.glowroot.xyzzy.instrumentation.api.checker.Nullable;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindParameter;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindThrowable;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindTraveler;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnBefore;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnReturn;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnThrow;
import org.glowroot.xyzzy.instrumentation.api.weaving.Pointcut;

public class ProducerInstrumentation {

    @Pointcut(className = "org.apache.kafka.clients.producer.KafkaProducer", methodName = "send",
            methodParameterTypes = {"org.apache.kafka.clients.producer.ProducerRecord",
                    "org.apache.kafka.clients.producer.Callback"},
            nestingGroup = "kafka-send", timerName = "kafka send")
    public static class SendAdvice {

        private static final TimerName timerName = Agent.getTimerName(SendAdvice.class);

        @OnBefore
        public static @Nullable AsyncTraceEntry onBefore(ThreadContext context,
                @BindParameter @Nullable ProducerRecord<?, ?> record,
                @BindParameter ParameterHolder<Callback> callbackHolder) {
            if (record == null) {
                return null;
            }
            String topic = record.topic();
            if (topic == null) {
                topic = "";
            }
            AsyncTraceEntry asyncTraceEntry = context.startAsyncServiceCallEntry("Kafka", topic,
                    MessageSupplier.create("kafka send: {}", topic), timerName);
            Callback callback = callbackHolder.get();
            if (callback == null) {
                callbackHolder.set(new CallbackWrapperForNullDelegate(asyncTraceEntry));
            } else {
                callbackHolder.set(new CallbackWrapper(callback, asyncTraceEntry,
                        context.createAuxThreadContext()));
            }
            return asyncTraceEntry;
        }

        @OnReturn
        public static void onReturn(@BindTraveler @Nullable AsyncTraceEntry asyncTraceEntry) {
            if (asyncTraceEntry != null) {
                asyncTraceEntry.stopSyncTimer();
            }
        }

        @OnThrow
        public static void onThrow(@BindThrowable Throwable t,
                @BindTraveler @Nullable AsyncTraceEntry asyncTraceEntry) {
            if (asyncTraceEntry != null) {
                asyncTraceEntry.stopSyncTimer();
                asyncTraceEntry.endWithError(t);
            }
        }
    }
}
