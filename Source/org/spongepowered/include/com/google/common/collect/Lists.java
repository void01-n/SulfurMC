package org.spongepowered.include.com.google.common.collect;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.AbstractSequentialList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.base.Function;
import org.spongepowered.include.com.google.common.base.Objects;
import org.spongepowered.include.com.google.common.base.Preconditions;

public final class Lists {
   public static <E> ArrayList<E> newArrayList() {
      return new ArrayList();
   }

   public static <F, T> List<T> transform(List<F> fromList, Function<? super F, ? extends T> function) {
      return (List<T>)(fromList instanceof RandomAccess ? new TransformingRandomAccessList(fromList, function) : new TransformingSequentialList(fromList, function));
   }

   static boolean equalsImpl(List<?> thisList, @Nullable Object other) {
      if (other == Preconditions.checkNotNull(thisList)) {
         return true;
      } else if (!(other instanceof List)) {
         return false;
      } else {
         List<?> otherList = (List)other;
         int size = thisList.size();
         if (size != otherList.size()) {
            return false;
         } else if (thisList instanceof RandomAccess && otherList instanceof RandomAccess) {
            for(int i = 0; i < size; ++i) {
               if (!Objects.equal(thisList.get(i), otherList.get(i))) {
                  return false;
               }
            }

            return true;
         } else {
            return Iterators.elementsEqual(thisList.iterator(), otherList.iterator());
         }
      }
   }

   static int indexOfImpl(List<?> list, @Nullable Object element) {
      if (list instanceof RandomAccess) {
         return indexOfRandomAccess(list, element);
      } else {
         ListIterator<?> listIterator = list.listIterator();

         while(listIterator.hasNext()) {
            if (Objects.equal(element, listIterator.next())) {
               return listIterator.previousIndex();
            }
         }

         return -1;
      }
   }

   private static int indexOfRandomAccess(List<?> list, @Nullable Object element) {
      int size = list.size();
      if (element == null) {
         for(int i = 0; i < size; ++i) {
            if (list.get(i) == null) {
               return i;
            }
         }
      } else {
         for(int i = 0; i < size; ++i) {
            if (element.equals(list.get(i))) {
               return i;
            }
         }
      }

      return -1;
   }

   static int lastIndexOfImpl(List<?> list, @Nullable Object element) {
      if (list instanceof RandomAccess) {
         return lastIndexOfRandomAccess(list, element);
      } else {
         ListIterator<?> listIterator = list.listIterator(list.size());

         while(listIterator.hasPrevious()) {
            if (Objects.equal(element, listIterator.previous())) {
               return listIterator.nextIndex();
            }
         }

         return -1;
      }
   }

   private static int lastIndexOfRandomAccess(List<?> list, @Nullable Object element) {
      if (element == null) {
         for(int i = list.size() - 1; i >= 0; --i) {
            if (list.get(i) == null) {
               return i;
            }
         }
      } else {
         for(int i = list.size() - 1; i >= 0; --i) {
            if (element.equals(list.get(i))) {
               return i;
            }
         }
      }

      return -1;
   }

   private static class TransformingSequentialList<F, T> extends AbstractSequentialList<T> implements Serializable {
      final List<F> fromList;
      final Function<? super F, ? extends T> function;

      TransformingSequentialList(List<F> fromList, Function<? super F, ? extends T> function) {
         this.fromList = (List)Preconditions.checkNotNull(fromList);
         this.function = (Function)Preconditions.checkNotNull(function);
      }

      public void clear() {
         this.fromList.clear();
      }

      public int size() {
         return this.fromList.size();
      }

      public ListIterator<T> listIterator(int index) {
         return new TransformedListIterator<F, T>(this.fromList.listIterator(index)) {
            T transform(F from) {
               return TransformingSequentialList.this.function.apply(from);
            }
         };
      }

      public boolean removeIf(Predicate<? super T> filter) {
         Preconditions.checkNotNull(filter);
         return this.fromList.removeIf((element) -> filter.test(this.function.apply(element)));
      }
   }

   private static class TransformingRandomAccessList<F, T> extends AbstractList<T> implements Serializable, RandomAccess {
      final List<F> fromList;
      final Function<? super F, ? extends T> function;

      TransformingRandomAccessList(List<F> fromList, Function<? super F, ? extends T> function) {
         this.fromList = (List)Preconditions.checkNotNull(fromList);
         this.function = (Function)Preconditions.checkNotNull(function);
      }

      public void clear() {
         this.fromList.clear();
      }

      public T get(int index) {
         return this.function.apply(this.fromList.get(index));
      }

      public Iterator<T> iterator() {
         return this.listIterator();
      }

      public ListIterator<T> listIterator(int index) {
         return new TransformedListIterator<F, T>(this.fromList.listIterator(index)) {
            T transform(F from) {
               return TransformingRandomAccessList.this.function.apply(from);
            }
         };
      }

      public boolean isEmpty() {
         return this.fromList.isEmpty();
      }

      public boolean removeIf(Predicate<? super T> filter) {
         Preconditions.checkNotNull(filter);
         return this.fromList.removeIf((element) -> filter.test(this.function.apply(element)));
      }

      public T remove(int index) {
         return this.function.apply(this.fromList.remove(index));
      }

      public int size() {
         return this.fromList.size();
      }
   }
}
