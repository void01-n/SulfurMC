package org.spongepowered.include.com.google.common.collect;

import java.util.Spliterator;
import java.util.Spliterators;
import javax.annotation.Nullable;

final class RegularImmutableSet<E> extends ImmutableSet.Indexed<E> {
   static final RegularImmutableSet<Object> EMPTY;
   private final transient Object[] elements;
   final transient Object[] table;
   private final transient int mask;
   private final transient int hashCode;

   RegularImmutableSet(Object[] elements, int hashCode, Object[] table, int mask) {
      this.elements = elements;
      this.table = table;
      this.mask = mask;
      this.hashCode = hashCode;
   }

   public boolean contains(@Nullable Object target) {
      Object[] table = this.table;
      if (target != null && table != null) {
         int i = Hashing.smearedHash(target);

         while(true) {
            i &= this.mask;
            Object candidate = table[i];
            if (candidate == null) {
               return false;
            }

            if (candidate.equals(target)) {
               return true;
            }

            ++i;
         }
      } else {
         return false;
      }
   }

   public int size() {
      return this.elements.length;
   }

   E get(int i) {
      return (E)this.elements[i];
   }

   public Spliterator<E> spliterator() {
      return Spliterators.spliterator(this.elements, 1297);
   }

   int copyIntoArray(Object[] dst, int offset) {
      System.arraycopy(this.elements, 0, dst, offset, this.elements.length);
      return offset + this.elements.length;
   }

   ImmutableList<E> createAsList() {
      return (ImmutableList<E>)(this.table == null ? ImmutableList.of() : new RegularImmutableAsList(this, this.elements));
   }

   public int hashCode() {
      return this.hashCode;
   }

   boolean isHashCodeFast() {
      return true;
   }

   static {
      EMPTY = new RegularImmutableSet<Object>(ObjectArrays.EMPTY_ARRAY, 0, (Object[])null, 0);
   }
}
