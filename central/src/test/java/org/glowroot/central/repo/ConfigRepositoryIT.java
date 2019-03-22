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
package org.glowroot.central.repo;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PoolingOptions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.central.util.ClusterManager;
import org.glowroot.central.util.Session;
import org.glowroot.common.util.Versions;
import org.glowroot.common2.config.CentralStorageConfig;
import org.glowroot.common2.config.CentralWebConfig;
import org.glowroot.common2.config.HttpProxyConfig;
import org.glowroot.common2.config.ImmutableCentralStorageConfig;
import org.glowroot.common2.config.ImmutableCentralWebConfig;
import org.glowroot.common2.config.ImmutableHttpProxyConfig;
import org.glowroot.common2.config.ImmutableLdapConfig;
import org.glowroot.common2.config.ImmutableRoleConfig;
import org.glowroot.common2.config.ImmutableSmtpConfig;
import org.glowroot.common2.config.ImmutableUserConfig;
import org.glowroot.common2.config.LdapConfig;
import org.glowroot.common2.config.RoleConfig;
import org.glowroot.common2.config.SmtpConfig;
import org.glowroot.common2.config.SmtpConfig.ConnectionSecurity;
import org.glowroot.common2.config.UserConfig;
import org.glowroot.common2.repo.ConfigRepository;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.AdvancedConfig;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.AlertConfig;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.AlertConfig.AlertCondition;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.AlertConfig.AlertCondition.MetricCondition;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.AlertConfig.AlertNotification;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.AlertConfig.AlertNotification.EmailNotification;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.CustomInstrumentationConfig;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.CustomInstrumentationConfig.CaptureKind;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.CustomInstrumentationConfig.MethodModifier;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.GaugeConfig;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.JvmConfig;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.MBeanAttribute;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.TransactionConfig;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.UiDefaultsConfig;
import org.glowroot.wire.api.model.Proto.OptionalInt32;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigRepositoryIT {

    private static ClusterManager clusterManager;
    private static Cluster cluster;
    private static Session session;
    private static ExecutorService asyncExecutor;
    private static ConfigRepository configRepository;
    private static AgentConfigDao agentConfigDao;

    @BeforeClass
    public static void setUp() throws Exception {
        SharedSetupRunListener.startCassandra();
        clusterManager = ClusterManager.create();
        cluster = Clusters.newCluster();
        session = new Session(cluster.newSession(), "glowroot_unit_tests", null,
                PoolingOptions.DEFAULT_MAX_QUEUE_SIZE);
        session.updateSchemaWithRetry("drop table if exists agent_config");
        session.updateSchemaWithRetry("drop table if exists user");
        session.updateSchemaWithRetry("drop table if exists role");
        session.updateSchemaWithRetry("drop table if exists central_config");
        session.updateSchemaWithRetry("drop table if exists agent");
        asyncExecutor = Executors.newCachedThreadPool();

        CentralConfigDao centralConfigDao = new CentralConfigDao(session, clusterManager);
        AgentDisplayDao agentDisplayDao =
                new AgentDisplayDao(session, clusterManager, MoreExecutors.directExecutor(), 10);
        agentConfigDao = new AgentConfigDao(session, agentDisplayDao, clusterManager, 10);
        UserDao userDao = new UserDao(session, clusterManager);
        RoleDao roleDao = new RoleDao(session, clusterManager);
        configRepository =
                new ConfigRepositoryImpl(centralConfigDao, agentConfigDao, userDao, roleDao, "");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        asyncExecutor.shutdown();
        // remove bad data so other tests don't have issue
        session.updateSchemaWithRetry("drop table if exists agent_config");
        session.updateSchemaWithRetry("drop table if exists user");
        session.updateSchemaWithRetry("drop table if exists role");
        session.updateSchemaWithRetry("drop table if exists central_config");
        session.close();
        cluster.close();
        clusterManager.close();
        SharedSetupRunListener.stopCassandra();
    }

    @Test
    public void shouldUpdateTransactionConfig() throws Exception {
        // given
        String agentId = UUID.randomUUID().toString();
        agentConfigDao.store(agentId, AgentConfig.getDefaultInstance(), true);
        TransactionConfig config = configRepository.getTransactionConfig(agentId);
        TransactionConfig updatedConfig = TransactionConfig.newBuilder()
                .setSlowThresholdMillis(OptionalInt32.newBuilder().setValue(1234))
                .setProfilingIntervalMillis(OptionalInt32.newBuilder().setValue(2345))
                .setCaptureThreadStats(true)
                .build();

        // when
        configRepository.updateTransactionConfig(agentId, updatedConfig,
                Versions.getVersion(config));
        config = configRepository.getTransactionConfig(agentId);

        // then
        assertThat(config).isEqualTo(updatedConfig);
    }

    @Test
    public void shouldUpdateJvmConfig() throws Exception {
        // given
        String agentId = UUID.randomUUID().toString();
        agentConfigDao.store(agentId, AgentConfig.getDefaultInstance(), true);
        JvmConfig config = configRepository.getJvmConfig(agentId);
        JvmConfig updatedConfig = JvmConfig.newBuilder()
                .addMaskSystemProperty("x")
                .addMaskSystemProperty("y")
                .addMaskSystemProperty("z")
                .build();

        // when
        configRepository.updateJvmConfig(agentId, updatedConfig,
                Versions.getVersion(config));
        config = configRepository.getJvmConfig(agentId);

        // then
        assertThat(config).isEqualTo(updatedConfig);
    }

    @Test
    public void shouldUpdateUiConfig() throws Exception {
        // given
        String agentId = UUID.randomUUID().toString();
        agentConfigDao.store(agentId, AgentConfig.getDefaultInstance(), true);
        UiDefaultsConfig config = configRepository.getUiDefaultsConfig(agentId);
        UiDefaultsConfig updatedConfig = UiDefaultsConfig.newBuilder()
                .setDefaultTransactionType("xyz")
                .addDefaultPercentile(99.0)
                .addDefaultPercentile(99.9)
                .addDefaultPercentile(99.99)
                .build();

        // when
        configRepository.updateUiDefaultsConfig(agentId, updatedConfig,
                Versions.getVersion(config));
        config = configRepository.getUiDefaultsConfig(agentId);

        // then
        assertThat(config).isEqualTo(updatedConfig);
    }

    @Test
    public void shouldUpdateAdvancedConfig() throws Exception {
        // given
        String agentId = UUID.randomUUID().toString();
        agentConfigDao.store(agentId, AgentConfig.getDefaultInstance(), true);
        AdvancedConfig config = configRepository.getAdvancedConfig(agentId);
        AdvancedConfig updatedConfig = AdvancedConfig.newBuilder()
                .setWeavingTimer(true)
                .setImmediatePartialStoreThresholdSeconds(OptionalInt32.newBuilder().setValue(1))
                .setMaxTransactionAggregates(OptionalInt32.newBuilder().setValue(2))
                .setMaxQueryAggregates(OptionalInt32.newBuilder().setValue(3))
                .setMaxServiceCallAggregates(OptionalInt32.newBuilder().setValue(4))
                .setMaxTraceEntriesPerTransaction(OptionalInt32.newBuilder().setValue(5))
                .setMaxProfileSamplesPerTransaction(OptionalInt32.newBuilder().setValue(6))
                .setMbeanGaugeNotFoundDelaySeconds(OptionalInt32.newBuilder().setValue(7))
                .build();

        // when
        configRepository.updateAdvancedConfig(agentId, updatedConfig, Versions.getVersion(config));
        config = configRepository.getAdvancedConfig(agentId);

        // then
        assertThat(config).isEqualTo(updatedConfig);
    }

    @Test
    public void shouldCrudGaugeConfig() throws Exception {
        // given
        String agentId = UUID.randomUUID().toString();
        agentConfigDao.store(agentId, AgentConfig.getDefaultInstance(), true);
        GaugeConfig config = GaugeConfig.newBuilder()
                .setMbeanObjectName("x")
                .addMbeanAttribute(MBeanAttribute.newBuilder()
                        .setName("y")
                        .setCounter(true))
                .build();

        // when
        configRepository.insertGaugeConfig(agentId, config);
        List<GaugeConfig> configs = configRepository.getGaugeConfigs(agentId);

        // then
        assertThat(configs).hasSize(1);
        assertThat(configs.get(0)).isEqualTo(config);

        // and further

        // given
        GaugeConfig updatedConfig = GaugeConfig.newBuilder()
                .setMbeanObjectName("x2")
                .addMbeanAttribute(MBeanAttribute.newBuilder()
                        .setName("y2"))
                .build();

        // when
        configRepository.updateGaugeConfig(agentId, updatedConfig, Versions.getVersion(config));
        configs = configRepository.getGaugeConfigs(agentId);

        // then
        assertThat(configs).hasSize(1);
        assertThat(configs.get(0)).isEqualTo(updatedConfig);

        // and further

        // when
        configRepository.deleteGaugeConfig(agentId, Versions.getVersion(updatedConfig));
        configs = configRepository.getGaugeConfigs(agentId);

        // then
        assertThat(configs).isEmpty();
    }

    @Test
    public void shouldCrudAlertConfig() throws Exception {
        // given
        String agentId = UUID.randomUUID().toString();
        agentConfigDao.store(agentId, AgentConfig.getDefaultInstance(), true);
        AlertConfig config = AlertConfig.newBuilder()
                .setCondition(AlertCondition.newBuilder()
                        .setMetricCondition(MetricCondition.newBuilder()
                                .setMetric("gauge:abc")
                                .setThreshold(111)
                                .setTimePeriodSeconds(60))
                        .build())
                .setNotification(AlertNotification.newBuilder()
                        .setEmailNotification(EmailNotification.newBuilder()
                                .addEmailAddress("noone@example.org")))
                .build();

        // when
        configRepository.insertAlertConfig(agentId, config);
        List<AlertConfig> configs = configRepository.getAlertConfigs(agentId);

        // then
        assertThat(configs).hasSize(1);
        assertThat(configs.get(0)).isEqualTo(config);

        // and further

        // given
        AlertConfig updatedConfig = AlertConfig.newBuilder()
                .setCondition(AlertCondition.newBuilder()
                        .setMetricCondition(MetricCondition.newBuilder()
                                .setMetric("gauge:abc2")
                                .setThreshold(222)
                                .setTimePeriodSeconds(62))
                        .build())
                .setNotification(AlertNotification.newBuilder()
                        .setEmailNotification(EmailNotification.newBuilder()
                                .addEmailAddress("noone2@example.org")))
                .build();

        // when
        configRepository.updateAlertConfig(agentId, updatedConfig, Versions.getVersion(config));
        configs = configRepository.getAlertConfigs(agentId);

        // then
        assertThat(configs).hasSize(1);
        assertThat(configs.get(0)).isEqualTo(updatedConfig);

        // and further

        // when
        configRepository.deleteAlertConfig(agentId, Versions.getVersion(updatedConfig));
        configs = configRepository.getAlertConfigs(agentId);

        // then
        assertThat(configs).isEmpty();
    }

    @Test
    public void shouldCrudCustomInstrumentationConfig() throws Exception {
        // given
        String agentId = UUID.randomUUID().toString();
        agentConfigDao.store(agentId, AgentConfig.getDefaultInstance(), true);
        CustomInstrumentationConfig config = CustomInstrumentationConfig.newBuilder()
                .setClassName("a")
                .setMethodName("b")
                .setMethodReturnType("c")
                .addMethodModifier(MethodModifier.PUBLIC)
                .setCaptureKind(CaptureKind.TRACE_ENTRY)
                .setTimerName("d")
                .setTraceEntryMessageTemplate("e")
                .setTraceEntryStackThresholdMillis(OptionalInt32.newBuilder().setValue(1))
                .setTransactionType("f")
                .setTransactionNameTemplate("g")
                .setTransactionSlowThresholdMillis(OptionalInt32.newBuilder().setValue(2))
                .build();

        // when
        configRepository.insertCustomInstrumentationConfig(agentId, config);
        List<CustomInstrumentationConfig> configs =
                configRepository.getCustomInstrumentationConfig(agentId);

        // then
        assertThat(configs).hasSize(1);
        assertThat(configs.get(0)).isEqualTo(config);

        // and further

        // given
        CustomInstrumentationConfig updatedConfig = CustomInstrumentationConfig.newBuilder()
                .setClassName("a2")
                .setMethodName("b2")
                .setMethodReturnType("c2")
                .addMethodModifier(MethodModifier.PUBLIC)
                .setCaptureKind(CaptureKind.TRACE_ENTRY)
                .setTimerName("d2")
                .setTraceEntryMessageTemplate("e2")
                .setTraceEntryStackThresholdMillis(OptionalInt32.newBuilder().setValue(12))
                .setTransactionType("f2")
                .setTransactionNameTemplate("g2")
                .setTransactionSlowThresholdMillis(OptionalInt32.newBuilder().setValue(22))
                .build();

        // when
        configRepository.updateCustomInstrumentationConfig(agentId, updatedConfig,
                Versions.getVersion(config));
        configs = configRepository.getCustomInstrumentationConfig(agentId);

        // then
        assertThat(configs).hasSize(1);
        assertThat(configs.get(0)).isEqualTo(updatedConfig);

        // and further

        // when
        configRepository.deleteCustomInstrumentationConfigs(agentId,
                ImmutableList.of(Versions.getVersion(updatedConfig)));
        configs = configRepository.getCustomInstrumentationConfig(agentId);

        // then
        assertThat(configs).isEmpty();
    }

    @Test
    public void shouldCrudUserConfig() throws Exception {
        // given
        UserConfig config = ImmutableUserConfig.builder()
                .username("auser")
                .addRoles("brole")
                .build();

        // when
        configRepository.insertUserConfig(config);
        List<UserConfig> configs = configRepository.getUserConfigs();

        // then
        assertThat(configs).hasSize(2);
        assertThat(configs.get(1)).isEqualTo(config);

        // and further

        // given
        String username = "auser";

        // when
        UserConfig readConfig = configRepository.getUserConfig(username);

        // then
        assertThat(readConfig).isNotNull();

        // and further

        // given
        UserConfig updatedConfig = ImmutableUserConfig.builder()
                .username("auser")
                .addRoles("brole2")
                .build();

        // when
        configRepository.updateUserConfig(updatedConfig, config.version());
        configs = configRepository.getUserConfigs();

        // then
        assertThat(configs).hasSize(2);
        assertThat(configs.get(1)).isEqualTo(updatedConfig);

        // and further

        // when
        configRepository.deleteUserConfig(updatedConfig.username());
        configs = configRepository.getUserConfigs();

        // then
        assertThat(configs).hasSize(1);
        assertThat(configs.get(0).username()).isEqualTo("anonymous");
    }

    @Test
    public void shouldCrudRoleConfig() throws Exception {
        // given
        RoleConfig config = ImmutableRoleConfig.builder()
                .central(true)
                .name("brole")
                .addPermissions("p1")
                .addPermissions("p2")
                .build();

        // when
        configRepository.insertRoleConfig(config);
        List<RoleConfig> configs = configRepository.getRoleConfigs();

        // then
        assertThat(configs).hasSize(2);
        assertThat(configs.get(1)).isEqualTo(config);

        // and further

        // given
        RoleConfig updatedConfig = ImmutableRoleConfig.builder()
                .central(true)
                .name("brole")
                .addPermissions("p5")
                .addPermissions("p6")
                .addPermissions("p7")
                .build();

        // when
        configRepository.updateRoleConfig(updatedConfig, config.version());
        configs = configRepository.getRoleConfigs();

        // then
        assertThat(configs).hasSize(2);
        assertThat(configs.get(1)).isEqualTo(updatedConfig);

        // and further

        // when
        configRepository.deleteRoleConfig(updatedConfig.name());
        configs = configRepository.getRoleConfigs();

        // then
        assertThat(configs).hasSize(1);
        assertThat(configs.get(0).name()).isEqualTo("Administrator");
    }

    @Test
    public void shouldUpdateWebConfig() throws Exception {
        // given
        CentralWebConfig config = configRepository.getCentralWebConfig();
        CentralWebConfig updatedConfig = ImmutableCentralWebConfig.builder()
                .sessionTimeoutMinutes(31)
                .sessionCookieName("GLOWROOT_SESSION_ID2")
                .build();

        // when
        configRepository.updateCentralWebConfig(updatedConfig, config.version());
        config = configRepository.getCentralWebConfig();

        // then
        assertThat(config).isEqualTo(updatedConfig);
    }

    @Test
    public void shouldUpdateCentralStorageConfig() throws Exception {
        // given
        CentralStorageConfig config = configRepository.getCentralStorageConfig();
        CentralStorageConfig updatedConfig = ImmutableCentralStorageConfig.builder()
                .addRollupExpirationHours(1)
                .addRollupExpirationHours(2)
                .addRollupExpirationHours(3)
                .addRollupExpirationHours(4)
                .addQueryAndServiceCallRollupExpirationHours(5)
                .addQueryAndServiceCallRollupExpirationHours(6)
                .addQueryAndServiceCallRollupExpirationHours(7)
                .addQueryAndServiceCallRollupExpirationHours(8)
                .addProfileRollupExpirationHours(9)
                .addProfileRollupExpirationHours(10)
                .addProfileRollupExpirationHours(11)
                .addProfileRollupExpirationHours(12)
                .traceExpirationHours(100)
                .build();

        // when
        configRepository.updateCentralStorageConfig(updatedConfig, config.version());
        config = configRepository.getCentralStorageConfig();

        // then
        assertThat(config).isEqualTo(updatedConfig);
    }

    @Test
    public void shouldUpdateSmtpConfig() throws Exception {
        // given
        SmtpConfig config = configRepository.getSmtpConfig();
        SmtpConfig updatedConfig = ImmutableSmtpConfig.builder()
                .host("a")
                .port(555)
                .connectionSecurity(ConnectionSecurity.SSL_TLS)
                .username("b")
                .encryptedPassword("c")
                .putAdditionalProperties("f", "g")
                .putAdditionalProperties("h", "i")
                .fromEmailAddress("d")
                .fromDisplayName("e")
                .build();

        // when
        configRepository.updateSmtpConfig(updatedConfig, config.version());
        config = configRepository.getSmtpConfig();

        // then
        assertThat(config).isEqualTo(updatedConfig);
    }

    @Test
    public void shouldUpdateHttpProxyConfig() throws Exception {
        // given
        HttpProxyConfig config = configRepository.getHttpProxyConfig();
        HttpProxyConfig updatedConfig = ImmutableHttpProxyConfig.builder()
                .host("a")
                .port(555)
                .username("b")
                .encryptedPassword("c")
                .build();

        // when
        configRepository.updateHttpProxyConfig(updatedConfig, config.version());
        config = configRepository.getHttpProxyConfig();

        // then
        assertThat(config).isEqualTo(updatedConfig);
    }

    @Test
    public void shouldUpdateLdapConfig() throws Exception {
        // given
        LdapConfig config = configRepository.getLdapConfig();
        LdapConfig updatedConfig = ImmutableLdapConfig.builder()
                .host("a")
                .port(1234)
                .username("b")
                .encryptedPassword("c")
                .userBaseDn("d")
                .userSearchFilter("e")
                .groupBaseDn("f")
                .groupSearchFilter("g")
                .build();

        // when
        configRepository.updateLdapConfig(updatedConfig, config.version());
        config = configRepository.getLdapConfig();

        // then
        assertThat(config).isEqualTo(updatedConfig);
    }
}
