package org.spongepowered.include.com.google.common.collect;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Spliterator;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.base.Preconditions;

abstract class AbstractMapBasedMultimap<K, V> extends AbstractMultimap<K, V> implements Serializable {
   private transient Map<K, Collection<V>> map;
   private transient int totalSize;

   protected AbstractMapBasedMultimap(Map<K, Collection<V>> map) {
      Preconditions.checkArgument(map.isEmpty());
      this.map = map;
   }

   Collection<V> createUnmodifiableEmptyCollection() {
      return unmodifiableCollectionSubclass(this.createCollection());
   }

   abstract Collection<V> createCollection();

   Collection<V> createCollection(@Nullable K key) {
      return this.createCollection();
   }

   public boolean put(@Nullable K key, @Nullable V value) {
      Collection<V> collection = (Collection)this.map.get(key);
      if (collection == null) {
         collection = this.createCollection(key);
         if (collection.add(value)) {
            ++this.totalSize;
            this.map.put(key, collection);
            return true;
         } else {
            throw new AssertionError("New Collection violated the Collection spec");
         }
      } else if (collection.add(value)) {
         ++this.totalSize;
         return true;
      } else {
         return false;
      }
   }

   public Collection<V> removeAll(@Nullable Object key) {
      Collection<V> collection = (Collection)this.map.remove(key);
      if (collection == null) {
         return this.createUnmodifiableEmptyCollection();
      } else {
         Collection<V> output = this.createCollection();
         output.addAll(collection);
         this.totalSize -= collection.size();
         collection.clear();
         return unmodifiableCollectionSubclass(output);
      }
   }

   static <E> Collection<E> unmodifiableCollectionSubclass(Collection<E> collection) {
      if (collection instanceof NavigableSet) {
         return Sets.<E>unmodifiableNavigableSet((NavigableSet)collection);
      } else if (collection instanceof SortedSet) {
         return Collections.unmodifiableSortedSet((SortedSet)collection);
      } else if (collection instanceof Set) {
         return Collections.unmodifiableSet((Set)collection);
      } else {
         return (Collection<E>)(collection instanceof List ? Collections.unmodifiableList((List)collection) : Collections.unmodifiableCollection(collection));
      }
   }

   public void clear() {
      for(Collection<V> collection : this.map.values()) {
         collection.clear();
      }

      this.map.clear();
      this.totalSize = 0;
   }

   public Collection<V> get(@Nullable K key) {
      Collection<V> collection = (Collection)this.map.get(key);
      if (collection == null) {
         collection = this.createCollection(key);
      }

      return this.wrapCollection(key, collection);
   }

   Collection<V> wrapCollection(@Nullable K key, Collection<V> collection) {
      if (collection instanceof NavigableSet) {
         return new WrappedNavigableSet(key, (NavigableSet)collection, (WrappedCollection)null);
      } else if (collection instanceof SortedSet) {
         return new WrappedSortedSet(key, (SortedSet)collection, (WrappedCollection)null);
      } else if (collection instanceof Set) {
         return new WrappedSet(key, (Set)collection);
      } else {
         return (Collection<V>)(collection instanceof List ? this.wrapList(key, (List)collection, (WrappedCollection)null) : new WrappedCollection(key, collection, (WrappedCollection)null));
      }
   }

   private List<V> wrapList(@Nullable K key, List<V> list, @Nullable AbstractMapBasedMultimap<K, V>.WrappedCollection ancestor) {
      return (List<V>)(list instanceof RandomAccess ? new RandomAccessWrappedList(key, list, ancestor) : new WrappedList(key, list, ancestor));
   }

   private static <E> Iterator<E> iteratorOrListIterator(Collection<E> collection) {
      return (Iterator<E>)(collection instanceof List ? ((List)collection).listIterator() : collection.iterator());
   }

   Set<K> createKeySet() {
      if (this.map instanceof NavigableMap) {
         return new NavigableKeySet((NavigableMap)this.map);
      } else {
         return (Set<K>)(this.map instanceof SortedMap ? new SortedKeySet((SortedMap)this.map) : new KeySet(this.map));
      }
   }

   private void removeValuesForKey(Object key) {
      Collection<V> collection = (Collection)Maps.safeRemove(this.map, key);
      if (collection != null) {
         int count = collection.size();
         collection.clear();
         this.totalSize -= count;
      }

   }

