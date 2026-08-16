package org.spongepowered.include.com.google.common.collect;

abstract class ImmutableAsList<E> extends ImmutableList<E> {
   abstract ImmutableCollection<E> delegateCollection();

   public boolean contains(Object target) {
      return this.delegateCollection().contains(target);
   }

   public int size() {
      return this.delegateCollection().size();
   }

   public boolean isEmpty() {
      return this.delegateCollection().isEmpty();
   }
}
