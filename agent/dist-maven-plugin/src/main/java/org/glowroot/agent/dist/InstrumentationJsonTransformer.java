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
package org.glowroot.agent.dist;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.base.Charsets.UTF_8;

class InstrumentationJsonTransformer {

    private final MavenProject project;

    InstrumentationJsonTransformer(MavenProject project) {
        this.project = project;
    }

    void execute() throws Exception {
        createArtifactJar(project.getDependencyArtifacts());
    }

    private void createArtifactJar(Set<Artifact> artifacts) throws Exception {
        List<String> descriptors = getDescriptors(artifacts);
        String instrumentationListJson = transform(descriptors);
        File metaInfDir = new File(project.getBuild().getOutputDirectory(), "META-INF");
        File file = new File(metaInfDir, "xyzzy.instrumentation-list.json");
        if (!metaInfDir.exists() && !metaInfDir.mkdirs()) {
            throw new IOException("Could not create directory: " + metaInfDir.getAbsolutePath());
        }
        Files.write(instrumentationListJson, file, UTF_8);
    }

    private static List<String> getDescriptors(Set<Artifact> artifacts)
            throws IOException {
        List<String> descriptors = Lists.newArrayList();
        for (Artifact artifact : artifacts) {
            String content = getInstrumentationJson(artifact);
            if (content == null) {
                continue;
            }
            descriptors.add(content);
        }
        return descriptors;
    }

    private static @Nullable String getInstrumentationJson(Artifact artifact) throws IOException {
        File artifactFile = artifact.getFile();
        if (!artifactFile.exists()) {
            return null;
        }
        if (artifactFile.isDirectory()) {
            File jsonFile = new File(artifactFile, "META-INF/xyzzy.instrumentation.json");
            if (!jsonFile.exists()) {
                return null;
            }
            return Files.toString(jsonFile, UTF_8);
        }
        JarInputStream jarIn = new JarInputStream(new FileInputStream(artifact.getFile()));
        try {
            JarEntry jarEntry;
            while ((jarEntry = jarIn.getNextJarEntry()) != null) {
                String name = jarEntry.getName();
                if (jarEntry.isDirectory()) {
                    continue;
                }
                if (!name.equals("META-INF/xyzzy.instrumentation.json")) {
                    continue;
                }
                InputStreamReader in = new InputStreamReader(jarIn, UTF_8);
                String content = CharStreams.toString(in);
                in.close();
                return content;
            }
            return null;
        } finally {
            jarIn.close();
        }
    }

    private static String transform(List<String> descriptors) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        boolean first = true;
        for (String descriptor : descriptors) {
            if (!first) {
                sb.append(",\n");
            }
            sb.append(indent(descriptor.trim()));
            first = false;
        }
        sb.append("\n]\n");
        return sb.toString();
    }

    private static String indent(String descriptor) {
        return descriptor.replaceAll("(?m)^", "  ");
    }
}
