package org.spongepowered.include.com.google.common.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import org.spongepowered.include.com.google.common.base.Preconditions;

public abstract class ByteSource {
   protected ByteSource() {
   }

   public CharSource asCharSource(Charset charset) {
      return new AsCharSource(charset);
   }

   public abstract InputStream openStream() throws IOException;

   public InputStream openBufferedStream() throws IOException {
      InputStream in = this.openStream();
      return in instanceof BufferedInputStream ? (BufferedInputStream)in : new BufferedInputStream(in);
   }

   public byte[] read() throws IOException {
      Closer closer = Closer.create();

      byte[] var3;
      try {
         InputStream in = (InputStream)closer.register(this.openStream());
         var3 = ByteStreams.toByteArray(in);
      } catch (Throwable e) {
         throw closer.rethrow(e);
      } finally {
         closer.close();
      }

      return var3;
   }

   private final class AsCharSource extends CharSource {
      final Charset charset;

      AsCharSource(Charset charset) {
         this.charset = (Charset)Preconditions.checkNotNull(charset);
      }

      public Reader openStream() throws IOException {
         return new InputStreamReader(ByteSource.this.openStream(), this.charset);
      }

      public String toString() {
         return ByteSource.this.toString() + ".asCharSource(" + this.charset + ")";
      }
   }
}
