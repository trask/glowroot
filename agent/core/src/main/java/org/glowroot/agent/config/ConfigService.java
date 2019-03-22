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
package org.glowroot.agent.config;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.glowroot.common.config.AdvancedConfig;
import org.glowroot.common.config.AlertConfig;
import org.glowroot.common.config.CustomInstrumentationConfig;
import org.glowroot.common.config.CustomInstrumentationConfig.AlreadyInTransactionBehavior;
import org.glowroot.common.config.CustomInstrumentationConfig.CaptureKind;
import org.glowroot.common.config.CustomInstrumentationConfig.MethodModifier;
import org.glowroot.common.config.GaugeConfig;
import org.glowroot.common.config.ImmutableAdvancedConfig;
import org.glowroot.common.config.ImmutableAlertConfig;
import org.glowroot.common.config.ImmutableCustomInstrumentationConfig;
import org.glowroot.common.config.ImmutableGaugeConfig;
import org.glowroot.common.config.ImmutableJvmConfig;
import org.glowroot.common.config.ImmutableMBeanAttribute;
import org.glowroot.common.config.ImmutableSyntheticMonitorConfig;
import org.glowroot.common.config.ImmutableTransactionConfig;
import org.glowroot.common.config.ImmutableUiDefaultsConfig;
import org.glowroot.common.config.JvmConfig;
import org.glowroot.common.config.PropertyValue;
import org.glowroot.common.config.SyntheticMonitorConfig;
import org.glowroot.common.config.TransactionConfig;
import org.glowroot.common.config.UiDefaultsConfig;
import org.glowroot.common.util.OnlyUsedByTests;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig;
import org.glowroot.xyzzy.engine.config.AdviceConfig;
import org.glowroot.xyzzy.engine.config.ImmutableAdviceConfig;
import org.glowroot.xyzzy.engine.config.InstrumentationDescriptor;
import org.glowroot.xyzzy.engine.config.PropertyDescriptor;
import org.glowroot.xyzzy.engine.config.DefaultValue.PropertyType;
import org.glowroot.xyzzy.engine.util.JavaVersion;
import org.glowroot.xyzzy.instrumentation.api.config.ConfigListener;

