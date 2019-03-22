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

import org.glowroot.common.config.AdvancedConfig;
import org.glowroot.common.config.AlertConfig;
import org.glowroot.common.config.CustomInstrumentationConfig;
import org.glowroot.common.config.GaugeConfig;
import org.glowroot.common.config.ImmutableAdvancedConfig;
import org.glowroot.common.config.ImmutableAlertConfig;
import org.glowroot.common.config.ImmutableCustomInstrumentationConfig;
import org.glowroot.common.config.ImmutableGaugeConfig;
import org.glowroot.common.config.ImmutableJvmConfig;
import org.glowroot.common.config.ImmutableSyntheticMonitorConfig;
import org.glowroot.common.config.ImmutableTransactionConfig;
import org.glowroot.common.config.ImmutableUiDefaultsConfig;
import org.glowroot.common.config.JvmConfig;
import org.glowroot.common.config.PropertyValue;
import org.glowroot.common.config.SyntheticMonitorConfig;
import org.glowroot.common.config.TransactionConfig;
import org.glowroot.common.config.UiDefaultsConfig;
import org.glowroot.common.util.Versions;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.InstrumentationProperty;

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
    abstract List<ImmutableInstrumentationConfig> instrumentation();

    @JsonInclude(Include.NON_EMPTY)
    abstract List<ImmutableCustomInstrumentationConfig> customInstrumentation();

    abstract @Nullable String version();

    AgentConfig toProto() {
        AgentConfig.Builder builder = AgentConfig.newBuilder()
                .setTransactionConfig(transactions().toProto())
                .setJvmConfig(jvm().toProto())
                .setUiDefaultsConfig(uiDefaults().toProto())
                .setAdvancedConfig(advanced().toProto());
        for (GaugeConfig config : gauges()) {
            builder.addGaugeConfig(config.toProto());
        }
        for (SyntheticMonitorConfig config : syntheticMonitors()) {
            builder.addSyntheticMonitorConfig(config.toProto());
        }
        for (AlertConfig config : alerts()) {
            builder.addAlertConfig(config.toProto());
        }
        for (InstrumentationConfig config : instrumentation()) {
            builder.addInstrumentationConfig(config.toProto());
        }
        for (CustomInstrumentationConfig config : customInstrumentation()) {
            builder.addCustomInstrumentationConfig(config.toProto());
        }
        return builder.build();
    }

    static AllConfigDto create(AgentConfig agentConfig) {
        ImmutableAllConfigDto.Builder builder = ImmutableAllConfigDto.builder()
                .transactions(TransactionConfig.create(agentConfig.getTransactionConfig()))
                .jvm(JvmConfig.create(agentConfig.getJvmConfig()))
                .uiDefaults(UiDefaultsConfig.create(agentConfig.getUiDefaultsConfig()))
                .advanced(AdvancedConfig.create(agentConfig.getAdvancedConfig()));
        for (AgentConfig.GaugeConfig config : agentConfig.getGaugeConfigList()) {
            builder.addGauges(GaugeConfig.create(config));
        }
        for (AgentConfig.SyntheticMonitorConfig config : agentConfig
                .getSyntheticMonitorConfigList()) {
            builder.addSyntheticMonitors(SyntheticMonitorConfig.create(config));
        }
        for (AgentConfig.AlertConfig config : agentConfig.getAlertConfigList()) {
            builder.addAlerts(AlertConfig.create(config));
        }
        for (AgentConfig.InstrumentationConfig config : agentConfig
                .getInstrumentationConfigList()) {
            if (config.getPropertyCount() > 0) {
                builder.addInstrumentation(InstrumentationConfig.create(config));
            }
        }
        for (AgentConfig.CustomInstrumentationConfig config : agentConfig
                .getCustomInstrumentationConfigList()) {
            builder.addCustomInstrumentation(CustomInstrumentationConfig.create(config));
        }
        return builder.version(Versions.getVersion(agentConfig))
                .build();
    }

    @Value.Immutable
    abstract static class InstrumentationConfig {

        abstract String id();

        // when written to config.json, this will have all instrumentation properties
        // so not using @Json.ForceEmpty since new instrumentation properties can't be added in
        // config.json anyways
        abstract Map<String, PropertyValue> properties();

        AgentConfig.InstrumentationConfig toProto() {
            AgentConfig.InstrumentationConfig.Builder builder =
                    AgentConfig.InstrumentationConfig.newBuilder()
                            .setId(id());
            for (Map.Entry<String, PropertyValue> entry : properties().entrySet()) {
                InstrumentationProperty.Builder property = InstrumentationProperty.newBuilder()
                        .setName(entry.getKey())
                        .setValue(entry.getValue().toProto());
                builder.addProperty(property);
            }
            return builder.build();
        }

        public static ImmutableInstrumentationConfig create(
                AgentConfig.InstrumentationConfig config) {
            ImmutableInstrumentationConfig.Builder builder =
                    ImmutableInstrumentationConfig.builder()
                            .id(config.getId());
            for (InstrumentationProperty property : config.getPropertyList()) {
                builder.putProperties(property.getName(),
                        PropertyValue.create(property.getValue()));
            }
            return builder.build();
        }
    }
}
