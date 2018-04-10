/*
 * Copyright 2016-2018 the original author or authors.
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
package org.glowroot.agent.plugin.jaxrs;

import org.glowroot.agent.plugin.api.Agent;
import org.glowroot.agent.plugin.api.ThreadContext;
import org.glowroot.agent.plugin.api.ThreadContext.Priority;
import org.glowroot.agent.plugin.api.ThreadContext.ServletRequestInfo;
import org.glowroot.agent.plugin.api.checker.Nullable;
import org.glowroot.agent.plugin.api.config.BooleanProperty;
import org.glowroot.agent.plugin.api.weaving.BindMethodMeta;
import org.glowroot.agent.plugin.api.weaving.OnBefore;
import org.glowroot.agent.plugin.api.weaving.Pointcut;

public class ResourceAspect {

    private static final BooleanProperty useAltTransactionNaming =
            Agent.getConfigService("jaxrs").getBooleanProperty("useAltTransactionNaming");

    @Pointcut(classAnnotation = "javax.ws.rs.Path",
            methodAnnotation = "javax.ws.rs.Path|javax.ws.rs.DELETE|javax.ws.rs.GET"
                    + "|javax.ws.rs.HEAD|javax.ws.rs.OPTIONS|javax.ws.rs.POST|javax.ws.rs.PUT",
            methodParameterTypes = {".."})
    public static class ResourceAdvice {

        @OnBefore
        public static void onBefore(ThreadContext context,
                @BindMethodMeta ResourceMethodMeta resourceMethodMeta) {

            if (useAltTransactionNaming.value()) {
                context.setTransactionName(resourceMethodMeta.getAltTransactionName(),
                        Priority.CORE_PLUGIN);
            } else {
                ServletRequestInfo servletRequestInfo = context.getServletRequestInfo();
                String transactionName = getTransactionName(resourceMethodMeta, servletRequestInfo);
                context.setTransactionName(transactionName, Priority.CORE_PLUGIN);
            }
        }

        private static String getTransactionName(ResourceMethodMeta resourceMethodMeta,
                @Nullable ServletRequestInfo servletRequestInfo) {
            if (servletRequestInfo == null) {
                return resourceMethodMeta.getPath();
            }
            String method = servletRequestInfo.getMethod();
            String servletPath = getServletPath(servletRequestInfo);
            if (method.isEmpty()) {
                return servletPath + resourceMethodMeta.getPath();
            } else {
                return method + " " + servletPath + resourceMethodMeta.getPath();
            }
        }

        private static String getServletPath(ServletRequestInfo servletRequestInfo) {
            if (servletRequestInfo.getPathInfo() == null) {
                // pathInfo is null when the servlet is mapped to "/" (not "/*") and therefore it is
                // replacing the default servlet and getServletPath() returns the full path
                return servletRequestInfo.getContextPath();
            } else {
                return servletRequestInfo.getContextPath() + servletRequestInfo.getServletPath();
            }
        }
    }
}