   Map<K, Collection<V>> createAsMap() {
      if (this.map instanceof NavigableMap) {
         return new NavigableAsMap((NavigableMap)this.map);
      } else {
         return (Map<K, Collection<V>>)(this.map instanceof SortedMap ? new SortedAsMap((SortedMap)this.map) : new AsMap(this.map));
      }
   }

   private class WrappedCollection extends AbstractCollection<V> {
      final K key;
      Collection<V> delegate;
      final AbstractMapBasedMultimap<K, V>.WrappedCollection ancestor;
      final Collection<V> ancestorDelegate;

      WrappedCollection(@Nullable K key, Collection<V> delegate, @Nullable AbstractMapBasedMultimap<K, V>.WrappedCollection ancestor) {
         this.key = key;
         this.delegate = delegate;
         this.ancestor = ancestor;
         this.ancestorDelegate = ancestor == null ? null : ancestor.getDelegate();
      }

      void refreshIfEmpty() {
         if (this.ancestor != null) {
            this.ancestor.refreshIfEmpty();
            if (this.ancestor.getDelegate() != this.ancestorDelegate) {
               throw new ConcurrentModificationException();
            }
         } else if (this.delegate.isEmpty()) {
            Collection<V> newDelegate = (Collection)AbstractMapBasedMultimap.this.map.get(this.key);
            if (newDelegate != null) {
               this.delegate = newDelegate;
            }
         }

      }

      void removeIfEmpty() {
         if (this.ancestor != null) {
            this.ancestor.removeIfEmpty();
         } else if (this.delegate.isEmpty()) {
            AbstractMapBasedMultimap.this.map.remove(this.key);
         }

      }

      K getKey() {
         return this.key;
      }

      void addToMap() {
         if (this.ancestor != null) {
            this.ancestor.addToMap();
         } else {
            AbstractMapBasedMultimap.this.map.put(this.key, this.delegate);
         }

      }

      public int size() {
         this.refreshIfEmpty();
         return this.delegate.size();
      }

      public boolean equals(@Nullable Object object) {
         if (object == this) {
            return true;
         } else {
            this.refreshIfEmpty();
            return this.delegate.equals(object);
         }
      }

      public int hashCode() {
         this.refreshIfEmpty();
         return this.delegate.hashCode();
      }

      public String toString() {
         this.refreshIfEmpty();
         return this.delegate.toString();
      }

      Collection<V> getDelegate() {
         return this.delegate;
      }

      public Iterator<V> iterator() {
         this.refreshIfEmpty();
         return new WrappedIterator();
      }

      public Spliterator<V> spliterator() {
         this.refreshIfEmpty();
         return this.delegate.spliterator();
      }

      public boolean add(V value) {
         this.refreshIfEmpty();
         boolean wasEmpty = this.delegate.isEmpty();
         boolean changed = this.delegate.add(value);
         if (changed) {
            AbstractMapBasedMultimap.this.totalSize++;
            if (wasEmpty) {
               this.addToMap();
            }
         }

         return changed;
      }

      AbstractMapBasedMultimap<K, V>.WrappedCollection getAncestor() {
         return this.ancestor;
      }

      public boolean addAll(Collection<? extends V> collection) {
         if (collection.isEmpty()) {
            return false;
         } else {
            int oldSize = this.size();
            boolean changed = this.delegate.addAll(collection);
            if (changed) {
               int newSize = this.delegate.size();
               AbstractMapBasedMultimap.this.totalSize = AbstractMapBasedMultimap.this.totalSize + (newSize - oldSize);
               if (oldSize == 0) {
                  this.addToMap();
               }
            }

            return changed;
         }
      }

      public boolean contains(Object o) {
         this.refreshIfEmpty();
         return this.delegate.contains(o);
      }

      public boolean containsAll(Collection<?> c) {
         this.refreshIfEmpty();
         return this.delegate.containsAll(c);
      }

      public void clear() {
         int oldSize = this.size();
         if (oldSize != 0) {
            this.delegate.clear();
            AbstractMapBasedMultimap.this.totalSize = AbstractMapBasedMultimap.this.totalSize - oldSize;
            this.removeIfEmpty();
         }
      }

