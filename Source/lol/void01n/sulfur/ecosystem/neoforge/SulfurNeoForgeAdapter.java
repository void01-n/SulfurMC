package lol.void01n.sulfur.ecosystem.neoforge;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import lol.void01n.sulfur.classloader.SulfurClassLoader;
import lol.void01n.sulfur.mod.SulfurModContainer;
import lol.void01n.sulfur.mod.SulfurModRegistry;
import lol.void01n.sulfur.mod.SulfurModsDirectory;
import lol.void01n.sulfur.transformengine.SulfurTransformationService;

public final class SulfurNeoForgeAdapter implements SulfurTransformationService {
   private static final boolean DEBUG = System.getProperties().containsKey("sulfur.debug");
   private static final String NEOFORGE_MODS_TOML = "META-INF/neoforge.mods.toml";
   private static final String MODS_TOML = "META-INF/mods.toml";
   private static final String AT_FILE_PATH = "META-INF/accesstransformer.cfg";
   private static final String COREMODS_JSON = "META-INF/coremods.json";
   private final AccessTransformerApplier atApplier = new AccessTransformerApplier();
   private final CoreModLoader coreModLoader = new CoreModLoader();

   public String name() {
      return "neoforge";
   }

   public void onLoad(Set<String> otherServiceNames) {
      if (otherServiceNames.contains("neoforge")) {
         throw new IllegalStateException("Sulfur detected two NeoForge-ecosystem SulfurTransformationService adapters — only one may be registered. Remove the duplicate.");
      } else {
         Path modsDir = resolveModsDir();
         if (!Files.isDirectory(modsDir, new LinkOption[0])) {
            if (DEBUG) {
               System.out.println("sulfur/neoforge: mods directory not found at '" + String.valueOf(modsDir) + "' — no NeoForge mods will be loaded this run.");
            }

         } else {
            List<Path> neoForgeMods = discoverNeoForgeModJars(modsDir);
            PrintStream var10000 = System.out;
            int var10001 = neoForgeMods.size();
            var10000.println("sulfur/loader: discovered " + var10001 + " NeoForge mod candidate(s) in " + String.valueOf(modsDir));

            for(Path modJar : neoForgeMods) {
               System.out.println("sulfur/loader: discovered JAR: " + String.valueOf(modJar.getFileName()));
               this.processModJar(modJar);
            }

            if (DEBUG) {
               var10000 = System.out;
               String var7 = this.atApplier.isEmpty() ? "empty (no AT files found)" : "loaded";
               var10000.println("sulfur/neoforge: AT applier " + var7);
               System.out.println("sulfur/neoforge: coremod loader " + (this.coreModLoader.isEmpty() ? "empty (no coremods found)" : "loaded"));
            }

         }
      }
   }

   public List<? extends SulfurTransformationService.SulfurTransformer> transformers() {
      List<SulfurTransformationService.SulfurTransformer> result = new ArrayList();
      if (!this.atApplier.isEmpty()) {
         result.add(new SulfurTransformationService.SulfurTransformer() {
            public boolean matches(String className) {
               return SulfurNeoForgeAdapter.this.atApplier.matches(className);
            }

            public byte[] transform(byte[] classBytes, String className) {
               return SulfurNeoForgeAdapter.this.atApplier.transform(classBytes, className);
            }
         });
      }

      result.addAll(this.coreModLoader.asTransformers());
      return List.copyOf(result);
   }

   private static Path resolveModsDir() {
      return SulfurModsDirectory.resolve();
   }

