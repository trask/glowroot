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
package org.glowroot.xyzzy.instrumentation.executor;

import org.glowroot.xyzzy.instrumentation.api.ClassInfo;

public class FutureClassMeta {

    private final boolean nonStandardFuture;

    public FutureClassMeta(ClassInfo classInfo) {
        // javax.ejb.AsyncResult is final, so don't need to worry about sub-classes
        nonStandardFuture = classInfo.getName().equals("javax.ejb.AsyncResult");
    }

    public boolean isNonStandardFuture() {
        return nonStandardFuture;
    }
}
