/*
 * Copyright 2017-2018 the original author or authors.
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
package org.glowroot.agent.plugin.play.interop;

import org.glowroot.agent.plugin.api.Logger;
import org.glowroot.agent.plugin.api.checker.Nullable;
import org.glowroot.agent.plugin.api.util.Base64;

import java.lang.reflect.Method;

class ScalaFunction1ClassBuilder {

    /*
    package org.glowroot.agent.plugin.play.interop;
    
    import org.glowroot.agent.plugin.play.interop.Function1;
    
    public class ScalaFunction1<T1, R> extends scala.runtime.AbstractFunction1<T1, R> {
    
        private final Function1<T1, R> f;
    
        public ScalaFunction1(Function1<T1, R> f) {
            this.f = f;
        }
    
        public R apply(T1 v1) {
            return f.apply(v1);
        }
    }
    
    compiled from glowroot-agent-play-plugin module root directory using
    curl http://search.maven.org/remotecontent?filepath=org/scala-lang/scala-library/2.11.11/scala-library-2.11.11.jar > scala-library.jar
    javac -classpath target/classes:scala-library.jar -g:none ScalaFunction1.java
    (make sure to use Java 6)
    base64 -w 84 ScalaFunction1.class | sed 's/^/+ "/' | sed 's/$/"/'
    */
    private static final String SCALA_FUNCTION1_CLASS_BYTES = ""
            + "yv66vgAAADIAGgoABQASCQAEABMLABQAFQcAFgcAFwEAAWYBADJMb3JnL2dsb3dyb290L2FnZW50L3BsdWdp"
            + "bi9wbGF5L2ludGVyb3AvRnVuY3Rpb24xOwEACVNpZ25hdHVyZQEAO0xvcmcvZ2xvd3Jvb3QvYWdlbnQvcGx1"
            + "Z2luL3BsYXkvaW50ZXJvcC9GdW5jdGlvbjE8VFQxO1RSOz47AQAGPGluaXQ+AQA1KExvcmcvZ2xvd3Jvb3Qv"
            + "YWdlbnQvcGx1Z2luL3BsYXkvaW50ZXJvcC9GdW5jdGlvbjE7KVYBAARDb2RlAQA+KExvcmcvZ2xvd3Jvb3Qv"
            + "YWdlbnQvcGx1Z2luL3BsYXkvaW50ZXJvcC9GdW5jdGlvbjE8VFQxO1RSOz47KVYBAAVhcHBseQEAJihMamF2"
            + "YS9sYW5nL09iamVjdDspTGphdmEvbGFuZy9PYmplY3Q7AQAJKFRUMTspVFI7AQBVPFQxOkxqYXZhL2xhbmcv"
            + "T2JqZWN0O1I6TGphdmEvbGFuZy9PYmplY3Q7PkxzY2FsYS9ydW50aW1lL0Fic3RyYWN0RnVuY3Rpb24xPFRU"
            + "MTtUUjs+OwwACgAYDAAGAAcHABkMAA4ADwEANW9yZy9nbG93cm9vdC9hZ2VudC9wbHVnaW4vcGxheS9pbnRl"
            + "cm9wL1NjYWxhRnVuY3Rpb24xAQAfc2NhbGEvcnVudGltZS9BYnN0cmFjdEZ1bmN0aW9uMQEAAygpVgEAMG9y"
            + "Zy9nbG93cm9vdC9hZ2VudC9wbHVnaW4vcGxheS9pbnRlcm9wL0Z1bmN0aW9uMQAhAAQABQAAAAEAEgAGAAcA"
            + "AQAIAAAAAgAJAAIAAQAKAAsAAgAMAAAAFgACAAIAAAAKKrcAASortQACsQAAAAAACAAAAAIADQABAA4ADwAC"
            + "AAwAAAAXAAIAAgAAAAsqtAACK7kAAwIAsAAAAAAACAAAAAIAEAABAAgAAAACABE=";

    private static final String SCALA_FUNCTION1_CLASS_NAME =
            ScalaFunction1ClassBuilder.class.getPackage().getName()
                    + ".ScalaFunction1";

    private static final Logger logger = Logger.getLogger(Converter.class);

    private static final Object globalLock = new Object();

    private ScalaFunction1ClassBuilder() {}

    static @Nullable Class<?> create(ClassLoader loader) {
        try {
            synchronized (globalLock) {
                try {
                    return Class.forName(SCALA_FUNCTION1_CLASS_NAME, false, loader);
                } catch (ClassNotFoundException e) {
                    byte[] bytes = Base64.decode(SCALA_FUNCTION1_CLASS_BYTES);
                    return defineClass(SCALA_FUNCTION1_CLASS_NAME, bytes, loader);
                }
            }
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            return null;
        }
    }

    private static Class<?> defineClass(String name, byte[] bytes, ClassLoader loader)
            throws Exception {
        Method defineClassMethod = ClassLoader.class.getDeclaredMethod("defineClass", String.class,
                byte[].class, int.class, int.class);
        defineClassMethod.setAccessible(true);
        Class<?> definedClass =
                (Class<?>) defineClassMethod.invoke(loader, name, bytes, 0, bytes.length);
        // FIXME checkNotNull(definedClass);
        return definedClass;
    }
}
