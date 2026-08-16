package org.spongepowered.asm.logging;

import java.util.logging.LogManager;
import java.util.logging.Logger;

public class LoggerAdapterJava extends LoggerAdapterAbstract {
   private static final java.util.logging.Level[] LEVELS;
   private final Logger logger;

   public LoggerAdapterJava(String name) {
      super(name);
      this.logger = getLogger(name);
   }

   public String getType() {
      return "java.util.logging Log Adapter";
   }

   public void catching(Level level, Throwable t) {
      this.warn("Catching {}: {}", t.getClass().getName(), t.getMessage(), t);
   }

   public void debug(String message, Object... params) {
      LoggerAdapterAbstract.FormattedMessage formatted = new LoggerAdapterAbstract.FormattedMessage(message, params);
      this.logger.fine(formatted.getMessage());
      if (formatted.hasThrowable()) {
         this.logger.fine(formatted.getThrowable().toString());
      }

   }

   public void debug(String message, Throwable t) {
      this.logger.fine(message);
      this.logger.fine(t.toString());
   }

   public void error(String message, Object... params) {
      LoggerAdapterAbstract.FormattedMessage formatted = new LoggerAdapterAbstract.FormattedMessage(message, params);
      this.logger.severe(formatted.getMessage());
      if (formatted.hasThrowable()) {
         this.logger.severe(formatted.getThrowable().toString());
      }

   }

   public void error(String message, Throwable t) {
      this.logger.severe(message);
      this.logger.severe(t.toString());
   }

   public void fatal(String message, Object... params) {
      LoggerAdapterAbstract.FormattedMessage formatted = new LoggerAdapterAbstract.FormattedMessage(message, params);
      this.logger.severe(formatted.getMessage());
      if (formatted.hasThrowable()) {
         this.logger.severe(formatted.getThrowable().toString());
      }

   }

   public void fatal(String message, Throwable t) {
      this.logger.severe(message);
      this.logger.severe(t.toString());
   }

   public void info(String message, Object... params) {
      LoggerAdapterAbstract.FormattedMessage formatted = new LoggerAdapterAbstract.FormattedMessage(message, params);
      this.logger.info(formatted.getMessage());
      if (formatted.hasThrowable()) {
         this.logger.info(formatted.getThrowable().toString());
      }

   }

   public void info(String message, Throwable t) {
      this.logger.info(message);
      this.logger.info(t.toString());
   }

   public void log(Level level, String message, Object... params) {
      java.util.logging.Level logLevel = LEVELS[level.ordinal()];
      LoggerAdapterAbstract.FormattedMessage formatted = new LoggerAdapterAbstract.FormattedMessage(message, params);
      this.logger.log(logLevel, formatted.getMessage());
      if (formatted.hasThrowable()) {
         this.logger.log(LEVELS[level.ordinal()], formatted.getThrowable().toString());
      }

   }

   public void log(Level level, String message, Throwable t) {
      java.util.logging.Level logLevel = LEVELS[level.ordinal()];
      this.logger.log(logLevel, message);
      this.logger.log(logLevel, t.toString());
   }

   public <T extends Throwable> T throwing(T t) {
      this.warn("Throwing {}: {}", t.getClass().getName(), t.getMessage(), t);
      return t;
   }

   public void trace(String message, Object... params) {
      LoggerAdapterAbstract.FormattedMessage formatted = new LoggerAdapterAbstract.FormattedMessage(message, params);
      this.logger.finer(formatted.getMessage());
      if (formatted.hasThrowable()) {
         this.logger.finer(formatted.getThrowable().toString());
      }

   }

   public void trace(String message, Throwable t) {
      this.logger.finer(message);
      this.logger.finer(t.toString());
   }

   public void warn(String message, Object... params) {
      LoggerAdapterAbstract.FormattedMessage formatted = new LoggerAdapterAbstract.FormattedMessage(message, params);
      this.logger.warning(formatted.getMessage());
      if (formatted.hasThrowable()) {
         this.logger.warning(formatted.getThrowable().toString());
      }

   }

   public void warn(String message, Throwable t) {
      this.logger.warning(message);
      this.logger.warning(t.toString());
   }

   private static Logger getLogger(String name) {
      LogManager logManager = LogManager.getLogManager();
      Logger logger = logManager.getLogger(name);
      return logger != null ? logger : LogManager.getLogManager().getLogger("global");
   }

   static {
      LEVELS = new java.util.logging.Level[]{java.util.logging.Level.SEVERE, java.util.logging.Level.SEVERE, java.util.logging.Level.WARNING, java.util.logging.Level.INFO, java.util.logging.Level.FINE, java.util.logging.Level.FINER};
   }
}
