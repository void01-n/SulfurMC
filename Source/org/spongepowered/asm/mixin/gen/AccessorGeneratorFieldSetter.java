package org.spongepowered.asm.mixin.gen;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.gen.throwables.InvalidAccessorException;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Bytecode;

public class AccessorGeneratorFieldSetter extends AccessorGeneratorField {
   private boolean mutable;

   public AccessorGeneratorFieldSetter(AccessorInfo info) {
      super(info);
   }

   public void validate() {
      if (Bytecode.hasFlag((ClassNode)this.info.getClassNode(), 512)) {
         throw new InvalidAccessorException(this.info, String.format("%s tried to change interface field %s::%s", this.info, this.info.getClassNode().name, this.targetField.name));
      } else {
         super.validate();
         ClassInfo.Method method = this.info.getClassInfo().findMethod(this.info.getMethod());
         this.mutable = method.isDecoratedMutable();
         if (!this.mutable && Bytecode.hasFlag((FieldNode)this.targetField, 16)) {
            if (this.info.getMixin().getOption(MixinEnvironment.Option.DEBUG_VERBOSE)) {
               MixinService.getService().getLogger("mixin").warn("{} for final field {}::{} is not @Mutable", this.info, ((MixinTargetContext)this.info.getMixin()).getTarget(), this.targetField.name);
            }

         }
      }
   }

   public MethodNode generate() {
      if (this.mutable) {
         FieldNode var10000 = this.targetField;
         var10000.access &= -17;
      }

      int stackSpace = this.targetIsStatic ? 0 : 1;
      int maxLocals = stackSpace + this.targetType.getSize();
      int maxStack = stackSpace + this.targetType.getSize();
      MethodNode method = this.createMethod(maxLocals, maxStack);
      if (!this.targetIsStatic) {
         method.instructions.add((AbstractInsnNode)(new VarInsnNode(25, 0)));
      }

      method.instructions.add((AbstractInsnNode)(new VarInsnNode(this.targetType.getOpcode(21), stackSpace)));
      int opcode = this.targetIsStatic ? 179 : 181;
      method.instructions.add((AbstractInsnNode)(new FieldInsnNode(opcode, this.info.getTargetClassNode().name, this.targetField.name, this.targetField.desc)));
      method.instructions.add((AbstractInsnNode)(new InsnNode(177)));
      return method;
   }
}
