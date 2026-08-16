package org.spongepowered.include.com.google.common.io;

import java.io.IOException;
import java.io.Reader;
import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.errorprone.annotations.CanIgnoreReturnValue;

public abstract class CharSource {
   protected CharSource() {
   }

   public abstract Reader openStream() throws IOException;

   @CanIgnoreReturnValue
   public <T> T readLines(LineProcessor<T> processor) throws IOException {
      Preconditions.checkNotNull(processor);
      Closer closer = Closer.create();

      Object var4;
      try {
         Reader reader = (Reader)closer.register(this.openStream());
         var4 = CharStreams.readLines(reader, processor);
      } catch (Throwable e) {
         throw closer.rethrow(e);
      } finally {
         closer.close();
      }

      return (T)var4;
   }
}
