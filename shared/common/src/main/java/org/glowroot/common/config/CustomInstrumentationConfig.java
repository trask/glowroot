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
package org.glowroot.common.config;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig;
import org.glowroot.wire.api.model.Proto.OptionalInt32;

@JsonSerialize
@Value.Immutable
public abstract class CustomInstrumentationConfig {

    private static final Logger logger = LoggerFactory.getLogger(CustomInstrumentationConfig.class);

    @Value.Default
    @JsonInclude(Include.NON_EMPTY)
    public String className() {
        return "";
    }

    @Value.Default
    @JsonInclude(Include.NON_EMPTY)
    public String classAnnotation() {
        return "";
    }

    @Value.Default
    @JsonInclude(Include.NON_EMPTY)
    public String subTypeRestriction() {
        return "";
    }

    @Value.Default
    @JsonInclude(Include.NON_EMPTY)
    public String superTypeRestriction() {
        return "";
    }

    // pointcuts with methodDeclaringClassName are no longer supported in 0.9.16, but included here
    // to help with transitioning of old instrumentation config
    @Deprecated
    @Value.Default
    @JsonInclude(Include.NON_EMPTY)
    public String methodDeclaringClassName() {
        return "";
    }

    @Value.Default
    @JsonInclude(Include.NON_EMPTY)
    public String methodName() {
        return "";
    }

    @Value.Default
    @JsonInclude(Include.NON_EMPTY)
    public String methodAnnotation() {
        return "";
    }

    // empty methodParameterTypes means match no-arg methods only
    public abstract ImmutableList<String> methodParameterTypes();

    @Value.Default
    @JsonInclude(Include.NON_EMPTY)
    public String methodReturnType() {
        return "";
    }

    // currently unused, but will have a purpose someday, e.g. to capture all public methods
    @JsonInclude(Include.NON_EMPTY)
    public abstract ImmutableList<MethodModifier> methodModifiers();

    @Value.Default
    @JsonInclude(Include.NON_EMPTY)
    public String nestingGroup() {
        return "";
    }

    @Value.Default
    @JsonInclude(Include.NON_EMPTY)
    public int order() {
        return 0;
    }

    public abstract CaptureKind captureKind();

    @Value.Default
    @JsonInclude(Include.NON_EMPTY)
    public String transactionType() {
        return "";
    }

    @Value.Default
    @JsonInclude(Include.NON_EMPTY)
    public String transactionNameTemplate() {
        return "";
    }

    @Value.Default
    @JsonInclude(Include.NON_EMPTY)
    public String transactionUserTemplate() {
        return "";
    }

    @JsonInclude(Include.NON_EMPTY)
    public abstract Map<String, String> transactionAttributeTemplates();

    // need to write zero since it is treated different from null
    @JsonInclude(Include.NON_NULL)
    public abstract @Nullable Integer transactionSlowThresholdMillis();

    @JsonInclude(Include.NON_NULL)
    public abstract @Nullable AlreadyInTransactionBehavior alreadyInTransactionBehavior();

    // corrected for data prior to 0.10.10
    @JsonIgnore
    @Value.Derived
    public @Nullable AlreadyInTransactionBehavior alreadyInTransactionBehaviorCorrected() {
        if (captureKind() == CaptureKind.TRANSACTION) {
            return MoreObjects.firstNonNull(alreadyInTransactionBehavior(),
                    AlreadyInTransactionBehavior.CAPTURE_TRACE_ENTRY);
        } else {
            return null;
        }
    }

    @Value.Default
    @JsonInclude(Include.NON_EMPTY)
    public boolean transactionOuter() {
        return false;
    }

    @Value.Default
    @JsonInclude(Include.NON_EMPTY)
    public String traceEntryMessageTemplate() {
        return "";
    }

    // need to write zero since it is treated different from null
    @JsonInclude(Include.NON_NULL)
    public abstract @Nullable Integer traceEntryStackThresholdMillis();

    @Value.Default
    @JsonInclude(Include.NON_EMPTY)
    public boolean traceEntryCaptureSelfNested() {
        return false;
    }

    @Value.Default
    @JsonInclude(Include.NON_EMPTY)
    public String timerName() {
        return "";
    }

