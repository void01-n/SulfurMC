package org.spongepowered.asm.mixin.transformer.ext;

import java.util.List;
import org.spongepowered.asm.service.ISyntheticClassRegistry;

public interface IExtensionRegistry {
   List<IExtension> getExtensions();

   List<IExtension> getActiveExtensions();

   <T extends IExtension> T getExtension(Class<? extends IExtension> var1);

   ISyntheticClassRegistry getSyntheticClassRegistry();
}
