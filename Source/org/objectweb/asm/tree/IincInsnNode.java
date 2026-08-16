package org.objectweb.asm.tree;

import java.util.Map;
import org.objectweb.asm.MethodVisitor;

public class IincInsnNode extends AbstractInsnNode {
   public int var;
   public int incr;

   public IincInsnNode(int varIndex, int incr) {
      super(132);
      this.var = varIndex;
      this.incr = incr;
   }

   public int getType() {
      return 10;
   }

   public void accept(MethodVisitor methodVisitor) {
      methodVisitor.visitIincInsn(this.var, this.incr);
      this.acceptAnnotations(methodVisitor);
   }

   public AbstractInsnNode clone(Map<LabelNode, LabelNode> clonedLabels) {
      return (new IincInsnNode(this.var, this.incr)).cloneAnnotations(this);
   }
}
