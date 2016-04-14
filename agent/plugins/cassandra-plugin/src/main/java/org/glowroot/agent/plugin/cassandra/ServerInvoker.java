/*
 * Copyright 2016 the original author or authors.
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
package org.glowroot.agent.plugin.cassandra;

import java.lang.reflect.Field;

import javax.annotation.Nullable;

import org.glowroot.agent.plugin.api.Agent;
import org.glowroot.agent.plugin.api.Logger;
import org.glowroot.agent.plugin.api.util.Reflection;

public class ServerInvoker {

    private static final Logger logger = Agent.getLogger(ServerInvoker.class);

    private final @Nullable Field messageField;

    public ServerInvoker(Class<?> clazz) {
        Class<?> queuedMessageClass = getQueuedMessageClass(clazz);
        messageField = Reflection.getDeclaredField(queuedMessageClass, "message");
    }

    @Nullable
    Object getMessage(Object queuedMessage) {
        return Reflection.getFieldValue(messageField, queuedMessage);
    }

    private static @Nullable Class<?> getQueuedMessageClass(Class<?> clazz) {
        try {
            return Class.forName("org.apache.cassandra.net.OutboundTcpConnection$QueuedMessage",
                    false, clazz.getClassLoader());
        } catch (ClassNotFoundException e) {
            logger.warn(e.getMessage(), e);
        }
        return null;
    }
}
