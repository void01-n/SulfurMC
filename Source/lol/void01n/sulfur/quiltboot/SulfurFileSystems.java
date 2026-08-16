package lol.void01n.sulfur.quiltboot;

import java.nio.file.spi.FileSystemProvider;
import java.util.HashSet;
import java.util.Set;

public final class SulfurFileSystems {
   private static final boolean DEBUG = System.getProperties().containsKey("sulfur.debug");
   private static final Set<String> EXPECTED_SCHEMES = Set.of("union", "quilt.jfs", "quilt.mfs", "quilt.ufs", "quilt.zfs");
   private static volatile boolean registered = false;

   private SulfurFileSystems() {
   }

   public static synchronized void registerAll() {
      if (!registered) {
         verifyExpectedProviders();
         registered = true;
      }
   }

   private static void verifyExpectedProviders() {
      Set<String> found = new HashSet();

      for(FileSystemProvider provider : FileSystemProvider.installedProviders()) {
         found.add(provider.getScheme());
      }

      for(String expected : EXPECTED_SCHEMES) {
         if (found.contains(expected)) {
            if (DEBUG) {
               System.out.println("sulfur: filesystem provider present: '" + expected + "'");
            }
         } else {
            if (!"union".equals(expected)) {
               throw new IllegalStateException("Sulfur filesystem provider missing for scheme '" + expected + "'. Is sulfur-filesystem on the module path? (META-INF/services/java.nio.file.spi.FileSystemProvider should list it.)");
            }

            if (DEBUG) {
               System.out.println("sulfur: WARNING — 'union' scheme FileSystemProvider not found. Sulfur's jar-handling module (SecureJarHandler equivalent) is not yet on the module path. NeoForge jar-in-jar mods will not load correctly until this packaging requirement is met (spec.txt Section 7 \"RESOLVED\" block).");
            }
         }
      }

   }
}
