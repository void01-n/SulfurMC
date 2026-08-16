package org.spongepowered.asm.mixin.transformer;

import java.util.List;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.ext.IExtensionRegistry;

public interface IMixinTransformer {
   void audit(MixinEnvironment var1);

   List<String> reload(String var1, ClassNode var2);

   boolean computeFramesForClass(MixinEnvironment var1, String var2, ClassNode var3);

   byte[] transformClassBytes(String var1, String var2, byte[] var3);

   byte[] transformClass(MixinEnvironment var1, String var2, byte[] var3);

   boolean transformClass(MixinEnvironment var1, String var2, ClassNode var3);

   boolean couldTransformClass(MixinEnvironment var1, String var2);

   byte[] generateClass(MixinEnvironment var1, String var2);

   boolean generateClass(MixinEnvironment var1, String var2, ClassNode var3);

   IExtensionRegistry getExtensions();
}
