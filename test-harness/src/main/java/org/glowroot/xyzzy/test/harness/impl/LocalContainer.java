/*
 * Copyright 2011-2019 the original author or authors.
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
package org.glowroot.xyzzy.test.harness.impl;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import com.google.common.io.Files;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.glowroot.xyzzy.engine.weaving.IsolatedWeavingClassLoader;
import org.glowroot.xyzzy.test.harness.AppUnderTest;
import org.glowroot.xyzzy.test.harness.Container;
import org.glowroot.xyzzy.test.harness.IncomingSpan;
import org.glowroot.xyzzy.test.harness.agent.MainEntryPoint;
import org.glowroot.xyzzy.test.harness.util.Ports;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class LocalContainer implements Container {

    private final IsolatedWeavingClassLoader isolatedWeavingClassLoader;
    private final TraceCollector traceCollector;
    private final File tmpDir;

    private volatile @Nullable Thread executingAppThread;

    public static LocalContainer create() throws Exception {
        return new LocalContainer();
    }

    private LocalContainer() throws Exception {
        int collectorPort = Ports.getAvailable();
        traceCollector = new TraceCollector(collectorPort);
        traceCollector.start();
        tmpDir = Files.createTempDir();
        isolatedWeavingClassLoader = new IsolatedWeavingClassLoader(AppUnderTest.class);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(isolatedWeavingClassLoader);
        try {
            MainEntryPoint.start(null, tmpDir, collectorPort);
        } finally {
            Thread.currentThread().setContextClassLoader(loader);
        }
        MainEntryPoint.resetInstrumentationProperties();
    }

    @Override
    public void setInstrumentationProperty(String instrumentationId, String propertyName,
            boolean propertyValue) throws Exception {
        MainEntryPoint.setInstrumentationProperty(instrumentationId, propertyName, propertyValue);
    }

    @Override
    public void setInstrumentationProperty(String instrumentationId, String propertyName,
            @Nullable Double propertyValue) throws Exception {
        MainEntryPoint.setInstrumentationProperty(instrumentationId, propertyName, propertyValue);
    }

    @Override
    public void setInstrumentationProperty(String instrumentationId, String propertyName,
            String propertyValue) throws Exception {
        MainEntryPoint.setInstrumentationProperty(instrumentationId, propertyName, propertyValue);
    }

    @Override
    public void setInstrumentationProperty(String instrumentationId, String propertyName,
            List<String> propertyValue) throws Exception {
        MainEntryPoint.setInstrumentationProperty(instrumentationId, propertyName, propertyValue);
    }

    @Override
    public IncomingSpan execute(Class<? extends AppUnderTest> appClass, Serializable... args)
            throws Exception {
        return executeInternal(appClass, null, null, args);
    }

    @Override
    public IncomingSpan execute(Class<? extends AppUnderTest> appClass, String transactionType,
            Serializable... args) throws Exception {
        return executeInternal(appClass, transactionType, null, args);
    }

    @Override
    public IncomingSpan execute(Class<? extends AppUnderTest> appClass, String transactionType,
            String transactionName, Serializable... args) throws Exception {
        return executeInternal(appClass, transactionType, transactionName, args);
    }

    @Override
    public void executeNoExpectedTrace(Class<? extends AppUnderTest> appClass, Serializable... args)
            throws Exception {
        executeInternal(appClass, args);
        MILLISECONDS.sleep(10);
        if (traceCollector != null && traceCollector.hasIncomingSpan()) {
            throw new IllegalStateException("Trace was collected when none was expected");
        }
    }

    @Override
    public void resetAfterEachTest() throws Exception {
        MainEntryPoint.resetInstrumentationProperties();
    }

    @Override
    public void close() throws Exception {
        traceCollector.close();
    }

    public IncomingSpan executeInternal(Class<? extends AppUnderTest> appClass,
            @Nullable String transactionType, @Nullable String transactionName, Serializable[] args)
            throws Exception {
        checkNotNull(traceCollector);
        executeInternal(appClass, args);
        IncomingSpan incomingSpan =
                traceCollector.getCompletedIncomingSpan(transactionType, transactionName, 10,
                        SECONDS);
        traceCollector.clearIncomingSpans();
        return incomingSpan;
    }

    private void executeInternal(Class<? extends AppUnderTest> appClass, Serializable[] args)
            throws Exception {
        IsolatedWeavingClassLoader isolatedWeavingClassLoader = this.isolatedWeavingClassLoader;
        checkNotNull(isolatedWeavingClassLoader);
        ClassLoader previousContextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(isolatedWeavingClassLoader);
        executingAppThread = Thread.currentThread();
        try {
            AppUnderTest app = isolatedWeavingClassLoader.newInstance(appClass, AppUnderTest.class);
            app.executeApp(args);
        } finally {
            executingAppThread = null;
            Thread.currentThread().setContextClassLoader(previousContextClassLoader);
        }
    }
}
