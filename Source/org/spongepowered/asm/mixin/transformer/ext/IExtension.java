package org.spongepowered.asm.mixin.transformer.ext;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;

public interface IExtension {
   boolean checkActive(MixinEnvironment var1);

   void preApply(ITargetClassContext var1);

   void postApply(ITargetClassContext var1);

   void export(MixinEnvironment var1, String var2, boolean var3, ClassNode var4);
}
