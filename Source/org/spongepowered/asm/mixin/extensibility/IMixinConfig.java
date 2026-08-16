package org.spongepowered.asm.mixin.extensibility;

import java.util.Set;
import org.spongepowered.asm.mixin.MixinEnvironment;

public interface IMixinConfig {
   int DEFAULT_PRIORITY = 1000;

   MixinEnvironment getEnvironment();

   String getName();

   IMixinConfigSource getSource();

   String getCleanSourceId();

   String getMixinPackage();

   int getPriority();

   IMixinConfigPlugin getPlugin();

   boolean isRequired();

   Set<String> getTargets();

   <V> void decorate(String var1, V var2);

   boolean hasDecoration(String var1);

   <V> V getDecoration(String var1);
}
