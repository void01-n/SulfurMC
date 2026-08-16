package org.spongepowered.asm.launch;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import java.util.EnumSet;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

public interface IClassProcessor {
   EnumSet<ILaunchPluginService.Phase> handlesClass(Type var1, boolean var2, String var3);

   boolean processClass(ILaunchPluginService.Phase var1, ClassNode var2, Type var3, String var4);

   boolean generatesClass(Type var1);

   boolean generateClass(Type var1, ClassNode var2);
}
