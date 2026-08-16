package lol.void01n.sulfur.filesystem;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SulfurUnifiedFileSystemProvider extends FileSystemProvider {
   public static final String SCHEME = "quilt.ufs";
   private static SulfurUnifiedFileSystemProvider INSTANCE;
   static final SulfurFileSystemRegistry<SulfurUnifiedFileSystem> REGISTRY = new SulfurFileSystemRegistry<SulfurUnifiedFileSystem>("quilt.ufs");

   public SulfurUnifiedFileSystemProvider() {
      if (INSTANCE == null) {
         INSTANCE = this;
      }

   }

   public static SulfurUnifiedFileSystemProvider instance() {
      if (INSTANCE != null) {
         return INSTANCE;
      } else {
         SulfurUnifiedFileSystemProvider found = (SulfurUnifiedFileSystemProvider)SulfurFileSystemRegistry.findInstalledProvider(SulfurUnifiedFileSystemProvider.class);
         if (found != null) {
            return found;
         } else {
            throw new IllegalStateException("SulfurUnifiedFileSystemProvider not found via installed providers");
         }
      }
   }

   public String getScheme() {
      return "quilt.ufs";
   }

   public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
      String name = uri.getAuthority();
      if (name != null && !name.isBlank()) {
         Object backing = env.get("backingPaths");
         if (!(backing instanceof List)) {
            throw new IllegalArgumentException("env must contain 'backingPaths' -> List<Path>");
         } else {
            List<Path> paths = (List)backing;
            SulfurUnifiedFileSystem fs = new SulfurUnifiedFileSystem(name, paths, this);
            REGISTRY.register(fs);
            return fs;
         }
      } else {
         throw new IllegalArgumentException("URI must have an authority: " + String.valueOf(uri));
      }
   }

   public FileSystem getFileSystem(URI uri) {
      return REGISTRY.get(uri);
   }

   public Path getPath(URI uri) {
      SulfurUnifiedFileSystem fs = (SulfurUnifiedFileSystem)REGISTRY.get(uri);
      return fs.getPath(uri.getPath());
   }

   public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
      SulfurUnifiedPath up = toUnified(path);

      for(Path backing : up.fs.backingPaths) {
         Path candidate = backing.resolve(up.relativePath);
         if (Files.exists(candidate, new LinkOption[0])) {
            return Files.newByteChannel(candidate, options, attrs);
         }
      }

      throw new NoSuchFileException(path.toString());
   }

   public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
      SulfurUnifiedPath udir = toUnified(dir);
      Set<String> seen = new LinkedHashSet();
      final List<Path> results = new ArrayList();

      for(Path backing : udir.fs.backingPaths) {
         Path candidate = backing.resolve(udir.relativePath);
         if (Files.isDirectory(candidate, new LinkOption[0])) {
            DirectoryStream<Path> stream = Files.newDirectoryStream(candidate);

            try {
               for(Path entry : stream) {
                  String fname = entry.getFileName().toString();
                  if (seen.add(fname)) {
                     String childRel = udir.relativePath.isEmpty() ? fname : udir.relativePath + "/" + fname;
                     SulfurUnifiedPath child = new SulfurUnifiedPath(udir.fs, childRel);
                     if (filter.accept(child)) {
                        results.add(child);
                     }
                  }
               }
            } catch (Throwable var16) {
               if (stream != null) {
                  try {
                     stream.close();
                  } catch (Throwable var15) {
                     var16.addSuppressed(var15);
                  }
               }

               throw var16;
            }

            if (stream != null) {
               stream.close();
            }
         }
      }

      return new DirectoryStream<Path>() {
         public Iterator<Path> iterator() {
            return results.iterator();
         }

         public void close() {
         }
      };
   }

   public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
      throw new IOException("SulfurUnifiedFileSystem is read-only");
   }

   public void delete(Path path) throws IOException {
      throw new IOException("SulfurUnifiedFileSystem is read-only");
   }

   public void copy(Path source, Path target, CopyOption... options) throws IOException {
      SulfurUnifiedPath us = toUnified(source);

      for(Path backing : us.fs.backingPaths) {
         Path candidate = backing.resolve(us.relativePath);
         if (Files.exists(candidate, new LinkOption[0])) {
            Files.copy(candidate, target, options);
            return;
         }
      }

      throw new NoSuchFileException(source.toString());
   }

   public void move(Path source, Path target, CopyOption... options) throws IOException {
      throw new IOException("SulfurUnifiedFileSystem is read-only");
   }

   public boolean isSameFile(Path path, Path path2) {
      return path.toAbsolutePath().normalize().equals(path2.toAbsolutePath().normalize());
   }

   public boolean isHidden(Path path) {
      String name = path.getFileName() == null ? "" : path.getFileName().toString();
      return name.startsWith(".");
   }

   public FileStore getFileStore(Path path) throws IOException {
      SulfurUnifiedPath up = toUnified(path);

      for(Path backing : up.fs.backingPaths) {
         Path candidate = backing.resolve(up.relativePath);
         if (Files.exists(candidate, new LinkOption[0])) {
            return Files.getFileStore(candidate);
         }
      }

      throw new NoSuchFileException(path.toString());
   }

   public void checkAccess(Path path, AccessMode... modes) throws IOException {
      for(AccessMode m : modes) {
         if (m == AccessMode.WRITE) {
            throw new IOException("SulfurUnifiedFileSystem is read-only");
         }
      }

      SulfurUnifiedPath up = toUnified(path);

      for(Path backing : up.fs.backingPaths) {
         Path candidate = backing.resolve(up.relativePath);
         if (Files.exists(candidate, new LinkOption[0])) {
            return;
         }
      }

      throw new NoSuchFileException(path.toString());
   }

   public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... opts) {
      SulfurUnifiedPath up = toUnified(path);

      for(Path backing : up.fs.backingPaths) {
         Path candidate = backing.resolve(up.relativePath);
         if (Files.exists(candidate, new LinkOption[0])) {
            V v = Files.getFileAttributeView(candidate, type, opts);
            if (v != null) {
               return v;
            }
         }
      }

      return null;
   }

   public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... opts) throws IOException {
      SulfurUnifiedPath up = toUnified(path);

      for(Path backing : up.fs.backingPaths) {
         Path candidate = backing.resolve(up.relativePath);

         try {
            return (A)Files.readAttributes(candidate, type, opts);
         } catch (NoSuchFileException var9) {
         }
      }

      throw new NoSuchFileException(path.toString());
   }

   public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... opts) throws IOException {
      SulfurUnifiedPath up = toUnified(path);

      for(Path backing : up.fs.backingPaths) {
         Path candidate = backing.resolve(up.relativePath);

         try {
            return Files.readAttributes(candidate, attributes, opts);
         } catch (NoSuchFileException var9) {
         }
      }

      throw new NoSuchFileException(path.toString());
   }

   public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
      throw new IOException("SulfurUnifiedFileSystem is read-only");
   }

   private static SulfurUnifiedPath toUnified(Path p) {
      if (!(p instanceof SulfurUnifiedPath)) {
         throw new IllegalArgumentException("Not an SulfurUnifiedPath: " + String.valueOf(p));
      } else {
         return (SulfurUnifiedPath)p;
      }
   }
}
