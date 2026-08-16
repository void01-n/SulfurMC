package org.spongepowered.include.com.google.common.collect;

import java.util.Iterator;
import org.spongepowered.include.com.google.common.base.Preconditions;

abstract class TransformedIterator<F, T> implements Iterator<T> {
   final Iterator<? extends F> backingIterator;

   TransformedIterator(Iterator<? extends F> backingIterator) {
      this.backingIterator = (Iterator)Preconditions.checkNotNull(backingIterator);
   }

   abstract T transform(F var1);

   public final boolean hasNext() {
      return this.backingIterator.hasNext();
   }

   public final T next() {
      return (T)this.transform(this.backingIterator.next());
   }

   public final void remove() {
      this.backingIterator.remove();
   }
}