      public boolean remove(Object o) {
         this.refreshIfEmpty();
         boolean changed = this.delegate.remove(o);
         if (changed) {
            AbstractMapBasedMultimap.this.totalSize--;
            this.removeIfEmpty();
         }

         return changed;
      }

      public boolean removeAll(Collection<?> c) {
         if (c.isEmpty()) {
            return false;
         } else {
            int oldSize = this.size();
            boolean changed = this.delegate.removeAll(c);
            if (changed) {
               int newSize = this.delegate.size();
               AbstractMapBasedMultimap.this.totalSize = AbstractMapBasedMultimap.this.totalSize + (newSize - oldSize);
               this.removeIfEmpty();
            }

            return changed;
         }
      }

      public boolean retainAll(Collection<?> c) {
         Preconditions.checkNotNull(c);
         int oldSize = this.size();
         boolean changed = this.delegate.retainAll(c);
         if (changed) {
            int newSize = this.delegate.size();
            AbstractMapBasedMultimap.this.totalSize = AbstractMapBasedMultimap.this.totalSize + (newSize - oldSize);
            this.removeIfEmpty();
         }

         return changed;
      }

      class WrappedIterator implements Iterator<V> {
         final Iterator<V> delegateIterator;
         final Collection<V> originalDelegate;

         WrappedIterator() {
            this.originalDelegate = WrappedCollection.this.delegate;
            this.delegateIterator = AbstractMapBasedMultimap.<V>iteratorOrListIterator(WrappedCollection.this.delegate);
         }

         WrappedIterator(Iterator<V> delegateIterator) {
            this.originalDelegate = WrappedCollection.this.delegate;
            this.delegateIterator = delegateIterator;
         }

         void validateIterator() {
            WrappedCollection.this.refreshIfEmpty();
            if (WrappedCollection.this.delegate != this.originalDelegate) {
               throw new ConcurrentModificationException();
            }
         }

         public boolean hasNext() {
            this.validateIterator();
            return this.delegateIterator.hasNext();
         }

         public V next() {
            this.validateIterator();
            return (V)this.delegateIterator.next();
         }

         public void remove() {
            this.delegateIterator.remove();
            AbstractMapBasedMultimap.this.totalSize--;
            WrappedCollection.this.removeIfEmpty();
         }

         Iterator<V> getDelegateIterator() {
            this.validateIterator();
            return this.delegateIterator;
         }
      }
   }

   private class WrappedSet extends WrappedCollection implements Set {
      WrappedSet(@Nullable K key, Set<V> delegate) {
         super(key, delegate, (WrappedCollection)null);
      }

      public boolean removeAll(Collection<?> c) {
         if (c.isEmpty()) {
            return false;
         } else {
            int oldSize = this.size();
            boolean changed = Sets.removeAllImpl((Set)this.delegate, c);
            if (changed) {
               int newSize = this.delegate.size();
               AbstractMapBasedMultimap.this.totalSize = AbstractMapBasedMultimap.this.totalSize + (newSize - oldSize);
               this.removeIfEmpty();
            }

            return changed;
         }
      }
   }

   private class WrappedSortedSet extends WrappedCollection implements SortedSet {
      WrappedSortedSet(@Nullable K key, SortedSet<V> delegate, @Nullable AbstractMapBasedMultimap<K, V>.WrappedCollection ancestor) {
         super(key, delegate, ancestor);
      }

      SortedSet<V> getSortedSetDelegate() {
         return (SortedSet)this.getDelegate();
      }

      public Comparator<? super V> comparator() {
         return this.getSortedSetDelegate().comparator();
      }

      public V first() {
         this.refreshIfEmpty();
         return (V)this.getSortedSetDelegate().first();
      }

      public V last() {
         this.refreshIfEmpty();
         return (V)this.getSortedSetDelegate().last();
      }

      public SortedSet<V> headSet(V toElement) {
         this.refreshIfEmpty();
         return AbstractMapBasedMultimap.this.new WrappedSortedSet(this.getKey(), this.getSortedSetDelegate().headSet(toElement), (WrappedCollection)(this.getAncestor() == null ? this : this.getAncestor()));
      }

      public SortedSet<V> subSet(V fromElement, V toElement) {
         this.refreshIfEmpty();
         return AbstractMapBasedMultimap.this.new WrappedSortedSet(this.getKey(), this.getSortedSetDelegate().subSet(fromElement, toElement), (WrappedCollection)(this.getAncestor() == null ? this : this.getAncestor()));
      }

