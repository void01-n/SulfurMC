package org.spongepowered.include.com.google.common.collect;

import java.io.Serializable;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.base.Objects;
import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.spongepowered.include.com.google.j2objc.annotations.RetainedWith;

public final class HashBiMap<K, V> extends Maps.IteratorBasedAbstractMap<K, V> implements Serializable, BiMap<K, V> {
   private transient BiEntry<K, V>[] hashTableKToV;
   private transient BiEntry<K, V>[] hashTableVToK;
   private transient BiEntry<K, V> firstInKeyInsertionOrder;
   private transient BiEntry<K, V> lastInKeyInsertionOrder;
   private transient int size;
   private transient int mask;
   private transient int modCount;
   @RetainedWith
   private transient BiMap<V, K> inverse;

   public static <K, V> HashBiMap<K, V> create() {
      return create(16);
   }

   public static <K, V> HashBiMap<K, V> create(int expectedSize) {
      return new HashBiMap<K, V>(expectedSize);
   }

   private HashBiMap(int expectedSize) {
      this.init(expectedSize);
   }

   private void init(int expectedSize) {
      CollectPreconditions.checkNonnegative(expectedSize, "expectedSize");
      int tableSize = Hashing.closedTableSize(expectedSize, (double)1.0F);
      this.hashTableKToV = this.createTable(tableSize);
      this.hashTableVToK = this.createTable(tableSize);
      this.firstInKeyInsertionOrder = null;
      this.lastInKeyInsertionOrder = null;
      this.size = 0;
      this.mask = tableSize - 1;
      this.modCount = 0;
   }

   private void delete(BiEntry<K, V> entry) {
      int keyBucket = entry.keyHash & this.mask;
      BiEntry<K, V> prevBucketEntry = null;

      for(BiEntry<K, V> bucketEntry = this.hashTableKToV[keyBucket]; bucketEntry != entry; bucketEntry = bucketEntry.nextInKToVBucket) {
         prevBucketEntry = bucketEntry;
      }

      if (prevBucketEntry == null) {
         this.hashTableKToV[keyBucket] = entry.nextInKToVBucket;
      } else {
         prevBucketEntry.nextInKToVBucket = entry.nextInKToVBucket;
      }

      int valueBucket = entry.valueHash & this.mask;
      prevBucketEntry = null;

      for(BiEntry<K, V> bucketEntry = this.hashTableVToK[valueBucket]; bucketEntry != entry; bucketEntry = bucketEntry.nextInVToKBucket) {
         prevBucketEntry = bucketEntry;
      }

      if (prevBucketEntry == null) {
         this.hashTableVToK[valueBucket] = entry.nextInVToKBucket;
      } else {
         prevBucketEntry.nextInVToKBucket = entry.nextInVToKBucket;
      }

      if (entry.prevInKeyInsertionOrder == null) {
         this.firstInKeyInsertionOrder = entry.nextInKeyInsertionOrder;
      } else {
         entry.prevInKeyInsertionOrder.nextInKeyInsertionOrder = entry.nextInKeyInsertionOrder;
      }

      if (entry.nextInKeyInsertionOrder == null) {
         this.lastInKeyInsertionOrder = entry.prevInKeyInsertionOrder;
      } else {
         entry.nextInKeyInsertionOrder.prevInKeyInsertionOrder = entry.prevInKeyInsertionOrder;
      }

      --this.size;
      ++this.modCount;
   }

