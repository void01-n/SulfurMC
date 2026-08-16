package org.spongepowered.include.com.google.common.base;

import java.io.Serializable;

public abstract class Optional<T> implements Serializable {
   public static <T> Optional<T> absent() {
      return Absent.<T>withType();
   }

   Optional() {
   }

   public abstract T or(T var1);
}
