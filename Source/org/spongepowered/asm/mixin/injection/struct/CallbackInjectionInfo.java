package org.spongepowered.asm.mixin.injection.struct;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInjector;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.include.com.google.common.base.Strings;

@InjectionInfo.AnnotationType(Inject.class)
@InjectionInfo.InjectorOrder(1000)
public class CallbackInjectionInfo extends InjectionInfo {
   protected CallbackInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
      super(mixin, method, annotation);
   }

   protected Injector parseInjector(AnnotationNode injectAnnotation) {
      boolean cancellable = (Boolean)Annotations.getValue(injectAnnotation, "cancellable", Boolean.FALSE);
      LocalCapture locals = (LocalCapture)Annotations.getValue(injectAnnotation, "locals", LocalCapture.class, LocalCapture.NO_CAPTURE);
      String identifier = (String)Annotations.getValue(injectAnnotation, "id", "");
      return new CallbackInjector(this, cancellable, locals, identifier);
   }

   public String getSliceId(String id) {
      return Strings.nullToEmpty(id);
   }
}
