package org.spongepowered.asm.logging;

public enum Level {
   FATAL,
   ERROR,
   WARN,
   INFO,
   DEBUG,
   TRACE;

   // $FF: synthetic method
   private static Level[] $values() {
      return new Level[]{FATAL, ERROR, WARN, INFO, DEBUG, TRACE};
   }
}
