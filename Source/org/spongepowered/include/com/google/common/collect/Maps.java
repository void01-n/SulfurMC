package org.spongepowered.include.com.google.common.collect;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeMap;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.base.Function;
import org.spongepowered.include.com.google.common.base.Joiner;
import org.spongepowered.include.com.google.common.base.Objects;
import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.j2objc.annotations.Weak;

public final class Maps {
   static final Joiner.MapJoiner STANDARD_JOINER;

   static <K> Function<Map.Entry<K, ?>, K> keyFunction() {
      return Maps.EntryFunction.KEY;
   }

   static <V> Function<Map.Entry<?, V>, V> valueFunction() {
      return Maps.EntryFunction.VALUE;
   }

   static <K, V> Iterator<K> keyIterator(Iterator<Map.Entry<K, V>> entryIterator) {
      return Iterators.transform(entryIterator, keyFunction());
   }

   static <K, V> Iterator<V> valueIterator(Iterator<Map.Entry<K, V>> entryIterator) {
      return Iterators.transform(entryIterator, valueFunction());
   }

   static int capacity(int expectedSize) {
      if (expectedSize < 3) {
         CollectPreconditions.checkNonnegative(expectedSize, "expectedSize");
         return expectedSize + 1;
      } else {
         return expectedSize < 1073741824 ? (int)((float)expectedSize / 0.75F + 1.0F) : Integer.MAX_VALUE;
      }
   }

   public static <K extends Comparable, V> TreeMap<K, V> newTreeMap() {
      return new TreeMap();
   }

   public static <K, V> Map.Entry<K, V> immutableEntry(@Nullable K key, @Nullable V value) {
      return new ImmutableEntry<K, V>(key, value);
   }

   static <V> V safeGet(Map<?, V> map, @Nullable Object key) {
      Preconditions.checkNotNull(map);

      try {
         return (V)map.get(key);
      } catch (ClassCastException var3) {
         return null;
      } catch (NullPointerException var4) {
         return null;
      }
   }

   static boolean safeContainsKey(Map<?, ?> map, Object key) {
      Preconditions.checkNotNull(map);

      try {
         return map.containsKey(key);
      } catch (ClassCastException var3) {
         return false;
      } catch (NullPointerException var4) {
         return false;
      }
   }

   static <V> V safeRemove(Map<?, V> map, Object key) {
      Preconditions.checkNotNull(map);

      try {
         return (V)map.remove(key);
      } catch (ClassCastException var3) {
         return null;
      } catch (NullPointerException var4) {
         return null;
      }
   }

   static boolean equalsImpl(Map<?, ?> map, Object object) {
      if (map == object) {
         return true;
      } else if (object instanceof Map) {
         Map<?, ?> o = (Map)object;
         return map.entrySet().equals(o.entrySet());
      } else {
         return false;
      }
   }

   static String toStringImpl(Map<?, ?> map) {
      StringBuilder sb = Collections2.newStringBuilderForCollection(map.size()).append('{');
      STANDARD_JOINER.appendTo(sb, map);
      return sb.append('}').toString();
   }

   @Nullable
   static <K> K keyOrNull(@Nullable Map.Entry<K, ?> entry) {
      return (K)(entry == null ? null : entry.getKey());
   }

   @Nullable
   static <V> V valueOrNull(@Nullable Map.Entry<?, V> entry) {
      return (V)(entry == null ? null : entry.getValue());
   }

   static {
      STANDARD_JOINER = Collections2.STANDARD_JOINER.withKeyValueSeparator("=");
   }

   private static enum EntryFunction implements Function<Map.Entry<?, ?>, Object> {
      KEY {
         @Nullable
         public Object apply(Map.Entry<?, ?> entry) {
            return entry.getKey();
         }
      },
      VALUE {
         @Nullable
         public Object apply(Map.Entry<?, ?> entry) {
            return entry.getValue();
         }
      };

      private EntryFunction() {
      }
   }

   abstract static class ViewCachingAbstractMap<K, V> extends AbstractMap<K, V> {
      private transient Set<Map.Entry<K, V>> entrySet;
      private transient Set<K> keySet;
      private transient Collection<V> values;

      abstract Set<Map.Entry<K, V>> createEntrySet();

      public Set<Map.Entry<K, V>> entrySet() {
         Set<Map.Entry<K, V>> result = this.entrySet;
         return result == null ? (this.entrySet = this.createEntrySet()) : result;
      }

      public Set<K> keySet() {
         Set<K> result = this.keySet;
         return result == null ? (this.keySet = this.createKeySet()) : result;
      }

      Set<K> createKeySet() {
         return new KeySet(this);
      }

      public Collection<V> values() {
         Collection<V> result = this.values;
         return result == null ? (this.values = this.createValues()) : result;
      }

      Collection<V> createValues() {
         return new Values(this);
      }
   }

   abstract static class IteratorBasedAbstractMap<K, V> extends AbstractMap<K, V> {
      public abstract int size();

      abstract Iterator<Map.Entry<K, V>> entryIterator();

      Spliterator<Map.Entry<K, V>> entrySpliterator() {
         return Spliterators.spliterator(this.entryIterator(), (long)this.size(), 65);
      }