      public SortedSet<V> tailSet(V fromElement) {
         this.refreshIfEmpty();
         return AbstractMapBasedMultimap.this.new WrappedSortedSet(this.getKey(), this.getSortedSetDelegate().tailSet(fromElement), (WrappedCollection)(this.getAncestor() == null ? this : this.getAncestor()));
      }
   }

   class WrappedNavigableSet extends WrappedSortedSet implements NavigableSet {
      WrappedNavigableSet(@Nullable K key, NavigableSet<V> delegate, @Nullable AbstractMapBasedMultimap<K, V>.WrappedCollection ancestor) {
         super(key, delegate, ancestor);
      }

      NavigableSet<V> getSortedSetDelegate() {
         return (NavigableSet)super.getSortedSetDelegate();
      }

      public V lower(V v) {
         return (V)this.getSortedSetDelegate().lower(v);
      }

      public V floor(V v) {
         return (V)this.getSortedSetDelegate().floor(v);
      }

      public V ceiling(V v) {
         return (V)this.getSortedSetDelegate().ceiling(v);
      }

      public V higher(V v) {
         return (V)this.getSortedSetDelegate().higher(v);
      }

      public V pollFirst() {
         return (V)Iterators.pollNext(this.iterator());
      }

      public V pollLast() {
         return (V)Iterators.pollNext(this.descendingIterator());
      }

      private NavigableSet<V> wrap(NavigableSet<V> wrapped) {
         return AbstractMapBasedMultimap.this.new WrappedNavigableSet(this.key, wrapped, (WrappedCollection)(this.getAncestor() == null ? this : this.getAncestor()));
      }

      public NavigableSet<V> descendingSet() {
         return this.wrap(this.getSortedSetDelegate().descendingSet());
      }

      public Iterator<V> descendingIterator() {
         return new WrappedCollection.WrappedIterator(this.getSortedSetDelegate().descendingIterator());
      }

      public NavigableSet<V> subSet(V fromElement, boolean fromInclusive, V toElement, boolean toInclusive) {
         return this.wrap(this.getSortedSetDelegate().subSet(fromElement, fromInclusive, toElement, toInclusive));
      }

      public NavigableSet<V> headSet(V toElement, boolean inclusive) {
         return this.wrap(this.getSortedSetDelegate().headSet(toElement, inclusive));
      }

      public NavigableSet<V> tailSet(V fromElement, boolean inclusive) {
         return this.wrap(this.getSortedSetDelegate().tailSet(fromElement, inclusive));
      }
   }

   private class WrappedList extends WrappedCollection implements List {
      WrappedList(@Nullable K key, List<V> delegate, @Nullable AbstractMapBasedMultimap<K, V>.WrappedCollection ancestor) {
         super(key, delegate, ancestor);
      }

      List<V> getListDelegate() {
         return (List)this.getDelegate();
      }

      public boolean addAll(int index, Collection<? extends V> c) {
         if (c.isEmpty()) {
            return false;
         } else {
            int oldSize = this.size();
            boolean changed = this.getListDelegate().addAll(index, c);
            if (changed) {
               int newSize = this.getDelegate().size();
               AbstractMapBasedMultimap.this.totalSize = AbstractMapBasedMultimap.this.totalSize + (newSize - oldSize);
               if (oldSize == 0) {
                  this.addToMap();
               }
            }

            return changed;
         }
      }

      public V get(int index) {
         this.refreshIfEmpty();
         return (V)this.getListDelegate().get(index);
      }

      public V set(int index, V element) {
         this.refreshIfEmpty();
         return (V)this.getListDelegate().set(index, element);
      }

      public void add(int index, V element) {
         this.refreshIfEmpty();
         boolean wasEmpty = this.getDelegate().isEmpty();
         this.getListDelegate().add(index, element);
         AbstractMapBasedMultimap.this.totalSize++;
         if (wasEmpty) {
            this.addToMap();
         }

      }

      public V remove(int index) {
         this.refreshIfEmpty();
         V value = (V)this.getListDelegate().remove(index);
         AbstractMapBasedMultimap.this.totalSize--;
         this.removeIfEmpty();
         return value;
      }

