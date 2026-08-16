package lol.void01n.sulfur.mod;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class SulfurModRegistry {
   private static final boolean DEBUG = System.getProperties().containsKey("sulfur.debug");
   private static final SulfurModRegistry INSTANCE = new SulfurModRegistry();
   private final ConcurrentHashMap<String, SulfurModContainer> mods = new ConcurrentHashMap();

   private SulfurModRegistry() {
   }

   public static SulfurModRegistry getInstance() {
      return INSTANCE;
   }

   public boolean register(SulfurModContainer mod) {
      SulfurModContainer existing = (SulfurModContainer)this.mods.putIfAbsent(mod.id, mod);
      if (existing != null) {
         String var10001 = mod.id;
         System.err.println("sulfur/registry: WARNING — duplicate mod ID '" + var10001 + "' from ecosystem '" + mod.ecosystem + "' (jar: " + String.valueOf(mod.jars.isEmpty() ? "none" : ((Path)mod.jars.get(0)).getFileName()) + "); already registered from ecosystem '" + existing.ecosystem + "'. Keeping first registration, skipping duplicate.");
         return false;
      } else {
         if (DEBUG) {
            System.out.println("sulfur/registry: registered mod '" + mod.id + "' v" + mod.version + " [" + mod.ecosystem + "] — " + mod.displayName);
         }

         return true;
      }
   }

   public SulfurModContainer unregister(String modId) {
      SulfurModContainer removed = (SulfurModContainer)this.mods.remove(modId);
      if (removed != null) {
         System.out.println("sulfur/registry: unregistered mod '" + modId + "' (jar removed). Already-loaded classes from this mod remain in the classloader until JVM restart.");
      }

      return removed;
   }

   public Optional<SulfurModContainer> getMod(String modId) {
      return Optional.ofNullable((SulfurModContainer)this.mods.get(modId));
   }

   public boolean isLoaded(String modId) {
      return this.mods.containsKey(modId);
   }

   public Collection<SulfurModContainer> getAllMods() {
      return Collections.unmodifiableCollection(this.mods.values());
   }

   public int size() {
      return this.mods.size();
   }

   public void dumpToLog() {
      System.out.println("sulfur/registry: " + this.mods.size() + " mod(s) registered:");

      for(SulfurModContainer mod : this.mods.values()) {
         System.out.println("  [" + mod.ecosystem + "] " + mod.id + " v" + mod.version + " (" + mod.displayName + ")");
      }

   }
}
