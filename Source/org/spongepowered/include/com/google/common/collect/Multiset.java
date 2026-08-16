package org.spongepowered.include.com.google.common.collect;

import java.util.Collection;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import org.spongepowered.include.com.google.common.base.Preconditions;

public interface Multiset<E> extends Collection<E> {
   int size();

   Set<E> elementSet();

   Set<Entry<E>> entrySet();

   default void forEach(Consumer<? super E> action) {
      Preconditions.checkNotNull(action);
      this.entrySet().forEach((entry) -> {
         E elem = (E)entry.getElement();
         int count = entry.getCount();

         for(int i = 0; i < count; ++i) {
            action.accept(elem);
         }

      });
   }

   default Spliterator<E> spliterator() {
      return Multisets.<E>spliteratorImpl(this);
   }

   public interface Entry<E> {
      E getElement();

      int getCount();
   }
}
