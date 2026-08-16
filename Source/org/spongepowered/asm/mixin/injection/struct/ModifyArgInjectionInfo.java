package org.spongepowered.asm.mixin.injection.struct;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.invoke.ModifyArgInjector;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.Annotations;

@InjectionInfo.AnnotationType(ModifyArg.class)
@InjectionInfo.HandlerPrefix("modify")
@InjectionInfo.InjectorOrder(1000)
public class ModifyArgInjectionInfo extends InjectionInfo {
   public ModifyArgInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
      super(mixin, method, annotation);
   }

   protected Injector parseInjector(AnnotationNode injectAnnotation) {
      int index = (Integer)Annotations.getValue(injectAnnotation, "index", -1);
      return new ModifyArgInjector(this, index);
   }

   protected String getDescription() {
      return "Argument modifier method";
   }
}
