package org.spongepowered.include.com.google.common.collect;

import java.util.Spliterator;
import java.util.Spliterators;

class RegularImmutableList<E> extends ImmutableList<E> {
   static final ImmutableList<Object> EMPTY;
   private final transient Object[] array;

   RegularImmutableList(Object[] array) {
      this.array = array;
   }

   public int size() {
      return this.array.length;
   }

   int copyIntoArray(Object[] dst, int dstOff) {
      System.arraycopy(this.array, 0, dst, dstOff, this.array.length);
      return dstOff + this.array.length;
   }

   public E get(int index) {
      return (E)this.array[index];
   }

   public UnmodifiableListIterator<E> listIterator(int index) {
      return Iterators.<E>forArray(this.array, 0, this.array.length, index);
   }

   public Spliterator<E> spliterator() {
      return Spliterators.spliterator(this.array, 1296);
   }

   static {
      EMPTY = new RegularImmutableList<Object>(ObjectArrays.EMPTY_ARRAY);
   }
}
