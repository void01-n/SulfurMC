package lol.void01n.sulfur.filesystem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SulfurMemoryFileSystemProvider extends FileSystemProvider {
   public static final String SCHEME = "quilt.mfs";
   private static SulfurMemoryFileSystemProvider INSTANCE;
   static final SulfurFileSystemRegistry<SulfurMemoryFileSystem> REGISTRY = new SulfurFileSystemRegistry<SulfurMemoryFileSystem>("quilt.mfs");

   public SulfurMemoryFileSystemProvider() {
      if (INSTANCE == null) {
         INSTANCE = this;
      }

   }

   public static SulfurMemoryFileSystemProvider instance() {
      if (INSTANCE != null) {
         return INSTANCE;
      } else {
         SulfurMemoryFileSystemProvider found = (SulfurMemoryFileSystemProvider)SulfurFileSystemRegistry.findInstalledProvider(SulfurMemoryFileSystemProvider.class);
         if (found != null) {
            return found;
         } else {
            throw new IllegalStateException("SulfurMemoryFileSystemProvider not found via installed providers");
         }
      }
   }

   public String getScheme() {
      return "quilt.mfs";
   }

   public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
      String name = uri.getAuthority();
      if (name != null && !name.isBlank()) {
         SulfurMemoryFileSystem fs = new SulfurMemoryFileSystem(name, this);
         REGISTRY.register(fs);
         return fs;
      } else {
         throw new IllegalArgumentException("URI must have an authority (filesystem name): " + String.valueOf(uri));
      }
   }

   public FileSystem getFileSystem(URI uri) {
      return REGISTRY.get(uri);
   }

   public Path getPath(URI uri) {
      SulfurMemoryFileSystem fs = (SulfurMemoryFileSystem)REGISTRY.get(uri);
      return fs.getPath(uri.getPath());
   }

   public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
      SulfurMemoryPath mp = toMemPath(path);
      boolean write = options.contains(StandardOpenOption.WRITE) || options.contains(StandardOpenOption.CREATE) || options.contains(StandardOpenOption.CREATE_NEW) || options.contains(StandardOpenOption.APPEND);
      if (write) {
         return mp.fs.writeChannel(mp.pathString);
      } else {
         byte[] data = mp.fs.read(mp.pathString);
         if (data == null) {
            throw new NoSuchFileException(path.toString());
         } else {
            return readOnlyChannel(data);
         }
      }
   }

   public InputStream newInputStream(Path path, OpenOption... options) throws IOException {
      SulfurMemoryPath mp = toMemPath(path);
      byte[] data = mp.fs.read(mp.pathString);
      if (data == null) {
         throw new NoSuchFileException(path.toString());
      } else {
         return new ByteArrayInputStream(data);
      }
   }

   public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
      SulfurMemoryPath mdir = toMemPath(dir);
      final List<Path> entries = mdir.fs.listDirectory(mdir.pathString, filter);
      return new DirectoryStream<Path>() {
         public Iterator<Path> iterator() {
            return entries.iterator();
         }

         public void close() {
         }
      };
   }

   public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
      SulfurMemoryPath mdir = toMemPath(dir);
      mdir.fs.createDirectory(mdir.pathString);
   }

   public void delete(Path path) throws IOException {
      SulfurMemoryPath mp = toMemPath(path);
      mp.fs.delete(mp.pathString);
   }

   public void copy(Path source, Path target, CopyOption... options) throws IOException {
      SulfurMemoryPath ms = toMemPath(source);
      byte[] data = ms.fs.read(ms.pathString);
      if (data == null) {
         throw new NoSuchFileException(source.toString());
      } else {
         SulfurMemoryPath mt = toMemPath(target);
         mt.fs.write(mt.pathString, data);
      }
   }

   public void move(Path source, Path target, CopyOption... options) throws IOException {
      this.copy(source, target, options);
      this.delete(source);
   }

   public boolean isSameFile(Path path, Path path2) {
      return path.toAbsolutePath().normalize().equals(path2.toAbsolutePath().normalize());
   }

   public boolean isHidden(Path path) {
      String name = path.getFileName() == null ? "" : path.getFileName().toString();
      return name.startsWith(".");
   }

   public FileStore getFileStore(Path path) {
      return toMemPath(path).fs.fileStore();
   }

   public void checkAccess(Path path, AccessMode... modes) throws IOException {
      SulfurMemoryPath mp = toMemPath(path);
      if (!mp.fs.exists(mp.pathString)) {
         throw new NoSuchFileException(path.toString());
      }
   }

   public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
      return null;
   }

   public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
      if (!type.isAssignableFrom(BasicFileAttributes.class)) {
         throw new UnsupportedOperationException("Attribute type not supported: " + String.valueOf(type));
      } else {
         SulfurMemoryPath mp = toMemPath(path);
         return (A)mp.fs.attributes(mp.pathString);
      }
   }

   public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
      BasicFileAttributes attrs = this.readAttributes(path, BasicFileAttributes.class, options);
      Map<String, Object> map = new LinkedHashMap();
      map.put("isDirectory", attrs.isDirectory());
      map.put("isRegularFile", attrs.isRegularFile());
      map.put("isSymbolicLink", false);
      map.put("isOther", false);
      map.put("size", attrs.size());
      map.put("fileKey", (Object)null);
      map.put("lastModifiedTime", attrs.lastModifiedTime());
      map.put("lastAccessTime", attrs.lastAccessTime());
      map.put("creationTime", attrs.creationTime());
      return map;
   }

   public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
   }

   private static SulfurMemoryPath toMemPath(Path p) {
      if (!(p instanceof SulfurMemoryPath)) {
         throw new IllegalArgumentException("Not an SulfurMemoryPath: " + String.valueOf(p));
      } else {
         return (SulfurMemoryPath)p;
      }
   }

   private static SeekableByteChannel readOnlyChannel(final byte[] data) {
      return new SeekableByteChannel() {
         private int pos = 0;
         private final ByteBuffer buf = ByteBuffer.wrap(data);

         public boolean isOpen() {
            return true;
         }

         public void close() {
         }

         public int read(ByteBuffer dst) {
            if (this.pos >= data.length) {
               return -1;
            } else {
               int n = Math.min(dst.remaining(), data.length - this.pos);
               dst.put(data, this.pos, n);
               this.pos += n;
               return n;
            }
         }

         public int write(ByteBuffer src) throws IOException {
            throw new IOException("Read-only channel");
         }

         public long position() {
            return (long)this.pos;
         }

         public SeekableByteChannel position(long p) {
            this.pos = (int)p;
            return this;
         }

         public long size() {
            return (long)data.length;
         }

         public SeekableByteChannel truncate(long s) throws IOException {
            throw new IOException("Read-only channel");
         }
      };
   }
}
