package lol.void01n.sulfur.bootstrap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.lang.module.Configuration;
import java.lang.module.FindException;
import java.lang.module.ModuleFinder;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import lol.void01n.sulfur.api.SulfurEnvironment;
import lol.void01n.sulfur.api.SulfurLoader;
import lol.void01n.sulfur.classloader.SulfurClassLoader;
import lol.void01n.sulfur.mixinservice.SulfurMixinService;
import lol.void01n.sulfur.mod.SulfurModRegistry;
import lol.void01n.sulfur.mod.SulfurModScanner;
import lol.void01n.sulfur.mod.SulfurModsDirectory;
import lol.void01n.sulfur.quiltboot.SulfurFileSystems;
import lol.void01n.sulfur.ssl.SulfurSSL;
import lol.void01n.sulfur.transformengine.SulfurTransformEngine;

public final class SulfurBootstrap {
   private static final boolean DEBUG = System.getProperties().containsKey("sulfur.debug");

   private SulfurBootstrap() {
   }

   public static void main(String... args) {
      boolean classloaderIsolation = Boolean.parseBoolean(System.getProperty("sulfur.classloaderIsolation", "true"));
      run(classloaderIsolation, args);
   }

   private static void run(boolean classloaderIsolation, String... args) {
      if (DEBUG) {
         System.out.println("sulfur: starting bootstrap, classloaderIsolation=" + classloaderIsolation);
      }

      detectAndSetGameDir(args);
      setupBootLog();
      System.out.println("sulfur/loader: starting Sulfur mod discovery");
      System.out.println("sulfur/loader: game directory = " + String.valueOf(SulfurModsDirectory.resolveGameDir().toAbsolutePath()));
      System.out.println("sulfur/loader: mods directory = " + String.valueOf(SulfurModsDirectory.resolve().toAbsolutePath()));
      SulfurFileSystems.registerAll();
      List<String> legacyClasspath = loadLegacyClassPath();
      System.setProperty("legacyClassPath", String.join(File.pathSeparator, legacyClasspath));
      List<Path> jarPaths = resolveJarPaths(legacyClasspath);
      ArrayList<Path> modularJarPaths = new ArrayList();

      for(Path candidate : jarPaths) {
         try {
            ModuleFinder.of(candidate).findAll();
            modularJarPaths.add(candidate);
         } catch (FindException e) {
            if (DEBUG) {
               PrintStream var10000 = System.out;
               String var10001 = String.valueOf(candidate.getFileName());
               var10000.println("sulfur: skipping '" + var10001 + "' for module-layer construction (not a valid automatic module: " + e.getMessage() + ") — it stays on SulfurClassLoader's URL classpath regardless.");
            }
         }
      }

      ModuleFinder jarModuleFinder = ModuleFinder.of((Path[])modularJarPaths.toArray((x$0) -> new Path[x$0]));
      Configuration bootModuleConfiguration = ModuleLayer.boot().configuration();
      List<String> moduleNames = jarModuleFinder.findAll().stream().map((ref) -> ref.descriptor().name()).toList();

      Configuration bootstrapConfiguration;
      try {
         bootstrapConfiguration = bootModuleConfiguration.resolveAndBind(jarModuleFinder, ModuleFinder.ofSystem(), moduleNames);
      } catch (FindException e) {
         if (DEBUG) {
            System.out.println("sulfur: no resolvable modules on classpath, using empty bootstrap layer (" + e.getMessage() + ")");
         }

         bootstrapConfiguration = bootModuleConfiguration.resolveAndBind(ModuleFinder.of(), ModuleFinder.ofSystem(), List.of());
      }

      ClassLoader parentLoader = classloaderIsolation ? null : Thread.currentThread().getContextClassLoader();
      SulfurTransformEngine transformEngine = new SulfurTransformEngine();
      SulfurClassLoader moduleClassLoader = new SulfurClassLoader((URL[])jarPaths.stream().map(SulfurBootstrap::toUrl).toArray((x$0) -> new URL[x$0]), parentLoader, transformEngine, SulfurBootstrap.class.getClassLoader());
      SulfurMixinService.bind(moduleClassLoader);
      transformEngine.initialize();
      System.out.println("sulfur/loader: discovered " + SulfurModRegistry.getInstance().size() + " mod candidate(s)");
      SulfurSSL.initialize(transformEngine);
      ModuleLayer.Controller layer = ModuleLayer.defineModules(bootstrapConfiguration, List.of(ModuleLayer.boot()), (m) -> moduleClassLoader);
      Thread.currentThread().setContextClassLoader(moduleClassLoader);
      SulfurEnvironment env = SulfurEnvironment.detect(args);
      SulfurModRegistry registry = SulfurModRegistry.getInstance();
      SulfurLoader.init(moduleClassLoader, transformEngine, registry, env);
      if (DEBUG) {
         PrintStream var21 = System.out;
         String var22 = String.valueOf(env);
         var21.println("sulfur: SulfurLoader initialized — env=" + var22 + ", mods=" + registry.size());
      }

      SulfurModScanner scanner = new SulfurModScanner(registry, transformEngine, moduleClassLoader);
      scanner.start();
      Consumer<String[]> consumer = findLaunchConsumer(layer.layer(), moduleClassLoader);
      if (consumer == null) {
         if (DEBUG) {
            System.out.println("sulfur: no Consumer service found — stopping after module-layer setup. Ensure SulfurLaunchConsumer is declared in META-INF/services/java.util.function.Consumer.");
         }

      } else {
         consumer.accept(args);
      }
   }

