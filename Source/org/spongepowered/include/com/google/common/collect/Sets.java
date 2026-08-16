package org.spongepowered.include.com.google.common.collect;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.base.Preconditions;

public final class Sets {
   public static <E> HashSet<E> newHashSet() {
      return new HashSet();
   }

   public static <E> HashSet<E> newHashSet(E... elements) {
      HashSet<E> set = newHashSetWithExpectedSize(elements.length);
      Collections.addAll(set, elements);
      return set;
   }

   public static <E> HashSet<E> newHashSetWithExpectedSize(int expectedSize) {
      return new HashSet(Maps.capacity(expectedSize));
   }

   static int hashCodeImpl(Set<?> s) {
      int hashCode = 0;

      for(Object o : s) {
         hashCode += o != null ? o.hashCode() : 0;
         hashCode = ~(~hashCode);
      }

      return hashCode;
   }

   static boolean equalsImpl(Set<?> s, @Nullable Object object) {
      if (s == object) {
         return true;
      } else if (object instanceof Set) {
         Set<?> o = (Set)object;

         try {
            return s.size() == o.size() && s.containsAll(o);
         } catch (NullPointerException var4) {
            return false;
         } catch (ClassCastException var5) {
            return false;
         }
      } else {
         return false;
      }
   }

   public static <E> NavigableSet<E> unmodifiableNavigableSet(NavigableSet<E> set) {
      return (NavigableSet<E>)(!(set instanceof ImmutableSortedSet) && !(set instanceof UnmodifiableNavigableSet) ? new UnmodifiableNavigableSet(set) : set);
   }

   static boolean removeAllImpl(Set<?> set, Iterator<?> iterator) {
      boolean changed;
      for(changed = false; iterator.hasNext(); changed |= set.remove(iterator.next())) {
      }

      return changed;
   }

   static boolean removeAllImpl(Set<?> set, Collection<?> collection) {
      Preconditions.checkNotNull(collection);
      if (collection instanceof Multiset) {
         collection = (collection).elementSet();
      }

      return collection instanceof Set && collection.size() > set.size() ? Iterators.removeAll(set.iterator(), collection) : removeAllImpl(set, collection.iterator());
   }

   abstract static class ImprovedAbstractSet<E> extends AbstractSet<E> {
      public boolean removeAll(Collection<?> c) {
         return Sets.removeAllImpl(this, c);
      }

      public boolean retainAll(Collection<?> c) {
         return super.retainAll((Collection)Preconditions.checkNotNull(c));
      }
   }

   static final class UnmodifiableNavigableSet<E> extends ForwardingSortedSet<E> implements Serializable, NavigableSet<E> {
      private final NavigableSet<E> delegate;
      private transient UnmodifiableNavigableSet<E> descendingSet;

      UnmodifiableNavigableSet(NavigableSet<E> delegate) {
         this.delegate = (NavigableSet)Preconditions.checkNotNull(delegate);
      }

      protected SortedSet<E> delegate() {
         return Collections.unmodifiableSortedSet(this.delegate);
      }

      public E lower(E e) {
         return (E)this.delegate.lower(e);
      }

      public E floor(E e) {
         return (E)this.delegate.floor(e);
      }

      public E ceiling(E e) {
         return (E)this.delegate.ceiling(e);
      }

      public E higher(E e) {
         return (E)this.delegate.higher(e);
      }

      public E pollFirst() {
         throw new UnsupportedOperationException();
      }

      public E pollLast() {
         throw new UnsupportedOperationException();
      }

      public NavigableSet<E> descendingSet() {
         UnmodifiableNavigableSet<E> result = this.descendingSet;
         if (result == null) {
            result = this.descendingSet = new UnmodifiableNavigableSet<E>(this.delegate.descendingSet());
            result.descendingSet = this;
         }

         return result;
      }

      public Iterator<E> descendingIterator() {
         return Iterators.<E>unmodifiableIterator(this.delegate.descendingIterator());
      }

      public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
         return Sets.<E>unmodifiableNavigableSet(this.delegate.subSet(fromElement, fromInclusive, toElement, toInclusive));
      }

      public NavigableSet<E> headSet(E toElement, boolean inclusive) {
         return Sets.<E>unmodifiableNavigableSet(this.delegate.headSet(toElement, inclusive));
      }

      public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
         return Sets.<E>unmodifiableNavigableSet(this.delegate.tailSet(fromElement, inclusive));
      }
   }
}
