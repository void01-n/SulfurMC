package org.spongepowered.tools.obfuscation.mirror;

import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.util.Bytecode;

public class MethodHandleASM extends MethodHandle {
   private final MethodNode method;

   public MethodHandleASM(TypeHandle owner, MethodNode method) {
      super(owner, method.name, method.desc);
      this.method = method;
   }

   public String getJavaSignature() {
      return TypeUtils.getJavaSignature(this.method.desc);
   }

   public Bytecode.Visibility getVisibility() {
      return Bytecode.getVisibility(this.method);
   }
}
