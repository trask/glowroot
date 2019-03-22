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
package org.glowroot.agent.init;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableList;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.glowroot.agent.collector.Collector;
import org.glowroot.agent.config.ConfigService;
import org.glowroot.agent.impl.ConfigServiceImpl;
import org.glowroot.agent.impl.GlowrootServiceImpl;
import org.glowroot.agent.impl.StackTraceCollector;
import org.glowroot.agent.impl.TraceCollector;
import org.glowroot.agent.impl.TransactionProcessor;
import org.glowroot.agent.impl.TransactionRegistry;
import org.glowroot.agent.live.LiveAggregateRepositoryImpl;
import org.glowroot.agent.live.LiveJvmServiceImpl;
import org.glowroot.agent.live.LiveTraceRepositoryImpl;
import org.glowroot.agent.live.LiveWeavingServiceImpl;
import org.glowroot.agent.util.OptionalService;
import org.glowroot.agent.util.ThreadAllocatedBytes;
import org.glowroot.agent.util.Tickers;
import org.glowroot.common.util.Clock;
import org.glowroot.common.util.OnlyUsedByTests;
import org.glowroot.common.util.ScheduledRunnable;
import org.glowroot.xyzzy.engine.bytecode.api.ThreadContextThreadLocal;
import org.glowroot.xyzzy.engine.config.InstrumentationDescriptor;
import org.glowroot.xyzzy.engine.impl.InstrumentationServiceImpl.ConfigServiceFactory;
import org.glowroot.xyzzy.engine.impl.TimerNameCache;
import org.glowroot.xyzzy.engine.init.EngineModule;
import org.glowroot.xyzzy.engine.init.PreCheckLoadedClasses.PreCheckClassFileTransformer;
import org.glowroot.xyzzy.engine.util.LazyPlatformMBeanServer;
import org.glowroot.xyzzy.engine.weaving.BytecodeServiceImpl.OnEnteringMain;
import org.glowroot.xyzzy.engine.weaving.IsolatedWeavingClassLoader;
import org.glowroot.xyzzy.engine.weaving.PreloadSomeSuperTypesCache;
import org.glowroot.xyzzy.engine.weaving.Weaver;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

public class AgentModule {

    // 1 minute
    private static final long ROLLUP_0_INTERVAL_MILLIS =
            Long.getLong("glowroot.internal.rollup.0.intervalMillis", MINUTES.toMillis(1));

    private final Clock clock;
    private final Ticker ticker;

    private final ConfigService configService;
    private final TransactionRegistry transactionRegistry;
    private final Random random;

    private final EngineModule engineModule;

    private volatile @MonotonicNonNull DeadlockedActiveWeavingRunnable deadlockedActiveWeavingRunnable;
    private volatile @MonotonicNonNull WriteSomeSuperTypesCacheToFileRunnable writeSomeSuperTypesCacheToFileRunnable;
    private volatile @MonotonicNonNull TraceCollector traceCollector;
    private volatile @MonotonicNonNull TransactionProcessor transactionProcessor;

    private volatile @MonotonicNonNull GaugeCollector gaugeCollector;
    private volatile @MonotonicNonNull StackTraceCollector stackTraceCollector;

    private volatile @MonotonicNonNull ImmediateTraceStoreWatcher immedateTraceStoreWatcher;

    private final boolean jvmRetransformClassesSupported;

    private volatile @MonotonicNonNull LiveTraceRepositoryImpl liveTraceRepository;
    private volatile @MonotonicNonNull LiveAggregateRepositoryImpl liveAggregateRepository;
    private volatile @MonotonicNonNull LiveWeavingServiceImpl liveWeavingService;
    private volatile @MonotonicNonNull LiveJvmServiceImpl liveJvmService;

