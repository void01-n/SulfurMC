package org.spongepowered.asm.launch.platform;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.spongepowered.asm.util.Files;
import org.spongepowered.asm.util.JavaVersion;
import org.spongepowered.include.com.google.common.io.ByteSource;

public final class MainAttributes {
   private static final Map<URI, MainAttributes> instances = new HashMap();
   protected final Attributes attributes;

   private MainAttributes() {
      this.attributes = new Attributes();
   }

   private MainAttributes(URI codeSource) {
      this.attributes = getAttributes(codeSource);
   }

   public final String get(String name) {
      return this.attributes != null ? this.attributes.getValue(name) : null;
   }

   public final String get(Attributes.Name name) {
      return this.attributes != null ? this.attributes.getValue(name) : null;
   }

   private static Attributes getAttributes(URI codeSource) {
      if (codeSource == null) {
         return null;
      } else {
         if ("file".equals(codeSource.getScheme())) {
            File file = Files.toFile(codeSource);
            if (file.isFile()) {
               Attributes attributes = getJarAttributes(file);
               if (attributes != null) {
                  return attributes;
               }
            } else if (file.isDirectory()) {
               Attributes attributes = getDirAttributes(file);
               if (attributes != null) {
                  return attributes;
               }
            }
         } else if (JavaVersion.current() >= 1.7) {
            Attributes attributes = getNioAttributes(codeSource);
            if (attributes != null) {
               return attributes;
            }
         }

         return new Attributes();
      }
   }

   private static Attributes getJarAttributes(File jar) {
      JarFile jarFile = null;

      Attributes var3;
      try {
         jarFile = new JarFile(jar);
         Manifest manifest = jarFile.getManifest();
         if (manifest == null) {
            return null;
         }

         var3 = manifest.getMainAttributes();
      } catch (IOException var14) {
         return null;
      } finally {
         try {
            if (jarFile != null) {
               jarFile.close();
            }
         } catch (IOException var13) {
         }

      }

      return var3;
   }

   private static Attributes getDirAttributes(File dir) {
      File manifestFile = new File(dir, "META-INF/MANIFEST.MF");
      if (manifestFile.isFile()) {
         ByteSource source = org.spongepowered.include.com.google.common.io.Files.asByteSource(manifestFile);
         InputStream inputStream = null;

         Attributes var5;
         try {
            inputStream = source.openBufferedStream();
            Manifest manifest = new Manifest(inputStream);
            var5 = manifest.getMainAttributes();
         } catch (IOException var15) {
            return null;
         } finally {
            try {
               if (inputStream != null) {
                  inputStream.close();
               }
            } catch (IOException var14) {
            }

         }

         return var5;
      } else {
         return null;
      }
   }

   private static Attributes getNioAttributes(URI uri) {
      try {
         Path manifestPath = Paths.get(uri).resolve("META-INF/MANIFEST.MF");
         BufferedInputStream inputStream = null;

         Attributes var4;
         try {
            inputStream = new BufferedInputStream(java.nio.file.Files.newInputStream(manifestPath));
            Manifest manifest = new Manifest(inputStream);
            var4 = manifest.getMainAttributes();
         } catch (IOException var16) {
            return null;
         } finally {
            try {
               if (inputStream != null) {
                  inputStream.close();
               }
            } catch (IOException var15) {
            }

         }

         return var4;
      } catch (FileSystemNotFoundException ex) {
         ex.printStackTrace();
      } catch (InvalidPathException ex) {
         ex.printStackTrace();
      }

      return null;
   }

   public static MainAttributes of(File jar) {
      return of(jar.toURI());
   }

   public static MainAttributes of(URI uri) {
      MainAttributes attributes = (MainAttributes)instances.get(uri);
      if (attributes == null) {
         attributes = new MainAttributes(uri);
         instances.put(uri, attributes);
      }

      return attributes;
   }
}
