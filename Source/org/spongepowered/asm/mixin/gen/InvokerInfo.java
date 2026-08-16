package org.spongepowered.asm.mixin.gen;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.gen.throwables.InvalidAccessorException;
import org.spongepowered.asm.mixin.injection.selectors.ElementNode;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorByName;
import org.spongepowered.asm.mixin.injection.selectors.TargetSelector;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.asm.MethodNodeEx;

class InvokerInfo extends AccessorInfo {
   InvokerInfo(MixinTargetContext mixin, MethodNode method) {
      super(mixin, method, Invoker.class);
   }

   protected AccessorInfo.AccessorType initType() {
      if (this.specifiedName != null) {
         String mappedReference = this.mixin.getReferenceMapper().remap(this.mixin.getClassRef(), this.specifiedName);
         return this.initType(mappedReference.replace('.', '/'), this.mixin.getTargetClassRef());
      } else {
         AccessorInfo.AccessorName accessorName = AccessorInfo.AccessorName.of(MethodNodeEx.getName(this.method), false);
         if (accessorName != null) {
            for(String prefix : AccessorInfo.AccessorType.OBJECT_FACTORY.getExpectedPrefixes()) {
               if (prefix.equals(accessorName.prefix)) {
                  return this.initType(accessorName.name, this.mixin.getTargetClassInfo().getSimpleName());
               }
            }
         }

         return AccessorInfo.AccessorType.METHOD_PROXY;
      }
   }

   private AccessorInfo.AccessorType initType(String targetName, String targetClassName) {
      if (!"<init>".equals(targetName) && !targetClassName.equals(targetName)) {
         return AccessorInfo.AccessorType.METHOD_PROXY;
      } else if (!this.returnType.equals(this.mixin.getTargetClassInfo().getType())) {
         throw new InvalidAccessorException(this.mixin, String.format("%s appears to have an invalid return type. %s requires matching return type. Found %s expected %s", this, AccessorInfo.AccessorType.OBJECT_FACTORY, Bytecode.getSimpleName(this.returnType), this.mixin.getTargetClassInfo().getSimpleName()));
      } else if (!this.isStatic) {
         throw new InvalidAccessorException(this.mixin, String.format("%s for %s must be static", this, AccessorInfo.AccessorType.OBJECT_FACTORY, Bytecode.getSimpleName(this.returnType)));
      } else {
         return AccessorInfo.AccessorType.OBJECT_FACTORY;
      }
   }

   protected Type initTargetFieldType() {
      return null;
   }

   protected ITargetSelector initTarget() {
      return this.type == AccessorInfo.AccessorType.OBJECT_FACTORY ? new MemberInfo("<init>", (String)null, Bytecode.changeDescriptorReturnType(this.method.desc, "V")) : new MemberInfo(this.getTargetName(this.specifiedName), (String)null, this.method.desc);
   }

   public void locate() {
      this.targetMethod = this.findTargetMethod();
   }

   private MethodNode findTargetMethod() {
      TargetSelector.Result<MethodNode> result = TargetSelector.run(this.target.configure(ITargetSelector.Configure.ORPHAN), ElementNode.methodList(this.classNode));

      try {
         return result.getSingleResult(true);
      } catch (IllegalStateException ex) {
         String message = String.format("%s matching %s in %s for %s", ex.getMessage(), this.target, this.classNode.name, this);
         if (this.type == AccessorInfo.AccessorType.METHOD_PROXY && this.specifiedName != null && this.target instanceof ITargetSelectorByName) {
            String name = ((ITargetSelectorByName)this.target).getName();
            if (name != null && (name.contains(".") || name.contains("/"))) {
               throw new InvalidAccessorException(this, "Invalid factory invoker failed to match the target class. " + message);
            }
         }

         throw new InvalidAccessorException(this, message);
      }
   }
}
