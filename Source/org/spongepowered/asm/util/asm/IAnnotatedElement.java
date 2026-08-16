package org.spongepowered.asm.util.asm;

import java.lang.annotation.Annotation;

public interface IAnnotatedElement {
   IAnnotationHandle getAnnotation(Class<? extends Annotation> var1);
}
