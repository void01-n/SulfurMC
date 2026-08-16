package org.spongepowered.include.com.google.common.collect;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.errorprone.annotations.CanIgnoreReturnValue;

abstract class AbstractListMultimap<K, V> extends AbstractMapBasedMultimap<K, V> implements ListMultimap<K, V> {
   protected AbstractListMultimap(Map<K, Collection<V>> map) {
      super(map);
   }

   abstract List<V> createCollection();

   List<V> createUnmodifiableEmptyCollection() {
      return ImmutableList.<V>of();
   }

   public List<V> get(@Nullable K key) {
      return (List)super.get(key);
   }

   @CanIgnoreReturnValue
   public List<V> removeAll(@Nullable Object key) {
      return (List)super.removeAll(key);
   }

   @CanIgnoreReturnValue
   public boolean put(@Nullable K key, @Nullable V value) {
      return super.put(key, value);
   }

   public Map<K, Collection<V>> asMap() {
      return super.asMap();
   }

   public boolean equals(@Nullable Object object) {
      return super.equals(object);
   }
}
