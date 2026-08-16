package org.spongepowered.include.com.google.common.collect;

import java.util.Map;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.base.Objects;

abstract class AbstractMapEntry<K, V> implements Map.Entry<K, V> {
   public abstract K getKey();

   public abstract V getValue();

   public V setValue(V value) {
      throw new UnsupportedOperationException();
   }

   public boolean equals(@Nullable Object object) {
      if (!(object instanceof Map.Entry)) {
         return false;
      } else {
         Map.Entry<?, ?> that = (Map.Entry)object;
         return Objects.equal(this.getKey(), that.getKey()) && Objects.equal(this.getValue(), that.getValue());
      }
   }

   public int hashCode() {
      K k = (K)this.getKey();
      V v = (V)this.getValue();
      return (k == null ? 0 : k.hashCode()) ^ (v == null ? 0 : v.hashCode());
   }

   public String toString() {
      return this.getKey() + "=" + this.getValue();
   }
}
