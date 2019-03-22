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

import java.util.List;

import com.google.common.collect.Lists;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.glowroot.xyzzy.engine.weaving.targets.Misc;
import org.glowroot.xyzzy.instrumentation.api.ClassInfo;
import org.glowroot.xyzzy.instrumentation.api.MethodInfo;
import org.glowroot.xyzzy.instrumentation.api.ParameterHolder;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindClassMeta;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindMethodMeta;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindMethodName;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindOptionalReturn;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindParameter;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindParameterArray;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindReceiver;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindReturn;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindThrowable;
import org.glowroot.xyzzy.instrumentation.api.weaving.BindTraveler;
import org.glowroot.xyzzy.instrumentation.api.weaving.IsEnabled;
import org.glowroot.xyzzy.instrumentation.api.weaving.MethodModifier;
import org.glowroot.xyzzy.instrumentation.api.weaving.Mixin;
import org.glowroot.xyzzy.instrumentation.api.weaving.MixinInit;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnAfter;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnBefore;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnReturn;
import org.glowroot.xyzzy.instrumentation.api.weaving.OnThrow;
import org.glowroot.xyzzy.instrumentation.api.weaving.OptionalReturn;
import org.glowroot.xyzzy.instrumentation.api.weaving.Pointcut;
import org.glowroot.xyzzy.instrumentation.api.weaving.Shim;

