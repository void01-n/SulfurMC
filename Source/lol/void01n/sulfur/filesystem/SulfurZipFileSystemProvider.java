package lol.void01n.sulfur.filesystem;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class SulfurZipFileSystemProvider extends FileSystemProvider {
   public static final String SCHEME = "quilt.zfs";
   private static SulfurZipFileSystemProvider INSTANCE;
   static final SulfurFileSystemRegistry<SulfurZipFileSystem> REGISTRY = new SulfurFileSystemRegistry<SulfurZipFileSystem>("quilt.zfs");

   public SulfurZipFileSystemProvider() {
      if (INSTANCE == null) {
         INSTANCE = this;
      }

   }

   public static SulfurZipFileSystemProvider instance() {
      if (INSTANCE != null) {
         return INSTANCE;
      } else {
         SulfurZipFileSystemProvider found = (SulfurZipFileSystemProvider)SulfurFileSystemRegistry.findInstalledProvider(SulfurZipFileSystemProvider.class);
         if (found != null) {
            return found;
         } else {
            throw new IllegalStateException("SulfurZipFileSystemProvider not found via installed providers");
         }
      }
   }

   public String getScheme() {
      return "quilt.zfs";
   }

   public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
      if (!"quilt.zfs".equals(uri.getScheme())) {
         throw new IllegalArgumentException("Expected scheme 'quilt.zfs', got: " + uri.getScheme());
      } else {
         Object sourceObj = env.get("source");
         if (!(sourceObj instanceof Path)) {
            throw new IllegalArgumentException("env must contain 'source' -> Path for scheme quilt.zfs");
         } else {
            Path sourcePath = (Path)sourceObj;
            String name = uri.getAuthority();
            if (name != null && !name.isBlank()) {
               FileSystem backingZip = FileSystems.newFileSystem(sourcePath, Map.of("enablePosixFileAttributes", "false"));
               SulfurZipFileSystem fs = new SulfurZipFileSystem(name, backingZip, this);
               REGISTRY.register(fs);
               return fs;
            } else {
               throw new IllegalArgumentException("URI must have an authority (filesystem name): " + String.valueOf(uri));
            }
         }
      }
   }

   public FileSystem getFileSystem(URI uri) {
      return REGISTRY.get(uri);
   }

   public Path getPath(URI uri) {
      SulfurZipFileSystem fs = (SulfurZipFileSystem)REGISTRY.get(uri);
      return fs.getRoot().resolve(uri.getPath());
   }

   public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
      return this.toBackingPath(path).getFileSystem().provider().newByteChannel(this.toBackingPath(path), options, attrs);
   }

   public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
      Path backing = this.toBackingPath(dir);
      final DirectoryStream<Path> inner = Files.newDirectoryStream(backing, (p) -> filter.accept(this.toZfsPath((SulfurZipPath)dir, p)));
      final SulfurZipPath zfsDir = (SulfurZipPath)dir;
      return new DirectoryStream<Path>() {
         public Iterator<Path> iterator() {
            return new Iterator<Path>() {
               final Iterator<Path> it;
               // $FF: synthetic field
               final <undefinedtype> this$1;

               {
                  this.this$1 = this$1;
                  this.it = this.this$1.val$inner.iterator();
               }

               public boolean hasNext() {
                  return this.it.hasNext();
               }

               public Path next() {
                  return this.this$1.this$0.toZfsPath(this.this$1.val$zfsDir, (Path)this.it.next());
               }
            };
         }

         public void close() throws IOException {
            inner.close();
         }
      };
   }

   public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
      throw new IOException("SulfurZipFileSystem is read-only");
   }

   public void delete(Path path) throws IOException {
      throw new IOException("SulfurZipFileSystem is read-only");
   }

   public void copy(Path source, Path target, CopyOption... options) throws IOException {
      Files.copy(this.toBackingPath(source), target, options);
   }

   public void move(Path source, Path target, CopyOption... options) throws IOException {
      throw new IOException("SulfurZipFileSystem is read-only");
   }

   public boolean isSameFile(Path path, Path path2) throws IOException {
      return path.toAbsolutePath().normalize().equals(path2.toAbsolutePath().normalize());
   }

   public boolean isHidden(Path path) {
      String name = path.getFileName() == null ? "" : path.getFileName().toString();
      return name.startsWith(".");
   }

   public FileStore getFileStore(Path path) throws IOException {
      return Files.getFileStore(this.toBackingPath(path));
   }

   public void checkAccess(Path path, AccessMode... modes) throws IOException {
      for(AccessMode m : modes) {
         if (m == AccessMode.WRITE) {
            throw new IOException("SulfurZipFileSystem is read-only");
         }
      }

      Files.readAttributes(this.toBackingPath(path), BasicFileAttributes.class);
   }

   public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
      return (V)Files.getFileAttributeView(this.toBackingPath(path), type, options);
   }

   public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
      return (A)Files.readAttributes(this.toBackingPath(path), type, options);
   }

   public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
      return Files.readAttributes(this.toBackingPath(path), attributes, options);
   }

   public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
      throw new IOException("SulfurZipFileSystem is read-only");
   }

   private Path toBackingPath(Path p) {
      if (!(p instanceof SulfurZipPath)) {
         throw new IllegalArgumentException("Not an SulfurZipPath: " + String.valueOf(p));
      } else {
         return ((SulfurZipPath)p).backingPath();
      }
   }

   private Path toZfsPath(SulfurZipPath parent, Path backingChild) {
      Path relative = parent.backingPath().relativize(backingChild);
      return parent.getFileSystem().getRoot().resolve(relative.toString());
   }
}
