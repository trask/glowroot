/*
 * Copyright 2015-2019 the original author or authors.
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

import org.junit.Test;

import org.glowroot.common.config.CustomInstrumentationConfig.CaptureKind;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomInstrumentationConfigValidationTest {

    private final CustomInstrumentationConfig baseConfig =
            ImmutableCustomInstrumentationConfig.builder()
                    .className("a")
                    .methodName("n")
                    .addMethodParameterTypes("java.lang.String")
                    .methodReturnType("")
                    .captureKind(CaptureKind.TIMER)
                    .timerName("t")
                    .traceEntryMessageTemplate("")
                    .traceEntryCaptureSelfNested(false)
                    .transactionType("")
                    .transactionNameTemplate("")
                    .transactionUserTemplate("")
                    .build();

    @Test
    public void testValid() {
        // when
        List<String> validationErrors = baseConfig.validationErrors();
        // then
        assertThat(validationErrors).isEmpty();
    }

    @Test
    public void testInvalidClassNameAndMethodName() {
        // given
        CustomInstrumentationConfig config = ImmutableCustomInstrumentationConfig.builder()
                .copyFrom(baseConfig)
                .className("")
                .methodName("")
                .build();
        // when
        List<String> validationErrors = config.validationErrors();
        // then
        assertThat(validationErrors).containsExactly(
                "className and classAnnotation are both empty",
                "methodName and methodAnnotation are both empty");
    }

    @Test
    public void testInvalidEmptyTimerName() {
        // given
        CustomInstrumentationConfig config = ImmutableCustomInstrumentationConfig.builder()
                .copyFrom(baseConfig)
                .timerName("")
                .build();
        // when
        List<String> validationErrors = config.validationErrors();
        // then
        assertThat(validationErrors).containsExactly("timerName is empty");
    }

    @Test
    public void testInvalidCharactersInTimerName() {
        // given
        CustomInstrumentationConfig config = ImmutableCustomInstrumentationConfig.builder()
                .copyFrom(baseConfig)
                .timerName("a_b")
                .build();
        // when
        List<String> validationErrors = config.validationErrors();
        // then
        assertThat(validationErrors).containsExactly("timerName contains invalid characters: a_b");
    }

    @Test
    public void testValidEmptyTimerName() {
        // given
        CustomInstrumentationConfig config = ImmutableCustomInstrumentationConfig.builder()
                .copyFrom(baseConfig)
                .captureKind(CaptureKind.OTHER)
                .timerName("")
                .build();
        // when
        List<String> validationErrors = config.validationErrors();
        // then
        assertThat(validationErrors).isEmpty();
    }

    @Test
    public void testInvalidTraceEntry() {
        // given
        CustomInstrumentationConfig config = ImmutableCustomInstrumentationConfig.builder()
                .copyFrom(baseConfig)
                .captureKind(CaptureKind.TRACE_ENTRY)
                .build();
        // when
        List<String> validationErrors = config.validationErrors();
        // then
        assertThat(validationErrors).containsExactly("traceEntryMessageTemplate is empty");
    }

    @Test
    public void testInvalidTransaction() {
        // given
        CustomInstrumentationConfig config = ImmutableCustomInstrumentationConfig.builder()
                .copyFrom(baseConfig)
                .captureKind(CaptureKind.TRANSACTION)
                .build();
        // when
        List<String> validationErrors = config.validationErrors();
        // then
        assertThat(validationErrors).containsExactly("transactionType is empty",
                "transactionNameTemplate is empty");
    }
}
