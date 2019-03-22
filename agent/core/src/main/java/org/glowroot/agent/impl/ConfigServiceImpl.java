/*
 * Copyright 2011-2019 the original author or authors.
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
package org.glowroot.agent.impl;

import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.glowroot.agent.config.ConfigService;
import org.glowroot.agent.config.InstrumentationConfig;
import org.glowroot.xyzzy.engine.config.InstrumentationDescriptor;
import org.glowroot.xyzzy.instrumentation.api.config.BooleanProperty;
import org.glowroot.xyzzy.instrumentation.api.config.ConfigListener;
import org.glowroot.xyzzy.instrumentation.api.config.DoubleProperty;
import org.glowroot.xyzzy.instrumentation.api.config.ListProperty;
import org.glowroot.xyzzy.instrumentation.api.config.StringProperty;

import static com.google.common.base.Preconditions.checkNotNull;

public class ConfigServiceImpl
        implements org.glowroot.xyzzy.instrumentation.api.config.ConfigService, ConfigListener {

    private static final Logger logger = LoggerFactory.getLogger(ConfigServiceImpl.class);

    private final ConfigService configService;

    // this is either the id of a registered instrumentation or it is null (see validation in
    // constructor)
    private final @Nullable String id;

    // cache for fast read access
    // visibility is provided by memoryBarrier in org.glowroot.config.ConfigService
    private @MonotonicNonNull InstrumentationConfig config;

    private final Map<ConfigListener, Boolean> configListeners =
            new MapMaker().weakKeys().makeMap();

    public static ConfigServiceImpl create(ConfigService configService,
            List<InstrumentationDescriptor> descriptors, String id) {
        ConfigServiceImpl configServiceImpl = new ConfigServiceImpl(configService, descriptors, id);
        configService.addInstrumentationConfigListener(configServiceImpl);
        configService.addConfigListener(configServiceImpl);
        return configServiceImpl;
    }

    private ConfigServiceImpl(ConfigService configService,
            List<InstrumentationDescriptor> descriptors, String id) {
        this.configService = configService;
        InstrumentationConfig config = configService.getInstrumentationConfig(id);
        if (config == null) {
            if (descriptors.isEmpty()) {
                logger.warn("unexpected instrumentation id: {} (there is no available"
                        + " instrumentation)", id);
            } else {
                List<String> ids = Lists.newArrayList();
                for (InstrumentationDescriptor descriptor : descriptors) {
                    ids.add(descriptor.id());
                }
                logger.warn("unexpected instrumentation id: {} (available instrumentation ids are"
                        + " {})", id, Joiner.on(", ").join(ids));
            }
            this.id = null;
        } else {
            this.id = id;
        }
    }

    @Override
    public StringProperty getStringProperty(String name) {
        if (name == null) {
            logger.error("getStringProperty(): argument 'name' must be non-null");
            return new StringPropertyImpl("");
        }
        StringPropertyImpl stringProperty = new StringPropertyImpl(name);
        configListeners.put(stringProperty, true);
        return stringProperty;
    }

    @Override
    public BooleanProperty getBooleanProperty(String name) {
        if (name == null) {
            logger.error("getBooleanProperty(): argument 'name' must be non-null");
            return new BooleanPropertyImpl("");
        }
        BooleanPropertyImpl booleanProperty = new BooleanPropertyImpl(name);
        configListeners.put(booleanProperty, true);
        return booleanProperty;
    }

    @Override
    public DoubleProperty getDoubleProperty(String name) {
        if (name == null) {
            logger.error("getDoubleProperty(): argument 'name' must be non-null");
            return new DoublePropertyImpl("");
        }
        DoublePropertyImpl doubleProperty = new DoublePropertyImpl(name);
        configListeners.put(doubleProperty, true);
        return doubleProperty;
    }

    @Override
    public ListProperty getListProperty(String name) {
        if (name == null) {
            logger.error("getListProperty(): argument 'name' must be non-null");
            return new ListPropertyImpl("");
        }
        ListPropertyImpl listProperty = new ListPropertyImpl(name);
        configListeners.put(listProperty, true);
        return listProperty;
    }

    @Override
    public void registerConfigListener(ConfigListener listener) {
        if (id == null) {
            return;
        }
        if (listener == null) {
            logger.error("registerConfigListener(): argument 'listener' must be non-null");
            return;
        }
        configService.addInstrumentationConfigListener(listener);
        listener.onChange();
        configService.writeMemoryBarrier();
    }

    @Override
    public void onChange() {
        if (id != null) {
            InstrumentationConfig config = configService.getInstrumentationConfig(id);
            // config should not be null since the id was already validated at construction time and
            // instrumentation cannot be removed (or their ids changed) at runtime
            checkNotNull(config);
            this.config = config;
        }
        for (ConfigListener configListener : configListeners.keySet()) {
            configListener.onChange();
        }
        configService.writeMemoryBarrier();
    }

    private class StringPropertyImpl implements StringProperty, ConfigListener {
        private final String name;
        // visibility is provided by memoryBarrier in outer class
        private String value = "";
        private StringPropertyImpl(String name) {
            this.name = name;
            if (config != null) {
                value = config.getStringProperty(name);
            }
        }
        @Override
        public String value() {
            return value;
        }
        @Override
        public void onChange() {
            if (config != null) {
                value = config.getStringProperty(name);
            }
        }
    }

    private class BooleanPropertyImpl implements BooleanProperty, ConfigListener {
        private final String name;
        // visibility is provided by memoryBarrier in outer class
        private boolean value;
        private BooleanPropertyImpl(String name) {
            this.name = name;
            if (config != null) {
                value = config.getBooleanProperty(name);
            }
        }
        @Override
        public boolean value() {
            return value;
        }
        @Override
        public void onChange() {
            if (config != null) {
                value = config.getBooleanProperty(name);
            }
        }
    }

    private class DoublePropertyImpl implements DoubleProperty, ConfigListener {
        private final String name;
        // visibility is provided by memoryBarrier in outer class
        private @Nullable Double value;
        private DoublePropertyImpl(String name) {
            this.name = name;
            if (config != null) {
                value = config.getDoubleProperty(name);
            }
        }
        @Override
        public @Nullable Double value() {
            return value;
        }
        @Override
        public void onChange() {
            if (config != null) {
                value = config.getDoubleProperty(name);
            }
        }
    }

    private class ListPropertyImpl implements ListProperty, ConfigListener {
        private final String name;
        // visibility is provided by memoryBarrier in outer class
        private List<String> value = ImmutableList.of();
        private ListPropertyImpl(String name) {
            this.name = name;
            if (config != null) {
                value = config.getListProperty(name);
            }
        }
        @Override
        public List<String> value() {
            return value;
        }
        @Override
        public void onChange() {
            if (config != null) {
                value = config.getListProperty(name);
            }
        }
    }
}
