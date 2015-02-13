/*
 * Copyright 2011-2015 the original author or authors.
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
package org.glowroot.local.ui;

import java.lang.instrument.Instrumentation;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import org.glowroot.collector.CollectorModule;
import org.glowroot.collector.TransactionCollectorImpl;
import org.glowroot.common.Clock;
import org.glowroot.common.JavaVersion;
import org.glowroot.common.Ticker;
import org.glowroot.config.ConfigModule;
import org.glowroot.config.ConfigService;
import org.glowroot.jvm.JvmModule;
import org.glowroot.local.store.AggregateDao;
import org.glowroot.local.store.CappedDatabase;
import org.glowroot.local.store.DataSource;
import org.glowroot.local.store.GaugePointDao;
import org.glowroot.local.store.GcEventDao;
import org.glowroot.local.store.StorageModule;
import org.glowroot.local.store.TraceDao;
import org.glowroot.markers.OnlyUsedByTests;
import org.glowroot.transaction.TransactionModule;
import org.glowroot.transaction.TransactionRegistry;
import org.glowroot.weaving.AnalyzedWorld;

import static com.google.common.base.Preconditions.checkNotNull;

public class LocalUiModule {

    private final LazyHttpServer lazyHttpServer;

    public LocalUiModule(Ticker ticker, Clock clock, JvmModule jvmModule,
            ConfigModule configModule, StorageModule storageModule,
            CollectorModule collectorModule, TransactionModule transactionModule,
            @Nullable Instrumentation instrumentation, Map<String, String> properties,
            String version) throws InterruptedException {

        ConfigService configService = configModule.getConfigService();

        AggregateDao aggregateDao = storageModule.getAggregateDao();
        TraceDao traceDao = storageModule.getTraceDao();
        GaugePointDao gaugePointDao = storageModule.getGaugePointDao();
        GcEventDao gcEventDao = storageModule.getGcEventDao();
        DataSource dataSource = storageModule.getDataSource();
        CappedDatabase cappedDatabase = storageModule.getCappedDatabase();
        TransactionCollectorImpl transactionCollector = collectorModule.getTransactionCollector();
        AnalyzedWorld analyzedWorld = transactionModule.getAnalyzedWorld();

        TransactionRegistry transactionRegistry = transactionModule.getTransactionRegistry();

        LayoutJsonService layoutJsonService = new LayoutJsonService(version, configService,
                configModule.getPluginDescriptors(), jvmModule.getHeapDumps(),
                collectorModule.getFixedAggregateIntervalSeconds(),
                storageModule.getFixedAggregateRollupSeconds(),
                collectorModule.getFixedGaugeIntervalSeconds(),
                storageModule.getFixedGaugeRollupSeconds());
        HttpSessionManager httpSessionManager =
                new HttpSessionManager(configService, clock, layoutJsonService);
        IndexHtmlHttpService indexHtmlHttpService =
                new IndexHtmlHttpService(httpSessionManager, layoutJsonService);
        TransactionCommonService transactionCommonService = new TransactionCommonService(
                aggregateDao, collectorModule.getAggregateCollector(),
                storageModule.getFixedAggregateRollupSeconds());
        TraceCommonService traceCommonService = new TraceCommonService(traceDao,
                transactionRegistry, transactionCollector, clock, ticker);
        TransactionJsonService transactionJsonService = new TransactionJsonService(
                transactionCommonService, traceDao, transactionRegistry, transactionCollector,
                clock, collectorModule.getFixedAggregateIntervalSeconds(),
                storageModule.getFixedAggregateRollupSeconds());
        TracePointJsonService tracePointJsonService = new TracePointJsonService(traceDao,
                transactionRegistry, transactionCollector, ticker, clock);
        TraceJsonService traceJsonService = new TraceJsonService(traceCommonService);
        TraceDetailHttpService traceDetailHttpService =
                new TraceDetailHttpService(traceCommonService);
        TraceExportHttpService traceExportHttpService =
                new TraceExportHttpService(traceCommonService);
        ErrorCommonService errorCommonService = new ErrorCommonService(
                aggregateDao, collectorModule.getAggregateCollector(),
                storageModule.getFixedAggregateRollupSeconds());
        ErrorJsonService errorJsonService = new ErrorJsonService(errorCommonService,
                transactionCommonService, traceDao, clock,
                collectorModule.getFixedAggregateIntervalSeconds(),
                storageModule.getFixedAggregateRollupSeconds());
        JvmJsonService jvmJsonService = new JvmJsonService(jvmModule.getLazyPlatformMBeanServer(),
                gaugePointDao, gcEventDao, configService, jvmModule.getThreadAllocatedBytes(),
                jvmModule.getHeapDumps(), jvmModule.getGcEvents(), jvmModule.getProcessId(),
                collectorModule.getFixedGaugeIntervalSeconds(),
                storageModule.getFixedGaugeRollupSeconds());
        ConfigJsonService configJsonService = new ConfigJsonService(configService,
                cappedDatabase, configModule.getPluginDescriptors(), httpSessionManager,
                transactionModule);
        ClasspathCache classpathCache = new ClasspathCache(analyzedWorld, instrumentation);
        InstrumentationJsonService instrumentationJsonService = new InstrumentationJsonService(
                configService, transactionModule.getAdviceCache(), classpathCache,
                transactionModule);
        GaugeJsonService gaugeJsonService =
                new GaugeJsonService(configService, jvmModule.getLazyPlatformMBeanServer());
        AlertJsonService alertJsonService = new AlertJsonService(configService);
        AdminJsonService adminJsonService = new AdminJsonService(aggregateDao, traceDao,
                gaugePointDao, gcEventDao, collectorModule.getAggregateCollector(), configService,
                transactionModule.getAdviceCache(), analyzedWorld, instrumentation,
                transactionCollector, dataSource, transactionRegistry);

        List<Object> jsonServices = Lists.newArrayList();
        jsonServices.add(layoutJsonService);
        jsonServices.add(transactionJsonService);
        jsonServices.add(tracePointJsonService);
        jsonServices.add(traceJsonService);
        jsonServices.add(errorJsonService);
        jsonServices.add(jvmJsonService);
        jsonServices.add(configJsonService);
        jsonServices.add(instrumentationJsonService);
        jsonServices.add(gaugeJsonService);
        jsonServices.add(alertJsonService);
        jsonServices.add(adminJsonService);

        int port = configService.getUserInterfaceConfig().port();
        String bindAddress = getBindAddress(properties);
        lazyHttpServer = new LazyHttpServer(bindAddress, port, httpSessionManager,
                indexHtmlHttpService, layoutJsonService, traceDetailHttpService,
                traceExportHttpService, jsonServices);
        if (instrumentation == null || JavaVersion.isJdk6()) {
            lazyHttpServer.initNonLazy(configJsonService);
        } else {
            // this checkNotNull is safe because of above conditional
            checkNotNull(instrumentation);
            lazyHttpServer.init(instrumentation, configJsonService);
        }
    }

    public int getPort() throws InterruptedException {
        return getPort(lazyHttpServer.get());
    }

    public int getNonLazyPort() {
        return getPort(lazyHttpServer.getNonLazy());
    }

    @OnlyUsedByTests
    public void close() throws InterruptedException {
        HttpServer httpServer = lazyHttpServer.get();
        if (httpServer != null) {
            httpServer.close();
        }
    }

    private static int getPort(@Nullable HttpServer httpServer) {
        return httpServer == null ? -1 : httpServer.getPort();
    }

    private static String getBindAddress(Map<String, String> properties) {
        // empty check to support parameterized script, e.g. -Dglowroot.ui.bind.address=${somevar}
        String bindAddress = properties.get("ui.bind.address");
        if (Strings.isNullOrEmpty(bindAddress)) {
            return "0.0.0.0";
        } else {
            return bindAddress;
        }
    }
}
