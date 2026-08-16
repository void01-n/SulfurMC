package org.spongepowered.include.com.google.common.collect;

import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.base.Function;
import org.spongepowered.include.com.google.common.base.Objects;
import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.common.base.Predicate;
import org.spongepowered.include.com.google.common.base.Predicates;
import org.spongepowered.include.com.google.errorprone.annotations.CanIgnoreReturnValue;

public final class Iterators {
   static final UnmodifiableListIterator<Object> EMPTY_LIST_ITERATOR = new UnmodifiableListIterator<Object>() {
      public boolean hasNext() {
         return false;
      }

      public Object next() {
         throw new NoSuchElementException();
      }

      public boolean hasPrevious() {
         return false;
      }

      public Object previous() {
         throw new NoSuchElementException();
      }

      public int nextIndex() {
         return 0;
      }

      public int previousIndex() {
         return -1;
      }
   };
   private static final Iterator<Object> EMPTY_MODIFIABLE_ITERATOR = new Iterator<Object>() {
      public boolean hasNext() {
         return false;
      }

      public Object next() {
         throw new NoSuchElementException();
      }

      public void remove() {
         CollectPreconditions.checkRemove(false);
      }
   };

   static <T> UnmodifiableIterator<T> emptyIterator() {
      return emptyListIterator();
   }

   static <T> UnmodifiableListIterator<T> emptyListIterator() {
      return EMPTY_LIST_ITERATOR;
   }

   public static <T> UnmodifiableIterator<T> unmodifiableIterator(final Iterator<? extends T> iterator) {
      Preconditions.checkNotNull(iterator);
      if (iterator instanceof UnmodifiableIterator) {
         UnmodifiableIterator<T> result = (UnmodifiableIterator)iterator;
         return result;
      } else {
         return new UnmodifiableIterator<T>() {
            public boolean hasNext() {
               return iterator.hasNext();
            }

            public T next() {
               return (T)iterator.next();
            }
         };
      }
   }

   public static boolean contains(Iterator<?> iterator, @Nullable Object element) {
      return any(iterator, Predicates.equalTo(element));
   }

   @CanIgnoreReturnValue
   public static boolean removeAll(Iterator<?> removeFrom, Collection<?> elementsToRemove) {
      return removeIf(removeFrom, Predicates.in(elementsToRemove));
   }

   @CanIgnoreReturnValue
   public static <T> boolean removeIf(Iterator<T> removeFrom, Predicate<? super T> predicate) {
      Preconditions.checkNotNull(predicate);
      boolean modified = false;

      while(removeFrom.hasNext()) {
         if (predicate.apply(removeFrom.next())) {
            removeFrom.remove();
            modified = true;
         }
      }

      return modified;
   }

   public static boolean elementsEqual(Iterator<?> iterator1, Iterator<?> iterator2) {
      while(true) {
         if (iterator1.hasNext()) {
            if (!iterator2.hasNext()) {
               return false;
            }

            Object o1 = iterator1.next();
            Object o2 = iterator2.next();
            if (Objects.equal(o1, o2)) {
               continue;
            }

            return false;
         }

         return !iterator2.hasNext();
      }
   }

   public static String toString(Iterator<?> iterator) {
      return Collections2.STANDARD_JOINER.appendTo((new StringBuilder()).append('['), iterator).append(']').toString();
   }

   public static <T> Iterator<T> concat(Iterator<? extends Iterator<? extends T>> inputs) {
      return new ConcatenatedIterator<T>(inputs);
   }

   public static <T> UnmodifiableIterator<T> filter(final Iterator<T> unfiltered, final Predicate<? super T> retainIfTrue) {
      Preconditions.checkNotNull(unfiltered);
      Preconditions.checkNotNull(retainIfTrue);
      return new AbstractIterator<T>() {
         protected T computeNext() {
            while(true) {
               if (unfiltered.hasNext()) {
                  T element = (T)unfiltered.next();
                  if (!retainIfTrue.apply(element)) {
                     continue;
                  }

                  return element;
               }

               return (T)this.endOfData();
            }
         }
      };
   }

