package org.spongepowered.tools.obfuscation;

import java.lang.annotation.Annotation;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorByName;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.obfuscation.mapping.IMapping;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.ConstraintParser;
import org.spongepowered.asm.util.asm.IAnnotatedElement;
import org.spongepowered.asm.util.asm.IAnnotationHandle;
import org.spongepowered.asm.util.throwables.ConstraintViolationException;
import org.spongepowered.asm.util.throwables.InvalidConstraintException;
import org.spongepowered.tools.obfuscation.interfaces.IMessagerEx;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.interfaces.IObfuscationManager;
import org.spongepowered.tools.obfuscation.mapping.IMappingConsumer;
import org.spongepowered.tools.obfuscation.mirror.AnnotationHandle;
import org.spongepowered.tools.obfuscation.mirror.FieldHandle;
import org.spongepowered.tools.obfuscation.mirror.MethodHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeUtils;

abstract class AnnotatedMixinElementHandler {
   protected final AnnotatedMixin mixin;
   protected final String classRef;
   protected final IMixinAnnotationProcessor ap;
   protected final IObfuscationManager obf;
   private IMappingConsumer mappings;

   AnnotatedMixinElementHandler(IMixinAnnotationProcessor ap, AnnotatedMixin mixin) {
      this.ap = ap;
      this.mixin = mixin;
      this.classRef = mixin.getClassRef();
      this.obf = ap.getObfuscationManager();
   }

   private IMappingConsumer getMappings() {
      if (this.mappings == null) {
         IMappingConsumer mappingConsumer = this.mixin.getMappings();
         if (mappingConsumer instanceof Mappings) {
            this.mappings = ((Mappings)mappingConsumer).asUnique();
         } else {
            this.mappings = mappingConsumer;
         }
      }

      return this.mappings;
   }

   protected final void addFieldMapping(ObfuscationType type, ShadowElementName name, String mcpSignature, String obfSignature) {
      this.addFieldMapping(type, name.name(), name.obfuscated(), mcpSignature, obfSignature);
   }

   protected final void addFieldMapping(ObfuscationType type, String mcpName, String obfName, String mcpSignature, String obfSignature) {
      MappingField from = new MappingField(this.classRef, mcpName, mcpSignature);
      MappingField to = new MappingField(this.classRef, obfName, obfSignature);
      this.getMappings().addFieldMapping(type, from, to);
   }

   protected final void addMethodMappings(String mcpName, String mcpSignature, ObfuscationData<MappingMethod> obfData) {
      for(ObfuscationType type : obfData) {
         MappingMethod obfMethod = obfData.get(type);
         this.addMethodMapping(type, mcpName, obfMethod.getSimpleName(), mcpSignature, obfMethod.getDesc());
      }

   }

   protected final void addMethodMapping(ObfuscationType type, ShadowElementName name, String mcpSignature, String obfSignature) {
      this.addMethodMapping(type, name.name(), name.obfuscated(), mcpSignature, obfSignature);
   }

   protected final void addMethodMapping(ObfuscationType type, String mcpName, String obfName, String mcpSignature, String obfSignature) {
      MappingMethod from = new MappingMethod(this.classRef, mcpName, mcpSignature);
      MappingMethod to = new MappingMethod(this.classRef, obfName, obfSignature);
      this.getMappings().addMethodMapping(type, from, to);
   }

   protected final void checkConstraints(ExecutableElement method, AnnotationHandle annotation) {
      try {
         ConstraintParser.Constraint constraint = ConstraintParser.parse((String)annotation.getValue("constraints"));

         try {
            constraint.check(this.ap.getTokenProvider());
         } catch (ConstraintViolationException ex) {
            this.ap.printMessage(IMessagerEx.MessageType.CONSTRAINT_VIOLATION, ex.getMessage(), method, annotation.asMirror());
         }
      } catch (InvalidConstraintException ex) {
         this.ap.printMessage(IMessagerEx.MessageType.INVALID_CONSTRAINT, ex.getMessage(), method, annotation.asMirror(), SuppressedBy.CONSTRAINTS);
      }

   }

   protected final void validateTarget(Element element, AnnotationHandle annotation, AliasedElementName name, String type) {
      if (element instanceof ExecutableElement) {
         this.validateTargetMethod((ExecutableElement)element, annotation, name, type, false, false);
      } else if (element instanceof VariableElement) {
         this.validateTargetField((VariableElement)element, annotation, name, type);
      }

   }

