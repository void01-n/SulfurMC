package org.spongepowered.tools.obfuscation.interfaces;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;

public interface IMessagerEx extends Messager {
   void printMessage(MessageType var1, CharSequence var2);

   void printMessage(MessageType var1, CharSequence var2, Element var3);

   void printMessage(MessageType var1, CharSequence var2, Element var3, AnnotationMirror var4);

   void printMessage(MessageType var1, CharSequence var2, Element var3, AnnotationMirror var4, AnnotationValue var5);

   public static enum MessageType {
      INFO(Kind.NOTE),
      NOTE(Kind.NOTE),
      ERROR(Kind.ERROR),
      WARNING(Kind.WARNING),
      MIXIN_ON_INVALID_TYPE(Kind.ERROR),
      MIXIN_SOFT_TARGET_NOT_FOUND(Kind.ERROR),
      MIXIN_SOFT_TARGET_NOT_RESOLVED(Kind.WARNING),
      MIXIN_SOFT_TARGET_IS_PUBLIC(Kind.WARNING),
      MIXIN_NO_TARGETS(Kind.ERROR),
      PARENT_VALIDATOR(Kind.ERROR),
      TARGET_VALIDATOR(Kind.ERROR),
      ACCESSOR_ATTACH_ERROR(Kind.ERROR),
      ACCESSOR_TARGET_NOT_FOUND(Kind.ERROR),
      ACCESSOR_TYPE_UNSUPPORTED(Kind.WARNING),
      ACCESSOR_NAME_UNRESOLVED(Kind.WARNING),
      INVOKER_RAW_RETURN_TYPE(Kind.WARNING),
      FACTORY_INVOKER_GENERIC_ARGS(Kind.ERROR),
      FACTORY_INVOKER_RETURN_TYPE(Kind.ERROR),
      FACTORY_INVOKER_NONSTATIC(Kind.ERROR),
      CONSTRAINT_VIOLATION(Kind.ERROR),
      INVALID_CONSTRAINT(Kind.WARNING),
      ACCESSOR_MAPPING_CONFLICT(Kind.ERROR),
      INJECTOR_MAPPING_CONFLICT(Kind.ERROR),
      OVERWRITE_MAPPING_CONFLICT(Kind.ERROR),
      SHADOW_MAPPING_CONFLICT(Kind.ERROR),
      INJECTOR_IN_INTERFACE(Kind.ERROR),
      INJECTOR_ON_NON_METHOD_ELEMENT(Kind.WARNING),
      OVERWRITE_ON_NON_METHOD_ELEMENT(Kind.ERROR),
      ACCESSOR_ON_NON_METHOD_ELEMENT(Kind.ERROR),
      SHADOW_ON_INVALID_ELEMENT(Kind.ERROR),
      INJECTOR_ON_NON_MIXIN_METHOD(Kind.ERROR),
      OVERWRITE_ON_NON_MIXIN_METHOD(Kind.ERROR),
      ACCESSOR_ON_NON_MIXIN_METHOD(Kind.ERROR),
      SHADOW_ON_NON_MIXIN_ELEMENT(Kind.ERROR),
      SOFT_IMPLEMENTS_ON_INVALID_TYPE(Kind.ERROR),
      SOFT_IMPLEMENTS_ON_NON_MIXIN(Kind.ERROR),
      SOFT_IMPLEMENTS_EMPTY(Kind.WARNING),
      TARGET_SELECTOR_VALIDATION(Kind.ERROR),
      INJECTOR_TARGET_NOT_FULLY_QUALIFIED(Kind.ERROR),
      MISSING_INJECTOR_DESC_MULTITARGET(Kind.ERROR),
      MISSING_INJECTOR_DESC_SINGLETARGET(Kind.WARNING),
      MISSING_INJECTOR_DESC_SIMULATED(Kind.OTHER),
      TARGET_ELEMENT_NOT_FOUND(Kind.WARNING),
      METHOD_VISIBILITY(Kind.WARNING),
      NO_OBFDATA_FOR_ACCESSOR(Kind.WARNING),
      NO_OBFDATA_FOR_CLASS(Kind.WARNING),
      NO_OBFDATA_FOR_TARGET(Kind.ERROR),
      NO_OBFDATA_FOR_CTOR(Kind.WARNING),
      NO_OBFDATA_FOR_OVERWRITE(Kind.ERROR),
      NO_OBFDATA_FOR_STATIC_OVERWRITE(Kind.WARNING),
      NO_OBFDATA_FOR_FIELD(Kind.WARNING),
      NO_OBFDATA_FOR_METHOD(Kind.WARNING),
      NO_OBFDATA_FOR_SHADOW(Kind.WARNING),
      NO_OBFDATA_FOR_SIMULATED_SHADOW(Kind.WARNING),
      NO_OBFDATA_FOR_SOFT_IMPLEMENTS(Kind.ERROR),
      BARE_REFERENCE(Kind.WARNING),
      OVERWRITE_DOCS(Kind.WARNING);

      private static boolean decorate = false;
      private static String prefix = "";
      private final Diagnostic.Kind originalKind;
      private Diagnostic.Kind kind;
      private boolean enabled = true;
      private boolean setByUser = false;

      private MessageType(Diagnostic.Kind kind) {
         this.originalKind = this.kind = kind;
      }

      public boolean isError() {
         return this.kind == Kind.ERROR;
      }

