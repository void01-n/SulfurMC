package org.spongepowered.asm.service.modlauncher;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.logging.LoggerAdapterAbstract;

public class LoggerAdapterLog4j2 extends LoggerAdapterAbstract {
   private static final Level[] LEVELS;
   private final Logger logger;

   public LoggerAdapterLog4j2(String name) {
      super(name);
      this.logger = LogManager.getLogger(name);
   }

   public String getType() {
      return "Log4j2 (via ModLauncher)";
   }

   public void catching(org.spongepowered.asm.logging.Level level, Throwable t) {
      this.logger.catching(LEVELS[level.ordinal()], t);
   }

   public void catching(Throwable t) {
      this.logger.catching(t);
   }

   public void debug(String message, Object... params) {
      this.logger.debug(message, params);
   }

   public void debug(String message, Throwable t) {
      this.logger.debug(message, t);
   }

   public void error(String message, Object... params) {
      this.logger.error(message, params);
   }

   public void error(String message, Throwable t) {
      this.logger.error(message, t);
   }

   public void fatal(String message, Object... params) {
      this.logger.fatal(message, params);
   }

   public void fatal(String message, Throwable t) {
      this.logger.fatal(message, t);
   }

   public void info(String message, Object... params) {
      this.logger.info(message, params);
   }

   public void info(String message, Throwable t) {
      this.logger.info(message, t);
   }

   public void log(org.spongepowered.asm.logging.Level level, String message, Object... params) {
      this.logger.log(LEVELS[level.ordinal()], message, params);
   }

   public void log(org.spongepowered.asm.logging.Level level, String message, Throwable t) {
      this.logger.log(LEVELS[level.ordinal()], message, t);
   }

   public <T extends Throwable> T throwing(T t) {
      return (T)this.logger.throwing(t);
   }

   public void trace(String message, Object... params) {
      this.logger.trace(message, params);
   }

   public void trace(String message, Throwable t) {
      this.logger.trace(message, t);
   }

   public void warn(String message, Object... params) {
      this.logger.warn(message, params);
   }

   public void warn(String message, Throwable t) {
      this.logger.warn(message, t);
   }

   static {
      LEVELS = new Level[]{Level.FATAL, Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG, Level.TRACE};
   }
}
