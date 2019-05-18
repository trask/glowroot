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
package org.glowroot.agent.tests;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.agent.it.harness.AppUnderTest;
import org.glowroot.agent.it.harness.Container;
import org.glowroot.agent.it.harness.Containers;
import org.glowroot.xyzzy.instrumentation.api.Agent;

public class BadInstrumentationIdIT {

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
        container.checkAndReset();
    }

    @Test
    public void shouldNotReadTrace() throws Exception {
        // given
        container.addExpectedLogMessage("org.glowroot.agent.impl.ConfigServiceImpl",
                "unexpected instrumentation id: not-to-be-found (available instrumentation ids are"
                        + " glowroot-integration-tests, glowroot-it-harness)");
        // when
        container.executeNoExpectedTrace(BadInstrumentationId.class);
        // then
    }

    public static class BadInstrumentationId implements AppUnderTest {
        @Override
        public void executeApp() {
            Agent.getConfigService("not-to-be-found");
        }
    }
}