      public int indexOf(Object o) {
         this.refreshIfEmpty();
         return this.getListDelegate().indexOf(o);
      }

      public int lastIndexOf(Object o) {
         this.refreshIfEmpty();
         return this.getListDelegate().lastIndexOf(o);
      }

      public ListIterator<V> listIterator() {
         this.refreshIfEmpty();
         return new WrappedListIterator();
      }

      public ListIterator<V> listIterator(int index) {
         this.refreshIfEmpty();
         return new WrappedListIterator(index);
      }

      public List<V> subList(int fromIndex, int toIndex) {
         this.refreshIfEmpty();
         return AbstractMapBasedMultimap.this.wrapList(this.getKey(), this.getListDelegate().subList(fromIndex, toIndex), (WrappedCollection)(this.getAncestor() == null ? this : this.getAncestor()));
      }

      private class WrappedListIterator extends WrappedCollection.WrappedIterator implements ListIterator {
         WrappedListIterator() {
         }

         public WrappedListIterator(int index) {
            super(WrappedList.this.getListDelegate().listIterator(index));
         }

         private ListIterator<V> getDelegateListIterator() {
            return (ListIterator)this.getDelegateIterator();
         }

         public boolean hasPrevious() {
            return this.getDelegateListIterator().hasPrevious();
         }

         public V previous() {
            return (V)this.getDelegateListIterator().previous();
         }

         public int nextIndex() {
            return this.getDelegateListIterator().nextIndex();
         }

         public int previousIndex() {
            return this.getDelegateListIterator().previousIndex();
         }

         public void set(V value) {
            this.getDelegateListIterator().set(value);
         }

         public void add(V value) {
            boolean wasEmpty = WrappedList.this.isEmpty();
            this.getDelegateListIterator().add(value);
            AbstractMapBasedMultimap.this.totalSize++;
            if (wasEmpty) {
               WrappedList.this.addToMap();
            }

         }
      }
   }

   private class RandomAccessWrappedList extends WrappedList implements RandomAccess {
      RandomAccessWrappedList(@Nullable K key, List<V> delegate, @Nullable AbstractMapBasedMultimap<K, V>.WrappedCollection ancestor) {
         super(key, delegate, ancestor);
      }
   }

   private class KeySet extends Maps.KeySet<K, Collection<V>> {
      KeySet(Map<K, Collection<V>> subMap) {
         super(subMap);
      }

      public Iterator<K> iterator() {
         final Iterator<Map.Entry<K, Collection<V>>> entryIterator = this.map().entrySet().iterator();
         return new Iterator<K>() {
            Map.Entry<K, Collection<V>> entry;

            public boolean hasNext() {
               return entryIterator.hasNext();
            }

            public K next() {
               this.entry = (Map.Entry)entryIterator.next();
               return (K)this.entry.getKey();
            }

            public void remove() {
               CollectPreconditions.checkRemove(this.entry != null);
               Collection<V> collection = (Collection)this.entry.getValue();
               entryIterator.remove();
               AbstractMapBasedMultimap.this.totalSize = AbstractMapBasedMultimap.this.totalSize - collection.size();
               collection.clear();
            }
         };
      }

      public Spliterator<K> spliterator() {
         return this.map().keySet().spliterator();
      }

      public boolean remove(Object key) {
         int count = 0;
         Collection<V> collection = (Collection)this.map().remove(key);
         if (collection != null) {
            count = collection.size();
            collection.clear();
            AbstractMapBasedMultimap.this.totalSize = AbstractMapBasedMultimap.this.totalSize - count;
         }

         return count > 0;
      }

      public void clear() {
         Iterators.clear(this.iterator());
      }

      public boolean containsAll(Collection<?> c) {
         return this.map().keySet().containsAll(c);
      }

      public boolean equals(@Nullable Object object) {
         return this == object || this.map().keySet().equals(object);
      }

      public int hashCode() {
         return this.map().keySet().hashCode();
      }
   }

   private class SortedKeySet extends KeySet implements SortedSet {
      SortedKeySet(SortedMap<K, Collection<V>> subMap) {
         super(subMap);
      }

      SortedMap<K, Collection<V>> sortedMap() {
         return (SortedMap)super.map();
      }

      public Comparator<? super K> comparator() {
         return this.sortedMap().comparator();
      }

