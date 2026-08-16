package org.spongepowered.asm.mixin.injection.selectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.injection.selectors.throwables.SelectorConstraintException;
import org.spongepowered.asm.mixin.injection.struct.InvalidMemberDescriptorException;
import org.spongepowered.asm.mixin.injection.struct.TargetNotSupportedException;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.struct.AnnotatedMethodInfo;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;

public class TargetSelectors implements Iterable<SelectedMethod> {
   private final ISelectorContext context;
   private final ClassNode targetClassNode;
   private final IMixinContext mixin;
   private final Object method;
   private final boolean isStatic;
   private final Set<ITargetSelector> selectors = new LinkedHashSet();
   private final List<SelectedMethod> targets = new ArrayList();
   private boolean doPermissivePass;

   public TargetSelectors(ISelectorContext context, ClassNode classNode) {
      this.context = context;
      this.targetClassNode = classNode;
      this.mixin = context.getMixin();
      this.method = context.getMethod();
      this.isStatic = this.method instanceof MethodNode && Bytecode.isStatic((MethodNode)this.method);
   }

   public void parse(Set<ITargetSelector> selectors) {
      for(ITargetSelector selector : selectors) {
         try {
            this.addSelector(selector.validate().attach(this.context));
         } catch (InvalidMemberDescriptorException ex) {
            throw new InvalidInjectionException(this.context, String.format("%s, has invalid target descriptor: %s. %s", this.context.getElementDescription(), ex.getMessage(), this.mixin.getReferenceMapper().getStatus()));
         } catch (TargetNotSupportedException ex) {
            throw new InvalidInjectionException(this.context, String.format("%s specifies a target class '%s', which is not supported", this.context.getElementDescription(), ex.getMessage()));
         } catch (InvalidSelectorException ex) {
            throw new InvalidInjectionException(this.context, String.format("%s is decorated with an invalid selector: %s", this.context.getElementDescription(), ex.getMessage()));
         }
      }

   }

   public TargetSelectors addSelector(ITargetSelector selector) {
      this.selectors.add(selector);
      return this;
   }

   public int size() {
      return this.targets.size();
   }

   public void clear() {
      this.targets.clear();
   }

   public Iterator<SelectedMethod> iterator() {
      return this.targets.iterator();
   }

   public void remove(SelectedMethod target) {
      this.targets.remove(target);
   }

   public boolean isPermissivePassEnabled() {
      return this.doPermissivePass;
   }

   public TargetSelectors setPermissivePass(boolean enabled) {
      this.doPermissivePass = enabled;
      return this;
   }

   public void find() {
      this.findRootTargets();
   }

   private void findRootTargets() {
      int passes = this.doPermissivePass ? 2 : 1;

      for(ITargetSelector selector : this.selectors) {
         selector = selector.configure(ITargetSelector.Configure.SELECT_MEMBER);
         int matchCount = 0;
         int maxCount = selector.getMaxMatchCount();
         ITargetSelector permissiveSelector = selector.configure(ITargetSelector.Configure.PERMISSIVE);
         int selectorPasses = permissiveSelector == selector ? 1 : passes;

         label73:
         for(int pass = 0; pass < selectorPasses && matchCount < 1; ++pass) {
            ITargetSelector passSelector = pass == 0 ? selector : permissiveSelector;

            for(MethodNode target : this.targetClassNode.methods) {
               if (passSelector.match(ElementNode.of(this.targetClassNode, target)).isExactMatch()) {
                  ++matchCount;
                  boolean isMixinMethod = Annotations.getVisible(target, MixinMerged.class) != null;
                  if (maxCount <= 1 || (this.isStatic || !Bytecode.isStatic(target)) && target != this.method && !isMixinMethod) {
                     this.checkTarget(target);
                     this.targets.add(new SelectedMethod(passSelector, target));
                  }

                  if (matchCount >= maxCount) {
                     break label73;
                  }
               }
            }
         }

         if (matchCount < selector.getMinMatchCount()) {
            throw new InvalidInjectionException(this.context, new SelectorConstraintException(selector, String.format("Injection validation failed: %s for %s did not match the required number of targets (required=%d, matched=%d). %s%s", selector, this.context.getElementDescription(), selector.getMinMatchCount(), matchCount, this.mixin.getReferenceMapper().getStatus(), AnnotatedMethodInfo.getDynamicInfo(this.method))));
         }
      }

   }

