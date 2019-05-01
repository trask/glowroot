/*
 * Copyright 2017-2019 the original author or authors.
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
package org.glowroot.xyzzy.instrumentation.javahttpserver;

import java.io.IOException;
import java.io.Serializable;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

import org.glowroot.xyzzy.test.harness.AppUnderTest;

@SuppressWarnings("restriction")
class TestFilter extends Filter implements AppUnderTest {

    @Override
    public void executeApp(Serializable... args) throws Exception {
        MockHttpExchange exchange = new MockHttpExchange("GET", "/testfilter");
        doFilter(exchange, null);
    }

    @Override
    public String description() {
        return getClass().getSimpleName();
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {}
}
