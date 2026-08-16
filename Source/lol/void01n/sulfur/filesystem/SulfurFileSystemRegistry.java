package lol.void01n.sulfur.filesystem;

import java.lang.ref.WeakReference;
import java.net.URI;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;

public final class SulfurFileSystemRegistry<FS extends SulfurBaseFileSystem<FS, ?>> {
   private final String scheme;
   private final Map<String, WeakReference<FS>> active = new HashMap();

   public SulfurFileSystemRegistry(String scheme) {
      this.scheme = scheme;
   }

   public synchronized void register(FS fs) {
      String var10000 = this.scheme;
      URI probe = URI.create(var10000 + "://" + fs.name() + "/probe");
      if (!"/probe".equals(probe.getPath())) {
         throw new IllegalArgumentException("Filesystem name contains a path separator: '" + fs.name() + "'");
      } else {
         WeakReference<FS> oldRef = (WeakReference)this.active.get(fs.name());
         if (oldRef != null && oldRef.get() != null) {
            throw new IllegalStateException("Filesystem already registered for name '" + fs.name() + "'");
         } else {
            this.active.put(fs.name(), new WeakReference(fs));
         }
      }
   }

   public synchronized void close(FS fs) {
      this.active.remove(fs.name());
   }

   public synchronized FS get(String name) {
      WeakReference<FS> ref = (WeakReference)this.active.get(name);
      return (FS)(ref != null ? (SulfurBaseFileSystem)ref.get() : null);
   }

   public synchronized FS get(URI uri) {
      if (!this.scheme.equals(uri.getScheme())) {
         String var10002 = this.scheme;
         throw new IllegalArgumentException("Wrong scheme for this registry: expected '" + var10002 + "', got '" + uri.getScheme() + "'");
      } else {
         String authority = uri.getAuthority();
         if (authority == null) {
            authority = uri.getHost();
         } else if (authority.endsWith(":0")) {
            authority = authority.substring(0, authority.length() - 2);
         }

         FS fs = this.get(authority);
         if (fs == null) {
            throw new FileSystemNotFoundException("No active filesystem for name '" + authority + "' (scheme: " + this.scheme + ")");
         } else {
            return fs;
         }
      }
   }

   public static <P extends FileSystemProvider> P findInstalledProvider(Class<P> type) {
      for(FileSystemProvider p : FileSystemProvider.installedProviders()) {
         if (type.isInstance(p)) {
            return (P)p;
         }
      }

      return null;
   }

   static {
      String key = "java.protocol.handler.pkgs";
      String pkg = "lol.void01n.sulfur.filesystem";
      String prop = System.getProperty("java.protocol.handler.pkgs");
      if (prop == null) {
         System.setProperty("java.protocol.handler.pkgs", "lol.void01n.sulfur.filesystem");
      } else if (!prop.contains("lol.void01n.sulfur.filesystem")) {
         System.setProperty("java.protocol.handler.pkgs", prop + "|lol.void01n.sulfur.filesystem");
      }

   }
}
