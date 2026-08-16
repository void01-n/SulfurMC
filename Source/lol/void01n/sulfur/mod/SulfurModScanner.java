package lol.void01n.sulfur.mod;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import lol.void01n.sulfur.classloader.SulfurClassLoader;
import lol.void01n.sulfur.ecosystem.neoforge.NeoForgeModMetadata;
import lol.void01n.sulfur.transformengine.SulfurTransformEngine;

public final class SulfurModScanner {
   private static final boolean DEBUG = System.getProperties().containsKey("sulfur.debug");
   private static final long POLL_INTERVAL_SECONDS = 60L;
   private static final String NEOFORGE_MODS_TOML = "META-INF/neoforge.mods.toml";
   private static final String MODS_TOML = "META-INF/mods.toml";
   private final SulfurModRegistry registry;
   private final SulfurTransformEngine transformEngine;
   private final SulfurClassLoader classLoader;
   private final Map<Path, Long> lastScanState = new HashMap();
   private final ScheduledExecutorService scheduler;

   public SulfurModScanner(SulfurModRegistry registry, SulfurTransformEngine transformEngine, SulfurClassLoader classLoader) {
      this.registry = registry;
      this.transformEngine = transformEngine;
      this.classLoader = classLoader;
      this.scheduler = Executors.newSingleThreadScheduledExecutor((r) -> {
         Thread t = new Thread(r, "sulfur-mod-scanner");
         t.setDaemon(true);
         return t;
      });
   }

   public void start() {
      this.seedInitialState();
      this.scheduler.scheduleAtFixedRate(this::scanSafely, 60L, 60L, TimeUnit.SECONDS);
      if (DEBUG) {
         System.out.println("sulfur/scanner: started — polling every 60s; mods dir: " + String.valueOf(resolveModsDir().toAbsolutePath()));
      }

   }

   public void stop() {
      this.scheduler.shutdownNow();
      if (DEBUG) {
         System.out.println("sulfur/scanner: stopped");
      }

   }

   private void seedInitialState() {
      Path modsDir = resolveModsDir();
      if (Files.isDirectory(modsDir, new LinkOption[0])) {
         try {
            DirectoryStream<Path> stream = Files.newDirectoryStream(modsDir, "*.jar");

            try {
               for(Path jar : stream) {
                  try {
                     this.lastScanState.put(jar.toAbsolutePath(), Files.getLastModifiedTime(jar).toMillis());
                  } catch (IOException var7) {
                  }
               }
            } catch (Throwable var8) {
               if (stream != null) {
                  try {
                     stream.close();
                  } catch (Throwable var6) {
                     var8.addSuppressed(var6);
                  }
               }

               throw var8;
            }

            if (stream != null) {
               stream.close();
            }
         } catch (IOException e) {
            if (DEBUG) {
               System.out.println("sulfur/scanner: failed seeding initial state: " + String.valueOf(e));
            }
         }

         if (DEBUG) {
            System.out.println("sulfur/scanner: seeded " + this.lastScanState.size() + " jar(s) as initial baseline state");
         }

      }
   }

   private void scanSafely() {
      try {
         this.scan();
      } catch (Exception e) {
         System.err.println("sulfur/scanner: uncaught exception during scan: " + String.valueOf(e));
         if (DEBUG) {
            e.printStackTrace(System.err);
         }
      }

   }

