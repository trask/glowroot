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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.immutables.value.Value;

import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig;
import org.glowroot.wire.api.model.Proto.OptionalInt32;

@Value.Immutable
public abstract class StatsdConfig {

    @Value.Default
    @JsonInclude(Include.NON_EMPTY)
    public String host() {
        return "";
    }

    @JsonInclude(Include.NON_NULL)
    public abstract @Nullable Integer port();

    @Value.Default
    @JsonInclude(Include.NON_EMPTY)
    public String prefix() {
        return "";
    }

    public AgentConfig.StatsdConfig toProto() {
        AgentConfig.StatsdConfig.Builder builder = AgentConfig.StatsdConfig.newBuilder()
                .setHost(host());
        Integer port = port();
        if (port != null) {
            builder.setPort(OptionalInt32.newBuilder().setValue(port));
        }
        return builder.setPrefix(prefix())
                .build();
    }

    public static ImmutableStatsdConfig create(AgentConfig.StatsdConfig config) {
        ImmutableStatsdConfig.Builder builder = ImmutableStatsdConfig.builder()
                .host(config.getHost());
        if (config.hasPort()) {
            builder.port(config.getPort().getValue());
        }
        return builder.prefix(config.getPrefix())
                .build();
    }
}
