/*
 * Copyright 2011-2018 the original author or authors.
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
package org.glowroot.agent.it.harness;

import java.util.List;

import org.glowroot.agent.it.harness.model.ServerSpan;

public interface Container {

    void setInstrumentationProperty(String instrumentationId, String propertyName,
            boolean propertyValue) throws Exception;

    void setInstrumentationProperty(String instrumentationId, String propertyName,
            Double propertyValue) throws Exception;

    void setInstrumentationProperty(String instrumentationId, String propertyName,
            String propertyValue) throws Exception;

    void setInstrumentationProperty(String instrumentationId, String propertyName,
            List<String> propertyValue) throws Exception;

    ServerSpan execute(Class<? extends AppUnderTest> appUnderTestClass) throws Exception;

    ServerSpan execute(Class<? extends AppUnderTest> appUnderTestClass, String transactionType)
            throws Exception;

    ServerSpan execute(Class<? extends AppUnderTest> appUnderTestClass, String transactionType,
            String transactionName) throws Exception;

    void executeNoExpectedTrace(Class<? extends AppUnderTest> appUnderTestClass) throws Exception;

    // checks no unexpected log messages
    // checks no active traces
    // resets instrumentation properties to defaults
    void resetConfig() throws Exception;

    void close() throws Exception;
}
