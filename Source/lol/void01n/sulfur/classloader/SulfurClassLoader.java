package lol.void01n.sulfur.classloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import lol.void01n.sulfur.mixinservice.SulfurMixinService;
import lol.void01n.sulfur.ssl.FabricApiForwarder;
import lol.void01n.sulfur.transformengine.SulfurTransformEngine;

public final class SulfurClassLoader extends URLClassLoader {
   private static final boolean DEBUG = System.getProperties().containsKey("sulfur.debug");
   private final SulfurTransformEngine transformEngine;
   private final ClassLoader hostLoader;
   private static final String[] HOST_DELEGATED_PREFIXES;
   private static volatile SulfurClassLoader instance;

   public SulfurClassLoader(URL[] urls, ClassLoader parent, SulfurTransformEngine transformEngine) {
      this(urls, parent, transformEngine, SulfurClassLoader.class.getClassLoader());
   }

   public SulfurClassLoader(URL[] urls, ClassLoader parent, SulfurTransformEngine transformEngine, ClassLoader hostLoader) {
      super("SULFUR", urls, parent);
      this.transformEngine = transformEngine;
      this.hostLoader = hostLoader;
      instance = this;
   }

   public static SulfurClassLoader getInstance() {
      return instance;
   }

   protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      for(String prefix : HOST_DELEGATED_PREFIXES) {
         if (name.startsWith(prefix)) {
            synchronized(this.getClassLoadingLock(name)) {
               Class<?> alreadyLoaded = this.findLoadedClass(name);
               Class<?> cls = alreadyLoaded != null ? alreadyLoaded : Class.forName(name, false, this.hostLoader);
               if (resolve) {
                  this.resolveClass(cls);
               }

               return cls;
            }
         }
      }

      return super.loadClass(name, resolve);
   }

   protected Class<?> findClass(String name) throws ClassNotFoundException {
      byte[] rawBytes;
      try {
         rawBytes = this.getRawClassBytes(name);
      } catch (IOException var9) {
         if (DEBUG) {
            System.out.println("sulfur: failed reading class bytes for " + name + ": " + String.valueOf(var9));
         }

         throw new ClassNotFoundException(name, var9);
      }

      if (rawBytes == null) {
         return super.findClass(name);
      } else {
         byte[] afterEcosystemTransformers = this.transformEngine.maybeTransform(rawBytes, name);
         byte[] afterMixin = SulfurMixinService.applyMixinTransform(name, afterEcosystemTransformers);
         int lastDot = name.lastIndexOf(46);
         if (lastDot > 0) {
            String packageName = name.substring(0, lastDot);
            if (this.getDefinedPackage(packageName) == null) {
               try {
                  this.definePackage(packageName, (String)null, (String)null, (String)null, (String)null, (String)null, (String)null, (URL)null);
               } catch (IllegalArgumentException var8) {
               }
            }
         }

         return this.defineClass(name, afterMixin, 0, afterMixin.length);
      }
   }

   public byte[] getRawClassBytes(String name) throws IOException {
      String internalName = name.replace('.', '/');
      byte[] stubBytes = FabricApiForwarder.getStubBytes(internalName);
      if (stubBytes != null) {
         if (DEBUG) {
            System.out.println("sulfur: loading SSL stub class " + name);
         }

         return stubBytes;
      } else {
         String resourcePath = internalName + ".class";
         InputStream in = this.getResourceAsStream(resourcePath);

         byte[] var10;
         label51: {
            try {
               if (in == null) {
                  var10 = null;
                  break label51;
               }

               var10 = in.readAllBytes();
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

            return var10;
         }

         if (in != null) {
            in.close();
         }

         return var10;
      }
   }

   public Class<?> findLoadedSulfurClass(String name) {
      return this.findLoadedClass(name);
   }

   public void addJar(URL url) {
      this.addURL(url);
   }

   static {
      ClassLoader.registerAsParallelCapable();
      HOST_DELEGATED_PREFIXES = new String[]{"lol.void01n.sulfur.", "org.spongepowered.asm.", "org.objectweb.asm."};
   }
}
