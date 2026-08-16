package org.spongepowered.tools.obfuscation;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.spongepowered.asm.mixin.gen.AccessorInfo;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorRemappable;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.include.com.google.common.base.Strings;
import org.spongepowered.tools.obfuscation.interfaces.IMessagerEx;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.mirror.AnnotationHandle;
import org.spongepowered.tools.obfuscation.mirror.FieldHandle;
import org.spongepowered.tools.obfuscation.mirror.MethodHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeUtils;

class AnnotatedMixinElementHandlerAccessor extends AnnotatedMixinElementHandler {
   public AnnotatedMixinElementHandlerAccessor(IMixinAnnotationProcessor ap, AnnotatedMixin mixin) {
      super(ap, mixin);
   }

   public void registerAccessor(AnnotatedElementAccessor elem) {
      if (elem.getAccessorType() == null) {
         elem.printMessage(this.ap, IMessagerEx.MessageType.ACCESSOR_TYPE_UNSUPPORTED, "Unsupported accessor type");
      } else {
         String targetName = this.getAccessorTargetName(elem);
         if (targetName == null) {
            elem.printMessage(this.ap, IMessagerEx.MessageType.ACCESSOR_NAME_UNRESOLVED, "Cannot inflect accessor target name");
         } else {
            elem.setTargetName(targetName);

            for(TypeHandle target : this.mixin.getTargets()) {
               try {
                  elem.attach(target);
               } catch (Exception ex) {
                  elem.printMessage(this.ap, IMessagerEx.MessageType.ACCESSOR_ATTACH_ERROR, ex.getMessage());
                  continue;
               }

               if (elem.getAccessorType() == AccessorInfo.AccessorType.OBJECT_FACTORY) {
                  this.registerFactoryForTarget((AnnotatedElementInvoker)elem, target);
               } else if (elem.getAccessorType() == AccessorInfo.AccessorType.METHOD_PROXY) {
                  this.registerInvokerForTarget((AnnotatedElementInvoker)elem, target);
               } else {
                  this.registerAccessorForTarget(elem, target);
               }
            }

         }
      }
   }

   private void registerAccessorForTarget(AnnotatedElementAccessor elem, TypeHandle target) {
      FieldHandle targetField = target.findField(elem.getTargetName(), elem.getTargetTypeName(), false);
      if (targetField == null) {
         if (!target.isImaginary()) {
            elem.printMessage(this.ap, IMessagerEx.MessageType.ACCESSOR_TARGET_NOT_FOUND, "Could not locate @Accessor target " + elem + " in target " + target);
            return;
         }

         targetField = new FieldHandle(target.getName(), elem.getTargetName(), elem.getTargetDesc());
      }

      if (elem.shouldRemap()) {
         ObfuscationData<MappingField> obfData = this.obf.getDataProvider().getObfField(targetField.asMapping(false).move(target.getName()));
         if (obfData.isEmpty()) {
            String info = this.mixin.isMultiTarget() ? " in target " + target : "";
            elem.printMessage(this.ap, IMessagerEx.MessageType.NO_OBFDATA_FOR_ACCESSOR, "Unable to locate obfuscation mapping" + info + " for @Accessor target " + elem);
         } else {
            obfData = AnnotatedMixinElementHandler.<MappingField>stripOwnerData(obfData);

            try {
               this.obf.getReferenceManager().addFieldMapping(this.mixin.getClassRef(), elem.getTargetName(), elem.getContext(), obfData);
            } catch (ReferenceManager.ReferenceConflictException ex) {
               elem.printMessage(this.ap, IMessagerEx.MessageType.ACCESSOR_MAPPING_CONFLICT, "Mapping conflict for @Accessor target " + elem + ": " + ex.getNew() + " for target " + target + " conflicts with existing mapping " + ex.getOld());
            }

         }
      }
   }

   private void registerInvokerForTarget(AnnotatedElementInvoker elem, TypeHandle target) {
      MethodHandle targetMethod = target.findMethod(elem.getTargetName(), elem.getTargetTypeName(), false);
      if (targetMethod == null) {
         if (!target.isImaginary()) {
            elem.printMessage(this.ap, IMessagerEx.MessageType.ACCESSOR_TARGET_NOT_FOUND, "Could not locate @Invoker target " + elem + " in target " + target);
            return;
         }

         targetMethod = new MethodHandle(target, elem.getTargetName(), elem.getTargetDesc());
      }

      if (elem.shouldRemap()) {
         ObfuscationData<MappingMethod> obfData = this.obf.getDataProvider().getObfMethod(targetMethod.asMapping(false).move(target.getName()));
         if (obfData.isEmpty()) {
            String info = this.mixin.isMultiTarget() ? " in target " + target : "";
            elem.printMessage(this.ap, IMessagerEx.MessageType.NO_OBFDATA_FOR_ACCESSOR, "Unable to locate obfuscation mapping" + info + " for @Accessor target " + elem);
         } else {
            obfData = AnnotatedMixinElementHandler.<MappingMethod>stripOwnerData(obfData);

            try {
               this.obf.getReferenceManager().addMethodMapping(this.mixin.getClassRef(), elem.getTargetName(), elem.getContext(), obfData);
            } catch (ReferenceManager.ReferenceConflictException ex) {
               elem.printMessage(this.ap, IMessagerEx.MessageType.ACCESSOR_MAPPING_CONFLICT, "Mapping conflict for @Invoker target " + elem + ": " + ex.getNew() + " for target " + target + " conflicts with existing mapping " + ex.getOld());
            }

         }
      }
   }

