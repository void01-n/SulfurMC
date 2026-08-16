package org.spongepowered.asm.mixin;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.spongepowered.asm.launch.GlobalProperties;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigSource;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.Config;
import org.spongepowered.asm.service.MixinService;

public final class Mixins {
   private static final ILogger logger = MixinService.getService().getLogger("mixin");
   private static final GlobalProperties.Keys CONFIGS_KEY;
   private static final Set<String> errorHandlers;
   private static final Set<String> registeredConfigs;

   private Mixins() {
   }

   public static void addConfigurations(String... configFiles) {
      addConfigurations(configFiles, (IMixinConfigSource)null);
   }

   public static void addConfigurations(String[] configFiles, IMixinConfigSource source) {
      MixinEnvironment fallback = MixinEnvironment.getDefaultEnvironment();

      for(String configFile : configFiles) {
         createConfiguration(configFile, fallback, source);
      }

   }

   public static void addConfiguration(String configFile) {
      addConfiguration(configFile, (IMixinConfigSource)null);
   }

   public static void addConfiguration(String configFile, IMixinConfigSource source) {
      createConfiguration(configFile, MixinEnvironment.getDefaultEnvironment(), source);
   }

   /** @deprecated */
   @Deprecated
   static void addConfiguration(String configFile, MixinEnvironment fallback) {
      createConfiguration(configFile, fallback, (IMixinConfigSource)null);
   }

   private static void createConfiguration(String configFile, MixinEnvironment fallback, IMixinConfigSource source) {
      Config config = null;

      try {
         config = Config.create(configFile, fallback, source);
      } catch (Exception ex) {
         logger.error("Error encountered reading mixin config " + configFile + ": " + ex.getClass().getName() + " " + ex.getMessage(), (Throwable)ex);
      }

      registerConfiguration(config);
   }

   private static void registerConfiguration(Config config) {
      if (config != null && !registeredConfigs.contains(config.getName())) {
         MixinEnvironment env = config.getEnvironment();
         if (env != null) {
            env.registerConfig(config.getName());
         }

         getConfigs().add(config);
         registeredConfigs.add(config.getName());
         Config parent = config.getParent();
         if (parent != null) {
            registerConfiguration(parent);
         }

      }
   }

   public static int getUnvisitedCount() {
      int count = 0;

      for(Config config : getConfigs()) {
         if (!config.isVisited()) {
            ++count;
         }
      }

      return count;
   }

   public static Set<Config> getConfigs() {
      Set<Config> mixinConfigs = (Set)GlobalProperties.get(CONFIGS_KEY);
      if (mixinConfigs == null) {
         mixinConfigs = new LinkedHashSet();
         GlobalProperties.put(CONFIGS_KEY, mixinConfigs);
      }

      return mixinConfigs;
   }

   public static Set<IMixinInfo> getMixinsForClass(String className) {
      ClassInfo classInfo = ClassInfo.fromCache(className);
      return classInfo != null ? classInfo.getAppliedMixins() : Collections.emptySet();
   }

   public static void registerErrorHandlerClass(String handlerName) {
      if (handlerName != null) {
         errorHandlers.add(handlerName);
      }

   }

   public static Set<String> getErrorHandlerClasses() {
      return Collections.unmodifiableSet(errorHandlers);
   }

   static {
      CONFIGS_KEY = GlobalProperties.Keys.of(GlobalProperties.Keys.CONFIGS + ".queue");
      errorHandlers = new LinkedHashSet();
      registeredConfigs = new HashSet();
   }
}
