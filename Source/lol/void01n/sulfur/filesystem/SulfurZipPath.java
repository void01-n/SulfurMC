package lol.void01n.sulfur.filesystem;

import java.io.File;
import java.net.URI;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Iterator;

public final class SulfurZipPath implements Path {
   private final SulfurZipFileSystem fs;
   private final Path backing;

   SulfurZipPath(SulfurZipFileSystem fs, Path backing) {
      this.fs = fs;
      this.backing = backing;
   }

   Path backingPath() {
      return this.backing;
   }

   public SulfurZipFileSystem getFileSystem() {
      return this.fs;
   }

   public boolean isAbsolute() {
      return this.backing.isAbsolute();
   }

   public Path getRoot() {
      Path r = this.backing.getRoot();
      return r == null ? null : new SulfurZipPath(this.fs, r);
   }

   public Path getFileName() {
      Path f = this.backing.getFileName();
      return f == null ? null : new SulfurZipPath(this.fs, f);
   }

   public Path getParent() {
      Path p = this.backing.getParent();
      return p == null ? null : new SulfurZipPath(this.fs, p);
   }

   public int getNameCount() {
      return this.backing.getNameCount();
   }

   public Path getName(int index) {
      return new SulfurZipPath(this.fs, this.backing.getName(index));
   }

   public Path subpath(int beginIndex, int endIndex) {
      return new SulfurZipPath(this.fs, this.backing.subpath(beginIndex, endIndex));
   }

   public boolean startsWith(Path other) {
      return other instanceof SulfurZipPath ? this.backing.startsWith(((SulfurZipPath)other).backing) : this.backing.startsWith(other.toString());
   }

   public boolean endsWith(Path other) {
      return other instanceof SulfurZipPath ? this.backing.endsWith(((SulfurZipPath)other).backing) : this.backing.endsWith(other.toString());
   }

   public Path normalize() {
      return new SulfurZipPath(this.fs, this.backing.normalize());
   }

   public Path resolve(Path other) {
      return other instanceof SulfurZipPath ? new SulfurZipPath(this.fs, this.backing.resolve(((SulfurZipPath)other).backing)) : new SulfurZipPath(this.fs, this.backing.resolve(other.toString()));
   }

   public Path resolve(String other) {
      return new SulfurZipPath(this.fs, this.backing.resolve(other));
   }

   public Path relativize(Path other) {
      if (other instanceof SulfurZipPath) {
         return new SulfurZipPath(this.fs, this.backing.relativize(((SulfurZipPath)other).backing));
      } else {
         throw new IllegalArgumentException("Cannot relativize against a non-SulfurZipPath");
      }
   }

   public URI toUri() {
      String var10000 = this.fs.name();
      return URI.create("quilt.zfs://" + var10000 + this.backing.toString());
   }

   public Path toAbsolutePath() {
      return new SulfurZipPath(this.fs, this.backing.toAbsolutePath());
   }

   public Path toRealPath(LinkOption... options) {
      return this.toAbsolutePath().normalize();
   }

   public File toFile() {
      throw new UnsupportedOperationException("SulfurZipPath cannot be converted to a File");
   }

   public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) {
      throw new UnsupportedOperationException("SulfurZipFileSystem does not support WatchService");
   }

   public int compareTo(Path other) {
      return other instanceof SulfurZipPath ? this.backing.compareTo(((SulfurZipPath)other).backing) : this.toString().compareTo(other.toString());
   }

   public Iterator<Path> iterator() {
      return new Iterator<Path>() {
         int i = 0;

         public boolean hasNext() {
            return this.i < SulfurZipPath.this.getNameCount();
         }

         public Path next() {
            return SulfurZipPath.this.getName(this.i++);
         }
      };
   }

   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else if (!(obj instanceof SulfurZipPath)) {
         return false;
      } else {
         SulfurZipPath o = (SulfurZipPath)obj;
         return this.fs == o.fs && this.backing.equals(o.backing);
      }
   }

   public int hashCode() {
      return this.backing.hashCode() ^ System.identityHashCode(this.fs);
   }

   public String toString() {
      return this.backing.toString();
   }
}
