package org.spongepowered.asm.launch.platform;

import cpw.mods.modlauncher.Environment;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.ILaunchHandlerService;
import cpw.mods.modlauncher.api.TypesafeMap;
import cpw.mods.modlauncher.api.IEnvironment.Keys;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;

public class MixinPlatformAgentMinecraftForge extends MixinPlatformAgentAbstract implements IMixinPlatformServiceAgent {
   public void init() {
   }

   public IMixinPlatformAgent.AcceptResult accept(MixinPlatformManager manager, IContainerHandle handle) {
      return IMixinPlatformAgent.AcceptResult.REJECTED;
   }

   public String getSideName() {
      Environment environment = Launcher.INSTANCE.environment();
      String launchTarget = ((String)environment.getProperty((TypesafeMap.Key)Keys.LAUNCHTARGET.get()).orElse("missing")).toLowerCase(Locale.ROOT);
      if (launchTarget.contains("server")) {
         return "SERVER";
      } else if (launchTarget.contains("client")) {
         return "CLIENT";
      } else {
         Optional<ILaunchHandlerService> launchHandler = environment.findLaunchHandler(launchTarget);
         if (launchHandler.isPresent()) {
            ILaunchHandlerService service = (ILaunchHandlerService)launchHandler.get();

            try {
               Method mdGetDist = service.getClass().getDeclaredMethod("getDist");
               String strDist = mdGetDist.invoke(service).toString().toLowerCase(Locale.ROOT);
               if (strDist.contains("server")) {
                  return "SERVER";
               }

               if (strDist.contains("client")) {
                  return "CLIENT";
               }
            } catch (Exception var7) {
               return null;
            }
         }

         return null;
      }
   }

   public Collection<IContainerHandle> getMixinContainers() {
      return null;
   }
}
