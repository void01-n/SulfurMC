package org.spongepowered.include.com.google.common.collect;

import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.errorprone.annotations.CanIgnoreReturnValue;

final class CollectPreconditions {
   static void checkEntryNotNull(Object key, Object value) {
      if (key == null) {
         throw new NullPointerException("null key in entry: null=" + value);
      } else if (value == null) {
         throw new NullPointerException("null value in entry: " + key + "=null");
      }
   }

   @CanIgnoreReturnValue
   static int checkNonnegative(int value, String name) {
      if (value < 0) {
         throw new IllegalArgumentException(name + " cannot be negative but was: " + value);
      } else {
         return value;
      }
   }

   static void checkRemove(boolean canRemove) {
      Preconditions.checkState(canRemove, "no calls to next() since the last call to remove()");
   }
}
