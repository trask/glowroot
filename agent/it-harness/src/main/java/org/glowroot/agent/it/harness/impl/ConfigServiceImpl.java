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
package org.glowroot.agent.it.harness.impl;

import java.util.List;
import java.util.ListIterator;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.glowroot.agent.it.harness.ConfigService;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.AdvancedConfig;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.CustomInstrumentationConfig;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.GaugeConfig;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.InstrumentationConfig;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.InstrumentationProperty;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.InstrumentationProperty.StringList;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.MBeanAttribute;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.TransactionConfig;
import org.glowroot.wire.api.model.Proto.OptionalInt32;
import org.glowroot.xyzzy.engine.util.JavaVersion;

class ConfigServiceImpl implements ConfigService {

    private final GrpcServerWrapper server;
    private final boolean reweavable;

    ConfigServiceImpl(GrpcServerWrapper server, boolean reweavable) {
        this.server = server;
        this.reweavable = reweavable;
    }

    @Override
    public void updateTransactionConfig(TransactionConfig config) throws Exception {
        AgentConfig agentConfig = server.getAgentConfig();
        server.updateAgentConfig(agentConfig.toBuilder()
                .setTransactionConfig(config)
                .build());
    }

    @Override
    public void setInstrumentationProperty(String instrumentationId, String propertyName,
            boolean propertyValue) throws Exception {
        updateInstrumentationConfig(instrumentationId, propertyName,
                InstrumentationProperty.Value.newBuilder().setBval(propertyValue).build());
    }

    @Override
    public void setInstrumentationProperty(String instrumentationId, String propertyName,
            @Nullable Double propertyValue) throws Exception {
        if (propertyValue == null) {
            updateInstrumentationConfig(instrumentationId, propertyName,
                    InstrumentationProperty.Value.newBuilder().setDvalNull(true).build());
        } else {
            updateInstrumentationConfig(instrumentationId, propertyName,
                    InstrumentationProperty.Value.newBuilder().setDval(propertyValue).build());
        }
    }

    @Override
    public void setInstrumentationProperty(String instrumentationId, String propertyName,
            String propertyValue) throws Exception {
        updateInstrumentationConfig(instrumentationId, propertyName,
                InstrumentationProperty.Value.newBuilder().setSval(propertyValue).build());
    }

    @Override
    public void setInstrumentationProperty(String instrumentationId, String propertyName,
            List<String> propertyValue) throws Exception {
        updateInstrumentationConfig(instrumentationId, propertyName,
                InstrumentationProperty.Value.newBuilder()
                        .setLval(StringList.newBuilder().addAllVal(propertyValue)).build());
    }

    @Override
    public int updateCustomInstrumentationConfigs(List<CustomInstrumentationConfig> configs)
            throws Exception {
        AgentConfig agentConfig = server.getAgentConfig();
        server.updateAgentConfig(agentConfig.toBuilder()
                .clearCustomInstrumentationConfig()
                .addAllCustomInstrumentationConfig(configs)
                .build());
        if (reweavable) {
            return server.reweave();
        } else {
            return 0;
        }
    }

    @Override
    public void updateAdvancedConfig(AdvancedConfig config) throws Exception {
        AgentConfig agentConfig = server.getAgentConfig();
        server.updateAgentConfig(agentConfig.toBuilder()
                .setAdvancedConfig(config)
                .build());
    }

    void initConfigForTests() throws Exception {
        AgentConfig agentConfig = server.getAgentConfig();
        server.updateAgentConfig(agentConfig.toBuilder()
                .setTransactionConfig(agentConfig.getTransactionConfig().toBuilder()
                        .setSlowThresholdMillis(of(0)))
                .build());
    }

    void resetConfigForTests() throws Exception {
        AgentConfig.Builder agentConfigBuilder = AgentConfig.newBuilder()
                .setTransactionConfig(getDefaultTransactionConfigForTests())
                .setAdvancedConfig(getDefaultAdvancedConfigForTests())
                .addAllGaugeConfig(getDefaultGaugeConfigsForTests());
        for (InstrumentationConfig config : server.getAgentConfig()
                .getInstrumentationConfigList()) {
            InstrumentationConfig.Builder builder = InstrumentationConfig.newBuilder()
                    .setId(config.getId());
            for (InstrumentationProperty property : config.getPropertyList()) {
                builder.addProperty(property.toBuilder()
                        .setValue(property.getDefault()));
            }
            agentConfigBuilder.addInstrumentationConfig(builder.build());
        }
        server.updateAgentConfig(agentConfigBuilder.build());
    }

