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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Beans {

    private static final Logger logger = LoggerFactory.getLogger(Beans.class);

    // sentinel method is used to represent null value in the weak valued ConcurrentMap below
    // using guava's Optional would make the weakness on the Optional instance instead of on the
    // Method instance which would cause unnecessary clearing of the map values
    private static final Accessor SENTINEL_ACCESSOR;

    static {
        try {
            SENTINEL_ACCESSOR =
                    Accessor.fromMethod(Beans.class.getDeclaredMethod("sentinelMethod"));
        } catch (Exception e) {
            // unrecoverable error
            throw new AssertionError(e);
        }
    }

    // note, not using nested loading cache since the nested loading cache maintains a strong
    // reference to the class loader
    //
    // weak keys in loading cache to prevent Class retention
    private static final LoadingCache<Class<?>, ConcurrentMap<String, Accessor>> getters =
            CacheBuilder.newBuilder()
                    .weakKeys()
                    .build(new CacheLoader<Class<?>, ConcurrentMap<String, Accessor>>() {
                        @Override
                        public ConcurrentMap<String, Accessor> load(Class<?> clazz) {
                            // weak values since Method has a strong reference to its Class which
                            // is used as the key in the outer loading cache
                            return new MapMaker().weakValues().makeMap();
                        }
                    });

    private Beans() {}

    public static @Nullable Object value(@Nullable Object obj, List<String> path) throws Exception {
        return value(obj, path, 0);
    }

    private static @Nullable Object value(@Nullable Object obj, List<String> path, int currIndex)
            throws Exception {
        if (obj == null) {
            return null;
        }
        if (currIndex == path.size()) {
            return obj;
        }
        String curr = path.get(currIndex);
        if (obj instanceof Map) {
            if (curr.equals("size")) {
                // special case
                return ((Map<?, ?>) obj).size();
            } else {
                return value(((Map<?, ?>) obj).get(curr), path, currIndex + 1);
            }
        }
        if (obj instanceof List) {
            if (curr.equals("size")) {
                // special case
                return ((List<?>) obj).size();
            } else {
                List</*@Nullable*/ Object> values = Lists.newArrayList();
                for (Object val : (List<?>) obj) {
                    values.add(value(val, path, currIndex));
                }
                return values;
            }
        }
        Accessor accessor = getAccessor(obj.getClass(), curr);
        if (accessor.equals(SENTINEL_ACCESSOR)) {
            // no appropriate method found, dynamic paths that may or may not resolve
            // correctly are ok, just return null
            return null;
        }
        Object currItem = accessor.evaluate(obj);
        return value(currItem, path, currIndex + 1);
    }

    private static Accessor getAccessor(Class<?> clazz, String name) {
        ConcurrentMap<String, Accessor> accessorsForType = getters.getUnchecked(clazz);
        Accessor accessor = accessorsForType.get(name);
        if (accessor == null) {
            accessor = loadPossiblyArrayBasedAccessor(clazz, name);
            if (accessor == null) {
                accessor = SENTINEL_ACCESSOR;
            }
            accessorsForType.put(name, accessor);
        }
        return accessor;
    }

    static @Nullable Accessor loadPossiblyArrayBasedAccessor(Class<?> clazz, String name) {
        if (clazz.getComponentType() != null && name.equals("length")) {
            return Accessor.arrayLength();
        }
        Class<?> componentType = clazz;
        while (componentType.getComponentType() != null) {
            componentType = componentType.getComponentType();
        }
        return loadAccessor(componentType, name);
    }

    private static @Nullable Accessor loadAccessor(Class<?> clazz, String name) {
        String capitalizedName = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        try {
            Method method = getAnyMethod(clazz, "get" + capitalizedName);
            return Accessor.fromMethod(method);
        } catch (Exception e) {
            // log exception at trace level
            logger.trace(e.getMessage(), e);
        }
        try {
            Method method = getAnyMethod(clazz, "is" + capitalizedName);
            return Accessor.fromMethod(method);
        } catch (Exception f) {
            // log exception at trace level
            logger.trace(f.getMessage(), f);
        }
        try {
            Method method = getAnyMethod(clazz, name);
            return Accessor.fromMethod(method);
        } catch (Exception g) {
            // log exception at trace level
            logger.trace(g.getMessage(), g);
        }
        try {
            Field field = getAnyField(clazz, name);
            return Accessor.fromField(field);
        } catch (Exception h) {
            // log exception at trace level
            logger.trace(h.getMessage(), h);
        }
        // log general failure message at debug level
        logger.debug("no accessor found for {} in class {}", name, clazz.getName());
        return null;
    }

    private static Method getAnyMethod(Class<?> clazz, String name, Class<?>... parameterTypes)
            throws Exception {
        try {
            Method method = clazz.getMethod(name, parameterTypes);
            // even though getMethod() above only returns public method, still need to
            // setAccessible() in case class is package-private
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            // log exception at trace level
            logger.trace(e.getMessage(), e);
            return getAnyDeclaredMethod(clazz, name, parameterTypes);
        } catch (NoClassDefFoundError e) {
            // NoClassDefFoundError is thrown if any method signature in clazz references a missing
            // class (see MissingOptionalDependenciesReflectionTest.java)
            // seems best to treat same as NoSuchMethodException

            // log exception at trace level
            logger.trace(e.getMessage(), e);
            return getAnyDeclaredMethod(clazz, name, parameterTypes);
        }
    }

    private static Field getAnyField(Class<?> clazz, String fieldName) throws Exception {
        try {
            Field field = clazz.getField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            // log exception at trace level
            logger.trace(e.getMessage(), e);
            return getAnyDeclaredField(clazz, fieldName);
        }
    }

    private static Method getAnyDeclaredMethod(Class<?> clazz, String name,
            Class<?>... parameterTypes) throws Exception {
        try {
            Method method = clazz.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            // log exception at trace level
            logger.trace(e.getMessage(), e);
            Class<?> superClass = clazz.getSuperclass();
            if (superClass == null) {
                throw e;
            }
            return getAnyDeclaredMethod(superClass, name, parameterTypes);
        } catch (NoClassDefFoundError e) {
            // NoClassDefFoundError is thrown if any method signature in clazz references a missing
            // class (see MissingOptionalDependenciesReflectionTest.java)
            // seems best to treat same as NoSuchMethodException

            // log exception at trace level
            logger.trace(e.getMessage(), e);
            Class<?> superClass = clazz.getSuperclass();
            if (superClass == null) {
                throw new NoSuchMethodException();
            }
            return getAnyDeclaredMethod(superClass, name, parameterTypes);
        }
    }

    private static Field getAnyDeclaredField(Class<?> clazz, String fieldName) throws Exception {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            // log exception at trace level
            logger.trace(e.getMessage(), e);
            Class<?> superClass = clazz.getSuperclass();
            if (superClass == null) {
                throw e;
            }
            return getAnyDeclaredField(superClass, fieldName);
        }
    }

    // this unused private method is required for use as SENTINEL_METHOD above
    @SuppressWarnings("unused")
    private static void sentinelMethod() {}
}
