/*
 * Copyright 2016-2019 the original author or authors.
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
package org.glowroot.xyzzy.instrumentation.redis;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.Jedis;

import org.glowroot.xyzzy.test.harness.AppUnderTest;
import org.glowroot.xyzzy.test.harness.Container;
import org.glowroot.xyzzy.test.harness.Containers;
import org.glowroot.xyzzy.test.harness.IncomingSpan;
import org.glowroot.xyzzy.test.harness.TransactionMarker;

import static org.glowroot.xyzzy.test.harness.util.HarnessAssertions.assertSingleOutgoingSpanMessage;

public class ConnectionIT {

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
        container.resetInstrumentationConfig();
    }

    @Test
    public void shouldTraceSet() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(JedisSet.class);

        // then
        assertSingleOutgoingSpanMessage(incomingSpan).matches("redis localhost:\\d+ SET");
    }

    @Test
    public void shouldTraceGet() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(JedisGet.class);

        // then
        assertSingleOutgoingSpanMessage(incomingSpan).matches("redis localhost:\\d+ GET");
    }

    @Test
    public void shouldTracePing() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(JedisPing.class);

        // then
        assertSingleOutgoingSpanMessage(incomingSpan).matches("redis localhost:\\d+ PING");
    }

    private abstract static class JedisBase implements AppUnderTest, TransactionMarker {

        private RedisMockServer redisMockServer;

        private Jedis jedis;

        @Override
        public void executeApp() throws Exception {
            redisMockServer = new RedisMockServer();
            jedis = new Jedis("localhost", redisMockServer.getPort());
            transactionMarker();
            redisMockServer.close();
        }

        protected Jedis getJedis() {
            return jedis;
        }
    }

    public static class JedisSet extends JedisBase implements TransactionMarker {
        @Override
        public void transactionMarker() {
            getJedis().set("key", "value");
        }
    }

    public static class JedisGet extends JedisBase implements TransactionMarker {
        @Override
        public void transactionMarker() {
            getJedis().get("key");
        }
    }

    public static class JedisPing extends JedisBase implements TransactionMarker {
        @Override
        public void transactionMarker() {
            getJedis().ping();
        }
    }
}
