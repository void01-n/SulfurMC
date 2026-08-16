package org.spongepowered.include.com.google.common.collect;

import java.util.Comparator;
import java.util.SortedSet;

public abstract class ForwardingSortedSet<E> extends ForwardingSet<E> implements SortedSet<E> {
   protected ForwardingSortedSet() {
   }

   protected abstract SortedSet<E> delegate();

   public Comparator<? super E> comparator() {
      return this.delegate().comparator();
   }

   public E first() {
      return (E)this.delegate().first();
   }

   public SortedSet<E> headSet(E toElement) {
      return this.delegate().headSet(toElement);
   }

   public E last() {
      return (E)this.delegate().last();
   }

   public SortedSet<E> subSet(E fromElement, E toElement) {
      return this.delegate().subSet(fromElement, toElement);
   }

   public SortedSet<E> tailSet(E fromElement) {
      return this.delegate().tailSet(fromElement);
   }
}
