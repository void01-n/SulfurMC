package org.spongepowered.asm.mixin.injection.selectors;

import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.util.asm.IAnnotationHandle;

public interface ISelectorContext {
   ISelectorContext getParent();

   IMixinContext getMixin();

   Object getMethod();

   IAnnotationHandle getAnnotation();

   IAnnotationHandle getSelectorAnnotation();

   String getSelectorCoordinate(boolean var1);

   String remap(String var1);

   String getElementDescription();
}
