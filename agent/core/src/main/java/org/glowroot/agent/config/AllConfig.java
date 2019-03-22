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
package org.glowroot.agent.config;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.immutables.value.Value;

import org.glowroot.common.config.AdvancedConfig;
import org.glowroot.common.config.AlertConfig;
import org.glowroot.common.config.CustomInstrumentationConfig;
import org.glowroot.common.config.GaugeConfig;
import org.glowroot.common.config.JvmConfig;
import org.glowroot.common.config.SyntheticMonitorConfig;
import org.glowroot.common.config.TransactionConfig;
import org.glowroot.common.config.UiDefaultsConfig;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.InstrumentationProperty;
import org.glowroot.xyzzy.engine.config.InstrumentationDescriptor;

@Value.Immutable
public abstract class AllConfig {

    abstract TransactionConfig transaction();
    abstract JvmConfig jvm();
    abstract UiDefaultsConfig uiDefaults();
    abstract AdvancedConfig advanced();
    abstract List<GaugeConfig> gauges();
    abstract List<SyntheticMonitorConfig> syntheticMonitors();
    abstract List<AlertConfig> alerts();
    abstract List<InstrumentationConfig> instrumentation();
    abstract List<CustomInstrumentationConfig> customInstrumentation();

    public static AllConfig create(AgentConfig agentConfig,
            List<InstrumentationDescriptor> instrumentationDescriptors) {
        ImmutableAllConfig.Builder builder = ImmutableAllConfig.builder()
                .transaction(TransactionConfig.create(agentConfig.getTransactionConfig()))
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
        Map<String, AgentConfig.InstrumentationConfig> newConfigs = Maps.newHashMap();
        for (AgentConfig.InstrumentationConfig config : agentConfig
                .getInstrumentationConfigList()) {
            newConfigs.put(config.getId(), config);
        }
        for (InstrumentationDescriptor descriptor : instrumentationDescriptors) {
            AgentConfig.InstrumentationConfig config = newConfigs.get(descriptor.id());
            List<InstrumentationProperty> properties = Lists.newArrayList();
            if (config == null) {
                properties = ImmutableList.of();
            } else {
                properties = config.getPropertyList();
            }
            builder.addInstrumentation(InstrumentationConfig.create(descriptor, properties));
        }
        for (AgentConfig.CustomInstrumentationConfig config : agentConfig
                .getCustomInstrumentationConfigList()) {
            builder.addCustomInstrumentation(CustomInstrumentationConfig.create(config));
        }
        return builder.build();
    }
}
