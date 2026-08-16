package org.objectweb.asm.tree;

import java.util.Map;
import org.objectweb.asm.MethodVisitor;

public class TypeInsnNode extends AbstractInsnNode {
   public String desc;

   public TypeInsnNode(int opcode, String type) {
      super(opcode);
      this.desc = type;
   }

   public void setOpcode(int opcode) {
      this.opcode = opcode;
   }

   public int getType() {
      return 3;
   }

   public void accept(MethodVisitor methodVisitor) {
      methodVisitor.visitTypeInsn(this.opcode, this.desc);
      this.acceptAnnotations(methodVisitor);
   }

   public AbstractInsnNode clone(Map<LabelNode, LabelNode> clonedLabels) {
      return (new TypeInsnNode(this.opcode, this.desc)).cloneAnnotations(this);
   }
}
