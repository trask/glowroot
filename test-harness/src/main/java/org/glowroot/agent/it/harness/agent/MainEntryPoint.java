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
package org.glowroot.agent.it.harness.agent;

import java.io.File;
import java.lang.instrument.Instrumentation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;

import org.glowroot.xyzzy.engine.init.EngineModule;
import org.glowroot.xyzzy.engine.init.MainEntryPointUtil;

import static com.google.common.base.Preconditions.checkNotNull;

public class MainEntryPoint {

    private MainEntryPoint() {}

    public static void premain(Instrumentation instrumentation, File agentJarFile) {
        // DO NOT USE ANY GUAVA CLASSES before initLogging() because they trigger loading of jul
        // (and thus org.glowroot.xyzzy.engine.jul.Logger and thus glowroot's shaded slf4j)
        Logger startupLogger;
        try {
            startupLogger = MainEntryPointUtil.initLogging("org.glowroot.zipkin", instrumentation);
        } catch (Throwable t) {
            // log error but don't re-throw which would prevent monitored app from starting
            // also, don't use logger since it failed to initialize
            System.err.println("Agent failed to start: " + t.getMessage());
            t.printStackTrace();
            return;
        }
        try {
            // FIXME simple way to avoid conflict when multiple java processes being monitored
            start(instrumentation, agentJarFile, new File(agentJarFile.getParentFile(), "tmp"));
        } catch (Throwable t) {
            // log error but don't re-throw which would prevent monitored app from starting
            startupLogger.error("Agent failed to start: {}", t.getMessage(), t);
        }
    }

    public static void start(Instrumentation instrumentation, @Nullable File agentJarFile,
            File tmpDir) throws Exception {

        AgentImpl agent = new AgentImpl();

        EngineModule.createWithManyDefaults(instrumentation, tmpDir,
                Global.getThreadContextThreadLocal(), new GlowrootServiceImpl(), agent,
                agentJarFile);

        int port = checkNotNull(Integer.getInteger("xyzzy.test.collectorPort"));
        Global.setTraceReporter(new TraceReporter(port));
    }

    public static void resetConfig() {
        // TODO
    }
}
