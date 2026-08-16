package org.spongepowered.include.com.google.common.io;

import java.io.IOException;
import java.io.Writer;
import org.spongepowered.include.com.google.common.base.Preconditions;

public abstract class CharSink {
   protected CharSink() {
   }

   public abstract Writer openStream() throws IOException;

   public void write(CharSequence charSequence) throws IOException {
      Preconditions.checkNotNull(charSequence);
      Closer closer = Closer.create();

      try {
         Writer out = (Writer)closer.register(this.openStream());
         out.append(charSequence);
         out.flush();
      } catch (Throwable e) {
         throw closer.rethrow(e);
      } finally {
         closer.close();
      }

   }
}
