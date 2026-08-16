package org.spongepowered.include.com.google.common.collect;

import [Ljava.lang.Object;;
import java.util.Arrays;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.spongepowered.include.com.google.errorprone.annotations.concurrent.LazyInit;
import org.spongepowered.include.com.google.j2objc.annotations.RetainedWith;

public abstract class ImmutableSet<E> extends ImmutableCollection<E> implements Set<E> {
   @LazyInit
   @RetainedWith
   private transient ImmutableList<E> asList;

   public static <E> ImmutableSet<E> of() {
      return RegularImmutableSet.EMPTY;
   }

   public static <E> ImmutableSet<E> of(E element) {
      return new SingletonImmutableSet<E>(element);
   }

   public static <E> ImmutableSet<E> of(E e1, E e2) {
      return construct(2, e1, e2);
   }

   public static <E> ImmutableSet<E> of(E e1, E e2, E e3, E e4) {
      return construct(4, e1, e2, e3, e4);
   }

   @SafeVarargs
   public static <E> ImmutableSet<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E... others) {
      int paramCount = 6;
      Object[] elements = new Object[6 + others.length];
      elements[0] = e1;
      elements[1] = e2;
      elements[2] = e3;
      elements[3] = e4;
      elements[4] = e5;
      elements[5] = e6;
      System.arraycopy(others, 0, elements, 6, others.length);
      return construct(elements.length, elements);
   }

   private static <E> ImmutableSet<E> construct(int n, Object... elements) {
      switch (n) {
         case 0:
            return of();
         case 1:
            E elem = (E)elements[0];
            return of(elem);
         default:
            int tableSize = chooseTableSize(n);
            Object[] table = new Object[tableSize];
            int mask = tableSize - 1;
            int hashCode = 0;
            int uniques = 0;
            int i = 0;

            for(; i < n; ++i) {
               Object element = ObjectArrays.checkElementNotNull(elements[i], i);
               int hash = element.hashCode();
               int j = Hashing.smear(hash);

               while(true) {
                  int index = j & mask;
                  Object value = table[index];
                  if (value == null) {
                     elements[uniques++] = element;
                     table[index] = element;
                     hashCode += hash;
                     break;
                  }

                  if (value.equals(element)) {
                     break;
                  }

                  ++j;
               }
            }

            Arrays.fill(elements, uniques, n, (Object)null);
            if (uniques == 1) {
               E element = (E)elements[0];
               return new SingletonImmutableSet<E>(element, hashCode);
            } else if (tableSize != chooseTableSize(uniques)) {
               return construct(uniques, elements);
            } else {
               Object[] uniqueElements = uniques < elements.length ? Arrays.copyOf(elements, uniques) : elements;
               return new RegularImmutableSet<E>(uniqueElements, hashCode, table, mask);
            }
      }
   }

   static int chooseTableSize(int setSize) {
      if (setSize >= 751619276) {
         Preconditions.checkArgument(setSize < 1073741824, "collection too large");
         return 1073741824;
      } else {
         int tableSize;
         for(tableSize = Integer.highestOneBit(setSize - 1) << 1; (double)tableSize * 0.7 < (double)setSize; tableSize <<= 1) {
         }

         return tableSize;
      }
   }

   public static <E> ImmutableSet<E> copyOf(E[] elements) {
      switch (elements.length) {
         case 0:
            return of();
         case 1:
            return of(elements[0]);
         default:
            return construct(elements.length, ((Object;)elements).clone());
      }
   }

   ImmutableSet() {
   }

   boolean isHashCodeFast() {
      return false;
   }

   public boolean equals(@Nullable Object object) {
      if (object == this) {
         return true;
      } else {
         return object instanceof ImmutableSet && this.isHashCodeFast() && ((ImmutableSet)object).isHashCodeFast() && this.hashCode() != object.hashCode() ? false : Sets.equalsImpl(this, object);
      }
   }

   public int hashCode() {
      return Sets.hashCodeImpl(this);
   }

   public abstract UnmodifiableIterator<E> iterator();

   public ImmutableList<E> asList() {
      ImmutableList<E> result = this.asList;
      return result == null ? (this.asList = this.createAsList()) : result;
   }

   ImmutableList<E> createAsList() {
      return new RegularImmutableAsList<E>(this, this.toArray());
   }

   public static <E> Builder<E> builder() {
      return new Builder<E>();
   }

   abstract static class Indexed<E> extends ImmutableSet<E> {
      abstract E get(int var1);

      public UnmodifiableIterator<E> iterator() {
         return this.asList().iterator();
      }

      public Spliterator<E> spliterator() {
         return CollectSpliterators.<E>indexed(this.size(), 1297, this::get);
      }

      public void forEach(Consumer<? super E> consumer) {
         Preconditions.checkNotNull(consumer);
         int n = this.size();

         for(int i = 0; i < n; ++i) {
            consumer.accept(this.get(i));
         }

      }

      ImmutableList<E> createAsList() {
         return new ImmutableAsList<E>() {
            public E get(int index) {
               return (E)Indexed.this.get(index);
            }

            Indexed<E> delegateCollection() {
               return Indexed.this;
            }
         };
      }
   }

   public static class Builder<E> extends ImmutableCollection.ArrayBasedBuilder<E> {
      public Builder() {
         this(4);
      }

      Builder(int capacity) {
         super(capacity);
      }

      @CanIgnoreReturnValue
      public Builder<E> add(E element) {
         super.add(element);
         return this;
      }

      @CanIgnoreReturnValue
      public Builder<E> add(E... elements) {
         super.add(elements);
         return this;
      }

      @CanIgnoreReturnValue
      public Builder<E> addAll(Iterable<? extends E> elements) {
         super.addAll(elements);
         return this;
      }

      public ImmutableSet<E> build() {
         ImmutableSet<E> result = ImmutableSet.<E>construct(this.size, this.contents);
         this.size = result.size();
         return result;
      }
   }
}
