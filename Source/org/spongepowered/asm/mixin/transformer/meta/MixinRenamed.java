package org.spongepowered.asm.mixin.transformer.meta;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface MixinRenamed {
   String originalName();

   boolean isInterfaceMember();
}
