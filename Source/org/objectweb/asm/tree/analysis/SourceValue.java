package org.objectweb.asm.tree.analysis;

import java.util.Set;
import org.objectweb.asm.tree.AbstractInsnNode;

public class SourceValue implements Value {
   public final int size;
   public final Set<AbstractInsnNode> insns;

   public SourceValue(int size) {
      this(size, (Set)(new SmallSet()));
   }

   public SourceValue(int size, AbstractInsnNode insnNode) {
      this.size = size;
      this.insns = new SmallSet<AbstractInsnNode>(insnNode);
   }

   public SourceValue(int size, Set<AbstractInsnNode> insnSet) {
      this.size = size;
      this.insns = insnSet;
   }

   public int getSize() {
      return this.size;
   }

   public boolean equals(Object value) {
      if (!(value instanceof SourceValue)) {
         return false;
      } else {
         SourceValue sourceValue = (SourceValue)value;
         return this.size == sourceValue.size && this.insns.equals(sourceValue.insns);
      }
   }

   public int hashCode() {
      return this.insns.hashCode();
   }
}
