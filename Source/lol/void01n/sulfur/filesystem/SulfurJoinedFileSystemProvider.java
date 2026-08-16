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

public final class SulfurJoinedFileSystemProvider extends FileSystemProvider {
   public static final String SCHEME = "quilt.jfs";
   private static SulfurJoinedFileSystemProvider INSTANCE;
   static final SulfurFileSystemRegistry<SulfurJoinedFileSystem> REGISTRY = new SulfurFileSystemRegistry<SulfurJoinedFileSystem>("quilt.jfs");

   public SulfurJoinedFileSystemProvider() {
      if (INSTANCE == null) {
         INSTANCE = this;
      }

   }

   public static SulfurJoinedFileSystemProvider instance() {
      if (INSTANCE != null) {
         return INSTANCE;
      } else {
         SulfurJoinedFileSystemProvider found = (SulfurJoinedFileSystemProvider)SulfurFileSystemRegistry.findInstalledProvider(SulfurJoinedFileSystemProvider.class);
         if (found != null) {
            return found;
         } else {
            throw new IllegalStateException("SulfurJoinedFileSystemProvider not found via installed providers");
         }
      }
   }

   public String getScheme() {
      return "quilt.jfs";
   }

   public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
      String name = uri.getAuthority();
      if (name != null && !name.isBlank()) {
         Object backing = env.get("backingPaths");
         if (!(backing instanceof List)) {
            throw new IllegalArgumentException("env must contain 'backingPaths' -> List<Path>");
         } else {
            List<Path> paths = (List)backing;
            SulfurJoinedFileSystem fs = new SulfurJoinedFileSystem(name, paths, this);
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
      SulfurJoinedFileSystem fs = (SulfurJoinedFileSystem)REGISTRY.get(uri);
      return fs.getPath(uri.getPath());
   }

   public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
      SulfurJoinedPath jp = toJoined(path);

      for(Path backing : jp.fs.backingPaths) {
         Path candidate = backing.resolve(jp.relativePath);
         if (Files.exists(candidate, new LinkOption[0])) {
            return Files.newByteChannel(candidate, options, attrs);
         }
      }

      throw new NoSuchFileException(path.toString());
   }

   public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
      SulfurJoinedPath jdir = toJoined(dir);
      Set<String> seen = new LinkedHashSet();
      final List<Path> results = new ArrayList();

      for(Path backing : jdir.fs.backingPaths) {
         Path candidate = backing.resolve(jdir.relativePath);
         if (Files.isDirectory(candidate, new LinkOption[0])) {
            DirectoryStream<Path> stream = Files.newDirectoryStream(candidate);

            try {
               for(Path entry : stream) {
                  String fname = entry.getFileName().toString();
                  if (seen.add(fname)) {
                     SulfurJoinedPath child = new SulfurJoinedPath(jdir.fs, jdir.relativePath.isEmpty() ? fname : jdir.relativePath + "/" + fname);
                     if (filter.accept(child)) {
                        results.add(child);
                     }
                  }
               }
            } catch (Throwable var15) {
               if (stream != null) {
                  try {
                     stream.close();
                  } catch (Throwable var14) {
                     var15.addSuppressed(var14);
                  }
               }

               throw var15;
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
      throw new IOException("SulfurJoinedFileSystem is read-only");
   }

   public void delete(Path path) throws IOException {
      throw new IOException("SulfurJoinedFileSystem is read-only");
   }

   public void copy(Path source, Path target, CopyOption... options) throws IOException {
      SulfurJoinedPath js = toJoined(source);

      for(Path backing : js.fs.backingPaths) {
         Path candidate = backing.resolve(js.relativePath);
         if (Files.exists(candidate, new LinkOption[0])) {
            Files.copy(candidate, target, options);
            return;
         }
      }

      throw new NoSuchFileException(source.toString());
   }

   public void move(Path source, Path target, CopyOption... options) throws IOException {
      throw new IOException("SulfurJoinedFileSystem is read-only");
   }

   public boolean isSameFile(Path path, Path path2) {
      return path.toAbsolutePath().normalize().equals(path2.toAbsolutePath().normalize());
   }

   public boolean isHidden(Path path) {
      String name = path.getFileName() == null ? "" : path.getFileName().toString();
      return name.startsWith(".");
   }

   public FileStore getFileStore(Path path) throws IOException {
      SulfurJoinedPath jp = toJoined(path);

      for(Path backing : jp.fs.backingPaths) {
         Path candidate = backing.resolve(jp.relativePath);
         if (Files.exists(candidate, new LinkOption[0])) {
            return Files.getFileStore(candidate);
         }
      }

      throw new NoSuchFileException(path.toString());
   }

   public void checkAccess(Path path, AccessMode... modes) throws IOException {
      for(AccessMode m : modes) {
         if (m == AccessMode.WRITE) {
            throw new IOException("SulfurJoinedFileSystem is read-only");
         }
      }

      SulfurJoinedPath jp = toJoined(path);

      for(Path backing : jp.fs.backingPaths) {
         Path candidate = backing.resolve(jp.relativePath);
         if (Files.exists(candidate, new LinkOption[0])) {
            return;
         }
      }

      throw new NoSuchFileException(path.toString());
   }

   public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... opts) {
      SulfurJoinedPath jp = toJoined(path);

      for(Path backing : jp.fs.backingPaths) {
         Path candidate = backing.resolve(jp.relativePath);
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
      SulfurJoinedPath jp = toJoined(path);

      for(Path backing : jp.fs.backingPaths) {
         Path candidate = backing.resolve(jp.relativePath);

         try {
            return (A)Files.readAttributes(candidate, type, opts);
         } catch (NoSuchFileException var9) {
         }
      }

      throw new NoSuchFileException(path.toString());
   }

   public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... opts) throws IOException {
      SulfurJoinedPath jp = toJoined(path);

      for(Path backing : jp.fs.backingPaths) {
         Path candidate = backing.resolve(jp.relativePath);

         try {
            return Files.readAttributes(candidate, attributes, opts);
         } catch (NoSuchFileException var9) {
         }
      }

      throw new NoSuchFileException(path.toString());
   }

   public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
      throw new IOException("SulfurJoinedFileSystem is read-only");
   }

   private static SulfurJoinedPath toJoined(Path p) {
      if (!(p instanceof SulfurJoinedPath)) {
         throw new IllegalArgumentException("Not an SulfurJoinedPath: " + String.valueOf(p));
      } else {
         return (SulfurJoinedPath)p;
      }
   }
}
