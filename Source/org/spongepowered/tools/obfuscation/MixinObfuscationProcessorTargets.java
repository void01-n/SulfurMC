package org.spongepowered.tools.obfuscation;

import java.util.Set;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.tools.obfuscation.interfaces.IMessagerEx;
import org.spongepowered.tools.obfuscation.mirror.AnnotationHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeUtils;

@SupportedAnnotationTypes({"org.spongepowered.asm.mixin.Mixin", "org.spongepowered.asm.mixin.Shadow", "org.spongepowered.asm.mixin.Overwrite", "org.spongepowered.asm.mixin.gen.Accessor", "org.spongepowered.asm.mixin.Implements"})
public class MixinObfuscationProcessorTargets extends MixinObfuscationProcessor {
   public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
      if (roundEnv.processingOver()) {
         this.postProcess(roundEnv);
         return true;
      } else {
         this.processMixins(roundEnv);
         this.processShadows(roundEnv);
         this.processOverwrites(roundEnv);
         this.processAccessors(roundEnv);
         this.processInvokers(roundEnv);
         this.processImplements(roundEnv);
         this.postProcess(roundEnv);
         return true;
      }
   }

   protected void postProcess(RoundEnvironment roundEnv) {
      super.postProcess(roundEnv);

      try {
         this.mixins.writeReferences();
         this.mixins.writeMappings();
      } catch (Exception ex) {
         ex.printStackTrace();
      }

   }

   private void processShadows(RoundEnvironment roundEnv) {
      for(Element elem : roundEnv.getElementsAnnotatedWith(Shadow.class)) {
         Element parent = elem.getEnclosingElement();
         if (!(parent instanceof TypeElement)) {
            this.mixins.printMessage((IMessagerEx.MessageType)IMessagerEx.MessageType.ERROR, "Unexpected parent with type " + TypeUtils.getElementType(parent), elem);
         } else {
            AnnotationHandle shadow = AnnotationHandle.of(elem, Shadow.class);
            if (elem.getKind() == ElementKind.FIELD) {
               this.mixins.registerShadow((TypeElement)parent, (VariableElement)elem, shadow);
            } else if (elem.getKind() != ElementKind.METHOD && elem.getKind() != ElementKind.CONSTRUCTOR) {
               this.mixins.printMessage((IMessagerEx.MessageType)IMessagerEx.MessageType.SHADOW_ON_INVALID_ELEMENT, "Element is not a method or field", elem);
            } else {
               this.mixins.registerShadow((TypeElement)parent, (ExecutableElement)elem, shadow);
            }
         }
      }

   }

   private void processOverwrites(RoundEnvironment roundEnv) {
      for(Element elem : roundEnv.getElementsAnnotatedWith(Overwrite.class)) {
         Element parent = elem.getEnclosingElement();
         if (!(parent instanceof TypeElement)) {
            this.mixins.printMessage((IMessagerEx.MessageType)IMessagerEx.MessageType.ERROR, "Unexpected parent with type " + TypeUtils.getElementType(parent), elem);
         } else if (elem.getKind() == ElementKind.METHOD) {
            this.mixins.registerOverwrite((TypeElement)parent, (ExecutableElement)elem);
         } else {
            this.mixins.printMessage((IMessagerEx.MessageType)IMessagerEx.MessageType.OVERWRITE_ON_NON_METHOD_ELEMENT, "Element is not a method", elem);
         }
      }

   }

   private void processAccessors(RoundEnvironment roundEnv) {
      for(Element elem : roundEnv.getElementsAnnotatedWith(Accessor.class)) {
         Element parent = elem.getEnclosingElement();
         if (!(parent instanceof TypeElement)) {
            this.mixins.printMessage((IMessagerEx.MessageType)IMessagerEx.MessageType.ERROR, "Unexpected parent with type " + TypeUtils.getElementType(parent), elem);
         } else if (elem.getKind() == ElementKind.METHOD) {
            this.mixins.registerAccessor((TypeElement)parent, (ExecutableElement)elem);
         } else {
            this.mixins.printMessage((IMessagerEx.MessageType)IMessagerEx.MessageType.ACCESSOR_ON_NON_METHOD_ELEMENT, "Element is not a method", elem);
         }
      }

   }

   private void processInvokers(RoundEnvironment roundEnv) {
      for(Element elem : roundEnv.getElementsAnnotatedWith(Invoker.class)) {
         Element parent = elem.getEnclosingElement();
         if (!(parent instanceof TypeElement)) {
            this.mixins.printMessage((IMessagerEx.MessageType)IMessagerEx.MessageType.ERROR, "Unexpected parent with type " + TypeUtils.getElementType(parent), elem);
         } else if (elem.getKind() == ElementKind.METHOD) {
            this.mixins.registerInvoker((TypeElement)parent, (ExecutableElement)elem);
         } else {
            this.mixins.printMessage((IMessagerEx.MessageType)IMessagerEx.MessageType.ACCESSOR_ON_NON_METHOD_ELEMENT, "Element is not a method", elem);
         }
      }

   }

   private void processImplements(RoundEnvironment roundEnv) {
      for(Element elem : roundEnv.getElementsAnnotatedWith(Implements.class)) {
         if (elem.getKind() != ElementKind.CLASS && elem.getKind() != ElementKind.INTERFACE) {
            this.mixins.printMessage((IMessagerEx.MessageType)IMessagerEx.MessageType.SOFT_IMPLEMENTS_ON_INVALID_TYPE, "Found an @Implements annotation on an element which is not a class or interface", elem);
         } else {
            AnnotationHandle implementsAnnotation = AnnotationHandle.of(elem, Implements.class);
            this.mixins.registerSoftImplements((TypeElement)elem, implementsAnnotation);
         }
      }

   }
}