   private void scan() {
      Path modsDir = resolveModsDir();
      if (!Files.isDirectory(modsDir, new LinkOption[0])) {
         if (DEBUG) {
            System.out.println("sulfur/scanner: mods dir not found, skipping scan");
         }

      } else {
         Map<Path, Long> currentState = new HashMap();

         try {
            DirectoryStream<Path> stream = Files.newDirectoryStream(modsDir, "*.jar");

            try {
               for(Path jar : stream) {
                  try {
                     FileTime mtime = Files.getLastModifiedTime(jar);
                     currentState.put(jar.toAbsolutePath(), mtime.toMillis());
                  } catch (IOException var10) {
                  }
               }
            } catch (Throwable var11) {
               if (stream != null) {
                  try {
                     stream.close();
                  } catch (Throwable var9) {
                     var11.addSuppressed(var9);
                  }
               }

               throw var11;
            }

            if (stream != null) {
               stream.close();
            }
         } catch (IOException e) {
            if (DEBUG) {
               System.out.println("sulfur/scanner: scan failed while listing dir: " + String.valueOf(e));
            }

            return;
         }

         for(Map.Entry<Path, Long> entry : currentState.entrySet()) {
            Path jar = (Path)entry.getKey();
            long newMtime = (Long)entry.getValue();
            Long prevMtime = (Long)this.lastScanState.get(jar);
            if (prevMtime == null) {
               this.handleNewJar(jar);
            } else if (newMtime != prevMtime) {
               this.handleModifiedJar(jar);
            }
         }

         for(Path jar : this.lastScanState.keySet()) {
            if (!currentState.containsKey(jar)) {
               this.handleRemovedJar(jar);
            }
         }

         this.lastScanState.clear();
         this.lastScanState.putAll(currentState);
      }
   }

   private void handleNewJar(Path jar) {
      System.out.println("sulfur/scanner: new mod jar detected: " + String.valueOf(jar.getFileName()) + " — loading in-place");
      this.loadJar(jar);
   }

   private void handleModifiedJar(Path jar) {
      System.out.println("sulfur/scanner: modified mod jar: " + String.valueOf(jar.getFileName()) + " — adding new classes to classloader (already-loaded classes cannot be unloaded without a JVM restart)");
      this.loadJar(jar);
   }

   private void handleRemovedJar(Path jar) {
      System.out.println("sulfur/scanner: mod jar removed: " + String.valueOf(jar.getFileName()) + " — unregistering from registry (classes already loaded remain until restart)");
      List<SulfurModContainer> toRemove = new ArrayList();

      for(SulfurModContainer mod : this.registry.getAllMods()) {
         if (mod.jars.contains(jar)) {
            toRemove.add(mod);
         }
      }

      for(SulfurModContainer mod : toRemove) {
         this.registry.unregister(mod.id);
      }

      if (toRemove.isEmpty() && DEBUG) {
         System.out.println("sulfur/scanner: no registered mod found for removed jar " + String.valueOf(jar.getFileName()) + " (may have been loaded as part of another mod)");
      }

   }

   private void loadJar(Path jar) {
      EcosystemType type;
      try {
         type = detectEcosystem(jar);
      } catch (IOException e) {
         PrintStream var10000 = System.err;
         String var10001 = String.valueOf(jar.getFileName());
         var10000.println("sulfur/scanner: could not inspect jar " + var10001 + ": " + String.valueOf(e));
         return;
      }

      if (DEBUG) {
         PrintStream var8 = System.out;
         String var9 = String.valueOf(jar.getFileName());
         var8.println("sulfur/scanner: loading " + var9 + " as " + type.name().toLowerCase() + " mod");
      }

      this.addJarToClassLoader(jar);
      this.registerMixinConfigs(jar, type);
      SulfurModContainer container = buildModContainer(jar, type);
      if (container != null) {
         this.registry.register(container);
         List<String> depProblems = SulfurDependencyResolver.checkDependencies(container, this.registry);
         if (!depProblems.isEmpty()) {
            System.err.println("sulfur/scanner: mod '" + container.id + "' has unsatisfied dependencies:");

            for(String p : depProblems) {
               System.err.println("  - " + p);
            }
         }
      }

   }

   private void addJarToClassLoader(Path jar) {
      try {
         this.classLoader.addJar(jar.toUri().toURL());
         if (DEBUG) {
            System.out.println("sulfur/scanner: added " + String.valueOf(jar.getFileName()) + " to SulfurClassLoader URL path");
         }
      } catch (Exception e) {
         PrintStream var10000 = System.err;
         String var10001 = String.valueOf(e);
         var10000.println("sulfur/scanner: failed adding jar to classloader: " + var10001 + " — classes from " + String.valueOf(jar.getFileName()) + " will not be loadable");
      }

   }

