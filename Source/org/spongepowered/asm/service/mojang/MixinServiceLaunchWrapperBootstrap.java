package org.spongepowered.asm.service.mojang;

import net.minecraft.launchwrapper.Launch;
import org.spongepowered.asm.service.IMixinServiceBootstrap;
import org.spongepowered.asm.service.ServiceInitialisationException;

public class MixinServiceLaunchWrapperBootstrap implements IMixinServiceBootstrap {
   public String getName() {
      return "LaunchWrapper";
   }

   public String getServiceClassName() {
      return "org.spongepowered.asm.service.mojang.MixinServiceLaunchWrapper";
   }

   public void bootstrap() {
      try {
         Launch.classLoader.hashCode();
      } catch (Throwable var2) {
         throw new ServiceInitialisationException(this.getName() + " is not available");
      }

      Launch.classLoader.addClassLoaderExclusion("org.spongepowered.asm.service.");
      Launch.classLoader.addClassLoaderExclusion("org.spongepowered.asm.launch.");
      Launch.classLoader.addClassLoaderExclusion("org.spongepowered.asm.logging.");
      Launch.classLoader.addClassLoaderExclusion("org.objectweb.asm.");
      Launch.classLoader.addClassLoaderExclusion("org.spongepowered.asm.lib.");
      Launch.classLoader.addClassLoaderExclusion("org.spongepowered.asm.mixin.");
      Launch.classLoader.addClassLoaderExclusion("org.spongepowered.asm.util.");
   }
}
