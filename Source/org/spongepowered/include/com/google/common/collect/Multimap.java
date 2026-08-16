package org.spongepowered.include.com.google.common.collect;

import java.util.Collection;
import java.util.Map;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.spongepowered.include.com.google.errorprone.annotations.CompatibleWith;

public interface Multimap<K, V> {
   @CanIgnoreReturnValue
   boolean put(@Nullable K var1, @Nullable V var2);

   @CanIgnoreReturnValue
   Collection<V> removeAll(@Nullable @CompatibleWith("K") Object var1);

   Collection<V> get(@Nullable K var1);

   Map<K, Collection<V>> asMap();
}
