package org.spongepowered.asm.service;

import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;

public interface IFeatureValidator {
   IFeatureValidator ALLOW_ALL = new IFeatureValidator() {
      public void validateEnumExtension(IMixinInfo mixin, ClassInfo targetClass) throws InvalidMixinException {
      }
   };

   void validateEnumExtension(IMixinInfo var1, ClassInfo var2) throws InvalidMixinException;
}
