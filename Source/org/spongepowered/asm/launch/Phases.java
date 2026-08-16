package org.spongepowered.asm.launch;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService.Phase;
import java.util.EnumSet;

public final class Phases {
   public static final EnumSet<ILaunchPluginService.Phase> NONE = EnumSet.noneOf(ILaunchPluginService.Phase.class);
   public static final EnumSet<ILaunchPluginService.Phase> BEFORE_ONLY;
   public static final EnumSet<ILaunchPluginService.Phase> AFTER_ONLY;

   private Phases() {
   }

   static {
      BEFORE_ONLY = EnumSet.of(Phase.BEFORE);
      AFTER_ONLY = EnumSet.of(Phase.AFTER);
   }
}
