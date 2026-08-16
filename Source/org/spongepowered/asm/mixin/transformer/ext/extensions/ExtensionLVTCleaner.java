package org.spongepowered.asm.mixin.transformer.ext.extensions;

import java.util.Iterator;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;
import org.spongepowered.asm.util.Locals;

public class ExtensionLVTCleaner implements IExtension {
   public boolean checkActive(MixinEnvironment environment) {
      return true;
   }

   public void preApply(ITargetClassContext context) {
   }

   public void postApply(ITargetClassContext context) {
   }

   public void export(MixinEnvironment env, String name, boolean force, ClassNode classNode) {
      for(MethodNode methodNode : classNode.methods) {
         if (methodNode.localVariables != null) {
            Iterator<LocalVariableNode> it = methodNode.localVariables.iterator();

            while(it.hasNext()) {
               if (it.next() instanceof Locals.SyntheticLocalVariableNode) {
                  it.remove();
               }
            }
         }
      }

   }
}
