/*
 * Copyright 2012-2019 the original author or authors.
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
package org.glowroot.xyzzy.engine.weaving.targets;

public class NestingMisc implements Misc {

    private static final String yes;

    private final boolean stopNesting;

    // test with static initializer present to verify static initializer merging
    static {
        yes = "yes";
    }

    public NestingMisc() {
        this(false);
    }

    private NestingMisc(boolean stopNesting) {
        this.stopNesting = stopNesting;
    }

    @Override
    public void execute1() {
        if (!stopNesting) {
            new NestingMisc(true).execute1();
        }
    }

    @Override
    public String executeWithReturn() {
        return yes;
    }

    @Override
    public void executeWithArgs(String one, int two) {}
}