public class SomeInstrumentation {

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc"
            + "|org.glowroot.xyzzy.engine.weaving.targets.DefaultMethodMisc"
            + "|org.glowroot.xyzzy.engine.weaving.targets.DefaultMethodMisc2",
            methodName = "execute1|execute2", methodParameterTypes = {}, timerName = "xyz")
    public static class BasicAdvice {
        @IsEnabled
        public static boolean isEnabled() {
            SomeInstrumentationThreadLocals.enabledCount.increment();
            return SomeInstrumentationThreadLocals.enabled.get();
        }
        @OnBefore
        public static void onBefore() {
            SomeInstrumentationThreadLocals.onBeforeCount.increment();
        }
        @OnReturn
        public static void onReturn() {
            SomeInstrumentationThreadLocals.onReturnCount.increment();
        }
        @OnThrow
        public static void onThrow() {
            SomeInstrumentationThreadLocals.onThrowCount.increment();
        }
        @OnAfter
        public static void onAfter() {
            SomeInstrumentationThreadLocals.onAfterCount.increment();
        }
        public static void enable() {
            SomeInstrumentationThreadLocals.enabled.set(true);
        }
        public static void disable() {
            SomeInstrumentationThreadLocals.enabled.set(false);
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.SuperBasicMisc",
            methodName = "superBasic", methodParameterTypes = {}, timerName = "superbasic")
    public static class SuperBasicAdvice {
        @IsEnabled
        public static boolean isEnabled() {
            SomeInstrumentationThreadLocals.enabledCount.increment();
            return SomeInstrumentationThreadLocals.enabled.get();
        }
        @OnBefore
        public static void onBefore() {
            SomeInstrumentationThreadLocals.onBeforeCount.increment();
        }
        @OnReturn
        public static void onReturn() {
            SomeInstrumentationThreadLocals.onReturnCount.increment();
        }
        @OnThrow
        public static void onThrow() {
            SomeInstrumentationThreadLocals.onThrowCount.increment();
        }
        @OnAfter
        public static void onAfter() {
            SomeInstrumentationThreadLocals.onAfterCount.increment();
        }
    }

    @Pointcut(className = "java.lang.Throwable", methodName = "toString", methodParameterTypes = {},
            timerName = "throwable to string")
    public static class ThrowableToStringAdvice {
        @IsEnabled
        public static boolean isEnabled() {
            SomeInstrumentationThreadLocals.enabledCount.increment();
            return SomeInstrumentationThreadLocals.enabled.get();
        }
        @OnBefore
        public static void onBefore() {
            SomeInstrumentationThreadLocals.onBeforeCount.increment();
        }
        @OnReturn
        public static void onReturn() {
            SomeInstrumentationThreadLocals.onReturnCount.increment();
        }
        @OnThrow
        public static void onThrow() {
            SomeInstrumentationThreadLocals.onThrowCount.increment();
        }
        @OnAfter
        public static void onAfter() {
            SomeInstrumentationThreadLocals.onAfterCount.increment();
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.GenericMisc", methodName = "*",
            methodParameterTypes = {"java.lang.Object|java.lang.Class"})
    public static class GenericMiscAdvice {
        @IsEnabled
        public static boolean isEnabled() {
            SomeInstrumentationThreadLocals.enabledCount.increment();
            return SomeInstrumentationThreadLocals.enabled.get();
        }
        @OnBefore
        public static void onBefore() {
            SomeInstrumentationThreadLocals.onBeforeCount.increment();
        }
        @OnReturn
        public static void onReturn() {
            SomeInstrumentationThreadLocals.onReturnCount.increment();
        }
        @OnThrow
        public static void onThrow() {
            SomeInstrumentationThreadLocals.onThrowCount.increment();
        }
        @OnAfter
        public static void onAfter() {
            SomeInstrumentationThreadLocals.onAfterCount.increment();
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.BasicMisc", methodName = "<init>",
            methodParameterTypes = {})
    public static class BasicMiscConstructorAdvice {
        @IsEnabled
        public static boolean isEnabled() {
            SomeInstrumentationThreadLocals.enabledCount.increment();
            return SomeInstrumentationThreadLocals.enabled.get();
        }
        @OnBefore
        public static void onBefore() {
            SomeInstrumentationThreadLocals.onBeforeCount.increment();
        }
        @OnReturn
        public static void onReturn() {
            SomeInstrumentationThreadLocals.onReturnCount.increment();
        }
        @OnThrow
        public static void onThrow() {
            SomeInstrumentationThreadLocals.onThrowCount.increment();
        }
        @OnAfter
        public static void onAfter() {
            SomeInstrumentationThreadLocals.onAfterCount.increment();
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.BasicMisc", methodName = "<init>",
            methodParameterTypes = {".."})
    public static class BasicMiscAllConstructorAdvice {
        @IsEnabled
        public static boolean isEnabled() {
            SomeInstrumentationThreadLocals.orderedEvents.get().add("isEnabled");
            SomeInstrumentationThreadLocals.enabledCount.increment();
            return SomeInstrumentationThreadLocals.enabled.get();
        }
        @OnBefore
        public static void onBefore() {
            SomeInstrumentationThreadLocals.orderedEvents.get().add("onBefore");
            SomeInstrumentationThreadLocals.onBeforeCount.increment();
        }
        @OnReturn
        public static void onReturn() {
            SomeInstrumentationThreadLocals.orderedEvents.get().add("onReturn");
            SomeInstrumentationThreadLocals.onReturnCount.increment();
        }
        @OnThrow
        public static void onThrow() {
            SomeInstrumentationThreadLocals.orderedEvents.get().add("onThrow");
            SomeInstrumentationThreadLocals.onThrowCount.increment();
        }
        @OnAfter
        public static void onAfter() {
            SomeInstrumentationThreadLocals.orderedEvents.get().add("onAfter");
            SomeInstrumentationThreadLocals.onAfterCount.increment();
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.BasicMisc",
            methodName = "withInnerArg",
            methodParameterTypes = {"org.glowroot.xyzzy.engine.weaving.targets.BasicMisc$Inner"})
    public static class BasicWithInnerClassArgAdvice {
        @IsEnabled
        public static boolean isEnabled() {
            SomeInstrumentationThreadLocals.enabledCount.increment();
            return SomeInstrumentationThreadLocals.enabled.get();
        }
        @OnBefore
        public static void onBefore() {
            SomeInstrumentationThreadLocals.onBeforeCount.increment();
        }
        @OnReturn
        public static void onReturn() {
            SomeInstrumentationThreadLocals.onReturnCount.increment();
        }
        @OnThrow
        public static void onThrow() {
            SomeInstrumentationThreadLocals.onThrowCount.increment();
        }
        @OnAfter
        public static void onAfter() {
            SomeInstrumentationThreadLocals.onAfterCount.increment();
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.BasicMisc$InnerMisc",
            methodName = "execute1", methodParameterTypes = {})
    public static class BasicWithInnerClassAdvice {
        @IsEnabled
        public static boolean isEnabled() {
            SomeInstrumentationThreadLocals.enabledCount.increment();
            return SomeInstrumentationThreadLocals.enabled.get();
        }
        @OnBefore
        public static void onBefore() {
            SomeInstrumentationThreadLocals.onBeforeCount.increment();
        }
        @OnReturn
        public static void onReturn() {
            SomeInstrumentationThreadLocals.onReturnCount.increment();
        }
        @OnThrow
        public static void onThrow() {
            SomeInstrumentationThreadLocals.onThrowCount.increment();
        }
        @OnAfter
        public static void onAfter() {
            SomeInstrumentationThreadLocals.onAfterCount.increment();
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc", methodName = "execute1",
            methodParameterTypes = {})
    public static class BindReceiverAdvice {
        @IsEnabled
        public static boolean isEnabled(@BindReceiver Misc receiver) {
            SomeInstrumentationThreadLocals.isEnabledReceiver.set(receiver);
            return true;
        }
        @OnBefore
        public static void onBefore(@BindReceiver Misc receiver) {
            SomeInstrumentationThreadLocals.onBeforeReceiver.set(receiver);
        }
        @OnReturn
        public static void onReturn(@BindReceiver Misc receiver) {
            SomeInstrumentationThreadLocals.onReturnReceiver.set(receiver);
        }
        @OnThrow
        public static void onThrow(@BindReceiver Misc receiver) {
            SomeInstrumentationThreadLocals.onThrowReceiver.set(receiver);
        }
        @OnAfter
        public static void onAfter(@BindReceiver Misc receiver) {
            SomeInstrumentationThreadLocals.onAfterReceiver.set(receiver);
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc",
            methodName = "executeWithArgs", methodParameterTypes = {"java.lang.String", "int"})
    public static class BindParameterAdvice {
        @IsEnabled
        public static boolean isEnabled(@BindParameter String one, @BindParameter int two) {
            SomeInstrumentationThreadLocals.isEnabledParams.set(new Object[] {one, two});
            return true;
        }
        @OnBefore
        public static void onBefore(@BindParameter String one, @BindParameter int two) {
            SomeInstrumentationThreadLocals.onBeforeParams.set(new Object[] {one, two});
        }
        @OnReturn
        public static void onReturn(@BindParameter String one, @BindParameter int two) {
            SomeInstrumentationThreadLocals.onReturnParams.set(new Object[] {one, two});
        }
        @OnThrow
        public static void onThrow(@BindParameter String one, @BindParameter int two) {
            SomeInstrumentationThreadLocals.onThrowParams.set(new Object[] {one, two});
        }
        @OnAfter
        public static void onAfter(@BindParameter String one, @BindParameter int two) {
            SomeInstrumentationThreadLocals.onAfterParams.set(new Object[] {one, two});
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc",
            methodName = "executeWithArgs", methodParameterTypes = {"java.lang.String", "int"})
    public static class BindParameterArrayAdvice {
        @IsEnabled
        public static boolean isEnabled(@BindParameterArray Object[] args) {
            SomeInstrumentationThreadLocals.isEnabledParams.set(args);
            return true;
        }
        @OnBefore
        public static void onBefore(@BindParameterArray Object[] args) {
            SomeInstrumentationThreadLocals.onBeforeParams.set(args);
        }
        @OnReturn
        public static void onReturn(@BindParameterArray Object[] args) {
            SomeInstrumentationThreadLocals.onReturnParams.set(args);
        }
        @OnThrow
        public static void onThrow(@BindParameterArray Object[] args) {
            SomeInstrumentationThreadLocals.onThrowParams.set(args);
        }
        @OnAfter
        public static void onAfter(@BindParameterArray Object[] args) {
            SomeInstrumentationThreadLocals.onAfterParams.set(args);
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc", methodName = "execute1",
            methodParameterTypes = {})
    public static class BindTravelerAdvice {
        @OnBefore
        public static String onBefore() {
            return "a traveler";
        }
        @OnReturn
        public static void onReturn(@BindTraveler String traveler) {
            SomeInstrumentationThreadLocals.onReturnTraveler.set(traveler);
        }
        @OnThrow
        public static void onThrow(@BindTraveler String traveler) {
            SomeInstrumentationThreadLocals.onThrowTraveler.set(traveler);
        }
        @OnAfter
        public static void onAfter(@BindTraveler String traveler) {
            SomeInstrumentationThreadLocals.onAfterTraveler.set(traveler);
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc", methodName = "execute1",
            methodParameterTypes = {})
    public static class BindPrimitiveTravelerAdvice {
        @OnBefore
        public static int onBefore() {
            return 3;
        }
        @OnReturn
        public static void onReturn(@BindTraveler int traveler) {
            SomeInstrumentationThreadLocals.onReturnTraveler.set(traveler);
        }
        @OnThrow
        public static void onThrow(@BindTraveler int traveler) {
            SomeInstrumentationThreadLocals.onThrowTraveler.set(traveler);
        }
        @OnAfter
        public static void onAfter(@BindTraveler int traveler) {
            SomeInstrumentationThreadLocals.onAfterTraveler.set(traveler);
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc", methodName = "execute1",
            methodParameterTypes = {})
    public static class BindPrimitiveBooleanTravelerAdvice {
        @OnBefore
        public static boolean onBefore() {
            return true;
        }
        @OnReturn
        public static void onReturn(@BindTraveler boolean traveler) {
            SomeInstrumentationThreadLocals.onReturnTraveler.set(traveler);
        }
        @OnThrow
        public static void onThrow(@BindTraveler boolean traveler) {
            SomeInstrumentationThreadLocals.onThrowTraveler.set(traveler);
        }
        @OnAfter
        public static void onAfter(@BindTraveler boolean traveler) {
            SomeInstrumentationThreadLocals.onAfterTraveler.set(traveler);
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc", methodName = "execute1",
            methodParameterTypes = {})
    public static class BindPrimitiveTravelerBadAdvice {
        @OnBefore
        public static void onBefore() {}
        @OnReturn
        public static void onReturn(@BindTraveler int traveler) {
            SomeInstrumentationThreadLocals.onReturnTraveler.set(traveler);
        }
        @OnThrow
        public static void onThrow(@BindTraveler int traveler) {
            SomeInstrumentationThreadLocals.onThrowTraveler.set(traveler);
        }
        @OnAfter
        public static void onAfter(@BindTraveler int traveler) {
            SomeInstrumentationThreadLocals.onAfterTraveler.set(traveler);
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc", methodName = "execute1",
            methodParameterTypes = {})
    public static class BindPrimitiveBooleanTravelerBadAdvice {
        @OnBefore
        public static void onBefore() {}
        @OnReturn
        public static void onReturn(@BindTraveler boolean traveler) {
            SomeInstrumentationThreadLocals.onReturnTraveler.set(traveler);
        }
        @OnThrow
        public static void onThrow(@BindTraveler boolean traveler) {
            SomeInstrumentationThreadLocals.onThrowTraveler.set(traveler);
        }
        @OnAfter
        public static void onAfter(@BindTraveler boolean traveler) {
            SomeInstrumentationThreadLocals.onAfterTraveler.set(traveler);
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc", methodName = "execute1",
            methodParameterTypes = {})
    public static class BindClassMetaAdvice {
        @IsEnabled
        public static boolean isEnabled(@BindClassMeta TestClassMeta meta) {
            SomeInstrumentationThreadLocals.isEnabledClassMeta.set(meta);
            return true;
        }
        @OnBefore
        public static void onBefore(@BindClassMeta TestClassMeta meta) {
            SomeInstrumentationThreadLocals.onBeforeClassMeta.set(meta);
        }
        @OnReturn
        public static void onReturn(@BindClassMeta TestClassMeta meta) {
            SomeInstrumentationThreadLocals.onReturnClassMeta.set(meta);
        }
        @OnThrow
        public static void onThrow(@BindClassMeta TestClassMeta meta) {
            SomeInstrumentationThreadLocals.onThrowClassMeta.set(meta);
        }
        @OnAfter
        public static void onAfter(@BindClassMeta TestClassMeta meta) {
            SomeInstrumentationThreadLocals.onAfterClassMeta.set(meta);
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc",
            methodName = "executeWithArgs", methodParameterTypes = {".."})
    public static class BindMethodMetaAdvice {
        @IsEnabled
        public static boolean isEnabled(@BindMethodMeta TestMethodMeta meta) {
            SomeInstrumentationThreadLocals.isEnabledMethodMeta.set(meta);
            return true;
        }
        @OnBefore
        public static void onBefore(@BindMethodMeta TestMethodMeta meta) {
            SomeInstrumentationThreadLocals.onBeforeMethodMeta.set(meta);
        }
        @OnReturn
        public static void onReturn(@BindMethodMeta TestMethodMeta meta) {
            SomeInstrumentationThreadLocals.onReturnMethodMeta.set(meta);
        }
        @OnThrow
        public static void onThrow(@BindMethodMeta TestMethodMeta meta) {
            SomeInstrumentationThreadLocals.onThrowMethodMeta.set(meta);
        }
        @OnAfter
        public static void onAfter(@BindMethodMeta TestMethodMeta meta) {
            SomeInstrumentationThreadLocals.onAfterMethodMeta.set(meta);
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.other.ArrayMisc",
            methodName = "executeArray", methodParameterTypes = {".."})
    public static class BindMethodMetaArrayAdvice {
        @IsEnabled
        public static boolean isEnabled(@BindMethodMeta TestMethodMeta meta) {
            SomeInstrumentationThreadLocals.isEnabledMethodMeta.set(meta);
            return true;
        }
        @OnBefore
        public static void onBefore(@BindMethodMeta TestMethodMeta meta) {
            SomeInstrumentationThreadLocals.onBeforeMethodMeta.set(meta);
        }
        @OnReturn
        public static void onReturn(@BindMethodMeta TestMethodMeta meta) {
            SomeInstrumentationThreadLocals.onReturnMethodMeta.set(meta);
        }
        @OnThrow
        public static void onThrow(@BindMethodMeta TestMethodMeta meta) {
            SomeInstrumentationThreadLocals.onThrowMethodMeta.set(meta);
        }
        @OnAfter
        public static void onAfter(@BindMethodMeta TestMethodMeta meta) {
            SomeInstrumentationThreadLocals.onAfterMethodMeta.set(meta);
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.other.ArrayMisc",
            methodName = "executeWithArrayReturn", methodParameterTypes = {".."})
    public static class BindMethodMetaReturnArrayAdvice {
        @IsEnabled
        public static boolean isEnabled(@BindMethodMeta TestMethodMeta meta) {
            SomeInstrumentationThreadLocals.isEnabledMethodMeta.set(meta);
            return true;
        }
        @OnBefore
        public static void onBefore(@BindMethodMeta TestMethodMeta meta) {
            SomeInstrumentationThreadLocals.onBeforeMethodMeta.set(meta);
        }
        @OnReturn
        public static void onReturn(@BindMethodMeta TestMethodMeta meta) {
            SomeInstrumentationThreadLocals.onReturnMethodMeta.set(meta);
        }
        @OnThrow
        public static void onThrow(@BindMethodMeta TestMethodMeta meta) {
            SomeInstrumentationThreadLocals.onThrowMethodMeta.set(meta);
        }
        @OnAfter
        public static void onAfter(@BindMethodMeta TestMethodMeta meta) {
            SomeInstrumentationThreadLocals.onAfterMethodMeta.set(meta);
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc",
            methodName = "executeWithReturn", methodParameterTypes = {})
    public static class BindReturnAdvice {
        @OnReturn
        public static void onReturn(@BindReturn String value) {
            SomeInstrumentationThreadLocals.returnValue.set(value);
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.PrimitiveMisc",
            methodName = "executeWithIntReturn", methodParameterTypes = {})
    public static class BindPrimitiveReturnAdvice {
        @OnReturn
        public static void onReturn(@BindReturn int value) {
            SomeInstrumentationThreadLocals.returnValue.set(value);
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.PrimitiveMisc",
            methodName = "executeWithIntReturn", methodParameterTypes = {})
    public static class BindAutoboxedReturnAdvice {
        @OnReturn
        public static void onReturn(@BindReturn Object value) {
            SomeInstrumentationThreadLocals.returnValue.set(value);
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc",
            methodName = "executeWithReturn", methodParameterTypes = {})
    public static class BindOptionalReturnAdvice {
        @OnReturn
        public static void onReturn(@BindOptionalReturn OptionalReturn optionalReturn) {
            SomeInstrumentationThreadLocals.optionalReturnValue.set(optionalReturn);
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc", methodName = "execute1",
            methodParameterTypes = {})
    public static class BindOptionalVoidReturnAdvice {
        @OnReturn
        public static void onReturn(@BindOptionalReturn OptionalReturn optionalReturn) {
            SomeInstrumentationThreadLocals.optionalReturnValue.set(optionalReturn);
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.PrimitiveMisc",
            methodName = "executeWithIntReturn", methodParameterTypes = {})
    public static class BindOptionalPrimitiveReturnAdvice {
        @OnReturn
        public static void onReturn(@BindOptionalReturn OptionalReturn optionalReturn) {
            SomeInstrumentationThreadLocals.optionalReturnValue.set(optionalReturn);
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc", methodName = "execute1",
            methodParameterTypes = {})
    public static class BindThrowableAdvice {
        @OnThrow
        public static void onThrow(@BindThrowable Throwable t) {
            SomeInstrumentationThreadLocals.onThrowCount.increment();
            SomeInstrumentationThreadLocals.throwable.set(t);
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc", methodName = "execute1",
            methodParameterTypes = {}, order = 1)
    public static class ThrowInOnBeforeAdvice {
        @IsEnabled
        public static boolean isEnabled() {
            SomeInstrumentationThreadLocals.enabledCount.increment();
            return true;
        }
        @OnBefore
        public static void onBefore() {
            throw new RuntimeException("Abxy");
        }
        @OnReturn
        public static void onReturn() {
            SomeInstrumentationThreadLocals.onReturnCount.increment();
        }
        @OnThrow
        public static void onThrow() {
            SomeInstrumentationThreadLocals.onThrowCount.increment();
        }
        @OnAfter
        public static void onAfter() {
            SomeInstrumentationThreadLocals.onAfterCount.increment();
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc", methodName = "execute1",
            methodParameterTypes = {}, order = 1000)
    public static class BasicHighOrderAdvice {
        @IsEnabled
        public static boolean isEnabled() {
            SomeInstrumentationThreadLocals.enabledCount.increment();
            return true;
        }
        @OnBefore
        public static void onBefore() {
            SomeInstrumentationThreadLocals.onBeforeCount.increment();
        }
        @OnReturn
        public static void onReturn() {
            SomeInstrumentationThreadLocals.onReturnCount.increment();
        }
        @OnThrow
        public static void onThrow() {
            SomeInstrumentationThreadLocals.onThrowCount.increment();
        }
        @OnAfter
        public static void onAfter() {
            SomeInstrumentationThreadLocals.onAfterCount.increment();
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc", methodName = "execute1",
            methodParameterTypes = {}, timerName = "efg")
    public static class BindMethodNameAdvice {
        @IsEnabled
        public static boolean isEnabled(@BindMethodName String methodName) {
            SomeInstrumentationThreadLocals.isEnabledMethodName.set(methodName);
            return true;
        }
        @OnBefore
        public static void onBefore(@BindMethodName String methodName) {
            SomeInstrumentationThreadLocals.onBeforeMethodName.set(methodName);
        }
        @OnReturn
        public static void onReturn(@BindMethodName String methodName) {
            SomeInstrumentationThreadLocals.onReturnMethodName.set(methodName);
        }
        @OnThrow
        public static void onThrow(@BindMethodName String methodName) {
            SomeInstrumentationThreadLocals.onThrowMethodName.set(methodName);
        }
        @OnAfter
        public static void onAfter(@BindMethodName String methodName) {
            SomeInstrumentationThreadLocals.onAfterMethodName.set(methodName);
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc",
            methodName = "executeWithReturn", methodParameterTypes = {})
    public static class ChangeReturnAdvice {
        @IsEnabled
        public static boolean isEnabled() {
            return true;
        }
        @OnReturn
        public static String onReturn(@BindReturn String value, @BindMethodName String methodName) {
            return "modified " + value + ":" + methodName;
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc",
            methodName = "executeWithArgs", methodParameterTypes = {".."})
    public static class MethodParametersDotDotAdvice1 {
        @OnBefore
        public static void onBefore() {
            SomeInstrumentationThreadLocals.onBeforeCount.increment();
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc",
            methodName = "executeWithArgs", methodParameterTypes = {"..", ".."})
    public static class MethodParametersBadDotDotAdvice1 {
        @OnBefore
        public static void onBefore() {
            SomeInstrumentationThreadLocals.onBeforeCount.increment();
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc",
            methodName = "executeWithArgs", methodParameterTypes = {"java.lang.String", ".."})
    public static class MethodParametersDotDotAdvice2 {
        @OnBefore
        public static void onBefore() {
            SomeInstrumentationThreadLocals.onBeforeCount.increment();
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc",
            methodName = "executeWithArgs",
            methodParameterTypes = {"java.lang.String", "int", ".."})
    public static class MethodParametersDotDotAdvice3 {
        @OnBefore
        public static void onBefore() {
            SomeInstrumentationThreadLocals.onBeforeCount.increment();
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc",
            subTypeRestriction = "org.glowroot.xyzzy.engine.weaving.targets.BasicMisc",
            methodName = "execute1", methodParameterTypes = {})
    public static class SubTypeRestrictionAdvice {
        @IsEnabled
        public static boolean isEnabled() {
            SomeInstrumentationThreadLocals.enabledCount.increment();
            return SomeInstrumentationThreadLocals.enabled.get();
        }
        @OnBefore
        public static void onBefore() {
            SomeInstrumentationThreadLocals.onBeforeCount.increment();
        }
        @OnReturn
        public static void onReturn() {
            SomeInstrumentationThreadLocals.onReturnCount.increment();
        }
        @OnThrow
        public static void onThrow() {
            SomeInstrumentationThreadLocals.onThrowCount.increment();
        }
        @OnAfter
        public static void onAfter() {
            SomeInstrumentationThreadLocals.onAfterCount.increment();
        }
        public static void enable() {
            SomeInstrumentationThreadLocals.enabled.set(true);
        }
        public static void disable() {
            SomeInstrumentationThreadLocals.enabled.set(false);
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.SuperBasicMisc",
            subTypeRestriction = "org.glowroot.xyzzy.engine.weaving.targets.BasicMisc",
            methodName = "callSuperBasic", methodParameterTypes = {})
    public static class SubTypeRestrictionWhereMethodNotReImplementedInSubClassAdvice {
        @IsEnabled
        public static boolean isEnabled() {
            SomeInstrumentationThreadLocals.enabledCount.increment();
            return SomeInstrumentationThreadLocals.enabled.get();
        }
        @OnBefore
        public static void onBefore() {
            SomeInstrumentationThreadLocals.onBeforeCount.increment();
        }
        @OnReturn
        public static void onReturn() {
            SomeInstrumentationThreadLocals.onReturnCount.increment();
        }
        @OnThrow
        public static void onThrow() {
            SomeInstrumentationThreadLocals.onThrowCount.increment();
        }
        @OnAfter
        public static void onAfter() {
            SomeInstrumentationThreadLocals.onAfterCount.increment();
        }
        public static void enable() {
            SomeInstrumentationThreadLocals.enabled.set(true);
        }
        public static void disable() {
            SomeInstrumentationThreadLocals.enabled.set(false);
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc",
            subTypeRestriction = "org.glowroot.xyzzy.engine.weaving.targets.SubBasicMisc",
            methodName = "execute1", methodParameterTypes = {})
    public static class SubTypeRestrictionWhereMethodNotReImplementedInSubSubClassAdvice {
        @IsEnabled
        public static boolean isEnabled() {
            SomeInstrumentationThreadLocals.enabledCount.increment();
            return SomeInstrumentationThreadLocals.enabled.get();
        }
        @OnBefore
        public static void onBefore() {
            SomeInstrumentationThreadLocals.onBeforeCount.increment();
        }
        @OnReturn
        public static void onReturn() {
            SomeInstrumentationThreadLocals.onReturnCount.increment();
        }
        @OnThrow
        public static void onThrow() {
            SomeInstrumentationThreadLocals.onThrowCount.increment();
        }
        @OnAfter
        public static void onAfter() {
            SomeInstrumentationThreadLocals.onAfterCount.increment();
        }
        public static void enable() {
            SomeInstrumentationThreadLocals.enabled.set(true);
        }
        public static void disable() {
            SomeInstrumentationThreadLocals.enabled.set(false);
        }
    }

    @Pointcut(classAnnotation = "org.glowroot.xyzzy.engine.weaving.SomeInstrumentation$SomeClass",
            methodAnnotation = "org.glowroot.xyzzy.engine.weaving.SomeInstrumentation$SomeMethod",
            methodParameterTypes = {".."}, timerName = "anno")
    public static class BasicAnnotationBasedAdvice {
        @IsEnabled
        public static boolean isEnabled() {
            SomeInstrumentationThreadLocals.enabledCount.increment();
            return SomeInstrumentationThreadLocals.enabled.get();
        }
        @OnBefore
        public static void onBefore() {
            SomeInstrumentationThreadLocals.onBeforeCount.increment();
        }
        @OnReturn
        public static void onReturn() {
            SomeInstrumentationThreadLocals.onReturnCount.increment();
        }
        @OnThrow
        public static void onThrow() {
            SomeInstrumentationThreadLocals.onThrowCount.increment();
        }
        @OnAfter
        public static void onAfter() {
            SomeInstrumentationThreadLocals.onAfterCount.increment();
        }
        public static void enable() {
            SomeInstrumentationThreadLocals.enabled.set(true);
        }
        public static void disable() {
            SomeInstrumentationThreadLocals.enabled.set(false);
        }
    }

    @Pointcut(className = "*",
            superTypeRestriction = "org.glowroot.xyzzy.engine.weaving.targets.SuperBasicMisc",
            methodAnnotation = "org.glowroot.xyzzy.engine.weaving.SomeInstrumentation$SomeMethod",
            methodParameterTypes = {".."}, timerName = "anno")
    public static class AnotherAnnotationBasedAdvice {
        @IsEnabled
        public static boolean isEnabled() {
            SomeInstrumentationThreadLocals.enabledCount.increment();
            return SomeInstrumentationThreadLocals.enabled.get();
        }
        @OnBefore
        public static void onBefore() {
            SomeInstrumentationThreadLocals.onBeforeCount.increment();
        }
        @OnReturn
        public static void onReturn() {
            SomeInstrumentationThreadLocals.onReturnCount.increment();
        }
        @OnThrow
        public static void onThrow() {
            SomeInstrumentationThreadLocals.onThrowCount.increment();
        }
        @OnAfter
        public static void onAfter() {
            SomeInstrumentationThreadLocals.onAfterCount.increment();
        }
        public static void enable() {
            SomeInstrumentationThreadLocals.enabled.set(true);
        }
        public static void disable() {
            SomeInstrumentationThreadLocals.enabled.set(false);
        }
    }

    @Pointcut(className = "*",
            superTypeRestriction = "org.glowroot.xyzzy.engine.weaving.targets.SuperBasicMiscButWrong",
            methodAnnotation = "org.glowroot.xyzzy.engine.weaving.SomeInstrumentation$SomeMethod",
            methodParameterTypes = {".."}, timerName = "anno")
    public static class AnotherAnnotationBasedAdviceButWrong {
        @IsEnabled
        public static boolean isEnabled() {
            SomeInstrumentationThreadLocals.enabledCount.increment();
            return SomeInstrumentationThreadLocals.enabled.get();
        }
        @OnBefore
        public static void onBefore() {
            SomeInstrumentationThreadLocals.onBeforeCount.increment();
        }
        @OnReturn
        public static void onReturn() {
            SomeInstrumentationThreadLocals.onReturnCount.increment();
        }
        @OnThrow
        public static void onThrow() {
            SomeInstrumentationThreadLocals.onThrowCount.increment();
        }
        @OnAfter
        public static void onAfter() {
            SomeInstrumentationThreadLocals.onAfterCount.increment();
        }
        public static void enable() {
            SomeInstrumentationThreadLocals.enabled.set(true);
        }
        public static void disable() {
            SomeInstrumentationThreadLocals.enabled.set(false);
        }
    }

    @Pointcut(className = "*.BasicMisc",
            superTypeRestriction = "org.glowroot.xyzzy.engine.weaving.targets.Misc",
            methodName = "execute1", methodParameterTypes = {})
    public static class SuperTypeRestrictionAdvice {
        @IsEnabled
        public static boolean isEnabled() {
            SomeInstrumentationThreadLocals.enabledCount.increment();
            return SomeInstrumentationThreadLocals.enabled.get();
        }
        @OnBefore
        public static void onBefore() {
            SomeInstrumentationThreadLocals.onBeforeCount.increment();
        }
        @OnReturn
        public static void onReturn() {
            SomeInstrumentationThreadLocals.onReturnCount.increment();
        }
        @OnThrow
        public static void onThrow() {
            SomeInstrumentationThreadLocals.onThrowCount.increment();
        }
        @OnAfter
        public static void onAfter() {
            SomeInstrumentationThreadLocals.onAfterCount.increment();
        }
        public static void enable() {
            SomeInstrumentationThreadLocals.enabled.set(true);
        }
        public static void disable() {
            SomeInstrumentationThreadLocals.enabled.set(false);
        }
    }

    @Pointcut(className = "*.BasicMisc",
            superTypeRestriction = "org.glowroot.xyzzy.engine.weaving.targets.SuperBasicMisc",
            methodName = "callSuperBasic", methodParameterTypes = {})
    public static class SuperTypeRestrictionWhereMethodNotReImplementedInSubClassAdvice {
        @IsEnabled
        public static boolean isEnabled() {
            SomeInstrumentationThreadLocals.enabledCount.increment();
            return SomeInstrumentationThreadLocals.enabled.get();
        }
        @OnBefore
        public static void onBefore() {
            SomeInstrumentationThreadLocals.onBeforeCount.increment();
        }
        @OnReturn
        public static void onReturn() {
            SomeInstrumentationThreadLocals.onReturnCount.increment();
        }
        @OnThrow
        public static void onThrow() {
            SomeInstrumentationThreadLocals.onThrowCount.increment();
        }
        @OnAfter
        public static void onAfter() {
            SomeInstrumentationThreadLocals.onAfterCount.increment();
        }
        public static void enable() {
            SomeInstrumentationThreadLocals.enabled.set(true);
        }
        public static void disable() {
            SomeInstrumentationThreadLocals.enabled.set(false);
        }
    }

    @Pointcut(className = "*SubMisc",
            superTypeRestriction = "org.glowroot.xyzzy.engine.weaving.targets.Misc",
            methodName = "sub", methodParameterTypes = {})
    public static class ComplexSuperTypeRestrictionAdvice {
        @IsEnabled
        public static boolean isEnabled() {
            SomeInstrumentationThreadLocals.enabledCount.increment();
            return SomeInstrumentationThreadLocals.enabled.get();
        }
        @OnBefore
        public static void onBefore() {
            SomeInstrumentationThreadLocals.onBeforeCount.increment();
        }
        @OnReturn
        public static void onReturn() {
            SomeInstrumentationThreadLocals.onReturnCount.increment();
        }
        @OnThrow
        public static void onThrow() {
            SomeInstrumentationThreadLocals.onThrowCount.increment();
        }
        @OnAfter
        public static void onAfter() {
            SomeInstrumentationThreadLocals.onAfterCount.increment();
        }
        public static void enable() {
            SomeInstrumentationThreadLocals.enabled.set(true);
        }
        public static void disable() {
            SomeInstrumentationThreadLocals.enabled.set(false);
        }
    }

    @Shim("org.glowroot.xyzzy.engine.weaving.targets.ShimmedMisc")
    public interface Shimmy {
        @Shim("java.lang.String getString()")
        Object shimmyGetString();
        @Shim("void setString(java.lang.String)")
        void shimmySetString(String string);
    }

    public interface HasString {
        String getString();
        void setString(String string);
    }

    @Mixin("org.glowroot.xyzzy.engine.weaving.targets.BasicMisc")
    public static class HasStringClassMixin implements HasString {
        private transient String string;
        @MixinInit
        private void initHasString() {
            if (string == null) {
                string = "a string";
            } else {
                string = "init called twice";
            }
        }
        @Override
        public String getString() {
            return string;
        }
        @Override
        public void setString(String string) {
            this.string = string;
        }
    }

    @Mixin("org.glowroot.xyzzy.engine.weaving.targets.Misc")
    public static class HasStringInterfaceMixin implements HasString {
        private transient String string;
        @MixinInit
        private void initHasString() {
            string = "a string";
        }
        @Override
        public String getString() {
            return string;
        }
        @Override
        public void setString(String string) {
            this.string = string;
        }
    }

    @Mixin({"org.glowroot.xyzzy.engine.weaving.targets.Misc",
            "org.glowroot.xyzzy.engine.weaving.targets.Misc2"})
    public static class HasStringMultipleMixin implements HasString {
        private transient String string;
        @MixinInit
        private void initHasString() {
            string = "a string";
        }
        @Override
        public String getString() {
            return string;
        }
        @Override
        public void setString(String string) {
            this.string = string;
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc", methodName = "execute*",
            methodParameterTypes = {".."}, timerName = "abc xyz")
    public static class InnerMethodAdvice extends BasicAdvice {}

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc", methodName = "execute*",
            methodParameterTypes = {".."})
    public static class MultipleMethodsAdvice extends BasicAdvice {}

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.StaticMisc",
            methodName = "executeStatic", methodParameterTypes = {})
    public static class StaticAdvice extends BasicAdvice {}

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc", methodName = "execute1",
            methodParameterTypes = {}, methodModifiers = MethodModifier.STATIC)
    public static class NonMatchingStaticAdvice extends BasicAdvice {}

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc", methodName = "execute1",
            methodParameterTypes = {},
            methodModifiers = {MethodModifier.PUBLIC, MethodModifier.NOT_STATIC})
    public static class MatchingPublicNonStaticAdvice extends BasicAdvice {}

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Mis*", methodName = "execute1",
            methodParameterTypes = {})
    public static class ClassNamePatternAdvice extends BasicAdvice {}

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc", methodName = "execute1",
            methodParameterTypes = {}, methodReturnType = "void")
    public static class MethodReturnVoidAdvice extends BasicAdvice {}

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc",
            methodName = "executeWithReturn", methodParameterTypes = {},
            methodReturnType = "java.lang.CharSequence")
    public static class MethodReturnCharSequenceAdvice extends BasicAdvice {}

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc",
            methodName = "executeWithReturn", methodParameterTypes = {},
            methodReturnType = "java.lang.String")
    public static class MethodReturnStringAdvice extends BasicAdvice {}

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc", methodName = "execute1",
            methodParameterTypes = {}, methodReturnType = "java.lang.String")
    public static class NonMatchingMethodReturnAdvice extends BasicAdvice {}

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc",
            methodName = "executeWithReturn", methodParameterTypes = {},
            methodReturnType = "java.lang.Number")
    public static class NonMatchingMethodReturnAdvice2 extends BasicAdvice {}

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc",
            methodName = "executeWithReturn", methodParameterTypes = {},
            methodReturnType = "java.lang.")
    public static class MethodReturnNarrowingAdvice extends BasicAdvice {}

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc", methodName = "*",
            methodParameterTypes = {".."}, timerName = "wild")
    public static class WildMethodAdvice extends BasicAdvice {}

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.PrimitiveMisc",
            methodName = "executePrimitive",
            methodParameterTypes = {"int", "double", "long", "byte[]"})
    public static class PrimitiveAdvice extends BasicAdvice {}

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.PrimitiveMisc",
            methodName = "executePrimitive", methodParameterTypes = {"int", "double", "*", ".."})
    public static class PrimitiveWithWildcardAdvice {
        @IsEnabled
        public static boolean isEnabled(@SuppressWarnings("unused") @BindParameter int x) {
            SomeInstrumentationThreadLocals.enabledCount.increment();
            return true;
        }
        @OnBefore
        public static void onBefore(@SuppressWarnings("unused") @BindParameter int x) {
            SomeInstrumentationThreadLocals.onBeforeCount.increment();
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.PrimitiveMisc",
            methodName = "executePrimitive", methodParameterTypes = {"int", "double", "*", ".."})
    public static class PrimitiveWithAutoboxAdvice {
        @IsEnabled
        public static boolean isEnabled(@SuppressWarnings("unused") @BindParameter Object x) {
            SomeInstrumentationThreadLocals.enabledCount.increment();
            return true;
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc",
            methodName = "executeWithArgs", methodParameterTypes = {"java.lang.String", "int"})
    public static class BrokenAdvice {
        @IsEnabled
        public static boolean isEnabled() {
            return true;
        }
        @OnBefore
        public static @Nullable Object onBefore() {
            return null;
        }
        @OnAfter
        public static void onAfter(@SuppressWarnings("unused") @BindTraveler Object traveler) {}
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc",
            methodName = "executeWithArgs", methodParameterTypes = {"java.lang.String", "int"})
    public static class VeryBadAdvice {
        @OnBefore
        public static Object onBefore() {
            SomeInstrumentationThreadLocals.onBeforeCount.increment();
            throw new IllegalStateException("Sorry");
        }
        @OnThrow
        public static void onThrow() {
            // should not get called
            SomeInstrumentationThreadLocals.onThrowCount.increment();
        }
        @OnAfter
        public static void onAfter() {
            // should not get called
            SomeInstrumentationThreadLocals.onAfterCount.increment();
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc",
            methodName = "executeWithArgs", methodParameterTypes = {"java.lang.String", "int"})
    public static class MoreVeryBadAdvice {
        @OnReturn
        public static void onReturn() {
            SomeInstrumentationThreadLocals.onReturnCount.increment();
            throw new IllegalStateException("Sorry");
        }
        @OnThrow
        public static void onThrow() {
            // should not get called
            SomeInstrumentationThreadLocals.onThrowCount.increment();
        }
        @OnAfter
        public static void onAfter() {
            // should not get called
            SomeInstrumentationThreadLocals.onAfterCount.increment();
        }
    }

    // same as MoreVeryBadAdvice, but testing weaving a method with a non-void return type
    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc",
            methodName = "executeWithReturn", methodParameterTypes = {})
    public static class MoreVeryBadAdvice2 {
        @OnReturn
        public static void onReturn() {
            SomeInstrumentationThreadLocals.onReturnCount.increment();
            throw new IllegalStateException("Sorry");
        }
        @OnThrow
        public static void onThrow() {
            // should not get called
            SomeInstrumentationThreadLocals.onThrowCount.increment();
        }
        @OnAfter
        public static void onAfter() {
            // should not get called
            SomeInstrumentationThreadLocals.onAfterCount.increment();
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc3", methodName = "identity",
            methodParameterTypes = {"org.glowroot.xyzzy.engine.weaving.targets.BasicMisc"})
    public static class CircularClassDependencyAdvice {
        @OnBefore
        public static void onBefore() {
            SomeInstrumentationThreadLocals.onBeforeCount.increment();
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc", methodName = "execute1",
            methodParameterTypes = {})
    public static class InterfaceAppearsTwiceInHierarchyAdvice {
        @OnBefore
        public static void onBefore() {
            SomeInstrumentationThreadLocals.onBeforeCount.increment();
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc",
            methodName = "executeWithArgs", methodParameterTypes = {".."})
    public static class FinalMethodAdvice {
        @OnBefore
        public static void onBefore() {
            SomeInstrumentationThreadLocals.onBeforeCount.increment();
        }
    }

    // test weaving against JSR bytecode that ends up being inlined via JSRInlinerAdapter
    @Pointcut(className = "org.apache.jackrabbit.core.persistence.pool.BundleDbPersistenceManager",
            methodName = "loadBundle",
            methodParameterTypes = {"org.apache.jackrabbit.core.id.NodeId"})
    public static class TestJSRMethodAdvice {}

    // test weaving against 1.7 bytecode with stack frames
    @Pointcut(className = "org.xnio.Buffers", methodName = "*", methodParameterTypes = {".."})
    public static class TestBytecodeWithStackFramesAdvice {}

    // test weaving against 1.7 bytecode with stack frames
    @Pointcut(className = "org.xnio.Buffers", methodName = "*", methodParameterTypes = {".."})
    public static class TestBytecodeWithStackFramesAdvice2 {
        @IsEnabled
        public static boolean isEnabled() {
            return true;
        }
    }

    // test weaving against 1.7 bytecode with stack frames
    @Pointcut(className = "org.xnio.Buffers", methodName = "*", methodParameterTypes = {".."})
    public static class TestBytecodeWithStackFramesAdvice3 {
        @OnBefore
        public static void onBefore() {}
    }

    // test weaving against 1.7 bytecode with stack frames
    @Pointcut(className = "org.xnio.Buffers", methodName = "*", methodParameterTypes = {".."})
    public static class TestBytecodeWithStackFramesAdvice4 {
        @OnReturn
        public static void onReturn() {}
    }

    // test weaving against 1.7 bytecode with stack frames
    @Pointcut(className = "org.xnio.Buffers", methodName = "*", methodParameterTypes = {".."})
    public static class TestBytecodeWithStackFramesAdvice5 {
        @OnThrow
        public static void onThrow() {}
    }

    // test weaving against 1.7 bytecode with stack frames
    @Pointcut(className = "org.xnio.Buffers", methodName = "*", methodParameterTypes = {".."})
    public static class TestBytecodeWithStackFramesAdvice6 {
        @OnAfter
        public static void onAfter() {}
    }

    @Pointcut(className = "TroublesomeBytecode", methodName = "*", methodParameterTypes = {".."})
    public static class TestTroublesomeBytecodeAdvice {
        @OnAfter
        public static void onAfter() {}
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.GenerateNotPerfectBytecode$Test",
            methodName = "test*", methodParameterTypes = {}, timerName = "xyz")
    public static class NotPerfectBytecodeAdvice {
        @IsEnabled
        public static boolean isEnabled() {
            SomeInstrumentationThreadLocals.enabledCount.increment();
            return true;
        }
        @OnBefore
        public static void onBefore() {
            SomeInstrumentationThreadLocals.onBeforeCount.increment();
        }
        @OnReturn
        public static void onReturn() {
            SomeInstrumentationThreadLocals.onReturnCount.increment();
        }
        @OnThrow
        public static void onThrow() {
            SomeInstrumentationThreadLocals.onThrowCount.increment();
        }
        @OnAfter
        public static void onAfter() {
            SomeInstrumentationThreadLocals.onAfterCount.increment();
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.GenerateMoreNotPerfectBytecode$Test"
            + "|org.glowroot.xyzzy.engine.weaving.GenerateStillMoreNotPerfectBytecode$Test",
            methodName = "execute", methodParameterTypes = {".."}, timerName = "xyz")
    public static class MoreNotPerfectBytecodeAdvice {
        @IsEnabled
        public static boolean isEnabled() {
            SomeInstrumentationThreadLocals.enabledCount.increment();
            return true;
        }
        @OnBefore
        public static void onBefore() {
            SomeInstrumentationThreadLocals.onBeforeCount.increment();
        }
        @OnReturn
        public static void onReturn() {
            SomeInstrumentationThreadLocals.onReturnCount.increment();
        }
        @OnThrow
        public static void onThrow() {
            SomeInstrumentationThreadLocals.onThrowCount.increment();
        }
        @OnAfter
        public static void onAfter() {
            SomeInstrumentationThreadLocals.onAfterCount.increment();
        }
    }

    @Pointcut(className = "HackedConstructorBytecode|MoreHackedConstructorBytecode",
            methodName = "<init>", methodParameterTypes = {}, timerName = "xyz")
    public static class HackedConstructorBytecodeAdvice {
        @IsEnabled
        public static boolean isEnabled() {
            SomeInstrumentationThreadLocals.enabledCount.increment();
            return true;
        }
        @OnBefore
        public static void onBefore() {
            SomeInstrumentationThreadLocals.onBeforeCount.increment();
        }
        @OnReturn
        public static void onReturn() {
            SomeInstrumentationThreadLocals.onReturnCount.increment();
        }
        @OnThrow
        public static void onThrow() {
            SomeInstrumentationThreadLocals.onThrowCount.increment();
        }
        @OnAfter
        public static void onAfter() {
            SomeInstrumentationThreadLocals.onAfterCount.increment();
        }
    }

    @Pointcut(className = "HackedConstructorBytecode", methodName = "<init>",
            methodParameterTypes = {}, nestingGroup = "xyz")
    public static class HackedConstructorBytecodeJumpingAdvice {
        @IsEnabled
        public static boolean isEnabled() {
            SomeInstrumentationThreadLocals.enabledCount.increment();
            return true;
        }
        @OnBefore
        public static void onBefore() {
            SomeInstrumentationThreadLocals.onBeforeCount.increment();
        }
        @OnReturn
        public static void onReturn() {
            SomeInstrumentationThreadLocals.onReturnCount.increment();
        }
        @OnThrow
        public static void onThrow() {
            SomeInstrumentationThreadLocals.onThrowCount.increment();
        }
        @OnAfter
        public static void onAfter() {
            SomeInstrumentationThreadLocals.onAfterCount.increment();
        }
    }

    @Pointcut(className = "java.lang.Iterable", methodName = "iterator|spliterator",
            methodParameterTypes = {".."})
    public static class IterableAdvice {
        @OnBefore
        public static void onBefore() {
            SomeInstrumentationThreadLocals.onBeforeCount.increment();
        }
        @OnReturn
        public static void onReturn() {
            SomeInstrumentationThreadLocals.onReturnCount.increment();
        }
        @OnThrow
        public static void onThrow() {
            SomeInstrumentationThreadLocals.onThrowCount.increment();
        }
        @OnAfter
        public static void onAfter() {
            SomeInstrumentationThreadLocals.onAfterCount.increment();
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc",
            methodName = "executeWithArgs", methodParameterTypes = {"java.lang.String", "int"})
    public static class BindMutableParameterAdvice {
        @OnBefore
        public static void onBefore(@BindParameter ParameterHolder<String> holder,
                @BindParameter ParameterHolder<Integer> holder2) {
            holder.set(holder.get() + " and more");
            holder2.set(holder2.get() + 1);
        }
    }

    @Pointcut(className = "org.glowroot.xyzzy.engine.weaving.targets.Misc",
            methodName = "executeWithArgs", methodParameterTypes = {"java.lang.String", "int"},
            nestingGroup = "xyz")
    public static class BindMutableParameterWithMoreFramesAdvice {
        @OnBefore
        public static void onBefore(@BindParameter ParameterHolder<String> holder,
                @BindParameter ParameterHolder<Integer> holder2) {
            holder.set(holder.get() + " and more");
            holder2.set(holder2.get() + 1);
        }
    }

    public static class TestClassMeta {

        private final ClassInfo classInfo;

        public TestClassMeta(ClassInfo classInfo) {
            this.classInfo = classInfo;
        }

        public String getClazzName() {
            return classInfo.getName();
        }
    }

    public static class TestMethodMeta {

        private final MethodInfo methodInfo;

        public TestMethodMeta(MethodInfo methodInfo) {
            this.methodInfo = methodInfo;
        }

        public String getDeclaringClassName() {
            return methodInfo.getDeclaringClassName();
        }

        public String getReturnTypeName() {
            return methodInfo.getReturnType().getName();
        }

        public List<String> getParameterTypeNames() {
            List<String> parameterTypeNames = Lists.newArrayList();
            for (Class<?> parameterType : methodInfo.getParameterTypes()) {
                parameterTypeNames.add(parameterType.getName());
            }
            return parameterTypeNames;
        }
    }

    public @interface SomeClass {}

    public @interface SomeMethod {}
}
