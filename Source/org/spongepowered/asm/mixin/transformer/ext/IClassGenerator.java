package org.spongepowered.asm.mixin.transformer.ext;

import org.objectweb.asm.tree.ClassNode;

public interface IClassGenerator {
   String getName();

   boolean generate(String var1, ClassNode var2);
}