      public K first() {
         return (K)this.sortedMap().firstKey();
      }

      public SortedSet<K> headSet(K toElement) {
         return AbstractMapBasedMultimap.this.new SortedKeySet(this.sortedMap().headMap(toElement));
      }

      public K last() {
         return (K)this.sortedMap().lastKey();
      }

      public SortedSet<K> subSet(K fromElement, K toElement) {
         return AbstractMapBasedMultimap.this.new SortedKeySet(this.sortedMap().subMap(fromElement, toElement));
      }

      public SortedSet<K> tailSet(K fromElement) {
         return AbstractMapBasedMultimap.this.new SortedKeySet(this.sortedMap().tailMap(fromElement));
      }
   }

   class NavigableKeySet extends SortedKeySet implements NavigableSet {
      NavigableKeySet(NavigableMap<K, Collection<V>> subMap) {
         super(subMap);
      }

      NavigableMap<K, Collection<V>> sortedMap() {
         return (NavigableMap)super.sortedMap();
      }

      public K lower(K k) {
         return (K)this.sortedMap().lowerKey(k);
      }

      public K floor(K k) {
         return (K)this.sortedMap().floorKey(k);
      }

      public K ceiling(K k) {
         return (K)this.sortedMap().ceilingKey(k);
      }

      public K higher(K k) {
         return (K)this.sortedMap().higherKey(k);
      }

      public K pollFirst() {
         return (K)Iterators.pollNext(this.iterator());
      }

      public K pollLast() {
         return (K)Iterators.pollNext(this.descendingIterator());
      }

      public NavigableSet<K> descendingSet() {
         return AbstractMapBasedMultimap.this.new NavigableKeySet(this.sortedMap().descendingMap());
      }

      public Iterator<K> descendingIterator() {
         return this.descendingSet().iterator();
      }

      public NavigableSet<K> headSet(K toElement) {
         return this.headSet(toElement, false);
      }

      public NavigableSet<K> headSet(K toElement, boolean inclusive) {
         return AbstractMapBasedMultimap.this.new NavigableKeySet(this.sortedMap().headMap(toElement, inclusive));
      }

      public NavigableSet<K> subSet(K fromElement, K toElement) {
         return this.subSet(fromElement, true, toElement, false);
      }

      public NavigableSet<K> subSet(K fromElement, boolean fromInclusive, K toElement, boolean toInclusive) {
         return AbstractMapBasedMultimap.this.new NavigableKeySet(this.sortedMap().subMap(fromElement, fromInclusive, toElement, toInclusive));
      }

      public NavigableSet<K> tailSet(K fromElement) {
         return this.tailSet(fromElement, true);
      }

      public NavigableSet<K> tailSet(K fromElement, boolean inclusive) {
         return AbstractMapBasedMultimap.this.new NavigableKeySet(this.sortedMap().tailMap(fromElement, inclusive));
      }
   }

   private class AsMap extends Maps.ViewCachingAbstractMap<K, Collection<V>> {
      final transient Map<K, Collection<V>> submap;

      AsMap(Map<K, Collection<V>> submap) {
         this.submap = submap;
      }

      protected Set<Map.Entry<K, Collection<V>>> createEntrySet() {
         return new AsMapEntries();
      }

      public boolean containsKey(Object key) {
         return Maps.safeContainsKey(this.submap, key);
      }

      public Collection<V> get(Object key) {
         Collection<V> collection = (Collection)Maps.safeGet(this.submap, key);
         return collection == null ? null : AbstractMapBasedMultimap.this.wrapCollection(key, collection);
      }

      public Set<K> keySet() {
         return AbstractMapBasedMultimap.this.keySet();
      }

      public int size() {
         return this.submap.size();
      }

      public Collection<V> remove(Object key) {
         Collection<V> collection = (Collection)this.submap.remove(key);
         if (collection == null) {
            return null;
         } else {
            Collection<V> output = AbstractMapBasedMultimap.this.createCollection();
            output.addAll(collection);
            AbstractMapBasedMultimap.this.totalSize = AbstractMapBasedMultimap.this.totalSize - collection.size();
            collection.clear();
            return output;
         }
      }

      public boolean equals(@Nullable Object object) {
         return this == object || this.submap.equals(object);
      }

      public int hashCode() {
         return this.submap.hashCode();
      }

