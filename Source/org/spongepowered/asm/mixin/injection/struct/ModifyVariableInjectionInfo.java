package org.spongepowered.asm.mixin.injection.struct;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.modify.LocalVariableDiscriminator;
import org.spongepowered.asm.mixin.injection.modify.ModifyVariableInjector;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;

@InjectionInfo.AnnotationType(ModifyVariable.class)
@InjectionInfo.HandlerPrefix("localvar")
@InjectionInfo.InjectorOrder(1000)
public class ModifyVariableInjectionInfo extends InjectionInfo {
   public ModifyVariableInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
      super(mixin, method, annotation);
   }

   protected Injector parseInjector(AnnotationNode injectAnnotation) {
      return new ModifyVariableInjector(this, LocalVariableDiscriminator.parse(injectAnnotation));
   }

   protected String getDescription() {
      return "Variable modifier method";
   }
}