    // accepts @Nullable Ticker to deal with shading issues when called from GlowrootModule
    public AgentModule(Clock clock, @Nullable Ticker nullableTicker,
            final List<InstrumentationDescriptor> instrumentationDescriptors,
            final ConfigService configService, @Nullable Instrumentation instrumentation,
            @Nullable File glowrootJarFile, File tmpDir,
            @Nullable PreCheckClassFileTransformer preCheckClassFileTransformer,
            Class<?>[] allPreCheckLoadedClasses) throws Exception {

        this.clock = clock;
        this.ticker = nullableTicker == null ? Tickers.getTicker() : nullableTicker;
        this.configService = configService;

        ThreadContextThreadLocal threadContextThreadLocal = new ThreadContextThreadLocal();
        TimerNameCache timerNameCache = new TimerNameCache();

        transactionRegistry = TransactionRegistry.create(threadContextThreadLocal, configService,
                timerNameCache, ticker, clock);

        ConfigServiceFactory configServiceFactory = new ConfigServiceFactory() {
            @Override
            public org.glowroot.xyzzy.instrumentation.api.config.ConfigService create(
                    String instrumentationId) {
                return ConfigServiceImpl.create(configService, instrumentationDescriptors,
                        instrumentationId);
            }
        };
        engineModule = new EngineModule(instrumentation, tmpDir, ticker, instrumentationDescriptors,
                configService.getAdviceConfigs(), threadContextThreadLocal, timerNameCache,
                configServiceFactory, transactionRegistry, ImmutableList.<String>of(),
                preCheckClassFileTransformer, allPreCheckLoadedClasses, glowrootJarFile);

        random = new Random();

        if (instrumentation == null) {
            // instrumentation is null when debugging with LocalContainer
            IsolatedWeavingClassLoader isolatedWeavingClassLoader =
                    (IsolatedWeavingClassLoader) Thread.currentThread().getContextClassLoader();
            checkNotNull(isolatedWeavingClassLoader);
            isolatedWeavingClassLoader.setWeaver(engineModule.getWeaver());
            jvmRetransformClassesSupported = false;
        } else {
            jvmRetransformClassesSupported = instrumentation.isRetransformClassesSupported();
        }

        ManagementFactory.getThreadMXBean().setThreadCpuTimeEnabled(true);
        ManagementFactory.getThreadMXBean().setThreadContentionMonitoringEnabled(true);
    }

    public void setOnEnteringMain(OnEnteringMain onEnteringMain) {
        engineModule.addOnEnteringMain(onEnteringMain);
    }

    public void onEnteringMain(ScheduledExecutorService backgroundExecutor, Collector collector,
            @Nullable Instrumentation instrumentation, @Nullable File glowrootJarFile)
            throws Exception {

        deadlockedActiveWeavingRunnable =
                new DeadlockedActiveWeavingRunnable(engineModule.getWeaver());
        deadlockedActiveWeavingRunnable.scheduleWithFixedDelay(backgroundExecutor, 5, 5, SECONDS);

        // complete initialization of xyzzy-annotation-api, xyzzy-instrumentation-api and
        // xyzzy-engine services
        OptionalService<ThreadAllocatedBytes> threadAllocatedBytes = ThreadAllocatedBytes.create();
        transactionRegistry.setThreadAllocatedBytes(threadAllocatedBytes.getService());
        traceCollector = new TraceCollector(configService, collector, clock, ticker);
        transactionProcessor = new TransactionProcessor(collector, traceCollector, configService,
                ROLLUP_0_INTERVAL_MILLIS, clock);
        transactionRegistry.setTransactionProcessor(transactionProcessor);

        File[] roots = File.listRoots();
        LazyPlatformMBeanServer lazyPlatformMBeanServer = engineModule.getLazyPlatformMBeanServer();
        if (roots != null) {
            for (File root : roots) {
                String name = root.getCanonicalPath();
                if (name.length() > 1 && (name.endsWith("/") || name.endsWith("\\"))) {
                    name = name.substring(0, name.length() - 1);
                }
                name = name.replaceAll(":", "");
                lazyPlatformMBeanServer.lazyRegisterMBean(new FileSystem(root),
                        "org.glowroot:type=FileSystem,name=" + name);
            }
        }
        gaugeCollector = new GaugeCollector(configService, collector, lazyPlatformMBeanServer,
                instrumentation, clock, ticker);
        // using fixed rate to keep gauge collections close to on the second mark
        long gaugeCollectionIntervalMillis = configService.getGaugeCollectionIntervalMillis();
        gaugeCollector.scheduleWithFixedDelay(gaugeCollectionIntervalMillis, MILLISECONDS);
        stackTraceCollector = new StackTraceCollector(transactionRegistry, configService, random);

        immedateTraceStoreWatcher = new ImmediateTraceStoreWatcher(backgroundExecutor,
                transactionRegistry, traceCollector, configService, ticker);
        immedateTraceStoreWatcher.scheduleWithFixedDelay(backgroundExecutor,
                ImmediateTraceStoreWatcher.PERIOD_MILLIS, ImmediateTraceStoreWatcher.PERIOD_MILLIS,
                MILLISECONDS);

        liveTraceRepository = new LiveTraceRepositoryImpl(transactionRegistry, traceCollector,
                clock, ticker);
        liveAggregateRepository = new LiveAggregateRepositoryImpl(transactionProcessor);
        liveWeavingService = new LiveWeavingServiceImpl(engineModule.getAnalyzedWorld(),
                instrumentation, configService, engineModule.getAdviceCache(),
                jvmRetransformClassesSupported);
        liveJvmService = new LiveJvmServiceImpl(lazyPlatformMBeanServer, transactionRegistry,
                traceCollector, threadAllocatedBytes.getAvailability(), configService,
                glowrootJarFile, clock);

        writeSomeSuperTypesCacheToFileRunnable = new WriteSomeSuperTypesCacheToFileRunnable(
                engineModule.getPreloadSomeSuperTypesCache());
        writeSomeSuperTypesCacheToFileRunnable.scheduleWithFixedDelay(backgroundExecutor, 5, 5,
                SECONDS);
    }

