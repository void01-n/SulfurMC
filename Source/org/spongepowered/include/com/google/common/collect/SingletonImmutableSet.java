package org.spongepowered.include.com.google.common.collect;

import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.errorprone.annotations.concurrent.LazyInit;

final class SingletonImmutableSet<E> extends ImmutableSet<E> {
   final transient E element;
   @LazyInit
   private transient int cachedHashCode;

   SingletonImmutableSet(E element) {
      this.element = (E)Preconditions.checkNotNull(element);
   }

   SingletonImmutableSet(E element, int hashCode) {
      this.element = element;
      this.cachedHashCode = hashCode;
   }

   public int size() {
      return 1;
   }

   public boolean contains(Object target) {
      return this.element.equals(target);
   }

   public UnmodifiableIterator<E> iterator() {
      return Iterators.<E>singletonIterator(this.element);
   }

   ImmutableList<E> createAsList() {
      return ImmutableList.<E>of(this.element);
   }

   int copyIntoArray(Object[] dst, int offset) {
      dst[offset] = this.element;
      return offset + 1;
   }

   public final int hashCode() {
      int code = this.cachedHashCode;
      if (code == 0) {
         this.cachedHashCode = code = this.element.hashCode();
      }

      return code;
   }

   boolean isHashCodeFast() {
      return this.cachedHashCode != 0;
   }

   public String toString() {
      return '[' + this.element.toString() + ']';
   }
}