   private void registerMixinConfigs(Path jar, EcosystemType type) {
      try {
         if (type == SulfurModScanner.EcosystemType.NEOFORGE) {
            this.registerNeoForgeMixinConfigs(jar);
         } else {
            this.registerQuiltFabricMixinConfigs(jar, type);
         }
      } catch (IOException e) {
         if (DEBUG) {
            PrintStream var10000 = System.out;
            String var10001 = String.valueOf(jar.getFileName());
            var10000.println("sulfur/scanner: failed reading manifest from " + var10001 + ": " + String.valueOf(e));
         }
      }

   }

   private void registerNeoForgeMixinConfigs(Path jar) throws IOException {
      JarFile jf = new JarFile(jar.toFile());

      label64: {
         try {
            ZipEntry tomlEntry = jf.getEntry("META-INF/neoforge.mods.toml");
            if (tomlEntry == null) {
               tomlEntry = jf.getEntry("META-INF/mods.toml");
            }

            if (tomlEntry == null) {
               break label64;
            }

            InputStream in = jf.getInputStream(tomlEntry);

            NeoForgeModMetadata meta;
            try {
               String var10001 = String.valueOf(jar.getFileName());
               meta = NeoForgeModMetadata.parse(in, var10001 + "!" + (jf.getEntry("META-INF/neoforge.mods.toml") != null ? "META-INF/neoforge.mods.toml" : "META-INF/mods.toml"));
            } catch (Throwable var10) {
               if (in != null) {
                  try {
                     in.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }
               }

               throw var10;
            }

            if (in != null) {
               in.close();
            }

            for(String configPath : meta.mixinConfigs) {
               reflectiveRegisterMixin(configPath, "registerNeoForgeMixinConfig");
            }
         } catch (Throwable var11) {
            try {
               jf.close();
            } catch (Throwable var8) {
               var11.addSuppressed(var8);
            }

            throw var11;
         }

         jf.close();
         return;
      }

      jf.close();
   }

   private void registerQuiltFabricMixinConfigs(Path jar, EcosystemType type) throws IOException {
      String var10000;
      switch (type.ordinal()) {
         case 1 -> var10000 = "quilt.mod.json";
         case 2 -> var10000 = "fabric.mod.json";
         default -> var10000 = null;
      }

      String manifestEntry = var10000;
      if (manifestEntry != null) {
         JarFile jf = new JarFile(jar.toFile());

         label57: {
            try {
               ZipEntry entry = jf.getEntry(manifestEntry);
               if (entry == null) {
                  break label57;
               }

               InputStream in = jf.getInputStream(entry);

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

               extractMixinConfigPaths(json).forEach((configPath) -> reflectiveRegisterMixin(configPath, "registerQuiltMixinConfig"));
            } catch (Throwable var13) {
               try {
                  jf.close();
               } catch (Throwable var10) {
                  var13.addSuppressed(var10);
               }

               throw var13;
            }

            jf.close();
            return;
         }

         jf.close();
      }
   }

   private static List<String> extractMixinConfigPaths(String json) {
      List<String> configs = new ArrayList();

      for(String key : new String[]{"\"mixin\"", "\"mixins\""}) {
         int keyIdx = json.indexOf(key);
         if (keyIdx >= 0) {
            int colon = json.indexOf(58, keyIdx + key.length());
            if (colon >= 0) {
               int start;
               for(start = colon + 1; start < json.length() && Character.isWhitespace(json.charAt(start)); ++start) {
               }

               if (start < json.length()) {
                  char first = json.charAt(start);
                  if (first == '[') {
                     int end = findClosing(json, start, '[', ']');
                     collectStrings(json.substring(start + 1, end), configs);
                  } else if (first == '"') {
                     int end = json.indexOf(34, start + 1);
                     if (end > start) {
                        String val = json.substring(start + 1, end);
                        if (!val.isBlank()) {
                           configs.add(val);
                        }
                     }
                  }
               }
            }
         }
      }

      return configs;
   }

