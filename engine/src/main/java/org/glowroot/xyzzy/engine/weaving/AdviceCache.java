/*
 * Copyright 2012-2019 the original author or authors.
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
package org.glowroot.xyzzy.engine.weaving;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.glowroot.xyzzy.engine.config.AdviceConfig;
import org.glowroot.xyzzy.engine.config.InstrumentationDescriptor;
import org.glowroot.xyzzy.engine.weaving.ClassLoaders.LazyDefinedClass;
import org.glowroot.xyzzy.engine.weaving.InstrumentationDetail.MixinClass;
import org.glowroot.xyzzy.engine.weaving.InstrumentationDetail.PointcutClass;
import org.glowroot.xyzzy.engine.weaving.InstrumentationDetail.ShimClass;
import org.glowroot.xyzzy.engine.weaving.Reweaving.PointcutClassName;
import org.glowroot.xyzzy.instrumentation.api.weaving.Shim;

import static com.google.common.base.Preconditions.checkNotNull;

public class AdviceCache {

    private static final Logger logger = LoggerFactory.getLogger(AdviceCache.class);

    private static final AtomicInteger jarFileCounter = new AtomicInteger();

    private final ImmutableList<Advice> nonReweavableAdvisors;
    private final ImmutableList<ShimType> shimTypes;
    private final ImmutableList<MixinType> mixinTypes;
    private final @Nullable Instrumentation instrumentation;
    private final File tmpDir;

    private volatile ImmutableSet<AdviceConfig> reweavableAdviceConfigs;
    private volatile ImmutableList<Advice> reweavableAdvisors;

    private volatile ImmutableList<Advice> allAdvisors;

    public AdviceCache(List<InstrumentationDescriptor> instrumentationDescriptors,
            List<AdviceConfig> reweavableAdviceConfigs,
            @Nullable Instrumentation instrumentation, File tmpDir) throws Exception {

        List<Advice> nonReweavableAdvisors = Lists.newArrayList();
        List<ShimType> shimTypes = Lists.newArrayList();
        List<MixinType> mixinTypes = Lists.newArrayList();
        Map<Advice, LazyDefinedClass> lazyAdvisors = Maps.newHashMap();
        for (InstrumentationDescriptor descriptor : instrumentationDescriptors) {
            InstrumentationDetailBuilder builder = new InstrumentationDetailBuilder(descriptor);
            InstrumentationDetail detail = builder.build();

            nonReweavableAdvisors.addAll(getAdvisors(detail.pointcutClasses()));
            mixinTypes.addAll(getMixinTypes(detail.mixinClasses()));
            shimTypes.addAll(getShimTypes(detail.shimClasses()));

            List<AdviceConfig> configs = descriptor.adviceConfigs();
            for (AdviceConfig config : configs) {
                config.logValidationErrorsIfAny();
            }
            lazyAdvisors.putAll(AdviceGenerator.createAdvisors(configs, descriptor.id(),
                    descriptor.jarFile() != null, false));
        }
        for (Map.Entry<Advice, LazyDefinedClass> entry : lazyAdvisors.entrySet()) {
            nonReweavableAdvisors.add(entry.getKey());
        }
        if (instrumentation == null) {
            // instrumentation is null when debugging with LocalContainer
            ClassLoader isolatedWeavingClassLoader = Thread.currentThread().getContextClassLoader();
            checkNotNull(isolatedWeavingClassLoader);
            ClassLoaders.defineClasses(lazyAdvisors.values(),
                    isolatedWeavingClassLoader);
        } else {
            ClassLoaders.createDirectoryOrCleanPreviousContentsWithPrefix(tmpDir,
                    "instrumentation-generated.jar");
            if (!lazyAdvisors.isEmpty()) {
                File jarFile = new File(tmpDir, "instrumentation-generated.jar");
                ClassLoaders.defineClassesInBootstrapClassLoader(lazyAdvisors.values(),
                        instrumentation, jarFile);
            }
        }
        this.nonReweavableAdvisors = ImmutableList.copyOf(nonReweavableAdvisors);
        this.shimTypes = ImmutableList.copyOf(shimTypes);
        this.mixinTypes = ImmutableList.copyOf(mixinTypes);
        this.instrumentation = instrumentation;
        this.tmpDir = tmpDir;
        this.reweavableAdviceConfigs = ImmutableSet.copyOf(reweavableAdviceConfigs);
        reweavableAdvisors =
                createReweavableAdvisors(reweavableAdviceConfigs, instrumentation, tmpDir, true);
        allAdvisors = ImmutableList
                .copyOf(Iterables.concat(nonReweavableAdvisors, reweavableAdvisors));
    }

    public Supplier<List<Advice>> getAdvisorsSupplier() {
        return new Supplier<List<Advice>>() {
            @Override
            public List<Advice> get() {
                return allAdvisors;
            }
        };
    }

    @VisibleForTesting
    public List<ShimType> getShimTypes() {
        return shimTypes;
    }

    @VisibleForTesting
    public List<MixinType> getMixinTypes() {
        return mixinTypes;
    }

    public void initialReweave(Class<?>[] initialLoadedClasses) {
        Set<PointcutClassName> pointcutClassNames = Sets.newHashSet();
        for (Advice advice : allAdvisors) {
            PointcutClassName pointcutClassName = getPointcutClassName(advice);
            // don't add Runnable/Callable subclasses to initial reweave, since they won't work
            // anyways since too late to add mixin interface
            // this is just an optimization, and (importantly) to keep class retransformation down
            // to a minimum since it has been known to have some problems on some JVMs
            if (pointcutClassName != null
                    && !advice.adviceType().getInternalName().startsWith("org/glowroot"
                            + "/instrumentation/executor/ExecutorInstrumentation$RunnableAdvice")
                    && !advice.adviceType().getInternalName().startsWith("org/glowroot"
                            + "/instrumentation/executor/ExecutorInstrumentation$CallableAdvice")) {
                pointcutClassNames.add(pointcutClassName);
            }
        }
        Reweaving.initialReweave(pointcutClassNames, initialLoadedClasses,
                checkNotNull(instrumentation));
    }

    public void updateAdvisors(List<AdviceConfig> reweavableConfigs)
            throws Exception {
        reweavableAdvisors =
                createReweavableAdvisors(reweavableConfigs, instrumentation, tmpDir, false);
        this.reweavableAdviceConfigs = ImmutableSet.copyOf(reweavableConfigs);
        allAdvisors = ImmutableList
                .copyOf(Iterables.concat(nonReweavableAdvisors, reweavableAdvisors));
    }

    public boolean isOutOfSync(List<AdviceConfig> reweavableAdviceConfigs) {
        return !this.reweavableAdviceConfigs
                .equals(ImmutableSet.copyOf(reweavableAdviceConfigs));
    }

    private static List<Advice> getAdvisors(List<PointcutClass> adviceClasses) {
        List<Advice> advisors = Lists.newArrayList();
        for (PointcutClass adviceClass : adviceClasses) {
            try {
                advisors.add(new AdviceBuilder(adviceClass).build());
            } catch (Throwable t) {
                logger.error("error creating advice: {}", adviceClass.type().getClassName(), t);
            }
        }
        return advisors;
    }

    private static List<MixinType> getMixinTypes(List<MixinClass> mixinClasses) {
        List<MixinType> mixinTypes = Lists.newArrayList();
        for (MixinClass mixinClass : mixinClasses) {
            mixinTypes.add(MixinType.create(mixinClass));
        }
        return mixinTypes;
    }

    private static List<ShimType> getShimTypes(List<ShimClass> shimClasses)
            throws ClassNotFoundException {
        List<ShimType> shimTypes = Lists.newArrayList();
        for (ShimClass shimClass : shimClasses) {
            Class<?> clazz = Class.forName(shimClass.type().getClassName(), false,
                    AdviceCache.class.getClassLoader());
            Shim shim = clazz.getAnnotation(Shim.class);
            if (shim != null) {
                shimTypes.add(ShimType.create(shim, clazz));
            }
        }
        return shimTypes;
    }

    private static ImmutableList<Advice> createReweavableAdvisors(
            List<AdviceConfig> reweavableAdviceConfigs, @Nullable Instrumentation instrumentation,
            File tmpDir, boolean cleanTmpDir) throws Exception {
        ImmutableMap<Advice, LazyDefinedClass> advisors =
                AdviceGenerator.createAdvisors(reweavableAdviceConfigs, null, false, true);
        if (instrumentation == null) {
            // instrumentation is null when debugging with LocalContainer
            ClassLoader isolatedWeavingClassLoader =
                    Thread.currentThread().getContextClassLoader();
            checkNotNull(isolatedWeavingClassLoader);
            ClassLoaders.defineClasses(advisors.values(), isolatedWeavingClassLoader);
        } else {
            if (cleanTmpDir) {
                ClassLoaders.createDirectoryOrCleanPreviousContentsWithPrefix(tmpDir,
                        "config-pointcuts");
            }
            if (!advisors.isEmpty()) {
                String suffix = "";
                int count = jarFileCounter.incrementAndGet();
                if (count > 1) {
                    suffix = "-" + count;
                }
                File jarFile = new File(tmpDir, "config-pointcuts" + suffix + ".jar");
                ClassLoaders.defineClassesInBootstrapClassLoader(advisors.values(), instrumentation,
                        jarFile);
            }
        }
        return advisors.keySet().asList();
    }

    private static @Nullable PointcutClassName getPointcutClassName(Advice advice) {
        PointcutClassName subTypeRestrictionPointcutClassName = null;
        Pattern subTypeRestrictionPattern = advice.pointcutSubTypeRestrictionPattern();
        if (subTypeRestrictionPattern != null) {
            subTypeRestrictionPointcutClassName =
                    PointcutClassName.fromPattern(subTypeRestrictionPattern, null, false);
        } else {
            String subTypeRestriction = advice.pointcut().subTypeRestriction();
            if (!subTypeRestriction.isEmpty()) {
                subTypeRestrictionPointcutClassName =
                        PointcutClassName.fromNonPattern(subTypeRestriction, null, false);
            }
        }
        Pattern classNamePattern = advice.pointcutClassNamePattern();
        if (classNamePattern != null) {
            return PointcutClassName.fromPattern(classNamePattern,
                    subTypeRestrictionPointcutClassName,
                    advice.pointcut().methodName().equals("<init>"));
        }
        String className = advice.pointcut().className();
        if (!className.isEmpty()) {
            return PointcutClassName.fromNonPattern(className, subTypeRestrictionPointcutClassName,
                    advice.pointcut().methodName().equals("<init>"));
        }
        return null;
    }

    // this method exists because tests cannot use (sometimes) shaded guava Supplier
    //
    // only used by tests
    public List<Advice> getAdvisors() {
        return getAdvisorsSupplier().get();
    }
}
