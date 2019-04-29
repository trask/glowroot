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
package org.glowroot.xyzzy.instrumentation.hibernate;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.xyzzy.test.harness.AppUnderTest;
import org.glowroot.xyzzy.test.harness.Container;
import org.glowroot.xyzzy.test.harness.Containers;
import org.glowroot.xyzzy.test.harness.ServerSpan;
import org.glowroot.xyzzy.test.harness.TransactionMarker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.glowroot.xyzzy.test.harness.util.HarnessAssertions.assertSingleLocalSpanMessage;

public class SessionIT {

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
    public void shouldCaptureCriteriaQuery() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(CriteriaQuery.class);

        // then
        ServerSpan.Timer mainThreadRootTimer = serverSpan.mainThreadRootTimer();
        assertThat(mainThreadRootTimer.childTimers().size()).isEqualTo(1);
        assertThat(mainThreadRootTimer.childTimers().get(0).name())
                .isEqualTo("hibernate query");
        assertThat(mainThreadRootTimer.childTimers().get(0).childTimers()).isEmpty();
        assertThat(serverSpan.childSpans()).isEmpty();
    }

    // TODO add unit test for jpa criteria query

    @Test
    public void shouldCaptureSave() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(SessionSave.class);

        // then
        List<ServerSpan.Timer> timers = serverSpan.mainThreadRootTimer().childTimers();
        assertThat(timers).hasSize(1);
        assertThat(timers.get(0).name()).isEqualTo("hibernate save");
    }

    @Test
    public void shouldCaptureSaveTwoArg() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(SessionSaveTwoArg.class);

        // then
        List<ServerSpan.Timer> timers = serverSpan.mainThreadRootTimer().childTimers();
        assertThat(timers).hasSize(1);
        assertThat(timers.get(0).name()).isEqualTo("hibernate save");
    }

    @Test
    public void shouldCaptureSaveOrUpdate() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(SessionSaveOrUpdate.class);

        // then
        List<ServerSpan.Timer> timers = serverSpan.mainThreadRootTimer().childTimers();
        assertThat(timers).hasSize(1);
        assertThat(timers.get(0).name()).isEqualTo("hibernate saveOrUpdate");
    }

    @Test
    public void shouldCaptureSaveOrUpdateTwoArg() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(SessionSaveOrUpdateTwoArg.class);

        // then
        List<ServerSpan.Timer> timers = serverSpan.mainThreadRootTimer().childTimers();
        assertThat(timers).hasSize(1);
        assertThat(timers.get(0).name()).isEqualTo("hibernate saveOrUpdate");
    }

    @Test
    public void shouldCaptureUpdate() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(SessionUpdate.class);

        // then
        List<ServerSpan.Timer> timers = serverSpan.mainThreadRootTimer().childTimers();
        assertThat(timers).hasSize(1);
        assertThat(timers.get(0).name()).isEqualTo("hibernate update");
    }

    @Test
    public void shouldCaptureUpdateTwoArg() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(SessionUpdateTwoArg.class);

        // then
        List<ServerSpan.Timer> timers = serverSpan.mainThreadRootTimer().childTimers();
        assertThat(timers).hasSize(1);
        assertThat(timers.get(0).name()).isEqualTo("hibernate update");
    }

    @Test
    public void shouldCaptureMergeCommand() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(SessionMerge.class);

        // then
        List<ServerSpan.Timer> timers = serverSpan.mainThreadRootTimer().childTimers();
        assertThat(timers).hasSize(1);
        assertThat(timers.get(0).name()).isEqualTo("hibernate merge");
    }

    @Test
    public void shouldCaptureMergeCommandTwoArg() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(SessionMergeTwoArg.class);

        // then
        List<ServerSpan.Timer> timers = serverSpan.mainThreadRootTimer().childTimers();
        assertThat(timers).hasSize(1);
        assertThat(timers.get(0).name()).isEqualTo("hibernate merge");
    }

    @Test
    public void shouldCapturePersistCommand() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(SessionPersist.class);

        // then
        List<ServerSpan.Timer> timers = serverSpan.mainThreadRootTimer().childTimers();
        assertThat(timers).hasSize(1);
        assertThat(timers.get(0).name()).isEqualTo("hibernate persist");
    }

    @Test
    public void shouldCapturePersistCommandTwoArg() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(SessionPersistTwoArg.class);

        // then
        List<ServerSpan.Timer> timers = serverSpan.mainThreadRootTimer().childTimers();
        assertThat(timers).hasSize(1);
        assertThat(timers.get(0).name()).isEqualTo("hibernate persist");
    }

    @Test
    public void shouldCaptureDelete() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(SessionDelete.class);

        // then
        List<ServerSpan.Timer> timers = serverSpan.mainThreadRootTimer().childTimers();
        assertThat(timers).hasSize(1);
        assertThat(timers.get(0).name()).isEqualTo("hibernate delete");
    }

    @Test
    public void shouldCaptureDeleteTwoArg() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(SessionDeleteTwoArg.class);

        // then
        List<ServerSpan.Timer> timers = serverSpan.mainThreadRootTimer().childTimers();
        assertThat(timers).hasSize(1);
        assertThat(timers.get(0).name()).isEqualTo("hibernate delete");
    }

    @Test
    public void shouldCaptureSessionFlush() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(SessionFlush.class);

        // then
        assertSingleLocalSpanMessage(serverSpan).isEqualTo("hibernate flush");
    }

    @Test
    public void shouldCaptureTransactionCommit() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(TransactionCommit.class);

        // then
        assertSingleLocalSpanMessage(serverSpan).isEqualTo("hibernate commit");
    }

    @Test
    public void shouldCaptureTransactionRollback() throws Exception {
        // when
        ServerSpan serverSpan = container.execute(TransactionRollback.class);

        // then
        assertSingleLocalSpanMessage(serverSpan).isEqualTo("hibernate rollback");
    }

    public abstract static class DoWithSession implements AppUnderTest, TransactionMarker {

        Session session;

        @Override
        public void executeApp() throws Exception {
            session = HibernateUtil.getSessionFactory().openSession();
            Transaction transaction = session.beginTransaction();
            initOutsideTransactionMarker();
            transactionMarker();
            transaction.commit();
            session.close();
        }

        protected void initOutsideTransactionMarker() {}
    }

    public static class CriteriaQuery extends DoWithSession {
        @Override
        public void transactionMarker() {
            session.createCriteria(Employee.class).list();
        }
    }

    public static class SessionSave extends DoWithSession {
        @Override
        public void transactionMarker() {
            session.save(new Employee("John"));
        }
    }

    public static class SessionSaveTwoArg extends DoWithSession {
        @Override
        public void transactionMarker() {
            session.save(null, new Employee("John"));
        }
    }

    public static class SessionSaveOrUpdate extends DoWithSession {
        @Override
        public void transactionMarker() {
            session.saveOrUpdate(new Employee("John"));
        }
    }

    public static class SessionSaveOrUpdateTwoArg extends DoWithSession {
        @Override
        public void transactionMarker() {
            session.saveOrUpdate(null, new Employee("John"));
        }
    }

    public static class SessionUpdate extends DoWithSession {

        private Employee employee;

        @Override
        public void transactionMarker() {
            session.update(employee);
        }

        @Override
        protected void initOutsideTransactionMarker() {
            employee = (Employee) session.merge(new Employee("John"));
            employee.setName("Hugh");
        }
    }

    public static class SessionUpdateTwoArg extends DoWithSession {

        private Employee employee;

        @Override
        public void transactionMarker() {
            session.update(null, employee);
        }

        @Override
        protected void initOutsideTransactionMarker() {
            employee = (Employee) session.merge(new Employee("John"));
            employee.setName("Hugh");
        }
    }

    public static class SessionMerge extends DoWithSession {
        @Override
        public void transactionMarker() {
            session.merge(new Employee("John"));
        }
    }

    public static class SessionMergeTwoArg extends DoWithSession {
        @Override
        public void transactionMarker() {
            session.merge(null, new Employee("John"));
        }
    }

    public static class SessionPersist extends DoWithSession {
        @Override
        public void transactionMarker() {
            session.persist(new Employee("John"));
        }
    }

    public static class SessionPersistTwoArg extends DoWithSession {
        @Override
        public void transactionMarker() {
            session.persist(null, new Employee("John"));
        }
    }

    public static class SessionDelete extends DoWithSession {

        private Employee employee;

        @Override
        public void transactionMarker() {
            session.delete(employee);
        }

        @Override
        protected void initOutsideTransactionMarker() {
            employee = (Employee) session.merge(new Employee("John"));
            employee.setName("Hugh");
        }
    }

    public static class SessionDeleteTwoArg extends DoWithSession {

        private Employee employee;

        @Override
        public void transactionMarker() {
            session.delete(null, employee);
        }

        @Override
        protected void initOutsideTransactionMarker() {
            employee = (Employee) session.merge(new Employee("John"));
            employee.setName("Hugh");
        }
    }

    public static class SessionFlush extends DoWithSession {
        @Override
        public void transactionMarker() {
            Employee employee = (Employee) session.merge(new Employee("John"));
            employee.setEmail(new Email("john@example.org"));
            session.flush();
        }
    }

    public static class TransactionCommit implements AppUnderTest, TransactionMarker {

        private Session session;

        @Override
        public void executeApp() throws Exception {
            session = HibernateUtil.getSessionFactory().openSession();
            transactionMarker();
            session.close();
        }

        @Override
        public void transactionMarker() throws Exception {
            Transaction transaction = session.beginTransaction();
            session.save(new Employee("John"));
            transaction.commit();
        }
    }

    public static class TransactionRollback implements AppUnderTest, TransactionMarker {

        private Session session;

        @Override
        public void executeApp() throws Exception {
            session = HibernateUtil.getSessionFactory().openSession();
            transactionMarker();
            session.close();
        }

        @Override
        public void transactionMarker() throws Exception {
            Transaction transaction = session.beginTransaction();
            session.save(new Employee("John"));
            transaction.rollback();
        }
    }
}
