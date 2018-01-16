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
package org.glowroot.agent.plugin.play;

import org.glowroot.agent.plugin.api.Agent;
import org.glowroot.agent.plugin.api.MessageSupplier;
import org.glowroot.agent.plugin.api.OptionalThreadContext;
import org.glowroot.agent.plugin.api.ThreadContext;
import org.glowroot.agent.plugin.api.ThreadContext.Priority;
import org.glowroot.agent.plugin.api.TimerName;
import org.glowroot.agent.plugin.api.TraceEntry;
import org.glowroot.agent.plugin.api.checker.Nullable;
import org.glowroot.agent.plugin.api.config.BooleanProperty;
import org.glowroot.agent.plugin.api.weaving.BindClassMeta;
import org.glowroot.agent.plugin.api.weaving.BindParameter;
import org.glowroot.agent.plugin.api.weaving.BindReceiver;
import org.glowroot.agent.plugin.api.weaving.BindReturn;
import org.glowroot.agent.plugin.api.weaving.BindThrowable;
import org.glowroot.agent.plugin.api.weaving.BindTraveler;
import org.glowroot.agent.plugin.api.weaving.OnBefore;
import org.glowroot.agent.plugin.api.weaving.OnReturn;
import org.glowroot.agent.plugin.api.weaving.OnThrow;
import org.glowroot.agent.plugin.api.weaving.Pointcut;
import org.glowroot.agent.plugin.api.weaving.Shim;
import org.glowroot.agent.plugin.play.interop.Function1;

public class Play2xAspect {

    private static final BooleanProperty useAltTransactionNaming =
            Agent.getConfigService("play").getBooleanProperty("useAltTransactionNaming");

    // ========== play 2.3.x - 2.6.x ==========

    @Shim("play.api.mvc.Request")
    public interface Request {

        @Nullable
        String uri();

        @Nullable
        String path();

        @Shim("play.api.mvc.Headers headers()")
        @Nullable
        Headers glowroot$headers();

        // play 2.6.x
        @Shim("play.api.libs.typedmap.TypedMap attrs()")
        @Nullable
        TypedMap glowroot$attrs();

        // play 2.3.x - 2.5.x
        @Shim("scala.collection.immutable.Map tags()")
        @Nullable
        ScalaMap glowroot$tags();
    }

    @Shim("play.api.mvc.Headers")
    public interface Headers {

        @Shim("scala.Option get(java.lang.String)")
        @Nullable
        ScalaOption glowroot$get(String key);
    }

    // play 2.6.x
    @Shim("play.api.libs.typedmap.TypedMap")
    public interface TypedMap {

        @Shim("scala.Option get(play.api.libs.typedmap.TypedKey)")
        @Nullable
        ScalaOption glowroot$get(Object key);
    }

    // play 2.6.x
    @Shim("play.api.routing.HandlerDef")
    public interface HandlerDef {

        @Nullable
        String controller();

        @Nullable
        String method();

        @Nullable
        String path();
    }

    @Shim("scala.collection.immutable.Map")
    public interface ScalaMap {

        @Shim("scala.Option get(java.lang.Object)")
        @Nullable
        ScalaOption glowroot$get(Object key);
    }

    @Shim("scala.Option")
    public interface ScalaOption {

        boolean isDefined();

        @Nullable
        Object get();
    }

    @Shim("scala.concurrent.Future")
    public interface ScalaFuture {

        @Shim("void onComplete(scala.Function1, scala.concurrent.ExecutionContext)")
        void glowroot$onComplete(Object f, Object executor);
    }

    @Shim("scala.util.Try")
    public interface ScalaTry {

        boolean isFailure();
    }

    @Shim("scala.util.Failure")
    public interface ScalaFailure {

        Throwable exception();
    }

    @Shim("scala.util.Success")
    public interface ScalaSuccess {

        Object value();
    }

    @Shim("akka.http.scaladsl.model.HttpResponse")
    public interface HttpResponse {

        @Shim("akka.http.scaladsl.model.StatusCode status()")
        StatusCode glowroot$status();
    }

    @Shim("akka.http.scaladsl.model.StatusCode")
    public interface StatusCode {

        int intValue();
    }

