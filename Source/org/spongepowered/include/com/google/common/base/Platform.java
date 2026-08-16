package org.spongepowered.include.com.google.common.base;

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

final class Platform {
   private static final Logger logger = Logger.getLogger(Platform.class.getName());
   private static final PatternCompiler patternCompiler = loadPatternCompiler();

   private Platform() {
   }

   static boolean stringIsNullOrEmpty(@Nullable String string) {
      return string == null || string.isEmpty();
   }

   private static PatternCompiler loadPatternCompiler() {
      ServiceLoader<PatternCompiler> loader = ServiceLoader.load(PatternCompiler.class);

      try {
         Iterator<PatternCompiler> it = loader.iterator();

         while(it.hasNext()) {
            try {
               return (PatternCompiler)it.next();
            } catch (ServiceConfigurationError e) {
               logPatternCompilerError(e);
            }
         }
      } catch (ServiceConfigurationError e) {
         logPatternCompilerError(e);
      }

      return new JdkPatternCompiler();
   }

   private static void logPatternCompilerError(ServiceConfigurationError e) {
      logger.log(Level.WARNING, "Error loading regex compiler, falling back to next option", e);
   }

   private static final class JdkPatternCompiler implements PatternCompiler {
      private JdkPatternCompiler() {
      }
   }
}
