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
package org.glowroot.xyzzy.instrumentation.okhttp3;

import java.io.IOException;

import org.glowroot.xyzzy.instrumentation.api.AsyncTraceEntry;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class CallbackWrapperForNullDelegate implements Callback {

    private final AsyncTraceEntry asyncTraceEntry;

    public CallbackWrapperForNullDelegate(AsyncTraceEntry asyncTraceEntry) {
        this.asyncTraceEntry = asyncTraceEntry;
    }

    @Override
    public void onFailure(Call call, IOException exception) {
        asyncTraceEntry.endWithError(exception);
    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        asyncTraceEntry.end();
    }
}