public class ConfigService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);

    private static final long GAUGE_COLLECTION_INTERVAL_MILLIS =
            Long.getLong("glowroot.internal.gaugeCollectionIntervalMillis", 5000);

    private final ConfigFile configFile;

    private final ImmutableList<InstrumentationDescriptor> instrumentationDescriptors;

    private final Set<ConfigListener> configListeners = Sets.newCopyOnWriteArraySet();
    private final Set<ConfigListener> instrumentationConfigListeners =
            Sets.newCopyOnWriteArraySet();

    private volatile TransactionConfig transactionConfig;
    private volatile JvmConfig jvmConfig;
    private volatile UiDefaultsConfig uiDefaultsConfig;
    private volatile AdvancedConfig advancedConfig;
    private volatile ImmutableList<GaugeConfig> gaugeConfigs;
    private volatile ImmutableList<SyntheticMonitorConfig> syntheticMonitorConfigs;
    private volatile ImmutableList<AlertConfig> alertConfigs;
    private volatile ImmutableList<InstrumentationConfig> instrumentationConfigs;
    private volatile ImmutableList<CustomInstrumentationConfig> customInstrumentationConfigs;

    // memory barrier is used to ensure memory visibility of config values
    private volatile boolean memoryBarrier;

    public static ConfigService create(List<File> confDirs, boolean configReadOnly,
            List<InstrumentationDescriptor> instrumentationDescriptors) {
        ConfigService configService =
                new ConfigService(confDirs, configReadOnly, instrumentationDescriptors);
        // it's nice to update config.json on startup if it is missing some/all config properties so
        // that the file contents can be reviewed/updated/copied if desired
        try {
            configService.writeAll();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return configService;
    }

    private ConfigService(List<File> confDirs, boolean configReadOnly,
            List<InstrumentationDescriptor> instrumentationDescriptors) {
        configFile = new ConfigFile(confDirs, configReadOnly);
        this.instrumentationDescriptors = ImmutableList.copyOf(instrumentationDescriptors);
        TransactionConfig transactionConfig =
                configFile.getConfig("transactions", ImmutableTransactionConfig.class);
        if (transactionConfig == null) {
            this.transactionConfig = ImmutableTransactionConfig.builder().build();
        } else {
            this.transactionConfig = transactionConfig;
        }
        JvmConfig jvmConfig = configFile.getConfig("jvm", ImmutableJvmConfig.class);
        if (jvmConfig == null) {
            this.jvmConfig = ImmutableJvmConfig.builder().build();
        } else {
            this.jvmConfig = jvmConfig;
        }
        UiDefaultsConfig uiDefaultsConfig =
                configFile.getConfig("uiDefaults", ImmutableUiDefaultsConfig.class);
        if (uiDefaultsConfig == null) {
            this.uiDefaultsConfig = ImmutableUiDefaultsConfig.builder().build();
        } else {
            this.uiDefaultsConfig = uiDefaultsConfig;
        }
        AdvancedConfig advancedConfig =
                configFile.getConfig("advanced", ImmutableAdvancedConfig.class);
        if (advancedConfig == null) {
            this.advancedConfig = ImmutableAdvancedConfig.builder().build();
        } else {
            this.advancedConfig = advancedConfig;
        }
        List<ImmutableGaugeConfig> gaugeConfigs =
                configFile.getConfig("gauges", new TypeReference<List<ImmutableGaugeConfig>>() {});
        if (gaugeConfigs == null) {
            this.gaugeConfigs = getDefaultGaugeConfigs();
        } else {
            this.gaugeConfigs = ImmutableList.<GaugeConfig>copyOf(gaugeConfigs);
        }
        List<ImmutableSyntheticMonitorConfig> syntheticMonitorConfigs = configFile.getConfig(
                "syntheticMonitors", new TypeReference<List<ImmutableSyntheticMonitorConfig>>() {});
        if (syntheticMonitorConfigs == null) {
            this.syntheticMonitorConfigs = ImmutableList.of();
        } else {
            this.syntheticMonitorConfigs =
                    ImmutableList.<SyntheticMonitorConfig>copyOf(syntheticMonitorConfigs);
        }
        List<ImmutableAlertConfig> alertConfigs =
                configFile.getConfig("alerts", new TypeReference<List<ImmutableAlertConfig>>() {});
        if (alertConfigs == null) {
            this.alertConfigs = ImmutableList.of();
        } else {
            this.alertConfigs = ImmutableList.<AlertConfig>copyOf(alertConfigs);
        }
        List<ImmutableInstrumentationConfigTemp> instrumentationConfigs =
                configFile.getConfig("instrumentation",
                        new TypeReference<List<ImmutableInstrumentationConfigTemp>>() {});
        this.instrumentationConfigs =
                fixInstrumentationConfigs(instrumentationConfigs, instrumentationDescriptors);

        List<ImmutableCustomInstrumentationConfig> customInstrumentationConfigs =
                configFile.getConfig("customInstrumentation",
                        new TypeReference<List<ImmutableCustomInstrumentationConfig>>() {});
        if (customInstrumentationConfigs == null) {
            this.customInstrumentationConfigs = ImmutableList.of();
        } else {
            this.customInstrumentationConfigs =
                    ImmutableList.<CustomInstrumentationConfig>copyOf(customInstrumentationConfigs);
        }
        for (CustomInstrumentationConfig config : this.customInstrumentationConfigs) {
            config.logValidationErrorsIfAny();
        }
    }

    public TransactionConfig getTransactionConfig() {
        return transactionConfig;
    }

    public JvmConfig getJvmConfig() {
        return jvmConfig;
    }

    public UiDefaultsConfig getUiDefaultsConfig() {
        return uiDefaultsConfig;
    }

    public AdvancedConfig getAdvancedConfig() {
        return advancedConfig;
    }

    public List<GaugeConfig> getGaugeConfigs() {
        return gaugeConfigs;
    }

    public ImmutableList<AlertConfig> getAlertConfigs() {
        return alertConfigs;
    }

    public ImmutableList<InstrumentationConfig> getInstrumentationConfigs() {
        return instrumentationConfigs;
    }

    public @Nullable InstrumentationConfig getInstrumentationConfig(String instrumentationId) {
        for (InstrumentationConfig config : instrumentationConfigs) {
            if (instrumentationId.equals(config.id())) {
                return config;
            }
        }
        return null;
    }

    public List<CustomInstrumentationConfig> getCustomInstrumentationConfigs() {
        return customInstrumentationConfigs;
    }

    public List<AdviceConfig> getAdviceConfigs() {
        List<AdviceConfig> adviceConfigs = Lists.newArrayList();
        for (CustomInstrumentationConfig config : customInstrumentationConfigs) {
            adviceConfigs.add(toAdviceConfig(config));
        }
        return adviceConfigs;
    }

    public long getGaugeCollectionIntervalMillis() {
        return GAUGE_COLLECTION_INTERVAL_MILLIS;
    }

    public AgentConfig getAgentConfig() {
        AgentConfig.Builder builder = AgentConfig.newBuilder()
                .setTransactionConfig(transactionConfig.toProto());
        for (GaugeConfig config : gaugeConfigs) {
            builder.addGaugeConfig(config.toProto());
        }
        builder.setJvmConfig(jvmConfig.toProto());
        for (SyntheticMonitorConfig config : syntheticMonitorConfigs) {
            builder.addSyntheticMonitorConfig(config.toProto());
        }
        for (AlertConfig config : alertConfigs) {
            builder.addAlertConfig(config.toProto());
        }
        builder.setUiDefaultsConfig(uiDefaultsConfig.toProto());
        for (InstrumentationConfig config : instrumentationConfigs) {
            builder.addInstrumentationConfig(config.toProto());
        }
        for (CustomInstrumentationConfig config : customInstrumentationConfigs) {
            builder.addCustomInstrumentationConfig(config.toProto());
        }
        builder.setAdvancedConfig(advancedConfig.toProto());
        return builder.build();
    }

    public void addConfigListener(ConfigListener listener) {
        configListeners.add(listener);
        listener.onChange();
    }

    public void addInstrumentationConfigListener(ConfigListener listener) {
        instrumentationConfigListeners.add(listener);
    }

    public void updateTransactionConfig(TransactionConfig config) throws IOException {
        configFile.writeConfig("transactions", config);
        transactionConfig = config;
        notifyConfigListeners();
    }

    public void updateGaugeConfigs(List<GaugeConfig> configs) throws IOException {
        configFile.writeConfig("gauges", configs);
        gaugeConfigs = ImmutableList.copyOf(configs);
        notifyConfigListeners();
    }

    public void updateJvmConfig(JvmConfig config) throws IOException {
        configFile.writeConfig("jvm", config);
        jvmConfig = config;
        notifyConfigListeners();
    }

    public void updateSyntheticMonitorConfigs(List<SyntheticMonitorConfig> configs)
            throws IOException {
        configFile.writeConfig("syntheticMonitors", configs);
        syntheticMonitorConfigs = ImmutableList.copyOf(configs);
        notifyConfigListeners();
    }

    public void updateAlertConfigs(List<AlertConfig> configs) throws IOException {
        configFile.writeConfig("alerts", configs);
        alertConfigs = ImmutableList.copyOf(configs);
        notifyConfigListeners();
    }

    public void updateUiDefaultsConfig(UiDefaultsConfig config) throws IOException {
        configFile.writeConfig("uiDefaults", config);
        uiDefaultsConfig = config;
        notifyConfigListeners();
    }

    public void updateInstrumentationConfigs(List<InstrumentationConfig> configs)
            throws IOException {
        // configs passed in are already sorted
        configFile.writeConfig("instrumentation", stripEmptyInstrumentationConfigs(configs));
        instrumentationConfigs = ImmutableList.copyOf(configs);
        notifyAllInstrumentationConfigListeners();
    }

    public void updateCustomInstrumentationConfigs(List<CustomInstrumentationConfig> configs)
            throws IOException {
        configFile.writeConfig("instrumentation", configs);
        customInstrumentationConfigs = ImmutableList.copyOf(configs);
        notifyConfigListeners();
    }

    public void updateAdvancedConfig(AdvancedConfig config) throws IOException {
        configFile.writeConfig("advanced", config);
        advancedConfig = config;
        notifyConfigListeners();
    }

    public void updateAllConfig(AllConfig config) throws IOException {
        Map<String, Object> configs = Maps.newHashMap();
        configs.put("transactions", config.transaction());
        configs.put("jvm", config.jvm());
        configs.put("uiDefaults", config.uiDefaults());
        configs.put("advanced", config.advanced());
        configs.put("gauges", config.gauges());
        configs.put("syntheticMonitors", config.syntheticMonitors());
        configs.put("alerts", config.alerts());
        configs.put("instrumentation", stripEmptyInstrumentationConfigs(config.instrumentation()));
        configs.put("customInstrumentation", config.customInstrumentation());
        configFile.writeAllConfigs(configs);
        this.transactionConfig = config.transaction();
        this.jvmConfig = config.jvm();
        this.uiDefaultsConfig = config.uiDefaults();
        this.advancedConfig = config.advanced();
        this.gaugeConfigs = ImmutableList.copyOf(config.gauges());
        this.syntheticMonitorConfigs = ImmutableList.copyOf(config.syntheticMonitors());
        this.alertConfigs = ImmutableList.copyOf(config.alerts());
        this.instrumentationConfigs = ImmutableList.copyOf(config.instrumentation());
        this.customInstrumentationConfigs = ImmutableList.copyOf(config.customInstrumentation());
        notifyConfigListeners();
        notifyAllInstrumentationConfigListeners();
    }

    public boolean readMemoryBarrier() {
        return memoryBarrier;
    }

    public void writeMemoryBarrier() {
        memoryBarrier = true;
    }

    // the updated config is not passed to the listeners to avoid the race condition of multiple
    // config updates being sent out of order, instead listeners must call get*Config() which will
    // never return the updates out of order (at worst it may return the most recent update twice
    // which is ok)
    private void notifyConfigListeners() {
        for (ConfigListener configListener : configListeners) {
            configListener.onChange();
        }
    }

    private void notifyAllInstrumentationConfigListeners() {
        for (ConfigListener listener : instrumentationConfigListeners) {
            listener.onChange();
        }
        writeMemoryBarrier();
    }

    @OnlyUsedByTests
    public void initConfigForTests() throws IOException {
        transactionConfig = ImmutableTransactionConfig.copyOf(transactionConfig)
                .withSlowThresholdMillis(0);
        writeAll();
        notifyConfigListeners();
    }

    @OnlyUsedByTests
    public void resetConfigForTests() throws IOException {
        transactionConfig = ImmutableTransactionConfig.builder()
                .slowThresholdMillis(0) // default for tests
                .build();
        jvmConfig = ImmutableJvmConfig.builder().build();
        uiDefaultsConfig = ImmutableUiDefaultsConfig.builder().build();
        advancedConfig = ImmutableAdvancedConfig.builder().build();
        gaugeConfigs = getDefaultGaugeConfigs();
        syntheticMonitorConfigs = ImmutableList.of();
        alertConfigs = ImmutableList.of();
        instrumentationConfigs =
                fixInstrumentationConfigs(ImmutableList.<ImmutableInstrumentationConfigTemp>of(),
                        instrumentationDescriptors);
        customInstrumentationConfigs = ImmutableList.of();
        writeAll();
        notifyConfigListeners();
        notifyAllInstrumentationConfigListeners();
    }

    private void writeAll() throws IOException {
        Map<String, Object> configs = Maps.newHashMap();
        configs.put("transactions", transactionConfig);
        configs.put("jvm", jvmConfig);
        configs.put("uiDefaults", uiDefaultsConfig);
        configs.put("advanced", advancedConfig);
        configs.put("gauges", gaugeConfigs);
        configs.put("syntheticMonitors", syntheticMonitorConfigs);
        configs.put("alerts", alertConfigs);
        configs.put("instrumentation", stripEmptyInstrumentationConfigs(instrumentationConfigs));
        configs.put("customInstrumentation", customInstrumentationConfigs);
        configFile.writeAllConfigsOnStartup(configs);
    }

    private static List<InstrumentationConfig> stripEmptyInstrumentationConfigs(
            List<InstrumentationConfig> configs) {
        List<InstrumentationConfig> nonEmptyConfigs = Lists.newArrayList();
        for (InstrumentationConfig config : configs) {
            if (!config.properties().isEmpty()) {
                nonEmptyConfigs.add(config);
            }
        }
        return nonEmptyConfigs;
    }

    private static ImmutableList<GaugeConfig> getDefaultGaugeConfigs() {
        List<GaugeConfig> defaultGaugeConfigs = Lists.newArrayList();
        defaultGaugeConfigs.add(ImmutableGaugeConfig.builder()
                .mbeanObjectName("java.lang:type=Memory")
                .addMbeanAttributes(ImmutableMBeanAttribute.of("HeapMemoryUsage.used", false))
                .build());
        defaultGaugeConfigs.add(ImmutableGaugeConfig.builder()
                .mbeanObjectName("java.lang:type=GarbageCollector,name=*")
                .addMbeanAttributes(ImmutableMBeanAttribute.of("CollectionCount", true))
                .addMbeanAttributes(ImmutableMBeanAttribute.of("CollectionTime", true))
                .build());
        defaultGaugeConfigs.add(ImmutableGaugeConfig.builder()
                .mbeanObjectName("java.lang:type=MemoryPool,name=*")
                .addMbeanAttributes(ImmutableMBeanAttribute.of("Usage.used", false))
                .build());
        ImmutableGaugeConfig.Builder operatingSystemMBean = ImmutableGaugeConfig.builder()
                .mbeanObjectName("java.lang:type=OperatingSystem")
                .addMbeanAttributes(ImmutableMBeanAttribute.of("FreePhysicalMemorySize", false));
        if (!JavaVersion.isJava6()) {
            // these are only available since 1.7
            operatingSystemMBean
                    .addMbeanAttributes(ImmutableMBeanAttribute.of("ProcessCpuLoad", false));
            operatingSystemMBean
                    .addMbeanAttributes(ImmutableMBeanAttribute.of("SystemCpuLoad", false));
        }
        defaultGaugeConfigs.add(operatingSystemMBean.build());
        return ImmutableList.copyOf(defaultGaugeConfigs);
    }

    public static ImmutableList<InstrumentationConfig> fixInstrumentationConfigs(
            @Nullable List<ImmutableInstrumentationConfigTemp> fileConfigs,
            List<InstrumentationDescriptor> descriptors) {

        Map<String, InstrumentationConfigTemp> fileConfigMap = Maps.newHashMap();
        if (fileConfigs != null) {
            for (InstrumentationConfigTemp configTemp : fileConfigs) {
                fileConfigMap.put(configTemp.id(), configTemp);
            }
        }
        List<InstrumentationConfig> fixedInstrumentationConfigs = Lists.newArrayList();
        for (InstrumentationDescriptor descriptor : descriptors) {
            InstrumentationConfigTemp fileConfig = fileConfigMap.get(descriptor.id());
            ImmutableInstrumentationConfig.Builder builder =
                    ImmutableInstrumentationConfig.builder()
                            .descriptor(descriptor);
            for (PropertyDescriptor propertyDescriptor : descriptor.properties()) {
                builder.putProperties(propertyDescriptor.name(),
                        getPropertyValue(fileConfig, propertyDescriptor));
            }
            fixedInstrumentationConfigs.add(builder.build());
        }
        return ImmutableList.copyOf(fixedInstrumentationConfigs);
    }

    private static PropertyValue getPropertyValue(@Nullable InstrumentationConfigTemp configTemp,
            PropertyDescriptor propertyDescriptor) {
        if (configTemp == null) {
            return InstrumentationConfig
                    .toPropertyValue(propertyDescriptor.getValidatedNonNullDefaultValue());
        }
        PropertyValue propertyValue = getValidatedPropertyValue(configTemp.properties(),
                propertyDescriptor.name(), propertyDescriptor.type());
        if (propertyValue == null) {
            return InstrumentationConfig
                    .toPropertyValue(propertyDescriptor.getValidatedNonNullDefaultValue());
        }
        return propertyValue;
    }

    private static @Nullable PropertyValue getValidatedPropertyValue(
            Map<String, PropertyValue> properties, String propertyName, PropertyType propertyType) {
        PropertyValue propertyValue = properties.get(propertyName);
        if (propertyValue == null) {
            return null;
        }
        Object value = propertyValue.value();
        if (value == null) {
            return InstrumentationConfig
                    .toPropertyValue(PropertyDescriptor.getDefaultValue(propertyType));
        }
        if (PropertyDescriptor.isValidType(value, propertyType)) {
            return propertyValue;
        } else if (value instanceof String && propertyType == PropertyType.LIST) {
            // handle upgrading from comma-separated string properties to list properties
            return new PropertyValue(
                    Splitter.on(',').trimResults().omitEmptyStrings().splitToList((String) value));
        } else {
            logger.warn("invalid value for instrumentation property: {}", propertyName);
            return InstrumentationConfig
                    .toPropertyValue(PropertyDescriptor.getDefaultValue(propertyType));
        }
    }

    private static AdviceConfig toAdviceConfig(CustomInstrumentationConfig config) {
        @SuppressWarnings("deprecation")
        ImmutableAdviceConfig.Builder builder = ImmutableAdviceConfig.builder()
                .className(config.className())
                .classAnnotation(config.classAnnotation())
                .subTypeRestriction(config.subTypeRestriction())
                .superTypeRestriction(config.superTypeRestriction())
                .methodDeclaringClassName(config.methodDeclaringClassName())
                .methodName(config.methodName())
                .methodAnnotation(config.methodAnnotation())
                .addAllMethodParameterTypes(config.methodParameterTypes())
                .methodReturnType(config.methodReturnType());
        for (MethodModifier methodModifier : config.methodModifiers()) {
            builder.addMethodModifiers(toAdviceConfig(methodModifier));
        }
        return builder.nestingGroup(config.nestingGroup())
                .order(config.order())
                .captureKind(toAdviceConfig(config.captureKind()))
                .transactionType(config.transactionType())
                .transactionNameTemplate(config.transactionNameTemplate())
                .transactionUserTemplate(config.transactionUserTemplate())
                .putAllTransactionAttributeTemplates(config.transactionAttributeTemplates())
                .transactionSlowThresholdMillis(config.transactionSlowThresholdMillis())
                .alreadyInTransactionBehavior(toAdviceConfig(config.alreadyInTransactionBehavior()))
                .transactionOuter(config.transactionOuter())
                .traceEntryMessageTemplate(config.traceEntryMessageTemplate())
                .traceEntryStackThresholdMillis(config.traceEntryStackThresholdMillis())
                .traceEntryCaptureSelfNested(config.traceEntryCaptureSelfNested())
                .timerName(config.timerName())
                .build();
    }

    private static AdviceConfig.MethodModifier toAdviceConfig(MethodModifier methodModifier) {
        return AdviceConfig.MethodModifier.valueOf(methodModifier.name());
    }

    private static AdviceConfig.CaptureKind toAdviceConfig(CaptureKind captureKind) {
        return AdviceConfig.CaptureKind.valueOf(captureKind.name());
    }

    private static AdviceConfig. /*@Nullable*/ AlreadyInTransactionBehavior toAdviceConfig(
            @Nullable AlreadyInTransactionBehavior alreadyInTransactionBehavior) {
        if (alreadyInTransactionBehavior == null) {
            return null;
        } else {
            return AdviceConfig.AlreadyInTransactionBehavior
                    .valueOf(alreadyInTransactionBehavior.name());
        }
    }

    @Value.Immutable
    interface InstrumentationConfigTemp {
        String id();
        Map<String, PropertyValue> properties();
    }
}