    @JsonIgnore
    @Value.Derived
    public boolean isTimerOrGreater() {
        return captureKind() == CaptureKind.TIMER || captureKind() == CaptureKind.TRACE_ENTRY
                || captureKind() == CaptureKind.TRANSACTION;
    }

    @JsonIgnore
    @Value.Derived
    public boolean isTraceEntryOrGreater() {
        return captureKind() == CaptureKind.TRACE_ENTRY || captureKind() == CaptureKind.TRANSACTION;
    }

    @JsonIgnore
    @Value.Derived
    public boolean isTransaction() {
        return captureKind() == CaptureKind.TRANSACTION;
    }

    @JsonIgnore
    @Value.Derived
    public ImmutableList<String> validationErrors() {
        List<String> errors = Lists.newArrayList();
        if (className().isEmpty() && classAnnotation().isEmpty()) {
            errors.add("className and classAnnotation are both empty");
        }
        if (methodName().isEmpty() && methodAnnotation().isEmpty()) {
            errors.add("methodName and methodAnnotation are both empty");
        }
        if (isTimerOrGreater() && timerName().isEmpty()) {
            errors.add("timerName is empty");
        }
        if (captureKind() == CaptureKind.TRACE_ENTRY && traceEntryMessageTemplate().isEmpty()) {
            errors.add("traceEntryMessageTemplate is empty");
        }
        if (isTransaction() && transactionType().isEmpty()) {
            errors.add("transactionType is empty");
        }
        if (isTransaction() && transactionNameTemplate().isEmpty()) {
            errors.add("transactionNameTemplate is empty");
        }
        if (!timerName().matches("[a-zA-Z0-9 ]*")) {
            errors.add("timerName contains invalid characters: " + timerName());
        }
        if (!methodDeclaringClassName().isEmpty()) {
            errors.add("methodDeclaringClassName is no longer supported");
        }
        return ImmutableList.copyOf(errors);
    }

    public void logValidationErrorsIfAny() {
        List<String> errors = validationErrors();
        if (!errors.isEmpty()) {
            logger.error("invalid instrumentation config: {} - {}", Joiner.on(", ").join(errors),
                    this);
        }
    }

