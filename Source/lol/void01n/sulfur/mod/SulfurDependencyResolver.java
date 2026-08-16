package lol.void01n.sulfur.mod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SulfurDependencyResolver {
   private static final boolean DEBUG = System.getProperties().containsKey("sulfur.debug");
   public static final String SULFUR_VERSION = "1.0.0-dev";
   public static final String MINECRAFT_VERSION = "1.21.4";

   private SulfurDependencyResolver() {
   }

   public static List<String> checkDependencies(SulfurModContainer mod, SulfurModRegistry registry) {
      return checkDependencies(mod, mod.dependencies, registry);
   }

   public static List<String> checkDependencies(SulfurModContainer mod, Map<String, String> deps, SulfurModRegistry registry) {
      List<String> unsatisfied = new ArrayList();

      for(Map.Entry<String, String> dep : deps.entrySet()) {
         String depId = (String)dep.getKey();
         String constraint = (String)dep.getValue();
         if (constraint == null) {
            constraint = "*";
         }

         String resolvedVersion = resolveBuiltinVersion(depId);
         if (resolvedVersion == null) {
            Optional<SulfurModContainer> container = registry.getMod(depId);
            if (container.isEmpty()) {
               unsatisfied.add("missing dependency '" + depId + "' (constraint: '" + constraint + "')");
               continue;
            }

            resolvedVersion = ((SulfurModContainer)container.get()).version;
         }

         if (!satisfies(resolvedVersion, constraint)) {
            unsatisfied.add("dependency '" + depId + "' found v" + resolvedVersion + " but constraint requires '" + constraint + "'");
         } else if (DEBUG) {
            System.out.println("sulfur/deps: [" + mod.id + "] ✓ " + depId + "@" + resolvedVersion + " satisfies '" + constraint + "'");
         }
      }

      return unsatisfied;
   }

   public static int validateAll(SulfurModRegistry registry) {
      Collection<SulfurModContainer> allMods = registry.getAllMods();
      int total = 0;

      for(SulfurModContainer mod : allMods) {
         if (!mod.dependencies.isEmpty()) {
            List<String> problems = checkDependencies(mod, registry);
            if (!problems.isEmpty()) {
               String var10001 = mod.id;
               System.err.println("sulfur/deps: mod '" + var10001 + "' has " + problems.size() + " unsatisfied dep(s):");

               for(String p : problems) {
                  System.err.println("  - " + p);
               }

               total += problems.size();
            }
         }
      }

      if (DEBUG && total == 0) {
         System.out.println("sulfur/deps: all dependency constraints satisfied (" + allMods.size() + " mod(s) checked)");
      }

      return total;
   }

   public static boolean satisfies(String resolvedVersion, String constraint) {
      if (constraint != null && !constraint.isBlank() && !"*".equals(constraint.trim())) {
         constraint = constraint.trim();
         if (constraint.startsWith(">=")) {
            return compareVersions(resolvedVersion, constraint.substring(2).trim()) >= 0;
         } else if (constraint.startsWith(">")) {
            return compareVersions(resolvedVersion, constraint.substring(1).trim()) > 0;
         } else if (constraint.startsWith("<=")) {
            return compareVersions(resolvedVersion, constraint.substring(2).trim()) <= 0;
         } else if (constraint.startsWith("<")) {
            return compareVersions(resolvedVersion, constraint.substring(1).trim()) < 0;
         } else if (!constraint.startsWith("~")) {
            if (constraint.contains(" ")) {
               if (DEBUG) {
                  System.out.println("sulfur/deps: WARNING — compound constraint '" + constraint + "' not yet fully supported (FlexVer TODO); treating as wildcard match");
               }

               return true;
            } else {
               return resolvedVersion.equals(constraint);
            }
         } else {
            String prefix = constraint.substring(1).trim();
            int lastDot = prefix.lastIndexOf(46);
            if (lastDot > 0) {
               String majorMinor = prefix.substring(0, lastDot);
               return resolvedVersion.startsWith(majorMinor + ".") || resolvedVersion.equals(majorMinor);
            } else {
               return resolvedVersion.startsWith(prefix + ".") || resolvedVersion.equals(prefix);
            }
         }
      } else {
         return true;
      }
   }

   private static String resolveBuiltinVersion(String depId) {
      String var10000;
      switch (depId.toLowerCase()) {
         case "sulfur":
            var10000 = "1.0.0-dev";
            break;
         case "minecraft":
            var10000 = "1.21.4";
            break;
         case "fabricloader":
         case "fabric-loader":
         case "fabric":
            var10000 = "1.0.0-dev";
            break;
         case "quiltloader":
         case "quilt-loader":
         case "quilt":
            var10000 = "1.0.0-dev";
            break;
         case "java":
            var10000 = String.valueOf(Runtime.version().feature());
            break;
         default:
            var10000 = null;
      }

      return var10000;
   }

   static int compareVersions(String a, String b) {
      if (a == null) {
         a = "0";
      }

      if (b == null) {
         b = "0";
      }

      if (a.startsWith("v") || a.startsWith("V")) {
         a = a.substring(1);
      }

      if (b.startsWith("v") || b.startsWith("V")) {
         b = b.substring(1);
      }

      String aRelease = a;
      String aSuffix = "";
      String bRelease = b;
      String bSuffix = "";
      int aDash = a.indexOf(45);
      if (aDash >= 0) {
         aRelease = a.substring(0, aDash);
         aSuffix = a.substring(aDash);
      }

      int bDash = b.indexOf(45);
      if (bDash >= 0) {
         bRelease = b.substring(0, bDash);
         bSuffix = b.substring(bDash);
      }

      String[] aParts = aRelease.split("\\.", -1);
      String[] bParts = bRelease.split("\\.", -1);
      int len = Math.max(aParts.length, bParts.length);

      for(int i = 0; i < len; ++i) {
         String ap = i < aParts.length ? aParts[i] : "0";
         String bp = i < bParts.length ? bParts[i] : "0";

         int cmp;
         try {
            cmp = Integer.compare(Integer.parseInt(ap), Integer.parseInt(bp));
         } catch (NumberFormatException var16) {
            cmp = ap.compareTo(bp);
         }

         if (cmp != 0) {
            return cmp;
         }
      }

      if (aSuffix.isEmpty() && !bSuffix.isEmpty()) {
         return 1;
      } else if (!aSuffix.isEmpty() && bSuffix.isEmpty()) {
         return -1;
      } else {
         return aSuffix.compareTo(bSuffix);
      }
   }
}
