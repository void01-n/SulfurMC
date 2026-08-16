package org.spongepowered.asm.service;

import org.objectweb.asm.tree.ClassNode;

public interface ITreeClassTransformer extends ITransformer {
   boolean transformClassNode(String var1, String var2, ClassNode var3);
}
