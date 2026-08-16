package org.spongepowered.asm.mixin.transformer;

import java.util.ListIterator;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;

final class EnumExtensionUtils {
   public static void checkForGotchas(MixinInfo mixin, ClassNode classNode) throws InvalidMixinException {
      checkForOrdinalSwitch(mixin, classNode);
   }

   private static void checkForOrdinalSwitch(MixinInfo mixin, ClassNode classNode) {
      for(MethodNode method : classNode.methods) {
         ListIterator var4 = method.instructions.iterator();

         while(var4.hasNext()) {
            AbstractInsnNode insn = (AbstractInsnNode)var4.next();
            if (isOrdinalCall(insn, mixin) && isSwitch(insn.getNext())) {
               Integer line = findLineNumber(insn);
               throw new InvalidMixinException(mixin, String.format("`ordinal` switch on enum extension type is not supported but was found on line %s. Instead, switch on the target enum, e.g. `switch ((TargetEnum) (Object) ...)`", line));
            }
         }
      }

   }

   private static boolean isOrdinalCall(AbstractInsnNode insn, MixinInfo mixin) {
      if (insn.getOpcode() != 182) {
         return false;
      } else {
         MethodInsnNode call = (MethodInsnNode)insn;
         return call.owner.equals(mixin.getClassRef()) && call.name.equals("ordinal") && call.desc.equals("()I");
      }
   }

   private static boolean isSwitch(AbstractInsnNode insn) {
      return insn.getOpcode() == 170 || insn.getOpcode() == 171;
   }

   private static Integer findLineNumber(AbstractInsnNode insn) {
      while(!(insn instanceof LineNumberNode)) {
         insn = insn.getPrevious();
         if (insn == null) {
            return null;
         }
      }

      return ((LineNumberNode)insn).line;
   }
}
