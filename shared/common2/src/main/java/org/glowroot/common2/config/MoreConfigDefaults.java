/*
 * Copyright 2016-2018 the original author or authors.
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
package org.glowroot.common2.config;

import com.google.common.annotations.VisibleForTesting;

import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.SyntheticMonitorConfig;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.SyntheticMonitorConfig.SyntheticMonitorKind;

public final class MoreConfigDefaults {

    private MoreConfigDefaults() {}

    public static String getDisplayOrDefault(SyntheticMonitorConfig config) {
        String display = config.getDisplay();
        if (!display.isEmpty()) {
            return display;
        }
        if (config.getKind() == SyntheticMonitorKind.PING) {
            return "Ping " + config.getPingUrl();
        } else {
            return "<Missing display>";
        }
    }

    public static String getDefaultAgentRollupDisplayPart(String agentRollupId) {
        return unescapeForDisplay(getLastPartForDisplay(agentRollupId));
    }

    @VisibleForTesting
    static String getLastPartForDisplay(String agentRollupId) {
        if (agentRollupId.endsWith("::")) {
            int index = agentRollupId.lastIndexOf("::", agentRollupId.length() - 3);
            if (index == -1) {
                return agentRollupId.substring(0, agentRollupId.length() - 2);
            } else {
                return agentRollupId.substring(index + 2, agentRollupId.length() - 2);
            }
        } else {
            int index = agentRollupId.lastIndexOf("::");
            if (index == -1) {
                return agentRollupId;
            } else {
                return agentRollupId.substring(index + 2);
            }
        }
    }

    private static String unescapeForDisplay(String escaped) {
        return escaped.replace("\\:", ":").replace("\\\\", "\\");
    }
}
