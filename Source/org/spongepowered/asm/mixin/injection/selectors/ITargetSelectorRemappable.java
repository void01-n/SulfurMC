package org.spongepowered.asm.mixin.injection.selectors;

import org.spongepowered.asm.obfuscation.mapping.IMapping;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;

public interface ITargetSelectorRemappable extends ITargetSelectorByName {
   boolean isFullyQualified();

   boolean isField();

   boolean isConstructor();

   boolean isClassInitialiser();

   boolean isInitialiser();

   IMapping<?> asMapping();

   MappingMethod asMethodMapping();

   MappingField asFieldMapping();

   ITargetSelectorRemappable move(String var1);

   ITargetSelectorRemappable transform(String var1);

   ITargetSelectorRemappable remapUsing(MappingMethod var1, boolean var2);
}
