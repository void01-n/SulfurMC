package org.spongepowered.include.com.google.common.collect;

import javax.annotation.Nullable;

final class Hashing {
   static int smear(int hashCode) {
      return 461845907 * Integer.rotateLeft(hashCode * -862048943, 15);
   }

   static int smearedHash(@Nullable Object o) {
      return smear(o == null ? 0 : o.hashCode());
   }

   static int closedTableSize(int expectedEntries, double loadFactor) {
      expectedEntries = Math.max(expectedEntries, 2);
      int tableSize = Integer.highestOneBit(expectedEntries);
      if (expectedEntries > (int)(loadFactor * (double)tableSize)) {
         tableSize <<= 1;
         return tableSize > 0 ? tableSize : 1073741824;
      } else {
         return tableSize;
      }
   }

   static boolean needsResizing(int size, int tableSize, double loadFactor) {
      return (double)size > loadFactor * (double)tableSize && tableSize < 1073741824;
   }
}
