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
package org.glowroot.xyzzy.engine.weaving;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PointcutClassFileTransformer implements ClassFileTransformer {

    private static final Logger logger =
            LoggerFactory.getLogger(PointcutClassFileTransformer.class);

    @Override
    public byte /*@Nullable*/ [] transform(@Nullable ClassLoader loader, @Nullable String className,
            @Nullable Class<?> classBeingRedefined, @Nullable ProtectionDomain protectionDomain,
            byte[] bytes) {
        try {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            PointcutClassVisitor cv = new PointcutClassVisitor(cw);
            ClassReader cr = new ClassReader(bytes);
            cr.accept(new JSRInlinerClassVisitor(cv), ClassReader.EXPAND_FRAMES);
            if (cv.isConstructorPointcut() && cv.hasOnBeforeMethod()) {
                return cw.toByteArray();
            }
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
        return null;
    }
}
