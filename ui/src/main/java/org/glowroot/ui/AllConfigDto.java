/*
 * Copyright 2018-2019 the original author or authors.
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
package org.glowroot.ui;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.immutables.value.Value;

import org.glowroot.common.config.AlertConfig;
import org.glowroot.common.config.GaugeConfig;
import org.glowroot.common.config.ImmutableAlertConfig;
import org.glowroot.common.config.ImmutableGaugeConfig;
import org.glowroot.common.config.ImmutableInstrumentationConfig;
import org.glowroot.common.config.ImmutableJvmConfig;
import org.glowroot.common.config.ImmutableSyntheticMonitorConfig;
import org.glowroot.common.config.ImmutableTransactionConfig;
import org.glowroot.common.config.ImmutableUiDefaultsConfig;
import org.glowroot.common.config.InstrumentationConfig;
import org.glowroot.common.config.JvmConfig;
import org.glowroot.common.config.PropertyValue;
import org.glowroot.common.config.SyntheticMonitorConfig;
import org.glowroot.common.config.TransactionConfig;
import org.glowroot.common.config.UiDefaultsConfig;
import org.glowroot.common.util.Versions;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.PluginProperty;
import org.glowroot.wire.api.model.Proto.OptionalInt32;

@Value.Immutable
abstract class AllConfigDto {

    @Value.Default
    ImmutableTransactionConfig transactions() {
        return ImmutableTransactionConfig.builder().build();
    }

    @Value.Default
    ImmutableJvmConfig jvm() {
        return ImmutableJvmConfig.builder().build();
    }

    @Value.Default
    ImmutableUiDefaultsConfig uiDefaults() {
        return ImmutableUiDefaultsConfig.builder().build();
    }

    @Value.Default
    ImmutableAdvancedConfig advanced() {
        return ImmutableAdvancedConfig.builder().build();
    }

    @JsonInclude(Include.NON_EMPTY)
    abstract List<ImmutableGaugeConfig> gauges();

    @JsonInclude(Include.NON_EMPTY)
    abstract List<ImmutableSyntheticMonitorConfig> syntheticMonitors();

    @JsonInclude(Include.NON_EMPTY)
    abstract List<ImmutableAlertConfig> alerts();

    @JsonInclude(Include.NON_EMPTY)
    abstract List<ImmutablePluginConfig> plugins();

    @JsonInclude(Include.NON_EMPTY)
    abstract List<ImmutableInstrumentationConfig> instrumentation();

    abstract @Nullable String version();

    AgentConfig toProto(String agentVersion) {
        AgentConfig.Builder builder = AgentConfig.newBuilder()
                .setTransactionConfig(transactions().toProto())
                .setJvmConfig(jvm().toProto())
                .setUiDefaultsConfig(uiDefaults().toProto())
                .setAdvancedConfig(advanced().toProto(agentVersion));
        for (GaugeConfig gaugeConfig : gauges()) {
            builder.addGaugeConfig(gaugeConfig.toProto());
        }
        for (SyntheticMonitorConfig syntheticMonitorConfig : syntheticMonitors()) {
            builder.addSyntheticMonitorConfig(syntheticMonitorConfig.toProto());
        }
        for (AlertConfig alertConfig : alerts()) {
            builder.addAlertConfig(alertConfig.toProto());
        }
        for (PluginConfig pluginConfig : plugins()) {
            builder.addPluginConfig(pluginConfig.toProto());
        }
        for (InstrumentationConfig instrumentationConfig : instrumentation()) {
            builder.addInstrumentationConfig(instrumentationConfig.toProto());
        }
        return builder.build();
    }

    static AllConfigDto create(AgentConfig config) {
        ImmutableAllConfigDto.Builder builder = ImmutableAllConfigDto.builder()
                .transactions(TransactionConfig.create(config.getTransactionConfig()))
                .jvm(JvmConfig.create(config.getJvmConfig()))
                .uiDefaults(UiDefaultsConfig.create(config.getUiDefaultsConfig()))
                .advanced(AdvancedConfig.create(config.getAdvancedConfig()));
        for (AgentConfig.GaugeConfig gaugeConfig : config.getGaugeConfigList()) {
            builder.addGauges(GaugeConfig.create(gaugeConfig));
        }
        for (AgentConfig.SyntheticMonitorConfig syntheticMonitorConfig : config
                .getSyntheticMonitorConfigList()) {
            builder.addSyntheticMonitors(SyntheticMonitorConfig.create(syntheticMonitorConfig));
        }
        for (AgentConfig.AlertConfig alertConfig : config.getAlertConfigList()) {
            builder.addAlerts(AlertConfig.create(alertConfig));
        }
        for (AgentConfig.PluginConfig pluginConfig : config.getPluginConfigList()) {
            if (pluginConfig.getPropertyCount() > 0) {
                builder.addPlugins(PluginConfig.create(pluginConfig));
            }
        }
        for (AgentConfig.InstrumentationConfig instrumentationConfig : config
                .getInstrumentationConfigList()) {
            builder.addInstrumentation(InstrumentationConfig.create(instrumentationConfig));
        }
        return builder.version(Versions.getVersion(config))
                .build();
    }

    @Value.Immutable
    abstract static class AdvancedConfig {

        private static final org.glowroot.common.config.ImmutableAdvancedConfig DEFAULTS =
                org.glowroot.common.config.ImmutableAdvancedConfig.builder().build();

        @Value.Default
        public int immediatePartialStoreThresholdSeconds() {
            return DEFAULTS.immediatePartialStoreThresholdSeconds();
        }

        @Value.Default
        public int maxTransactionAggregates() {
            return DEFAULTS.maxTransactionAggregates();
        }

        @Value.Default
        public int maxQueryAggregates() {
            return DEFAULTS.maxQueryAggregates();
        }

        @Value.Default
        public int maxServiceCallAggregates() {
            return DEFAULTS.maxServiceCallAggregates();
        }

        @Value.Default
        public int maxTraceEntriesPerTransaction() {
            return DEFAULTS.maxTraceEntriesPerTransaction();
        }

        @Value.Default
        public int maxProfileSamplesPerTransaction() {
            return DEFAULTS.maxProfileSamplesPerTransaction();
        }

        // introduced in 0.12.3
        public abstract @Nullable Integer maxTracesStoredPerMinute();

        @Value.Default
        public int mbeanGaugeNotFoundDelaySeconds() {
            return DEFAULTS.mbeanGaugeNotFoundDelaySeconds();
        }

        @Value.Default
        @JsonInclude(Include.NON_EMPTY)
        public boolean weavingTimer() {
            return DEFAULTS.weavingTimer();
        }

        public AgentConfig.AdvancedConfig toProto(String agentVersion) {
            AgentConfig.AdvancedConfig.Builder builder = AgentConfig.AdvancedConfig.newBuilder()
                    .setImmediatePartialStoreThresholdSeconds(
                            of(immediatePartialStoreThresholdSeconds()))
                    .setMaxTransactionAggregates(of(maxTransactionAggregates()))
                    .setMaxQueryAggregates(of(maxQueryAggregates()))
                    .setMaxServiceCallAggregates(of(maxServiceCallAggregates()))
                    .setMaxTraceEntriesPerTransaction(of(maxTraceEntriesPerTransaction()))
                    .setMaxProfileSamplesPerTransaction(of(maxProfileSamplesPerTransaction()));
            if (supportsMaxTracesStoredPerMinute(agentVersion)) {
                Integer maxTracesStoredPerMinute = maxTracesStoredPerMinute();
                if (maxTracesStoredPerMinute == null) {
                    builder.setMaxTracesStoredPerMinute(of(DEFAULTS.maxTracesStoredPerMinute()));
                } else {
                    builder.setMaxTracesStoredPerMinute(of(maxTracesStoredPerMinute));
                }
            }
            return builder.setMbeanGaugeNotFoundDelaySeconds(of(mbeanGaugeNotFoundDelaySeconds()))
                    .setWeavingTimer(weavingTimer())
                    .build();
        }

        public static ImmutableAdvancedConfig create(AgentConfig.AdvancedConfig config) {
            ImmutableAdvancedConfig.Builder builder = ImmutableAdvancedConfig.builder();
            if (config.hasImmediatePartialStoreThresholdSeconds()) {
                builder.immediatePartialStoreThresholdSeconds(
                        config.getImmediatePartialStoreThresholdSeconds().getValue());
            }
            if (config.hasMaxTransactionAggregates()) {
                builder.maxTransactionAggregates(config.getMaxTransactionAggregates().getValue());
            }
            if (config.hasMaxQueryAggregates()) {
                builder.maxQueryAggregates(config.getMaxQueryAggregates().getValue());
            }
            if (config.hasMaxServiceCallAggregates()) {
                builder.maxServiceCallAggregates(config.getMaxServiceCallAggregates().getValue());
            }
            if (config.hasMaxTraceEntriesPerTransaction()) {
                builder.maxTraceEntriesPerTransaction(
                        config.getMaxTraceEntriesPerTransaction().getValue());
            }
            if (config.hasMaxProfileSamplesPerTransaction()) {
                builder.maxProfileSamplesPerTransaction(
                        config.getMaxProfileSamplesPerTransaction().getValue());
            }
            if (config.hasMaxTracesStoredPerMinute()) {
                builder.maxTracesStoredPerMinute(config.getMaxTracesStoredPerMinute().getValue());
            }
            if (config.hasMbeanGaugeNotFoundDelaySeconds()) {
                builder.mbeanGaugeNotFoundDelaySeconds(
                        config.getMbeanGaugeNotFoundDelaySeconds().getValue());
            }
            return builder.weavingTimer(config.getWeavingTimer())
                    .build();
        }

        // max traces stored per minute was introduced in agent version 0.13.4
        private static boolean supportsMaxTracesStoredPerMinute(String agentVersion) {
            return !agentVersion.startsWith("0.9.") && !agentVersion.startsWith("0.10.")
                    && !agentVersion.startsWith("0.11.") && !agentVersion.startsWith("0.12.")
                    && !agentVersion.startsWith("0.13.0,") && !agentVersion.startsWith("0.13.1,")
                    && !agentVersion.startsWith("0.13.2,") && !agentVersion.startsWith("0.13.3,");
        }

        private static OptionalInt32 of(int value) {
            return OptionalInt32.newBuilder().setValue(value).build();
        }
    }

    @Value.Immutable
    abstract static class PluginConfig {

        abstract String id();

        // when written to config.json, this will have all plugin properties
        // so not using @Json.ForceEmpty since new plugin properties can't be added in config.json
        // anyways
        abstract Map<String, PropertyValue> properties();

        AgentConfig.PluginConfig toProto() {
            AgentConfig.PluginConfig.Builder builder = AgentConfig.PluginConfig.newBuilder()
                    .setId(id());
            for (Map.Entry<String, PropertyValue> entry : properties().entrySet()) {
                PluginProperty.Builder property = PluginProperty.newBuilder()
                        .setName(entry.getKey())
                        .setValue(entry.getValue().toProto());
                builder.addProperty(property);
            }
            return builder.build();
        }

        public static ImmutablePluginConfig create(AgentConfig.PluginConfig config) {
            ImmutablePluginConfig.Builder builder = ImmutablePluginConfig.builder()
                    .id(config.getId());
            for (PluginProperty prop : config.getPropertyList()) {
                builder.putProperties(prop.getName(), PropertyValue.create(prop.getValue()));
            }
            return builder.build();
        }
    }
}
