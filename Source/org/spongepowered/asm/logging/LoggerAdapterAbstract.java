package org.spongepowered.asm.logging;

public abstract class LoggerAdapterAbstract implements ILogger {
   private final String id;

   protected LoggerAdapterAbstract(String id) {
      this.id = id;
   }

   public String getId() {
      return this.id;
   }

   public void catching(Throwable t) {
      this.catching(Level.WARN, t);
   }

   public void debug(String message, Object... params) {
      this.log(Level.DEBUG, message, params);
   }

   public void debug(String message, Throwable t) {
      this.log(Level.DEBUG, message, t);
   }

   public void error(String message, Object... params) {
      this.log(Level.ERROR, message, params);
   }

   public void error(String message, Throwable t) {
      this.log(Level.ERROR, message, t);
   }

   public void fatal(String message, Object... params) {
      this.log(Level.FATAL, message, params);
   }

   public void fatal(String message, Throwable t) {
      this.log(Level.FATAL, message, t);
   }

   public void info(String message, Object... params) {
      this.log(Level.INFO, message, params);
   }

   public void info(String message, Throwable t) {
      this.log(Level.INFO, message, t);
   }

   public void trace(String message, Object... params) {
      this.log(Level.TRACE, message, params);
   }

   public void trace(String message, Throwable t) {
      this.log(Level.TRACE, message, t);
   }

   public void warn(String message, Object... params) {
      this.log(Level.WARN, message, params);
   }

   public void warn(String message, Throwable t) {
      this.log(Level.WARN, message, t);
   }

   public static class FormattedMessage {
      private String message;
      private Throwable t;

      public FormattedMessage(String message, Object... params) {
         if (params.length == 0) {
            this.message = message;
         } else {
            StringBuilder sb = new StringBuilder();
            int pos = 0;

            int param;
            for(param = 0; pos < message.length() && param < params.length; ++param) {
               int delimPos = message.indexOf("{}", pos);
               if (delimPos < 0) {
                  break;
               }

               sb.append(message.substring(pos, delimPos)).append(params[param]);
               pos = delimPos + 2;
            }

            if (pos < message.length()) {
               sb.append(message.substring(pos));
            }

            if (param < params.length && params[params.length - 1] instanceof Throwable) {
               this.t = (Throwable)params[params.length - 1];
            }

            this.message = sb.toString();
         }
      }

      public String toString() {
         return this.message;
      }

      public String getMessage() {
         return this.message;
      }

      public boolean hasThrowable() {
         return this.t != null;
      }

      public Throwable getThrowable() {
         return this.t;
      }
   }
}