    private void updateInstrumentationConfig(String instrumentationId, String name,
            InstrumentationProperty.Value value)
            throws Exception {
        InstrumentationConfig config = getInstrumentationConfig(instrumentationId);
        List<InstrumentationProperty> properties = Lists.newArrayList(config.getPropertyList());
        ListIterator<InstrumentationProperty> i = properties.listIterator();
        boolean found = false;
        while (i.hasNext()) {
            InstrumentationProperty existingInstrumentationProperty = i.next();
            if (existingInstrumentationProperty.getName().equals(name)) {
                i.set(existingInstrumentationProperty.toBuilder()
                        .setValue(value)
                        .build());
                found = true;
                break;
            }
        }
        if (!found) {
            throw new IllegalStateException(
                    "Could not find instrumentation property with name: " + name);
        }
        updateInstrumentationConfig(config.toBuilder()
                .clearProperty()
                .addAllProperty(properties)
                .build());
    }

    private InstrumentationConfig getInstrumentationConfig(String instrumentationId)
            throws InterruptedException {
        AgentConfig agentConfig = server.getAgentConfig();
        for (InstrumentationConfig config : agentConfig.getInstrumentationConfigList()) {
            if (config.getId().equals(instrumentationId)) {
                return config;
            }
        }
        throw new IllegalStateException(
                "Could not find instrumentation with id: " + instrumentationId);
    }

    private void updateInstrumentationConfig(InstrumentationConfig config) throws Exception {
        AgentConfig agentConfig = server.getAgentConfig();
        List<InstrumentationConfig> InstrumentationConfigs =
                Lists.newArrayList(agentConfig.getInstrumentationConfigList());
        ListIterator<InstrumentationConfig> i = InstrumentationConfigs.listIterator();
        boolean found = false;
        while (i.hasNext()) {
            if (i.next().getId().equals(config.getId())) {
                i.set(config);
                found = true;
                break;
            }
        }
        if (!found) {
            throw new IllegalStateException(
                    "Could not find instrumentation with id: " + config.getId());
        }
        server.updateAgentConfig(agentConfig.toBuilder()
                .clearInstrumentationConfig()
                .addAllInstrumentationConfig(InstrumentationConfigs)
                .build());
    }

    private static TransactionConfig getDefaultTransactionConfigForTests() {
        // TODO this needs to be kept in sync with default values
        return TransactionConfig.newBuilder()
                .setProfilingIntervalMillis(of(1000))
                .setSlowThresholdMillis(of(0)) // default for tests
                .setCaptureThreadStats(true)
                .build();
    }

    private static AdvancedConfig getDefaultAdvancedConfigForTests() {
        // TODO this needs to be kept in sync with default values
        return AdvancedConfig.newBuilder()
                .setWeavingTimer(false)
                .setImmediatePartialStoreThresholdSeconds(of(60))
                .setMaxTransactionAggregates(of(500))
                .setMaxQueryAggregates(of(500))
                .setMaxServiceCallAggregates(of(500))
                .setMaxTraceEntriesPerTransaction(of(2000))
                .setMaxProfileSamplesPerTransaction(of(50000))
                .setMbeanGaugeNotFoundDelaySeconds(of(60))
                .build();
    }

    private static List<GaugeConfig> getDefaultGaugeConfigsForTests() {
        // TODO this needs to be kept in sync with default values
        List<GaugeConfig> defaultGaugeConfigs = Lists.newArrayList();
        defaultGaugeConfigs.add(GaugeConfig.newBuilder()
                .setMbeanObjectName("java.lang:type=Memory")
                .addMbeanAttribute(MBeanAttribute.newBuilder()
                        .setName("HeapMemoryUsage.used"))
                .build());
        defaultGaugeConfigs.add(GaugeConfig.newBuilder()
                .setMbeanObjectName("java.lang:type=GarbageCollector,name=*")
                .addMbeanAttribute(MBeanAttribute.newBuilder()
                        .setName("CollectionCount")
                        .setCounter(true))
                .addMbeanAttribute(MBeanAttribute.newBuilder()
                        .setName("CollectionTime")
                        .setCounter(true))
                .build());
        defaultGaugeConfigs.add(GaugeConfig.newBuilder()
                .setMbeanObjectName("java.lang:type=MemoryPool,name=*")
                .addMbeanAttribute(MBeanAttribute.newBuilder()
                        .setName("Usage.used"))
                .build());
        GaugeConfig.Builder operatingSystemMBean = GaugeConfig.newBuilder()
                .setMbeanObjectName("java.lang:type=OperatingSystem")
                .addMbeanAttribute(MBeanAttribute.newBuilder()
                        .setName("FreePhysicalMemorySize"));
        if (!JavaVersion.isJava6()) {
            // these are only available since 1.7
            operatingSystemMBean.addMbeanAttribute(MBeanAttribute.newBuilder()
                    .setName("ProcessCpuLoad"));
            operatingSystemMBean.addMbeanAttribute(MBeanAttribute.newBuilder()
                    .setName("SystemCpuLoad"));
        }
        defaultGaugeConfigs.add(operatingSystemMBean.build());
        return ImmutableList.copyOf(defaultGaugeConfigs);
    }

    private static OptionalInt32 of(int value) {
        return OptionalInt32.newBuilder().setValue(value).build();
    }
}
