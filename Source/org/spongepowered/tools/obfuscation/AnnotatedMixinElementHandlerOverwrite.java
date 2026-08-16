package org.spongepowered.tools.obfuscation;

import java.lang.reflect.Method;
import java.util.Locale;
import javax.lang.model.element.ExecutableElement;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.interfaces.IMessagerEx;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.mirror.AnnotationHandle;
import org.spongepowered.tools.obfuscation.mirror.MethodHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;

class AnnotatedMixinElementHandlerOverwrite extends AnnotatedMixinElementHandler {
   AnnotatedMixinElementHandlerOverwrite(IMixinAnnotationProcessor ap, AnnotatedMixin mixin) {
      super(ap, mixin);
   }

   public void registerMerge(MethodHandle method) {
      if (!method.isImaginary()) {
         this.validateTargetMethod(method.getElement(), (AnnotationHandle)null, new AnnotatedMixinElementHandler.AliasedElementName(method, AnnotationHandle.MISSING), "overwrite", true, true);
      }

   }

   public void registerOverwrite(AnnotatedElementOverwrite elem) {
      AnnotatedMixinElementHandler.AliasedElementName name = new AnnotatedMixinElementHandler.AliasedElementName(elem.getElement(), elem.getAnnotation());
      this.validateTargetMethod((ExecutableElement)elem.getElement(), elem.getAnnotation(), name, "@Overwrite", true, false);
      this.checkConstraints((ExecutableElement)elem.getElement(), elem.getAnnotation());
      if (elem.shouldRemap()) {
         for(TypeHandle target : this.mixin.getTargets()) {
            if (!this.registerOverwriteForTarget(elem, target)) {
               return;
            }
         }
      }

      if (!"true".equalsIgnoreCase(this.ap.getOption("disableOverwriteChecker"))) {
         String javadoc = this.ap.getJavadocProvider().getJavadoc(elem.getElement());
         if (javadoc == null) {
            this.ap.printMessage(IMessagerEx.MessageType.OVERWRITE_DOCS, "@Overwrite is missing javadoc comment", elem.getElement(), SuppressedBy.OVERWRITE);
            return;
         }

         if (!javadoc.toLowerCase(Locale.ROOT).contains("@author")) {
            this.ap.printMessage(IMessagerEx.MessageType.OVERWRITE_DOCS, "@Overwrite is missing an @author tag", elem.getElement(), SuppressedBy.OVERWRITE);
         }

         if (!javadoc.toLowerCase(Locale.ROOT).contains("@reason")) {
            this.ap.printMessage(IMessagerEx.MessageType.OVERWRITE_DOCS, "@Overwrite is missing an @reason tag", elem.getElement(), SuppressedBy.OVERWRITE);
         }
      }

   }

   private boolean registerOverwriteForTarget(AnnotatedElementOverwrite elem, TypeHandle target) {
      MappingMethod targetMethod = target.getMappingMethod(elem.getSimpleName(), elem.getDesc());
      ObfuscationData<MappingMethod> obfData = this.obf.getDataProvider().getObfMethod(targetMethod);
      if (obfData.isEmpty()) {
         IMessagerEx.MessageType messageType = IMessagerEx.MessageType.NO_OBFDATA_FOR_OVERWRITE;

         try {
            Method md = ((ExecutableElement)elem.getElement()).getClass().getMethod("isStatic");
            if ((Boolean)md.invoke(elem.getElement())) {
               messageType = IMessagerEx.MessageType.NO_OBFDATA_FOR_STATIC_OVERWRITE;
            }
         } catch (Exception var7) {
         }

         this.ap.printMessage(messageType, "Unable to locate obfuscation mapping for @Overwrite method", elem.getElement());
         return false;
      } else {
         try {
            this.addMethodMappings(elem.getSimpleName(), elem.getDesc(), obfData);
            return true;
         } catch (Mappings.MappingConflictException ex) {
            elem.printMessage(this.ap, IMessagerEx.MessageType.OVERWRITE_MAPPING_CONFLICT, "Mapping conflict for @Overwrite method: " + ex.getNew().getSimpleName() + " for target " + target + " conflicts with existing mapping " + ex.getOld().getSimpleName());
            return false;
         }
      }
   }

   static class AnnotatedElementOverwrite extends AnnotatedMixinElementHandler.AnnotatedElement<ExecutableElement> {
      private final boolean shouldRemap;

      public AnnotatedElementOverwrite(ExecutableElement element, AnnotationHandle annotation, boolean shouldRemap) {
         super(element, annotation);
         this.shouldRemap = shouldRemap;
      }

      public boolean shouldRemap() {
         return this.shouldRemap;
      }
   }
}
