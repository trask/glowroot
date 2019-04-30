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
package org.glowroot.xyzzy.instrumentation.spring;

import java.time.Duration;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.glowroot.agent.it.harness.Container;
import org.glowroot.agent.it.harness.TraceEntryMarker;
import org.glowroot.agent.it.harness.impl.JavaagentContainer;
import org.glowroot.wire.api.model.TraceOuterClass.Trace;

import static org.assertj.core.api.Assertions.assertThat;

public class WebFluxRestControllerIT {

    private static Container container;

    @BeforeClass
    public static void setUp() throws Exception {
        // System.setProperty("glowroot.test.xdebug", "true");
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
    public void shouldCaptureWebFlux1() throws Exception {
        // when
        Trace trace = container.execute(HittingWebFlux1.class, "Web");

        // then
        assertThat(trace.getHeader().getTransactionName()).isEqualTo("/webflux1");

        Iterator<Trace.Entry> i = trace.getEntryList().iterator();

        Trace.Entry entry = i.next();
        assertThat(entry.getDepth()).isEqualTo(0);
        assertThat(entry.getMessage()).isEqualTo("spring controller:"
                + " org.glowroot.xyzzy.instrumentation.spring.WebFluxRestControllerIT$TestRestController"
                + ".webflux1()");

        entry = i.next();
        assertThat(entry.getDepth()).isEqualTo(1);
        assertThat(entry.getMessage()).isEqualTo("test local span");

        entry = i.next();
        assertThat(entry.getDepth()).isEqualTo(1);
        assertThat(entry.getMessage()).isEqualTo("auxiliary thread");

        entry = i.next();
        assertThat(entry.getDepth()).isEqualTo(2);
        assertThat(entry.getMessage()).isEqualTo("test local span");

        assertThat(i.hasNext()).isFalse();
    }

    @Test
    public void shouldCaptureWebFlux2() throws Exception {
        // when
        Trace trace = container.execute(HittingWebFlux2.class, "Web");

        // then
        assertThat(trace.getHeader().getTransactionName()).isEqualTo("/webflux2");

        Iterator<Trace.Entry> i = trace.getEntryList().iterator();

        Trace.Entry entry = i.next();
        assertThat(entry.getDepth()).isEqualTo(0);
        assertThat(entry.getMessage()).isEqualTo("spring controller:"
                + " org.glowroot.xyzzy.instrumentation.spring.WebFluxRestControllerIT$TestRestController"
                + ".webflux2()");

        entry = i.next();
        assertThat(entry.getDepth()).isEqualTo(1);
        assertThat(entry.getMessage()).isEqualTo("test local span");

        assertThat(i.hasNext()).isFalse();
    }

    public static class HittingWebFlux1 extends InvokeSpringControllerInTomcat {
        @Override
        public void executeApp() throws Exception {
            executeApp("webapp1", "", "/webflux1");
        }
    }

    public static class HittingWebFlux2 extends InvokeSpringControllerInTomcat {
        @Override
        public void executeApp() throws Exception {
            try {
                executeApp("webapp1", "", "/webflux2");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @RestController
    public static class TestRestController {

        @RequestMapping("webflux1")
        public Mono<String> webflux1() {
            new CreateLocalSpan().traceEntryMarker();
            CompletableFuture<String> completableFuture = new CompletableFuture<>();
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                    new CreateLocalSpan().traceEntryMarker();
                    completableFuture.complete("1");
                }
            });
            return Mono.fromFuture(completableFuture);
        }

        @RequestMapping("webflux2")
        public Flux<Long> webflux2() {
            new Exception().printStackTrace();
            new CreateLocalSpan().traceEntryMarker();
            return Flux.interval(Duration.ofSeconds(1));
        }
    }

    private static class CreateLocalSpan implements TraceEntryMarker {
        @Override
        public void traceEntryMarker() {}
    }
}
