/*
 * Copyright 2018 the original author or authors.
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
package org.glowroot.agent.bytecode.api;

public class FastGlowrootThread extends Thread {

    private final ThreadContextHolder threadContextHolder = new ThreadContextHolder();

    public FastGlowrootThread() {
        super();
    }

    public FastGlowrootThread(Runnable target) {
        super(target);
    }

    public FastGlowrootThread(ThreadGroup group, Runnable target) {
        super(group, target);
    }

    public FastGlowrootThread(String name) {
        super(name);
    }

    public FastGlowrootThread(ThreadGroup group, String name) {
        super(group, name);
    }

    public FastGlowrootThread(Runnable target, String name) {
        super(target, name);
    }

    public FastGlowrootThread(ThreadGroup group, Runnable target, String name) {
        super(group, target, name);
    }

    public FastGlowrootThread(ThreadGroup group, Runnable target, String name, long stackSize) {
        super(group, target, name, stackSize);
    }

    public ThreadContextHolder getThreadContextHolder() {
        return threadContextHolder;
    }
}
