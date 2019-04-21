/*
 * Copyright 2011-2019 the original author or authors.
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
package org.glowroot.agent.plugin.jdbc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.glowroot.agent.plugin.api.QueryEntry;
import org.glowroot.agent.plugin.api.checker.Nullable;

// used to capture and mirror the state of statements since the underlying {@link Statement} values
// cannot be inspected after they have been set
//
// this class must be public since it is referenced from bytecode inside other packages via @Mixin
public class StatementMirror {

    private final String url;

    // this field is not used by PreparedStatementMirror subclass
    //
    // ok for this field to be non-volatile since it is only temporary storage for a single thread
    // while that thread is adding batches into the statement and executing it
    private @Nullable List<String> batchedSql;

    // ok for this field to be non-volatile since it is only temporary storage for a single thread
    // while that thread is adding batches into the statement and executing it
    private @Nullable QueryEntry lastQueryEntry;

    public StatementMirror(String url) {
        this.url = url;
    }

    void addBatch(String sql) {
        // synchronization isn't an issue here as this method is called only by
        // the monitored thread
        if (batchedSql == null) {
            batchedSql = new ArrayList<String>();
        }
        batchedSql.add(sql);
    }

    List<String> getBatchedSql() {
        if (batchedSql == null) {
            return Collections.emptyList();
        } else {
            return batchedSql;
        }
    }

    void clearBatch() {
        batchedSql = null;
    }

    String getUrl() {
        return url;
    }

    @Nullable
    QueryEntry getLastQueryEntry() {
        return lastQueryEntry;
    }

    void setLastQueryEntry(QueryEntry lastQueryEntry) {
        this.lastQueryEntry = lastQueryEntry;
    }

    void clearLastQueryEntry() {
        lastQueryEntry = null;
    }
}
