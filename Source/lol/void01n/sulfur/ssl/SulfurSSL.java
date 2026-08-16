package lol.void01n.sulfur.ssl;

import lol.void01n.sulfur.transformengine.SulfurTransformEngine;

public final class SulfurSSL {
   private static final boolean DEBUG = System.getProperties().containsKey("sulfur.debug");
   private static volatile boolean initialized = false;

   private SulfurSSL() {
   }

   public static synchronized void initialize(SulfurTransformEngine engine) {
      if (!initialized) {
         if (DEBUG) {
            System.out.println("sulfur/ssl: initializing SSL (Sulfur Standard Libs)");
         }

         SslTransformer sslTransformer = new SslTransformer();
         engine.registerLateTransformer(sslTransformer);
         if (DEBUG) {
            System.out.println("sulfur/ssl: SslTransformer registered");
         }

         FabricApiForwarder.initialize();
         initialized = true;
         if (DEBUG) {
            System.out.println("sulfur/ssl: initialized");
         }

      }
   }

   public static boolean isInitialized() {
      return initialized;
   }
}