   private void insert(BiEntry<K, V> entry, @Nullable BiEntry<K, V> oldEntryForKey) {
      int keyBucket = entry.keyHash & this.mask;
      entry.nextInKToVBucket = this.hashTableKToV[keyBucket];
      this.hashTableKToV[keyBucket] = entry;
      int valueBucket = entry.valueHash & this.mask;
      entry.nextInVToKBucket = this.hashTableVToK[valueBucket];
      this.hashTableVToK[valueBucket] = entry;
      if (oldEntryForKey == null) {
         entry.prevInKeyInsertionOrder = this.lastInKeyInsertionOrder;
         entry.nextInKeyInsertionOrder = null;
         if (this.lastInKeyInsertionOrder == null) {
            this.firstInKeyInsertionOrder = entry;
         } else {
            this.lastInKeyInsertionOrder.nextInKeyInsertionOrder = entry;
         }

         this.lastInKeyInsertionOrder = entry;
      } else {
         entry.prevInKeyInsertionOrder = oldEntryForKey.prevInKeyInsertionOrder;
         if (entry.prevInKeyInsertionOrder == null) {
            this.firstInKeyInsertionOrder = entry;
         } else {
            entry.prevInKeyInsertionOrder.nextInKeyInsertionOrder = entry;
         }

         entry.nextInKeyInsertionOrder = oldEntryForKey.nextInKeyInsertionOrder;
         if (entry.nextInKeyInsertionOrder == null) {
            this.lastInKeyInsertionOrder = entry;
         } else {
            entry.nextInKeyInsertionOrder.prevInKeyInsertionOrder = entry;
         }
      }

      ++this.size;
      ++this.modCount;
   }

   private BiEntry<K, V> seekByKey(@Nullable Object key, int keyHash) {
      for(BiEntry<K, V> entry = this.hashTableKToV[keyHash & this.mask]; entry != null; entry = entry.nextInKToVBucket) {
         if (keyHash == entry.keyHash && Objects.equal(key, entry.key)) {
            return entry;
         }
      }

      return null;
   }

   private BiEntry<K, V> seekByValue(@Nullable Object value, int valueHash) {
      for(BiEntry<K, V> entry = this.hashTableVToK[valueHash & this.mask]; entry != null; entry = entry.nextInVToKBucket) {
         if (valueHash == entry.valueHash && Objects.equal(value, entry.value)) {
            return entry;
         }
      }

      return null;
   }

   public boolean containsKey(@Nullable Object key) {
      return this.seekByKey(key, Hashing.smearedHash(key)) != null;
   }

   public boolean containsValue(@Nullable Object value) {
      return this.seekByValue(value, Hashing.smearedHash(value)) != null;
   }

   @Nullable
   public V get(@Nullable Object key) {
      return (V)Maps.valueOrNull(this.seekByKey(key, Hashing.smearedHash(key)));
   }

   @CanIgnoreReturnValue
   public V put(@Nullable K key, @Nullable V value) {
      return (V)this.put(key, value, false);
   }

   @CanIgnoreReturnValue
   public V forcePut(@Nullable K key, @Nullable V value) {
      return (V)this.put(key, value, true);
   }

   private V put(@Nullable K key, @Nullable V value, boolean force) {
      int keyHash = Hashing.smearedHash(key);
      int valueHash = Hashing.smearedHash(value);
      BiEntry<K, V> oldEntryForKey = this.seekByKey(key, keyHash);
      if (oldEntryForKey != null && valueHash == oldEntryForKey.valueHash && Objects.equal(value, oldEntryForKey.value)) {
         return value;
      } else {
         BiEntry<K, V> oldEntryForValue = this.seekByValue(value, valueHash);
         if (oldEntryForValue != null) {
            if (!force) {
               throw new IllegalArgumentException("value already present: " + value);
            }

            this.delete(oldEntryForValue);
         }

         BiEntry<K, V> newEntry = new BiEntry<K, V>(key, keyHash, value, valueHash);
         if (oldEntryForKey != null) {
            this.delete(oldEntryForKey);
            this.insert(newEntry, oldEntryForKey);
            oldEntryForKey.prevInKeyInsertionOrder = null;
            oldEntryForKey.nextInKeyInsertionOrder = null;
            this.rehashIfNecessary();
            return oldEntryForKey.value;
         } else {
            this.insert(newEntry, (BiEntry)null);
            this.rehashIfNecessary();
            return null;
         }
      }
   }

