package org.spongepowered.include.com.google.common.io;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.common.base.Throwables;
import org.spongepowered.include.com.google.errorprone.annotations.CanIgnoreReturnValue;

public final class Closer implements Closeable {
   private static final Suppressor SUPPRESSOR;
   final Suppressor suppressor;
   private final Deque<Closeable> stack = new ArrayDeque(4);
   private Throwable thrown;

   public static Closer create() {
      return new Closer(SUPPRESSOR);
   }

   Closer(Suppressor suppressor) {
      this.suppressor = (Suppressor)Preconditions.checkNotNull(suppressor);
   }

   @CanIgnoreReturnValue
   public <C extends Closeable> C register(@Nullable C closeable) {
      if (closeable != null) {
         this.stack.addFirst(closeable);
      }

      return closeable;
   }

   public RuntimeException rethrow(Throwable e) throws IOException {
      Preconditions.checkNotNull(e);
      this.thrown = e;
      Throwables.propagateIfPossible(e, IOException.class);
      throw new RuntimeException(e);
   }

   public void close() throws IOException {
      Throwable throwable = this.thrown;

      while(!this.stack.isEmpty()) {
         Closeable closeable = (Closeable)this.stack.removeFirst();

         try {
            closeable.close();
         } catch (Throwable e) {
            if (throwable == null) {
               throwable = e;
            } else {
               this.suppressor.suppress(closeable, throwable, e);
            }
         }
      }

      if (this.thrown == null && throwable != null) {
         Throwables.propagateIfPossible(throwable, IOException.class);
         throw new AssertionError(throwable);
      }
   }

   static {
      SUPPRESSOR = (Suppressor)(Closer.SuppressingSuppressor.isAvailable() ? Closer.SuppressingSuppressor.INSTANCE : Closer.LoggingSuppressor.INSTANCE);
   }

   static final class LoggingSuppressor implements Suppressor {
      static final LoggingSuppressor INSTANCE = new LoggingSuppressor();

      public void suppress(Closeable closeable, Throwable thrown, Throwable suppressed) {
         Closeables.logger.log(Level.WARNING, "Suppressing exception thrown when closing " + closeable, suppressed);
      }
   }

   static final class SuppressingSuppressor implements Suppressor {
      static final SuppressingSuppressor INSTANCE = new SuppressingSuppressor();
      static final Method addSuppressed = getAddSuppressed();

      static boolean isAvailable() {
         return addSuppressed != null;
      }

      private static Method getAddSuppressed() {
         try {
            return Throwable.class.getMethod("addSuppressed", Throwable.class);
         } catch (Throwable var1) {
            return null;
         }
      }

      public void suppress(Closeable closeable, Throwable thrown, Throwable suppressed) {
         if (thrown != suppressed) {
            try {
               addSuppressed.invoke(thrown, suppressed);
            } catch (Throwable var5) {
               Closer.LoggingSuppressor.INSTANCE.suppress(closeable, thrown, suppressed);
            }

         }
      }
   }

   interface Suppressor {
      void suppress(Closeable var1, Throwable var2, Throwable var3);
   }
}
