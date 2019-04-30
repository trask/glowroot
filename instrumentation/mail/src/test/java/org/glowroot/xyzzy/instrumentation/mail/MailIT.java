/**
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
package org.glowroot.xyzzy.instrumentation.mail;

import java.util.Iterator;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.xyzzy.test.harness.AppUnderTest;
import org.glowroot.xyzzy.test.harness.OutgoingSpan;
import org.glowroot.xyzzy.test.harness.Container;
import org.glowroot.xyzzy.test.harness.Containers;
import org.glowroot.xyzzy.test.harness.IncomingSpan;
import org.glowroot.xyzzy.test.harness.Span;
import org.glowroot.xyzzy.test.harness.TransactionMarker;

import static org.assertj.core.api.Assertions.assertThat;

public class MailIT {

    private static Container container;

    @BeforeClass
    public static void setUp() throws Exception {
        container = Containers.create();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        container.close();
    }

    @After
    public void afterEachTest() throws Exception {
        container.resetInstrumentationProperties();
    }

    @Test
    public void shouldSendMessage() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ExecuteSend.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        OutgoingSpan outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage()).startsWith("mail connect smtp://");

        outgoingSpan = (OutgoingSpan) i.next();
        assertThat(outgoingSpan.getMessage()).isEqualTo("mail send message");

        assertThat(i.hasNext()).isFalse();
    }

    public static class ExecuteSend extends DoMail {
        @Override
        public void transactionMarker() {
            GreenMailUtil.sendTextEmailTest("to@localhost.com", "from@localhost.com",
                    "some subject", "some body");
        }
    }

    private abstract static class DoMail implements AppUnderTest, TransactionMarker {
        @Override
        public void executeApp() throws Exception {
            GreenMail greenMail = new GreenMail(); // uses test ports by default
            greenMail.start();
            try {
                transactionMarker();
            } finally {
                greenMail.stop();
            }
        }
    }
}
