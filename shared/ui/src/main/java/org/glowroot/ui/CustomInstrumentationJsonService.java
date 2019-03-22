/*
 * Copyright 2013-2019 the original author or authors.
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
package org.glowroot.ui;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.glowroot.common.live.LiveJvmService;
import org.glowroot.common.live.LiveJvmService.AgentNotConnectedException;
import org.glowroot.common.live.LiveWeavingService;
import org.glowroot.common.util.ObjectMappers;
import org.glowroot.common.util.Versions;
import org.glowroot.common2.repo.ConfigRepository;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.CustomInstrumentationConfig;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.CustomInstrumentationConfig.AlreadyInTransactionBehavior;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.CustomInstrumentationConfig.CaptureKind;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.CustomInstrumentationConfig.MethodModifier;
import org.glowroot.wire.api.model.DownstreamServiceOuterClass.GlobalMeta;
import org.glowroot.wire.api.model.DownstreamServiceOuterClass.MethodSignature;
import org.glowroot.wire.api.model.Proto.OptionalInt32;

import static com.google.common.base.Preconditions.checkNotNull;

@JsonService
class CustomInstrumentationJsonService {

    private static final Logger logger =
            LoggerFactory.getLogger(CustomInstrumentationJsonService.class);

    private static final ObjectMapper mapper = ObjectMappers.create();

    private static final Ordering<CustomInstrumentationConfig> ordering =
            new CustomInstrumentationConfigOrdering();

    private final boolean central;
    private final ConfigRepository configRepository;
    private final @Nullable LiveWeavingService liveWeavingService;
    private final @Nullable LiveJvmService liveJvmService;

    CustomInstrumentationJsonService(boolean central, ConfigRepository configRepository,
            @Nullable LiveWeavingService liveWeavingService,
            @Nullable LiveJvmService liveJvmService) {
        this.central = central;
        this.configRepository = configRepository;
        this.liveWeavingService = liveWeavingService;
        this.liveJvmService = liveJvmService;
    }

    @GET(path = "/backend/config/custom-instrumentation",
            permission = "agent:config:view:customInstrumentation")
    String getConfig(@BindAgentId String agentId,
            @BindRequest CustomInstrumentationReadRequest request)
            throws Exception {
        Optional<String> version = request.version();
        if (version.isPresent()) {
            return getConfigInternal(agentId, version.get());
        } else {
            List<CustomInstrumentationConfig> configs =
                    configRepository.getCustomInstrumentationConfig(agentId);
            configs = ordering.immutableSortedCopy(configs);
            List<CustomInstrumentationConfigDto> dtos = Lists.newArrayList();
            for (CustomInstrumentationConfig config : configs) {
                dtos.add(CustomInstrumentationConfigDto.create(config));
            }
            GlobalMeta globalMeta = null;
            if (liveWeavingService != null) {
                try {
                    globalMeta = liveWeavingService.getGlobalMeta(agentId);
                } catch (AgentNotConnectedException e) {
                    logger.debug(e.getMessage(), e);
                }
            }
            return mapper.writeValueAsString(ImmutableCustomInstrumentationListResponse.builder()
                    .addAllConfigs(dtos)
                    .jvmOutOfSync(globalMeta != null && globalMeta.getJvmOutOfSync())
                    .jvmRetransformClassesSupported(
                            globalMeta != null && globalMeta.getJvmRetransformClassesSupported())
                    .build());
        }
    }

    @GET(path = "/backend/config/preload-classpath-cache",
            permission = "agent:config:view:customInstrumentation")
    void preloadClasspathCache(final @BindAgentId String agentId) throws Exception {
        if (liveWeavingService == null) {
            return;
        }
        if (central) {
            try {
                liveWeavingService.preloadClasspathCache(agentId);
            } catch (AgentNotConnectedException e) {
                logger.debug(e.getMessage(), e);
            }
        } else {
            // HttpServer is configured with a very small thread pool to keep number of threads down
            // (currently only a single thread), so spawn a background thread to perform the
            // preloading so it doesn't block other http requests
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // TODO report checker framework issue that occurs without checkNotNull
                        checkNotNull(liveWeavingService);
                        liveWeavingService.preloadClasspathCache(agentId);
                    } catch (AgentNotConnectedException e) {
                        logger.debug(e.getMessage(), e);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            });
            thread.setDaemon(true);
            thread.setName("Glowroot-Temporary-Thread");
            thread.start();
        }
    }

    @GET(path = "/backend/config/new-custom-instrumentation-check-agent-connected",
            permission = "agent:config:edit:customInstrumentation")
    String checkAgentConnected(@BindAgentId String agentId) throws Exception {
        checkNotNull(liveJvmService); // agent:config:edit is disabled in offline viewer
        return Boolean.toString(liveJvmService.isAvailable(agentId));
    }

    @GET(path = "/backend/config/matching-class-names",
            permission = "agent:config:edit:customInstrumentation")
    String getMatchingClassNames(@BindAgentId String agentId,
            @BindRequest ClassNamesRequest request) throws Exception {
        checkNotNull(liveWeavingService); // agent:config:edit is disabled in offline viewer
        return mapper.writeValueAsString(liveWeavingService.getMatchingClassNames(agentId,
                request.partialClassName(), request.limit()));
    }

    @GET(path = "/backend/config/matching-method-names",
            permission = "agent:config:edit:customInstrumentation")
    String getMatchingMethodNames(@BindAgentId String agentId,
            @BindRequest MethodNamesRequest request) throws Exception {
        checkNotNull(liveWeavingService); // agent:config:edit is disabled in offline viewer
        List<String> matchingMethodNames = liveWeavingService.getMatchingMethodNames(agentId,
                request.className(), request.partialMethodName(), request.limit());
        return mapper.writeValueAsString(matchingMethodNames);
    }

    @GET(path = "/backend/config/method-signatures",
            permission = "agent:config:edit:customInstrumentation")
    String getMethodSignatures(@BindAgentId String agentId,
            @BindRequest MethodSignaturesRequest request) throws Exception {
        checkNotNull(liveWeavingService); // agent:config:edit is disabled in offline viewer
        List<MethodSignature> signatures = liveWeavingService.getMethodSignatures(agentId,
                request.className(), request.methodName());
        List<MethodSignatureDto> methodSignatures = Lists.newArrayList();
        for (MethodSignature signature : signatures) {
            methodSignatures.add(MethodSignatureDto.create(signature));
        }
        return mapper.writeValueAsString(methodSignatures);
    }

    @POST(path = "/backend/config/custom-instrumentation/add",
            permission = "agent:config:edit:customInstrumentation")
    String addCustomInstrumentationConfig(@BindAgentId String agentId,
            @BindRequest CustomInstrumentationConfigDto configDto) throws Exception {
        CustomInstrumentationConfig config = configDto.convert();
        configRepository.insertCustomInstrumentationConfig(agentId, config);
        return getConfigInternal(agentId, Versions.getVersion(config));
    }

    @POST(path = "/backend/config/custom-instrumentation/update",
            permission = "agent:config:edit:customInstrumentation")
    String updateCustomInstrumentationConfig(@BindAgentId String agentId,
            @BindRequest CustomInstrumentationConfigDto configDto) throws Exception {
        CustomInstrumentationConfig config = configDto.convert();
        String version = configDto.version().get();
        configRepository.updateCustomInstrumentationConfig(agentId, config, version);
        return getConfigInternal(agentId, Versions.getVersion(config));
    }

    @POST(path = "/backend/config/custom-instrumentation/remove",
            permission = "agent:config:edit:customInstrumentation")
    void removeCustomInstrumentationConfig(@BindAgentId String agentId,
            @BindRequest CustomInstrumentationDeleteRequest request) throws Exception {
        configRepository.deleteCustomInstrumentationConfigs(agentId, request.versions());
    }

    @POST(path = "/backend/config/custom-instrumentation/import",
            permission = "agent:config:edit:customInstrumentation")
    void importCustomInstrumentationConfig(@BindAgentId String agentId,
            @BindRequest CustomInstrumentationImportRequest request) throws Exception {
        List<CustomInstrumentationConfig> configs = Lists.newArrayList();
        for (CustomInstrumentationConfigDto configDto : request.configs()) {
            configs.add(configDto.convert());
        }
        configRepository.insertCustomInstrumentationConfigs(agentId, configs);
    }

    @POST(path = "/backend/config/reweave", permission = "agent:config:edit:customInstrumentation")
    String reweave(@BindAgentId String agentId) throws Exception {
        checkNotNull(liveWeavingService); // agent:config:edit is disabled in offline viewer
        int count = liveWeavingService.reweave(agentId);
        return "{\"classes\":" + count + "}";
    }

    private String getConfigInternal(String agentId, String version) throws Exception {
        CustomInstrumentationConfig config =
                configRepository.getCustomInstrumentationConfig(agentId, version);
        if (config == null) {
            throw new JsonServiceException(HttpResponseStatus.NOT_FOUND);
        }
        List<MethodSignature> methodSignatures = null;
        if (liveWeavingService != null) {
            try {
                methodSignatures = liveWeavingService.getMethodSignatures(agentId,
                        config.getClassName(), config.getMethodName());
            } catch (AgentNotConnectedException e) {
                logger.debug(e.getMessage(), e);
            }
        }

        ImmutableCustomInstrumentationConfigResponse.Builder builder =
                ImmutableCustomInstrumentationConfigResponse.builder()
                        .agentNotConnected(methodSignatures == null)
                        .config(CustomInstrumentationConfigDto.create(config));
        if (methodSignatures == null) {
            // agent not connected
            List<String> modifiers = Lists.newArrayList();
            if (!isSignatureAll(config)) {
                for (MethodModifier modifier : config.getMethodModifierList()) {
                    modifiers.add(modifier.name());
                }
                builder.addMethodSignatures(ImmutableMethodSignatureDto.builder()
                        .name(config.getMethodName())
                        .parameterTypes(config.getMethodParameterTypeList())
                        .returnType(config.getMethodReturnType())
                        .modifiers(modifiers)
                        .build());
            }
        } else {
            for (MethodSignature methodSignature : methodSignatures) {
                builder.addMethodSignatures(MethodSignatureDto.create(methodSignature));
            }
        }
        return mapper.writeValueAsString(builder.build());
    }

    private static boolean isSignatureAll(CustomInstrumentationConfig config) {
        return config.getMethodModifierCount() == 0 && config.getMethodReturnType().isEmpty()
                && config.getMethodParameterTypeCount() == 1
                && config.getMethodParameterType(0).equals("..");
    }

    @Value.Immutable
    interface CustomInstrumentationReadRequest {
        Optional<String> version();
    }

    @Value.Immutable
    interface ClassNamesRequest {
        String partialClassName();
        int limit();
    }

    @Value.Immutable
    interface MethodNamesRequest {
        String className();
        String partialMethodName();
        int limit();
    }

    @Value.Immutable
    interface MethodSignaturesRequest {
        String className();
        String methodName();
    }

    @Value.Immutable
    interface CustomInstrumentationListResponse {
        ImmutableList<CustomInstrumentationConfigDto> configs();
        boolean jvmOutOfSync();
        boolean jvmRetransformClassesSupported();
    }

    @Value.Immutable
    interface CustomInstrumentationConfigResponse {
        boolean agentNotConnected();
        CustomInstrumentationConfigDto config();
        ImmutableList<MethodSignatureDto> methodSignatures();
    }

    @Value.Immutable
    interface CustomInstrumentationErrorResponse {
        ImmutableList<String> errors();
    }

    @Value.Immutable
    interface CustomInstrumentationImportRequest {
        ImmutableList<ImmutableCustomInstrumentationConfigDto> configs();
    }

    @Value.Immutable
    interface CustomInstrumentationDeleteRequest {
        List<String> versions();
    }

    @Value.Immutable
    @JsonInclude(Include.ALWAYS)
    abstract static class CustomInstrumentationConfigDto {

        abstract String className();
        abstract String classAnnotation();
        abstract String subTypeRestriction();
        abstract String superTypeRestriction();
        // pointcuts with methodDeclaringClassName are no longer supported in 0.9.16, but
        // included here to help with transitioning of old instrumentation config
        //
        // also included here to support glowroot central 0.9.16 or newer running agent 0.9.15 or
        // older
        @Deprecated
        @JsonInclude(Include.NON_NULL)
        abstract @Nullable String methodDeclaringClassName();
        abstract String methodName();
        abstract String methodAnnotation();
        abstract ImmutableList<String> methodParameterTypes();
        abstract String methodReturnType();
        abstract ImmutableList<MethodModifier> methodModifiers();
        abstract String nestingGroup();
        abstract int order();
        abstract CaptureKind captureKind();
        abstract String transactionType();
        abstract String transactionNameTemplate();
        abstract String transactionUserTemplate();
        abstract Map<String, String> transactionAttributeTemplates();
        abstract @Nullable Integer transactionSlowThresholdMillis();
        abstract @Nullable AlreadyInTransactionBehavior alreadyInTransactionBehavior();
        abstract boolean transactionOuter();
        abstract String traceEntryMessageTemplate();
        abstract @Nullable Integer traceEntryStackThresholdMillis();
        abstract boolean traceEntryCaptureSelfNested();
        abstract String timerName();
        abstract Optional<String> version(); // absent for insert operations

        private CustomInstrumentationConfig convert() {
            CustomInstrumentationConfig.Builder builder = CustomInstrumentationConfig.newBuilder()
                    .setClassName(className())
                    .setClassAnnotation(classAnnotation())
                    .setSubTypeRestriction(subTypeRestriction())
                    .setSuperTypeRestriction(superTypeRestriction())
                    // pointcuts with methodDeclaringClassName are no longer supported in 0.9.16,
                    // but included here to help with transitioning of old instrumentation config
                    //
                    // also included here to support glowroot central 0.9.16 or newer running agent
                    // 0.9.15 or older
                    .setMethodDeclaringClassName(Strings.nullToEmpty(methodDeclaringClassName()))
                    .setMethodName(methodName())
                    .setMethodAnnotation(methodAnnotation())
                    .addAllMethodParameterType(methodParameterTypes())
                    .setMethodReturnType(methodReturnType())
                    .addAllMethodModifier(methodModifiers())
                    .setNestingGroup(nestingGroup())
                    .setOrder(order())
                    .setCaptureKind(captureKind())
                    .setTransactionType(transactionType())
                    .setTransactionNameTemplate(transactionNameTemplate())
                    .setTransactionUserTemplate(transactionUserTemplate())
                    .putAllTransactionAttributeTemplates(transactionAttributeTemplates());
            Integer transactionSlowThresholdMillis = transactionSlowThresholdMillis();
            if (transactionSlowThresholdMillis != null) {
                builder.setTransactionSlowThresholdMillis(
                        OptionalInt32.newBuilder().setValue(transactionSlowThresholdMillis));
            }
            AlreadyInTransactionBehavior alreadyInTransactionBehavior =
                    alreadyInTransactionBehavior();
            if (alreadyInTransactionBehavior != null) {
                builder.setAlreadyInTransactionBehavior(alreadyInTransactionBehavior);
            }
            builder.setTransactionOuter(transactionOuter())
                    .setTraceEntryMessageTemplate(traceEntryMessageTemplate());
            Integer traceEntryStackThresholdMillis = traceEntryStackThresholdMillis();
            if (traceEntryStackThresholdMillis != null) {
                builder.setTraceEntryStackThresholdMillis(
                        OptionalInt32.newBuilder().setValue(traceEntryStackThresholdMillis));
            }
            return builder.setTraceEntryCaptureSelfNested(traceEntryCaptureSelfNested())
                    .setTimerName(timerName())
                    .build();
        }

        private static CustomInstrumentationConfigDto create(CustomInstrumentationConfig config) {
            @SuppressWarnings("deprecation")
            ImmutableCustomInstrumentationConfigDto.Builder builder =
                    ImmutableCustomInstrumentationConfigDto.builder()
                            .className(config.getClassName())
                            .classAnnotation(config.getClassAnnotation())
                            .subTypeRestriction(config.getSubTypeRestriction())
                            .superTypeRestriction(config.getSuperTypeRestriction())
                            // pointcuts with methodDeclaringClassName are no longer supported in
                            // 0.9.16, but included here to help with transitioning of old
                            // instrumentation config
                            //
                            // also included here to support glowroot central 0.9.16 or newer
                            // running agent 0.9.15 or older
                            .methodDeclaringClassName(
                                    Strings.emptyToNull(config.getMethodDeclaringClassName()))
                            .methodName(config.getMethodName())
                            .methodAnnotation(config.getMethodAnnotation())
                            .addAllMethodParameterTypes(config.getMethodParameterTypeList())
                            .methodReturnType(config.getMethodReturnType())
                            .addAllMethodModifiers(config.getMethodModifierList())
                            .nestingGroup(config.getNestingGroup())
                            .order(config.getOrder())
                            .captureKind(config.getCaptureKind())
                            .transactionType(config.getTransactionType())
                            .transactionNameTemplate(config.getTransactionNameTemplate())
                            .transactionUserTemplate(config.getTransactionUserTemplate())
                            .putAllTransactionAttributeTemplates(
                                    config.getTransactionAttributeTemplatesMap());
            if (config.hasTransactionSlowThresholdMillis()) {
                builder.transactionSlowThresholdMillis(
                        config.getTransactionSlowThresholdMillis().getValue());
            }
            if (config.getCaptureKind() == CaptureKind.TRANSACTION) {
                builder.alreadyInTransactionBehavior(config.getAlreadyInTransactionBehavior());
            }
            builder.transactionOuter(config.getTransactionOuter())
                    .traceEntryMessageTemplate(config.getTraceEntryMessageTemplate());
            if (config.hasTraceEntryStackThresholdMillis()) {
                builder.traceEntryStackThresholdMillis(
                        config.getTraceEntryStackThresholdMillis().getValue());
            }
            return builder.traceEntryCaptureSelfNested(config.getTraceEntryCaptureSelfNested())
                    .timerName(config.getTimerName())
                    .version(Versions.getVersion(config))
                    .build();
        }
    }

    @Value.Immutable
    @JsonInclude(Include.ALWAYS)
    abstract static class MethodSignatureDto {

        abstract String name();
        abstract ImmutableList<String> parameterTypes();
        abstract String returnType();
        abstract ImmutableList<String> modifiers();

        private static MethodSignatureDto create(MethodSignature methodSignature) {
            return ImmutableMethodSignatureDto.builder()
                    .name(methodSignature.getName())
                    .addAllParameterTypes(methodSignature.getParameterTypeList())
                    .returnType(methodSignature.getReturnType())
                    .modifiers(methodSignature.getModifierList())
                    .build();
        }
    }

    @VisibleForTesting
    static class CustomInstrumentationConfigOrdering extends Ordering<CustomInstrumentationConfig> {
        @Override
        public int compare(CustomInstrumentationConfig left, CustomInstrumentationConfig right) {
            int compare = left.getClassName().compareToIgnoreCase(right.getClassName());
            if (compare != 0) {
                return compare;
            }
            compare = left.getMethodName().compareToIgnoreCase(right.getMethodName());
            if (compare != 0) {
                return compare;
            }
            List<String> leftParameterTypes = left.getMethodParameterTypeList();
            List<String> rightParameterTypes = right.getMethodParameterTypeList();
            compare = Ints.compare(leftParameterTypes.size(), rightParameterTypes.size());
            if (compare != 0) {
                return compare;
            }
            for (int i = 0; i < leftParameterTypes.size(); i++) {
                compare = leftParameterTypes.get(i).compareToIgnoreCase(rightParameterTypes.get(i));
                if (compare != 0) {
                    return compare;
                }
            }
            return 0;
        }
    }
}
