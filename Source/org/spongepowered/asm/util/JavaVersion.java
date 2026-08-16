package org.spongepowered.asm.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class JavaVersion {
   public static final double JAVA_6 = 1.6;
   public static final double JAVA_7 = 1.7;
   public static final double JAVA_8 = 1.8;
   public static final double JAVA_9 = (double)9.0F;
   public static final double JAVA_10 = (double)10.0F;
   public static final double JAVA_11 = (double)11.0F;
   public static final double JAVA_12 = (double)12.0F;
   public static final double JAVA_13 = (double)13.0F;
   public static final double JAVA_14 = (double)14.0F;
   public static final double JAVA_15 = (double)15.0F;
   public static final double JAVA_16 = (double)16.0F;
   public static final double JAVA_17 = (double)17.0F;
   public static final double JAVA_18 = (double)18.0F;
   public static final double JAVA_19 = (double)19.0F;
   public static final double JAVA_20 = (double)20.0F;
   public static final double JAVA_21 = (double)21.0F;
   public static final double JAVA_22 = (double)22.0F;
   public static final double JAVA_23 = (double)23.0F;
   public static final double JAVA_24 = (double)24.0F;
   public static final double JAVA_25 = (double)25.0F;
   private static double current = (double)0.0F;

   private JavaVersion() {
   }

   public static double current() {
      if (current == (double)0.0F) {
         current = resolveCurrentVersion();
      }

      return current;
   }

   private static double resolveCurrentVersion() {
      String version = System.getProperty("java.version");
      Matcher decimalMatcher = Pattern.compile("[0-9]+\\.[0-9]+").matcher(version);
      if (decimalMatcher.find()) {
         return Double.parseDouble(decimalMatcher.group());
      } else {
         Matcher numberMatcher = Pattern.compile("[0-9]+").matcher(version);
         return numberMatcher.find() ? Double.parseDouble(numberMatcher.group()) : 1.6;
      }
   }
}
