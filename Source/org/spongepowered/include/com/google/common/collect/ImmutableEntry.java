package org.spongepowered.include.com.google.common.collect;

import java.io.Serializable;
import javax.annotation.Nullable;

class ImmutableEntry<K, V> extends AbstractMapEntry<K, V> implements Serializable {
   final K key;
   final V value;

   ImmutableEntry(@Nullable K key, @Nullable V value) {
      this.key = key;
      this.value = value;
   }

   @Nullable
   public final K getKey() {
      return this.key;
   }

   @Nullable
   public final V getValue() {
      return this.value;
   }

   public final V setValue(V value) {
      throw new UnsupportedOperationException();
   }
}
