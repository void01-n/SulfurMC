package lol.void01n.sulfur.filesystem;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.List;

public final class SulfurUnifiedFileSystem extends SulfurBaseFileSystem<SulfurUnifiedFileSystem, SulfurUnifiedFileSystemProvider> {
   final String name;
   final List<Path> backingPaths;
   private final SulfurUnifiedFileSystemProvider provider;
   private volatile boolean open = true;

   SulfurUnifiedFileSystem(String name, List<Path> backingPaths, SulfurUnifiedFileSystemProvider provider) {
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
         SulfurUnifiedFileSystemProvider.REGISTRY.close(this);
      }

   }

   public Iterable<Path> getRootDirectories() {
      return List.of(new SulfurUnifiedPath(this, ""));
   }

   public Iterable<FileStore> getFileStores() {
      List<FileStore> stores = new ArrayList();

      for(Path backing : this.backingPaths) {
         try {
            stores.add(Files.getFileStore(backing));
         } catch (IOException var5) {
         }
      }

      return stores;
   }

   public Path getPath(String first, String... more) {
      String full = more.length == 0 ? first : first + "/" + String.join("/", more);
      return new SulfurUnifiedPath(this, full);
   }
}
