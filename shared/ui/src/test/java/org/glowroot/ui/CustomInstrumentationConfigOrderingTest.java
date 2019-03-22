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
package org.glowroot.ui;

import com.google.common.collect.Ordering;
import org.junit.Test;

import org.glowroot.ui.CustomInstrumentationJsonService.CustomInstrumentationConfigOrdering;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.CustomInstrumentationConfig;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.CustomInstrumentationConfig.CaptureKind;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomInstrumentationConfigOrderingTest {

    private final CustomInstrumentationConfig left = CustomInstrumentationConfig.newBuilder()
            .setClassName("a")
            .setMethodName("n")
            .addMethodParameterType("java.lang.String")
            .setMethodReturnType("")
            .setCaptureKind(CaptureKind.TIMER)
            .setTimerName("t")
            .setTraceEntryMessageTemplate("")
            .setTraceEntryCaptureSelfNested(false)
            .setTransactionType("")
            .setTransactionNameTemplate("")
            .setTransactionUserTemplate("")
            .build();

    private final CustomInstrumentationConfig right = CustomInstrumentationConfig.newBuilder()
            .setClassName("b")
            .setMethodName("m")
            .setMethodReturnType("")
            .setCaptureKind(CaptureKind.TIMER)
            .setTimerName("t")
            .setTraceEntryMessageTemplate("")
            .setTraceEntryCaptureSelfNested(false)
            .setTransactionType("")
            .setTransactionNameTemplate("")
            .setTransactionUserTemplate("")
            .build();

    @Test
    public void testDifferentClassNames() {
        // given
        Ordering<CustomInstrumentationConfig> ordering = new CustomInstrumentationConfigOrdering();
        // when
        int compare = ordering.compare(left, right);
        // then
        assertThat(compare).isNegative();
    }

    @Test
    public void testSameClassNames() {
        // given
        Ordering<CustomInstrumentationConfig> ordering = new CustomInstrumentationConfigOrdering();
        // when
        int compare = ordering.compare(left, right.toBuilder().setClassName("a").build());
        // then
        assertThat(compare).isPositive();
    }

    @Test
    public void testSameClassAndMethodNames() {
        // given
        Ordering<CustomInstrumentationConfig> ordering = new CustomInstrumentationConfigOrdering();
        // when
        int compare = ordering.compare(left.toBuilder().setMethodName("m").build(),
                right.toBuilder().setClassName("a").build());
        // then
        assertThat(compare).isPositive();
    }

    @Test
    public void testSameClassAndMethodNamesAndParamCount() {
        // given
        Ordering<CustomInstrumentationConfig> ordering = new CustomInstrumentationConfigOrdering();
        // when
        int compare =
                ordering.compare(left.toBuilder().setMethodName("m").build(), right.toBuilder()
                        .setClassName("a").addMethodParameterType("java.lang.Throwable").build());
        // then
        assertThat(compare).isNegative();
    }

    @Test
    public void testSameEverything() {
        // given
        Ordering<CustomInstrumentationConfig> ordering = new CustomInstrumentationConfigOrdering();
        // when
        int compare = ordering.compare(left.toBuilder().setMethodName("m").build(), right
                .toBuilder().setClassName("a").addMethodParameterType("java.lang.String").build());
        // then
        assertThat(compare).isZero();
    }
}
