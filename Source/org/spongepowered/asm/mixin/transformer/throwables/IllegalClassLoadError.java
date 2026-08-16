package org.spongepowered.asm.mixin.transformer.throwables;

public class IllegalClassLoadError extends MixinTransformerError {
   public IllegalClassLoadError(String message) {
      super(message);
   }

   public IllegalClassLoadError(Throwable cause) {
      super(cause);
   }

   public IllegalClassLoadError(String message, Throwable cause) {
      super(message, cause);
   }
}