    public ConfigService getConfigService() {
        return configService;
    }

    public LazyPlatformMBeanServer getLazyPlatformMBeanServer() {
        return engineModule.getLazyPlatformMBeanServer();
    }

    public LiveTraceRepositoryImpl getLiveTraceRepository() {
        if (liveTraceRepository == null) {
            throw new IllegalStateException("onEnteringMain() was never called");
        }
        return liveTraceRepository;
    }

    public LiveAggregateRepositoryImpl getLiveAggregateRepository() {
        if (liveAggregateRepository == null) {
            throw new IllegalStateException("onEnteringMain() was never called");
        }
        return liveAggregateRepository;
    }

    public LiveWeavingServiceImpl getLiveWeavingService() {
        if (liveWeavingService == null) {
            throw new IllegalStateException("onEnteringMain() was never called");
        }
        return liveWeavingService;
    }

    public LiveJvmServiceImpl getLiveJvmService() {
        if (liveJvmService == null) {
            throw new IllegalStateException("onEnteringMain() was never called");
        }
        return liveJvmService;
    }

    @OnlyUsedByTests
    public void close() throws Exception {
        if (immedateTraceStoreWatcher != null) {
            immedateTraceStoreWatcher.cancel();
        }
        if (stackTraceCollector != null) {
            stackTraceCollector.close();
        }
        if (gaugeCollector != null) {
            gaugeCollector.close();
        }
        LazyPlatformMBeanServer lazyPlatformMBeanServer = engineModule.getLazyPlatformMBeanServer();
        if (lazyPlatformMBeanServer != null) {
            lazyPlatformMBeanServer.unregisterMBeansThatWereRegistered();
        }
        if (traceCollector != null) {
            traceCollector.close();
        }
        if (transactionProcessor != null) {
            transactionProcessor.close();
        }
        if (deadlockedActiveWeavingRunnable != null) {
            deadlockedActiveWeavingRunnable.cancel();
        }
        if (writeSomeSuperTypesCacheToFileRunnable != null) {
            writeSomeSuperTypesCacheToFileRunnable.cancel();
        }
    }

    private static class DeadlockedActiveWeavingRunnable extends ScheduledRunnable {

        private final Weaver weaver;

        private DeadlockedActiveWeavingRunnable(Weaver weaver) {
            this.weaver = weaver;
        }

        @Override
        public void runInternal() {
            if (weaver.checkForDeadlockedActiveWeaving()) {
                // no need to keep checking for (and logging) deadlocked active weaving
                throw new TerminateSubsequentExecutionsException();
            }
        }
    }

    private static class WriteSomeSuperTypesCacheToFileRunnable extends ScheduledRunnable {

        private final PreloadSomeSuperTypesCache preloadSomeSuperTypesCache;

        private WriteSomeSuperTypesCacheToFileRunnable(
                PreloadSomeSuperTypesCache preloadSomeSuperTypesCache) {
            this.preloadSomeSuperTypesCache = preloadSomeSuperTypesCache;
        }

        @Override
        public void runInternal() {
            preloadSomeSuperTypesCache.writeToFileAsync();
        }
    }
}
