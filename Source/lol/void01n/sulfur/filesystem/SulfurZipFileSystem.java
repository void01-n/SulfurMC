package lol.void01n.sulfur.filesystem;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.List;

public final class SulfurZipFileSystem extends SulfurBaseFileSystem<SulfurZipFileSystem, SulfurZipFileSystemProvider> {
   private final String name;
   private final FileSystem backing;
   private final SulfurZipFileSystemProvider provider;
   private volatile boolean open = true;

   SulfurZipFileSystem(String name, FileSystem backing, SulfurZipFileSystemProvider provider) {
      this.name = name;
      this.backing = backing;
      this.provider = provider;
   }

   public String name() {
      return this.name;
   }

   SulfurZipPath getRoot() {
      return new SulfurZipPath(this, (Path)this.backing.getRootDirectories().iterator().next());
   }

   Path resolve(String relativePath) {
      Path root = (Path)this.backing.getRootDirectories().iterator().next();
      return relativePath.isEmpty() ? root : root.resolve(relativePath);
   }

   public FileSystemProvider provider() {
      return this.provider;
   }

   public void close() throws IOException {
      if (this.open) {
         this.open = false;
         SulfurZipFileSystemProvider.REGISTRY.close(this);
         this.backing.close();
      }

   }

   public boolean isOpen() {
      return this.open;
   }

   public Iterable<Path> getRootDirectories() {
      return List.of(this.getRoot());
   }

   public Iterable<FileStore> getFileStores() {
      return this.backing.getFileStores();
   }

   public Path getPath(String first, String... more) {
      if (more.length != 0) {
         first + "/" + String.join("/", more);
      }

      return new SulfurZipPath(this, this.backing.getPath(first, more));
   }
}
