package org.objectweb.asm.tree;

import java.util.List;
import java.util.Map;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class LookupSwitchInsnNode extends AbstractInsnNode {
   public LabelNode dflt;
   public List<Integer> keys;
   public List<LabelNode> labels;

   public LookupSwitchInsnNode(LabelNode dflt, int[] keys, LabelNode[] labels) {
      super(171);
      this.dflt = dflt;
      this.keys = Util.asArrayList(keys);
      this.labels = Util.asArrayList(labels);
   }

   public int getType() {
      return 12;
   }

   public void accept(MethodVisitor methodVisitor) {
      int[] keysArray = new int[this.keys.size()];
      int i = 0;

      for(int n = keysArray.length; i < n; ++i) {
         keysArray[i] = (Integer)this.keys.get(i);
      }

      Label[] labelsArray = new Label[this.labels.size()];
      int i = 0;

      for(int n = labelsArray.length; i < n; ++i) {
         labelsArray[i] = ((LabelNode)this.labels.get(i)).getLabel();
      }

      methodVisitor.visitLookupSwitchInsn(this.dflt.getLabel(), keysArray, labelsArray);
      this.acceptAnnotations(methodVisitor);
   }

   public AbstractInsnNode clone(Map<LabelNode, LabelNode> clonedLabels) {
      LookupSwitchInsnNode clone = new LookupSwitchInsnNode(clone(this.dflt, clonedLabels), (int[])null, clone(this.labels, clonedLabels));
      clone.keys.addAll(this.keys);
      return clone.cloneAnnotations(this);
   }
}
