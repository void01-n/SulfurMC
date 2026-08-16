package org.spongepowered.tools.obfuscation.ext;

import java.util.HashSet;
import java.util.Set;

public final class SpecialPackages {
   private static final Set<String> suppressWarningsForPackages = new HashSet();

   private SpecialPackages() {
   }

   public static final void addExcludedPackage(String packageName) {
      String internalName = packageName.replace('.', '/');
      if (!internalName.endsWith("/")) {
         internalName = internalName + "/";
      }

      suppressWarningsForPackages.add(internalName);
   }

   public static boolean isExcludedPackage(String internalName) {
      for(String prefix : suppressWarningsForPackages) {
         if (internalName.startsWith(prefix)) {
            return true;
         }
      }

      return false;
   }

   static {
      addExcludedPackage("java.");
      addExcludedPackage("javax.");
      addExcludedPackage("sun.");
      addExcludedPackage("com.sun.");
   }
}
