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

@InjectionPoint.AtCode("RETURN")
public class BeforeReturn extends InjectionPoint {
   private final int ordinal;

   public BeforeReturn(InjectionPointData data) {
      super(data);
      this.ordinal = data.getOrdinal();
   }

   public boolean checkPriority(int targetPriority, int ownerPriority) {
      return true;
   }

   public InjectionPoint.RestrictTargetLevel getTargetRestriction(IInjectionPointContext context) {
      return InjectionPoint.RestrictTargetLevel.ALLOW_ALL;
   }

   public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
      boolean found = false;
      int returnOpcode = Type.getReturnType(desc).getOpcode(172);
      int ordinal = 0;
      ListIterator<AbstractInsnNode> iter = insns.iterator();

      while(iter.hasNext()) {
         AbstractInsnNode insn = (AbstractInsnNode)iter.next();
         if (insn instanceof InsnNode && insn.getOpcode() == returnOpcode) {
            if (this.ordinal == -1 || this.ordinal == ordinal) {
               nodes.add(insn);
               found = true;
            }

            ++ordinal;
         }
      }

      return found;
   }
}
