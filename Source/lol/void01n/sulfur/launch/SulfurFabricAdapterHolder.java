package lol.void01n.sulfur.launch;

import lol.void01n.sulfur.ssl.FabricModAdapter;

public final class SulfurFabricAdapterHolder {
   private static volatile FabricModAdapter instance;

   private SulfurFabricAdapterHolder() {
   }

   public static void set(FabricModAdapter adapter) {
      instance = adapter;
   }

   public static FabricModAdapter get() {
      return instance;
   }
}
