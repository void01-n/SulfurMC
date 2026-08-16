package org.spongepowered.include.com.google.common.collect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class ArrayListMultimap<K, V> extends AbstractListMultimap<K, V> {
   transient int expectedValuesPerKey = 3;

   public static <K, V> ArrayListMultimap<K, V> create() {
      return new ArrayListMultimap<K, V>();
   }

   private ArrayListMultimap() {
      super(new HashMap());
   }

   List<V> createCollection() {
      return new ArrayList(this.expectedValuesPerKey);
   }
}
