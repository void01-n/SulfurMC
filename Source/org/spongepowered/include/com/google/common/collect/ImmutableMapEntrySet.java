package org.spongepowered.include.com.google.common.collect;

import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.j2objc.annotations.Weak;

abstract class ImmutableMapEntrySet<K, V> extends ImmutableSet<Map.Entry<K, V>> {
   abstract ImmutableMap<K, V> map();

   public int size() {
      return this.map().size();
   }

   public boolean contains(@Nullable Object object) {
      if (!(object instanceof Map.Entry)) {
         return false;
      } else {
         Map.Entry<?, ?> entry = (Map.Entry)object;
         V value = (V)this.map().get(entry.getKey());
         return value != null && value.equals(entry.getValue());
      }
   }

   boolean isHashCodeFast() {
      return this.map().isHashCodeFast();
   }

   public int hashCode() {
      return this.map().hashCode();
   }

   static final class RegularEntrySet<K, V> extends ImmutableMapEntrySet<K, V> {
      @Weak
      private final transient ImmutableMap<K, V> map;
      private final transient Map.Entry<K, V>[] entries;

      RegularEntrySet(ImmutableMap<K, V> map, Map.Entry<K, V>[] entries) {
         this.map = map;
         this.entries = entries;
      }

      ImmutableMap<K, V> map() {
         return this.map;
      }

      public UnmodifiableIterator<Map.Entry<K, V>> iterator() {
         return Iterators.<Map.Entry<K, V>>forArray(this.entries);
      }

      public Spliterator<Map.Entry<K, V>> spliterator() {
         return Spliterators.spliterator(this.entries, 1297);
      }

      public void forEach(Consumer<? super Map.Entry<K, V>> action) {
         Preconditions.checkNotNull(action);

         for(Map.Entry<K, V> entry : this.entries) {
            action.accept(entry);
         }

      }

      ImmutableList<Map.Entry<K, V>> createAsList() {
         return new RegularImmutableAsList<Map.Entry<K, V>>(this, this.entries);
      }
   }
}
