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
package org.glowroot.xyzzy.instrumentation.jdbc;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.glowroot.xyzzy.instrumentation.api.checker.Nullable;
import org.glowroot.xyzzy.instrumentation.jdbc.message.BindParameterList;

// used to capture and mirror the state of prepared statements since the underlying
// PreparedStatement values cannot be inspected after they have been set
class PreparedStatementMirror extends StatementMirror {

    private static final int CAPTURED_BATCH_SIZE_LIMIT = 1000;

    private static final int PARAMETERS_INITIAL_CAPACITY = 4;

    private final String sql;
    // ok for this field to be non-volatile since it is only temporary storage for a single thread
    // while that thread is setting parameter values into the prepared statement and executing it
    private BindParameterList parameters;
    private boolean parametersShared;
    // ok for this field to be non-volatile since it is only temporary storage for a single thread
    // while that thread is setting parameter values into the prepared statement and executing it
    private @Nullable Collection<BindParameterList> batchedParameters;
    private int batchSize;

    PreparedStatementMirror(String sql) {
        this.sql = sql;
        // TODO delay creation to optimize case when bind parameter capture is disabled
        parameters = new BindParameterList(PARAMETERS_INITIAL_CAPACITY);
    }

    void addBatch() {
        // synchronization isn't an issue here as this method is called only by the monitored thread
        if (batchedParameters == null) {
            batchedParameters = new ConcurrentLinkedQueue<BindParameterList>();
        }
        if (batchSize++ < CAPTURED_BATCH_SIZE_LIMIT) {
            batchedParameters.add(parameters);
            parametersShared = true;
        }
    }

    Collection<BindParameterList> getBatchedParameters() {
        if (batchedParameters == null) {
            return Collections.emptyList();
        } else {
            return batchedParameters;
        }
    }

    @Nullable
    BindParameterList getParameters() {
        parametersShared = true;
        return parameters;
    }

    String getSql() {
        return sql;
    }

    int getBatchSize() {
        return batchSize;
    }

    // remember parameterIndex starts at 1 not 0
    void setParameterValue(int parameterIndex, @Nullable Object object) {
        if (parametersShared) {
            // separate method for less common path to not impact inlining budget of fast(er) path
            copyParameters();
        }
        parameters.set(parameterIndex - 1, object);
    }

    private void copyParameters() {
        parameters = BindParameterList.copyOf(parameters);
        parametersShared = false;
    }

    void clearParameters() {
        if (parametersShared) {
            parameters = new BindParameterList(parameters.size());
            parametersShared = false;
        } else {
            parameters.clear();
        }
    }

    @Override
    public void clearBatch() {
        if (parametersShared) {
            parameters = new BindParameterList(parameters.size());
            parametersShared = false;
        } else {
            parameters.clear();
        }
        batchedParameters = null;
        batchSize = 0;
    }

    static class ByteArrayParameterValue {

        private static final char[] hexDigits = "0123456789abcdef".toCharArray();

        private final int length;
        private final byte /*@Nullable*/ [] bytes;

        ByteArrayParameterValue(byte[] bytes, boolean displayAsHex) {
            length = bytes.length;
            // only retain bytes if needed for displaying as hex
            this.bytes = displayAsHex ? bytes : null;
        }

        @Override
        public String toString() {
            if (bytes != null) {
                StringBuilder sb = new StringBuilder(2 + 2 * bytes.length);
                sb.append("0x");
                for (byte b : bytes) {
                    // this logic copied from com.google.common.hash.HashCode.toString()
                    sb.append(hexDigits[(b >> 4) & 0xf]).append(hexDigits[b & 0xf]);
                }
                return sb.toString();
            } else {
                return "{" + length + " bytes}";
            }
        }
    }

    static class StreamingParameterValue {

        private final Class<?> clazz;

        StreamingParameterValue(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public String toString() {
            return "{stream:" + clazz.getSimpleName() + "}";
        }
    }
}
