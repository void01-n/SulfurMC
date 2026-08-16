package org.spongepowered.include.com.google.common.collect;

import java.util.Collections;
import java.util.Spliterator;
import org.spongepowered.include.com.google.common.base.Preconditions;

final class SingletonImmutableList<E> extends ImmutableList<E> {
   final transient E element;

   SingletonImmutableList(E element) {
      this.element = (E)Preconditions.checkNotNull(element);
   }

   public E get(int index) {
      Preconditions.checkElementIndex(index, 1);
      return this.element;
   }

   public UnmodifiableIterator<E> iterator() {
      return Iterators.<E>singletonIterator(this.element);
   }

   public Spliterator<E> spliterator() {
      return Collections.singleton(this.element).spliterator();
   }

   public int size() {
      return 1;
   }

   public ImmutableList<E> subList(int fromIndex, int toIndex) {
      Preconditions.checkPositionIndexes(fromIndex, toIndex, 1);
      return (ImmutableList<E>)(fromIndex == toIndex ? ImmutableList.of() : this);
   }

   public String toString() {
      return '[' + this.element.toString() + ']';
   }
}
