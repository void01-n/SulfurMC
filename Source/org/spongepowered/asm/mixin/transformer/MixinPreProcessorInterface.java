package org.spongepowered.asm.mixin.transformer;

import java.lang.reflect.Modifier;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidInterfaceMixinException;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;

class MixinPreProcessorInterface extends MixinPreProcessorStandard {
   MixinPreProcessorInterface(MixinInfo mixin, MixinInfo.MixinClassNode classNode) {
      super(mixin, classNode);
   }

   protected void prepareMethod(MixinInfo.MixinMethodNode mixinMethod, ClassInfo.Method method) {
      boolean isPublic = Bytecode.hasFlag((MethodNode)mixinMethod, 1);
      MixinEnvironment.Feature injectorsInInterfaceMixins = MixinEnvironment.Feature.INJECTORS_IN_INTERFACE_MIXINS;
      MixinEnvironment.CompatibilityLevel currentLevel = MixinEnvironment.getCompatibilityLevel();
      MixinEnvironment.CompatibilityLevel requiredLevelSynthetic = MixinEnvironment.CompatibilityLevel.requiredFor(2);
      if (!isPublic && mixinMethod.isSynthetic() && mixinMethod.isSynthetic()) {
         if (currentLevel.isLessThan(requiredLevelSynthetic)) {
            throw new InvalidInterfaceMixinException(this.mixin, String.format("Interface mixin contains a synthetic private method but compatibility level %s is required! Found %s in %s", requiredLevelSynthetic, method, this.mixin));
         }
      } else {
         if (!isPublic) {
            if ("<clinit>".equals(mixinMethod.name) && "()V".equals(mixinMethod.desc)) {
               return;
            }

            MixinEnvironment.CompatibilityLevel requiredLevelPrivate = MixinEnvironment.CompatibilityLevel.requiredFor(4);
            if (currentLevel.isLessThan(requiredLevelPrivate)) {
               throw new InvalidInterfaceMixinException(this.mixin, String.format("Interface mixin contains a private method but compatibility level %s is required! Found %s in %s", requiredLevelPrivate, method, this.mixin));
            }
         }

         AnnotationNode injectorAnnotation = InjectionInfo.getInjectorAnnotation(this.mixin, mixinMethod);
         if (injectorAnnotation == null) {
            super.prepareMethod(mixinMethod, method);
         } else if (injectorsInInterfaceMixins.isAvailable() && !injectorsInInterfaceMixins.isEnabled()) {
            throw new InvalidInterfaceMixinException(this.mixin, String.format("Interface mixin contains an injector but Feature.INJECTORS_IN_INTERFACE_MIXINS is disabled! Found %s in %s", method, this.mixin));
         } else {
            MixinEnvironment.CompatibilityLevel classLevel = MixinEnvironment.CompatibilityLevel.forClassVersion(this.mixin.getClassVersion());
            if (isPublic && !classLevel.supports(4) && classLevel.supports(2)) {
               Bytecode.setVisibility((MethodNode)mixinMethod, Bytecode.Visibility.PRIVATE);
               mixinMethod.access |= 4096;
            }

         }
      }
   }

   protected boolean validateField(MixinTargetContext context, FieldNode field, AnnotationNode shadow) {
      if (Bytecode.isStatic(field) && Bytecode.hasFlag((FieldNode)field, 1) && Bytecode.hasFlag((FieldNode)field, 16)) {
         if (shadow == null) {
            throw new InvalidInterfaceMixinException(this.mixin, String.format("Interface mixin %s contains a non-shadow field: %s", this.mixin, field.name));
         } else if (Annotations.getVisible(field, Mutable.class) != null) {
            throw new InvalidInterfaceMixinException(this.mixin, String.format("@Shadow field %s.%s is marked as mutable. This is not allowed.", this.mixin, field.name));
         } else {
            String prefix = (String)Annotations.getValue(shadow, "prefix", Shadow.class);
            if (field.name.startsWith(prefix)) {
               throw new InvalidMixinException(context, String.format("@Shadow field %s.%s has a shadow prefix. This is not allowed.", context, field.name));
            } else if ("super$".equals(field.name)) {
               throw new InvalidInterfaceMixinException(this.mixin, String.format("Interface mixin %s contains an imaginary super. This is not allowed", this.mixin));
            } else {
               return true;
            }
         }
      } else {
         throw new InvalidInterfaceMixinException(this.mixin, String.format("Interface mixin contains an illegal field! Found %s %s in %s", Modifier.toString(field.access), field.name, this.mixin));
      }
   }
}
