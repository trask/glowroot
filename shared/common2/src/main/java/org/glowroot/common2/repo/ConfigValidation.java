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
package org.glowroot.common2.repo;

import java.util.Set;

import com.google.common.collect.Sets;

import org.glowroot.common.util.Versions;
import org.glowroot.common2.repo.ConfigRepository.DuplicateMBeanObjectNameException;
import org.glowroot.common2.repo.ConfigRepository.DuplicateSyntheticMonitorDisplayException;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig;

public class ConfigValidation {

    private ConfigValidation() {}

    public static void validatePartOne(AgentConfig agentConfig) throws Exception {
        Set<String> gaugeMBeanObjectNames = Sets.newHashSet();
        for (AgentConfig.GaugeConfig config : agentConfig.getGaugeConfigList()) {
            if (!gaugeMBeanObjectNames.add(config.getMbeanObjectName())) {
                throw new DuplicateMBeanObjectNameException();
            }
        }
        Set<String> syntheticMonitorDisplays = Sets.newHashSet();
        for (AgentConfig.SyntheticMonitorConfig config : agentConfig
                .getSyntheticMonitorConfigList()) {
            if (!syntheticMonitorDisplays.add(config.getDisplay())) {
                throw new DuplicateSyntheticMonitorDisplayException();
            }
        }
        Set<String> alertVersions = Sets.newHashSet();
        for (AgentConfig.AlertConfig config : agentConfig.getAlertConfigList()) {
            if (!alertVersions.add(Versions.getVersion(config))) {
                throw new IllegalStateException("Duplicate alerts");
            }
        }
        Set<String> instrumentationIds = Sets.newHashSet();
        for (AgentConfig.InstrumentationConfig config : agentConfig
                .getInstrumentationConfigList()) {
            if (!instrumentationIds.add(config.getId())) {
                throw new IllegalStateException("Duplicate instrumentation id: " + config.getId());
            }
        }
        Set<String> instrumentationVersions = Sets.newHashSet();
        for (AgentConfig.CustomInstrumentationConfig config : agentConfig
                .getCustomInstrumentationConfigList()) {
            if (!instrumentationVersions.add(Versions.getVersion(config))) {
                throw new IllegalStateException("Duplicate custom instrumentation");
            }
        }
    }

    public static void validatePartTwo(AgentConfig agentConfig, Set<String> validInstrumentationIds)
            throws Exception {
        for (AgentConfig.InstrumentationConfig config : agentConfig
                .getInstrumentationConfigList()) {
            if (!validInstrumentationIds.contains(config.getId())) {
                throw new IllegalStateException("Invalid instrumentation id: " + config.getId());
            }
        }
    }
}
