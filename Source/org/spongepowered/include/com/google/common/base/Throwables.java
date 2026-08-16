package org.spongepowered.include.com.google.common.base;

import java.lang.reflect.Method;
import javax.annotation.Nullable;

public final class Throwables {
   @Nullable
   private static final Object jla = getJLA();
   @Nullable
   private static final Method getStackTraceElementMethod;
   @Nullable
   private static final Method getStackTraceDepthMethod;

   public static <X extends Throwable> void throwIfInstanceOf(Throwable throwable, Class<X> declaredType) throws X {
      Preconditions.checkNotNull(throwable);
      if (declaredType.isInstance(throwable)) {
         throw (Throwable)declaredType.cast(throwable);
      }
   }

   /** @deprecated */
   @Deprecated
   public static <X extends Throwable> void propagateIfInstanceOf(@Nullable Throwable throwable, Class<X> declaredType) throws X {
      if (throwable != null) {
         throwIfInstanceOf(throwable, declaredType);
      }

   }

   public static void throwIfUnchecked(Throwable throwable) {
      Preconditions.checkNotNull(throwable);
      if (throwable instanceof RuntimeException) {
         throw (RuntimeException)throwable;
      } else if (throwable instanceof Error) {
         throw (Error)throwable;
      }
   }

   /** @deprecated */
   @Deprecated
   public static void propagateIfPossible(@Nullable Throwable throwable) {
      if (throwable != null) {
         throwIfUnchecked(throwable);
      }

   }

   public static <X extends Throwable> void propagateIfPossible(@Nullable Throwable throwable, Class<X> declaredType) throws X {
      propagateIfInstanceOf(throwable, declaredType);
      propagateIfPossible(throwable);
   }

   @Nullable
   private static Object getJLA() {
      try {
         Class<?> sharedSecrets = Class.forName("sun.misc.SharedSecrets", false, (ClassLoader)null);
         Method langAccess = sharedSecrets.getMethod("getJavaLangAccess");
         return langAccess.invoke((Object)null);
      } catch (ThreadDeath death) {
         throw death;
      } catch (Throwable var3) {
         return null;
      }
   }

   @Nullable
   private static Method getGetMethod() {
      return getJlaMethod("getStackTraceElement", Throwable.class, Integer.TYPE);
   }

   @Nullable
   private static Method getSizeMethod() {
      return getJlaMethod("getStackTraceDepth", Throwable.class);
   }

   @Nullable
   private static Method getJlaMethod(String name, Class<?>... parameterTypes) throws ThreadDeath {
      try {
         return Class.forName("sun.misc.JavaLangAccess", false, (ClassLoader)null).getMethod(name, parameterTypes);
      } catch (ThreadDeath death) {
         throw death;
      } catch (Throwable var4) {
         return null;
      }
   }

   static {
      getStackTraceElementMethod = jla == null ? null : getGetMethod();
      getStackTraceDepthMethod = jla == null ? null : getSizeMethod();
   }
}
