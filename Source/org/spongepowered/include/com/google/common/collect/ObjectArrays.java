package org.spongepowered.include.com.google.common.collect;

import java.lang.reflect.Array;
import java.util.Arrays;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.errorprone.annotations.CanIgnoreReturnValue;

public final class ObjectArrays {
   static final Object[] EMPTY_ARRAY = new Object[0];

   public static <T> T[] newArray(Class<T> type, int length) {
      return (T[])((Object[])Array.newInstance(type, length));
   }

   public static <T> T[] newArray(T[] reference, int length) {
      return (T[])Platform.newArray(reference, length);
   }

   public static <T> T[] concat(T[] first, T[] second, Class<T> type) {
      T[] result = (T[])newArray(type, first.length + second.length);
      System.arraycopy(first, 0, result, 0, first.length);
      System.arraycopy(second, 0, result, first.length, second.length);
      return result;
   }

   public static <T> T[] concat(@Nullable T element, T[] array) {
      T[] result = (T[])newArray(array, array.length + 1);
      result[0] = element;
      System.arraycopy(array, 0, result, 1, array.length);
      return result;
   }

   public static <T> T[] concat(T[] array, @Nullable T element) {
      T[] result = (T[])Arrays.copyOf(array, array.length + 1);
      result[array.length] = element;
      return result;
   }

   @CanIgnoreReturnValue
   static Object[] checkElementsNotNull(Object... array) {
      return checkElementsNotNull(array, array.length);
   }

   @CanIgnoreReturnValue
   static Object[] checkElementsNotNull(Object[] array, int length) {
      for(int i = 0; i < length; ++i) {
         checkElementNotNull(array[i], i);
      }

      return array;
   }

   @CanIgnoreReturnValue
   static Object checkElementNotNull(Object element, int index) {
      if (element == null) {
         throw new NullPointerException("at index " + index);
      } else {
         return element;
      }
   }
}
