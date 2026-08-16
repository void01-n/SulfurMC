package lol.void01n.sulfur.mod;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class SulfurModsDirectory {
   private static final String MODS_DIR_PROPERTY = "sulfur.modsDir";
   private static final String GAME_DIR_PROPERTY = "sulfur.gameDir";

   private SulfurModsDirectory() {
   }

   public static Path resolve() {
      String override = System.getProperty("sulfur.modsDir");
      if (override != null) {
         return Paths.get(override);
      } else {
         String gameDir = System.getProperty("sulfur.gameDir");
         return gameDir != null && !gameDir.isBlank() ? Paths.get(gameDir, "mods") : Paths.get("mods");
      }
   }

   public static Path resolveGameDir() {
      String gameDir = System.getProperty("sulfur.gameDir");
      return gameDir != null && !gameDir.isBlank() ? Paths.get(gameDir) : Paths.get("").toAbsolutePath();
   }
}
