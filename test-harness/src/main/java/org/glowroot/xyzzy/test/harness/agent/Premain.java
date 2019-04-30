/*
 * Copyright 2019 the original author or authors.
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
package org.glowroot.xyzzy.test.harness.agent;

import java.io.File;
import java.lang.instrument.Instrumentation;

// this is only used via JavaagentContainer, with bootstrap class path already configured properly
public class Premain {

    private Premain() {}

    public static void premain(@SuppressWarnings("unused") String agentArgs,
            Instrumentation instrumentation) throws Exception {
        String tmpDirPath = System.getProperty("xyzzy.test.tmpDir");
        if (tmpDirPath == null) {
            throw new IllegalStateException("Missing xyzzy.test.tmpDir");
        }
        MainEntryPoint.premain(instrumentation, new File(tmpDirPath));
    }
}
