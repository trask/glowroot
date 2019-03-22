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
package org.glowroot.agent.config;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.io.Resources;
import com.google.gson.JsonSyntaxException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.glowroot.xyzzy.engine.config.ImmutableInstrumentationDescriptor;
import org.glowroot.xyzzy.engine.config.InstrumentationDescriptor;
import org.glowroot.xyzzy.engine.config.InstrumentationDescriptors;

import static com.google.common.base.Charsets.UTF_8;

public class InstrumentationDescriptorBuilder {

    private static final Logger logger =
            LoggerFactory.getLogger(InstrumentationDescriptorBuilder.class);

    private InstrumentationDescriptorBuilder() {}

    public static List<InstrumentationDescriptor> create(@Nullable File instrumentationDir,
            boolean offlineViewer) throws Exception {
        List<File> jarFiles = getJarFiles(instrumentationDir);
        List<InstrumentationDescriptor> instrumentationDescriptors = Lists.newArrayList();
        instrumentationDescriptors.addAll(InstrumentationDescriptors.readInstrumentationList());
        if (offlineViewer) {
            instrumentationDescriptors.addAll(createForOfflineViewer(jarFiles, instrumentationDir));
        } else {
            instrumentationDescriptors.addAll(readDescriptors(jarFiles, instrumentationDir));
        }
        return new DescriptorOrdering().sortedCopy(instrumentationDescriptors);
    }

    private static ImmutableList<File> getJarFiles(@Nullable File instrumentationDir) {
        if (instrumentationDir == null) {
            return ImmutableList.of();
        }
        File[] jarFiles = instrumentationDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });
        if (jarFiles == null) {
            logger.warn("listFiles() returned null on directory: {}",
                    instrumentationDir.getAbsolutePath());
            return ImmutableList.of();
        }
        return ImmutableList.copyOf(jarFiles);
    }

    private static ImmutableList<File> getStandaloneDescriptors(@Nullable File instrumentationDir) {
        if (instrumentationDir == null) {
            return ImmutableList.of();
        }
        File[] descriptorFiles = instrumentationDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".json");
            }
        });
        if (descriptorFiles == null) {
            logger.warn("listFiles() returned null on directory: {}",
                    instrumentationDir.getAbsolutePath());
            return ImmutableList.of();
        }
        return ImmutableList.copyOf(descriptorFiles);
    }

    private static ImmutableList<URL> getResources(String resourceName) throws IOException {
        ClassLoader loader = InstrumentationDescriptorBuilder.class.getClassLoader();
        if (loader == null) {
            return ImmutableList
                    .copyOf(Iterators.forEnumeration(ClassLoader.getSystemResources(resourceName)));
        } else {
            return ImmutableList
                    .copyOf(Iterators.forEnumeration(loader.getResources(resourceName)));
        }
    }

    private static List<InstrumentationDescriptor> createForOfflineViewer(List<File> jarFiles,
            @Nullable File instrumentationDir) throws IOException {
        List<InstrumentationDescriptor> descriptors = readDescriptors(jarFiles, instrumentationDir);
        List<InstrumentationDescriptor> descriptorsWithoutAdvice = Lists.newArrayList();
        for (InstrumentationDescriptor descriptor : descriptors) {
            descriptorsWithoutAdvice.add(ImmutableInstrumentationDescriptor.builder()
                    .from(descriptor)
                    .classes(ImmutableList.<String>of())
                    .build());
        }
        return descriptorsWithoutAdvice;
    }

    private static List<InstrumentationDescriptor> readDescriptors(List<File> jarFiles,
            @Nullable File instrumentationDir) throws IOException {
        List<InstrumentationDescriptor> descriptors = Lists.newArrayList();
        for (File jarFile : jarFiles) {
            URL url =
                    new URL("jar:" + jarFile.toURI() + "!/META-INF/xyzzy.instrumentation.json");
            buildDescriptor(url, jarFile, descriptors);
        }
        for (File file : getStandaloneDescriptors(instrumentationDir)) {
            buildDescriptor(file.toURI().toURL(), null, descriptors);
        }
        // also add descriptors on the class path (this is primarily for integration tests)
        for (URL url : getResources("META-INF/xyzzy.instrumentation.json")) {
            buildDescriptor(url, null, descriptors);
        }
        return descriptors;
    }

    private static void buildDescriptor(URL url, @Nullable File jarFile,
            List<InstrumentationDescriptor> descriptors) throws IOException {
        String content = Resources.toString(url, UTF_8);
        ImmutableInstrumentationDescriptor descriptor;
        try {
            descriptor = InstrumentationDescriptors.getGson().fromJson(content,
                    ImmutableInstrumentationDescriptor.class);
        } catch (JsonSyntaxException e) {
            logger.error("error parsing instrumentation descriptor: {}", url.toExternalForm(), e);
            return;
        }
        descriptors.add(descriptor.withJarFile(jarFile));
    }

    private static class DescriptorOrdering extends Ordering<InstrumentationDescriptor> {
        @Override
        public int compare(InstrumentationDescriptor left, InstrumentationDescriptor right) {
            return left.id().compareToIgnoreCase(right.id());
        }
    }
}
