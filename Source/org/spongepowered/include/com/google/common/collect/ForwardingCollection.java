package org.spongepowered.include.com.google.common.collect;

import java.util.Collection;
import java.util.Iterator;
import org.spongepowered.include.com.google.errorprone.annotations.CanIgnoreReturnValue;

public abstract class ForwardingCollection<E> extends ForwardingObject implements Collection<E> {
   protected ForwardingCollection() {
   }

   protected abstract Collection<E> delegate();

   public Iterator<E> iterator() {
      return this.delegate().iterator();
   }

   public int size() {
      return this.delegate().size();
   }

   @CanIgnoreReturnValue
   public boolean removeAll(Collection<?> collection) {
      return this.delegate().removeAll(collection);
   }

   public boolean isEmpty() {
      return this.delegate().isEmpty();
   }

   public boolean contains(Object object) {
      return this.delegate().contains(object);
   }

   @CanIgnoreReturnValue
   public boolean add(E element) {
      return this.delegate().add(element);
   }

   @CanIgnoreReturnValue
   public boolean remove(Object object) {
      return this.delegate().remove(object);
   }

   public boolean containsAll(Collection<?> collection) {
      return this.delegate().containsAll(collection);
   }

   @CanIgnoreReturnValue
   public boolean addAll(Collection<? extends E> collection) {
      return this.delegate().addAll(collection);
   }

   @CanIgnoreReturnValue
   public boolean retainAll(Collection<?> collection) {
      return this.delegate().retainAll(collection);
   }

   public void clear() {
      this.delegate().clear();
   }

   public Object[] toArray() {
      return this.delegate().toArray();
   }

   @CanIgnoreReturnValue
   public <T> T[] toArray(T[] array) {
      return (T[])this.delegate().toArray(array);
   }
}