      public Diagnostic.Kind getKind() {
         return this.kind;
      }

      public void setKind(Diagnostic.Kind kind) {
         this.kind = kind;
         this.setByUser = true;
      }

      public void quench(Diagnostic.Kind kind) {
         if (!this.setByUser && kind.ordinal() > this.kind.ordinal()) {
            this.kind = kind;
         }

      }

      public boolean isEnabled() {
         return this.enabled;
      }

      public void setEnabled(boolean enabled) {
         this.enabled = enabled;
         this.setByUser = true;
      }

      public void reset() {
         this.kind = this.originalKind;
         this.enabled = true;
         this.setByUser = false;
      }

      public CharSequence decorate(CharSequence msg) {
         return decorate ? String.format("%s[%s] %s", prefix, this.name(), msg) : prefix + msg;
      }

      public static void setDecoration(boolean enabled) {
         decorate = enabled;
      }

      public static void setPrefix(String prefix) {
         IMessagerEx.MessageType.prefix = prefix;
      }

      public static Set<String> getSupportedOptions() {
         Set<String> supportedOptions = new HashSet();

         for(MessageType type : values()) {
            supportedOptions.add("MSG_" + type.name());
         }

         return supportedOptions;
      }

      public static void applyOptions(IMixinAnnotationProcessor.CompilerEnvironment env, IOptionProvider options) {
         setDecoration("true".equalsIgnoreCase(options.getOption("showMessageTypes")));
         INFO.setEnabled(!env.isDevelopmentEnvironment() && !"true".equalsIgnoreCase(options.getOption("quiet")));
         INJECTOR_IN_INTERFACE.setEnabled(options.getOption("disableInterfaceMixins", false));
         if ("error".equalsIgnoreCase(options.getOption("overwriteErrorLevel"))) {
            OVERWRITE_DOCS.setKind(Kind.ERROR);
         }

         for(MessageType type : values()) {
            String option = options.getOption("MSG_" + type.name());
            if (option != null) {
               if ("note".equalsIgnoreCase(option)) {
                  type.setKind(Kind.NOTE);
               } else if ("warning".equalsIgnoreCase(option)) {
                  type.setKind(Kind.WARNING);
               } else if ("error".equalsIgnoreCase(option)) {
                  type.setKind(Kind.ERROR);
               } else if ("disabled".equalsIgnoreCase(option)) {
                  type.setEnabled(false);
               }
            }
         }

      }

      // $FF: synthetic method
      private static MessageType[] $values() {
         return new MessageType[]{INFO, NOTE, ERROR, WARNING, MIXIN_ON_INVALID_TYPE, MIXIN_SOFT_TARGET_NOT_FOUND, MIXIN_SOFT_TARGET_NOT_RESOLVED, MIXIN_SOFT_TARGET_IS_PUBLIC, MIXIN_NO_TARGETS, PARENT_VALIDATOR, TARGET_VALIDATOR, ACCESSOR_ATTACH_ERROR, ACCESSOR_TARGET_NOT_FOUND, ACCESSOR_TYPE_UNSUPPORTED, ACCESSOR_NAME_UNRESOLVED, INVOKER_RAW_RETURN_TYPE, FACTORY_INVOKER_GENERIC_ARGS, FACTORY_INVOKER_RETURN_TYPE, FACTORY_INVOKER_NONSTATIC, CONSTRAINT_VIOLATION, INVALID_CONSTRAINT, ACCESSOR_MAPPING_CONFLICT, INJECTOR_MAPPING_CONFLICT, OVERWRITE_MAPPING_CONFLICT, SHADOW_MAPPING_CONFLICT, INJECTOR_IN_INTERFACE, INJECTOR_ON_NON_METHOD_ELEMENT, OVERWRITE_ON_NON_METHOD_ELEMENT, ACCESSOR_ON_NON_METHOD_ELEMENT, SHADOW_ON_INVALID_ELEMENT, INJECTOR_ON_NON_MIXIN_METHOD, OVERWRITE_ON_NON_MIXIN_METHOD, ACCESSOR_ON_NON_MIXIN_METHOD, SHADOW_ON_NON_MIXIN_ELEMENT, SOFT_IMPLEMENTS_ON_INVALID_TYPE, SOFT_IMPLEMENTS_ON_NON_MIXIN, SOFT_IMPLEMENTS_EMPTY, TARGET_SELECTOR_VALIDATION, INJECTOR_TARGET_NOT_FULLY_QUALIFIED, MISSING_INJECTOR_DESC_MULTITARGET, MISSING_INJECTOR_DESC_SINGLETARGET, MISSING_INJECTOR_DESC_SIMULATED, TARGET_ELEMENT_NOT_FOUND, METHOD_VISIBILITY, NO_OBFDATA_FOR_ACCESSOR, NO_OBFDATA_FOR_CLASS, NO_OBFDATA_FOR_TARGET, NO_OBFDATA_FOR_CTOR, NO_OBFDATA_FOR_OVERWRITE, NO_OBFDATA_FOR_STATIC_OVERWRITE, NO_OBFDATA_FOR_FIELD, NO_OBFDATA_FOR_METHOD, NO_OBFDATA_FOR_SHADOW, NO_OBFDATA_FOR_SIMULATED_SHADOW, NO_OBFDATA_FOR_SOFT_IMPLEMENTS, BARE_REFERENCE, OVERWRITE_DOCS};
      }
   }
}
