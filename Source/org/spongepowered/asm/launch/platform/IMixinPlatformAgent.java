package org.spongepowered.asm.launch.platform;

import org.spongepowered.asm.launch.platform.container.IContainerHandle;

public interface IMixinPlatformAgent {
   AcceptResult accept(MixinPlatformManager var1, IContainerHandle var2);

   String getPhaseProvider();

   void prepare();

   void initPrimaryContainer();

   void inject();

   public static enum AcceptResult {
      ACCEPTED,
      REJECTED,
      INVALID;

      // $FF: synthetic method
      private static AcceptResult[] $values() {
         return new AcceptResult[]{ACCEPTED, REJECTED, INVALID};
      }
   }
}
