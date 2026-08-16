package org.spongepowered.include.com.google.common.primitives;

import java.util.Arrays;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.base.Preconditions;

public final class Longs {
   private static final byte[] asciiDigits = createAsciiDigits();

   private static byte[] createAsciiDigits() {
      byte[] result = new byte[128];
      Arrays.fill(result, (byte)-1);

      for(int i = 0; i <= 9; ++i) {
         result[48 + i] = (byte)i;
      }

      for(int i = 0; i <= 26; ++i) {
         result[65 + i] = (byte)(10 + i);
         result[97 + i] = (byte)(10 + i);
      }

      return result;
   }

   private static int digit(char c) {
      return c < 128 ? asciiDigits[c] : -1;
   }

   @Nullable
   @CheckForNull
   public static Long tryParse(String string) {
      return tryParse(string, 10);
   }

   @Nullable
   @CheckForNull
   public static Long tryParse(String string, int radix) {
      if (((String)Preconditions.checkNotNull(string)).isEmpty()) {
         return null;
      } else if (radix >= 2 && radix <= 36) {
         boolean negative = string.charAt(0) == '-';
         int index = negative ? 1 : 0;
         if (index == string.length()) {
            return null;
         } else {
            int digit = digit(string.charAt(index++));
            if (digit >= 0 && digit < radix) {
               long accum = (long)(-digit);

               for(long cap = Long.MIN_VALUE / (long)radix; index < string.length(); accum -= (long)digit) {
                  digit = digit(string.charAt(index++));
                  if (digit < 0 || digit >= radix || accum < cap) {
                     return null;
                  }

                  accum *= (long)radix;
                  if (accum < Long.MIN_VALUE + (long)digit) {
                     return null;
                  }
               }

               if (negative) {
                  return accum;
               } else if (accum == Long.MIN_VALUE) {
                  return null;
               } else {
                  return -accum;
               }
            } else {
               return null;
            }
         }
      } else {
         throw new IllegalArgumentException("radix must be between MIN_RADIX and MAX_RADIX but was " + radix);
      }
   }
}
