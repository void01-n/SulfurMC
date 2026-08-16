package org.spongepowered.asm.launch.platform;

import java.util.Collection;
import net.minecraft.launchwrapper.Launch;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;

public class MixinPlatformAgentLiteLoaderLegacy extends MixinPlatformAgentAbstract implements IMixinPlatformServiceAgent {
   public IMixinPlatformAgent.AcceptResult accept(MixinPlatformManager manager, IContainerHandle handle) {
      return IMixinPlatformAgent.AcceptResult.REJECTED;
   }

   public String getSideName() {
      return MixinPlatformAgentAbstract.invokeStringMethod(Launch.classLoader, "com.mumfrey.liteloader.launch.LiteLoaderTweaker", "getEnvironmentType");
   }

   public void init() {
   }

   public Collection<IContainerHandle> getMixinContainers() {
      return null;
   }
}
