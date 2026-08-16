package org.spongepowered.tools.obfuscation.fg3;

import java.util.Collection;
import java.util.Set;
import org.spongepowered.include.com.google.common.collect.ImmutableList;
import org.spongepowered.include.com.google.common.collect.ImmutableSet;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.service.IObfuscationService;
import org.spongepowered.tools.obfuscation.service.ObfuscationTypeDescriptor;

public class ObfuscationServiceFG3 implements IObfuscationService {
   public static final String SEARGE = "searge";
   public static final String REOBF_TSRG_FILE = "reobfTsrgFile";
   public static final String REOBF_EXTRA_TSRG_FILES = "reobfTsrgFiles";
   public static final String OUT_TSRG_SRG_FILE = "outTsrgFile";
   public static final String TSRG_OUTPUT_BEHAVIOUR = "mergeBehaviour";

   public Set<String> getSupportedOptions() {
      return ImmutableSet.<String>of("reobfTsrgFile", "reobfTsrgFiles", "outTsrgFile", "mergeBehaviour");
   }

   public Collection<ObfuscationTypeDescriptor> getObfuscationTypes(IMixinAnnotationProcessor ap) {
      ImmutableList.Builder<ObfuscationTypeDescriptor> list = ImmutableList.<ObfuscationTypeDescriptor>builder();
      if (ap.getOptions("mappingTypes").contains("tsrg")) {
         list.add(new ObfuscationTypeDescriptor("searge", "reobfTsrgFile", "reobfTsrgFiles", "outTsrgFile", ObfuscationEnvironmentFG3.class));
      }

      return list.build();
   }
}
