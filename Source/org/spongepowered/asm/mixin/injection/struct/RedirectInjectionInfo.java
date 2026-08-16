package org.spongepowered.asm.mixin.injection.struct;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.invoke.RedirectInjector;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;

@InjectionInfo.AnnotationType(Redirect.class)
@InjectionInfo.HandlerPrefix("redirect")
@InjectionInfo.InjectorOrder(10000)
public class RedirectInjectionInfo extends InjectionInfo {
   public RedirectInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
      super(mixin, method, annotation);
   }

   protected Injector parseInjector(AnnotationNode injectAnnotation) {
      return new RedirectInjector(this);
   }

   protected String getDescription() {
      return "Redirector";
   }
}