      public String toString() {
         return this.submap.toString();
      }

      public void clear() {
         if (this.submap == AbstractMapBasedMultimap.this.map) {
            AbstractMapBasedMultimap.this.clear();
         } else {
            Iterators.clear(new AsMapIterator());
         }

      }

      Map.Entry<K, Collection<V>> wrapEntry(Map.Entry<K, Collection<V>> entry) {
         K key = (K)entry.getKey();
         return Maps.<K, Collection<V>>immutableEntry(key, AbstractMapBasedMultimap.this.wrapCollection(key, (Collection)entry.getValue()));
      }

      class AsMapEntries extends Maps.EntrySet<K, Collection<V>> {
         Map<K, Collection<V>> map() {
            return AsMap.this;
         }

         public Iterator<Map.Entry<K, Collection<V>>> iterator() {
            return AsMap.this.new AsMapIterator();
         }

         public Spliterator<Map.Entry<K, Collection<V>>> spliterator() {
            return CollectSpliterators.map(AsMap.this.submap.entrySet().spliterator(), AsMap.this::wrapEntry);
         }

         public boolean contains(Object o) {
            return Collections2.safeContains(AsMap.this.submap.entrySet(), o);
         }

         public boolean remove(Object o) {
            if (!this.contains(o)) {
               return false;
            } else {
               Map.Entry<?, ?> entry = (Map.Entry)o;
               AbstractMapBasedMultimap.this.removeValuesForKey(entry.getKey());
               return true;
            }
         }
      }

      class AsMapIterator implements Iterator<Map.Entry<K, Collection<V>>> {
         final Iterator<Map.Entry<K, Collection<V>>> delegateIterator;
         Collection<V> collection;

         AsMapIterator() {
            this.delegateIterator = AsMap.this.submap.entrySet().iterator();
         }

         public boolean hasNext() {
            return this.delegateIterator.hasNext();
         }

         public Map.Entry<K, Collection<V>> next() {
            Map.Entry<K, Collection<V>> entry = (Map.Entry)this.delegateIterator.next();
            this.collection = (Collection)entry.getValue();
            return AsMap.this.wrapEntry(entry);
         }

         public void remove() {
            this.delegateIterator.remove();
            AbstractMapBasedMultimap.this.totalSize = AbstractMapBasedMultimap.this.totalSize - this.collection.size();
            this.collection.clear();
         }
      }
   }

   private class SortedAsMap extends AsMap implements SortedMap {
      SortedSet<K> sortedKeySet;

      SortedAsMap(SortedMap<K, Collection<V>> submap) {
         super(submap);
      }

      SortedMap<K, Collection<V>> sortedMap() {
         return (SortedMap)this.submap;
      }

      public Comparator<? super K> comparator() {
         return this.sortedMap().comparator();
      }

      public K firstKey() {
         return (K)this.sortedMap().firstKey();
      }

      public K lastKey() {
         return (K)this.sortedMap().lastKey();
      }

      public SortedMap<K, Collection<V>> headMap(K toKey) {
         return AbstractMapBasedMultimap.this.new SortedAsMap(this.sortedMap().headMap(toKey));
      }

      public SortedMap<K, Collection<V>> subMap(K fromKey, K toKey) {
         return AbstractMapBasedMultimap.this.new SortedAsMap(this.sortedMap().subMap(fromKey, toKey));
      }

      public SortedMap<K, Collection<V>> tailMap(K fromKey) {
         return AbstractMapBasedMultimap.this.new SortedAsMap(this.sortedMap().tailMap(fromKey));
      }

      public SortedSet<K> keySet() {
         SortedSet<K> result = this.sortedKeySet;
         return result == null ? (this.sortedKeySet = this.createKeySet()) : result;
      }

      SortedSet<K> createKeySet() {
         return AbstractMapBasedMultimap.this.new SortedKeySet(this.sortedMap());
      }
   }

   class NavigableAsMap extends SortedAsMap implements NavigableMap {
      NavigableAsMap(NavigableMap<K, Collection<V>> submap) {
         super(submap);
      }

      NavigableMap<K, Collection<V>> sortedMap() {
         return (NavigableMap)super.sortedMap();
      }

