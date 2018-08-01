/*
 * Copyright 2018 the original author or authors.
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
package org.glowroot.agent.plugin.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import org.glowroot.agent.it.harness.Container;
import org.glowroot.agent.it.harness.impl.JavaagentContainer;
import org.glowroot.wire.api.model.TraceOuterClass.Trace;

import static com.google.common.base.Charsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class RequestBodyIT {

    private static final String PLUGIN_ID = "servlet";

    private static Container container;

    @BeforeClass
    public static void setUp() throws Exception {
        container = JavaagentContainer.create();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        container.close();
    }

    @After
    public void afterEachTest() throws Exception {
        container.checkAndReset();
    }

    @Test
    public void testCaptureRequestBody() throws Exception {
        // given
        container.getConfigService().setPluginProperty(PLUGIN_ID, "captureRequestBodyNumBytes",
                10.0);

        // when
        Trace trace = container.execute(ReadRequestBody.class, "Web");

        // then
        Trace.DetailEntry detailEntry = ResponseHeaderIT.getDetailEntry(trace, "Request body");
        assertThat(detailEntry).isNotNull();
        assertThat(detailEntry.getValueCount()).isEqualTo(1);
        Trace.DetailValue detailValue = detailEntry.getValue(0);
        assertThat(detailValue.getString())
                .isEqualTo(BaseEncoding.base64().encode("abcdefghij".getBytes(UTF_8)));
    }

    @SuppressWarnings("serial")
    public static class ReadRequestBody extends TestServlet {
        @Override
        protected void before(HttpServletRequest request, HttpServletResponse response) {
            ((MockHttpServletRequest) request)
                    .setContent("abcdefghijklmnopqrstuvwxyz".getBytes(UTF_8));
            ((MockHttpServletRequest) request).setMethod("POST");
        }
        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response)
                throws IOException {
            ByteStreams.exhaust(request.getInputStream());
        }
    }
}
