package lol.void01n.sulfur.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import lol.void01n.sulfur.classloader.SulfurClassLoader;
import lol.void01n.sulfur.mixinservice.SulfurMixinService;

public final class FabricModAdapter {
   private static final boolean DEBUG = System.getProperties().containsKey("sulfur.debug");
   private static final String FABRIC_MOD_JSON = "fabric.mod.json";
   private final List<FabricEntrypointRecord> entrypointRecords = new ArrayList();

   public void processJar(Path jar, SulfurMixinService service) {
      if (DEBUG) {
         System.out.println("sulfur/ssl: processing Fabric mod jar: " + String.valueOf(jar.getFileName()));
      }

      try {
         JarFile jf = new JarFile(jar.toFile());

         label86: {
            try {
               ZipEntry fabricJson = jf.getEntry("fabric.mod.json");
               if (fabricJson != null) {
                  InputStream in = jf.getInputStream(fabricJson);

                  FabricModMetadata meta;
                  try {
                     meta = FabricModMetadata.parse(in, String.valueOf(jar.getFileName()) + "!fabric.mod.json");
                  } catch (Throwable var13) {
                     if (in != null) {
                        try {
                           in.close();
                        } catch (Throwable var12) {
                           var13.addSuppressed(var12);
                        }
                     }

                     throw var13;
                  }

                  if (in != null) {
                     in.close();
                  }

                  for(String configPath : meta.mixinConfigs) {
                     service.registerQuiltMixinConfig(configPath);
                     if (DEBUG) {
                        System.out.println("sulfur/ssl: registered Fabric mixin config '" + configPath + "' from " + String.valueOf(jar.getFileName()));
                     }
                  }

                  Iterator var17 = meta.entrypoints.entrySet().iterator();

                  while(true) {
                     if (!var17.hasNext()) {
                        break label86;
                     }

                     Map.Entry<String, List<String>> entry = (Map.Entry)var17.next();
                     String category = (String)entry.getKey();

                     for(String className : (List)entry.getValue()) {
                        this.entrypointRecords.add(new FabricEntrypointRecord(meta.id, category, className));
                        if (DEBUG) {
                           System.out.println("sulfur/ssl: recorded Fabric entrypoint '" + className + "' (" + category + ") from " + meta.id);
                        }
                     }
                  }
               }
            } catch (Throwable var14) {
               try {
                  jf.close();
               } catch (Throwable var11) {
                  var14.addSuppressed(var11);
               }

               throw var14;
            }

            jf.close();
            return;
         }

         jf.close();
      } catch (IOException e) {
         PrintStream var10000 = System.err;
         String var10001 = String.valueOf(jar);
         var10000.println("sulfur/ssl: failed processing Fabric mod jar " + var10001 + ": " + String.valueOf(e));
      }

   }

   public void invokeEntrypoints(String category, SulfurClassLoader loader) {
      for(FabricEntrypointRecord record : this.entrypointRecords) {
         if (category.equals(record.category)) {
            try {
               Class<?> cls = loader.loadClass(record.className);
               Object instance = cls.getDeclaredConstructor().newInstance();
               cls.getMethod("onInitialize").invoke(instance);
               if (DEBUG) {
                  System.out.println("sulfur/ssl: invoked Fabric entrypoint " + record.className + " (" + category + ") from mod " + record.modId);
               }
            } catch (NoSuchMethodException var7) {
               System.err.println("sulfur/ssl: entrypoint " + record.className + " has no onInitialize() method — not a valid Fabric entrypoint?");
            } catch (Exception e) {
               String var10001 = record.className;
               System.err.println("sulfur/ssl: failed invoking Fabric entrypoint " + var10001 + " from mod " + record.modId + ": " + String.valueOf(e));
            }
         }
      }

   }

   public boolean hasEntrypoints() {
      return !this.entrypointRecords.isEmpty();
   }

   private static record FabricEntrypointRecord(String modId, String category, String className) {
   }
}