   protected void findNestedTargets() {
      boolean recursed = false;

      do {
         recursed = false;
         ListIterator<SelectedMethod> iter = this.targets.listIterator();

         while(iter.hasNext()) {
            SelectedMethod target = (SelectedMethod)iter.next();
            ITargetSelector next = target.next();
            if (next != null) {
               recursed = true;
               TargetSelector.Result<AbstractInsnNode> result = TargetSelector.run(next, ElementNode.dynamicInsnList(target.getMethod().instructions));
               iter.remove();

               for(ElementNode<AbstractInsnNode> candidate : result.candidates) {
                  if (candidate.getInsn().getOpcode() == 186) {
                     if (!candidate.getOwner().equals(this.mixin.getTargetClassRef())) {
                        throw new InvalidInjectionException(this.context, String.format("%s, failed to select into child. Cannot select foreign method: %s. %s", this.context.getElementDescription(), candidate, this.mixin.getReferenceMapper().getStatus()));
                     }

                     MethodNode method = this.findMethod(candidate);
                     if (method == null) {
                        throw new InvalidInjectionException(this.context, String.format("%s, failed to select into child. %s%s was not found in the target class.", this.context.getElementDescription(), candidate.getName(), candidate.getDesc()));
                     }

                     iter.add(new SelectedMethod(target, next, method));
                  }
               }
            }
         }
      } while(recursed);

   }

   private void checkTarget(MethodNode target) {
      AnnotationNode merged = Annotations.getVisible(target, MixinMerged.class);
      if (merged != null) {
         if (Annotations.getVisible(target, Final.class) != null) {
            throw new InvalidInjectionException(this.context, String.format("%s cannot inject into @Final method %s::%s%s merged by %s", this, this.mixin.getTargetClassName(), target.name, target.desc, Annotations.getValue(merged, "mixin")));
         }
      }
   }

   private MethodNode findMethod(ElementNode<AbstractInsnNode> searchFor) {
      for(MethodNode target : this.targetClassNode.methods) {
         if (target.name.equals(searchFor.getSyntheticName()) && target.desc.equals(searchFor.getDesc())) {
            return target;
         }
      }

      return null;
   }

   public void validate(int expectedCallbackCount, int requiredCallbackCount) {
      int targetCount = this.targets.size();
      if (targetCount <= 0) {
         if (this.mixin.getOption(MixinEnvironment.Option.DEBUG_INJECTORS) && expectedCallbackCount > 0) {
            throw new InvalidInjectionException(this.context, String.format("Injection validation failed: %s could not find any targets matching %s in %s. %s%s", this.context.getElementDescription(), namesOf(this.selectors), this.mixin.getTargetClassRef(), this.mixin.getReferenceMapper().getStatus(), AnnotatedMethodInfo.getDynamicInfo(this.method)));
         } else if (requiredCallbackCount > 0) {
            throw new InvalidInjectionException(this.context, String.format("Critical injection failure: %s could not find any targets matching %s in %s. %s%s", this.context.getElementDescription(), namesOf(this.selectors), this.mixin.getTargetClassRef(), this.mixin.getReferenceMapper().getStatus(), AnnotatedMethodInfo.getDynamicInfo(this.method)));
         }
      }
   }

   private static String namesOf(Collection<ITargetSelector> selectors) {
      int index = 0;
      int count = selectors.size();
      StringBuilder sb = new StringBuilder();

      for(ITargetSelector selector : selectors) {
         if (index > 0) {
            if (index == count - 1) {
               sb.append(" or ");
            } else {
               sb.append(", ");
            }
         }

         sb.append('\'').append(selector.toString()).append('\'');
         ++index;
      }

      return sb.toString();
   }

   public static class SelectedMethod {
      private final SelectedMethod parent;
      private final ITargetSelector selector;
      private final MethodNode method;

      SelectedMethod(SelectedMethod parent, ITargetSelector selector, MethodNode method) {
         this.parent = parent;
         this.selector = selector;
         this.method = method;
      }

      SelectedMethod(ITargetSelector selector, MethodNode method) {
         this((SelectedMethod)null, selector, method);
      }

      public String toString() {
         return this.method.name + this.method.desc;
      }

      public SelectedMethod getParent() {
         return this.parent;
      }

      public ITargetSelector next() {
         return this.selector.next();
      }

      public MethodNode getMethod() {
         return this.method;
      }
   }
}
