package lol.void01n.sulfur.api;

public enum SulfurEnvironment {
   CLIENT,
   SERVER,
   DATA;

   private static volatile SulfurEnvironment current;

   public static SulfurEnvironment detect(String[] launchArgs) {
      String prop = System.getProperty("sulfur.env");
      if (prop != null) {
         switch (prop.toLowerCase()) {
            case "client" -> {
               return CLIENT;
            }
            case "server" -> {
               return SERVER;
            }
            case "data" -> {
               return DATA;
            }
         }
      }

      for(String arg : launchArgs) {
         switch (arg) {
            case "--client" -> {
               return CLIENT;
            }
            case "--server" -> {
               return SERVER;
            }
            case "--data" -> {
               return DATA;
            }
         }
      }

      return CLIENT;
   }

   public static SulfurEnvironment current() {
      SulfurEnvironment e = current;
      if (e == null) {
         throw new IllegalStateException("SulfurEnvironment not yet set — call SulfurLoader.init() first");
      } else {
         return e;
      }
   }

   static void set(SulfurEnvironment env) {
      current = env;
   }

   public boolean isClient() {
      return this == CLIENT;
   }

   public boolean isServer() {
      return this == SERVER || this == DATA;
   }

   public boolean isData() {
      return this == DATA;
   }

   // $FF: synthetic method
   private static SulfurEnvironment[] $values() {
      return new SulfurEnvironment[]{CLIENT, SERVER, DATA};
   }
}
