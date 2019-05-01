/*
 * Copyright 2014-2019 the original author or authors.
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
package org.glowroot.xyzzy.instrumentation.logger;

import java.util.Iterator;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
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

public class Log4j1xIT {

    private static final String INSTRUMENTATION_ID = "log4j-1x";

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
                .isEqualTo("log warn: o.g.x.i.logger.Log4j1xIT$ShouldLog - def");
        assertThat(localSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage())
                .isEqualTo("log error: o.g.x.i.logger.Log4j1xIT$ShouldLog - efg");
        assertThat(localSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage())
                .isEqualTo("log fatal: o.g.x.i.logger.Log4j1xIT$ShouldLog - fgh");
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
        assertThat(incomingSpan.getError().message()).isEqualTo("efg_");
        assertThat(incomingSpan.getError().exception()).isInstanceOf(IllegalStateException.class);
        assertThat(incomingSpan.getError().exception().getMessage()).isEqualTo("567");

        Iterator<Span> i = incomingSpan.childSpans().iterator();

        LocalSpan localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage())
                .isEqualTo("log warn: o.g.x.i.l.Log4j1xIT$ShouldLogWithThrowable - def_");
        assertThat(localSpan.getError().message()).isNull();
        assertThat(localSpan.getError().exception()).isInstanceOf(IllegalStateException.class);
        assertThat(localSpan.getError().exception().getMessage()).isEqualTo("456");
        assertThat(localSpan.getError().exception().getStackTrace()[0].getMethodName())
                .isEqualTo("transactionMarker");
        assertThat(localSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage())
                .isEqualTo("log error: o.g.x.i.l.Log4j1xIT$ShouldLogWithThrowable - efg_");
        assertThat(localSpan.getError().message()).isNull();
        assertThat(localSpan.getError().exception()).isInstanceOf(IllegalStateException.class);
        assertThat(localSpan.getError().exception().getMessage()).isEqualTo("567");
        assertThat(localSpan.getError().exception().getStackTrace()[0].getMethodName())
                .isEqualTo("transactionMarker");
        assertThat(localSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage())
                .isEqualTo("log fatal: o.g.x.i.l.Log4j1xIT$ShouldLogWithThrowable - fgh_");
        assertThat(localSpan.getError().message()).isNull();
        assertThat(localSpan.getError().exception()).isInstanceOf(IllegalStateException.class);
        assertThat(localSpan.getError().exception().getMessage()).isEqualTo("678");
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
        assertThat(incomingSpan.getError().message()).isEqualTo("efg_");

        Iterator<Span> i = incomingSpan.childSpans().iterator();

        LocalSpan localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage())
                .isEqualTo("log warn: o.g.x.i.l.Log4j1xIT$ShouldLogWithNullThrowable - def_");
        assertThat(localSpan.getError().message()).isEqualTo("def_");
        assertThat(localSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage())
                .isEqualTo("log error: o.g.x.i.l.Log4j1xIT$ShouldLogWithNullThrowable - efg_");
        assertThat(localSpan.getError().message()).isEqualTo("efg_");
        assertThat(localSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage())
                .isEqualTo("log fatal: o.g.x.i.l.Log4j1xIT$ShouldLogWithNullThrowable - fgh_");
        assertThat(localSpan.getError().message()).isEqualTo("fgh_");
        assertThat(localSpan.childSpans()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testLogWithPriority() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID,
                "traceErrorOnErrorWithoutThrowable", true);

        // when
        IncomingSpan incomingSpan = container.execute(ShouldLogWithPriority.class);

        // then
        assertThat(incomingSpan.getError().message()).isEqualTo("efg__");

        Iterator<Span> i = incomingSpan.childSpans().iterator();

        LocalSpan localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage())
                .isEqualTo("log warn: o.g.x.i.l.Log4j1xIT$ShouldLogWithPriority - def__");
        assertThat(localSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage())
                .isEqualTo("log error: o.g.x.i.l.Log4j1xIT$ShouldLogWithPriority - efg__");
        assertThat(localSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage())
                .isEqualTo("log fatal: o.g.x.i.l.Log4j1xIT$ShouldLogWithPriority - fgh__");
        assertThat(localSpan.childSpans()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testLogWithPriorityAndThrowable() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ShouldLogWithPriorityAndThrowable.class);

        // then
        assertThat(incomingSpan.getError().exception()).isInstanceOf(IllegalStateException.class);
        assertThat(incomingSpan.getError().exception().getMessage()).isEqualTo("567_");

        Iterator<Span> i = incomingSpan.childSpans().iterator();

        LocalSpan localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo(
                "log warn: o.g.x.i.l.Log4j1xIT$ShouldLogWithPriorityAndThrowable - def___");
        assertThat(localSpan.getError().message()).isNull();
        assertThat(localSpan.getError().exception()).isInstanceOf(IllegalStateException.class);
        assertThat(localSpan.getError().exception().getMessage()).isEqualTo("456_");
        assertThat(localSpan.getError().exception().getStackTrace()[0].getMethodName())
                .isEqualTo("transactionMarker");
        assertThat(localSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo(
                "log error: o.g.x.i.l.Log4j1xIT$ShouldLogWithPriorityAndThrowable - efg___");
        assertThat(localSpan.getError().message()).isNull();
        assertThat(localSpan.getError().exception()).isInstanceOf(IllegalStateException.class);
        assertThat(localSpan.getError().exception().getMessage()).isEqualTo("567_");
        assertThat(localSpan.getError().exception().getStackTrace()[0].getMethodName())
                .isEqualTo("transactionMarker");
        assertThat(localSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo(
                "log fatal: o.g.x.i.l.Log4j1xIT$ShouldLogWithPriorityAndThrowable - fgh___");
        assertThat(localSpan.getError().message()).isNull();
        assertThat(localSpan.getError().exception()).isInstanceOf(IllegalStateException.class);
        assertThat(localSpan.getError().exception().getMessage()).isEqualTo("678_");
        assertThat(localSpan.getError().exception().getStackTrace()[0].getMethodName())
                .isEqualTo("transactionMarker");
        assertThat(localSpan.childSpans()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testLogWithPriorityAndNullThrowable() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID,
                "traceErrorOnErrorWithoutThrowable", true);

        // when
        IncomingSpan incomingSpan = container.execute(ShouldLogWithPriorityAndNullThrowable.class);

        // then
        assertThat(incomingSpan.getError().message()).isEqualTo("efg___null");

        Iterator<Span> i = incomingSpan.childSpans().iterator();

        LocalSpan localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo(
                "log warn: o.g.x.i.l.Log4j1xIT$ShouldLogWithPriorityAndNullThrowable - def___null");
        assertThat(localSpan.getError().message()).isEqualTo("def___null");
        assertThat(localSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo(
                "log error: o.g.x.i.l.Log4j1xIT$ShouldLogWithPriorityAndNullThrowable - efg___null");
        assertThat(localSpan.getError().message()).isEqualTo("efg___null");
        assertThat(localSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo(
                "log fatal: o.g.x.i.l.Log4j1xIT$ShouldLogWithPriorityAndNullThrowable - fgh___null");
        assertThat(localSpan.getError().message()).isEqualTo("fgh___null");
        assertThat(localSpan.childSpans()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testLocalizedLog() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ShouldLocalizedLog.class);

        // then
        assertThat(incomingSpan.getError().message()).isEqualTo("efg____");
        assertThat(incomingSpan.getError().exception()).isInstanceOf(IllegalStateException.class);
        assertThat(incomingSpan.getError().exception().getMessage()).isEqualTo("567__");

        Iterator<Span> i = incomingSpan.childSpans().iterator();

        LocalSpan localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage())
                .isEqualTo("log warn: o.g.x.i.l.Log4j1xIT$ShouldLocalizedLog - def____");
        assertThat(localSpan.getError().message()).isNull();
        assertThat(localSpan.getError().exception()).isInstanceOf(IllegalStateException.class);
        assertThat(localSpan.getError().exception().getMessage()).isEqualTo("456__");
        assertThat(localSpan.getError().exception().getStackTrace()[0].getMethodName())
                .isEqualTo("transactionMarker");
        assertThat(localSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage())
                .isEqualTo("log error: o.g.x.i.l.Log4j1xIT$ShouldLocalizedLog - efg____");
        assertThat(localSpan.getError().message()).isNull();
        assertThat(localSpan.getError().exception()).isInstanceOf(IllegalStateException.class);
        assertThat(localSpan.getError().exception().getMessage()).isEqualTo("567__");
        assertThat(localSpan.getError().exception().getStackTrace()[0].getMethodName())
                .isEqualTo("transactionMarker");
        assertThat(localSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage())
                .isEqualTo("log fatal: o.g.x.i.l.Log4j1xIT$ShouldLocalizedLog - fgh____");
        assertThat(localSpan.getError().message()).isNull();
        assertThat(localSpan.getError().exception()).isInstanceOf(IllegalStateException.class);
        assertThat(localSpan.getError().exception().getMessage()).isEqualTo("678__");
        assertThat(localSpan.getError().exception().getStackTrace()[0].getMethodName())
                .isEqualTo("transactionMarker");
        assertThat(localSpan.childSpans()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testLocalizedLogWithNullThrowable() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID,
                "traceErrorOnErrorWithoutThrowable", true);

        // when
        IncomingSpan incomingSpan = container.execute(ShouldLocalizedLogWithNullThrowable.class);

        // then
        assertThat(incomingSpan.getError().message()).isEqualTo("efg____null");

        Iterator<Span> i = incomingSpan.childSpans().iterator();

        LocalSpan localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo(
                "log warn: o.g.x.i.l.Log4j1xIT$ShouldLocalizedLogWithNullThrowable - def____null");
        assertThat(localSpan.getError().message()).isEqualTo("def____null");
        assertThat(localSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo(
                "log error: o.g.x.i.l.Log4j1xIT$ShouldLocalizedLogWithNullThrowable - efg____null");
        assertThat(localSpan.getError().message()).isEqualTo("efg____null");
        assertThat(localSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo(
                "log fatal: o.g.x.i.l.Log4j1xIT$ShouldLocalizedLogWithNullThrowable - fgh____null");
        assertThat(localSpan.getError().message()).isEqualTo("fgh____null");
        assertThat(localSpan.childSpans()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testLocalizedLogWithParameters() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ShouldLocalizedLogWithParameters.class);

        // then
        assertThat(incomingSpan.getError().exception()).isInstanceOf(IllegalStateException.class);
        assertThat(incomingSpan.getError().exception().getMessage()).isEqualTo("567__");

        Iterator<Span> i = incomingSpan.childSpans().iterator();

        LocalSpan localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo(
                "log warn: o.g.x.i.l.Log4j1xIT$ShouldLocalizedLogWithParameters - def____");
        assertThat(localSpan.getError().message()).isNull();
        assertThat(localSpan.getError().exception()).isInstanceOf(IllegalStateException.class);
        assertThat(localSpan.getError().exception().getMessage()).isEqualTo("456__");
        assertThat(localSpan.getError().exception().getStackTrace()[0].getMethodName())
                .isEqualTo("transactionMarker");
        assertThat(localSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo(
                "log error: o.g.x.i.l.Log4j1xIT$ShouldLocalizedLogWithParameters - efg____");
        assertThat(localSpan.getError().message()).isNull();
        assertThat(localSpan.getError().exception()).isInstanceOf(IllegalStateException.class);
        assertThat(localSpan.getError().exception().getMessage()).isEqualTo("567__");
        assertThat(localSpan.getError().exception().getStackTrace()[0].getMethodName())
                .isEqualTo("transactionMarker");
        assertThat(localSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo(
                "log fatal: o.g.x.i.l.Log4j1xIT$ShouldLocalizedLogWithParameters - fgh____");
        assertThat(localSpan.getError().message()).isNull();
        assertThat(localSpan.getError().exception()).isInstanceOf(IllegalStateException.class);
        assertThat(localSpan.getError().exception().getMessage()).isEqualTo("678__");
        assertThat(localSpan.getError().exception().getStackTrace()[0].getMethodName())
                .isEqualTo("transactionMarker");
        assertThat(localSpan.childSpans()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testLocalizedLogWithEmptyParameters() throws Exception {
        // when
        IncomingSpan incomingSpan = container.execute(ShouldLocalizedLogWithEmptyParameters.class);

        // then
        assertThat(incomingSpan.getError().exception()).isInstanceOf(IllegalStateException.class);
        assertThat(incomingSpan.getError().exception().getMessage()).isEqualTo("567__");

        Iterator<Span> i = incomingSpan.childSpans().iterator();

        LocalSpan localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo(
                "log warn: o.g.x.i.l.Log4j1xIT$ShouldLocalizedLogWithEmptyParameters - def____");
        assertThat(localSpan.getError().message()).isNull();
        assertThat(localSpan.getError().exception()).isInstanceOf(IllegalStateException.class);
        assertThat(localSpan.getError().exception().getMessage()).isEqualTo("456__");
        assertThat(localSpan.getError().exception().getStackTrace()[0].getMethodName())
                .isEqualTo("transactionMarker");
        assertThat(localSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo(
                "log error: o.g.x.i.l.Log4j1xIT$ShouldLocalizedLogWithEmptyParameters - efg____");
        assertThat(localSpan.getError().message()).isNull();
        assertThat(localSpan.getError().exception()).isInstanceOf(IllegalStateException.class);
        assertThat(localSpan.getError().exception().getMessage()).isEqualTo("567__");
        assertThat(localSpan.getError().exception().getStackTrace()[0].getMethodName())
                .isEqualTo("transactionMarker");
        assertThat(localSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo(
                "log fatal: o.g.x.i.l.Log4j1xIT$ShouldLocalizedLogWithEmptyParameters - fgh____");
        assertThat(localSpan.getError().message()).isNull();
        assertThat(localSpan.getError().exception()).isInstanceOf(IllegalStateException.class);
        assertThat(localSpan.getError().exception().getMessage()).isEqualTo("678__");
        assertThat(localSpan.getError().exception().getStackTrace()[0].getMethodName())
                .isEqualTo("transactionMarker");
        assertThat(localSpan.childSpans()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void testLocalizedLogWithParametersAndNullThrowable() throws Exception {
        // given
        container.setInstrumentationProperty(INSTRUMENTATION_ID,
                "traceErrorOnErrorWithoutThrowable", true);

        // when
        IncomingSpan incomingSpan =
                container.execute(ShouldLocalizedLogWithParametersAndNullThrowable.class);

        // then
        assertThat(incomingSpan.getError().message()).isEqualTo("efg____null");

        Iterator<Span> i = incomingSpan.childSpans().iterator();

        LocalSpan localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo("log warn: o.g.x.i.l.Log4j1xIT"
                + "$ShouldLocalizedLogWithParametersAndNullThrowable - def____null");
        assertThat(localSpan.getError().message()).isEqualTo("def____null");
        assertThat(localSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo("log error: o.g.x.i.l.Log4j1xIT"
                + "$ShouldLocalizedLogWithParametersAndNullThrowable - efg____null");
        assertThat(localSpan.getError().message()).isEqualTo("efg____null");
        assertThat(localSpan.childSpans()).isEmpty();

        localSpan = (LocalSpan) i.next();
        assertThat(localSpan.getMessage()).isEqualTo("log fatal: o.g.x.i.l.Log4j1xIT"
                + "$ShouldLocalizedLogWithParametersAndNullThrowable - fgh____null");
        assertThat(localSpan.getError().message()).isEqualTo("fgh____null");
        assertThat(localSpan.childSpans()).isEmpty();

        assertThat(i.hasNext()).isFalse();
    }

    public static class ShouldLog implements AppUnderTest, TransactionMarker {
        private static final Logger logger = Logger.getLogger(ShouldLog.class);
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
            logger.fatal("fgh");
        }
    }

    public static class ShouldLogWithThrowable implements AppUnderTest, TransactionMarker {
        private static final Logger logger = Logger.getLogger(ShouldLogWithThrowable.class);
        @Override
        public void executeApp() {
            transactionMarker();
        }
        @Override
        public void transactionMarker() {
            logger.debug("bcd_", new IllegalStateException("234"));
            logger.info("cde_", new IllegalStateException("345"));
            logger.warn("def_", new IllegalStateException("456"));
            logger.error("efg_", new IllegalStateException("567"));
            logger.fatal("fgh_", new IllegalStateException("678"));
        }
    }

    public static class ShouldLogWithNullThrowable implements AppUnderTest, TransactionMarker {
        private static final Logger logger = Logger.getLogger(ShouldLogWithNullThrowable.class);
        @Override
        public void executeApp() {
            transactionMarker();
        }
        @Override
        public void transactionMarker() {
            logger.debug("bcd_", null);
            logger.info("cde_", null);
            logger.warn("def_", null);
            logger.error("efg_", null);
            logger.fatal("fgh_", null);
        }
    }

    public static class ShouldLogWithPriority implements AppUnderTest, TransactionMarker {
        private static final Logger logger = Logger.getLogger(ShouldLogWithPriority.class);
        @Override
        public void executeApp() {
            transactionMarker();
        }
        @Override
        public void transactionMarker() {
            try {
                logger.log(null, "abc__");
            } catch (NullPointerException e) {
                // re-throw if it does not originate from log4j
                if (!e.getStackTrace()[0].getClassName().startsWith("org.apache.log4j.")) {
                    throw e;
                }
            }
            logger.log(Level.DEBUG, "bcd__");
            logger.log(Level.INFO, "cde__");
            logger.log(Level.WARN, "def__");
            logger.log(Level.ERROR, "efg__");
            logger.log(Level.FATAL, "fgh__");
        }
    }

    public static class ShouldLogWithPriorityAndThrowable
            implements AppUnderTest, TransactionMarker {
        private static final Logger logger =
                Logger.getLogger(ShouldLogWithPriorityAndThrowable.class);
        @Override
        public void executeApp() {
            transactionMarker();
        }
        @Override
        public void transactionMarker() {
            try {
                logger.log(null, "abc___", new IllegalStateException("123_"));
            } catch (NullPointerException e) {
                // re-throw if it does not originate from log4j
                if (!e.getStackTrace()[0].getClassName().startsWith("org.apache.log4j.")) {
                    throw e;
                }
            }
            logger.log(Level.DEBUG, "bcd___", new IllegalStateException("234_"));
            logger.log(Level.INFO, "cde___", new IllegalStateException("345_"));
            logger.log(Level.WARN, "def___", new IllegalStateException("456_"));
            logger.log(Level.ERROR, "efg___", new IllegalStateException("567_"));
            logger.log(Level.FATAL, "fgh___", new IllegalStateException("678_"));
        }
    }

    public static class ShouldLogWithPriorityAndNullThrowable
            implements AppUnderTest, TransactionMarker {
        private static final Logger logger =
                Logger.getLogger(ShouldLogWithPriorityAndNullThrowable.class);
        @Override
        public void executeApp() {
            transactionMarker();
        }
        @Override
        public void transactionMarker() {
            logger.log(Level.DEBUG, "bcd___null", null);
            logger.log(Level.INFO, "cde___null", null);
            logger.log(Level.WARN, "def___null", null);
            logger.log(Level.ERROR, "efg___null", null);
            logger.log(Level.FATAL, "fgh___null", null);
        }
    }

    public static class ShouldLocalizedLog implements AppUnderTest, TransactionMarker {
        private static final Logger logger = Logger.getLogger(ShouldLocalizedLog.class);
        @Override
        public void executeApp() {
            transactionMarker();
        }
        @Override
        public void transactionMarker() {
            try {
                logger.l7dlog(null, "abc____", new IllegalStateException("123__"));
            } catch (NullPointerException e) {
                // re-throw if it does not originate from log4j
                if (!e.getStackTrace()[0].getClassName().startsWith("org.apache.log4j.")) {
                    throw e;
                }
            }
            logger.l7dlog(Level.DEBUG, "bcd____", new IllegalStateException("234__"));
            logger.l7dlog(Level.INFO, "cde____", new IllegalStateException("345__"));
            logger.l7dlog(Level.WARN, "def____", new IllegalStateException("456__"));
            logger.l7dlog(Level.ERROR, "efg____", new IllegalStateException("567__"));
            logger.l7dlog(Level.FATAL, "fgh____", new IllegalStateException("678__"));
        }
    }

    public static class ShouldLocalizedLogWithNullThrowable
            implements AppUnderTest, TransactionMarker {
        private static final Logger logger =
                Logger.getLogger(ShouldLocalizedLogWithNullThrowable.class);
        @Override
        public void executeApp() {
            transactionMarker();
        }
        @Override
        public void transactionMarker() {
            logger.l7dlog(Level.DEBUG, "bcd____null", null);
            logger.l7dlog(Level.INFO, "cde____null", null);
            logger.l7dlog(Level.WARN, "def____null", null);
            logger.l7dlog(Level.ERROR, "efg____null", null);
            logger.l7dlog(Level.FATAL, "fgh____null", null);
        }
    }

    public static class ShouldLocalizedLogWithParameters
            implements AppUnderTest, TransactionMarker {
        private static final Logger logger =
                Logger.getLogger(ShouldLocalizedLogWithParameters.class);
        @Override
        public void executeApp() {
            transactionMarker();
        }
        @Override
        public void transactionMarker() {
            try {
                logger.l7dlog(null, "abc____", new Object[] {"a", "b", "c"},
                        new IllegalStateException("123__"));
            } catch (NullPointerException e) {
                // re-throw if it does not originate from log4j
                if (!e.getStackTrace()[0].getClassName().startsWith("org.apache.log4j.")) {
                    throw e;
                }
            }
            logger.l7dlog(Level.DEBUG, "bcd____", new Object[] {"b", "c", "d"},
                    new IllegalStateException("234__"));
            logger.l7dlog(Level.INFO, "cde____", new Object[] {"c", "d", "e"},
                    new IllegalStateException("345__"));
            logger.l7dlog(Level.WARN, "def____", new Object[] {"d", "e", "f"},
                    new IllegalStateException("456__"));
            logger.l7dlog(Level.ERROR, "efg____", new Object[] {"e", "f", "g"},
                    new IllegalStateException("567__"));
            logger.l7dlog(Level.FATAL, "fgh____", new Object[] {"f", "g", "h"},
                    new IllegalStateException("678__"));
        }
    }

    public static class ShouldLocalizedLogWithEmptyParameters
            implements AppUnderTest, TransactionMarker {
        private static final Logger logger =
                Logger.getLogger(ShouldLocalizedLogWithEmptyParameters.class);
        @Override
        public void executeApp() {
            transactionMarker();
        }
        @Override
        public void transactionMarker() {
            try {
                logger.l7dlog(null, "abc____", new Object[] {"a", "b", "c"},
                        new IllegalStateException("123__"));
            } catch (NullPointerException e) {
                // re-throw if it does not originate from log4j
                if (!e.getStackTrace()[0].getClassName().startsWith("org.apache.log4j.")) {
                    throw e;
                }
            }
            logger.l7dlog(Level.DEBUG, "bcd____", new Object[] {},
                    new IllegalStateException("234__"));
            logger.l7dlog(Level.INFO, "cde____", new Object[] {},
                    new IllegalStateException("345__"));
            logger.l7dlog(Level.WARN, "def____", new Object[] {},
                    new IllegalStateException("456__"));
            logger.l7dlog(Level.ERROR, "efg____", new Object[] {},
                    new IllegalStateException("567__"));
            logger.l7dlog(Level.FATAL, "fgh____", new Object[] {},
                    new IllegalStateException("678__"));
        }
    }

    public static class ShouldLocalizedLogWithParametersAndNullThrowable
            implements AppUnderTest, TransactionMarker {
        private static final Logger logger =
                Logger.getLogger(ShouldLocalizedLogWithParametersAndNullThrowable.class);
        @Override
        public void executeApp() {
            transactionMarker();
        }
        @Override
        public void transactionMarker() {
            logger.l7dlog(Level.DEBUG, "bcd____null", new Object[] {"b_", "c_", "d_"}, null);
            logger.l7dlog(Level.INFO, "cde____null", new Object[] {"c_", "d_", "e_"}, null);
            logger.l7dlog(Level.WARN, "def____null", new Object[] {"d_", "e_", "f_"}, null);
            logger.l7dlog(Level.ERROR, "efg____null", new Object[] {"e_", "f_", "g_"}, null);
            logger.l7dlog(Level.FATAL, "fgh____null", new Object[] {"f_", "g_", "h_"}, null);
        }
    }
}
