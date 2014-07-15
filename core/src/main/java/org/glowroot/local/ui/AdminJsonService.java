/*
 * Copyright 2012-2014 the original author or authors.
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

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.glowroot.advicegen.MessageTemplate;
import org.glowroot.advicegen.MessageTemplate.ValuePathPart;
import org.glowroot.collector.TransactionCollectorImpl;
import org.glowroot.common.ObjectMappers;
import org.glowroot.config.CapturePoint;
import org.glowroot.config.ConfigService;
import org.glowroot.local.store.AggregateDao;
import org.glowroot.local.store.DataSource;
import org.glowroot.local.store.GaugePointDao;
import org.glowroot.local.store.TraceDao;
import org.glowroot.markers.OnlyUsedByTests;
import org.glowroot.markers.Singleton;
import org.glowroot.transaction.AdviceCache;
import org.glowroot.transaction.TransactionRegistry;
import org.glowroot.weaving.AnalyzedWorld;

/**
 * Json service for various admin tasks.
 * 
 * @author Trask Stalnaker
 * @since 0.5
 */
@Singleton
@JsonService
class AdminJsonService {

    private static final Logger logger = LoggerFactory.getLogger(AdminJsonService.class);
    private static final ObjectMapper mapper = ObjectMappers.create();

    private final AggregateDao aggregateDao;
    private final TraceDao traceDao;
    private final GaugePointDao gaugePointDao;
    private final ConfigService configService;
    private final AdviceCache adviceCache;
    private final AnalyzedWorld analyzedWorld;
    @Nullable
    private final Instrumentation instrumentation;
    private final TransactionCollectorImpl transactionCollector;
    private final DataSource dataSource;
    private final TransactionRegistry transactionRegistry;

    AdminJsonService(AggregateDao aggregateDao, TraceDao traceDao, GaugePointDao gaugePointDao,
            ConfigService configService, AdviceCache adviceCache, AnalyzedWorld analyzedWorld,
            @Nullable Instrumentation instrumentation,
            TransactionCollectorImpl transactionCollector, DataSource dataSource,
            TransactionRegistry transactionRegistry) {
        this.aggregateDao = aggregateDao;
        this.traceDao = traceDao;
        this.gaugePointDao = gaugePointDao;
        this.configService = configService;
        this.adviceCache = adviceCache;
        this.analyzedWorld = analyzedWorld;
        this.instrumentation = instrumentation;
        this.transactionCollector = transactionCollector;
        this.dataSource = dataSource;
        this.transactionRegistry = transactionRegistry;
    }

    @POST("/backend/admin/delete-all-aggregates")
    void deleteAllAggregates() {
        logger.debug("deleteAllAggregates()");
        aggregateDao.deleteAll();
    }

    @POST("/backend/admin/delete-all-traces")
    void deleteAllTraces() {
        logger.debug("deleteAllTraces()");
        traceDao.deleteAll();
        gaugePointDao.deleteAll();
    }

    @POST("/backend/admin/reweave-capture-points")
    String reweaveCapturePoints() throws IOException, UnmodifiableClassException {
        if (instrumentation == null) {
            logger.warn("retransformClasses does not work under IsolatedWeavingClassLoader");
            return "{}";
        }
        if (!instrumentation.isRetransformClassesSupported()) {
            logger.warn("retransformClasses is not supported");
            return "{}";
        }
        ImmutableList<CapturePoint> capturePoints = configService.getCapturePoints();
        adviceCache.updateAdvisors(capturePoints, false);
        Set<String> classNames = Sets.newHashSet();
        for (CapturePoint capturePoint : capturePoints) {
            classNames.add(capturePoint.getClassName());
        }
        Set<Class<?>> classes = Sets.newHashSet();
        List<Class<?>> possibleNewReweavableClasses = getExistingSubClasses(classNames);
        // need to remove these classes from AnalyzedWorld, otherwise if a subclass and its parent
        // class are both in the list and the subclass is re-transformed first, it will use the
        // old cached AnalyzedClass for its parent which will have the old AnalyzedMethod advisors
        List<Class<?>> existingReweavableClasses =
                analyzedWorld.getClassesWithReweavableAdvice(true);
        analyzedWorld.removeClasses(possibleNewReweavableClasses);
        classes.addAll(existingReweavableClasses);
        classes.addAll(possibleNewReweavableClasses);
        if (classes.isEmpty()) {
            return "{\"classes\":0}";
        }
        instrumentation.retransformClasses(Iterables.toArray(classes, Class.class));
        List<Class<?>> updatedReweavableClasses =
                analyzedWorld.getClassesWithReweavableAdvice(false);
        // all existing reweavable classes were woven
        int count = existingReweavableClasses.size();
        // now add newly reweavable classes
        for (Class<?> possibleNewReweavableClass : possibleNewReweavableClasses) {
            if (updatedReweavableClasses.contains(possibleNewReweavableClass)
                    && !existingReweavableClasses.contains(possibleNewReweavableClass)) {
                count++;
            }
        }
        return "{\"classes\":" + count + "}";
    }

