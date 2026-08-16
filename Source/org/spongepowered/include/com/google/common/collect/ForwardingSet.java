package org.spongepowered.include.com.google.common.collect;

import java.util.Set;
import javax.annotation.Nullable;

public abstract class ForwardingSet<E> extends ForwardingCollection<E> implements Set<E> {
   protected ForwardingSet() {
   }

   protected abstract Set<E> delegate();

   public boolean equals(@Nullable Object object) {
      return object == this || this.delegate().equals(object);
   }

   public int hashCode() {
      return this.delegate().hashCode();
   }
}
