package org.spongepowered.asm.mixin;

public final class MixinIntrinsics {
   private MixinIntrinsics() {
   }

   public static int currentEnumOrdinal() {
      throw new UnsupportedOperationException("This method is intrinsic and can only be used in initializers of enum extension constants");
   }
}