   protected final void validateTargetMethod(ExecutableElement method, AnnotationHandle annotation, AliasedElementName name, String type, boolean overwrite, boolean merge) {
      String signature = TypeUtils.getJavaSignature((Element)method);

      for(TypeHandle target : this.mixin.getTargets()) {
         if (!target.isImaginary()) {
            MethodHandle targetMethod = target.findMethod(method);
            if (targetMethod == null && name.hasPrefix()) {
               targetMethod = target.findMethod(name.baseName(), signature);
            }

            if (targetMethod == null && name.hasAliases()) {
               for(String alias : name.getAliases()) {
                  if ((targetMethod = target.findMethod(alias, signature)) != null) {
                     break;
                  }
               }
            }

            if (targetMethod != null) {
               if (overwrite) {
                  this.validateMethodVisibility(method, annotation, type, target, targetMethod);
               }
            } else if (!merge) {
               this.printMessage(IMessagerEx.MessageType.TARGET_ELEMENT_NOT_FOUND, "Cannot find target for " + type + " method in " + target, method, annotation, SuppressedBy.TARGET);
            }
         }
      }

   }

   private void validateMethodVisibility(ExecutableElement method, AnnotationHandle annotation, String type, TypeHandle target, MethodHandle targetMethod) {
      Bytecode.Visibility visTarget = targetMethod.getVisibility();
      if (visTarget != null) {
         Bytecode.Visibility visMethod = TypeUtils.getVisibility(method);
         String visibility = "visibility of " + visTarget + " method in " + target;
         if (visTarget.ordinal() > visMethod.ordinal()) {
            this.printMessage(IMessagerEx.MessageType.METHOD_VISIBILITY, visMethod + " " + type + " method cannot reduce " + visibility, method, annotation, SuppressedBy.VISIBILITY);
         } else if (visTarget == Bytecode.Visibility.PRIVATE && visMethod.ordinal() > visTarget.ordinal()) {
            this.printMessage(IMessagerEx.MessageType.METHOD_VISIBILITY, visMethod + " " + type + " method will upgrade " + visibility, method, annotation, SuppressedBy.VISIBILITY);
         }

      }
   }

   protected final void validateTargetField(VariableElement field, AnnotationHandle annotation, AliasedElementName name, String type) {
      String fieldType = field.asType().toString();

      for(TypeHandle target : this.mixin.getTargets()) {
         if (!target.isImaginary()) {
            FieldHandle targetField = target.findField(field);
            if (targetField == null) {
               for(String alias : name.getAliases()) {
                  if ((targetField = target.findField(alias, fieldType)) != null) {
                     break;
                  }
               }

               if (targetField == null) {
                  this.ap.printMessage(IMessagerEx.MessageType.TARGET_ELEMENT_NOT_FOUND, "Cannot find target for " + type + " field in " + target, field, annotation.asMirror(), SuppressedBy.TARGET);
               }
            }
         }
      }

   }

   protected final void validateReferencedTarget(AnnotatedElementExecutable elem, String reference, ITargetSelector targetSelector, String subject) {
      if (targetSelector instanceof ITargetSelectorByName) {
         ITargetSelectorByName nameRef = (ITargetSelectorByName)targetSelector;
         String signature = nameRef.toDescriptor();

         for(TypeHandle target : this.mixin.getTargets()) {
            if (!target.isImaginary()) {
               MethodHandle targetMethod = target.findMethod(nameRef.getName(), signature);
               if (targetMethod == null) {
                  this.ap.printMessage(IMessagerEx.MessageType.TARGET_ELEMENT_NOT_FOUND, "Cannot find target method \"" + nameRef.getName() + nameRef.getDesc() + "\" for " + subject + " in " + target, elem.getElement(), elem.getAnnotation().asMirror(), SuppressedBy.TARGET);
               }
            }
         }

      }
   }

   private void printMessage(IMessagerEx.MessageType type, String msg, Element e, AnnotationHandle annotation, SuppressedBy suppressedBy) {
      if (annotation == null) {
         this.ap.printMessage(type, msg, e, suppressedBy);
      } else {
         this.ap.printMessage(type, msg, e, annotation.asMirror(), suppressedBy);
      }

   }

   protected static <T extends IMapping<T>> ObfuscationData<T> stripOwnerData(ObfuscationData<T> data) {
      ObfuscationData<T> stripped = new ObfuscationData<T>();

      for(ObfuscationType type : data) {
         T mapping = data.get(type);
         stripped.put(type, (IMapping)mapping.move((String)null));
      }

      return stripped;
   }

