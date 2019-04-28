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
package org.glowroot.agent.it.harness.validation;

import java.util.List;

import org.assertj.core.api.AbstractCharSequenceAssert;

import org.glowroot.agent.it.harness.model.ClientSpan;
import org.glowroot.agent.it.harness.model.LocalSpan;
import org.glowroot.agent.it.harness.model.ServerSpan;
import org.glowroot.agent.it.harness.model.Span;

import static org.assertj.core.api.Assertions.assertThat;

public class HarnessAssertions {

    public static AbstractCharSequenceAssert<?, String> assertSingleClientSpanMessage(
            ServerSpan serverSpan) {

        List<Span> spans = serverSpan.childSpans();

        assertThat(spans).hasSize(1);

        Span span = spans.get(0);
        assertThat(span).isInstanceOf(ClientSpan.class);

        return assertThat(span.getMessage());
    }

    public static AbstractCharSequenceAssert<?, String> assertSingleLocalSpanMessage(
            ServerSpan serverSpan) {

        List<Span> spans = serverSpan.childSpans();

        assertThat(spans).hasSize(1);

        return assertSingleLocalSpanMessage(spans.get(0));
    }

    public static AbstractCharSequenceAssert<?, String> assertSingleLocalSpanMessage(Span span) {

        assertThat(span).isInstanceOf(LocalSpan.class);
        assertThat(((LocalSpan) span).childSpans()).isEmpty();

        return assertThat(span.getMessage());
    }
}
