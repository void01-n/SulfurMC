package org.spongepowered.asm.mixin.injection.code;

import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.spongepowered.asm.mixin.injection.struct.Constructor;
import org.spongepowered.asm.mixin.injection.struct.IChainedDecoration;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.transformer.struct.Initialiser;
import org.spongepowered.asm.util.Bytecode;

public class InsnListEx extends InsnListReadOnly implements IInsnListEx {
   protected final Target target;
   private Map<String, Object> decorations;

   public InsnListEx(Target target) {
      super(target.insns);
      this.target = target;
   }

   public String toString() {
      return this.target.toString();
   }

   public String getTargetName() {
      return this.target.getName();
   }

   public String getTargetDesc() {
      return this.target.getDesc();
   }

   public String getTargetSignature() {
      return this.target.getSignature();
   }

   public int getTargetAccess() {
      return this.target.method.access;
   }

   public boolean isTargetStatic() {
      return this.target.isStatic;
   }

   public boolean isTargetConstructor() {
      return this.target instanceof Constructor;
   }

   public boolean isTargetStaticInitialiser() {
      return "<clinit>".equals(this.target.getName());
   }

   public AbstractInsnNode getSpecialNode(IInsnListEx.SpecialNodeType type) {
      switch (type) {
         case DELEGATE_CTOR:
            if (this.target instanceof Constructor) {
               Bytecode.DelegateInitialiser superCall = ((Constructor)this.target).findDelegateInitNode();
               if (superCall.isPresent && this.contains(superCall.insn)) {
                  return superCall.insn;
               }
            }

            return null;
         case INITIALISER_INJECTION_POINT:
            if (this.target instanceof Constructor) {
               Initialiser.InjectionMode mode = Initialiser.InjectionMode.DEFAULT;
               AbstractInsnNode initialiserInjectionPoint = ((Constructor)this.target).findInitialiserInjectionPoint(mode);
               if (this.contains(initialiserInjectionPoint)) {
                  return initialiserInjectionPoint;
               }
            }

            return null;
         case CTOR_BODY:
            if (this.target instanceof Constructor) {
               AbstractInsnNode beforeBody = ((Constructor)this.target).findFirstBodyInsn();
               if (this.contains(beforeBody)) {
                  return beforeBody;
               }
            }

            return null;
         default:
            return null;
      }
   }

   public <V> InsnListEx decorate(String key, V value) {
      if (this.decorations == null) {
         this.decorations = new HashMap();
      }

      if (value instanceof IChainedDecoration && this.decorations.containsKey(key)) {
         Object previous = this.decorations.get(key);
         if (previous.getClass().equals(value.getClass())) {
            ((IChainedDecoration)value).replace(previous);
         }
      }

      this.decorations.put(key, value);
      return this;
   }

   public InsnListEx undecorate(String key) {
      if (this.decorations != null) {
         this.decorations.remove(key);
      }

      return this;
   }

   public InsnListEx undecorate() {
      this.decorations = null;
      return this;
   }

   public boolean hasDecoration(String key) {
      return this.decorations != null && this.decorations.get(key) != null;
   }

   public <V> V getDecoration(String key) {
      return (V)(this.decorations == null ? null : this.decorations.get(key));
   }

   public <V> V getDecoration(String key, V defaultValue) {
      V existing = (V)(this.decorations == null ? null : this.decorations.get(key));
      return (V)(existing != null ? existing : defaultValue);
   }
}
