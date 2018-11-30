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
package org.glowroot.common2.model;

import com.fasterxml.jackson.core.JsonGenerator;

import org.glowroot.common2.repo.proto.NetworkGraphOuterClass.NetworkGraph;

public class MutableNetworkGraph {

    public void merge(NetworkGraph networkGraph) {
        // FIXME
    }

    public boolean isEmpty() {
        // FIXME
        return false;
    }

    public NetworkGraph toProto() {
        // FIXME
        return NetworkGraph.getDefaultInstance();
    }

    public void writeJson(JsonGenerator jg) {
        // FIXME
    }
}
