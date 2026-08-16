package org.spongepowered.include.com.google.common.base;

import org.spongepowered.include.com.google.errorprone.annotations.CanIgnoreReturnValue;

@FunctionalInterface
public interface Supplier<T> extends java.util.function.Supplier<T> {
   @CanIgnoreReturnValue
   T get();
}
