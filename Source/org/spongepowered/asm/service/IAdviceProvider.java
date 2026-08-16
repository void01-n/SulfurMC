package org.spongepowered.asm.service;

public interface IAdviceProvider {
   IAdviceProvider GENERIC = new IAdviceProvider() {
      public String higherCompatibilityNeeded(int requiredCompatibility, String requiredCompatibilityString) {
         return "Increase your compatibility version to at least " + requiredCompatibilityString;
      }
   };

   String higherCompatibilityNeeded(int var1, String var2);
}
