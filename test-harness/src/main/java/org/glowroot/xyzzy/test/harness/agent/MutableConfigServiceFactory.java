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
package org.glowroot.xyzzy.test.harness.agent;

import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.glowroot.xyzzy.engine.config.InstrumentationDescriptor;
import org.glowroot.xyzzy.engine.config.PropertyDescriptor;
import org.glowroot.xyzzy.engine.impl.InstrumentationServiceImpl.ConfigServiceFactory;
import org.glowroot.xyzzy.instrumentation.api.config.BooleanProperty;
import org.glowroot.xyzzy.instrumentation.api.config.ConfigListener;
import org.glowroot.xyzzy.instrumentation.api.config.ConfigService;
import org.glowroot.xyzzy.instrumentation.api.config.DoubleProperty;
import org.glowroot.xyzzy.instrumentation.api.config.ListProperty;
import org.glowroot.xyzzy.instrumentation.api.config.StringProperty;

public class MutableConfigServiceFactory implements ConfigServiceFactory {

    private final Map<String, InstrumentationDescriptor> instrumentationDescriptors;

    private final Map<String, MutableConfigService> configServices = Maps.newConcurrentMap();

    public MutableConfigServiceFactory(List<InstrumentationDescriptor> descriptors) {
        instrumentationDescriptors = Maps.newHashMap();
        for (InstrumentationDescriptor descriptor : descriptors) {
            instrumentationDescriptors.put(descriptor.id(), descriptor);
        }
    }

    @Override
    public ConfigService create(String instrumentationId) {
        InstrumentationDescriptor descriptor = instrumentationDescriptors.get(instrumentationId);
        if (descriptor == null) {
            throw unexpectedInstrumentationId(instrumentationId);
        }
        MutableConfigService configService = new MutableConfigService(descriptor.properties());
        configServices.put(instrumentationId, configService);
        return configService;
    }

    public void setInstrumentationProperty(String instrumentationId, String propertyName,
            boolean propertyValue) throws Exception {
        MutableConfigService configService = configServices.get(instrumentationId);
        if (configService == null) {
            throw new IllegalStateException("Unexpected instrumentation id: " + instrumentationId);
        }
        BooleanPropertyImpl booleanProperty = configService.booleanProperties.get(propertyName);
        if (booleanProperty == null) {
            throw new IllegalStateException("Unexpected boolean property name: " + propertyName);
        }
        booleanProperty.value = propertyValue;
    }

    public void setInstrumentationProperty(String instrumentationId, String propertyName,
            @Nullable Double propertyValue) throws Exception {
        MutableConfigService configService = configServices.get(instrumentationId);
        if (configService == null) {
            throw new IllegalStateException("Unexpected instrumentation id: " + instrumentationId);
        }
        DoublePropertyImpl doubleProperty = configService.doubleProperties.get(propertyName);
        if (doubleProperty == null) {
            throw new IllegalStateException("Unexpected double property name: " + propertyName);
        }
        doubleProperty.value = propertyValue;
    }

    public void setInstrumentationProperty(String instrumentationId, String propertyName,
            String propertyValue) throws Exception {
        MutableConfigService configService = configServices.get(instrumentationId);
        if (configService == null) {
            throw new IllegalStateException("Unexpected instrumentation id: " + instrumentationId);
        }
        StringPropertyImpl stringProperty = configService.stringProperties.get(propertyName);
        if (stringProperty == null) {
            throw new IllegalStateException("Unexpected string property name: " + propertyName);
        }
        stringProperty.value = propertyValue;
    }

    public void setInstrumentationProperty(String instrumentationId, String propertyName,
            List<String> propertyValue) throws Exception {
        MutableConfigService configService = configServices.get(instrumentationId);
        if (configService == null) {
            throw new IllegalStateException("Unexpected instrumentation id: " + instrumentationId);
        }
        ListPropertyImpl listProperty = configService.listProperties.get(propertyName);
        if (listProperty == null) {
            throw new IllegalStateException("Unexpected list property name: " + propertyName);
        }
        listProperty.value = propertyValue;
    }

    public void resetConfig() {
        for (MutableConfigService configService : configServices.values()) {
            configService.resetConfig();
        }
    }

    private RuntimeException unexpectedInstrumentationId(String id) {
        if (instrumentationDescriptors.isEmpty()) {
            return new IllegalStateException("Unexpected instrumentation id: " + id
                    + " (there is no available instrumentation)");
        } else {
            List<String> ids = Lists.newArrayList();
            for (InstrumentationDescriptor descriptor : instrumentationDescriptors.values()) {
                ids.add(descriptor.id());
            }
            return new IllegalStateException("Unexpected instrumentation id: " + id
                    + " (available instrumentation ids are " + Joiner.on(", ").join(ids) + ")");
        }
    }

    private static class MutableConfigService implements ConfigService {

        private final Map<String, StringPropertyImpl> stringProperties;
        private final Map<String, BooleanPropertyImpl> booleanProperties;
        private final Map<String, DoublePropertyImpl> doubleProperties;
        private final Map<String, ListPropertyImpl> listProperties;

        private final List<PropertyDescriptor> propertyDescriptors;