      public Map.Entry<K, Collection<V>> lowerEntry(K key) {
         Map.Entry<K, Collection<V>> entry = this.sortedMap().lowerEntry(key);
         return entry == null ? null : this.wrapEntry(entry);
      }

      public K lowerKey(K key) {
         return (K)this.sortedMap().lowerKey(key);
      }

      public Map.Entry<K, Collection<V>> floorEntry(K key) {
         Map.Entry<K, Collection<V>> entry = this.sortedMap().floorEntry(key);
         return entry == null ? null : this.wrapEntry(entry);
      }

      public K floorKey(K key) {
         return (K)this.sortedMap().floorKey(key);
      }

      public Map.Entry<K, Collection<V>> ceilingEntry(K key) {
         Map.Entry<K, Collection<V>> entry = this.sortedMap().ceilingEntry(key);
         return entry == null ? null : this.wrapEntry(entry);
      }

      public K ceilingKey(K key) {
         return (K)this.sortedMap().ceilingKey(key);
      }

      public Map.Entry<K, Collection<V>> higherEntry(K key) {
         Map.Entry<K, Collection<V>> entry = this.sortedMap().higherEntry(key);
         return entry == null ? null : this.wrapEntry(entry);
      }

      public K higherKey(K key) {
         return (K)this.sortedMap().higherKey(key);
      }

      public Map.Entry<K, Collection<V>> firstEntry() {
         Map.Entry<K, Collection<V>> entry = this.sortedMap().firstEntry();
         return entry == null ? null : this.wrapEntry(entry);
      }

      public Map.Entry<K, Collection<V>> lastEntry() {
         Map.Entry<K, Collection<V>> entry = this.sortedMap().lastEntry();
         return entry == null ? null : this.wrapEntry(entry);
      }

      public Map.Entry<K, Collection<V>> pollFirstEntry() {
         return this.pollAsMapEntry(this.entrySet().iterator());
      }

      public Map.Entry<K, Collection<V>> pollLastEntry() {
         return this.pollAsMapEntry(this.descendingMap().entrySet().iterator());
      }

      Map.Entry<K, Collection<V>> pollAsMapEntry(Iterator<Map.Entry<K, Collection<V>>> entryIterator) {
         if (!entryIterator.hasNext()) {
            return null;
         } else {
            Map.Entry<K, Collection<V>> entry = (Map.Entry)entryIterator.next();
            Collection<V> output = AbstractMapBasedMultimap.this.createCollection();
            output.addAll((Collection)entry.getValue());
            entryIterator.remove();
            return Maps.<K, Collection<V>>immutableEntry(entry.getKey(), AbstractMapBasedMultimap.unmodifiableCollectionSubclass(output));
         }
      }

      public NavigableMap<K, Collection<V>> descendingMap() {
         return AbstractMapBasedMultimap.this.new NavigableAsMap(this.sortedMap().descendingMap());
      }

      public NavigableSet<K> keySet() {
         return (NavigableSet)super.keySet();
      }

      NavigableSet<K> createKeySet() {
         return AbstractMapBasedMultimap.this.new NavigableKeySet(this.sortedMap());
      }

      public NavigableSet<K> navigableKeySet() {
         return this.keySet();
      }

      public NavigableSet<K> descendingKeySet() {
         return this.descendingMap().navigableKeySet();
      }

      public NavigableMap<K, Collection<V>> subMap(K fromKey, K toKey) {
         return this.subMap(fromKey, true, toKey, false);
      }

      public NavigableMap<K, Collection<V>> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
         return AbstractMapBasedMultimap.this.new NavigableAsMap(this.sortedMap().subMap(fromKey, fromInclusive, toKey, toInclusive));
      }

      public NavigableMap<K, Collection<V>> headMap(K toKey) {
         return this.headMap(toKey, false);
      }

      public NavigableMap<K, Collection<V>> headMap(K toKey, boolean inclusive) {
         return AbstractMapBasedMultimap.this.new NavigableAsMap(this.sortedMap().headMap(toKey, inclusive));
      }

      public NavigableMap<K, Collection<V>> tailMap(K fromKey) {
         return this.tailMap(fromKey, true);
      }

      public NavigableMap<K, Collection<V>> tailMap(K fromKey, boolean inclusive) {
         return AbstractMapBasedMultimap.this.new NavigableAsMap(this.sortedMap().tailMap(fromKey, inclusive));
      }
   }
}
