package org.spongepowered.include.com.google.common.collect;

import java.io.Serializable;
import java.util.Comparator;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.base.Preconditions;

final class ComparatorOrdering<T> extends Ordering<T> implements Serializable {
   final Comparator<T> comparator;

   ComparatorOrdering(Comparator<T> comparator) {
      this.comparator = (Comparator)Preconditions.checkNotNull(comparator);
   }

   public int compare(T a, T b) {
      return this.comparator.compare(a, b);
   }

   public boolean equals(@Nullable Object object) {
      if (object == this) {
         return true;
      } else if (object instanceof ComparatorOrdering) {
         ComparatorOrdering<?> that = (ComparatorOrdering)object;
         return this.comparator.equals(that.comparator);
      } else {
         return false;
      }
   }

   public int hashCode() {
      return this.comparator.hashCode();
   }

   public String toString() {
      return this.comparator.toString();
   }
}
