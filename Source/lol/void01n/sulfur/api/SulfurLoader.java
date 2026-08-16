package lol.void01n.sulfur.api;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Optional;
import lol.void01n.sulfur.classloader.SulfurClassLoader;
import lol.void01n.sulfur.mod.SulfurModContainer;
import lol.void01n.sulfur.mod.SulfurModRegistry;
import lol.void01n.sulfur.transformengine.SulfurTransformEngine;

public final class SulfurLoader {
   private static volatile SulfurLoader instance;
   private static final boolean DEBUG = System.getProperties().containsKey("sulfur.debug");
   private final SulfurClassLoader classLoader;
   private final SulfurTransformEngine transformEngine;
   private final SulfurModRegistry modRegistry;
   private final SulfurEnvironment environment;

   private SulfurLoader(SulfurClassLoader classLoader, SulfurTransformEngine transformEngine, SulfurModRegistry modRegistry, SulfurEnvironment environment) {
      this.classLoader = classLoader;
      this.transformEngine = transformEngine;
      this.modRegistry = modRegistry;
      this.environment = environment;
   }

   public static SulfurLoader init(SulfurClassLoader classLoader, SulfurTransformEngine transformEngine, SulfurModRegistry modRegistry, SulfurEnvironment environment) {
      if (instance != null) {
         throw new IllegalStateException("SulfurLoader.init() called more than once");
      } else {
         SulfurLoader loader = new SulfurLoader(classLoader, transformEngine, modRegistry, environment);
         instance = loader;
         SulfurEnvironment.set(environment);
         if (DEBUG) {
            PrintStream var10000 = System.out;
            String var10001 = String.valueOf(environment);
            var10000.println("sulfur/api: SulfurLoader initialized; env=" + var10001 + "; mods=" + modRegistry.size());
         }

         return loader;
      }
   }

   public static SulfurLoader getInstance() {
      SulfurLoader i = instance;
      if (i == null) {
         throw new IllegalStateException("SulfurLoader not yet initialized — call SulfurLoader.init() first (or don't call this from static initializers / pre-launch code)");
      } else {
         return i;
      }
   }

   public Collection<SulfurModContainer> getAllMods() {
      return this.modRegistry.getAllMods();
   }

   public Optional<SulfurModContainer> getMod(String modId) {
      return this.modRegistry.getMod(modId);
   }

   public boolean isModLoaded(String modId) {
      return this.modRegistry.isLoaded(modId);
   }

   public SulfurEnvironment getEnvironment() {
      return this.environment;
   }

   public SulfurClassLoader getClassLoader() {
      return this.classLoader;
   }

   public SulfurTransformEngine getTransformEngine() {
      return this.transformEngine;
   }

   public SulfurModRegistry getModRegistry() {
      return this.modRegistry;
   }
}
