package org.spongepowered.include.com.google.common.collect;

import javax.annotation.Nullable;

class DescendingImmutableSortedSet<E> extends ImmutableSortedSet<E> {
   private final ImmutableSortedSet<E> forward;

   DescendingImmutableSortedSet(ImmutableSortedSet<E> forward) {
      super(Ordering.from(forward.comparator()).reverse());
      this.forward = forward;
   }

   public boolean contains(@Nullable Object object) {
      return this.forward.contains(object);
   }

   public int size() {
      return this.forward.size();
   }

   public UnmodifiableIterator<E> iterator() {
      return this.forward.descendingIterator();
   }

   ImmutableSortedSet<E> headSetImpl(E toElement, boolean inclusive) {
      return this.forward.tailSet(toElement, inclusive).descendingSet();
   }

   ImmutableSortedSet<E> subSetImpl(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
      return this.forward.subSet(toElement, toInclusive, fromElement, fromInclusive).descendingSet();
   }

   ImmutableSortedSet<E> tailSetImpl(E fromElement, boolean inclusive) {
      return this.forward.headSet(fromElement, inclusive).descendingSet();
   }

   public ImmutableSortedSet<E> descendingSet() {
      return this.forward;
   }

   public UnmodifiableIterator<E> descendingIterator() {
      return this.forward.iterator();
   }

   ImmutableSortedSet<E> createDescendingSet() {
      throw new AssertionError("should never be called");
   }

   public E lower(E element) {
      return this.forward.higher(element);
   }

   public E floor(E element) {
      return this.forward.ceiling(element);
   }

   public E ceiling(E element) {
      return this.forward.floor(element);
   }

   public E higher(E element) {
      return this.forward.lower(element);
   }
}
