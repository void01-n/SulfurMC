package org.spongepowered.include.com.google.common.collect;

import javax.annotation.Nullable;

public final class Multimaps {
   static boolean equalsImpl(Multimap<?, ?> multimap, @Nullable Object object) {
      if (object == multimap) {
         return true;
      } else if (object instanceof Multimap) {
         Multimap<?, ?> that = (Multimap)object;
         return multimap.asMap().equals(that.asMap());
      } else {
         return false;
      }
   }
}
