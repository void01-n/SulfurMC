package org.spongepowered.tools.obfuscation.interfaces;

import java.util.Collection;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorRemappable;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.mapping.IMappingConsumer;

public interface IObfuscationEnvironment {
   MappingMethod getObfMethod(ITargetSelectorRemappable var1);

   MappingMethod getObfMethod(MappingMethod var1);

   MappingMethod getObfMethod(MappingMethod var1, boolean var2);

   MappingField getObfField(ITargetSelectorRemappable var1);

   MappingField getObfField(MappingField var1);

   MappingField getObfField(MappingField var1, boolean var2);

   String getObfClass(String var1);

   ITargetSelectorRemappable remapDescriptor(ITargetSelectorRemappable var1);

   String remapDescriptor(String var1);

   void writeMappings(Collection<IMappingConsumer> var1);
}
