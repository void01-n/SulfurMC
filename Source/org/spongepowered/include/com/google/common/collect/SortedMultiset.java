package org.spongepowered.include.com.google.common.collect;

import java.util.NavigableSet;

public interface SortedMultiset<E> extends SortedIterable<E>, SortedMultisetBridge<E> {
   NavigableSet<E> elementSet();
}