   @Nullable
   private K putInverse(@Nullable V value, @Nullable K key, boolean force) {
      int valueHash = Hashing.smearedHash(value);
      int keyHash = Hashing.smearedHash(key);
      BiEntry<K, V> oldEntryForValue = this.seekByValue(value, valueHash);
      if (oldEntryForValue != null && keyHash == oldEntryForValue.keyHash && Objects.equal(key, oldEntryForValue.key)) {
         return key;
      } else {
         BiEntry<K, V> oldEntryForKey = this.seekByKey(key, keyHash);
         if (oldEntryForKey != null) {
            if (!force) {
               throw new IllegalArgumentException("value already present: " + key);
            }

            this.delete(oldEntryForKey);
         }

         if (oldEntryForValue != null) {
            this.delete(oldEntryForValue);
         }

         BiEntry<K, V> newEntry = new BiEntry<K, V>(key, keyHash, value, valueHash);
         this.insert(newEntry, oldEntryForKey);
         if (oldEntryForKey != null) {
            oldEntryForKey.prevInKeyInsertionOrder = null;
            oldEntryForKey.nextInKeyInsertionOrder = null;
         }

         this.rehashIfNecessary();
         return (K)Maps.keyOrNull(oldEntryForValue);
      }
   }

   private void rehashIfNecessary() {
      BiEntry<K, V>[] oldKToV = this.hashTableKToV;
      if (Hashing.needsResizing(this.size, oldKToV.length, (double)1.0F)) {
         int newTableSize = oldKToV.length * 2;
         this.hashTableKToV = this.createTable(newTableSize);
         this.hashTableVToK = this.createTable(newTableSize);
         this.mask = newTableSize - 1;
         this.size = 0;

         for(BiEntry<K, V> entry = this.firstInKeyInsertionOrder; entry != null; entry = entry.nextInKeyInsertionOrder) {
            this.insert(entry, entry);
         }

         ++this.modCount;
      }

   }

   private BiEntry<K, V>[] createTable(int length) {
      return new BiEntry[length];
   }

   @CanIgnoreReturnValue
   public V remove(@Nullable Object key) {
      BiEntry<K, V> entry = this.seekByKey(key, Hashing.smearedHash(key));
      if (entry == null) {
         return null;
      } else {
         this.delete(entry);
         entry.prevInKeyInsertionOrder = null;
         entry.nextInKeyInsertionOrder = null;
         return entry.value;
      }
   }

   public void clear() {
      this.size = 0;
      Arrays.fill(this.hashTableKToV, (Object)null);
      Arrays.fill(this.hashTableVToK, (Object)null);
      this.firstInKeyInsertionOrder = null;
      this.lastInKeyInsertionOrder = null;
      ++this.modCount;
   }

   public int size() {
      return this.size;
   }

   public Set<K> keySet() {
      return new KeySet();
   }

   public Set<V> values() {
      return this.inverse().keySet();
   }

   Iterator<Map.Entry<K, V>> entryIterator() {
      return new Itr() {
         Map.Entry<K, V> output(BiEntry<K, V> entry) {
            return new null.MapEntry(entry);
         }

         class MapEntry extends AbstractMapEntry<K, V> {
            BiEntry<K, V> delegate;

            MapEntry(BiEntry<K, V> entry) {
               this.delegate = entry;
            }

            public K getKey() {
               return this.delegate.key;
            }

            public V getValue() {
               return this.delegate.value;
            }

            public V setValue(V value) {
               V oldValue = this.delegate.value;
               int valueHash = Hashing.smearedHash(value);
               if (valueHash == this.delegate.valueHash && Objects.equal(value, oldValue)) {
                  return value;
               } else {
                  Preconditions.checkArgument(HashBiMap.this.seekByValue(value, valueHash) == null, "value already present: %s", value);
                  HashBiMap.this.delete(this.delegate);
                  BiEntry<K, V> newEntry = new BiEntry<K, V>(this.delegate.key, this.delegate.keyHash, value, valueHash);
                  HashBiMap.this.insert(newEntry, this.delegate);
                  this.delegate.prevInKeyInsertionOrder = null;
                  this.delegate.nextInKeyInsertionOrder = null;
                  expectedModCount = HashBiMap.this.modCount;
                  if (toRemove == this.delegate) {
                     toRemove = newEntry;
                  }

                  this.delegate = newEntry;
                  return oldValue;
               }
            }
         }
      };
   }

