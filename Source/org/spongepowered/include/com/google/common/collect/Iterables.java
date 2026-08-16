package org.spongepowered.include.com.google.common.collect;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.base.Function;
import org.spongepowered.include.com.google.common.base.Preconditions;

public final class Iterables {
   public static String toString(Iterable<?> iterable) {
      return Iterators.toString(iterable.iterator());
   }

   public static <T> Iterable<T> concat(Iterable<? extends T> a, Iterable<? extends T> b) {
      return FluentIterable.<T>concat(a, b);
   }

   public static <F, T> Iterable<T> transform(final Iterable<F> fromIterable, final Function<? super F, ? extends T> function) {
      Preconditions.checkNotNull(fromIterable);
      Preconditions.checkNotNull(function);
      return new FluentIterable<T>() {
         public Iterator<T> iterator() {
            return Iterators.transform(fromIterable.iterator(), function);
         }

         public void forEach(Consumer<? super T> action) {
            Preconditions.checkNotNull(action);
            fromIterable.forEach((f) -> action.accept(function.apply(f)));
         }

         public Spliterator<T> spliterator() {
            return CollectSpliterators.map(fromIterable.spliterator(), function);
         }
      };
   }

   @Nullable
   public static <T> T getFirst(Iterable<? extends T> iterable, @Nullable T defaultValue) {
      return (T)Iterators.getNext(iterable.iterator(), defaultValue);
   }

   static <T> Function<Iterable<? extends T>, Iterator<? extends T>> toIterator() {
      return new Function<Iterable<? extends T>, Iterator<? extends T>>() {
         public Iterator<? extends T> apply(Iterable<? extends T> iterable) {
            return iterable.iterator();
         }
      };
   }
}
