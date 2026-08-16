package lol.void01n.sulfur.ssl;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import lol.void01n.sulfur.api.SulfurEnvironment;
import lol.void01n.sulfur.api.SulfurLoader;

public final class SulfurFabricLoaderBridge {
   private SulfurFabricLoaderBridge() {
   }

   public static boolean isModLoaded(String id) {
      try {
         return SulfurLoader.getInstance().isModLoaded(id);
      } catch (Exception var2) {
         return false;
      }
   }

   public static Path getGameDir() {
      String override = System.getProperty("sulfur.gameDir");
      return Paths.get(override != null ? override : ".");
   }

   public static Path getConfigDir() {
      String override = System.getProperty("sulfur.configDir");
      return override != null ? Paths.get(override) : getGameDir().resolve("config");
   }

   public static boolean isDevelopmentEnvironment() {
      return System.getProperties().containsKey("sulfur.dev");
   }

   public static int getEnvironmentOrdinal() {
      try {
         SulfurEnvironment env = SulfurEnvironment.current();
         return env.isClient() ? 0 : 1;
      } catch (Exception var1) {
         return 0;
      }
   }

   public static String getModVersion(String id) {
      try {
         return (String)SulfurLoader.getInstance().getMod(id).map((m) -> m.version).orElse("0.0.0");
      } catch (Exception var2) {
         return "0.0.0";
      }
   }

   public static Optional<Object> getModContainer(String id) {
      return Optional.empty();
   }

   public static List<Object> getAllMods() {
      return List.of();
   }
}