        private MutableConfigService(List<PropertyDescriptor> propertyDescriptors) {
            Map<String, StringPropertyImpl> stringProperties = Maps.newHashMap();
            Map<String, BooleanPropertyImpl> booleanProperties = Maps.newHashMap();
            Map<String, DoublePropertyImpl> doubleProperties = Maps.newHashMap();
            Map<String, ListPropertyImpl> listProperties = Maps.newHashMap();
            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                String name = propertyDescriptor.name();
                Object value = propertyDescriptor.getValidatedNonNullDefaultValue().value();
                switch (propertyDescriptor.type()) {
                    case STRING:
                        if (value instanceof String) {
                            stringProperties.put(name, new StringPropertyImpl((String) value));
                        } else {
                            throw unexpectedValueType(name, "string", value);
                        }
                        break;
                    case BOOLEAN:
                        if (value instanceof Boolean) {
                            booleanProperties.put(name, new BooleanPropertyImpl((Boolean) value));
                        } else {
                            throw unexpectedValueType(name, "boolean", value);
                        }
                        break;
                    case DOUBLE:
                        if (value == null || value instanceof Double) {
                            doubleProperties.put(name, new DoublePropertyImpl((Double) value));
                        } else {
                            throw unexpectedValueType(name, "number", value);
                        }
                        break;
                    case LIST:
                        if (value instanceof List) {
                            List<String> stringList = Lists.newArrayList();
                            for (Object val : (List<?>) value) {
                                if (val instanceof String) {
                                    stringList.add((String) val);
                                } else {
                                    throw unexpectedValueTypeForList(name, val);
                                }
                            }
                            listProperties.put(name, new ListPropertyImpl(stringList));
                        } else {
                            throw unexpectedValueType(name, "list", value);
                        }
                        break;
                    default:
                        throw new IllegalStateException("Unexpected property descriptor type: "
                                + propertyDescriptor.type());
                }
            }

            this.stringProperties = ImmutableMap.copyOf(stringProperties);
            this.booleanProperties = ImmutableMap.copyOf(booleanProperties);
            this.doubleProperties = ImmutableMap.copyOf(doubleProperties);
            this.listProperties = ImmutableMap.copyOf(listProperties);
            this.propertyDescriptors = propertyDescriptors;
        }

        public void resetConfig() {
            // properties have already been validated during construction
            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                String name = propertyDescriptor.name();
                Object value = propertyDescriptor.getValidatedNonNullDefaultValue().value();
                switch (propertyDescriptor.type()) {
                    case STRING:
                        stringProperties.get(name).value = (String) value;
                        break;
                    case BOOLEAN:
                        booleanProperties.get(name).value = (Boolean) value;
                        break;
                    case DOUBLE:
                        doubleProperties.get(name).value = (Double) value;
                        break;
                    case LIST:
                        List<String> stringList = Lists.newArrayList();
                        for (Object val : (List<?>) value) {
                            stringList.add((String) val);
                        }
                        listProperties.get(name).value = stringList;
                        break;
                    default:
                        throw new IllegalStateException("Unexpected property descriptor type: "
                                + propertyDescriptor.type());
                }
            }
        }

        @Override
        public void registerConfigListener(ConfigListener listener) {}

        @Override
        public StringProperty getStringProperty(String name) {
            StringProperty stringProperty = stringProperties.get(name);
            if (stringProperty == null) {
                throw new IllegalStateException("No such string property: " + name);
            }
            return stringProperty;
        }

        @Override
        public BooleanProperty getBooleanProperty(String name) {
            BooleanProperty booleanProperty = booleanProperties.get(name);
            if (booleanProperty == null) {
                throw new IllegalStateException("No such boolean property: " + name);
            }
            return booleanProperty;
        }

        @Override
        public DoubleProperty getDoubleProperty(String name) {
            DoubleProperty doubleProperty = doubleProperties.get(name);
            if (doubleProperty == null) {
                throw new IllegalStateException("No such double property: " + name);
            }
            return doubleProperty;
        }

        @Override
        public ListProperty getListProperty(String name) {
            ListProperty listProperty = listProperties.get(name);
            if (listProperty == null) {
                throw new IllegalStateException("No such list property: " + name);
            }
            return listProperty;
        }

        private static RuntimeException unexpectedValueType(String propertyName,
                String propertyType, @Nullable Object value) {
            String found = value == null ? "null" : value.getClass().getSimpleName();
            return new IllegalStateException("Unexpected value for " + propertyType + " property "
                    + propertyName + ": " + found);
        }

        private static RuntimeException unexpectedValueTypeForList(String propertyName,
                @Nullable Object value) {
            String found = value == null ? "null" : value.getClass().getSimpleName();
            return new IllegalStateException(
                    "Unexpected value for element of list property " + propertyName + ": " + found);
        }
    }

    private static class StringPropertyImpl implements StringProperty {

        private volatile String value;

        private StringPropertyImpl(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }

    private static class BooleanPropertyImpl implements BooleanProperty {

        private volatile boolean value;

        private BooleanPropertyImpl(boolean value) {
            this.value = value;
        }

        @Override
        public boolean value() {
            return value;
        }
    }

    private static class DoublePropertyImpl implements DoubleProperty {

        private volatile @Nullable Double value;

        private DoublePropertyImpl(@Nullable Double value) {
            this.value = value;
        }

        @Override
        public @Nullable Double value() {
            return value;
        }
    }

    private static class ListPropertyImpl implements ListProperty {

        private volatile List<String> value;

        private ListPropertyImpl(List<String> value) {
            this.value = value;
        }

        @Override
        public List<String> value() {
            return value;
        }
    }
}
