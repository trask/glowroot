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
package org.glowroot.xyzzy.instrumentation.log4j2x;

import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.xyzzy.test.harness.AppUnderTest;
import org.glowroot.xyzzy.test.harness.Container;
import org.glowroot.xyzzy.test.harness.Containers;
import org.glowroot.xyzzy.test.harness.IncomingSpan;
import org.glowroot.xyzzy.test.harness.LocalSpan;
import org.glowroot.xyzzy.test.harness.Span;
import org.glowroot.xyzzy.test.harness.TransactionMarker;

import static org.assertj.core.api.Assertions.assertThat;

public class Log4j2xIT {

    private static final String INSTRUMENTATION_ID = "log4j-2x";

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
        container.resetAfterEachTest();
    }

    @Test
    public void testLog() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID,
                "traceErrorOnErrorWithoutThrowable", true);

        // when
        IncomingSpan incomingSpan = container.execute(ShouldLog.class);

        // then
        assertThat(incomingSpan.getError().message()).isEqualTo("efg");

        Iterator<Span> i = incomingSpan.childSpans().iterator();

        LocalSpan localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage())
                .isEqualTo("log warn: " + ShouldLog.logger.getName() + " - def");
        assertThat(localSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage())
                .isEqualTo("log error: " + ShouldLog.logger.getName() + " - efg");
        assertThat(localSpan.childSpans()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testLogWithThrowable() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID,
                "traceErrorOnErrorWithoutThrowable", true);

        // when
        IncomingSpan incomingSpan = container.execute(ShouldLogWithThrowable.class);

        // then
        assertThat(incomingSpan.getError().message()).isEqualTo("efg_t");
        assertThat(incomingSpan.getError().exception()).isInstanceOf(IllegalStateException.class);
        assertThat(incomingSpan.getError().exception().getMessage()).isEqualTo("567");

        Iterator<Span> i = incomingSpan.childSpans().iterator();

        LocalSpan localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage())
                .isEqualTo("log warn: " + ShouldLogWithThrowable.logger.getName() + " - def_t");
        assertThat(localSpan.getError().message()).isNull();
        assertThat(localSpan.getError().exception()).isInstanceOf(IllegalStateException.class);
        assertThat(localSpan.getError().exception().getMessage()).isEqualTo("456");
        assertThat(localSpan.getError().exception().getStackTrace()[0].getMethodName())
                .isEqualTo("transactionMarker");
        assertThat(localSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage())
                .isEqualTo("log error: " + ShouldLogWithThrowable.logger.getName() + " - efg_t");
        assertThat(localSpan.getError().message()).isNull();
        assertThat(localSpan.getError().exception()).isInstanceOf(IllegalStateException.class);
        assertThat(localSpan.getError().exception().getMessage()).isEqualTo("567");
        assertThat(localSpan.getError().exception().getStackTrace()[0].getMethodName())
                .isEqualTo("transactionMarker");
        assertThat(localSpan.childSpans()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testLogWithNullThrowable() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID,
                "traceErrorOnErrorWithoutThrowable", true);

        // when
        IncomingSpan incomingSpan = container.execute(ShouldLogWithNullThrowable.class);

        // then
        assertThat(incomingSpan.getError().message()).isEqualTo("efg_tnull");

        Iterator<Span> i = incomingSpan.childSpans().iterator();

        LocalSpan localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo(
                "log warn: " + ShouldLogWithNullThrowable.logger.getName() + " - def_tnull");
        assertThat(localSpan.getError().message()).isEqualTo("def_tnull");
        assertThat(localSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo(
                "log error: " + ShouldLogWithNullThrowable.logger.getName() + " - efg_tnull");
        assertThat(localSpan.getError().message()).isEqualTo("efg_tnull");
        assertThat(localSpan.childSpans()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testLogWithOneParameter() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ShouldLogWithOneParameter.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        LocalSpan localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo(
                "log warn: " + ShouldLogWithOneParameter.logger.getName() + " - def_1 d");
        assertThat(localSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo(
                "log error: " + ShouldLogWithOneParameter.logger.getName() + " - efg_1 e");
        assertThat(localSpan.childSpans()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testLogWithOneParameterAndThrowable() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ShouldLogWithOneParameterAndThrowable.class);

        // then
        assertThat(incomingSpan.getError().message()).isEqualTo("efg_1_t e");
        assertThat(incomingSpan.getError().exception()).isInstanceOf(IllegalStateException.class);
        assertThat(incomingSpan.getError().exception().getMessage()).isEqualTo("567");

        Iterator<Span> i = incomingSpan.childSpans().iterator();

        LocalSpan localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo("log warn: "
                + ShouldLogWithOneParameterAndThrowable.logger.getName() + " - def_1_t d");
        assertThat(localSpan.getError().message()).isNull();
        assertThat(localSpan.getError().exception()).isInstanceOf(IllegalStateException.class);
        assertThat(localSpan.getError().exception().getMessage()).isEqualTo("456");
        assertThat(localSpan.getError().exception().getStackTrace()[0].getMethodName())
                .isEqualTo("transactionMarker");
        assertThat(localSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo("log error: "
                + ShouldLogWithOneParameterAndThrowable.logger.getName() + " - efg_1_t e");
        assertThat(localSpan.getError().message()).isNull();
        assertThat(localSpan.getError().exception()).isInstanceOf(IllegalStateException.class);
        assertThat(localSpan.getError().exception().getMessage()).isEqualTo("567");
        assertThat(localSpan.getError().exception().getStackTrace()[0].getMethodName())
                .isEqualTo("transactionMarker");
        assertThat(localSpan.childSpans()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testLogWithTwoParameters() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ShouldLogWithTwoParameters.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        LocalSpan localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo(
                "log warn: " + ShouldLogWithTwoParameters.logger.getName() + " - def_2 d e");
        assertThat(localSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo(
                "log error: " + ShouldLogWithTwoParameters.logger.getName() + " - efg_2 e f");
        assertThat(localSpan.childSpans()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testLogWithMoreThanTwoParameters() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ShouldLogWithMoreThanTwoParameters.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        LocalSpan localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo("log warn: "
                + ShouldLogWithMoreThanTwoParameters.logger.getName() + " - def_3 d e f");
        assertThat(localSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo("log error: "
                + ShouldLogWithMoreThanTwoParameters.logger.getName() + " - efg_3 e f g");
        assertThat(localSpan.childSpans()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testLogWithParametersAndThrowable() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ShouldLogWithParametersAndThrowable.class);

        // then
        Iterator<Span> i = incomingSpan.childSpans().iterator();

        LocalSpan localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo("log warn: "
                + ShouldLogWithParametersAndThrowable.logger.getName() + " - def_3_t d e f");
        assertThat(localSpan.getError().message()).isNull();
        assertThat(localSpan.getError().exception()).isInstanceOf(IllegalStateException.class);
        assertThat(localSpan.getError().exception().getMessage()).isEqualTo("456");
        assertThat(localSpan.getError().exception().getStackTrace()[0].getMethodName())
                .isEqualTo("transactionMarker");
        assertThat(localSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo("log error: "
                + ShouldLogWithParametersAndThrowable.logger.getName() + " - efg_3_t e f g");
        assertThat(localSpan.getError().message()).isNull();
        assertThat(localSpan.getError().exception()).isInstanceOf(IllegalStateException.class);
        assertThat(localSpan.getError().exception().getMessage()).isEqualTo("567");
        assertThat(localSpan.getError().exception().getStackTrace()[0].getMethodName())
                .isEqualTo("transactionMarker");
        assertThat(localSpan.childSpans()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    public static class ShouldLog implements AppUnderTest, TransactionMarker {
        private static final Logger logger = LogManager.getLogger(ShouldLog.class);
        @Override
        public void executeApp() {
            transactionMarker();
        }
        @Override
        public void transactionMarker() {
            logger.debug("bcd");
            logger.info("cde");
            logger.warn("def");
            logger.error("efg");
        }
    }

    public static class ShouldLogWithThrowable implements AppUnderTest, TransactionMarker {
        private static final Logger logger = LogManager.getLogger(ShouldLogWithThrowable.class);
        @Override
        public void executeApp() {
            transactionMarker();
        }
        @Override
        public void transactionMarker() {
            logger.debug("bcd_t", new IllegalStateException("234"));
            logger.info("cde_t", new IllegalStateException("345"));
            logger.warn("def_t", new IllegalStateException("456"));
            logger.error("efg_t", new IllegalStateException("567"));
        }
    }

    public static class ShouldLogWithNullThrowable implements AppUnderTest, TransactionMarker {
        private static final Logger logger =
                LogManager.getLogger(ShouldLogWithNullThrowable.class);
        @Override
        public void executeApp() {
            transactionMarker();
        }
        @Override
        public void transactionMarker() {
            logger.debug("bcd_tnull", (Throwable) null);
            logger.info("cde_tnull", (Throwable) null);
            logger.warn("def_tnull", (Throwable) null);
            logger.error("efg_tnull", (Throwable) null);
        }
    }

    public static class ShouldLogWithOneParameter implements AppUnderTest, TransactionMarker {
        private static final Logger logger =
                LogManager.getLogger(ShouldLogWithOneParameter.class);
        @Override
        public void executeApp() {
            transactionMarker();
        }
        @Override
        public void transactionMarker() {
            logger.debug("bcd_1 {}", "b");
            logger.info("cde_1 {}", "c");
            logger.warn("def_1 {}", "d");
            logger.error("efg_1 {}", "e");
        }
    }

    public static class ShouldLogWithOneParameterAndThrowable
            implements AppUnderTest, TransactionMarker {
        private static final Logger logger =
                LogManager.getLogger(ShouldLogWithOneParameterAndThrowable.class);
        @Override
        public void executeApp() {
            transactionMarker();
        }
        @Override
        public void transactionMarker() {
            logger.debug("bcd_1_t {}", "b", new IllegalStateException("234"));
            logger.info("cde_1_t {}", "c", new IllegalStateException("345"));
            logger.warn("def_1_t {}", "d", new IllegalStateException("456"));
            logger.error("efg_1_t {}", "e", new IllegalStateException("567"));
        }
    }

    public static class ShouldLogWithTwoParameters implements AppUnderTest, TransactionMarker {
        private static final Logger logger =
                LogManager.getLogger(ShouldLogWithTwoParameters.class);
        @Override
        public void executeApp() {
            transactionMarker();
        }
        @Override
        public void transactionMarker() {
            logger.debug("bcd_2 {} {}", "b", "c");
            logger.info("cde_2 {} {}", "c", "d");
            logger.warn("def_2 {} {}", "d", "e");
            logger.error("efg_2 {} {}", "e", "f");
        }
    }

    public static class ShouldLogWithMoreThanTwoParameters
            implements AppUnderTest, TransactionMarker {
        private static final Logger logger =
                LogManager.getLogger(ShouldLogWithMoreThanTwoParameters.class);
        @Override
        public void executeApp() {
            transactionMarker();
        }
        @Override
        public void transactionMarker() {
            logger.debug("bcd_3 {} {} {}", new Object[] {"b", "c", "d"});
            logger.info("cde_3 {} {} {}", new Object[] {"c", "d", "e"});
            logger.warn("def_3 {} {} {}", new Object[] {"d", "e", "f"});
            logger.error("efg_3 {} {} {}", new Object[] {"e", "f", "g"});
        }
    }

    public static class ShouldLogWithParametersAndThrowable
            implements AppUnderTest, TransactionMarker {
        private static final Logger logger =
                LogManager.getLogger(ShouldLogWithParametersAndThrowable.class);
        @Override
        public void executeApp() {
            transactionMarker();
        }
        @Override
        public void transactionMarker() {
            logger.debug("bcd_3_t {} {} {}",
                    new Object[] {"b", "c", "d", new IllegalStateException("234")});
            logger.info("cde_3_t {} {} {}",
                    new Object[] {"c", "d", "e", new IllegalStateException("345")});
            logger.warn("def_3_t {} {} {}",
                    new Object[] {"d", "e", "f", new IllegalStateException("456")});
            logger.error("efg_3_t {} {} {}",
                    new Object[] {"e", "f", "g", new IllegalStateException("567")});
        }
    }
}
