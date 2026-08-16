package org.spongepowered.asm.mixin.injection.points;

import java.util.Collection;
import java.util.ListIterator;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.spongepowered.asm.mixin.injection.IInjectionPointContext;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.refmap.IMixinContext;

@InjectionPoint.AtCode("TAIL")
public class BeforeFinalReturn extends InjectionPoint {
   private final IMixinContext context;

   public BeforeFinalReturn(InjectionPointData data) {
      super(data);
      this.context = data.getMixin();
   }

   public boolean checkPriority(int targetPriority, int ownerPriority) {
      return true;
   }

   public InjectionPoint.RestrictTargetLevel getTargetRestriction(IInjectionPointContext context) {
      return InjectionPoint.RestrictTargetLevel.ALLOW_ALL;
   }

   public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
      AbstractInsnNode ret = null;
      int returnOpcode = Type.getReturnType(desc).getOpcode(172);
      ListIterator<AbstractInsnNode> iter = insns.iterator();

      while(iter.hasNext()) {
         AbstractInsnNode insn = (AbstractInsnNode)iter.next();
         if (insn instanceof InsnNode && insn.getOpcode() == returnOpcode) {
            ret = insn;
         }
      }

      if (ret == null) {
         throw new InvalidInjectionException(this.context, "TAIL could not locate a valid RETURN in the target method!");
      } else {
         nodes.add(ret);
         return true;
      }
   }
}