   private static Consumer<String[]> findLaunchConsumer(ModuleLayer layer, ClassLoader cl) {
      ServiceLoader<Consumer> layerLoader = ServiceLoader.load(layer, Consumer.class);
      Optional<ServiceLoader.Provider<Consumer>> layerFirst = layerLoader.stream().findFirst();
      if (layerFirst.isPresent()) {
         if (DEBUG) {
            System.out.println("sulfur: found Consumer via module-layer ServiceLoader");
         }

         return (Consumer)((ServiceLoader.Provider)layerFirst.get()).get();
      } else {
         ServiceLoader<Consumer> clLoader = ServiceLoader.load(Consumer.class, cl);
         Optional<ServiceLoader.Provider<Consumer>> clFirst = clLoader.stream().findFirst();
         if (clFirst.isPresent()) {
            if (DEBUG) {
               System.out.println("sulfur: found Consumer via classloader ServiceLoader (unnamed module fallback)");
            }

            return (Consumer)((ServiceLoader.Provider)clFirst.get()).get();
         } else {
            return null;
         }
      }
   }

   private static void setupBootLog() {
      try {
         Path gameDir = Paths.get(System.getProperty("sulfur.gameDir"));
         Path logFile = gameDir.resolve("sulfur-boot.log");
         PrintStream fileOut = new PrintStream(new FileOutputStream(logFile.toFile(), true), true, "UTF-8");
         System.setOut(new PrintStream(new TeeOutputStream(System.out, fileOut), true, "UTF-8"));
         System.setErr(new PrintStream(new TeeOutputStream(System.err, fileOut), true, "UTF-8"));
         fileOut.println("===== Sulfur boot log — new run =====");
      } catch (Exception var3) {
      }

   }

   private static void detectAndSetGameDir(String[] args) {
      if (System.getProperty("sulfur.gameDir") == null) {
         for(int i = 0; i < args.length - 1; ++i) {
            if ("--gameDir".equals(args[i])) {
               System.setProperty("sulfur.gameDir", args[i + 1]);
               return;
            }
         }

         System.setProperty("sulfur.gameDir", Paths.get("").toAbsolutePath().toString());
      }
   }

   private static List<Path> resolveJarPaths(List<String> legacyClasspath) {
      ArrayList<Path> paths = new ArrayList();

      for(String entry : legacyClasspath) {
         Path path = Paths.get(entry);
         if (Files.exists(path, new LinkOption[0])) {
            paths.add(path);
         } else if (DEBUG) {
            System.out.println("sulfur: skipping missing classpath entry '" + entry + "'");
         }
      }

      return paths;
   }

   private static URL toUrl(Path path) {
      try {
         return path.toUri().toURL();
      } catch (MalformedURLException e) {
         throw new UncheckedIOException(new IOException("Failed to convert " + String.valueOf(path) + " to a URL", e));
      }
   }

   private static List<String> loadLegacyClassPath() {
      String legacyCpPath = System.getProperty("legacyClassPath.file");
      if (legacyCpPath != null) {
         try {
            return Files.readAllLines(Paths.get(legacyCpPath));
         } catch (IOException e) {
            throw new IllegalStateException("Failed to load the legacy class path from the specified file: " + legacyCpPath, e);
         }
      } else {
         String legacyClasspath = System.getProperty("legacyClassPath", System.getProperty("java.class.path"));
         Objects.requireNonNull(legacyClasspath, "Missing legacyClassPath, cannot bootstrap Sulfur");
         return legacyClasspath.isEmpty() ? List.of() : Arrays.asList(legacyClasspath.split(File.pathSeparator));
      }
   }

   private static final class TeeOutputStream extends OutputStream {
      private final OutputStream a;
      private final OutputStream b;

      TeeOutputStream(OutputStream a, OutputStream b) {
         this.a = a;
         this.b = b;
      }

      public void write(int b1) throws IOException {
         this.a.write(b1);
         this.b.write(b1);
      }

      public void write(byte[] buf, int off, int len) throws IOException {
         this.a.write(buf, off, len);
         this.b.write(buf, off, len);
      }

      public void flush() throws IOException {
         this.a.flush();
         this.b.flush();
      }
   }
}
