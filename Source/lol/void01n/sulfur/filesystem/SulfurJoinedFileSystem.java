package lol.void01n.sulfur.filesystem;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.List;

public final class SulfurJoinedFileSystem extends SulfurBaseFileSystem<SulfurJoinedFileSystem, SulfurJoinedFileSystemProvider> {
   final String name;
   final List<Path> backingPaths;
   private final SulfurJoinedFileSystemProvider provider;
   private volatile boolean open = true;

   SulfurJoinedFileSystem(String name, List<Path> backingPaths, SulfurJoinedFileSystemProvider provider) {
      this.name = name;
      this.backingPaths = List.copyOf(backingPaths);
      this.provider = provider;
   }

   public String name() {
      return this.name;
   }

   public FileSystemProvider provider() {
      return this.provider;
   }

   public boolean isOpen() {
      return this.open;
   }

   public void close() throws IOException {
      if (this.open) {
         this.open = false;
         SulfurJoinedFileSystemProvider.REGISTRY.close(this);
      }

   }

   public Iterable<Path> getRootDirectories() {
      return List.of(new SulfurJoinedPath(this, ""));
   }

   public Iterable<FileStore> getFileStores() {
      return List.of();
   }

   public Path getPath(String first, String... more) {
      String full = more.length == 0 ? first : first + "/" + String.join("/", more);
      return new SulfurJoinedPath(this, full);
   }
}
