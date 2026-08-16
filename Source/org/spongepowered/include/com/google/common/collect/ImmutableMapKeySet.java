package org.spongepowered.include.com.google.common.collect;

import java.util.Map;
import java.util.Spliterator;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.j2objc.annotations.Weak;

final class ImmutableMapKeySet<K, V> extends ImmutableSet.Indexed<K> {
   @Weak
   private final ImmutableMap<K, V> map;

   ImmutableMapKeySet(ImmutableMap<K, V> map) {
      this.map = map;
   }

   public int size() {
      return this.map.size();
   }

   public UnmodifiableIterator<K> iterator() {
      return this.map.keyIterator();
   }

   public Spliterator<K> spliterator() {
      return this.map.keySpliterator();
   }

   public boolean contains(@Nullable Object object) {
      return this.map.containsKey(object);
   }

   K get(int index) {
      return (K)((Map.Entry)this.map.entrySet().asList().get(index)).getKey();
   }

   public void forEach(Consumer<? super K> action) {
      Preconditions.checkNotNull(action);
      this.map.forEach((k, v) -> action.accept(k));
   }
}