      public Set<Map.Entry<K, V>> entrySet() {
         return new EntrySet<K, V>() {
            Map<K, V> map() {
               return IteratorBasedAbstractMap.this;
            }

            public Iterator<Map.Entry<K, V>> iterator() {
               return IteratorBasedAbstractMap.this.entryIterator();
            }

            public Spliterator<Map.Entry<K, V>> spliterator() {
               return IteratorBasedAbstractMap.this.entrySpliterator();
            }

            public void forEach(Consumer<? super Map.Entry<K, V>> action) {
               IteratorBasedAbstractMap.this.forEachEntry(action);
            }
         };
      }

      void forEachEntry(Consumer<? super Map.Entry<K, V>> action) {
         this.entryIterator().forEachRemaining(action);
      }

      public void clear() {
         Iterators.clear(this.entryIterator());
      }
   }

   static class KeySet<K, V> extends Sets.ImprovedAbstractSet<K> {
      @Weak
      final Map<K, V> map;

      KeySet(Map<K, V> map) {
         this.map = (Map)Preconditions.checkNotNull(map);
      }

      Map<K, V> map() {
         return this.map;
      }

      public Iterator<K> iterator() {
         return Maps.keyIterator(this.map().entrySet().iterator());
      }

      public void forEach(Consumer<? super K> action) {
         Preconditions.checkNotNull(action);
         this.map.forEach((k, v) -> action.accept(k));
      }

      public int size() {
         return this.map().size();
      }

      public boolean isEmpty() {
         return this.map().isEmpty();
      }

      public boolean contains(Object o) {
         return this.map().containsKey(o);
      }

      public boolean remove(Object o) {
         if (this.contains(o)) {
            this.map().remove(o);
            return true;
         } else {
            return false;
         }
      }

      public void clear() {
         this.map().clear();
      }
   }

   static class Values<K, V> extends AbstractCollection<V> {
      @Weak
      final Map<K, V> map;

      Values(Map<K, V> map) {
         this.map = (Map)Preconditions.checkNotNull(map);
      }

      final Map<K, V> map() {
         return this.map;
      }

      public Iterator<V> iterator() {
         return Maps.valueIterator(this.map().entrySet().iterator());
      }

      public void forEach(Consumer<? super V> action) {
         Preconditions.checkNotNull(action);
         this.map.forEach((k, v) -> action.accept(v));
      }

      public boolean remove(Object o) {
         try {
            return super.remove(o);
         } catch (UnsupportedOperationException var5) {
            for(Map.Entry<K, V> entry : this.map().entrySet()) {
               if (Objects.equal(o, entry.getValue())) {
                  this.map().remove(entry.getKey());
                  return true;
               }
            }

            return false;
         }
      }

      public boolean removeAll(Collection<?> c) {
         try {
            return super.removeAll((Collection)Preconditions.checkNotNull(c));
         } catch (UnsupportedOperationException var6) {
            Set<K> toRemove = Sets.<K>newHashSet();

            for(Map.Entry<K, V> entry : this.map().entrySet()) {
               if (c.contains(entry.getValue())) {
                  toRemove.add(entry.getKey());
               }
            }

            return this.map().keySet().removeAll(toRemove);
         }
      }

      public boolean retainAll(Collection<?> c) {
         try {
            return super.retainAll((Collection)Preconditions.checkNotNull(c));
         } catch (UnsupportedOperationException var6) {
            Set<K> toRetain = Sets.<K>newHashSet();

            for(Map.Entry<K, V> entry : this.map().entrySet()) {
               if (c.contains(entry.getValue())) {
                  toRetain.add(entry.getKey());
               }
            }

            return this.map().keySet().retainAll(toRetain);
         }
      }

      public int size() {
         return this.map().size();
      }

      public boolean isEmpty() {
         return this.map().isEmpty();
      }

      public boolean contains(@Nullable Object o) {
         return this.map().containsValue(o);
      }

      public void clear() {
         this.map().clear();
      }
   }

   abstract static class EntrySet<K, V> extends Sets.ImprovedAbstractSet<Map.Entry<K, V>> {
      abstract Map<K, V> map();

      public int size() {
         return this.map().size();
      }

      public void clear() {
         this.map().clear();
      }

      public boolean contains(Object o) {
         if (!(o instanceof Map.Entry)) {
            return false;
         } else {
            Map.Entry<?, ?> entry = (Map.Entry)o;
            Object key = entry.getKey();
            V value = (V)Maps.safeGet(this.map(), key);
            return Objects.equal(value, entry.getValue()) && (value != null || this.map().containsKey(key));
         }
      }

      public boolean isEmpty() {
         return this.map().isEmpty();
      }

      public boolean remove(Object o) {
         if (this.contains(o)) {
            Map.Entry<?, ?> entry = (Map.Entry)o;
            return this.map().keySet().remove(entry.getKey());
         } else {
            return false;
         }
      }

      public boolean removeAll(Collection<?> c) {
         try {
            return super.removeAll((Collection)Preconditions.checkNotNull(c));
         } catch (UnsupportedOperationException var3) {
            return Sets.removeAllImpl(this, c.iterator());
         }
      }

      public boolean retainAll(Collection<?> c) {
         try {
            return super.retainAll((Collection)Preconditions.checkNotNull(c));
         } catch (UnsupportedOperationException var7) {
            Set<Object> keys = Sets.<Object>newHashSetWithExpectedSize(c.size());

            for(Object o : c) {
               if (this.contains(o)) {
                  Map.Entry<?, ?> entry = (Map.Entry)o;
                  keys.add(entry.getKey());
               }
            }

            return this.map().keySet().retainAll(keys);
         }
      }
   }
}
