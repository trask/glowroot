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
package org.glowroot.xyzzy.instrumentation.api.weaving;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotates a method in a {@literal @}{@link Pointcut} class that should be run just after each
 * method (or constructor) execution picked out by the {@link Pointcut}, but only if the method
 * picked out by the {@link Pointcut} returns successfully (does not throw an {@code Exception}).
 * Only one method in a {@literal @}{@code Pointcut} class may be annotated with
 * 
 * {@literal @}{@code OnReturn}.
 * <p>
 * An {@literal @}{@code OnReturn} method can accept parameters annotated with any of the following:
 * {@literal @}{@link BindReceiver}, {@literal @}{@link BindParameter},
 * 
 * {@literal @}{@link BindParameterArray}, {@literal @}{@link BindMethodName},
 * 
 * {@literal @}{@link BindTraveler} or {@literal @}{@link BindReturn}.
 * 
 * {@literal @}{@link BindTraveler} can only be used if there is a corresponding
 * 
 * {@literal @}{@link OnBefore} method that returns a non-{@code void} type (the <em>traveler</em>).
 * {@literal @}{@link BindReturn} can only be used if each method picked out by the {@link Pointcut}
 * returns a non-{@code void} type. If {@literal @}{@link BindReturn} is used, it must be the first
 * parameter to the {@literal @}{@code OnReturn} method.
 * <p>
 * An {@literal @}{@code OnReturn} method may return {@code void} or a non-{@code void} type. If it
 * returns a non-{@code void} type, the value returned by the {@literal @}{@code OnReturn} method is
 * returned from the method execution picked out by the {@link Pointcut} instead of that method's
 * original return value. This can be used to wrap the original return value by passing the original
 * return value in to an {@literal @}{@code OnReturn} method (using
 * 
 * {@literal @}{@link BindReturn}) and then returning the wrapped value.
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface OnReturn {}
