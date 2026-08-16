package org.spongepowered.asm.mixin.injection.code;

import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.selectors.TargetSelectors;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;
import org.spongepowered.asm.util.Annotations;

public class InjectorTarget {
   private final ISliceContext context;
   private final Map<String, InsnListReadOnly> cache = new HashMap();
   private final Target target;
   private final TargetSelectors.SelectedMethod selectedMethod;
   private final String mergedBy;
   private final int mergedPriority;

   public InjectorTarget(ISliceContext context, Target target, TargetSelectors.SelectedMethod selectedMethod) {
      this.context = context;
      this.target = target;
      this.selectedMethod = selectedMethod;
      AnnotationNode merged = Annotations.getVisible(target.method, MixinMerged.class);
      this.mergedBy = (String)Annotations.getValue(merged, "mixin");
      this.mergedPriority = (Integer)Annotations.getValue(merged, "priority", 1000);
   }

   public String toString() {
      return this.target.toString();
   }

   public InjectionNodes.InjectionNode addInjectionNode(AbstractInsnNode node) {
      return this.target.addInjectionNode(node);
   }

   public InjectionNodes.InjectionNode getInjectionNode(AbstractInsnNode node) {
      return this.target.getInjectionNode(node);
   }

   public String getName() {
      return this.target.getName();
   }

   public String getDesc() {
      return this.target.getDesc();
   }

   public String getSignature() {
      return this.target.getSignature();
   }

   public Target getTarget() {
      return this.target;
   }

   public MethodNode getMethod() {
      return this.target.method;
   }

   public TargetSelectors.SelectedMethod getSelectedMethod() {
      return this.selectedMethod;
   }

   public boolean isMerged() {
      return this.mergedBy != null;
   }

   public String getMergedBy() {
      return this.mergedBy;
   }

   public int getMergedPriority() {
      return this.mergedPriority;
   }

   public InsnList getSlice(String id) {
      InsnListReadOnly slice = (InsnListReadOnly)this.cache.get(id);
      if (slice == null) {
         MethodSlice sliceInfo = this.context.getSlice(id);
         if (sliceInfo != null) {
            slice = sliceInfo.getSlice(this.target);
         } else {
            slice = new InsnListEx(this.target);
         }

         this.cache.put(id, slice);
      }

      return slice;
   }

   public InsnList getSlice(InjectionPoint injectionPoint) {
      return this.getSlice(injectionPoint.getSlice());
   }

   public void dispose() {
      for(InsnListReadOnly insns : this.cache.values()) {
         insns.dispose();
      }

      this.cache.clear();
   }
}
