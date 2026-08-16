package lol.void01n.sulfur.filesystem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class SulfurMemoryFileSystem extends SulfurBaseFileSystem<SulfurMemoryFileSystem, SulfurMemoryFileSystemProvider> {
   private final String name;
   private final SulfurMemoryFileSystemProvider provider;
   private volatile boolean open = true;
   private static final byte[] DIRECTORY_MARKER = new byte[0];
   private final ConcurrentHashMap<String, byte[]> entries = new ConcurrentHashMap();

   SulfurMemoryFileSystem(String name, SulfurMemoryFileSystemProvider provider) {
      this.name = name;
      this.provider = provider;
      this.entries.put("/", DIRECTORY_MARKER);
   }

   public String name() {
      return this.name;
   }

   public FileSystemProvider provider() {
      return this.provider;
   }

   public boolean isReadOnly() {
      return false;
   }

   public boolean isOpen() {
      return this.open;
   }

   public void close() {
      if (this.open) {
         this.open = false;
         SulfurMemoryFileSystemProvider.REGISTRY.close(this);
      }

   }

   byte[] read(String absPath) {
      byte[] data = (byte[])this.entries.get(normalize(absPath));
      return data == DIRECTORY_MARKER ? null : data;
   }

   boolean exists(String absPath) {
      return this.entries.containsKey(normalize(absPath));
   }

   boolean isDirectory(String absPath) {
      byte[] d = (byte[])this.entries.get(normalize(absPath));
      return d == DIRECTORY_MARKER;
   }

   void write(String absPath, byte[] data) {
      String key = normalize(absPath);
      this.ensureParentDirectoriesExist(key);
      this.entries.put(key, data);
   }

   SeekableByteChannel writeChannel(final String absPath) {
      return new SeekableByteChannel() {
         private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
         private boolean channelOpen = true;

         public boolean isOpen() {
            return this.channelOpen;
         }

         public void close() {
            if (this.channelOpen) {
               this.channelOpen = false;
               SulfurMemoryFileSystem.this.write(absPath, this.baos.toByteArray());
            }

         }

         public int read(ByteBuffer dst) throws IOException {
            throw new IOException("Write-only");
         }

         public int write(ByteBuffer src) {
            byte[] b = new byte[src.remaining()];
            src.get(b);
            this.baos.write(b, 0, b.length);
            return b.length;
         }

         public long position() {
            return (long)this.baos.size();
         }

         public SeekableByteChannel position(long p) {
            return this;
         }

         public long size() {
            return (long)this.baos.size();
         }

         public SeekableByteChannel truncate(long s) {
            return this;
         }
      };
   }

   void createDirectory(String absPath) {
      String key = normalize(absPath);
      this.ensureParentDirectoriesExist(key);
      this.entries.putIfAbsent(key, DIRECTORY_MARKER);
   }

   void delete(String absPath) throws IOException {
      String key = normalize(absPath);
      byte[] removed = (byte[])this.entries.remove(key);
      if (removed == null) {
         throw new NoSuchFileException(absPath);
      }
   }

   BasicFileAttributes attributes(String absPath) throws NoSuchFileException {
      final String key = normalize(absPath);
      byte[] data = (byte[])this.entries.get(key);
      if (data == null) {
         throw new NoSuchFileException(absPath);
      } else {
         final boolean isDir = data == DIRECTORY_MARKER;
         final long size = isDir ? 0L : (long)data.length;
         final FileTime now = FileTime.fromMillis(System.currentTimeMillis());
         return new BasicFileAttributes() {
            public FileTime lastModifiedTime() {
               return now;
            }

            public FileTime lastAccessTime() {
               return now;
            }

            public FileTime creationTime() {
               return now;
            }

            public boolean isRegularFile() {
               return !isDir;
            }

            public boolean isDirectory() {
               return isDir;
            }

            public boolean isSymbolicLink() {
               return false;
            }

            public boolean isOther() {
               return false;
            }

            public long size() {
               return size;
            }

            public Object fileKey() {
               return key;
            }
         };
      }
   }

   List<Path> listDirectory(String absPath, DirectoryStream.Filter<? super Path> filter) throws IOException {
      String key = normalize(absPath);
      if (!this.isDirectory(key)) {
         throw new NotDirectoryException(absPath);
      } else {
         List<Path> result = new ArrayList();
         String prefix = key.equals("/") ? "/" : key + "/";

         for(String entry : this.entries.keySet()) {
            if (!entry.equals(key) && entry.startsWith(prefix)) {
               String rest = entry.substring(prefix.length());
               if (!rest.contains("/")) {
                  Path child = this.getPath(entry);
                  if (filter.accept(child)) {
                     result.add(child);
                  }
               }
            }
         }

         return result;
      }
   }

   FileStore fileStore() {
      return new FileStore() {
         public String name() {
            return "SulfurMemory:" + SulfurMemoryFileSystem.this.name;
         }

         public String type() {
            return "memory";
         }

         public boolean isReadOnly() {
            return false;
         }

         public long getTotalSpace() {
            return Long.MAX_VALUE;
         }

         public long getUsableSpace() {
            return Long.MAX_VALUE;
         }

         public long getUnallocatedSpace() {
            return Long.MAX_VALUE;
         }

         public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
            return type.isAssignableFrom(BasicFileAttributeView.class);
         }

         public boolean supportsFileAttributeView(String name) {
            return "basic".equals(name);
         }

         public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
            return null;
         }

         public Object getAttribute(String attribute) {
            return null;
         }
      };
   }

   public Iterable<Path> getRootDirectories() {
      return List.of(new SulfurMemoryPath(this, "/"));
   }

   public Iterable<FileStore> getFileStores() {
      return List.of(this.fileStore());
   }

   public Path getPath(String first, String... more) {
      String full = more.length == 0 ? first : first + "/" + String.join("/", more);
      return new SulfurMemoryPath(this, normalize(full));
   }

   private static String normalize(String path) {
      if (path != null && !path.isEmpty()) {
         if (!path.startsWith("/")) {
            path = "/" + path;
         }

         return path.replaceAll("/+", "/");
      } else {
         return "/";
      }
   }

   private void ensureParentDirectoriesExist(String key) {
      String parent = key.contains("/") ? key.substring(0, key.lastIndexOf(47)) : "/";
      if (!parent.isEmpty() && !this.entries.containsKey(parent)) {
         this.ensureParentDirectoriesExist(parent);
         this.entries.putIfAbsent(parent.isEmpty() ? "/" : parent, DIRECTORY_MARKER);
      }

   }
}
