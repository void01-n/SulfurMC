package lol.void01n.sulfur.launch;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import lol.void01n.sulfur.api.SulfurEnvironment;
import lol.void01n.sulfur.api.SulfurLoader;
import lol.void01n.sulfur.classloader.SulfurClassLoader;
import lol.void01n.sulfur.mod.SulfurDependencyResolver;
import lol.void01n.sulfur.mod.SulfurModContainer;
import lol.void01n.sulfur.mod.SulfurModRegistry;
import lol.void01n.sulfur.ssl.FabricModAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public final class SulfurLaunchConsumer implements Consumer<String[]> {
   private static final boolean DEBUG = System.getProperties().containsKey("sulfur.debug");
   private static final String MC_CLIENT_MAIN = "net.minecraft.client.main.Main";
   private static final String MC_SERVER_MAIN = "net.minecraft.server.Main";
   private static final String MC_DATA_MAIN = "net.minecraft.data.Main";
   private static final String MOD_ANNOTATION_DESC = "Lnet/neoforged/fml/common/Mod;";

   public void accept(String[] args) {
      SulfurLoader loader;
      try {
         loader = SulfurLoader.getInstance();
      } catch (IllegalStateException e) {
         System.err.println("sulfur/launch: SulfurLoader not initialized — SulfurBootstrap.run() must call SulfurLoader.init() before handing off to SulfurLaunchConsumer.");
         throw e;
      }

      SulfurModRegistry registry = loader.getModRegistry();
      SulfurEnvironment env = loader.getEnvironment();
      SulfurClassLoader classLoader = loader.getClassLoader();
      if (DEBUG) {
         System.out.println("sulfur/launch: entering launch consumer, env=" + String.valueOf(env));
      }

      int depProblems = SulfurDependencyResolver.validateAll(registry);
      if (depProblems > 0) {
         System.err.println("sulfur/launch: WARNING — " + depProblems + " unsatisfied dependency constraint(s) detected across " + registry.size() + " loaded mod(s). Some mods may not function correctly.");
      }

      registry.dumpToLog();
      Collection<SulfurModContainer> allMods = registry.getAllMods();
      System.out.println("sulfur/launch: " + allMods.size() + " mod(s) registered across all ecosystems.");
      this.invokeNeoForgeEntrypoints(loader, env, classLoader, registry);
      this.invokeQuiltEntrypoints(loader, env, classLoader, registry);
      this.invokeFabricEntrypoints(loader, env);
      this.launchMinecraft(classLoader, env, args);
   }

   private void invokeNeoForgeEntrypoints(SulfurLoader loader, SulfurEnvironment env, SulfurClassLoader classLoader, SulfurModRegistry registry) {
      long neoForgeCount = registry.getAllMods().stream().filter((m) -> "neoforge".equals(m.ecosystem)).count();
      if (neoForgeCount == 0L) {
         if (DEBUG) {
            System.out.println("sulfur/launch: no NeoForge mods, skipping NeoForge entrypoints.");
         }

      } else {
         if (DEBUG) {
            System.out.println("sulfur/launch: scanning " + neoForgeCount + " NeoForge mod jar(s) for @Mod class.");
         }

         for(SulfurModContainer mod : registry.getAllMods()) {
            if ("neoforge".equals(mod.ecosystem)) {
               try {
                  this.initializeNeoForgeMod(mod, classLoader);
               } catch (Exception e) {
                  String var10001 = mod.id;
                  System.err.println("sulfur/launch: error initializing NeoForge mod '" + var10001 + "': " + String.valueOf(e));
               }
            }
         }

      }
   }

   private void initializeNeoForgeMod(SulfurModContainer mod, SulfurClassLoader classLoader) {
      if (!mod.jars.isEmpty()) {
         Path jar = (Path)mod.jars.get(0);
         String modClass = this.findModAnnotatedClass(jar, mod.id);
         if (modClass == null) {
            if (DEBUG) {
               String var10001 = mod.id;
               System.out.println("sulfur/launch: no @Mod(\"" + var10001 + "\") class found in " + String.valueOf(jar.getFileName()) + " — mod may self-init via Mixin/event");
            }

         } else {
            String dotName = modClass.replace('/', '.');
            System.out.println("sulfur/loader: initializing mod: " + mod.id);

            try {
               Class<?> cls = classLoader.loadClass(dotName);
               Object instance = cls.getDeclaredConstructor().newInstance();
               System.out.println("sulfur/loader: initialized mod: " + mod.id);
            } catch (InvocationTargetException e) {
               System.out.println("sulfur/loader: FAILED to initialize mod: " + mod.id);
               System.err.println("sulfur/launch: @Mod class '" + dotName + "' constructor threw:");
               e.getCause().printStackTrace(System.err);
            } catch (LinkageError e) {
               System.out.println("sulfur/loader: FAILED to initialize mod: " + mod.id);
               System.err.println("sulfur/launch: @Mod class '" + dotName + "' failed to link (missing class/method on classpath):");
               e.printStackTrace(System.err);
            } catch (Exception e) {
               System.out.println("sulfur/loader: FAILED to initialize mod: " + mod.id);
               System.err.println("sulfur/launch: failed constructing NeoForge @Mod class '" + dotName + "':");
               e.printStackTrace(System.err);
            }

         }
      }
   }

   private String findModAnnotatedClass(Path jar, String modId) {
      try {
         JarFile jf = new JarFile(jar.toFile());

         label124: {
            String var13;
            try {
               Enumeration<JarEntry> entries = jf.entries();

               while(true) {
                  if (!entries.hasMoreElements()) {
                     break label124;
                  }

                  ZipEntry entry = (ZipEntry)entries.nextElement();
                  if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                     try {
                        InputStream in = jf.getInputStream(entry);

                        label116: {
                           label131: {
                              try {
                                 byte[] bytes = in.readAllBytes();
                                 ClassReader cr = new ClassReader(bytes);
                                 ClassNode cn = new ClassNode();
                                 cr.accept(cn, 7);
                                 if (cn.visibleAnnotations == null) {
                                    break label131;
                                 }

                                 Iterator var10 = cn.visibleAnnotations.iterator();

                                 label111:
                                 while(true) {
                                    if (!var10.hasNext()) {
                                       break label116;
                                    }

                                    AnnotationNode ann = (AnnotationNode)var10.next();
                                    if ("Lnet/neoforged/fml/common/Mod;".equals(ann.desc) && ann.values != null) {
                                       for(int i = 0; i + 1 < ann.values.size(); i += 2) {
                                          if ("value".equals(ann.values.get(i)) && modId.equals(ann.values.get(i + 1))) {
                                             var13 = cn.name;
                                             break label111;
                                          }
                                       }
                                    }
                                 }
                              } catch (Throwable var16) {
                                 if (in != null) {
                                    try {
                                       in.close();
                                    } catch (Throwable var15) {
                                       var16.addSuppressed(var15);
                                    }
                                 }

                                 throw var16;
                              }

                              if (in != null) {
                                 in.close();
                              }
                              break;
                           }

                           if (in != null) {
                              in.close();
                           }
                           continue;
                        }

                        if (in != null) {
                           in.close();
                        }
                     } catch (Exception var17) {
                     }
                  }
               }
            } catch (Throwable var18) {
               try {
                  jf.close();
               } catch (Throwable var14) {
                  var18.addSuppressed(var14);
               }

               throw var18;
            }

            jf.close();
            return var13;
         }

         jf.close();
      } catch (IOException e) {
         if (DEBUG) {
            PrintStream var10000 = System.out;
            String var10001 = String.valueOf(jar.getFileName());
            var10000.println("sulfur/launch: I/O error scanning " + var10001 + " for @Mod: " + String.valueOf(e));
         }
      }

      return null;
   }

   private void invokeQuiltEntrypoints(SulfurLoader loader, SulfurEnvironment env, SulfurClassLoader classLoader, SulfurModRegistry registry) {
      long quiltCount = registry.getAllMods().stream().filter((m) -> "quilt".equals(m.ecosystem)).count();
      if (quiltCount == 0L) {
         if (DEBUG) {
            System.out.println("sulfur/launch: no Quilt mods, skipping Quilt entrypoints.");
         }

      } else {
         if (DEBUG) {
            System.out.println("sulfur/launch: invoking Quilt entrypoints for " + quiltCount + " mod(s).");
         }

         for(SulfurModContainer mod : registry.getAllMods()) {
            if ("quilt".equals(mod.ecosystem)) {
               this.invokeQuiltModEntrypoints(mod, classLoader, env);
            }
         }

      }
   }

   private void invokeQuiltModEntrypoints(SulfurModContainer mod, SulfurClassLoader classLoader, SulfurEnvironment env) {
      if (!mod.jars.isEmpty()) {
         System.out.println("sulfur/loader: initializing mod: " + mod.id);
         boolean[] failed = new boolean[]{false};
         this.invokeQuiltModEntrypointsInner(mod, classLoader, env, failed);
         if (failed[0]) {
            System.out.println("sulfur/loader: FAILED to initialize mod: " + mod.id);
         } else {
            System.out.println("sulfur/loader: initialized mod: " + mod.id);
         }

      }
   }

   private void invokeQuiltModEntrypointsInner(SulfurModContainer mod, SulfurClassLoader classLoader, SulfurEnvironment env, boolean[] failed) {
      if (!mod.jars.isEmpty()) {
         Path jar = (Path)mod.jars.get(0);

         String json;
         try {
            JarFile jf = new JarFile(jar.toFile());

            label83: {
               try {
                  ZipEntry qmjEntry = jf.getEntry("quilt.mod.json");
                  if (qmjEntry != null) {
                     InputStream in = jf.getInputStream(qmjEntry);

                     try {
                        json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                     } catch (Throwable var14) {
                        if (in != null) {
                           try {
                              in.close();
                           } catch (Throwable var13) {
                              var14.addSuppressed(var13);
                           }
                        }

                        throw var14;
                     }

                     if (in != null) {
                        in.close();
                     }
                     break label83;
                  }
               } catch (Throwable var15) {
                  try {
                     jf.close();
                  } catch (Throwable var12) {
                     var15.addSuppressed(var12);
                  }

                  throw var15;
               }

               jf.close();
               return;
            }

            jf.close();
         } catch (IOException e) {
            if (DEBUG) {
               PrintStream var10000 = System.out;
               String var10001 = String.valueOf(jar.getFileName());
               var10000.println("sulfur/launch: could not read quilt.mod.json from " + var10001 + ": " + String.valueOf(e));
            }

            return;
         }

         List<String> initClasses = extractQuiltEntrypoints(json, "init");
         List<String> clientInitClasses = extractQuiltEntrypoints(json, "client_init");
         List<String> serverInitClasses = extractQuiltEntrypoints(json, "server_init");
         if (initClasses.isEmpty()) {
            initClasses = extractQuiltEntrypoints(json, "main");
         }

         if (clientInitClasses.isEmpty()) {
            clientInitClasses = extractQuiltEntrypoints(json, "client");
         }

         if (serverInitClasses.isEmpty()) {
            serverInitClasses = extractQuiltEntrypoints(json, "server");
         }

         failed[0] |= this.invokeEntrypointList(initClasses, classLoader, mod.id, "init");
         if (env.isClient()) {
            failed[0] |= this.invokeEntrypointList(clientInitClasses, classLoader, mod.id, "client_init");
         }

         if (env.isServer()) {
            failed[0] |= this.invokeEntrypointList(serverInitClasses, classLoader, mod.id, "server_init");
         }

      }
   }

   private static List<String> extractQuiltEntrypoints(String json, String key) {
      List<String> result = new ArrayList();

      for(String context : new String[]{json, extractJsonObject(json, "quilt_loader")}) {
         if (context != null) {
            int idx = context.indexOf("\"entrypoints\"");
            if (idx >= 0) {
               int colon = context.indexOf(58, idx + "\"entrypoints\"".length());
               if (colon >= 0) {
                  int braceOpen = context.indexOf(123, colon + 1);
                  if (braceOpen >= 0) {
                     int braceClose = findClosing(context, braceOpen, '{', '}');
                     String epBlock = context.substring(braceOpen + 1, braceClose);
                     String keySearch = "\"" + key + "\"";
                     int keyIdx = epBlock.indexOf(keySearch);
                     if (keyIdx >= 0) {
                        int keyColon = epBlock.indexOf(58, keyIdx + keySearch.length());
                        if (keyColon >= 0) {
                           int valueStart;
                           for(valueStart = keyColon + 1; valueStart < epBlock.length() && Character.isWhitespace(epBlock.charAt(valueStart)); ++valueStart) {
                           }

                           if (valueStart < epBlock.length()) {
                              char first = epBlock.charAt(valueStart);
                              if (first == '[') {
                                 int arrEnd = findClosing(epBlock, valueStart, '[', ']');
                                 String arr = epBlock.substring(valueStart + 1, arrEnd);
                                 extractEntrypointClassNames(arr, result);
                              } else if (first == '"') {
                                 int q2 = epBlock.indexOf(34, valueStart + 1);
                                 if (q2 > valueStart) {
                                    addEntrypointClassName(epBlock.substring(valueStart + 1, q2), result);
                                 }
                              }

                              if (!result.isEmpty()) {
                                 break;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return result;
   }

   private static void extractEntrypointClassNames(String arrayContent, List<String> out) {
      int i = 0;

      while(i < arrayContent.length()) {
         while(i < arrayContent.length() && (Character.isWhitespace(arrayContent.charAt(i)) || arrayContent.charAt(i) == ',')) {
            ++i;
         }

         if (i >= arrayContent.length()) {
            break;
         }

         char c = arrayContent.charAt(i);
         if (c == '{') {
            int objEnd = findClosing(arrayContent, i, '{', '}');
            String obj = arrayContent.substring(i + 1, objEnd);
            String val = extractJsonStringValue(obj, "value");
            if (val != null) {
               addEntrypointClassName(val, out);
            }

            i = objEnd + 1;
         } else if (c == '"') {
            int q2 = arrayContent.indexOf(34, i + 1);
            if (q2 < 0) {
               break;
            }

            addEntrypointClassName(arrayContent.substring(i + 1, q2), out);
            i = q2 + 1;
         } else {
            ++i;
         }
      }

   }

   private static void addEntrypointClassName(String raw, List<String> out) {
      if (raw != null && !raw.isBlank()) {
         int colonColon = raw.indexOf("::");
         String className = (colonColon > 0 ? raw.substring(0, colonColon) : raw).trim();
         if (!className.isEmpty()) {
            out.add(className);
         }

      }
   }

   private boolean invokeEntrypointList(List<String> classNames, SulfurClassLoader classLoader, String modId, String category) {
      boolean anyFailed = false;

      for(String className : classNames) {
         try {
            Class<?> cls = classLoader.loadClass(className);
            Object instance = cls.getDeclaredConstructor().newInstance();
            boolean invoked = tryInvoke(instance, cls, "onInitialize") || tryInvoke(instance, cls, "init") || tryInvoke(instance, cls, "onLoad");
            if (!invoked && DEBUG) {
               System.out.println("sulfur/launch: Quilt entrypoint " + className + " has no standard initializer method (onInitialize/init/onLoad)");
            }

            if (invoked && DEBUG) {
               System.out.println("sulfur/launch: invoked Quilt entrypoint " + className + " (" + category + ") from " + modId);
            }
         } catch (ClassNotFoundException e) {
            anyFailed = true;
            System.err.println("sulfur/launch: Quilt entrypoint class '" + className + "' not found (mod: " + modId + "): " + e.getMessage());
         } catch (InvocationTargetException e) {
            anyFailed = true;
            System.err.println("sulfur/launch: Quilt entrypoint '" + className + "' (mod: " + modId + ") threw:");
            e.getCause().printStackTrace(System.err);
         } catch (Exception e) {
            anyFailed = true;
            System.err.println("sulfur/launch: failed invoking Quilt entrypoint '" + className + "' (mod: " + modId + "):");
            e.printStackTrace(System.err);
         }
      }

      return anyFailed;
   }

   private static boolean tryInvoke(Object instance, Class<?> cls, String methodName) {
      try {
         Method m = cls.getMethod(methodName);
         m.invoke(instance);
         return true;
      } catch (NoSuchMethodException var6) {
         return false;
      } catch (InvocationTargetException e) {
         Throwable cause = e.getCause();
         if (cause instanceof RuntimeException re) {
            throw re;
         } else if (cause instanceof Error err) {
            throw err;
         } else {
            throw new RuntimeException("Entrypoint " + methodName + "() threw", cause);
         }
      } catch (IllegalAccessException var8) {
         return false;
      }
   }

   private void invokeFabricEntrypoints(SulfurLoader loader, SulfurEnvironment env) {
      long fabricCount = loader.getModRegistry().getAllMods().stream().filter((m) -> "fabric".equals(m.ecosystem)).count();
      if (fabricCount == 0L) {
         if (DEBUG) {
            System.out.println("sulfur/launch: no Fabric-ecosystem mods, skipping SSL entrypoints.");
         }

      } else {
         if (DEBUG) {
            System.out.println("sulfur/launch: invoking Fabric entrypoints for " + fabricCount + " mod(s).");
         }

         FabricModAdapter fabricAdapter = SulfurFabricAdapterHolder.get();
         if (fabricAdapter == null) {
            if (DEBUG) {
               System.out.println("sulfur/launch: no FabricModAdapter instance available (normal if no pure-Fabric mods processed).");
            }

         } else {
            fabricAdapter.invokeEntrypoints("main", loader.getClassLoader());
            if (env.isClient()) {
               fabricAdapter.invokeEntrypoints("client", loader.getClassLoader());
            }

            if (env.isServer()) {
               fabricAdapter.invokeEntrypoints("server", loader.getClassLoader());
            }

         }
      }
   }

   private void launchMinecraft(SulfurClassLoader classLoader, SulfurEnvironment env, String[] args) {
      String var10000;
      switch (env) {
         case CLIENT -> var10000 = "net.minecraft.client.main.Main";
         case SERVER -> var10000 = "net.minecraft.server.Main";
         case DATA -> var10000 = "net.minecraft.data.Main";
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      String mainClass = var10000;
      System.out.println("sulfur/launch: launching Minecraft — main class: " + mainClass + " (env=" + String.valueOf(env) + ")");

      try {
         Class<?> main = classLoader.loadClass(mainClass);
         Method mainMethod = main.getMethod("main", String[].class);
         mainMethod.invoke((Object)null, args);
      } catch (ClassNotFoundException var8) {
         System.err.println("sulfur/launch: Minecraft main class '" + mainClass + "' not found. Ensure the Minecraft jar (or a stub) is on legacyClassPath.");
         System.err.println("  To run Sulfur standalone without Minecraft, this is expected.");
      } catch (InvocationTargetException e) {
         Throwable cause = e.getCause();
         if (cause instanceof RuntimeException re) {
            throw re;
         }

         if (cause instanceof Error err) {
            throw err;
         }

         throw new RuntimeException("Minecraft threw an uncaught exception", cause);
      } catch (NoSuchMethodException var10) {
         System.err.println("sulfur/launch: " + mainClass + " has no main(String[]) method — is this the correct main class for 1.21.4?");
      } catch (Exception e) {
         throw new RuntimeException("Failed to launch Minecraft via " + mainClass, e);
      }

   }

   private static String extractJsonObject(String json, String key) {
      String search = "\"" + key + "\"";
      int idx = json.indexOf(search);
      if (idx < 0) {
         return null;
      } else {
         int colon = json.indexOf(58, idx + search.length());
         if (colon < 0) {
            return null;
         } else {
            int braceOpen = json.indexOf(123, colon + 1);
            if (braceOpen < 0) {
               return null;
            } else {
               int braceClose = findClosing(json, braceOpen, '{', '}');
               return json.substring(braceOpen + 1, braceClose);
            }
         }
      }
   }

   private static String extractJsonStringValue(String json, String key) {
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
}
