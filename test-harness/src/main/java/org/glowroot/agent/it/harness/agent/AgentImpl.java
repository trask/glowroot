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
package org.glowroot.agent.it.harness.agent;

import org.glowroot.agent.it.harness.model.ServerSpanImpl;
import org.glowroot.xyzzy.engine.bytecode.api.ThreadContextThreadLocal;
import org.glowroot.xyzzy.engine.weaving.AgentSPI;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.TimerName;
import org.glowroot.xyzzy.instrumentation.api.TraceEntry;

class AgentImpl implements AgentSPI {

    AgentImpl() {}

    // in addition to returning TraceEntry, this method needs to put the newly created
    // ThreadContextPlus into the threadContextHolder that is passed in
    @Override
    public TraceEntry startTransaction(String transactionType, String transactionName,
            MessageSupplier messageSupplier, TimerName timerName,
            ThreadContextThreadLocal.Holder threadContextHolder, int rootNestingGroupId,
            int rootSuppressionKeyId) {

        ServerSpanImpl serverSpan = new ServerSpanImpl(messageSupplier, threadContextHolder);
        ThreadContextImpl threadContext = new ThreadContextImpl(threadContextHolder, serverSpan,
                null, rootNestingGroupId, rootSuppressionKeyId);
        threadContextHolder.set(threadContext);

        return serverSpan;
    }
}
