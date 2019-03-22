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
package org.glowroot.xyzzy.engine.impl;

import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.glowroot.xyzzy.engine.config.InstrumentationDescriptor;
import org.glowroot.xyzzy.engine.config.PropertyDescriptor;
import org.glowroot.xyzzy.engine.impl.InstrumentationServiceImpl.ConfigServiceFactory;
import org.glowroot.xyzzy.instrumentation.api.config.BooleanProperty;
import org.glowroot.xyzzy.instrumentation.api.config.ConfigListener;
import org.glowroot.xyzzy.instrumentation.api.config.ConfigService;
import org.glowroot.xyzzy.instrumentation.api.config.DoubleProperty;
import org.glowroot.xyzzy.instrumentation.api.config.ListProperty;
import org.glowroot.xyzzy.instrumentation.api.config.StringProperty;
import org.glowroot.xyzzy.instrumentation.api.util.ImmutableMap;

public class SimpleConfigServiceFactory implements ConfigServiceFactory {

    private static final Logger logger = LoggerFactory.getLogger(SimpleConfigServiceFactory.class);

    private final List<InstrumentationDescriptor> instrumentationDescriptors;

    public SimpleConfigServiceFactory(List<InstrumentationDescriptor> instrumentationDescriptors) {
        this.instrumentationDescriptors = instrumentationDescriptors;
    }

    @Override
    public ConfigService create(String instrumentationId) {
        InstrumentationDescriptor descriptor =
                getDescriptor(instrumentationId, instrumentationDescriptors);
        if (descriptor == null) {
            return new ConfigServiceImpl(ImmutableList.<PropertyDescriptor>of());
        } else {
            return new ConfigServiceImpl(descriptor.properties());
        }
    }

    private static @Nullable InstrumentationDescriptor getDescriptor(String id,
            List<InstrumentationDescriptor> descriptors) {
        for (InstrumentationDescriptor descriptor : descriptors) {
            if (id.equals(descriptor.id())) {
                return descriptor;
            }
        }
        if (descriptors.isEmpty()) {
            logger.warn("unexpected instrumentation id: {} (there is no available instrumentation)",
                    id);
        } else {
            List<String> ids = Lists.newArrayList();
            for (InstrumentationDescriptor descriptor : descriptors) {
                ids.add(descriptor.id());
            }
            logger.warn("unexpected instrumentation id: {} (available instrumentation ids are {})",
                    id, Joiner.on(", ").join(ids));
        }
        return null;
    }

    private static class ConfigServiceImpl implements ConfigService {

        private final Map<String, StringProperty> stringProperties;
        private final Map<String, BooleanProperty> booleanProperties;
        private final Map<String, DoubleProperty> doubleProperties;
        private final Map<String, ListProperty> listProperties;

        private final StringProperty defaultStringProperty = new StringPropertyImpl("");
        private final BooleanProperty defaultBooleanProperty = new BooleanPropertyImpl(false);
        private final DoubleProperty defaultDoubleProperty = new DoublePropertyImpl(null);
        private final ListProperty defaultListProperty =
                new ListPropertyImpl(Lists.<String>newArrayList());

        private ConfigServiceImpl(List<PropertyDescriptor> propertyDescriptors) {
            Map<String, StringProperty> stringProperties = Maps.newHashMap();
            Map<String, BooleanProperty> booleanProperties = Maps.newHashMap();
            Map<String, DoubleProperty> doubleProperties = Maps.newHashMap();
            Map<String, ListProperty> listProperties = Maps.newHashMap();
            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                String name = propertyDescriptor.name();
                Object value = propertyDescriptor.getValidatedNonNullDefaultValue().value();
                switch (propertyDescriptor.type()) {
                    case STRING:
                        if (value instanceof String) {
                            stringProperties.put(name, new StringPropertyImpl((String) value));
                        } else {
                            logUnexpectedValueType(name, String.class, value);
                        }
                        break;
                    case BOOLEAN:
                        if (value instanceof Boolean) {
                            booleanProperties.put(name, new BooleanPropertyImpl((Boolean) value));
                        } else {
                            logUnexpectedValueType(name, Boolean.class, value);
                        }
                        break;
                    case DOUBLE:
                        if (value == null || value instanceof Double) {
                            doubleProperties.put(name, new DoublePropertyImpl((Double) value));
                        } else {
                            logUnexpectedValueType(name, Double.class, value);
                        }
                        break;
                    case LIST:
                        if (value instanceof List) {
                            List<String> list = Lists.newArrayList();
                            for (Object val : (List<?>) value) {
                                if (val instanceof String) {
                                    list.add((String) val);
                                }
                            }
                            listProperties.put(name, new ListPropertyImpl(list));
                        } else {
                            logUnexpectedValueType(name, List.class, value);
                        }
                        break;
                    default:
                }
            }

            this.stringProperties = ImmutableMap.copyOf(stringProperties);
            this.booleanProperties = ImmutableMap.copyOf(booleanProperties);
            this.doubleProperties = ImmutableMap.copyOf(doubleProperties);
            this.listProperties = ImmutableMap.copyOf(listProperties);
        }

        @Override
        public void registerConfigListener(ConfigListener listener) {}

        @Override
        public StringProperty getStringProperty(String name) {
            return MoreObjects.firstNonNull(stringProperties.get(name), defaultStringProperty);
        }

        @Override
        public BooleanProperty getBooleanProperty(String name) {
            return MoreObjects.firstNonNull(booleanProperties.get(name), defaultBooleanProperty);
        }

        @Override
        public DoubleProperty getDoubleProperty(String name) {
            return MoreObjects.firstNonNull(doubleProperties.get(name), defaultDoubleProperty);
        }

        @Override
        public ListProperty getListProperty(String name) {
            return MoreObjects.firstNonNull(listProperties.get(name), defaultListProperty);
        }

        private static void logUnexpectedValueType(String name, Class<?> expectedType,
                @Nullable Object value) {
            logger.error("unexpected value for property \"{}\", expected \"{}\" but found: \"{}\"",
                    name, expectedType.getName(),
                    value == null ? "null" : value.getClass().getName());
        }
    }

    private static class StringPropertyImpl implements StringProperty {

        private final String value;

        private StringPropertyImpl(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }

    private static class BooleanPropertyImpl implements BooleanProperty {

        private final boolean value;

        private BooleanPropertyImpl(boolean value) {
            this.value = value;
        }

        @Override
        public boolean value() {
            return value;
        }
    }

    private static class DoublePropertyImpl implements DoubleProperty {

        private final @Nullable Double value;

        private DoublePropertyImpl(@Nullable Double value) {
            this.value = value;
        }

        @Override
        public @Nullable Double value() {
            return value;
        }
    }

    private static class ListPropertyImpl implements ListProperty {

        private final List<String> value;

        private ListPropertyImpl(List<String> value) {
            this.value = value;
        }

        @Override
        public List<String> value() {
            return value;
        }
    }
}
