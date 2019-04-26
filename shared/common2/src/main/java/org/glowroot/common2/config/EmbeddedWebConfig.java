/*
 * Copyright 2013-2018 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.immutables.value.Value;

import org.glowroot.common.util.Versions;

@Value.Immutable
public abstract class EmbeddedWebConfig implements WebConfig {

    @Value.Default
    public int port() {
        return 4000;
    }

    @Value.Default
    public String bindAddress() {
        return "127.0.0.1";
    }

    @Value.Default
    @JsonInclude(Include.NON_EMPTY)
    public boolean https() {
        return false;
    }

    @Value.Default
    public String contextPath() {
        return "/";
    }

    // timeout 0 means sessions do not time out (except on jvm restart)
    @Override
    @Value.Default
    public int sessionTimeoutMinutes() {
        return 30;
    }

    @Override
    @Value.Default
    public String sessionCookieName() {
        return "GLOWROOT_SESSION_ID";
    }

    @Value.Derived
    @JsonIgnore
    public String version() {
        return Versions.getJsonVersion(this);
    }
}
