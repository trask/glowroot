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
package org.glowroot.xyzzy.engine.weaving;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.Lists;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.objectweb.asm.Type;

import org.glowroot.xyzzy.engine.bytecode.api.Util;
import org.glowroot.xyzzy.instrumentation.api.ClassInfo;
import org.glowroot.xyzzy.instrumentation.api.MethodInfo;

import static com.google.common.base.Preconditions.checkNotNull;

// can't generate classes in bootstrap class loader, so this is needed for storing meta holders
// similar technique is not good for non-bootstrap class loaders anyways since then weak references
// would need to be used to prevent retention of meta holders
public class BootstrapMetaHolders {

    private static final Map<String, Integer> classMetaHolderIndexes =
            new ConcurrentHashMap<String, Integer>();
    private static final Map<String, Integer> methodMetaHolderIndexes =
            new ConcurrentHashMap<String, Integer>();
    private static final List</*@Nullable*/ ClassMetaHolder> classMetaHolders =
            Lists.newCopyOnWriteArrayList();
    private static final List</*@Nullable*/ MethodMetaHolder> methodMetaHolders =
            Lists.newCopyOnWriteArrayList();

    private BootstrapMetaHolders() {}

    static int reserveClassMetaHolderIndex(String metaHolderInternalName,
            String classMetaFieldName) {
        synchronized (classMetaHolders) {
            String key = metaHolderInternalName + '.' + classMetaFieldName;
            Integer index = classMetaHolderIndexes.get(key);
            if (index == null) {
                classMetaHolders.add(null);
                index = classMetaHolders.size() - 1;
                classMetaHolderIndexes.put(key, index);
            }
            return index;
        }
    }

    static int reserveMethodMetaHolderIndex(String metaHolderInternalName,
            String methodMetaFieldName) {
        synchronized (methodMetaHolders) {
            methodMetaHolders.add(null);
            int index = methodMetaHolders.size() - 1;
            methodMetaHolderIndexes.put(metaHolderInternalName + '.' + methodMetaFieldName, index);
            return index;
        }
    }

    static void createClassMetaHolder(String metaHolderInternalName,
            String classMetaFieldName, Type classMetaType, Type type) {
        String key = metaHolderInternalName + '.' + classMetaFieldName;
        Integer index = classMetaHolderIndexes.get(key);
        checkNotNull(index, "ClassMetaHolder was not reserved for key: " + key);
        ClassMetaHolder classMetaHolder = new ClassMetaHolder(classMetaType, type);
        classMetaHolders.set(index, classMetaHolder);
    }

    static void createMethodMetaHolder(String metaHolderInternalName,
            String methodMetaFieldName, Type methodMetaType, Type methodOwnerType,
            String methodName, Type methodReturnType, List<Type> methodParameterTypes) {
        String key = metaHolderInternalName + '.' + methodMetaFieldName;
        Integer index = methodMetaHolderIndexes.get(key);
        checkNotNull(index, "MethodMetaHolder was not reserved for key: " + key);
        MethodMetaHolder methodMetaHolder = new MethodMetaHolder(methodMetaType, methodOwnerType,
                methodName, methodReturnType, methodParameterTypes);
        methodMetaHolders.set(index, methodMetaHolder);
    }

    public static Object getClassMeta(int index) throws Exception {
        ClassMetaHolder classMetaHolder = classMetaHolders.get(index);
        checkNotNull(classMetaHolder, "ClassMetaHolder was not instantiated for index: " + index);
        return classMetaHolder.getClassMeta();
    }

    public static Object getMethodMeta(int index) throws Exception {
        MethodMetaHolder methodMetaHolder = methodMetaHolders.get(index);
        checkNotNull(methodMetaHolder, "MethodMetaHolder was not instantiated for index: " + index);
        return methodMetaHolder.getMethodMeta();
    }

    private static Class<?> getType(Type type) throws ClassNotFoundException {
        switch (type.getSort()) {
            case Type.VOID:
                return void.class;
            case Type.BOOLEAN:
                return boolean.class;
            case Type.CHAR:
                return char.class;
            case Type.BYTE:
                return byte.class;
            case Type.SHORT:
                return short.class;
            case Type.INT:
                return int.class;
            case Type.FLOAT:
                return float.class;
            case Type.LONG:
                return long.class;
            case Type.DOUBLE:
                return double.class;
            case Type.ARRAY:
                return Util.getArrayClass(getType(type.getElementType()),
                        type.getDimensions());
            default:
                return Class.forName(type.getClassName(), false, null);
        }
    }

    private static class ClassMetaHolder {

        private final Type classMetaType;
        private final Type type;
        private volatile @MonotonicNonNull Object classMeta;

        private ClassMetaHolder(Type classMetaType, Type type) {
            this.classMetaType = classMetaType;
            this.type = type;
        }

        private Object getClassMeta() throws Exception {
            Object classMetaLocal = classMeta;
            if (classMetaLocal != null) {
                return classMetaLocal;
            }
            synchronized (this) {
                if (classMeta == null) {
                    ClassInfo classInfo = new ClassInfoImpl(type.getClassName(), null);
                    classMeta = getType(classMetaType).getConstructor(ClassInfo.class)
                            .newInstance(classInfo);
                }
            }
            return classMeta;
        }
    }

    private static class MethodMetaHolder {

        private final Type methodMetaType;
        private final Type methodOwnerType;
        private final String methodName;
        private final Type methodReturnType;
        private final List<Type> methodParameterTypes;
        private volatile @MonotonicNonNull Object methodMeta;

        private MethodMetaHolder(Type methodMetaType, Type methodOwnerType, String methodName,
                Type methodReturnType, List<Type> methodParameterTypes) {
            this.methodMetaType = methodMetaType;
            this.methodOwnerType = methodOwnerType;
            this.methodName = methodName;
            this.methodReturnType = methodReturnType;
            this.methodParameterTypes = methodParameterTypes;
        }

        private Object getMethodMeta() throws Exception {
            Object methodMetaLocal = methodMeta;
            if (methodMetaLocal != null) {
                return methodMetaLocal;
            }
            synchronized (this) {
                if (methodMeta == null) {
                    List<Class<?>> methodParameterClasses =
                            Lists.newArrayListWithCapacity(methodParameterTypes.size());
                    for (Type methodParameterType : methodParameterTypes) {
                        methodParameterClasses.add(getType(methodParameterType));
                    }
                    MethodInfo methodInfo =
                            new MethodInfoImpl(methodName, getType(methodReturnType),
                                    methodParameterClasses, methodOwnerType.getClassName(), null);
                    methodMeta = getType(methodMetaType).getConstructor(MethodInfo.class)
                            .newInstance(methodInfo);
                }
            }
            return methodMeta;
        }
    }
}
