package lol.void01n.sulfur.filesystem;

import java.io.File;
import java.net.URI;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.Iterator;

public final class SulfurMemoryPath implements Path {
   final SulfurMemoryFileSystem fs;
   final String pathString;

   SulfurMemoryPath(SulfurMemoryFileSystem fs, String pathString) {
      this.fs = fs;
      this.pathString = pathString.isEmpty() ? "/" : pathString;
   }

   public SulfurMemoryFileSystem getFileSystem() {
      return this.fs;
   }

   public boolean isAbsolute() {
      return this.pathString.startsWith("/");
   }

   public Path getRoot() {
      return this.isAbsolute() ? new SulfurMemoryPath(this.fs, "/") : null;
   }

   public Path getFileName() {
      int idx = this.pathString.lastIndexOf(47);
      if (idx < 0) {
         return new SulfurMemoryPath(this.fs, this.pathString);
      } else {
         String name = this.pathString.substring(idx + 1);
         return name.isEmpty() ? null : new SulfurMemoryPath(this.fs, name);
      }
   }

   public Path getParent() {
      int idx = this.pathString.lastIndexOf(47);
      return idx <= 0 ? null : new SulfurMemoryPath(this.fs, this.pathString.substring(0, idx));
   }

   public int getNameCount() {
      String s = this.pathString.startsWith("/") ? this.pathString.substring(1) : this.pathString;
      return s.isEmpty() ? 0 : s.split("/").length;
   }

   public Path getName(int index) {
      String s = this.pathString.startsWith("/") ? this.pathString.substring(1) : this.pathString;
      return new SulfurMemoryPath(this.fs, s.split("/")[index]);
   }

   public Path subpath(int beginIndex, int endIndex) {
      String s = this.pathString.startsWith("/") ? this.pathString.substring(1) : this.pathString;
      String[] parts = s.split("/");
      return new SulfurMemoryPath(this.fs, String.join("/", (CharSequence[])Arrays.copyOfRange(parts, beginIndex, endIndex)));
   }

   public boolean startsWith(Path other) {
      return this.pathString.startsWith(other.toString());
   }

   public boolean endsWith(Path other) {
      return this.pathString.endsWith(other.toString());
   }

   public Path normalize() {
      return this;
   }

   public Path resolve(Path other) {
      if (other.isAbsolute()) {
         return new SulfurMemoryPath(this.fs, other.toString());
      } else {
         String base = this.pathString.endsWith("/") ? this.pathString : this.pathString + "/";
         return new SulfurMemoryPath(this.fs, base + String.valueOf(other));
      }
   }

   public Path resolve(String other) {
      return this.resolve((Path)(new SulfurMemoryPath(this.fs, other)));
   }

   public Path relativize(Path other) {
      String o = other.toString();
      if (!o.startsWith(this.pathString)) {
         throw new IllegalArgumentException("Cannot relativize " + o + " against " + this.pathString);
      } else {
         String rel = o.substring(this.pathString.length());
         return new SulfurMemoryPath(this.fs, rel.startsWith("/") ? rel.substring(1) : rel);
      }
   }

   public URI toUri() {
      String var10000 = this.fs.name();
      return URI.create("quilt.mfs://" + var10000 + this.pathString);
   }

   public Path toAbsolutePath() {
      return this.isAbsolute() ? this : new SulfurMemoryPath(this.fs, "/" + this.pathString);
   }

   public Path toRealPath(LinkOption... options) {
      return this.toAbsolutePath();
   }

   public File toFile() {
      throw new UnsupportedOperationException("SulfurMemoryPath cannot be converted to a File");
   }

   public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) {
      throw new UnsupportedOperationException("SulfurMemoryFileSystem does not support WatchService");
   }

   public int compareTo(Path other) {
      return this.pathString.compareTo(other.toString());
   }

   public Iterator<Path> iterator() {
      final int count = this.getNameCount();
      return new Iterator<Path>() {
         int i = 0;

         public boolean hasNext() {
            return this.i < count;
         }

         public Path next() {
            return SulfurMemoryPath.this.getName(this.i++);
         }
      };
   }

   public boolean equals(Object o) {
      if (!(o instanceof SulfurMemoryPath other)) {
         return false;
      } else {
         return this.fs == other.fs && this.pathString.equals(other.pathString);
      }
   }

   public int hashCode() {
      return this.pathString.hashCode();
   }

   public String toString() {
      return this.pathString;
   }
}