    public AgentConfig.CustomInstrumentationConfig toProto() {
        AgentConfig.CustomInstrumentationConfig.Builder builder =
                AgentConfig.CustomInstrumentationConfig.newBuilder()
                        .setClassName(className())
                        .setClassAnnotation(classAnnotation())
                        .setSubTypeRestriction(subTypeRestriction())
                        .setSuperTypeRestriction(superTypeRestriction())
                        // pointcuts with methodDeclaringClassName are no longer supported in
                        // 0.9.16, but included here to help with transitioning of old
                        // instrumentation config
                        .setMethodDeclaringClassName(methodDeclaringClassName())
                        .setMethodName(methodName())
                        .setMethodAnnotation(methodAnnotation())
                        .addAllMethodParameterType(methodParameterTypes())
                        .setMethodReturnType(methodReturnType());
        for (MethodModifier methodModifier : methodModifiers()) {
            builder.addMethodModifier(toProto(methodModifier));
        }
        builder.setNestingGroup(nestingGroup())
                .setOrder(order())
                .setCaptureKind(toProto(captureKind()))
                .setTransactionType(transactionType())
                .setTransactionNameTemplate(transactionNameTemplate())
                .setTransactionUserTemplate(transactionUserTemplate())
                .putAllTransactionAttributeTemplates(
                        transactionAttributeTemplates());
        Integer transactionSlowThresholdMillis = transactionSlowThresholdMillis();
        if (transactionSlowThresholdMillis != null) {
            builder.setTransactionSlowThresholdMillis(
                    OptionalInt32.newBuilder().setValue(transactionSlowThresholdMillis));
        }
        AlreadyInTransactionBehavior alreadyInTransactionBehavior =
                alreadyInTransactionBehaviorCorrected();
        if (alreadyInTransactionBehavior != null) {
            builder.setAlreadyInTransactionBehavior(toProto(alreadyInTransactionBehavior));
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

    public static ImmutableCustomInstrumentationConfig create(
            AgentConfig.CustomInstrumentationConfig config) {
        @SuppressWarnings("deprecation")
        ImmutableCustomInstrumentationConfig.Builder builder =
                ImmutableCustomInstrumentationConfig.builder()
                        .className(config.getClassName())
                        .classAnnotation(config.getClassAnnotation())
                        .subTypeRestriction(config.getSubTypeRestriction())
                        .superTypeRestriction(config.getSuperTypeRestriction())
                        // pointcuts with methodDeclaringClassName are no longer supported in
                        // 0.9.16, but
                        // included here to help with transitioning of old instrumentation config
                        .methodDeclaringClassName(config.getMethodDeclaringClassName())
                        .methodName(config.getMethodName())
                        .methodAnnotation(config.getMethodAnnotation())
                        .addAllMethodParameterTypes(config.getMethodParameterTypeList())
                        .methodReturnType(config.getMethodReturnType());
        for (AgentConfig.CustomInstrumentationConfig.MethodModifier methodModifier : config
                .getMethodModifierList()) {
            builder.addMethodModifiers(fromProto(methodModifier));
        }
        builder.nestingGroup(config.getNestingGroup())
                .order(config.getOrder())
                .captureKind(fromProto(config.getCaptureKind()))
                .transactionType(config.getTransactionType())
                .transactionNameTemplate(config.getTransactionNameTemplate())
                .transactionUserTemplate(config.getTransactionUserTemplate())
                .putAllTransactionAttributeTemplates(config.getTransactionAttributeTemplatesMap());
        if (config.hasTransactionSlowThresholdMillis()) {
            builder.transactionSlowThresholdMillis(
                    config.getTransactionSlowThresholdMillis().getValue());
        }
        if (config
                .getCaptureKind() == AgentConfig.CustomInstrumentationConfig.CaptureKind.TRANSACTION) {
            builder.alreadyInTransactionBehavior(
                    fromProto(config.getAlreadyInTransactionBehavior()));
        }
        builder.transactionOuter(config.getTransactionOuter())
                .traceEntryMessageTemplate(config.getTraceEntryMessageTemplate());
        if (config.hasTraceEntryStackThresholdMillis()) {
            builder.traceEntryStackThresholdMillis(
                    config.getTraceEntryStackThresholdMillis().getValue());
        }
        return builder.traceEntryCaptureSelfNested(config.getTraceEntryCaptureSelfNested())
                .timerName(config.getTimerName())
                .build();
    }

    private static AgentConfig.CustomInstrumentationConfig.MethodModifier toProto(
            MethodModifier methodModifier) {
        return AgentConfig.CustomInstrumentationConfig.MethodModifier
                .valueOf(methodModifier.name());
    }

    private static AgentConfig.CustomInstrumentationConfig.CaptureKind toProto(
            CaptureKind captureKind) {
        return AgentConfig.CustomInstrumentationConfig.CaptureKind.valueOf(captureKind.name());
    }

    private static AgentConfig.CustomInstrumentationConfig.AlreadyInTransactionBehavior toProto(
            AlreadyInTransactionBehavior alreadyInTransactionBehavior) {
        return AgentConfig.CustomInstrumentationConfig.AlreadyInTransactionBehavior
                .valueOf(alreadyInTransactionBehavior.name());
    }

    private static MethodModifier fromProto(
            AgentConfig.CustomInstrumentationConfig.MethodModifier methodModifier) {
        return MethodModifier.valueOf(methodModifier.name());
    }

    private static CaptureKind fromProto(
            AgentConfig.CustomInstrumentationConfig.CaptureKind captureKind) {
        return CaptureKind.valueOf(captureKind.name());
    }

    private static AlreadyInTransactionBehavior fromProto(
            AgentConfig.CustomInstrumentationConfig.AlreadyInTransactionBehavior alreadyInTransactionBehavior) {
        return AlreadyInTransactionBehavior.valueOf(alreadyInTransactionBehavior.name());
    }

    public enum MethodModifier {
        PUBLIC, STATIC, NOT_STATIC;
    }

    public enum CaptureKind {
        TRANSACTION, TRACE_ENTRY, TIMER, OTHER
    }

    public enum AlreadyInTransactionBehavior {
        CAPTURE_TRACE_ENTRY, CAPTURE_NEW_TRANSACTION, DO_NOTHING
    }
}
