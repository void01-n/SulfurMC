package org.spongepowered.include.com.google.common.collect;

import javax.annotation.Nullable;

class ImmutableMapEntry<K, V> extends ImmutableEntry<K, V> {
   static <K, V> ImmutableMapEntry<K, V>[] createEntryArray(int size) {
      return new ImmutableMapEntry[size];
   }

   ImmutableMapEntry(K key, V value) {
      super(key, value);
      CollectPreconditions.checkEntryNotNull(key, value);
   }

   @Nullable
   ImmutableMapEntry<K, V> getNextInKeyBucket() {
      return null;
   }

   @Nullable
   ImmutableMapEntry<K, V> getNextInValueBucket() {
      return null;
   }

   boolean isReusable() {
      return true;
   }

   static class NonTerminalImmutableMapEntry<K, V> extends ImmutableMapEntry<K, V> {
      private final transient ImmutableMapEntry<K, V> nextInKeyBucket;

      NonTerminalImmutableMapEntry(K key, V value, ImmutableMapEntry<K, V> nextInKeyBucket) {
         super(key, value);
         this.nextInKeyBucket = nextInKeyBucket;
      }

      @Nullable
      final ImmutableMapEntry<K, V> getNextInKeyBucket() {
         return this.nextInKeyBucket;
      }

      final boolean isReusable() {
         return false;
      }
   }
}
