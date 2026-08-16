package org.spongepowered.asm.mixin.struct;

import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.throwables.MixinTransformerError;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.Handles;

public abstract class MemberRef {
   public abstract boolean isField();

   public abstract int getOpcode();

   public abstract void setOpcode(int var1);

   public abstract String getOwner();

   public abstract void setOwner(String var1);

   public abstract String getName();

   public abstract void setName(String var1);

   public abstract String getDesc();

   public abstract void setDesc(String var1);

   public boolean ownerIsMixin() {
      String owner = this.getOwner();
      return !owner.startsWith("[") && ClassInfo.isMixin(owner);
   }

   public String toString() {
      return String.format("%s for %s.%s%s%s", Bytecode.getOpcodeName(this.getOpcode()), this.getOwner(), this.getName(), this.isField() ? ":" : "", this.getDesc());
   }

   public boolean equals(Object obj) {
      if (!(obj instanceof MemberRef)) {
         return false;
      } else {
         MemberRef other = (MemberRef)obj;
         return this.getOpcode() == other.getOpcode() && this.getOwner().equals(other.getOwner()) && this.getName().equals(other.getName()) && this.getDesc().equals(other.getDesc());
      }
   }

   public int hashCode() {
      return this.toString().hashCode();
   }

   public static final class Method extends MemberRef {
      public final MethodInsnNode insn;

      public Method(MethodInsnNode insn) {
         this.insn = insn;
      }

      public boolean isField() {
         return false;
      }

      public int getOpcode() {
         return this.insn.getOpcode();
      }

      public void setOpcode(int opcode) {
         if ((opcode & 191) == 0) {
            throw new IllegalArgumentException("Invalid opcode for method instruction: 0x" + Integer.toHexString(opcode));
         } else {
            this.insn.setOpcode(opcode);
         }
      }

      public String getOwner() {
         return this.insn.owner;
      }

      public void setOwner(String owner) {
         this.insn.owner = owner;
      }

      public String getName() {
         return this.insn.name;
      }

      public void setName(String name) {
         this.insn.name = name;
      }

      public String getDesc() {
         return this.insn.desc;
      }

      public void setDesc(String desc) {
         this.insn.desc = desc;
      }
   }

   public static final class Field extends MemberRef {
      public final FieldInsnNode insn;

      public Field(FieldInsnNode insn) {
         this.insn = insn;
      }

      public boolean isField() {
         return true;
      }

      public int getOpcode() {
         return this.insn.getOpcode();
      }

      public void setOpcode(int opcode) {
         if ((opcode & 183) == 0) {
            throw new IllegalArgumentException("Invalid opcode for field instruction: 0x" + Integer.toHexString(opcode));
         } else {
            this.insn.setOpcode(opcode);
         }
      }

      public String getOwner() {
         return this.insn.owner;
      }

      public void setOwner(String owner) {
         this.insn.owner = owner;
      }

      public String getName() {
         return this.insn.name;
      }

      public void setName(String name) {
         this.insn.name = name;
      }

      public String getDesc() {
         return this.insn.desc;
      }

      public void setDesc(String desc) {
         this.insn.desc = desc;
      }
   }

   public static final class Handle extends MemberRef {
      private org.objectweb.asm.Handle handle;

      public Handle(org.objectweb.asm.Handle handle) {
         this.handle = handle;
      }

      public org.objectweb.asm.Handle getMethodHandle() {
         return this.handle;
      }

      public boolean isField() {
         return Handles.isField(this.handle);
      }

      public int getOpcode() {
         int opcode = Handles.opcodeFromTag(this.handle.getTag());
         if (opcode == 0) {
            throw new MixinTransformerError("Invalid tag " + this.handle.getTag() + " for method handle " + this.handle + ".");
         } else {
            return opcode;
         }
      }

      public void setOpcode(int opcode) {
         int tag = Handles.tagFromOpcode(opcode);
         if (tag == 0) {
            throw new MixinTransformerError("Invalid opcode " + Bytecode.getOpcodeName(opcode) + " for method handle " + this.handle + ".");
         } else {
            this.setHandle(tag, this.handle.getOwner(), this.handle.getName(), this.handle.getDesc(), this.handle.isInterface());
         }
      }

      public String getOwner() {
         return this.handle.getOwner();
      }

      public void setOwner(String owner) {
         this.setHandle(this.handle.getTag(), owner, this.handle.getName(), this.handle.getDesc(), this.handle.isInterface());
      }

      public String getName() {
         return this.handle.getName();
      }

      public void setName(String name) {
         this.setHandle(this.handle.getTag(), this.handle.getOwner(), name, this.handle.getDesc(), this.handle.isInterface());
      }

      public String getDesc() {
         return this.handle.getDesc();
      }

      public void setDesc(String desc) {
         this.setHandle(this.handle.getTag(), this.handle.getOwner(), this.handle.getName(), desc, this.handle.isInterface());
      }

      public void setHandle(int tag, String owner, String name, String desc, boolean isInterface) {
         this.handle = new org.objectweb.asm.Handle(tag, owner, name, desc, isInterface);
      }
   }
}
