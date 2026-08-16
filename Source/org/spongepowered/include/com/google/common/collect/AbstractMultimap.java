package org.spongepowered.include.com.google.common.collect;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.errorprone.annotations.CanIgnoreReturnValue;

abstract class AbstractMultimap<K, V> implements Multimap<K, V> {
   private transient Set<K> keySet;
   private transient Map<K, Collection<V>> asMap;

   @CanIgnoreReturnValue
   public boolean put(@Nullable K key, @Nullable V value) {
      return this.get(key).add(value);
   }

   public Set<K> keySet() {
      Set<K> result = this.keySet;
      return result == null ? (this.keySet = this.createKeySet()) : result;
   }

   Set<K> createKeySet() {
      return new Maps.KeySet(this.asMap());
   }

   public Map<K, Collection<V>> asMap() {
      Map<K, Collection<V>> result = this.asMap;
      return result == null ? (this.asMap = this.createAsMap()) : result;
   }

   abstract Map<K, Collection<V>> createAsMap();

   public boolean equals(@Nullable Object object) {
      return Multimaps.equalsImpl(this, object);
   }

   public int hashCode() {
      return this.asMap().hashCode();
   }

   public String toString() {
      return this.asMap().toString();
   }
}
