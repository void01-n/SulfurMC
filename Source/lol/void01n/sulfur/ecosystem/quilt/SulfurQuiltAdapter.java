package lol.void01n.sulfur.ecosystem.quilt;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import lol.void01n.sulfur.launch.SulfurFabricAdapterHolder;
import lol.void01n.sulfur.mixinservice.SulfurMixinService;
import lol.void01n.sulfur.mod.SulfurModContainer;
import lol.void01n.sulfur.mod.SulfurModRegistry;
import lol.void01n.sulfur.mod.SulfurModsDirectory;
import lol.void01n.sulfur.ssl.FabricModAdapter;
import lol.void01n.sulfur.transformengine.SulfurTransformationService;

public final class SulfurQuiltAdapter implements SulfurTransformationService {
   private static final boolean DEBUG = System.getProperties().containsKey("sulfur.debug");
   private static final String QUILT_MOD_JSON = "quilt.mod.json";
   private static final String FABRIC_MOD_JSON = "fabric.mod.json";
   private final List<String> discoveredMixinConfigs = new ArrayList();
   private final FabricModAdapter fabricModAdapter = new FabricModAdapter();

   public String name() {
      return "quilt";
   }

   public void onLoad(Set<String> otherServiceNames) {
      if (otherServiceNames.contains("quilt")) {
         throw new IllegalStateException("Sulfur detected two Quilt-ecosystem SulfurTransformationService adapters — only one may be registered.");
      } else {
         Path modsDir = resolveModsDir();
         if (!Files.isDirectory(modsDir, new LinkOption[0])) {
            if (DEBUG) {
               System.out.println("sulfur/quilt: mods directory not found at '" + String.valueOf(modsDir) + "' — no Quilt/Fabric mods will be loaded this run.");
            }

         } else {
            List<Path> modJars = discoverModJars(modsDir);
            PrintStream var10000 = System.out;
            int var10001 = modJars.size();
            var10000.println("sulfur/loader: discovered " + var10001 + " Quilt/Fabric mod candidate(s) in " + String.valueOf(modsDir));

            for(Path modJar : modJars) {
               System.out.println("sulfur/loader: discovered JAR: " + String.valueOf(modJar.getFileName()));
               this.processModJar(modJar);
            }

            for(String configPath : this.discoveredMixinConfigs) {
               try {
                  registerMixinConfig(configPath);
               } catch (Exception e) {
                  System.err.println("sulfur/quilt: failed registering mixin config '" + configPath + "': " + String.valueOf(e));
               }
            }

            if (this.fabricModAdapter.hasEntrypoints()) {
               SulfurFabricAdapterHolder.set(this.fabricModAdapter);
            }

         }
      }
   }

   public List<? extends SulfurTransformationService.SulfurTransformer> transformers() {
      return List.of();
   }

   private static Path resolveModsDir() {
      return SulfurModsDirectory.resolve();
   }

