package org.spongepowered.asm.mixin.refmap;

public interface IClassReferenceMapper {
   String remapClassName(String var1, String var2);

   String remapClassNameWithContext(String var1, String var2, String var3);
}
