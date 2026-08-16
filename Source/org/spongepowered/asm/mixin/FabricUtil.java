package org.spongepowered.asm.mixin;

import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.refmap.IMixinContext;

public final class FabricUtil {
   public static final String KEY_MOD_ID = "fabric-modId";
   public static final String KEY_COMPATIBILITY = "fabric-compat";
   public static final int COMPATIBILITY_0_9_2 = 9002;
   public static final int COMPATIBILITY_0_10_0 = 10000;
   public static final int COMPATIBILITY_0_14_0 = 14000;
   public static final int COMPATIBILITY_0_16_5 = 16005;
   public static final int COMPATIBILITY_0_17_0 = 17000;
   public static final int COMPATIBILITY_0_17_1 = 17001;
   public static final int COMPATIBILITY_LATEST = 17001;

   public static String getModId(IMixinConfig config) {
      return getModId(config, "(unknown)");
   }

   public static String getModId(IMixinConfig config, String defaultValue) {
      return (String)getDecoration(config, "fabric-modId", defaultValue);
   }

   public static String getModId(ISelectorContext context) {
      return (String)getDecoration(getConfig(context), "fabric-modId", "(unknown)");
   }

   public static int getCompatibility(ISelectorContext context) {
      return getCompatibility(getConfig(context));
   }

   public static int getCompatibility(IMixinContext context) {
      return getCompatibility(context.getMixin().getConfig());
   }

   public static int getCompatibility(IMixinConfig config) {
      return (Integer)getDecoration(config, "fabric-compat", 17001);
   }

   private static IMixinConfig getConfig(ISelectorContext context) {
      return context.getMixin().getMixin().getConfig();
   }

   private static <T> T getDecoration(IMixinConfig config, String key, T defaultValue) {
      return (T)(config.hasDecoration(key) ? config.getDecoration(key) : defaultValue);
   }

   private FabricUtil() {
   }
}
