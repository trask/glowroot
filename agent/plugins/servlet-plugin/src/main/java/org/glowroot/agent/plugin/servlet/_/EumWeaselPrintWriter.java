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
package org.glowroot.agent.plugin.servlet._;

import java.io.Writer;

import org.glowroot.agent.plugin.api.Agent;
import org.glowroot.agent.plugin.api.ThreadContext;
import org.glowroot.agent.plugin.api.checker.Nullable;
import org.glowroot.agent.plugin.api.config.ConfigListener;
import org.glowroot.agent.plugin.api.config.EumConfigService;

public class EumWeaselPrintWriter extends EumPrintWriter {

    private static EumConfigService eumConfigService = Agent.getEumConfigService();

    // needs to be public so it can be seen from collocated pointcut
    public static final byte[] EUM_JS = EumUtil.readEumJs("weasel.debug.js");

    // script snippet is from https://github.com/instana/weasel
    private static final String EUM_SCRIPT_PART_0 = "<script>"
            + "(function(i, s, o, g, r, a, m) {"
            + "  i['EumObject'] = r;"
            + "  i[r] = i[r] || function() { (i[r].q = i[r].q || []).push(arguments) };"
            + "  i[r].l = 1 * new Date();"
            + "  a = s.createElement(o);"
            + "  m = s.getElementsByTagName(o)[0];"
            + "  a.async = 1;"
            + "  a.src = g + '." + EumUtil.revisionHash(EUM_JS) + ".js';"
            + "  m.parentNode.insertBefore(a, m);"
            + "  i[r]('reportingUrl', g)"
            + "})(window, document, 'script', '".replace(" ", "");

    private static final String EUM_SCRIPT_PART_2 = "','gteum');gteum('traceId','";

    private static final String EUM_SCRIPT_PART_4 = "');</script>";

    private static @Nullable String currReportingUrl;

    static {
        eumConfigService.registerConfigListener(new ConfigListener() {
            @Override
            public void onChange() {
                String value = eumConfigService.getReportingUrlProperty().value();
                if (value.isEmpty()) {
                    currReportingUrl = null;
                } else if (value.startsWith("/") && !value.startsWith("//")) {
                    currReportingUrl = value;
                }
            }
        });
    }

    private final String contextPath;
    private final ThreadContext context;

    public EumWeaselPrintWriter(Writer delegate, String contextPath, ThreadContext context) {
        super(delegate);
        this.contextPath = contextPath;
        this.context = context;
    }

    @Override
    protected void writeScript() {
        superWrite(EUM_SCRIPT_PART_0, 0, EUM_SCRIPT_PART_0.length());
        if (currReportingUrl == null) {
            superWrite(contextPath, 0, contextPath.length());
            superWrite("/--glowroot-eum", 0, "/--glowroot-eum".length());
        } else {
            superWrite(currReportingUrl, 0, currReportingUrl.length());
        }
        superWrite(EUM_SCRIPT_PART_2, 0, EUM_SCRIPT_PART_2.length());
        String traceId = context.getTraceId();
        superWrite(traceId, 0, traceId.length());
        superWrite(EUM_SCRIPT_PART_4, 0, EUM_SCRIPT_PART_4.length());
    }
}
