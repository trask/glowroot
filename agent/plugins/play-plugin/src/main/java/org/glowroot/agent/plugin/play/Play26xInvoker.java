/*
 * Copyright 2017-2018 the original author or authors.
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
package org.glowroot.agent.plugin.play;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.glowroot.agent.plugin.api.Logger;
import org.glowroot.agent.plugin.api.checker.Nullable;

public class Play26xInvoker {

    private static final Logger logger = Logger.getLogger(Play26xInvoker.class);

    private final @Nullable Object handlerDefTypedKey;

    public Play26xInvoker(Class<?> clazz) {
        if (isPlay26(clazz)) {
            Object handlerDefTypedKey = null;
            try {
                Class<?> attrsClass = Class.forName("play.api.routing.Router$Attrs$", false,
                        clazz.getClassLoader());
                Field instanceField = attrsClass.getField("MODULE$");
                Object instance = instanceField.get(null);
                Method handlerDefMethod = attrsClass.getMethod("HandlerDef");
                handlerDefTypedKey = handlerDefMethod.invoke(instance);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            this.handlerDefTypedKey = handlerDefTypedKey;
        } else {
            this.handlerDefTypedKey = null;
        }
    }

    public @Nullable Object getHandlerDefTypedKey() {
        return handlerDefTypedKey;
    }

    private static boolean isPlay26(Class<?> clazz) {
        try {
            Class.forName("play.api.routing.HandlerDef", false, clazz.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            logger.debug(e.getMessage(), e);
            return false;
        }
    }
}
