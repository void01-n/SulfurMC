package org.spongepowered.asm.launch.platform.container;

import java.util.Collection;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigSource;

public interface IContainerHandle extends IMixinConfigSource {
   String getAttribute(String var1);

   Collection<IContainerHandle> getNestedContainers();
}
