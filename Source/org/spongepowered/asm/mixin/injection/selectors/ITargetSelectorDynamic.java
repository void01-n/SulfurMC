package org.spongepowered.asm.mixin.injection.selectors;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public interface ITargetSelectorDynamic extends ITargetSelector {
   @Retention(RetentionPolicy.RUNTIME)
   @Target({ElementType.TYPE})
   public @interface SelectorAnnotation {
      Class<? extends Annotation> value();
   }

   @Retention(RetentionPolicy.RUNTIME)
   @Target({ElementType.TYPE})
   public @interface SelectorId {
      String namespace() default "";

      String value();
   }
}
