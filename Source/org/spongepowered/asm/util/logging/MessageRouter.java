package org.spongepowered.asm.util.logging;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.Level;
import org.spongepowered.asm.service.MixinService;

public final class MessageRouter {
   private static Messager messager;

   private MessageRouter() {
   }

   public static Messager getMessager() {
      if (messager == null) {
         messager = new LoggingMessager();
      }

      return messager;
   }

   public static void setMessager(Messager messager) {
      MessageRouter.messager = messager == null ? null : new DebugInterceptingMessager(messager);
   }

   static class LoggingMessager implements Messager {
      private static final ILogger logger = MixinService.getService().getLogger("mixin");

      public void printMessage(Diagnostic.Kind kind, CharSequence msg) {
         logger.log(messageKindToLoggingLevel(kind), msg.toString());
      }

      public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e) {
         logger.log(messageKindToLoggingLevel(kind), msg.toString());
      }

      public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e, AnnotationMirror a) {
         logger.log(messageKindToLoggingLevel(kind), msg.toString());
      }

      public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e, AnnotationMirror a, AnnotationValue v) {
         logger.log(messageKindToLoggingLevel(kind), msg.toString());
      }

      private static Level messageKindToLoggingLevel(Diagnostic.Kind kind) {
         switch (kind) {
            case ERROR:
               return Level.ERROR;
            case WARNING:
            case MANDATORY_WARNING:
               return Level.WARN;
            case NOTE:
               return Level.INFO;
            case OTHER:
            default:
               return Level.DEBUG;
         }
      }
   }

   static class DebugInterceptingMessager implements Messager {
      private final Messager wrapped;

      DebugInterceptingMessager(Messager messager) {
         this.wrapped = messager;
      }

      public void printMessage(Diagnostic.Kind kind, CharSequence msg) {
         if (kind != Kind.OTHER) {
            this.wrapped.printMessage(kind, msg);
         }

      }

      public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e) {
         if (kind != Kind.OTHER) {
            this.wrapped.printMessage(kind, msg, e);
         }

      }

      public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e, AnnotationMirror a) {
         if (kind != Kind.OTHER) {
            this.wrapped.printMessage(kind, msg, e, a);
         }

      }

      public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e, AnnotationMirror a, AnnotationValue v) {
         if (kind != Kind.OTHER) {
            this.wrapped.printMessage(kind, msg, e, a, v);
         }

      }
   }
}
