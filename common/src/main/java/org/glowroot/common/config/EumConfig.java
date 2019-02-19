/*
 * Copyright 2019 the original author or authors.
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
package org.glowroot.common.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.immutables.value.Value;

import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig;

@Value.Immutable
public abstract class EumConfig {

    @Value.Default
    @JsonInclude(Include.NON_EMPTY)
    public boolean enabled() {
        return false;
    }

    @Value.Default
    @JsonInclude(Include.NON_EMPTY)
    public String reportingUrl() {
        return "";
    }

    public AgentConfig.EumConfig toProto() {
        return AgentConfig.EumConfig.newBuilder()
                .setEnabled(enabled())
                .setReportingUrl(reportingUrl())
                .build();
    }

    public static ImmutableEumConfig create(AgentConfig.EumConfig config) {
        ImmutableEumConfig.Builder builder = ImmutableEumConfig.builder();
        return builder.enabled(config.getEnabled())
                .reportingUrl(config.getReportingUrl())
                .build();
    }
}
