package org.spongepowered.include.com.google.common.collect;

import java.util.List;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.errorprone.annotations.CanIgnoreReturnValue;

public interface ListMultimap<K, V> extends Multimap<K, V> {
   List<V> get(@Nullable K var1);

   @CanIgnoreReturnValue
   List<V> removeAll(@Nullable Object var1);
}
