/*
 * Copyright 2016-2017 the original author or authors.
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
package org.glowroot.agent.plugin.cassandra;

import java.net.InetAddress;

import javax.annotation.Nullable;

import org.glowroot.agent.plugin.api.Agent;
import org.glowroot.agent.plugin.api.AsyncTraceEntry;
import org.glowroot.agent.plugin.api.AuxThreadContext;
import org.glowroot.agent.plugin.api.MessageSupplier;
import org.glowroot.agent.plugin.api.OptionalThreadContext;
import org.glowroot.agent.plugin.api.ThreadContext;
import org.glowroot.agent.plugin.api.ThreadContext.Priority;
import org.glowroot.agent.plugin.api.TimerName;
import org.glowroot.agent.plugin.api.TraceEntry;
import org.glowroot.agent.plugin.api.weaving.BindClassMeta;
import org.glowroot.agent.plugin.api.weaving.BindParameter;
import org.glowroot.agent.plugin.api.weaving.BindReceiver;
import org.glowroot.agent.plugin.api.weaving.BindThrowable;
import org.glowroot.agent.plugin.api.weaving.BindTraveler;
import org.glowroot.agent.plugin.api.weaving.Mixin;
import org.glowroot.agent.plugin.api.weaving.OnBefore;
import org.glowroot.agent.plugin.api.weaving.OnReturn;
import org.glowroot.agent.plugin.api.weaving.OnThrow;
import org.glowroot.agent.plugin.api.weaving.Pointcut;

public class ServerAspect {

    // the field and method names are verbose since they will be mixed in to existing classes
    @Mixin("org.apache.cassandra.net.IAsyncCallbackWithFailure")
    public abstract static class CallbackImpl implements CallbackMixin {

        private volatile @Nullable AsyncTraceEntry glowroot$asyncTraceEntry;

        @Override
        public @Nullable AsyncTraceEntry glowroot$getAsyncTraceEntry() {
            return glowroot$asyncTraceEntry;
        }

        @Override
        public void glowroot$setAsyncTraceEntry(@Nullable AsyncTraceEntry asyncTraceEntry) {
            glowroot$asyncTraceEntry = asyncTraceEntry;
        }
    }

    // the field and method names are verbose since they will be mixed in to existing classes
    @Mixin("org.apache.cassandra.net.MessageOut")
    public abstract static class MessageOutImpl implements MessageOutMixin {

        private volatile @Nullable AuxThreadContext glowroot$auxContext;

        @Override
        public @Nullable AuxThreadContext glowroot$getAuxContext() {
            return glowroot$auxContext;
        }

        @Override
        public void glowroot$setAuxContext(@Nullable AuxThreadContext auxContext) {
            glowroot$auxContext = auxContext;
        }
    }

    // the method names are verbose since they will be mixed in to existing classes
    public interface CallbackMixin {

        @Nullable
        AsyncTraceEntry glowroot$getAsyncTraceEntry();

        void glowroot$setAsyncTraceEntry(@Nullable AsyncTraceEntry asyncTraceEntry);
    }

    // the method names are verbose since they will be mixed in to existing classes
    public interface MessageOutMixin {

        @Nullable
        AuxThreadContext glowroot$getAuxContext();

        void glowroot$setAuxContext(@Nullable AuxThreadContext auxContext);
    }

    @Pointcut(className = "org.apache.cassandra.net.MessagingService",
            methodName = "sendRRWithFailure",
            methodParameterTypes = {"org.apache.cassandra.net.MessageOut", "java.net.InetAddress",
                    "org.apache.cassandra.net.IAsyncCallbackWithFailure"},
            nestingGroup = "sendrr", timerName = "send rr")
    public static class SendRRWithFailureAdvice {
        private static final TimerName timerName =
                Agent.getTimerName(SendRRWithFailureAdvice.class);
        @OnBefore
        public static @Nullable AsyncTraceEntry onBefore(ThreadContext context,
                @SuppressWarnings("unused") @BindParameter @Nullable Object messageOut,
                @BindParameter @Nullable InetAddress address,
                @BindParameter @Nullable CallbackMixin callback) {
            if (callback == null) {
                return null;
            }
            String addr = address == null ? "" : address.toString();
            AsyncTraceEntry asyncTraceEntry =
                    context.startAsyncServiceCallEntry("sendRRWithFailure", addr,
                            MessageSupplier.create("sendRRWithFailure: {}", addr), timerName);
            // important to inject values into callback in @OnBefore since it's possible for
            // callback to be invoked prior to @OnReturn
            callback.glowroot$setAsyncTraceEntry(asyncTraceEntry);
            return asyncTraceEntry;
        }
        @OnReturn
        public static void onReturn(@BindTraveler @Nullable AsyncTraceEntry asyncTraceEntry) {
            if (asyncTraceEntry != null) {
                asyncTraceEntry.stopSyncTimer();
            }
        }
        @OnThrow
        public static void onThrow(@BindThrowable Throwable throwable,
                @BindTraveler @Nullable AsyncTraceEntry asyncTraceEntry) {
            if (asyncTraceEntry != null) {
                asyncTraceEntry.stopSyncTimer();
                asyncTraceEntry.endWithError(throwable);
            }
        }
    }

    @Pointcut(className = "org.apache.cassandra.net.MessagingService",
            methodName = "sendRR",
            methodParameterTypes = {"org.apache.cassandra.net.MessageOut", "java.net.InetAddress",
                    "org.apache.cassandra.service.AbstractWriteResponseHandler", "boolean"},
            nestingGroup = "sendrr", timerName = "send rr")
    public static class SendRRAdvice {
        @OnBefore
        public static @Nullable AsyncTraceEntry onBefore(ThreadContext context,
                @BindParameter @Nullable Object messageOut,
                @BindParameter @Nullable InetAddress address,
                @BindParameter @Nullable CallbackMixin callback) {
            return SendRRWithFailureAdvice.onBefore(context, messageOut, address, callback);
        }
        @OnReturn
        public static void onReturn(@BindTraveler @Nullable AsyncTraceEntry asyncTraceEntry) {
            SendRRWithFailureAdvice.onReturn(asyncTraceEntry);
        }
        @OnThrow
        public static void onThrow(@BindThrowable Throwable throwable,
                @BindTraveler @Nullable AsyncTraceEntry asyncTraceEntry) {
            SendRRWithFailureAdvice.onThrow(throwable, asyncTraceEntry);
        }
    }

    @Pointcut(className = "org.apache.cassandra.net.IAsyncCallback",
            subTypeRestriction = "org.apache.cassandra.net.IAsyncCallbackWithFailure",
            methodName = "response", methodParameterTypes = {"org.apache.cassandra.net.MessageIn"})
    public static class ResponseCompletedAdvice {
        @OnBefore
        public static void onBefore(@BindReceiver CallbackMixin callback) {
            AsyncTraceEntry asyncTraceEntry = callback.glowroot$getAsyncTraceEntry();
            if (asyncTraceEntry != null) {
                asyncTraceEntry.end();
            }
        }
    }

    @Pointcut(className = "org.apache.cassandra.net.IAsyncCallbackWithFailure",
            methodName = "onFailure", methodParameterTypes = {"java.net.InetAddress"})
    public static class OnFailureAdvice {
        @OnBefore
        public static void onBefore(@BindReceiver CallbackMixin callback) {
            AsyncTraceEntry asyncTraceEntry = callback.glowroot$getAsyncTraceEntry();
            if (asyncTraceEntry != null) {
                asyncTraceEntry.endWithError("exception on remote host or timeout (unspecified)");
            }
        }
    }

    @Pointcut(className = "org.apache.cassandra.net.OutboundTcpConnection",
            methodName = "enqueue",
            methodParameterTypes = {"org.apache.cassandra.net.MessageOut", "int"})
    public static class OutboundTcpConnectionEnqueueAdvice {
        @OnBefore
        public static void onBefore(ThreadContext threadContext,
                @BindParameter MessageOutMixin messageOut) {
            messageOut.glowroot$setAuxContext(threadContext.createAuxThreadContext());
        }
    }

    @Pointcut(className = "org.apache.cassandra.net.OutboundTcpConnection",
            methodName = "writeConnected",
            methodParameterTypes = {"org.apache.cassandra.net.OutboundTcpConnection$QueuedMessage",
                    "boolean"},
            order = Priority.CORE_PLUGIN)
    public static class OutboundTcpConnectionWriteConnectedAdvice {
        @OnBefore
        public static @Nullable TraceEntry onBefore(@BindParameter Object queuedMessage,
                @BindClassMeta ServerInvoker serverInvoker) {
            MessageOutMixin message = (MessageOutMixin) serverInvoker.getMessage(queuedMessage);
            if (message == null) {
                return null;
            }
            AuxThreadContext auxContext = message.glowroot$getAuxContext();
            if (auxContext != null) {
                message.glowroot$setAuxContext(null);
                return auxContext.start();
            }
            return null;
        }
        @OnReturn
        public static void onReturn(@BindTraveler @Nullable TraceEntry traceEntry) {
            if (traceEntry != null) {
                traceEntry.end();
            }
        }
        @OnThrow
        public static void onThrow(@BindThrowable Throwable t,
                @BindTraveler @Nullable TraceEntry traceEntry) {
            if (traceEntry != null) {
                traceEntry.endWithError(t);
            }
        }
    }

    @Pointcut(className = "org.apache.cassandra.net.OutboundTcpConnection",
            methodName = "writeConnected",
            methodParameterTypes = {"org.apache.cassandra.net.OutboundTcpConnection$QueuedMessage",
                    "boolean"},
            timerName = "write connected", order = Priority.CORE_PLUGIN + 1)
    public static class OutboundTcpConnectionWriteConnectedAdvice2 {
        private static final TimerName timerName =
                Agent.getTimerName(OutboundTcpConnectionWriteConnectedAdvice2.class);
        @OnBefore
        public static TraceEntry onBefore(OptionalThreadContext threadContext) {
            return threadContext.startTraceEntry(MessageSupplier.create("write connected"),
                    timerName);
        }
        @OnReturn
        public static void onReturn(@BindTraveler TraceEntry traceEntry) {
            traceEntry.end();
        }
        @OnThrow
        public static void onThrow(@BindThrowable Throwable t,
                @BindTraveler TraceEntry traceEntry) {
            traceEntry.endWithError(t);
        }
    }
}
