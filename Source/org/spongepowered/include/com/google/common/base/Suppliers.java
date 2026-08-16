package org.spongepowered.include.com.google.common.base;

import java.io.Serializable;

public final class Suppliers {
   public static <T> Supplier<T> memoize(Supplier<T> delegate) {
      if (!(delegate instanceof NonSerializableMemoizingSupplier) && !(delegate instanceof MemoizingSupplier)) {
         return (Supplier<T>)(delegate instanceof Serializable ? new MemoizingSupplier(delegate) : new NonSerializableMemoizingSupplier(delegate));
      } else {
         return delegate;
      }
   }

   static class MemoizingSupplier<T> implements Serializable, Supplier<T> {
      final Supplier<T> delegate;
      transient volatile boolean initialized;
      transient T value;

      MemoizingSupplier(Supplier<T> delegate) {
         this.delegate = (Supplier)Preconditions.checkNotNull(delegate);
      }

      public T get() {
         if (!this.initialized) {
            synchronized(this) {
               if (!this.initialized) {
                  T t = this.delegate.get();
                  this.value = t;
                  this.initialized = true;
                  return t;
               }
            }
         }

         return this.value;
      }

      public String toString() {
         return "Suppliers.memoize(" + this.delegate + ")";
      }
   }

   static class NonSerializableMemoizingSupplier<T> implements Supplier<T> {
      volatile Supplier<T> delegate;
      volatile boolean initialized;
      T value;

      NonSerializableMemoizingSupplier(Supplier<T> delegate) {
         this.delegate = (Supplier)Preconditions.checkNotNull(delegate);
      }

      public T get() {
         if (!this.initialized) {
            synchronized(this) {
               if (!this.initialized) {
                  T t = this.delegate.get();
                  this.value = t;
                  this.initialized = true;
                  this.delegate = null;
                  return t;
               }
            }
         }

         return this.value;
      }

      public String toString() {
         return "Suppliers.memoize(" + this.delegate + ")";
      }
   }
}
