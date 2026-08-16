package org.spongepowered.include.com.google.gson.internal;

import java.math.BigDecimal;

public final class LazilyParsedNumber extends Number {
   private final String value;

   public LazilyParsedNumber(String value) {
      this.value = value;
   }

   public int intValue() {
      try {
         return Integer.parseInt(this.value);
      } catch (NumberFormatException var4) {
         try {
            return (int)Long.parseLong(this.value);
         } catch (NumberFormatException var3) {
            return (new BigDecimal(this.value)).intValue();
         }
      }
   }

   public long longValue() {
      try {
         return Long.parseLong(this.value);
      } catch (NumberFormatException var2) {
         return (new BigDecimal(this.value)).longValue();
      }
   }

   public float floatValue() {
      return Float.parseFloat(this.value);
   }

   public double doubleValue() {
      return Double.parseDouble(this.value);
   }

   public String toString() {
      return this.value;
   }
}
