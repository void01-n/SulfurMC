package org.spongepowered.asm.mixin.transformer;

import org.spongepowered.asm.launch.MixinInitialisationError;
import org.spongepowered.asm.service.IMixinInternal;

public interface IMixinTransformerFactory extends IMixinInternal {
   IMixinTransformer createTransformer() throws MixinInitialisationError;
}