    @Pointcut(className = "play.api.mvc.Action", methodName = "apply",
            methodParameterTypes = {"play.api.mvc.Request"},
            nestingGroup = "outer-handler-or-filter", timerName = "http request")
    public static class AkkaHttpServerAdvice {

        private static final TimerName timerName = Agent.getTimerName(AkkaHttpServerAdvice.class);

        @OnBefore
        public static @Nullable TraceEntry onBefore(OptionalThreadContext context,
                                                    @BindParameter @Nullable Request request, @BindClassMeta Play26xInvoker invoker) {
            if (request == null) {
                return null;
            }
            String transactionType = "Web";
            String transactionName = null;
            boolean setTransactionTypeWithCoreMaxPriority = false;
            boolean setTransactionNameWithCoreMaxPriority = false;
            Headers headers = request.glowroot$headers();
            if (headers != null) {
                ScalaOption transactionTypeOverrideOption =
                        headers.glowroot$get("Glowroot-Transaction-Type");
                if (transactionTypeOverrideOption != null
                        && transactionTypeOverrideOption.isDefined()) {
                    Object transactionTypeHeader = transactionTypeOverrideOption.get();
                    if ("Synthetic".equals(transactionTypeHeader)) {
                        // Glowroot-Transaction-Type header currently only accepts "Synthetic", in
                        // order to prevent spamming of transaction types, which could cause some
                        // issues
                        transactionType = "Synthetic";
                        setTransactionTypeWithCoreMaxPriority = true;
                    }
                }
                ScalaOption transactionNameHeaderOption =
                        headers.glowroot$get("Glowroot-Transaction-Name");
                if (transactionNameHeaderOption != null
                        && transactionNameHeaderOption.isDefined()) {
                    Object transactionNameHeaderObj = transactionNameHeaderOption.get();
                    if (transactionNameHeaderObj != null) {
                        String transactionNameHeader = transactionNameHeaderObj.toString();
                        if (transactionNameHeader != null && !transactionNameHeader.isEmpty()) {
                            // Glowroot-Transaction-Name header is useful for automated tests which
                            // want to send a more specific name for the transaction
                            transactionName = transactionNameHeaderObj.toString();
                            setTransactionNameWithCoreMaxPriority = true;
                        }
                    }
                }
            }
            if (transactionName == null) {
                Object handlerDefTypedKey = invoker.getHandlerDefTypedKey();
                if (handlerDefTypedKey == null) {
                    // play 2.3.x - 2.5.x
                    transactionName = getTransactionNameFor23xThrough25x(request);
                } else {
                    // play 2.6.x
                    transactionName = getTransactionNameFor26x(request, handlerDefTypedKey);
                }
                if (transactionName == null) {
                    transactionName = Strings.nullToEmpty(request.path());
                }
            }
            TraceEntry traceEntry = null;
            if (context.isInTransaction()) {
                if (setTransactionTypeWithCoreMaxPriority) {
                    context.setTransactionType(transactionType, Priority.CORE_MAX);
                } else {
                    context.setTransactionType(transactionType, Priority.CORE_PLUGIN);
                }
                if (setTransactionNameWithCoreMaxPriority) {
                    context.setTransactionName(transactionName, Priority.CORE_MAX);
                } else {
                    context.setTransactionName(transactionName, Priority.CORE_PLUGIN);
                }
            } else {
                traceEntry = context.startTransaction(transactionType, transactionName,
                        MessageSupplier.create(request.uri()), timerName);
                if (setTransactionTypeWithCoreMaxPriority) {
                    context.setTransactionType(transactionType, Priority.CORE_MAX);
                }
                if (setTransactionNameWithCoreMaxPriority) {
                    context.setTransactionName(transactionName, Priority.CORE_MAX);
                }
            }
            return traceEntry;
        }