   public void forEach(BiConsumer<? super K, ? super V> action) {
      Preconditions.checkNotNull(action);

      for(BiEntry<K, V> entry = this.firstInKeyInsertionOrder; entry != null; entry = entry.nextInKeyInsertionOrder) {
         action.accept(entry.key, entry.value);
      }

   }

   public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
      Preconditions.checkNotNull(function);
      BiEntry<K, V> oldFirst = this.firstInKeyInsertionOrder;
      this.clear();

      for(BiEntry<K, V> entry = oldFirst; entry != null; entry = entry.nextInKeyInsertionOrder) {
         this.put(entry.key, function.apply(entry.key, entry.value));
      }

   }

   public BiMap<V, K> inverse() {
      return this.inverse == null ? (this.inverse = new Inverse()) : this.inverse;
   }

   private static final class BiEntry<K, V> extends ImmutableEntry<K, V> {
      final int keyHash;
      final int valueHash;
      @Nullable
      BiEntry<K, V> nextInKToVBucket;
      @Nullable
      BiEntry<K, V> nextInVToKBucket;
      @Nullable
      BiEntry<K, V> nextInKeyInsertionOrder;
      @Nullable
      BiEntry<K, V> prevInKeyInsertionOrder;

      BiEntry(K key, int keyHash, V value, int valueHash) {
         super(key, value);
         this.keyHash = keyHash;
         this.valueHash = valueHash;
      }
   }

   abstract class Itr<T> implements Iterator<T> {
      BiEntry<K, V> next;
      BiEntry<K, V> toRemove;
      int expectedModCount;

      Itr() {
         this.next = HashBiMap.this.firstInKeyInsertionOrder;
         this.toRemove = null;
         this.expectedModCount = HashBiMap.this.modCount;
      }

      public boolean hasNext() {
         if (HashBiMap.this.modCount != this.expectedModCount) {
            throw new ConcurrentModificationException();
         } else {
            return this.next != null;
         }
      }

      public T next() {
         if (!this.hasNext()) {
            throw new NoSuchElementException();
         } else {
            BiEntry<K, V> entry = this.next;
            this.next = entry.nextInKeyInsertionOrder;
            this.toRemove = entry;
            return (T)this.output(entry);
         }
      }

      public void remove() {
         if (HashBiMap.this.modCount != this.expectedModCount) {
            throw new ConcurrentModificationException();
         } else {
            CollectPreconditions.checkRemove(this.toRemove != null);
            HashBiMap.this.delete(this.toRemove);
            this.expectedModCount = HashBiMap.this.modCount;
            this.toRemove = null;
         }
      }

      abstract T output(BiEntry<K, V> var1);
   }

   private final class KeySet extends Maps.KeySet<K, V> {
      KeySet() {
         super(HashBiMap.this);
      }

      public Iterator<K> iterator() {
         return new Itr() {
            K output(BiEntry<K, V> entry) {
               return entry.key;
            }
         };
      }

      public boolean remove(@Nullable Object o) {
         BiEntry<K, V> entry = HashBiMap.this.seekByKey(o, Hashing.smearedHash(o));
         if (entry == null) {
            return false;
         } else {
            HashBiMap.this.delete(entry);
            entry.prevInKeyInsertionOrder = null;
            entry.nextInKeyInsertionOrder = null;
            return true;
         }
      }
   }

   private final class Inverse extends Maps.IteratorBasedAbstractMap<V, K> implements Serializable, BiMap<V, K> {
      private Inverse() {
      }

      BiMap<K, V> forward() {
         return HashBiMap.this;
      }

      public int size() {
         return HashBiMap.this.size;
      }

      public void clear() {
         this.forward().clear();
      }

      public boolean containsKey(@Nullable Object value) {
         return this.forward().containsValue(value);
      }

      public K get(@Nullable Object value) {
         return (K)Maps.keyOrNull(HashBiMap.this.seekByValue(value, Hashing.smearedHash(value)));
      }

      @CanIgnoreReturnValue
      public K put(@Nullable V value, @Nullable K key) {
         return (K)HashBiMap.this.putInverse(value, key, false);
      }

      public K forcePut(@Nullable V value, @Nullable K key) {
         return (K)HashBiMap.this.putInverse(value, key, true);
      }

      public K remove(@Nullable Object value) {
         BiEntry<K, V> entry = HashBiMap.this.seekByValue(value, Hashing.smearedHash(value));
         if (entry == null) {
            return null;
         } else {
            HashBiMap.this.delete(entry);
            entry.prevInKeyInsertionOrder = null;
            entry.nextInKeyInsertionOrder = null;
            return entry.key;
         }
      }

      public BiMap<K, V> inverse() {
         return this.forward();
      }

      public Set<V> keySet() {
         return new InverseKeySet();
      }

      public Set<K> values() {
         return this.forward().keySet();
      }

      Iterator<Map.Entry<V, K>> entryIterator() {
         return new Itr() {
            Map.Entry<V, K> output(BiEntry<K, V> entry) {
               return new null.InverseEntry(entry);
            }

            class InverseEntry extends AbstractMapEntry<V, K> {
               BiEntry<K, V> delegate;

               InverseEntry(BiEntry<K, V> entry) {
                  this.delegate = entry;
               }

               public V getKey() {
                  return this.delegate.value;
               }

               public K getValue() {
                  return this.delegate.key;
               }

               public K setValue(K key) {
                  K oldKey = this.delegate.key;
                  int keyHash = Hashing.smearedHash(key);
                  if (keyHash == this.delegate.keyHash && Objects.equal(key, oldKey)) {
                     return key;
                  } else {
                     Preconditions.checkArgument(HashBiMap.this.seekByKey(key, keyHash) == null, "value already present: %s", key);
                     HashBiMap.this.delete(this.delegate);
                     BiEntry<K, V> newEntry = new BiEntry<K, V>(key, keyHash, this.delegate.value, this.delegate.valueHash);
                     this.delegate = newEntry;
                     HashBiMap.this.insert(newEntry, (BiEntry)null);
                     expectedModCount = HashBiMap.this.modCount;
                     return oldKey;
                  }
               }
            }
         };
      }

      public void forEach(BiConsumer<? super V, ? super K> action) {
         Preconditions.checkNotNull(action);
         HashBiMap.this.forEach((k, v) -> action.accept(v, k));
      }

      public void replaceAll(BiFunction<? super V, ? super K, ? extends K> function) {
         Preconditions.checkNotNull(function);
         BiEntry<K, V> oldFirst = HashBiMap.this.firstInKeyInsertionOrder;
         this.clear();

         for(BiEntry<K, V> entry = oldFirst; entry != null; entry = entry.nextInKeyInsertionOrder) {
            this.put(entry.value, function.apply(entry.value, entry.key));
         }

      }

      private final class InverseKeySet extends Maps.KeySet<V, K> {
         InverseKeySet() {
            super(Inverse.this);
         }

         public boolean remove(@Nullable Object o) {
            BiEntry<K, V> entry = HashBiMap.this.seekByValue(o, Hashing.smearedHash(o));
            if (entry == null) {
               return false;
            } else {
               HashBiMap.this.delete(entry);
               return true;
            }
         }

         public Iterator<V> iterator() {
            return new Itr() {
               V output(BiEntry<K, V> entry) {
                  return entry.value;
               }
            };
         }
      }
   }
}