   private static List<Path> discoverNeoForgeModJars(Path modsDir) {
      List<Path> result = new ArrayList();

      try {
         DirectoryStream<Path> stream = Files.newDirectoryStream(modsDir, "*.jar");

         try {
            for(Path jar : stream) {
               if (isNeoForgeModJar(jar)) {
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
            System.out.println("sulfur/neoforge: failed scanning mods dir: " + e.getMessage());
         }
      }

      return result;
   }

   private static boolean isNeoForgeModJar(Path jar) {
      try {
         JarFile jf = new JarFile(jar.toFile());

         boolean var2;
         try {
            var2 = jf.getEntry("META-INF/neoforge.mods.toml") != null || jf.getEntry("META-INF/mods.toml") != null;
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
      } catch (IOException e) {
         if (DEBUG) {
            PrintStream var10000 = System.out;
            String var10001 = String.valueOf(jar);
            var10000.println("sulfur/neoforge: could not inspect jar " + var10001 + ": " + String.valueOf(e));
         }

         return false;
      }
   }

   private void processModJar(Path jar) {
      if (DEBUG) {
         System.out.println("sulfur/neoforge: processing mod jar: " + String.valueOf(jar.getFileName()));
      }

      SulfurClassLoader cl = SulfurClassLoader.getInstance();
      if (cl != null) {
         try {
            cl.addJar(jar.toUri().toURL());
            if (DEBUG) {
               System.out.println("sulfur/neoforge: added " + String.valueOf(jar.getFileName()) + " to SulfurClassLoader URL path");
            }
         } catch (Exception e) {
            PrintStream var10000 = System.err;
            String var10001 = String.valueOf(jar.getFileName());
            var10000.println("sulfur/neoforge: failed adding " + var10001 + " to classloader: " + String.valueOf(e) + " - its classes/mixins will not resolve");
         }
      } else if (DEBUG) {
         System.out.println("sulfur/neoforge: SulfurClassLoader.getInstance() was null - cannot add " + String.valueOf(jar.getFileName()) + " to classpath");
      }

      try {
         JarFile jf = new JarFile(jar.toFile());

         try {
            this.processAtFile(jf, jar);
            this.processCoremods(jf, jar);
            this.processModMetadata(jf, jar);
         } catch (Throwable var7) {
            try {
               jf.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }

            throw var7;
         }

         jf.close();
      } catch (IOException e) {
         PrintStream var10 = System.err;
         String var11 = String.valueOf(jar);
         var10.println("sulfur/neoforge: failed processing mod jar " + var11 + ": " + String.valueOf(e));
      }

   }

   private void processAtFile(JarFile jf, Path jar) throws IOException {
      ZipEntry atEntry = jf.getEntry("META-INF/accesstransformer.cfg");
      if (atEntry != null) {
         InputStream in = jf.getInputStream(atEntry);

         try {
            this.atApplier.loadAtFile(in, String.valueOf(jar.getFileName()) + "!META-INF/accesstransformer.cfg");
         } catch (Throwable var8) {
            if (in != null) {
               try {
                  in.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (in != null) {
            in.close();
         }

      }
   }

   private void processCoremods(JarFile jf, Path jar) throws IOException {
      ZipEntry coreEntry = jf.getEntry("META-INF/coremods.json");
      if (coreEntry != null) {
         InputStream in = jf.getInputStream(coreEntry);

         String json;
         try {
            json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
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

         Map<String, String> entries = parseCoremodsJson(json);
         if (entries.isEmpty()) {
            if (DEBUG) {
               System.out.println("sulfur/neoforge: coremods.json in " + String.valueOf(jar.getFileName()) + " is empty or malformed");
            }

         } else {
            if (DEBUG) {
               PrintStream var10000 = System.out;
               String var10001 = String.valueOf(jar.getFileName());
               var10000.println("sulfur/neoforge: coremods.json in " + var10001 + " declares " + entries.size() + " coremod(s)");
            }

            for(Map.Entry<String, String> entry : entries.entrySet()) {
               String transformerKey = (String)entry.getKey();
               String value = (String)entry.getValue();
               boolean isScript = value.endsWith(".js") || value.endsWith(".groovy") || value.endsWith(".ts");
               if (isScript) {
                  System.out.println("sulfur/neoforge: WARNING — script-based coremod '" + transformerKey + "' (" + value + ") in " + String.valueOf(jar.getFileName()) + " is not yet supported (script engine not available at bootstrap time).");
               } else {
                  String className = transformerKey.contains(".") ? transformerKey : (value.contains(".") ? value : null);
                  if (className == null) {
                     if (DEBUG) {
                        System.out.println("sulfur/neoforge: coremod entry '" + transformerKey + "' does not resolve to a class name — skipping");
                     }
                  } else {
                     this.registerJavaCoremod(transformerKey, className, jar);
                  }
               }
            }

         }
      }
   }

   private void registerJavaCoremod(String name, String className, Path jar) {
      try {
         Class<?> cls = Class.forName(className);
         Method getTransformers = null;

         for(Method m : cls.getMethods()) {
            if ("getTransformers".equals(m.getName()) && m.getParameterCount() == 0) {
               getTransformers = m;
               break;
            }
         }

         if (getTransformers == null) {
            System.err.println("sulfur/neoforge: coremod class '" + className + "' has no getTransformers() method — not a valid ICoreMod");
            return;
         }

         Object instance = cls.getDeclaredConstructor().newInstance();
         Iterable<?> transformers = (Iterable)getTransformers.invoke(instance);
         List<String> targets = new ArrayList();

         for(Object t : transformers) {
            Method getTarget = null;
            Method applyMethod = null;

            for(Method m : t.getClass().getMethods()) {
               if (("getTargetClassName".equals(m.getName()) || "target".equals(m.getName())) && m.getParameterCount() == 0) {
                  getTarget = m;
               }

               if ("transform".equals(m.getName()) && m.getParameterCount() == 1) {
                  applyMethod = m;
               }
            }

            if (getTarget != null && applyMethod != null) {
               String target = (String)getTarget.invoke(t);
               targets.add(target);
               String registrationName = name + ":" + target;
               this.coreModLoader.registerCoremod(registrationName, List.of(target), (node) -> {
                  try {
                     applyMethod.invoke(t, node);
                  } catch (Exception ex) {
                     System.err.println("sulfur/coremod: '" + registrationName + "' transform failed: " + String.valueOf(ex));
                  }

               });
            } else if (DEBUG) {
               System.out.println("sulfur/neoforge: coremod transformer " + t.getClass().getName() + " missing target() or transform() — skipping");
            }
         }

         if (DEBUG) {
            System.out.println("sulfur/neoforge: registered Java coremod '" + name + "' (" + className + ") targeting " + String.valueOf(targets));
         }
      } catch (ClassNotFoundException var17) {
         if (DEBUG) {
            System.out.println("sulfur/neoforge: coremod class '" + className + "' not on classpath yet (will miss unless jar is added before class loading)");
         }
      } catch (Exception e) {
         System.err.println("sulfur/neoforge: failed loading Java coremod '" + className + "': " + String.valueOf(e));
      }

   }

   private static Map<String, String> parseCoremodsJson(String json) {
      Map<String, String> result = new LinkedHashMap();
      int i = json.indexOf(123);
      if (i < 0) {
         return result;
      } else {
         int end = json.lastIndexOf(125);
         if (end < i) {
            return result;
         } else {
            String body = json.substring(i + 1, end);

            int vq2;
            for(int pos = 0; pos < body.length(); pos = vq2 + 1) {
               int kq1 = body.indexOf(34, pos);
               if (kq1 < 0) {
                  break;
               }

               int kq2 = body.indexOf(34, kq1 + 1);
               if (kq2 < 0) {
                  break;
               }

               String key = body.substring(kq1 + 1, kq2);
               int colon = body.indexOf(58, kq2 + 1);
               if (colon < 0) {
                  break;
               }

               int vq1 = body.indexOf(34, colon + 1);
               if (vq1 < 0) {
                  break;
               }

               vq2 = body.indexOf(34, vq1 + 1);
               if (vq2 < 0) {
                  break;
               }

               String value = body.substring(vq1 + 1, vq2);
               result.put(key, value);
            }

            return result;
         }
      }
   }

   private void processModMetadata(JarFile jf, Path jar) throws IOException {
      ZipEntry tomlEntry = jf.getEntry("META-INF/neoforge.mods.toml");
      String tomlSource = "META-INF/neoforge.mods.toml";
      if (tomlEntry == null) {
         tomlEntry = jf.getEntry("META-INF/mods.toml");
         tomlSource = "META-INF/mods.toml";
      }

      if (tomlEntry != null) {
         InputStream in = jf.getInputStream(tomlEntry);

         NeoForgeModMetadata meta;
         try {
            String var10001 = String.valueOf(jar.getFileName());
            meta = NeoForgeModMetadata.parse(in, var10001 + "!" + tomlSource);
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
            registerNeoForgeMixinConfig(configPath);
         }

         SulfurModContainer container = new SulfurModContainer(meta.id, meta.version, "neoforge", List.of(jar.toAbsolutePath()), meta.displayName, meta.dependencies);
         System.out.println("sulfur/loader: identified mod: " + meta.id);
         System.out.println("sulfur/loader: ecosystem: neoforge");
         System.out.println("sulfur/loader: version: " + meta.version);
         boolean registered = SulfurModRegistry.getInstance().register(container);
         if (registered) {
            System.out.println("sulfur/loader: registered mod: " + meta.id);
         }

      }
   }

   private static void registerNeoForgeMixinConfig(String configPath) {
      try {
         Class<?> serviceClass = Class.forName("lol.void01n.sulfur.mixinservice.SulfurMixinService");
         Class<?> mixinServiceClass = Class.forName("org.spongepowered.asm.service.MixinService");
         Object service = mixinServiceClass.getMethod("getService").invoke((Object)null);
         if (serviceClass.isInstance(service)) {
            serviceClass.getMethod("registerNeoForgeMixinConfig", String.class).invoke(service, configPath);
            if (DEBUG) {
               System.out.println("sulfur/neoforge: registered mixin config '" + configPath + "'");
            }
         }
      } catch (Exception e) {
         Throwable root;
         for(root = e; root.getCause() != null && root.getCause() != root; root = root.getCause()) {
         }

         System.out.println("sulfur/neoforge: could not register mixin config '" + configPath + "' reflectively: " + String.valueOf(e) + " | root cause: " + String.valueOf(root));
         root.printStackTrace(System.out);
      }

   }
}
