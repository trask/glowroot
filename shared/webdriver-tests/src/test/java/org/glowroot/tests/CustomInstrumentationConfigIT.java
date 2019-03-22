/*
 * Copyright 2013-2019 the original author or authors.
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
package org.glowroot.tests;

import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import org.junit.Test;

import org.glowroot.tests.config.ConfigSidebar;
import org.glowroot.tests.config.CustomInstrumentationConfigPage;
import org.glowroot.tests.util.Utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openqa.selenium.By.xpath;

public class CustomInstrumentationConfigIT extends WebDriverIT {

    @Test
    public void shouldAddTransactionCustomInstrumentation() throws Exception {
        // given
        App app = app();
        GlobalNavbar globalNavbar = globalNavbar();
        ConfigSidebar configSidebar = new ConfigSidebar(driver);

        app.open();
        globalNavbar.clickConfigLink();
        configSidebar.clickCustomInstrumentationLink();

        // when
        createTransactionCustomInstrumentation();

        // then
        app.open();
        globalNavbar.clickConfigLink();
        configSidebar.clickCustomInstrumentationLink();
        clickLinkWithWait("org.glowroot.agent.it.harness.Container::execute");
        CustomInstrumentationConfigPage configPage = new CustomInstrumentationConfigPage(driver);
        assertThat(configPage.getClassNameTextField().getAttribute("value"))
                .isEqualTo("org.glowroot.agent.it.harness.Container");
        assertThat(configPage.getMethodNameTextField().getAttribute("value")).isEqualTo("execute");
        assertThat(configPage.getCaptureKindTransactionRadioButton().isSelected()).isTrue();
        assertThat(configPage.getTransactionTypeTextField().getAttribute("value"))
                .isEqualTo("a type");
        assertThat(configPage.getTransactionNameTemplateTextField().getAttribute("value"))
                .isEqualTo("a trace");
        assertThat(configPage.getTraceEntryMessageTemplateTextField().getAttribute("value"))
                .isEqualTo("a trace entry");
        assertThat(configPage.getTimerNameTextField().getAttribute("value")).isEqualTo("a timer");
        assertThat(configPage.getTransactionSlowThresholdMillisTextField().getAttribute("value"))
                .isEqualTo("123");
    }

    @Test
    public void shouldNotValidateOnDeleteCustomInstrumentation() throws Exception {
        // given
        App app = app();
        GlobalNavbar globalNavbar = globalNavbar();
        ConfigSidebar configSidebar = new ConfigSidebar(driver);

        app.open();
        globalNavbar.clickConfigLink();
        configSidebar.clickCustomInstrumentationLink();
        createTransactionCustomInstrumentation();

        app.open();
        globalNavbar.clickConfigLink();
        configSidebar.clickCustomInstrumentationLink();
        clickLinkWithWait("org.glowroot.agent.it.harness.Container::execute");
        CustomInstrumentationConfigPage configPage = new CustomInstrumentationConfigPage(driver);

        // when
        Utils.clearInput(configPage.getTimerNameTextField());
        configPage.clickDeleteButton();

        // then
        // wait for delete to complete
        waitFor(xpath(".//div[normalize-space()='There is no custom instrumentation']"));
    }

    @Test
    public void shouldAddErrorEntryCustomInstrumentation() throws Exception {
        // given
        App app = app();
        GlobalNavbar globalNavbar = globalNavbar();
        ConfigSidebar configSidebar = new ConfigSidebar(driver);

        app.open();
        globalNavbar.clickConfigLink();
        configSidebar.clickCustomInstrumentationLink();

        // when
        createTraceEntryCustomInstrumentation();

        // then
        app.open();
        globalNavbar.clickConfigLink();
        configSidebar.clickCustomInstrumentationLink();
        clickLinkWithWait("org.glowroot.agent.it.harness.Container::execute");
        CustomInstrumentationConfigPage configPage = new CustomInstrumentationConfigPage(driver);
        assertThat(configPage.getClassNameTextField().getAttribute("value"))
                .isEqualTo("org.glowroot.agent.it.harness.Container");
        assertThat(configPage.getMethodNameTextField().getAttribute("value")).isEqualTo("execute");
        assertThat(configPage.getCaptureKindTraceEntryRadioButton().isSelected()).isTrue();
        assertThat(configPage.getTimerNameTextField().getAttribute("value")).isEqualTo("a timer");
        assertThat(configPage.getTraceEntryMessageTemplateTextField().getAttribute("value"))
                .isEqualTo("a trace entry");
        if (driver instanceof JBrowserDriver) {
            // just a little workaround
            assertThat(configPage.getTraceEntryStackThresholdTextField().getAttribute("value"))
                    .isNull();
        } else {
            assertThat(configPage.getTraceEntryStackThresholdTextField().getAttribute("value"))
                    .isEqualTo("");
        }
    }

    @Test
    public void shouldAddTimerCustomInstrumentation() throws Exception {
        // given
        App app = app();
        GlobalNavbar globalNavbar = globalNavbar();
        ConfigSidebar configSidebar = new ConfigSidebar(driver);

        app.open();
        globalNavbar.clickConfigLink();
        configSidebar.clickCustomInstrumentationLink();

        // when
        createTimerCustomInstrumentation();

        // then
        app.open();
        globalNavbar.clickConfigLink();
        configSidebar.clickCustomInstrumentationLink();
        clickLinkWithWait("org.glowroot.agent.it.harness.Container::execute");
        CustomInstrumentationConfigPage configPage = new CustomInstrumentationConfigPage(driver);
        assertThat(configPage.getClassNameTextField().getAttribute("value"))
                .isEqualTo("org.glowroot.agent.it.harness.Container");
        assertThat(configPage.getMethodNameTextField().getAttribute("value")).isEqualTo("execute");
        assertThat(configPage.getCaptureKindTimerRadioButton().isSelected()).isTrue();
        assertThat(configPage.getTimerNameTextField().getAttribute("value")).isEqualTo("a timer");
    }

    private void createTransactionCustomInstrumentation() {
        clickNewCustomInstrumentation();
        CustomInstrumentationConfigPage configPage = new CustomInstrumentationConfigPage(driver);
        configPage.getClassNameTextField().sendKeys("harness.Container");
        configPage.clickClassNameAutoCompleteItem("org.glowroot.agent.it.harness.Container");
        configPage.getMethodNameTextField().sendKeys("exec");
        configPage.clickMethodNameAutoCompleteItem("execute");
        configPage.getCaptureKindTransactionRadioButton().click();
        configPage.getTransactionTypeTextField().clear();
        configPage.getTransactionTypeTextField().sendKeys("a type");
        configPage.getTransactionNameTemplateTextField().clear();
        configPage.getTransactionNameTemplateTextField().sendKeys("a trace");
        configPage.getTraceEntryMessageTemplateTextField().clear();
        configPage.getTraceEntryMessageTemplateTextField().sendKeys("a trace entry");
        configPage.getTimerNameTextField().clear();
        configPage.getTimerNameTextField().sendKeys("a timer");
        configPage.getTransactionSlowThresholdMillisTextField().clear();
        configPage.getTransactionSlowThresholdMillisTextField().sendKeys("123");
        configPage.clickAddButton();
        // the delete button does not appear until after the save/redirect
        configPage.waitForDeleteButton();
        clickLink("Return to list");
    }

    private void createTraceEntryCustomInstrumentation() {
        clickNewCustomInstrumentation();
        CustomInstrumentationConfigPage configPage = new CustomInstrumentationConfigPage(driver);
        // exercise limit first
        configPage.getClassNameTextField().sendKeys("java.io.File");
        configPage.clickClassNameAutoCompleteItem("java.io.File");
        configPage.getMethodNameTextField().sendKeys("a");
        configPage.clickMethodNameAutoCompleteItem("canExecute");
        configPage.getClassNameTextField().clear();
        // now do the real thing
        configPage.getClassNameTextField().sendKeys("harness.Container");
        configPage.clickClassNameAutoCompleteItem("org.glowroot.agent.it.harness.Container");
        configPage.getMethodNameTextField().sendKeys("exec");
        configPage.clickMethodNameAutoCompleteItem("execute");
        configPage.getCaptureKindTraceEntryRadioButton().click();
        configPage.getTraceEntryMessageTemplateTextField().clear();
        configPage.getTraceEntryMessageTemplateTextField().sendKeys("a trace entry");
        configPage.getTimerNameTextField().clear();
        configPage.getTimerNameTextField().sendKeys("a timer");
        configPage.clickAddButton();
        // the delete button does not appear until after the save/redirect
        configPage.waitForDeleteButton();
        clickLink("Return to list");
    }

    private void createTimerCustomInstrumentation() {
        clickNewCustomInstrumentation();
        CustomInstrumentationConfigPage configPage = new CustomInstrumentationConfigPage(driver);
        configPage.getClassNameTextField().sendKeys("harness.Container");
        configPage.clickClassNameAutoCompleteItem("org.glowroot.agent.it.harness.Container");
        configPage.getMethodNameTextField().sendKeys("exec");
        configPage.clickMethodNameAutoCompleteItem("execute");
        configPage.getCaptureKindTimerRadioButton().click();
        configPage.getTimerNameTextField().clear();
        configPage.getTimerNameTextField().sendKeys("a timer");
        configPage.clickAddButton();
        // the delete button does not appear until after the save/redirect
        configPage.waitForDeleteButton();
        clickLink("Return to list");
    }

    private void clickNewCustomInstrumentation() {
        if (WebDriverSetup.useCentral) {
            clickWithWait(xpath(
                    "//a[@href='config/custom-instrumentation?agent-id=" + agentId + "&new']"));
        } else {
            clickWithWait(xpath("//a[@href='config/custom-instrumentation?new']"));
        }
    }
}
