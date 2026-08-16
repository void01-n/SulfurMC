package org.spongepowered.include.com.google.common.collect;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.errorprone.annotations.concurrent.LazyInit;
import org.spongepowered.include.com.google.j2objc.annotations.RetainedWith;

class RegularImmutableBiMap<K, V> extends ImmutableBiMap<K, V> {
   static final RegularImmutableBiMap<Object, Object> EMPTY;
   private final transient ImmutableMapEntry<K, V>[] keyTable;
   private final transient ImmutableMapEntry<K, V>[] valueTable;
   private final transient Map.Entry<K, V>[] entries;
   private final transient int mask;
   private final transient int hashCode;
   @LazyInit
   @RetainedWith
   private transient ImmutableBiMap<V, K> inverse;

   private RegularImmutableBiMap(ImmutableMapEntry<K, V>[] keyTable, ImmutableMapEntry<K, V>[] valueTable, Map.Entry<K, V>[] entries, int mask, int hashCode) {
      this.keyTable = keyTable;
      this.valueTable = valueTable;
      this.entries = entries;
      this.mask = mask;
      this.hashCode = hashCode;
   }

   @Nullable
   public V get(@Nullable Object key) {
      return (V)(this.keyTable == null ? null : RegularImmutableMap.get(key, this.keyTable, this.mask));
   }

   ImmutableSet<Map.Entry<K, V>> createEntrySet() {
      return (ImmutableSet<Map.Entry<K, V>>)(this.isEmpty() ? ImmutableSet.of() : new ImmutableMapEntrySet.RegularEntrySet(this, this.entries));
   }

   public void forEach(BiConsumer<? super K, ? super V> action) {
      Preconditions.checkNotNull(action);

      for(Map.Entry<K, V> entry : this.entries) {
         action.accept(entry.getKey(), entry.getValue());
      }

   }

   boolean isHashCodeFast() {
      return true;
   }

   public int hashCode() {
      return this.hashCode;
   }

   public int size() {
      return this.entries.length;
   }

   public ImmutableBiMap<V, K> inverse() {
      if (this.isEmpty()) {
         return ImmutableBiMap.<V, K>of();
      } else {
         ImmutableBiMap<V, K> result = this.inverse;
         return result == null ? (this.inverse = new Inverse()) : result;
      }
   }

   static {
      EMPTY = new RegularImmutableBiMap<Object, Object>((ImmutableMapEntry[])null, (ImmutableMapEntry[])null, ImmutableMap.EMPTY_ENTRY_ARRAY, 0, 0);
   }

   private final class Inverse extends ImmutableBiMap<V, K> {
      private Inverse() {
      }

      public int size() {
         return this.inverse().size();
      }

      public ImmutableBiMap<K, V> inverse() {
         return RegularImmutableBiMap.this;
      }

      public void forEach(BiConsumer<? super V, ? super K> action) {
         Preconditions.checkNotNull(action);
         RegularImmutableBiMap.this.forEach((k, v) -> action.accept(v, k));
      }

      public K get(@Nullable Object value) {
         if (value != null && RegularImmutableBiMap.this.valueTable != null) {
            int bucket = Hashing.smear(value.hashCode()) & RegularImmutableBiMap.this.mask;

            for(ImmutableMapEntry<K, V> entry = RegularImmutableBiMap.this.valueTable[bucket]; entry != null; entry = entry.getNextInValueBucket()) {
               if (value.equals(entry.getValue())) {
                  return (K)entry.getKey();
               }
            }

            return null;
         } else {
            return null;
         }
      }

      ImmutableSet<Map.Entry<V, K>> createEntrySet() {
         return new InverseEntrySet();
      }

      final class InverseEntrySet extends ImmutableMapEntrySet<V, K> {
         ImmutableMap<V, K> map() {
            return Inverse.this;
         }

         boolean isHashCodeFast() {
            return true;
         }

         public int hashCode() {
            return RegularImmutableBiMap.this.hashCode;
         }

         public UnmodifiableIterator<Map.Entry<V, K>> iterator() {
            return this.asList().iterator();
         }

         public void forEach(Consumer<? super Map.Entry<V, K>> action) {
            this.asList().forEach(action);
         }

         ImmutableList<Map.Entry<V, K>> createAsList() {
            return new ImmutableAsList<Map.Entry<V, K>>() {
               public Map.Entry<V, K> get(int index) {
                  Map.Entry<K, V> entry = RegularImmutableBiMap.this.entries[index];
                  return Maps.<V, K>immutableEntry(entry.getValue(), entry.getKey());
               }

               ImmutableCollection<Map.Entry<V, K>> delegateCollection() {
                  return InverseEntrySet.this;
               }
            };
         }
      }
   }
}
