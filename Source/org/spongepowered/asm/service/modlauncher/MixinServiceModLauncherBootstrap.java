package org.spongepowered.asm.service.modlauncher;

import cpw.mods.modlauncher.Launcher;
import org.spongepowered.asm.service.IMixinServiceBootstrap;
import org.spongepowered.asm.service.ServiceInitialisationException;

public class MixinServiceModLauncherBootstrap implements IMixinServiceBootstrap {
   public String getName() {
      return "ModLauncher";
   }

   public String getServiceClassName() {
      return "org.spongepowered.asm.service.modlauncher.MixinServiceModLauncher";
   }

   public void bootstrap() {
      try {
         Launcher.INSTANCE.hashCode();
      } catch (Throwable var2) {
         throw new ServiceInitialisationException(this.getName() + " is not available");
      }
   }
}