        @OnReturn
        public static void onReturn(@BindReturn @Nullable ScalaFuture future,
                                    final ThreadContext context, @BindTraveler @Nullable TraceEntry traceEntry,
                                    @BindClassMeta ScalaInvoker invoker) {
            if (traceEntry == null) {
                return;
            }
            if (future == null) {
                traceEntry.end();
                return;
            }
            Object callback = invoker.toScala(new Function1<Object, /*@Nullable*/ Void>() {
                @Override
                public @Nullable Void apply(Object v1) {
                    if (v1 instanceof ScalaFailure) {
                        context.setTransactionError(((ScalaFailure) v1).exception());
                    } else if (v1 instanceof ScalaSuccess) {
                        Object result = ((ScalaSuccess) v1).value();
                        if (result instanceof HttpResponse) {
                            int statusCode = ((HttpResponse) result).glowroot$status().intValue();
                            // FIXME handle statusCode
                        }
                    }
                    System.out.println("COMPLETED: " + v1);
                    context.setTransactionAsyncComplete();
                    return null;
                }
            });
            if (callback == null) {
                traceEntry.end();
                return;
            }
            Object directExecutor = invoker.getDirectExecutor();
            if (directExecutor == null) {
                traceEntry.end();
                return;
            }
            context.setTransactionAsync();
            traceEntry.end();
            future.glowroot$onComplete(callback, directExecutor);
        }

        @OnThrow
        public static void onThrow(@BindThrowable Throwable t, ThreadContext context,
                                   @BindTraveler @Nullable TraceEntry traceEntry) {
            if (traceEntry != null) {
                context.setTransactionError(t);
                traceEntry.endWithError(t);
            }
        }

        private static @Nullable String getTransactionNameFor26x(Request request,
                                                                 Object handlerDefTypedKey) {
            TypedMap attrs = request.glowroot$attrs();
            if (attrs == null) {
                return null;
            }
            ScalaOption handlerDefOption = attrs.glowroot$get(handlerDefTypedKey);
            if (handlerDefOption == null || !handlerDefOption.isDefined()) {
                return null;
            }
            Object handlerDefObj = handlerDefOption.get();
            if (!(handlerDefObj instanceof HandlerDef)) {
                return null;
            }
            HandlerDef handlerDef = (HandlerDef) handlerDefObj;
            if (useAltTransactionNaming.value()) {
                return getTransactionName(handlerDef.controller(), handlerDef.method());
            } else {
                String path = handlerDef.path();
                if (path == null) {
                    return null;
                }
                return Routes.simplifiedRoute(path);
            }
        }

        private static @Nullable String getTransactionNameFor23xThrough25x(Request request) {
            ScalaMap tags = request.glowroot$tags();
            if (tags == null) {
                return null;
            }
            if (useAltTransactionNaming.value()) {
                ScalaOption controllerOption = tags.glowroot$get("ROUTE_CONTROLLER");
                ScalaOption methodOption = tags.glowroot$get("ROUTE_ACTION_METHOD");
                if (controllerOption == null || !controllerOption.isDefined()
                        || methodOption == null || !methodOption.isDefined()) {
                    return null;
                }
                Object controller = controllerOption.get();
                Object method = methodOption.get();
                if (controller == null || method == null) {
                    return null;
                }
                return getTransactionName(controller.toString(), method.toString());
            } else {
                ScalaOption option = tags.glowroot$get("ROUTE_PATTERN");
                if (option == null || !option.isDefined()) {
                    return null;
                }
                Object route = option.get();
                if (route == null) {
                    return null;
                }
                return Routes.simplifiedRoute(route.toString());
            }
        }

        private static @Nullable String getTransactionName(@Nullable String controller,
                                                           @Nullable String method) {
            if (controller == null || method == null) {
                return null;
            }
            return getAltTransactionName(controller, method);
        }
    }

    @Pointcut(className = "views.html.*", methodName = "apply", methodParameterTypes = {".."},
            timerName = "play render", nestingGroup = "play-render")
    public static class RenderAdvice {

        private static final TimerName timerName = Agent.getTimerName(RenderAdvice.class);

        @OnBefore
        public static TraceEntry onBefore(ThreadContext context, @BindReceiver Object view) {
            String viewName = view.getClass().getSimpleName();
            // strip off trailing $
            viewName = viewName.substring(0, viewName.length() - 1);
            return context.startTraceEntry(MessageSupplier.create("play render: {}", viewName),
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

    private static String getAltTransactionName(String controllerName, String methodName) {
        int index = controllerName.lastIndexOf('.');
        if (index == -1) {
            return controllerName + "#" + methodName;
        } else {
            return controllerName.substring(index + 1) + "#" + methodName;
        }
    }
}
