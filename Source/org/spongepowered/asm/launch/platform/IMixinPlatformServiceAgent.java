package org.spongepowered.asm.launch.platform;

import java.util.Collection;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.util.IConsumer;

public interface IMixinPlatformServiceAgent extends IMixinPlatformAgent {
   void init();

   String getSideName();

   Collection<IContainerHandle> getMixinContainers();

   /** @deprecated */
   @Deprecated
   void wire(MixinEnvironment.Phase var1, IConsumer<MixinEnvironment.Phase> var2);

   /** @deprecated */
   @Deprecated
   void unwire();
}