   private void registerFactoryForTarget(AnnotatedElementInvoker elem, TypeHandle target) {
      String returnType = TypeUtils.getTypeName(elem.getReturnType());
      if (!returnType.equals(target.toString())) {
         elem.printMessage(this.ap, IMessagerEx.MessageType.FACTORY_INVOKER_RETURN_TYPE, "Invalid Factory @Invoker return type, expected " + target + " but found " + returnType);
      } else if (!elem.isStatic()) {
         elem.printMessage(this.ap, IMessagerEx.MessageType.FACTORY_INVOKER_NONSTATIC, "Factory @Invoker must be static");
      } else if (elem.shouldRemap()) {
         ObfuscationData<String> obfData = this.obf.getDataProvider().getObfClass(elem.getAnnotationValue().replace('.', '/'));
         this.obf.getReferenceManager().addClassMapping(this.mixin.getClassRef(), elem.getAnnotationValue(), obfData);
      }
   }

   private String getAccessorTargetName(AnnotatedElementAccessor elem) {
      String value = elem.getAnnotationValue();
      return Strings.isNullOrEmpty(value) ? this.inflectAccessorTarget(elem) : value;
   }

   private String inflectAccessorTarget(AnnotatedElementAccessor elem) {
      return AccessorInfo.inflectTarget((String)elem.getSimpleName(), elem.getAccessorType(), "", elem, false);
   }

   static class AnnotatedElementAccessor extends AnnotatedMixinElementHandler.AnnotatedElementExecutable {
      protected final boolean shouldRemap;
      protected final TypeMirror returnType;
      protected String targetName;

      public AnnotatedElementAccessor(ExecutableElement element, AnnotationHandle annotation, IMixinContext context, boolean shouldRemap) {
         super(element, annotation, context, "value");
         this.shouldRemap = shouldRemap;
         this.returnType = ((ExecutableElement)this.getElement()).getReturnType();
      }

      public void attach(TypeHandle target) {
      }

      public boolean shouldRemap() {
         return this.shouldRemap;
      }

      public String getAnnotationValue() {
         return (String)this.getAnnotation().getValue();
      }

      public TypeMirror getTargetType() {
         switch (this.getAccessorType()) {
            case FIELD_GETTER:
               return this.returnType;
            case FIELD_SETTER:
               return ((VariableElement)((ExecutableElement)this.getElement()).getParameters().get(0)).asType();
            default:
               return null;
         }
      }

      public String getTargetTypeName() {
         return TypeUtils.getTypeName(this.getTargetType());
      }

      public String getTargetDesc() {
         return TypeUtils.getInternalName(this.getTargetType());
      }

      public ITargetSelectorRemappable getContext() {
         return new MemberInfo(this.getTargetName(), (String)null, this.getTargetDesc());
      }

      public AccessorInfo.AccessorType getAccessorType() {
         return this.returnType.getKind() == TypeKind.VOID ? AccessorInfo.AccessorType.FIELD_SETTER : AccessorInfo.AccessorType.FIELD_GETTER;
      }

      public void setTargetName(String targetName) {
         this.targetName = targetName;
      }

      public String getTargetName() {
         return this.targetName;
      }

      public TypeMirror getReturnType() {
         return this.returnType;
      }

      public boolean isStatic() {
         return ((ExecutableElement)this.element).getModifiers().contains(Modifier.STATIC);
      }

      public String toString() {
         return this.targetName != null ? this.targetName : "<invalid>";
      }
   }

   static class AnnotatedElementInvoker extends AnnotatedElementAccessor {
      private AccessorInfo.AccessorType type;

      public AnnotatedElementInvoker(ExecutableElement element, AnnotationHandle annotation, IMixinContext context, boolean shouldRemap) {
         super(element, annotation, context, shouldRemap);
         this.type = AccessorInfo.AccessorType.METHOD_PROXY;
      }

      public void attach(TypeHandle target) {
         this.type = AccessorInfo.AccessorType.METHOD_PROXY;
         if (this.returnType.getKind() == TypeKind.DECLARED) {
            String specifiedName = this.getAnnotationValue();
            if (specifiedName != null) {
               if ("<init>".equals(specifiedName) || target.getName().equals(specifiedName.replace('.', '/'))) {
                  this.type = AccessorInfo.AccessorType.OBJECT_FACTORY;
               }

            } else {
               AccessorInfo.AccessorName accessorName = AccessorInfo.AccessorName.of(this.getSimpleName(), false);
               if (accessorName != null) {
                  for(String prefix : AccessorInfo.AccessorType.OBJECT_FACTORY.getExpectedPrefixes()) {
                     if (prefix.equals(accessorName.prefix) && ("<init>".equals(accessorName.name) || target.getSimpleName().equalsIgnoreCase(accessorName.name))) {
                        this.type = AccessorInfo.AccessorType.OBJECT_FACTORY;
                        return;
                     }
                  }

               }
            }
         }
      }

      public String getAnnotationValue() {
         String value = super.getAnnotationValue();
         return this.type == AccessorInfo.AccessorType.OBJECT_FACTORY && value == null ? this.returnType.toString() : value;
      }

      public boolean shouldRemap() {
         return (this.type == AccessorInfo.AccessorType.OBJECT_FACTORY || this.type == AccessorInfo.AccessorType.METHOD_PROXY || this.getAnnotationValue() != null) && super.shouldRemap();
      }

      public String getTargetDesc() {
         return this.getDesc();
      }

      public AccessorInfo.AccessorType getAccessorType() {
         return this.type;
      }

      public String getTargetTypeName() {
         return TypeUtils.getJavaSignature(this.getElement());
      }
   }
}
