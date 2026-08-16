package org.spongepowered.include.com.google.common.collect;

import java.util.ListIterator;

abstract class TransformedListIterator<F, T> extends TransformedIterator<F, T> implements ListIterator<T> {
   TransformedListIterator(ListIterator<? extends F> backingIterator) {
      super(backingIterator);
   }

   private ListIterator<? extends F> backingIterator() {
      return Iterators.<F>cast(this.backingIterator);
   }

   public final boolean hasPrevious() {
      return this.backingIterator().hasPrevious();
   }

   public final T previous() {
      return (T)this.transform(this.backingIterator().previous());
   }

   public final int nextIndex() {
      return this.backingIterator().nextIndex();
   }

   public final int previousIndex() {
      return this.backingIterator().previousIndex();
   }

   public void set(T element) {
      throw new UnsupportedOperationException();
   }

   public void add(T element) {
      throw new UnsupportedOperationException();
   }
}