    @POST("/backend/admin/generate-java-pointcut")
    String generateJavaPointcut(String content) throws Exception {
        logger.debug("generateJavaPointcut(): content={}", content);
        CapturePoint pointcutConfig =
                ObjectMappers.readRequiredValue(mapper, content, CapturePoint.class);

        Class<?> typeClass = null;
        ImmutableList<ClassLoader> loaders = analyzedWorld.getClassLoaders();
        if (loaders.isEmpty()) {
            // this is needed for testing the UI outside of javaagent
            ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
            if (systemClassLoader == null) {
                loaders = ImmutableList.of();
            } else {
                loaders = ImmutableList.of(systemClassLoader);
            }
        }

        for (ClassLoader loader : loaders) {
            try {
                typeClass = Class.forName(pointcutConfig.getClassName(), false, loader);
                break;
            } catch (ClassNotFoundException e) {
            }
        }
        if (typeClass == null) {
            return "could not find type: " + pointcutConfig.getClassName();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("package your.package.name.here;\n");
        sb.append("\n");
        sb.append("import org.glowroot.api.*;\n");
        sb.append("import org.glowroot.api.weaving.*;\n");
        sb.append("\n");
        sb.append("public class YourAspectNameHere {\n");
        sb.append("\n");
        sb.append("    private static final PluginServices pluginServices =\n");
        sb.append("             PluginServices.get(\"your-plugin-id-here\");\n");
        sb.append("\n");
        String methodArgTypes = "";
        if (!pointcutConfig.getMethodParameterTypes().isEmpty()) {
            methodArgTypes = "\""
                    + Joiner.on("\", \"").join(pointcutConfig.getMethodParameterTypes())
                    + "\"";
        }
        sb.append("    @Pointcut(type = \"" + pointcutConfig.getClassName() + "\",\n");
        sb.append("              methodName = \"" + pointcutConfig.getMethodName() + "\",\n");
        sb.append("              methodArgTypes = {" + methodArgTypes + "},\n");
        sb.append("              metricName = \"" + pointcutConfig.getMetricName() + "\"");
        if (pointcutConfig.isTraceEntryOrGreater()
                && !pointcutConfig.isTraceEntryCaptureSelfNested()) {
            sb.append("\n");
            sb.append("              ignoreSelfNested = true");
        }
        sb.append(");\n");
        sb.append("    public static class YourAdviceNameHere {\n");
        sb.append("\n");
        sb.append("        private static final TraceMetricName traceMetricName =\n");
        sb.append("                pluginServices.getTraceMetricName(YourAdviceNameHere.class);\n");
        sb.append("\n");
        sb.append("        @IsEnabled\n");
        sb.append("        public static boolean isEnabled() {\n");
        sb.append("            return pluginServices.isEnabled();\n");
        sb.append("        }\n");
        sb.append("\n");
        if (pointcutConfig.isTraceEntryOrGreater()) {
            generateSpanMethods(pointcutConfig, sb);
        } else {
            generateMetricOnlyMethods(sb);
        }
        sb.append("    }\n");
        sb.append("}\n");
        if (pointcutConfig.isTraceEntryOrGreater()) {
            Class<?>[] parameterTypes = new Class<?>[pointcutConfig.getMethodParameterTypes().size()];
            for (int i = 0; i < pointcutConfig.getMethodParameterTypes().size(); i++) {
                parameterTypes[i] = Class.forName(pointcutConfig.getMethodParameterTypes().get(i),
                        false, typeClass.getClassLoader());
            }
            Method method =
                    typeClass.getDeclaredMethod(pointcutConfig.getMethodName(), parameterTypes);
            MessageTemplate template = MessageTemplate.create(
                    pointcutConfig.getTraceEntryTemplate(), typeClass, method.getReturnType(),
                    method.getParameterTypes());

            generateInvoker(template, sb);
        }
        return sb.toString();
    }

    // TODO re-write all this as builder, with append1, append2, append3 for different indentation
    // levels to keep lines short
    private void generateSpanMethods(CapturePoint pointcutConfig, StringBuilder sb) {
        sb.append("        @OnBefore\n");
        sb.append("        public static Span onBefore(@BindReceiver Object receiver");
        for (int i = 0; i < pointcutConfig.getMethodParameterTypes().size(); i++) {
            String methodArgType = pointcutConfig.getMethodParameterTypes().get(i);
            sb.append(",\n");
            sb.append("                @BindMethodArg ");
            if (methodArgType.startsWith("java.")) {
                sb.append(methodArgType);
            } else {
                sb.append("Object");
            }
            sb.append(" arg" + i);
        }
        sb.append(",\n");
        sb.append("                @BindClassMeta YourInvokerNameHere invoker");
        sb.append(") {\n");
        if (pointcutConfig.isTransaction()) {
            sb.append("            return pluginServices.startTransaction(\n");
            sb.append("                    \"" + pointcutConfig.getTransactionType() + "\",\n");
            sb.append("                    \"" + pointcutConfig.getTransactionNameTemplate()
                    + "\",\n");
        } else {
            sb.append("            return pluginServices.startSpan(\n");
        }
        sb.append("                    MessageSupplier.from(\""
                + pointcutConfig.getTraceEntryTemplate() + "\"),\n");
        sb.append("                    traceMetricName);\n");
        sb.append("        }\n");
        sb.append("\n");
        sb.append("        @OnThrow\n");
        sb.append("        public static void onThrow(@BindThrowable Throwable t, @BindTraveler Span span) {\n");
        sb.append("            span.endWithError(ErrorMessage.from(t));\n");
        sb.append("        }\n");
        sb.append("\n");
        sb.append("        @OnReturn\n");
        sb.append("        public static void onReturn(@BindTraveler Span span) {\n");
        Long traceEntryStackThresholdMillis = pointcutConfig.getTraceEntryStackThresholdMillis();
        if (traceEntryStackThresholdMillis == null) {
            sb.append("            span.end();\n");
        } else {
            sb.append("            span.endWithStackTrace(" + traceEntryStackThresholdMillis
                    + ", TimeUnit.MILLIS);\n");
        }
        sb.append("        }\n");
    }

    private void generateMetricOnlyMethods(StringBuilder sb) {
        sb.append("        @OnBefore\n");
        sb.append("        public static TraceMetricTimer onBefore() {\n");
        sb.append("            return pluginServices.startTraceMetric(traceMetricName);\n");
        sb.append("        }\n");
        sb.append("\n");
        sb.append("        @OnAfter\n");
        sb.append("        public static void onAfter(@BindTraveler TraceMetricTimer traceMetricTimer) {\n");
        sb.append("            traceMetricTimer.stop();\n");
        sb.append("        }\n");
    }

    private void generateInvoker(MessageTemplate template, StringBuilder sb) {
        sb.append("\n");
        sb.append("----------------------------------------");
        sb.append("----------------------------------------\n");
        sb.append("\n");
        sb.append("package your.package.name.here;\n");
        sb.append("\n");
        sb.append("import org.glowroot.api.*;\n");
        sb.append("import org.glowroot.api.weaving.*;\n");
        sb.append("\n");
        sb.append("public class YourInvokerNameHere {\n");
        sb.append("\n");
        sb.append("    private static final Logger logger =\n");
        sb.append("            LoggerFactory.getLogger(YourInvokerNameHere.class);\n");
        sb.append("\n");
        for (ValuePathPart valuePathPart : template.getThisPathParts()) {
            Method method = valuePathPart.getPathEvaluator().getAccessors()[0].getMethod();
            sb.append("    private final Method " + method.getName() + "Method;\n");
        }
        sb.append("\n");
        sb.append("    public YourInvokerNameHere(Class<?> wovenClass) {\n");
        for (ValuePathPart valuePathPart : template.getThisPathParts()) {
            Method method = valuePathPart.getPathEvaluator().getAccessors()[0].getMethod();
            sb.append("        " + method.getName() + "Method = getMethod(wovenClass, \""
                    + method.getName() + "\");\n");
        }
        sb.append("    }\n");
        sb.append("\n");
        for (ValuePathPart valuePathPart : template.getThisPathParts()) {
            Method method = valuePathPart.getPathEvaluator().getAccessors()[0].getMethod();
            String returnType = method.getReturnType().getName();
            if (!returnType.startsWith("java.")) {
                returnType = "Object";
            }
            sb.append("    public " + returnType + " " + method.getName() + "(Object obj) {\n");
            sb.append("        if (" + method.getName() + "Method == null) {\n");
            sb.append("            return null;\n");
            sb.append("        }\n");
            sb.append("        try {\n");
            sb.append("            return (" + returnType + ") " + method.getName()
                    + "Method.invoke(obj);\n");
            sb.append("        } catch (Throwable t) {\n");
            sb.append("            logger.warn(\"error calling "
                    + method.getDeclaringClass().getName() + "." + method.getName() + "()\", t);\n");
            sb.append("            return \"<" + method.getDeclaringClass().getName() + "."
                    + method.getName() + "()>\";\n");
            sb.append("        }\n");
            sb.append("    }\n");
            sb.append("\n");
        }
        sb.append("    private static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {\n");
        sb.append("        if (clazz == null) {\n");
        sb.append("            return null;\n");
        sb.append("        }\n");
        sb.append("        try {\n");
        sb.append("            return clazz.getMethod(methodName, parameterTypes);\n");
        sb.append("        } catch (SecurityException e) {\n");
        sb.append("            logger.warn(e.getMessage(), e);\n");
        sb.append("            return null;\n");
        sb.append("        } catch (NoSuchMethodException e) {\n");
        sb.append("            logger.warn(e.getMessage(), e);\n");
        sb.append("            return null;\n");
        sb.append("        }\n");
        sb.append("    }\n");
        sb.append("}\n");
    }

    @POST("/backend/admin/compact-data")
    void compactData() {
        logger.debug("compactData()");
        try {
            dataSource.compact();
        } catch (SQLException e) {
            // this might be serious, worth logging as error
            logger.error(e.getMessage(), e);
        }
    }

    @OnlyUsedByTests
    @POST("/backend/admin/reset-all-config")
    void resetAllConfig() throws IOException {
        logger.debug("resetAllConfig()");
        configService.resetAllConfig();
    }

    @OnlyUsedByTests
    @GET("/backend/admin/num-active-transactions")
    String getNumActiveTransactions() {
        logger.debug("getNumActiveTransactions()");
        return Integer.toString(transactionRegistry.getTransactions().size());
    }

    @OnlyUsedByTests
    @GET("/backend/admin/num-pending-complete-transactions")
    String getNumPendingCompleteTransactions() {
        logger.debug("getNumPendingCompleteTransactions()");
        return Integer.toString(transactionCollector.getPendingCompleteTransactions().size());
    }

    @OnlyUsedByTests
    @GET("/backend/admin/num-traces")
    String getNumTraces() {
        logger.debug("getNumTraces()");
        return Long.toString(traceDao.count());
    }

    @RequiresNonNull("instrumentation")
    private List<Class<?>> getExistingSubClasses(Set<String> classNames) {
        List<Class<?>> classes = Lists.newArrayList();
        for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
            if (isSubClassOfOneOf(clazz, classNames)) {
                classes.add(clazz);
            }
        }
        return classes;
    }

    private static boolean isSubClassOfOneOf(Class<?> clazz, Set<String> classNames) {
        if (classNames.contains(clazz.getName())) {
            return true;
        }
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null && isSubClassOfOneOf(superclass, classNames)) {
            return true;
        }
        for (Class<?> iface : clazz.getInterfaces()) {
            if (isSubClassOfOneOf(iface, classNames)) {
                return true;
            }
        }
        return false;
    }
}
