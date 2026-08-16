package org.spongepowered.include.com.google.common.collect;

import org.spongepowered.include.com.google.errorprone.annotations.CanIgnoreReturnValue;

public abstract class ImmutableBiMap<K, V> extends ImmutableBiMapFauxverideShim<K, V> implements BiMap<K, V> {
   public static <K, V> ImmutableBiMap<K, V> of() {
      return RegularImmutableBiMap.EMPTY;
   }

   public static <K, V> ImmutableBiMap<K, V> of(K k1, V v1) {
      return new SingletonImmutableBiMap<K, V>(k1, v1);
   }

   ImmutableBiMap() {
   }

   public abstract ImmutableBiMap<V, K> inverse();

   public ImmutableSet<V> values() {
      return this.inverse().keySet();
   }

   /** @deprecated */
   @Deprecated
   @CanIgnoreReturnValue
   public V forcePut(K key, V value) {
      throw new UnsupportedOperationException();
   }
}
