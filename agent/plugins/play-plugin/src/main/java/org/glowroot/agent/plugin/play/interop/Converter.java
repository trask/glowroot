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
package org.glowroot.agent.plugin.play.interop;

import java.lang.reflect.Constructor;

import org.glowroot.agent.plugin.api.Logger;
import org.glowroot.agent.plugin.api.checker.Nullable;

public class Converter {

    private static final Logger logger = Logger.getLogger(Converter.class);

    private final Constructor<?> scalaFunction1Constructor;

    public static @Nullable Converter create(@Nullable ClassLoader loader) {
        if (loader == null) {
            return null;
        }
        Class<?> scalaFunction1Class = ScalaFunction1ClassBuilder.create(loader);
        if (scalaFunction1Class == null) {
            return null;
        }
        Constructor<?> scalaFunction1Constructor;
        try {
            scalaFunction1Constructor = scalaFunction1Class.getConstructor(Function1.class);
        } catch (NoSuchMethodException e) {
            logger.error(e.getMessage(), e);
            return null;
        }
        return new Converter(scalaFunction1Constructor);
    }

    public Converter(Constructor<?> scalaFunction1Constructor) {
        this.scalaFunction1Constructor = scalaFunction1Constructor;
    }

    public @Nullable Object toScalaFunction1(Function1<?, ?> f) {
        try {
            return scalaFunction1Constructor.newInstance(f);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }
}