   private static List<Path> discoverModJars(Path modsDir) {
      List<Path> result = new ArrayList();

      try {
         DirectoryStream<Path> stream = Files.newDirectoryStream(modsDir, "*.jar");

         try {
            for(Path jar : stream) {
               if (isQuiltOrFabricModJar(jar)) {
                  result.add(jar);
               }
            }
         } catch (Throwable var6) {
            if (stream != null) {
               try {
                  stream.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (stream != null) {
            stream.close();
         }
      } catch (IOException e) {
         if (DEBUG) {
            System.out.println("sulfur/quilt: failed scanning mods dir: " + e.getMessage());
         }
      }

      return result;
   }

   private static boolean isQuiltOrFabricModJar(Path jar) {
      try {
         JarFile jf = new JarFile(jar.toFile());

         boolean var2;
         try {
            var2 = jf.getEntry("quilt.mod.json") != null || jf.getEntry("fabric.mod.json") != null;
         } catch (Throwable var5) {
            try {
               jf.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }

            throw var5;
         }

         jf.close();
         return var2;
      } catch (IOException var6) {
         return false;
      }
   }

   private void processModJar(Path jar) {
      if (DEBUG) {
         System.out.println("sulfur/quilt: processing mod jar: " + String.valueOf(jar.getFileName()));
      }

      try {
         JarFile jf = new JarFile(jar.toFile());

         try {
            ZipEntry quiltJson = jf.getEntry("quilt.mod.json");
            ZipEntry fabricJson = jf.getEntry("fabric.mod.json");
            if (quiltJson != null) {
               InputStream in = jf.getInputStream(quiltJson);

               String json;
               try {
                  json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
               } catch (Throwable var11) {
                  if (in != null) {
                     try {
                        in.close();
                     } catch (Throwable var10) {
                        var11.addSuppressed(var10);
                     }
                  }

                  throw var11;
               }

               if (in != null) {
                  in.close();
               }

               this.extractMixinConfigs(json, jar.getFileName().toString());
               registerQuiltMod(jar, json);
            } else if (fabricJson != null) {
               this.processPureFabricMod(jar, jf, fabricJson);
            }
         } catch (Throwable var12) {
            try {
               jf.close();
            } catch (Throwable var9) {
               var12.addSuppressed(var9);
            }

            throw var12;
         }

         jf.close();
      } catch (IOException e) {
         PrintStream var10000 = System.err;
         String var10001 = String.valueOf(jar);
         var10000.println("sulfur/quilt: failed processing mod jar " + var10001 + ": " + String.valueOf(e));
      }

   }

   private void processPureFabricMod(Path jar, JarFile jf, ZipEntry fabricJsonEntry) throws IOException {
      InputStream in = jf.getInputStream(fabricJsonEntry);

      String json;
      try {
         json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      } catch (Throwable var12) {
         if (in != null) {
            try {
               in.close();
            } catch (Throwable var11) {
               var12.addSuppressed(var11);
            }
         }

         throw var12;
      }

      if (in != null) {
         in.close();
      }

      this.extractMixinConfigs(json, jar.getFileName().toString());

      try {
         Class<?> msClass = Class.forName("lol.void01n.sulfur.mixinservice.SulfurMixinService");
         Class<?> mixinServiceClass = Class.forName("org.spongepowered.asm.service.MixinService");
         Object service = mixinServiceClass.getMethod("getService").invoke((Object)null);
         if (msClass.isInstance(service)) {
            this.fabricModAdapter.processJar(jar, (SulfurMixinService)service);
         }
      } catch (Exception e) {
         if (DEBUG) {
            PrintStream var10000 = System.out;
            String var10001 = String.valueOf(jar.getFileName());
            var10000.println("sulfur/quilt: could not delegate to FabricModAdapter for " + var10001 + ": " + String.valueOf(e));
         }
      }

      String id = extractString(json, "id");
      String version = extractString(json, "version");
      String name = extractString(json, "name");
      Map<String, String> deps = extractDependsBlock(json);
      SulfurModContainer container = new SulfurModContainer(id != null ? id : jar.getFileName().toString().replaceAll("\\.jar$", ""), version != null ? version : "unknown", "fabric", List.of(jar.toAbsolutePath()), name != null ? name : id, deps);
      System.out.println("sulfur/loader: identified mod: " + container.id);
      System.out.println("sulfur/loader: ecosystem: fabric");
      System.out.println("sulfur/loader: version: " + container.version);
      boolean registered = SulfurModRegistry.getInstance().register(container);
      if (registered) {
         System.out.println("sulfur/loader: registered mod: " + container.id);
      }

   }

   private static void registerQuiltMod(Path jar, String json) {
      String id = extractString(json, "id");
      String version = extractString(json, "version");
      String name = extractString(json, "name");
      Map<String, String> deps = extractDependsBlock(json);
      SulfurModContainer container = new SulfurModContainer(id != null ? id : jar.getFileName().toString().replaceAll("\\.jar$", ""), version != null ? version : "unknown", "quilt", List.of(jar.toAbsolutePath()), name != null ? name : id, deps);
      System.out.println("sulfur/loader: identified mod: " + container.id);
      System.out.println("sulfur/loader: ecosystem: quilt");
      System.out.println("sulfur/loader: version: " + container.version);
      boolean registered = SulfurModRegistry.getInstance().register(container);
      if (registered) {
         System.out.println("sulfur/loader: registered mod: " + container.id);
      }

   }

   private void extractMixinConfigs(String json, String sourceName) {
      for(String key : new String[]{"\"mixin\"", "\"mixins\""}) {
         int keyIdx = json.indexOf(key);
         if (keyIdx >= 0) {
            int colon = json.indexOf(58, keyIdx + key.length());
            if (colon >= 0) {
               int valueStart;
               for(valueStart = colon + 1; valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart)); ++valueStart) {
               }

               if (valueStart < json.length()) {
                  char first = json.charAt(valueStart);
                  if (first == '[') {
                     int end = findClosingBracket(json, valueStart, '[', ']');
                     String array = json.substring(valueStart + 1, end);
                     this.extractStringValues(array, sourceName);
                  } else if (first == '"') {
                     int strEnd = json.indexOf(34, valueStart + 1);
                     if (strEnd > valueStart) {
                        String config = json.substring(valueStart + 1, strEnd);
                        if (!config.isBlank()) {
                           this.discoveredMixinConfigs.add(config);
                           if (DEBUG) {
                              System.out.println("sulfur/quilt: found mixin config '" + config + "' from " + sourceName);
                           }
                        }
                     }
                  }
               }
            }
         }
      }

   }

   private void extractStringValues(String fragment, String sourceName) {
      int i = 0;

      while(i < fragment.length()) {
         int q1 = fragment.indexOf(34, i);
         if (q1 < 0) {
            break;
         }

         int q2 = fragment.indexOf(34, q1 + 1);
         if (q2 < 0) {
            break;
         }

         String token = fragment.substring(q1 + 1, q2);
         if (token.equals("config")) {
            int colonIdx = fragment.indexOf(58, q2 + 1);
            if (colonIdx >= 0) {
               int vq1 = fragment.indexOf(34, colonIdx + 1);
               int vq2 = fragment.indexOf(34, vq1 + 1);
               if (vq1 >= 0 && vq2 > vq1) {
                  String config = fragment.substring(vq1 + 1, vq2);
                  if (!config.isBlank()) {
                     this.discoveredMixinConfigs.add(config);
                     if (DEBUG) {
                        System.out.println("sulfur/quilt: found mixin config (object form) '" + config + "' from " + sourceName);
                     }
                  }

                  i = vq2 + 1;
                  continue;
               }
            }
         } else if (!token.isBlank() && token.endsWith(".json")) {
            this.discoveredMixinConfigs.add(token);
            if (DEBUG) {
               System.out.println("sulfur/quilt: found mixin config '" + token + "' from " + sourceName);
            }
         }

         i = q2 + 1;
      }

   }

   private static String extractString(String json, String key) {
      String search = "\"" + key + "\"";
      int idx = json.indexOf(search);
      if (idx < 0) {
         return null;
      } else {
         int colon = json.indexOf(58, idx + search.length());
         if (colon < 0) {
            return null;
         } else {
            int q1 = json.indexOf(34, colon + 1);
            if (q1 < 0) {
               return null;
            } else {
               int q2 = json.indexOf(34, q1 + 1);
               return q2 < 0 ? null : json.substring(q1 + 1, q2);
            }
         }
      }
   }

   private static Map<String, String> extractDependsBlock(String json) {
      Map<String, String> result = new HashMap();
      int idx = json.indexOf("\"depends\"");
      if (idx < 0) {
         return result;
      } else {
         int colon = json.indexOf(58, idx + "\"depends\"".length());
         if (colon < 0) {
            return result;
         } else {
            int braceOpen = json.indexOf(123, colon + 1);
            if (braceOpen < 0) {
               return result;
            } else {
               int braceClose = findClosingBracket(json, braceOpen, '{', '}');
               String block = json.substring(braceOpen + 1, braceClose);

               int q2;
               int vq2;
               for(int i = 0; i < block.length(); i = Math.max(q2 + 1, vq2 + 1)) {
                  int q1 = block.indexOf(34, i);
                  if (q1 < 0) {
                     break;
                  }

                  q2 = block.indexOf(34, q1 + 1);
                  if (q2 < 0) {
                     break;
                  }

                  String depId = block.substring(q1 + 1, q2);
                  int c = block.indexOf(58, q2 + 1);
                  if (c < 0) {
                     break;
                  }

                  int vq1 = block.indexOf(34, c + 1);
                  vq2 = block.indexOf(34, vq1 + 1);
                  if (vq1 >= 0 && vq2 > vq1) {
                     result.put(depId, block.substring(vq1 + 1, vq2));
                  }
               }

               return result;
            }
         }
      }
   }

   private static int findClosingBracket(String s, int openIdx, char open, char close) {
      int depth = 0;

      for(int i = openIdx; i < s.length(); ++i) {
         if (s.charAt(i) == open) {
            ++depth;
         } else if (s.charAt(i) == close) {
            --depth;
            if (depth == 0) {
               return i;
            }
         }
      }

      return s.length() - 1;
   }

   private static void registerMixinConfig(String configPath) {
      try {
         Class<?> serviceClass = Class.forName("lol.void01n.sulfur.mixinservice.SulfurMixinService");
         Class<?> mixinServiceClass = Class.forName("org.spongepowered.asm.service.MixinService");
         Object service = mixinServiceClass.getMethod("getService").invoke((Object)null);
         if (serviceClass.isInstance(service)) {
            serviceClass.getMethod("registerQuiltMixinConfig", String.class).invoke(service, configPath);
         }
      } catch (Exception e) {
         if (DEBUG) {
            System.out.println("sulfur/quilt: could not register mixin config reflectively: " + String.valueOf(e));
         }
      }

   }
}
