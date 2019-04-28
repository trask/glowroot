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
package org.glowroot.agent.it.harness.agent;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.security.CodeSource;
import java.util.jar.JarFile;

// this class is registered as the Premain-Class in the MANIFEST.MF of the agent jar
//
// this class should have minimal dependencies since it will live in the system class loader while
// the rest of the agent will live in the bootstrap class loader
public class Premain {

    private Premain() {}

    public static void premain(@SuppressWarnings("unused") String agentArgs,
            Instrumentation instrumentation) {
        try {
            CodeSource codeSource = Premain.class.getProtectionDomain().getCodeSource();
            File agentJarFile = getAgentJarFile(codeSource);
            instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(agentJarFile));
            Class<?> mainEntryPointClass = Class.forName("org.glowroot.zipkin.MainEntryPoint", true,
                    Premain.class.getClassLoader());
            Method premainMethod =
                    mainEntryPointClass.getMethod("premain", Instrumentation.class, File.class);
            premainMethod.invoke(null, instrumentation, agentJarFile);
        } catch (Throwable t) {
            // log error but don't re-throw which would prevent monitored app from starting
            System.err.println("Glowroot failed to start: " + t.getMessage());
            t.printStackTrace();
        }
    }

    static File getAgentJarFile(CodeSource codeSource) throws Exception {
        if (codeSource == null) {
            throw new IOException("Could not determine glowroot jar location");
        }
        File codeSourceFile = new File(codeSource.getLocation().toURI());
        if (codeSourceFile.getName().endsWith(".jar")) {
            return codeSourceFile;
        }
        throw new IOException("Could not determine glowroot jar location");
    }
}
