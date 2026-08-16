package org.spongepowered.asm.util.asm;

import java.lang.reflect.Field;
import java.util.jar.Attributes.Name;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.launch.platform.MainAttributes;
import org.spongepowered.asm.util.VersionNumber;

public final class ASM {
   private static int majorVersion = 5;
   private static int minorVersion = 0;
   private static int implMinorVersion = 0;
   private static int patchVersion = 0;
   private static String maxVersion = "FALLBACK";
   private static int maxClassVersion = 50;
   private static int maxClassMajorVersion = 50;
   private static int maxClassMinorVersion = 0;
   private static String maxJavaVersion = "V1.6";
   public static final int API_VERSION = detectVersion();

   private ASM() {
   }

   public static boolean isAtLeastVersion(int majorVersion) {
      return ASM.majorVersion >= majorVersion;
   }

   public static boolean isAtLeastVersion(int majorVersion, int minorVersion) {
      return ASM.majorVersion >= majorVersion && (ASM.majorVersion > majorVersion || implMinorVersion >= minorVersion);
   }

   public static boolean isAtLeastVersion(int majorVersion, int minorVersion, int patchVersion) {
      if (ASM.majorVersion != majorVersion) {
         return ASM.majorVersion > majorVersion;
      } else {
         return implMinorVersion >= minorVersion && (implMinorVersion > minorVersion || ASM.patchVersion >= patchVersion);
      }
   }

   public static int getApiVersionMajor() {
      return majorVersion;
   }

   public static int getApiVersionMinor() {
      return minorVersion;
   }

   public static String getApiVersionString() {
      return String.format("%d.%d", majorVersion, minorVersion);
   }

   public static String getVersionString() {
      return String.format("ASM %d.%d%s (%s)", majorVersion, implMinorVersion, patchVersion > 0 ? "." + patchVersion : "", maxVersion);
   }

   public static int getMaxSupportedClassVersion() {
      return maxClassVersion;
   }

   public static int getMaxSupportedClassVersionMajor() {
      return maxClassMajorVersion;
   }

   public static int getMaxSupportedClassVersionMinor() {
      return maxClassMinorVersion;
   }

   public static String getClassVersionString() {
      return String.format("Up to Java %s (class file version %d.%d)", maxJavaVersion, maxClassMajorVersion, maxClassMinorVersion);
   }

   private static int detectVersion() {
      int apiVersion = 262144;
      VersionNumber packageVersion = getPackageVersion(Opcodes.class);

      for(Field field : Opcodes.class.getDeclaredFields()) {
         if (field.getType() == Integer.TYPE) {
            try {
               String name = field.getName();
               int version = field.getInt((Object)null);
               if (name.startsWith("ASM")) {
                  int minor = version >> 8 & 255;
                  int major = version >> 16 & 255;
                  boolean experimental = (version >> 24 & 255) != 0;
                  if (major >= majorVersion) {
                     maxVersion = name;
                     if (!experimental) {
                        apiVersion = version;
                        majorVersion = major;
                        implMinorVersion = minor;
                        minorVersion = minor;
                        if (packageVersion.getMajor() == major && minor == 0) {
                           implMinorVersion = packageVersion.getMinor();
                           patchVersion = packageVersion.getPatch();
                        }
                     }
                  }
               } else if (name.matches("V([0-9_]+)")) {
                  int minor = version >> 16 & '\uffff';
                  int major = version & '\uffff';
                  if (major > maxClassMajorVersion || major == maxClassMajorVersion && minor > maxClassMinorVersion) {
                     maxClassMajorVersion = major;
                     maxClassMinorVersion = minor;
                     maxClassVersion = version;
                     maxJavaVersion = name.replace('_', '.').substring(1);
                  }
               } else if ("ACC_PUBLIC".equals(name)) {
                  break;
               }
            } catch (ReflectiveOperationException ex) {
               throw new Error(ex);
            }
         }
      }

      return apiVersion;
   }

   private static VersionNumber getPackageVersion(Class<?> clazz) {
      String implVersion = clazz.getPackage().getImplementationVersion();
      if (implVersion != null) {
         return VersionNumber.parse(implVersion);
      } else {
         try {
            MainAttributes manifest = MainAttributes.of(clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
            return VersionNumber.parse(manifest.get(Name.IMPLEMENTATION_VERSION));
         } catch (Exception var3) {
            return VersionNumber.NONE;
         }
      }
   }
}
