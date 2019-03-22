/*
 * Copyright 2015-2019 the original author or authors.
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
package org.glowroot.agent.init;

import java.io.Closeable;
import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.List;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.glowroot.common.util.OnlyUsedByTests;
import org.glowroot.xyzzy.engine.init.PreCheckLoadedClasses.PreCheckClassFileTransformer;

public interface GlowrootAgentInit {

    void init(@Nullable File instrumentationDir, List<File> confDirs, File logDir, File tmpDir,
            @Nullable File glowrootJarFile, Map<String, String> properties,
            @Nullable Instrumentation instrumentation,
            @Nullable PreCheckClassFileTransformer preCheckClassFileTransformer,
            Class<?>[] allPreCheckLoadedClasses, String glowrootVersion,
            Closeable agentDirLockCloseable) throws Exception;

    @OnlyUsedByTests
    void initConfigForTests() throws Exception;

    @OnlyUsedByTests
    void resetConfigForTests() throws Exception;

    @OnlyUsedByTests
    void close() throws Exception;

    @OnlyUsedByTests
    void awaitClose() throws Exception;
}
