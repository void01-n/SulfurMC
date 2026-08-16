package org.spongepowered.tools.obfuscation.interfaces;

import javax.annotation.processing.ProcessingEnvironment;
import org.spongepowered.asm.util.ITokenProvider;

public interface IMixinAnnotationProcessor extends IMessagerSuppressible, IOptionProvider {
   CompilerEnvironment getCompilerEnvironment();

   ProcessingEnvironment getProcessingEnvironment();

   IObfuscationManager getObfuscationManager();

   ITokenProvider getTokenProvider();

   ITypeHandleProvider getTypeProvider();

   IJavadocProvider getJavadocProvider();

   public static enum CompilerEnvironment {
      JAVAC(false, "Java Compiler"),
      JDT(true, "Eclipse (JDT)") {
         protected boolean isDetected(ProcessingEnvironment processingEnv) {
            return processingEnv.getClass().getName().contains("jdt");
         }
      },
      IDEA(true, "IntelliJ IDEA") {
         protected boolean isDetected(ProcessingEnvironment processingEnv) {
            for(String ideaSystemProperty : new String[]{"idea.plugins.path", "idea.config.path", "idea.home.path", "idea.paths.selector"}) {
               if (System.getProperty(ideaSystemProperty) != null) {
                  return true;
               }
            }

            return false;
         }
      };

      private final boolean isDevelopmentEnvironment;
      private final String friendlyName;

      private CompilerEnvironment(boolean isDevelopmentEnvironment, String friendlyName) {
         this.isDevelopmentEnvironment = isDevelopmentEnvironment;
         this.friendlyName = friendlyName;
      }

      public boolean isCompiler() {
         return !this.isDevelopmentEnvironment;
      }

      public boolean isDevelopmentEnvironment() {
         return this.isDevelopmentEnvironment;
      }

      public String getFriendlyName() {
         return this.friendlyName;
      }

      protected boolean isDetected(ProcessingEnvironment processingEnv) {
         return false;
      }

      public static CompilerEnvironment detect(ProcessingEnvironment processingEnv) {
         for(CompilerEnvironment environment : values()) {
            if (environment.isDetected(processingEnv)) {
               return environment;
            }
         }

         return JAVAC;
      }

      // $FF: synthetic method
      private static CompilerEnvironment[] $values() {
         return new CompilerEnvironment[]{JAVAC, JDT, IDEA};
      }
   }
}
