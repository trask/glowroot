/**
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
package org.glowroot.xyzzy.instrumentation.quartz;

import java.io.Serializable;
import java.util.Date;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import org.glowroot.xyzzy.test.harness.AppUnderTest;
import org.glowroot.xyzzy.test.harness.Container;
import org.glowroot.xyzzy.test.harness.Containers;
import org.glowroot.xyzzy.test.harness.IncomingSpan;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

public class QuartzIT {

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
    public void shouldCaptureJobExecution() throws Exception {
        IncomingSpan incomingSpan = container.execute(ExecuteJob.class);
        assertThat(incomingSpan.transactionType()).isEqualTo("Background");
        assertThat(incomingSpan.transactionName()).isEqualTo("Quartz job: ajob");
        assertThat(incomingSpan.getMessage()).isEqualTo("Quartz job: ajob");
    }

    private static JobDetail createJob1x() throws Exception {
        Class<?> clazz = Class.forName("org.quartz.JobDetail");
        Object job = clazz.newInstance();
        clazz.getMethod("setName", String.class).invoke(job, "ajob");
        clazz.getMethod("setJobClass", Class.class).invoke(job, TestJob.class);
        return (JobDetail) job;
    }

    private static JobDetail createJob2x() throws Exception {
        Class<?> clazz = Class.forName("org.quartz.JobBuilder");
        Object jobBuilder = clazz.getMethod("newJob", Class.class).invoke(null, TestJob.class);
        clazz.getMethod("withIdentity", String.class, String.class).invoke(jobBuilder, "ajob",
                "agroup");
        return (JobDetail) clazz.getMethod("build").invoke(jobBuilder);
    }

    private static Trigger createTrigger1x() throws Exception {
        Class<?> clazz = Class.forName("org.quartz.SimpleTrigger");
        Object trigger = clazz.newInstance();
        clazz.getMethod("setName", String.class).invoke(trigger, "atrigger");
        clazz.getMethod("setStartTime", Date.class).invoke(trigger, new Date());
        return (Trigger) trigger;
    }

    private static Trigger createTrigger2x() throws Exception {
        Class<?> clazz = Class.forName("org.quartz.TriggerBuilder");
        Object triggerBuilder = clazz.getMethod("newTrigger").invoke(null);
        clazz.getMethod("withIdentity", String.class, String.class).invoke(triggerBuilder,
                "atrigger", "agroup");
        clazz.getMethod("startNow").invoke(triggerBuilder);
        return (Trigger) clazz.getMethod("build").invoke(triggerBuilder);
    }

    public static class ExecuteJob implements AppUnderTest {
        @Override
        public void executeApp(Serializable... args) throws Exception {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            JobDetail job;
            Trigger trigger;
            try {
                job = createJob2x();
                trigger = createTrigger2x();
            } catch (ClassNotFoundException e) {
                job = createJob1x();
                trigger = createTrigger1x();
            }
            scheduler.scheduleJob(job, trigger);
            SECONDS.sleep(1);
            scheduler.shutdown();
        }
    }
}
