/*
 * Copyright 2015-2016 the original author or authors.
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
package org.glowroot.agent.weaving;

import javax.annotation.Nullable;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

import org.glowroot.agent.impl.FastThreadLocalThread;

import static org.objectweb.asm.Opcodes.ASM5;

class JettyThreadPoolHackClassVisitor extends ClassVisitor {

    private final ClassWriter cw;

    JettyThreadPoolHackClassVisitor(ClassWriter cw) {
        super(ASM5, cw);
        this.cw = cw;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
            @Nullable String signature, String /*@Nullable*/[] exceptions) {
        MethodVisitor mv = cw.visitMethod(access, name, desc, signature, exceptions);
        if (name.equals("newThread") && desc.equals("(Ljava/lang/Runnable;)Ljava/lang/Thread;")) {
            return new JettyThreadPoolHackMethodVisitor(mv, access, name, desc);
        } else {
            return mv;
        }
    }

    private static class JettyThreadPoolHackMethodVisitor extends AdviceAdapter {

        private static final String GLOWROOT_THREAD_INTERNAL_NAME =
                Type.getType(FastThreadLocalThread.class).getInternalName();

        private JettyThreadPoolHackMethodVisitor(MethodVisitor mv, int access, String name,
                String desc) {
            super(ASM5, mv, access, name, desc);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            if (opcode == NEW && type.equals("java/lang/Thread")) {
                mv.visitTypeInsn(opcode, GLOWROOT_THREAD_INTERNAL_NAME);
            } else {
                mv.visitTypeInsn(opcode, type);
            }
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc,
                boolean itf) {
            if (opcode == INVOKESPECIAL && owner.equals("java/lang/Thread")
                    && name.equals("<init>")) {
                super.visitMethodInsn(opcode, GLOWROOT_THREAD_INTERNAL_NAME, name, desc, itf);
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }
    }
}
