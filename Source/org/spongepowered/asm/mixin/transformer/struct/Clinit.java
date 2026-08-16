package org.spongepowered.asm.mixin.transformer.struct;

import java.util.ListIterator;
import java.util.Map;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.Bytecode;

public class Clinit {
   protected final MethodNode clinit;
   protected final AbstractInsnNode finalReturn;

   public Clinit(MethodNode clinit, AbstractInsnNode finalReturn) {
      this.clinit = clinit;
      this.finalReturn = finalReturn;
   }

   public void append(IMixinInfo mixinInfo, MethodNode mixinClinit) {
      prepareClinit(mixinClinit, (Target)null);
      Map<LabelNode, LabelNode> labels = Bytecode.cloneLabels(mixinClinit.instructions);
      this.appendInsns(mixinInfo, mixinClinit, labels);
      this.clinit.maxLocals = Math.max(this.clinit.maxLocals, mixinClinit.maxLocals);
      this.clinit.maxStack = Math.max(this.clinit.maxStack, mixinClinit.maxStack);

      for(TryCatchBlockNode tryCatch : mixinClinit.tryCatchBlocks) {
         this.clinit.tryCatchBlocks.add(new TryCatchBlockNode((LabelNode)labels.get(tryCatch.start), (LabelNode)labels.get(tryCatch.end), (LabelNode)labels.get(tryCatch.handler), tryCatch.type));
      }

      for(LocalVariableNode local : mixinClinit.localVariables) {
         this.clinit.localVariables.add(new LocalVariableNode(local.name, local.desc, local.signature, (LabelNode)labels.get(local.start), (LabelNode)labels.get(local.end), local.index));
      }

   }

   protected void appendInsns(IMixinInfo mixinInfo, MethodNode mixinClinit, Map<LabelNode, LabelNode> labels) {
      ListIterator var4 = mixinClinit.instructions.iterator();

      while(var4.hasNext()) {
         AbstractInsnNode insn = (AbstractInsnNode)var4.next();
         if (insn.getOpcode() != 177) {
            this.clinit.instructions.insertBefore(this.finalReturn, insn.clone(labels));
         }
      }

   }

   public static Clinit prepare(Target clinit) {
      return new Clinit(clinit.method, prepareClinit(clinit.method, clinit));
   }

   protected static AbstractInsnNode prepareClinit(MethodNode clinit, Target target) {
      LabelNode endLabel = new LabelNode();
      AbstractInsnNode existingFinalReturn = null;
      ListIterator<AbstractInsnNode> iter = clinit.instructions.iterator();

      while(iter.hasNext()) {
         AbstractInsnNode insn = (AbstractInsnNode)iter.next();
         if (insn.getOpcode() == 177) {
            if (insn.getNext() == null) {
               existingFinalReturn = insn;
               break;
            }

            AbstractInsnNode newInsn = new JumpInsnNode(167, endLabel);
            iter.set(newInsn);
            if (target != null) {
               InjectionNodes.InjectionNode injectionNode = target.getInjectionNode(insn);
               if (injectionNode != null) {
                  injectionNode.replace(newInsn);
               }
            }
         }
      }

      if (existingFinalReturn != null) {
         clinit.instructions.insertBefore(existingFinalReturn, (AbstractInsnNode)endLabel);
         return existingFinalReturn;
      } else {
         clinit.instructions.add((AbstractInsnNode)endLabel);
         InsnNode finalReturn = new InsnNode(177);
         clinit.instructions.add((AbstractInsnNode)finalReturn);
         return finalReturn;
      }
   }
}
