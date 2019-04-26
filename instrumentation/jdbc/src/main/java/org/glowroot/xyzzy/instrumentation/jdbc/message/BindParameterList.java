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
package org.glowroot.xyzzy.instrumentation.jdbc.message;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.glowroot.xyzzy.instrumentation.api.checker.Nullable;

// micro-optimized list for bind parameters
public class BindParameterList implements Iterable</*@Nullable*/ Object> {

    private @Nullable Object[] parameters;
    private int size;

    public static BindParameterList copyOf(BindParameterList bindParameterList) {
        return new BindParameterList(bindParameterList.parameters, bindParameterList.size);
    }

    public BindParameterList(int capacity) {
        parameters = new Object[capacity];
    }

    private BindParameterList(@Nullable Object[] parameters, int size) {
        if (parameters.length == size) {
            this.parameters = parameters.clone();
        } else {
            // clone is faster even in this case, but worth the one time hit (for cached statements
            // where this will be called over and over) to resize the array and use less memory
            this.parameters = new Object[size];
            System.arraycopy(parameters, 0, this.parameters, 0, size);
        }
        this.size = size;
    }

    public void set(int i, @Nullable Object parameter) {
        int capacity = parameters.length;
        if (i >= capacity) {
            // using same capacity increase formula as ArrayList
            capacity = capacity + (capacity >> 1);
            if (i >= capacity) {
                capacity = i + 1;
            }
            parameters = Arrays.copyOf(parameters, capacity);
        }
        parameters[i] = parameter;
        int newSize = i + 1;
        if (newSize > size) {
            size = newSize;
        }
    }

    public int size() {
        return size;
    }

    boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        Arrays.fill(parameters, null);
        size = 0;
    }

    @Override
    public Iterator</*@Nullable*/ Object> iterator() {
        return new ParameterIterator();
    }

    private class ParameterIterator implements Iterator</*@Nullable*/ Object> {

        private int i;

        @Override
        public boolean hasNext() {
            return i < size;
        }

        @Override
        public @Nullable Object next() {
            if (i < size) {
                return parameters[i++];
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
