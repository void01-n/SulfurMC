package org.spongepowered.include.com.google.common.base;

public final class Functions {
   public static Function<Object, String> toStringFunction() {
      return Functions.ToStringFunction.INSTANCE;
   }

   private static enum ToStringFunction implements Function<Object, String> {
      INSTANCE;

      public String apply(Object o) {
         Preconditions.checkNotNull(o);
         return o.toString();
      }

      public String toString() {
         return "Functions.toStringFunction()";
      }
   }
}
