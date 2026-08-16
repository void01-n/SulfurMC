package org.spongepowered.tools.obfuscation.service;

import java.util.Collection;
import java.util.Set;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;

public interface IObfuscationService {
   Set<String> getSupportedOptions();

   Collection<ObfuscationTypeDescriptor> getObfuscationTypes(IMixinAnnotationProcessor var1);
}