   protected static <T extends IMapping<T>> ObfuscationData<T> stripDescriptors(ObfuscationData<T> data) {
      ObfuscationData<T> stripped = new ObfuscationData<T>();

      for(ObfuscationType type : data) {
         T mapping = data.get(type);
         stripped.put(type, (IMapping)mapping.transform((String)null));
      }

      return stripped;
   }

   abstract static class AnnotatedElement<E extends Element> implements IAnnotatedElement {
      protected final E element;
      protected final AnnotationHandle annotation;
      private final String desc;

      public AnnotatedElement(E element, AnnotationHandle annotation) {
         this.element = element;
         this.annotation = annotation;
         this.desc = TypeUtils.getDescriptor(element);
      }

      public E getElement() {
         return this.element;
      }

      public AnnotationHandle getAnnotation() {
         return this.annotation;
      }

      public String getSimpleName() {
         return this.getElement().getSimpleName().toString();
      }

      public String getDesc() {
         return this.desc;
      }

      public final void printMessage(IMessagerEx messager, IMessagerEx.MessageType type, CharSequence msg) {
         messager.printMessage(type, msg, this.element, this.annotation.asMirror());
      }

      public IAnnotationHandle getAnnotation(Class<? extends Annotation> annotationClass) {
         return AnnotationHandle.of(this.element, annotationClass);
      }
   }

   abstract static class AnnotatedElementExecutable extends AnnotatedElement<ExecutableElement> implements ISelectorContext {
      private final IMixinContext context;
      private final String selectorCoordinate;

      public AnnotatedElementExecutable(ExecutableElement element, AnnotationHandle annotation, IMixinContext context, String selectorCoordinate) {
         super(element, annotation);
         this.context = context;
         this.selectorCoordinate = selectorCoordinate;
      }

      public ISelectorContext getParent() {
         return null;
      }

      public IMixinContext getMixin() {
         return this.context;
      }

      public Object getMethod() {
         return new IAnnotatedElement() {
            public IAnnotationHandle getAnnotation(Class<? extends Annotation> annotationClass) {
               return AnnotationHandle.of(AnnotatedElementExecutable.this.getElement(), annotationClass);
            }

            public String toString() {
               return ((ExecutableElement)AnnotatedElementExecutable.this.getElement()).getSimpleName().toString();
            }
         };
      }

      public IAnnotationHandle getSelectorAnnotation() {
         return this.getAnnotation();
      }

      public String getSelectorCoordinate(boolean leaf) {
         return leaf ? this.selectorCoordinate : TypeUtils.getName(this.element);
      }

      public String remap(String reference) {
         return reference;
      }

      public String getElementDescription() {
         return String.format("%s annotation on %s", this.getAnnotation(), this);
      }

      public String toString() {
         return TypeUtils.getName(this.element);
      }
   }

   static class AliasedElementName {
      protected final String originalName;
      private final List<String> aliases;

      public AliasedElementName(Element element, AnnotationHandle annotation) {
         this.originalName = element.getSimpleName().toString();
         this.aliases = annotation.<String>getList("aliases");
      }

      public AliasedElementName(MethodHandle method, AnnotationHandle annotation) {
         this.originalName = method.getName();
         this.aliases = annotation.<String>getList("aliases");
      }

      public boolean hasAliases() {
         return this.aliases.size() > 0;
      }

      public List<String> getAliases() {
         return this.aliases;
      }

      public String baseName() {
         return this.originalName;
      }

      public boolean hasPrefix() {
         return false;
      }
   }

   static class ShadowElementName extends AliasedElementName {
      private final boolean hasPrefix;
      private final String prefix;
      private final String baseName;
      private String obfuscated;

      ShadowElementName(Element element, AnnotationHandle shadow) {
         super(element, shadow);
         this.prefix = (String)shadow.getValue("prefix", "shadow$");
         boolean hasPrefix = false;
         String name = this.originalName;
         if (name.startsWith(this.prefix)) {
            hasPrefix = true;
            name = name.substring(this.prefix.length());
         }

         this.hasPrefix = hasPrefix;
         this.obfuscated = this.baseName = name;
      }

      public String toString() {
         return this.baseName;
      }

      public String baseName() {
         return this.baseName;
      }

      public ShadowElementName setObfuscatedName(String name) {
         this.obfuscated = name;
         return this;
      }

      public boolean hasPrefix() {
         return this.hasPrefix;
      }

      public String name() {
         return this.prefix(this.baseName);
      }

      public String obfuscated() {
         return this.prefix(this.obfuscated);
      }

      public String prefix(String name) {
         return this.hasPrefix ? this.prefix + name : name;
      }
   }
}
