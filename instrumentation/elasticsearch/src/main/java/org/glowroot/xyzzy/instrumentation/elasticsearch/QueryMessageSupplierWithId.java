/*
 * Copyright 2017-2019 the original author or authors.
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
package org.glowroot.xyzzy.instrumentation.elasticsearch;

import org.glowroot.xyzzy.instrumentation.api.QueryMessage;
import org.glowroot.xyzzy.instrumentation.api.QueryMessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.checker.Nullable;

class QueryMessageSupplierWithId extends QueryMessageSupplier {

    private final @Nullable String id;

    QueryMessageSupplierWithId(@Nullable String id) {
        this.id = id;
    }

    @Override
    public QueryMessage get() {
        if (id == null) {
            return QueryMessage.create(Constants.QUERY_MESSAGE_PREFIX);
        } else {
            return QueryMessage.create(Constants.QUERY_MESSAGE_PREFIX, " [" + id + "]");
        }
    }
}
