package org.spongepowered.asm.logging;

public interface ILogger {
   String getId();

   String getType();

   void catching(Level var1, Throwable var2);

   void catching(Throwable var1);

   void debug(String var1, Object... var2);

   void debug(String var1, Throwable var2);

   void error(String var1, Object... var2);

   void error(String var1, Throwable var2);

   void fatal(String var1, Object... var2);

   void fatal(String var1, Throwable var2);

   void info(String var1, Object... var2);

   void info(String var1, Throwable var2);

   void log(Level var1, String var2, Object... var3);

   void log(Level var1, String var2, Throwable var3);

   <T extends Throwable> T throwing(T var1);

   void trace(String var1, Object... var2);

   void trace(String var1, Throwable var2);

   void warn(String var1, Object... var2);

   void warn(String var1, Throwable var2);
}
