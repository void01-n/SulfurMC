package org.spongepowered.asm.logging;

public class LoggerAdapterDefault extends LoggerAdapterAbstract {
   public LoggerAdapterDefault(String name) {
      super(name);
   }

   public String getType() {
      return "Default Logger (No Logging)";
   }

   public void catching(Level level, Throwable t) {
   }

   public void log(Level level, String message, Object... params) {
   }

   public void log(Level level, String message, Throwable t) {
   }

   public <T extends Throwable> T throwing(T t) {
      return null;
   }
}