   public static <T> boolean any(Iterator<T> iterator, Predicate<? super T> predicate) {
      return indexOf(iterator, predicate) != -1;
   }

   public static <T> int indexOf(Iterator<T> iterator, Predicate<? super T> predicate) {
      Preconditions.checkNotNull(predicate, "predicate");

      for(int i = 0; iterator.hasNext(); ++i) {
         T current = (T)iterator.next();
         if (predicate.apply(current)) {
            return i;
         }
      }

      return -1;
   }

   public static <F, T> Iterator<T> transform(Iterator<F> fromIterator, final Function<? super F, ? extends T> function) {
      Preconditions.checkNotNull(function);
      return new TransformedIterator<F, T>(fromIterator) {
         T transform(F from) {
            return function.apply(from);
         }
      };
   }

   @Nullable
   public static <T> T getNext(Iterator<? extends T> iterator, @Nullable T defaultValue) {
      return (T)(iterator.hasNext() ? iterator.next() : defaultValue);
   }

   @Nullable
   static <T> T pollNext(Iterator<T> iterator) {
      if (iterator.hasNext()) {
         T result = (T)iterator.next();
         iterator.remove();
         return result;
      } else {
         return null;
      }
   }

   static void clear(Iterator<?> iterator) {
      Preconditions.checkNotNull(iterator);

      while(iterator.hasNext()) {
         iterator.next();
         iterator.remove();
      }

   }

   @SafeVarargs
   public static <T> UnmodifiableIterator<T> forArray(T... array) {
      return forArray(array, 0, array.length, 0);
   }

   static <T> UnmodifiableListIterator<T> forArray(final T[] array, final int offset, int length, int index) {
      Preconditions.checkArgument(length >= 0);
      int end = offset + length;
      Preconditions.checkPositionIndexes(offset, end, array.length);
      Preconditions.checkPositionIndex(index, length);
      return (UnmodifiableListIterator<T>)(length == 0 ? emptyListIterator() : new AbstractIndexedListIterator<T>(length, index) {
         protected T get(int index) {
            return (T)array[offset + index];
         }
      });
   }

   public static <T> UnmodifiableIterator<T> singletonIterator(@Nullable final T value) {
      return new UnmodifiableIterator<T>() {
         boolean done;

         public boolean hasNext() {
            return !this.done;
         }

         public T next() {
            if (this.done) {
               throw new NoSuchElementException();
            } else {
               this.done = true;
               return value;
            }
         }
      };
   }

   static <T> ListIterator<T> cast(Iterator<T> iterator) {
      return (ListIterator)iterator;
   }

   private static class ConcatenatedIterator<T> extends MultitransformedIterator<Iterator<? extends T>, T> {
      public ConcatenatedIterator(Iterator<? extends Iterator<? extends T>> iterators) {
         super(getComponentIterators(iterators));
      }

      Iterator<? extends T> transform(Iterator<? extends T> iterator) {
         return iterator;
      }

      private static <T> Iterator<Iterator<? extends T>> getComponentIterators(Iterator<? extends Iterator<? extends T>> iterators) {
         return new MultitransformedIterator<Iterator<? extends T>, Iterator<? extends T>>(iterators) {
            Iterator<? extends Iterator<? extends T>> transform(Iterator<? extends T> iterator) {
               if (iterator instanceof ConcatenatedIterator) {
                  ConcatenatedIterator<? extends T> concatIterator = (ConcatenatedIterator)iterator;
                  return Iterators.ConcatenatedIterator.getComponentIterators(concatIterator.backingIterator);
               } else {
                  return Iterators.<Iterator<? extends T>>singletonIterator(iterator);
               }
            }
         };
      }
   }
}
