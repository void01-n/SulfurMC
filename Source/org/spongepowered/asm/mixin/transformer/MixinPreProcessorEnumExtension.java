package org.spongepowered.asm.mixin.transformer;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;

class MixinPreProcessorEnumExtension extends MixinPreProcessorStandard {
   private static final List<Class<? extends Annotation>> DISALLOWED_ANNOTATIONS_ON_CONSTANTS = Arrays.asList(Shadow.class, Unique.class);

   MixinPreProcessorEnumExtension(MixinInfo mixin, MixinInfo.MixinClassNode classNode) {
      super(mixin, classNode);
   }

   protected void prepareShadow(MixinInfo.MixinMethodNode mixinMethod, ClassInfo.Method method) {
      if ("<init>".equals(mixinMethod.name) && !Bytecode.hasFlag((MethodNode)mixinMethod, 4096)) {
         Annotations.setVisible((MethodNode)mixinMethod, Shadow.class);
      }

      super.prepareShadow(mixinMethod, method);
   }

   protected boolean validateField(MixinTargetContext context, FieldNode field, AnnotationNode shadow) {
      if (Bytecode.isEnumConstant(field, this.classNode)) {
         for(Class<? extends Annotation> annotation : DISALLOWED_ANNOTATIONS_ON_CONSTANTS) {
            if (Annotations.getVisible(field, annotation) != null) {
               throw new InvalidMixinException(context, String.format("Enum constant %s in %s has @%s annotation. This is not allowed.", field.name, context, annotation.getSimpleName()));
            }
         }

         return true;
      } else {
         return super.validateField(context, field, shadow);
      }
   }

   protected boolean validateMethod(MixinTargetContext context, MixinInfo.MixinMethodNode mixinMethod) {
      String mixinClassDesc = 'L' + this.mixin.getClassRef() + ';';
      if (mixinMethod.name.equals("values") && mixinMethod.desc.equals("()[" + mixinClassDesc)) {
         return false;
      } else {
         return mixinMethod.name.equals("valueOf") && mixinMethod.desc.equals("(Ljava/lang/String;)" + mixinClassDesc) ? false : super.validateMethod(context, mixinMethod);
      }
   }
}
