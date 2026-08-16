package org.spongepowered.asm.mixin.transformer;

import java.util.Arrays;

public final class MixinHooks {
   private MixinHooks() {
   }

   public static Enum<?>[] concatEnumValues(Enum<?>[] existing, Enum<?>[] added) {
      Enum<?>[] result = (Enum[])Arrays.copyOf(existing, existing.length + added.length);
      System.arraycopy(added, 0, result, existing.length, added.length);
      return result;
   }
}
