package lol.void01n.sulfur.filesystem;

import java.io.File;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.Iterator;

public final class SulfurUnifiedPath implements Path {
   final SulfurUnifiedFileSystem fs;
   final String relativePath;

   SulfurUnifiedPath(SulfurUnifiedFileSystem fs, String relativePath) {
      this.fs = fs;
      this.relativePath = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
   }

   public FileSystem getFileSystem() {
      return this.fs;
   }

   public boolean isAbsolute() {
      return this.relativePath.isEmpty();
   }

   public Path getRoot() {
      return new SulfurUnifiedPath(this.fs, "");
   }

   public Path getFileName() {
      int idx = this.relativePath.lastIndexOf(47);
      if (idx < 0) {
         return this.relativePath.isEmpty() ? null : new SulfurUnifiedPath(this.fs, this.relativePath);
      } else {
         String name = this.relativePath.substring(idx + 1);
         return name.isEmpty() ? null : new SulfurUnifiedPath(this.fs, name);
      }
   }

   public Path getParent() {
      if (this.relativePath.isEmpty()) {
         return null;
      } else {
         int idx = this.relativePath.lastIndexOf(47);
         return idx < 0 ? new SulfurUnifiedPath(this.fs, "") : new SulfurUnifiedPath(this.fs, this.relativePath.substring(0, idx));
      }
   }

   public int getNameCount() {
      return this.relativePath.isEmpty() ? 0 : this.relativePath.split("/").length;
   }

   public Path getName(int index) {
      String[] parts = this.relativePath.split("/");
      if (index >= 0 && index < parts.length) {
         return new SulfurUnifiedPath(this.fs, parts[index]);
      } else {
         throw new IllegalArgumentException("Index out of range: " + index);
      }
   }

   public Path subpath(int beginIndex, int endIndex) {
      String[] parts = this.relativePath.split("/");
      return new SulfurUnifiedPath(this.fs, String.join("/", (CharSequence[])Arrays.copyOfRange(parts, beginIndex, endIndex)));
   }

   public boolean startsWith(Path other) {
      return this.toString().startsWith(other.toString());
   }

   public boolean endsWith(Path other) {
      return this.toString().endsWith(other.toString());
   }

   public Path normalize() {
      return this;
   }

   public Path resolve(Path other) {
      if (other.isAbsolute()) {
         return other;
      } else {
         String o = other.toString();
         if (this.relativePath.isEmpty()) {
            return new SulfurUnifiedPath(this.fs, o);
         } else {
            String var10003 = this.relativePath;
            return new SulfurUnifiedPath(this.fs, var10003 + (o.isEmpty() ? "" : "/" + o));
         }
      }
   }

   public Path resolve(String other) {
      return this.resolve((Path)(new SulfurUnifiedPath(this.fs, other)));
   }

   public Path relativize(Path other) {
      String s = this.toString();
      String o = other.toString();
      if (!o.startsWith(s)) {
         throw new IllegalArgumentException("Cannot relativize " + o + " against " + s);
      } else {
         String rel = o.substring(s.length());
         if (rel.startsWith("/")) {
            rel = rel.substring(1);
         }

         return new SulfurUnifiedPath(this.fs, rel);
      }
   }

   public URI toUri() {
      String var10000 = this.fs.name();
      return URI.create("quilt.ufs://" + var10000 + "/" + this.relativePath);
   }

   public Path toAbsolutePath() {
      return new SulfurUnifiedPath(this.fs, this.relativePath);
   }

   public Path toRealPath(LinkOption... options) {
      return this.toAbsolutePath();
   }

   public File toFile() {
      throw new UnsupportedOperationException("SulfurUnifiedPath cannot be converted to a File");
   }

   public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) {
      throw new UnsupportedOperationException("SulfurUnifiedFileSystem does not support WatchService");
   }

   public int compareTo(Path other) {
      return this.relativePath.compareTo(other instanceof SulfurUnifiedPath ? ((SulfurUnifiedPath)other).relativePath : other.toString());
   }

   public Iterator<Path> iterator() {
      final int count = this.getNameCount();
      return new Iterator<Path>() {
         int i = 0;

         public boolean hasNext() {
            return this.i < count;
         }

         public Path next() {
            return SulfurUnifiedPath.this.getName(this.i++);
         }
      };
   }

   public boolean equals(Object o) {
      if (!(o instanceof SulfurUnifiedPath other)) {
         return false;
      } else {
         return this.fs == other.fs && this.relativePath.equals(other.relativePath);
      }
   }

   public int hashCode() {
      return this.relativePath.hashCode() ^ System.identityHashCode(this.fs);
   }

   public String toString() {
      return this.relativePath;
   }
}