   private static void collectStrings(String fragment, List<String> out) {
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
         if ("config".equals(token)) {
            int c = fragment.indexOf(58, q2 + 1);
            if (c >= 0) {
               int vq1 = fragment.indexOf(34, c + 1);
               int vq2 = fragment.indexOf(34, vq1 + 1);
               if (vq1 >= 0 && vq2 > vq1) {
                  out.add(fragment.substring(vq1 + 1, vq2));
                  i = vq2 + 1;
                  continue;
               }
            }
         } else if (!token.isBlank() && token.endsWith(".json")) {
            out.add(token);
         }

         i = q2 + 1;
      }

   }

   private static void reflectiveRegisterMixin(String configPath, String methodName) {
      try {
         Class<?> serviceClass = Class.forName("lol.void01n.sulfur.mixinservice.SulfurMixinService");
         Class<?> mixinSvc = Class.forName("org.spongepowered.asm.service.MixinService");
         Object svc = mixinSvc.getMethod("getService").invoke((Object)null);
         if (serviceClass.isInstance(svc)) {
            serviceClass.getMethod(methodName, String.class).invoke(svc, configPath);
            if (DEBUG) {
               System.out.println("sulfur/scanner: registered mixin config '" + configPath + "'");
            }
         }
      } catch (Exception e) {
         if (DEBUG) {
            System.out.println("sulfur/scanner: failed registering mixin config '" + configPath + "': " + String.valueOf(e));
         }
      }

   }

   private static SulfurModContainer buildModContainer(Path jar, EcosystemType type) {
      return type == SulfurModScanner.EcosystemType.NEOFORGE ? buildNeoForgeModContainer(jar) : buildQuiltFabricModContainer(jar, type);
   }

   private static SulfurModContainer buildNeoForgeModContainer(Path jar) {
      try {
         JarFile jf = new JarFile(jar.toFile());

         SulfurModContainer var14;
         label67: {
            try {
               ZipEntry tomlEntry = jf.getEntry("META-INF/neoforge.mods.toml");
               if (tomlEntry == null) {
                  tomlEntry = jf.getEntry("META-INF/mods.toml");
               }

               if (tomlEntry == null) {
                  String id = jar.getFileName().toString().replaceAll("\\.jar$", "");
                  var14 = new SulfurModContainer(id, "unknown", "neoforge", List.of(jar.toAbsolutePath()), id, Map.of());
                  break label67;
               }

               InputStream in = jf.getInputStream(tomlEntry);

               NeoForgeModMetadata meta;
               try {
                  meta = NeoForgeModMetadata.parse(in, jar.getFileName().toString());
               } catch (Throwable var9) {
                  if (in != null) {
                     try {
                        in.close();
                     } catch (Throwable var8) {
                        var9.addSuppressed(var8);
                     }
                  }

                  throw var9;
               }

               if (in != null) {
                  in.close();
               }

               var14 = new SulfurModContainer(meta.id, meta.version, "neoforge", List.of(jar.toAbsolutePath()), meta.displayName, meta.dependencies);
            } catch (Throwable var10) {
               try {
                  jf.close();
               } catch (Throwable var7) {
                  var10.addSuppressed(var7);
               }

               throw var10;
            }

            jf.close();
            return var14;
         }

         jf.close();
         return var14;
      } catch (IOException e) {
         if (DEBUG) {
            PrintStream var10000 = System.out;
            String var10001 = String.valueOf(jar.getFileName());
            var10000.println("sulfur/scanner: failed reading NeoForge manifest from " + var10001 + ": " + String.valueOf(e));
         }

         return null;
      }
   }

   private static SulfurModContainer buildQuiltFabricModContainer(Path jar, EcosystemType type) {
      String id = jar.getFileName().toString().replaceAll("\\.jar$", "");
      String version = "unknown";
      String displayName = id;
      Map<String, String> deps = Map.of();
      String var10000;
      switch (type.ordinal()) {
         case 1 -> var10000 = "quilt.mod.json";
         case 2 -> var10000 = "fabric.mod.json";
         default -> var10000 = null;
      }

      String manifestEntry = var10000;
      if (manifestEntry != null) {
         try {
            JarFile jf = new JarFile(jar.toFile());

            try {
               ZipEntry entry = jf.getEntry(manifestEntry);
               if (entry != null) {
                  InputStream in = jf.getInputStream(entry);

                  String json;
                  try {
                     json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                  } catch (Throwable var15) {
                     if (in != null) {
                        try {
                           in.close();
                        } catch (Throwable var14) {
                           var15.addSuppressed(var14);
                        }
                     }

                     throw var15;
                  }

                  if (in != null) {
                     in.close();
                  }

                  String parsedId = extractString(json, "id");
                  String parsedVersion = extractString(json, "version");
                  String parsedName = extractString(json, "name");
                  if (parsedId != null) {
                     id = parsedId;
                  }

                  if (parsedVersion != null) {
                     version = parsedVersion;
                  }

                  if (parsedName != null) {
                     displayName = parsedName;
                  }

                  deps = extractDependsBlock(json);
               }
            } catch (Throwable var16) {
               try {
                  jf.close();
               } catch (Throwable var13) {
                  var16.addSuppressed(var13);
               }

               throw var16;
            }

            jf.close();
         } catch (IOException e) {
            if (DEBUG) {
               PrintStream var20 = System.out;
               String var10001 = String.valueOf(jar.getFileName());
               var20.println("sulfur/scanner: failed reading manifest from " + var10001 + ": " + String.valueOf(e));
            }
         }
      }

      switch (type.ordinal()) {
         case 1 -> var10000 = "quilt";
         case 2 -> var10000 = "fabric";
         default -> var10000 = "quilt";
      }

      String ecosystem = var10000;
      return new SulfurModContainer(id, version, ecosystem, List.of(jar.toAbsolutePath()), displayName, deps);
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
         idx = json.indexOf("\"quilt_loader\"");
      }

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
               int braceClose = findClosing(json, braceOpen, '{', '}');
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

   private static EcosystemType detectEcosystem(Path jar) throws IOException {
      JarFile jf = new JarFile(jar.toFile());

      EcosystemType var7;
      label47: {
         label46: {
            label45: {
               try {
                  if (jf.getEntry("META-INF/neoforge.mods.toml") != null || jf.getEntry("META-INF/mods.toml") != null) {
                     var7 = SulfurModScanner.EcosystemType.NEOFORGE;
                     break label47;
                  }

                  if (jf.getEntry("quilt.mod.json") != null) {
                     var7 = SulfurModScanner.EcosystemType.QUILT;
                     break label46;
                  }

                  if (jf.getEntry("fabric.mod.json") != null) {
                     var7 = SulfurModScanner.EcosystemType.FABRIC;
                     break label45;
                  }
               } catch (Throwable var5) {
                  try {
                     jf.close();
                  } catch (Throwable var4) {
                     var5.addSuppressed(var4);
                  }

                  throw var5;
               }

               jf.close();
               return SulfurModScanner.EcosystemType.QUILT;
            }

            jf.close();
            return var7;
         }

         jf.close();
         return var7;
      }

      jf.close();
      return var7;
   }

   private static int findClosing(String s, int openIdx, char open, char close) {
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

   private static Path resolveModsDir() {
      return SulfurModsDirectory.resolve();
   }

   private static enum EcosystemType {
      NEOFORGE,
      QUILT,
      FABRIC;

      // $FF: synthetic method
      private static EcosystemType[] $values() {
         return new EcosystemType[]{NEOFORGE, QUILT, FABRIC};
      }
   }
}
