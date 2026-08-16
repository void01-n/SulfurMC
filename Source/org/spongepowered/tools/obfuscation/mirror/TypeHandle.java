package org.spongepowered.tools.obfuscation.mirror;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorByName;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.asm.IAnnotationHandle;
import org.spongepowered.include.com.google.common.collect.ImmutableList;
import org.spongepowered.tools.obfuscation.interfaces.ITypeHandleProvider;
import org.spongepowered.tools.obfuscation.mirror.mapping.MappingMethodResolvable;

public class TypeHandle {
   private final String name;
   private final PackageElement pkg;
   private final TypeElement element;
   protected final ITypeHandleProvider typeProvider;
   private TypeReference reference;

   public TypeHandle(PackageElement pkg, String name, ITypeHandleProvider typeProvider) {
      this.name = name.replace('.', '/');
      this.pkg = pkg;
      this.element = null;
      this.typeProvider = typeProvider;
   }

   public TypeHandle(TypeElement element, ITypeHandleProvider typeProvider) {
      this.pkg = TypeUtils.getPackage(element);
      this.name = TypeUtils.getInternalName(element);
      this.element = element;
      this.typeProvider = typeProvider;
   }

   public TypeHandle(DeclaredType type, ITypeHandleProvider typeProvider) {
      this((TypeElement)type.asElement(), typeProvider);
   }

   public final String toString() {
      return this.name.replace('/', '.');
   }

   public final String getName() {
      return this.name;
   }

   public final String getSimpleName() {
      return Bytecode.getSimpleName(this.name);
   }

   public final PackageElement getPackage() {
      return this.pkg;
   }

   public final TypeElement getElement() {
      return this.element;
   }

   protected TypeElement getTargetElement() {
      return this.element;
   }

   public IAnnotationHandle getAnnotation(Class<? extends Annotation> annotationClass) {
      return AnnotationHandle.of(this.getTargetElement(), annotationClass);
   }

   protected final List<? extends Element> getEnclosedElements() {
      return getEnclosedElements(this.getTargetElement());
   }

   protected <T extends Element> List<T> getEnclosedElements(ElementKind... kind) {
      return getEnclosedElements(this.getTargetElement(), kind);
   }

   public boolean hasTypeMirror() {
      return this.getTargetElement() != null;
   }

   public TypeMirror getTypeMirror() {
      return this.getTargetElement() != null ? this.getTargetElement().asType() : null;
   }

   public TypeHandle getSuperclass() {
      TypeElement targetElement = this.getTargetElement();
      if (targetElement == null) {
         return null;
      } else {
         TypeMirror superClass = targetElement.getSuperclass();
         return superClass != null && superClass.getKind() != TypeKind.NONE ? this.typeProvider.getTypeHandle((Object)superClass) : null;
      }
   }

   public List<TypeHandle> getInterfaces() {
      if (this.getTargetElement() == null) {
         return Collections.emptyList();
      } else {
         ImmutableList.Builder<TypeHandle> list = ImmutableList.<TypeHandle>builder();

         for(TypeMirror iface : this.getTargetElement().getInterfaces()) {
            list.add(this.typeProvider.getTypeHandle((Object)iface));
         }

         return list.build();
      }
   }

   public List<MethodHandle> getMethods() {
      List<MethodHandle> methods = new ArrayList();

      for(ExecutableElement method : this.getEnclosedElements(ElementKind.METHOD)) {
         MethodHandle handle = new MethodHandle(this, method);
         methods.add(handle);
      }

      return methods;
   }

   public boolean isPublic() {
      TypeElement targetElement = this.getTargetElement();
      if (targetElement != null && targetElement.getModifiers().contains(Modifier.PUBLIC)) {
         for(Element e = targetElement.getEnclosingElement(); e != null && e.getKind() != ElementKind.PACKAGE; e = e.getEnclosingElement()) {
            if (!e.getModifiers().contains(Modifier.PUBLIC)) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean isImaginary() {
      return this.getTargetElement() == null;
   }

   public boolean isSimulated() {
      return false;
   }

   public boolean isNotInterface() {
      TypeElement target = this.getTargetElement();
      return target != null && !target.getKind().isInterface();
   }

   public boolean isSuperTypeOf(TypeHandle other) {
      List<TypeHandle> superTypes = new ArrayList();
      if (other.getSuperclass() != null) {
         superTypes.add(other.getSuperclass());
      }

      superTypes.addAll(other.getInterfaces());

      for(TypeHandle superType : superTypes) {
         if (this.name.equals(superType.name) || this.isSuperTypeOf(superType)) {
            return true;
         }
      }

      return false;
   }

   public final TypeReference getReference() {
      if (this.reference == null) {
         this.reference = new TypeReference(this);
      }

      return this.reference;
   }

   public MappingMethod getMappingMethod(String name, String desc) {
      return new MappingMethodResolvable(this, name, desc);
   }

   public String findDescriptor(ITargetSelectorByName selector) {
      String desc = selector.getDesc();
      if (desc == null) {
         for(ExecutableElement method : this.getEnclosedElements(ElementKind.METHOD)) {
            if (method.getSimpleName().toString().equals(selector.getName())) {
               desc = TypeUtils.getDescriptor(method);
               break;
            }
         }
      }

      return desc;
   }

   public final FieldHandle findField(VariableElement element) {
      return this.findField(element, true);
   }

   public final FieldHandle findField(VariableElement element, boolean matchCase) {
      return this.findField(element.getSimpleName().toString(), TypeUtils.getTypeName(element.asType()), matchCase);
   }

   public final FieldHandle findField(String name, String type) {
      return this.findField(name, type, true);
   }

   public FieldHandle findField(String name, String type, boolean matchCase) {
      String rawType = TypeUtils.stripGenerics(type);

      for(VariableElement field : this.getEnclosedElements(ElementKind.FIELD)) {
         if (compareElement(field, name, type, matchCase)) {
            return new FieldHandle(this.getTargetElement(), field);
         }

         if (compareElement(field, name, rawType, matchCase)) {
            return new FieldHandle(this.getTargetElement(), field, true);
         }
      }

      return null;
   }

   public final MethodHandle findMethod(ExecutableElement element) {
      return this.findMethod(element, true);
   }

   public final MethodHandle findMethod(ExecutableElement element, boolean matchCase) {
      return this.findMethod(element.getSimpleName().toString(), TypeUtils.getJavaSignature((Element)element), matchCase);
   }

   public final MethodHandle findMethod(String name, String signature) {
      return this.findMethod(name, signature, true);
   }

   public MethodHandle findMethod(String name, String signature, boolean matchCase) {
      String rawSignature = TypeUtils.stripGenerics(signature);
      return findMethod(this, name, signature, rawSignature, matchCase);
   }

   protected static MethodHandle findMethod(TypeHandle target, String name, String signature, String rawSignature, boolean matchCase) {
      for(ExecutableElement method : getEnclosedElements(target.getTargetElement(), ElementKind.CONSTRUCTOR, ElementKind.METHOD)) {
         if (compareElement(method, name, signature, matchCase) || compareElement(method, name, rawSignature, matchCase)) {
            return new MethodHandle(target, method);
         }
      }

      return null;
   }

   protected static boolean compareElement(Element elem, String name, String type, boolean matchCase) {
      try {
         String elementName = elem.getSimpleName().toString();
         String elementType = TypeUtils.getJavaSignature(elem);
         String rawElementType = TypeUtils.stripGenerics(elementType);
         boolean compared = matchCase ? name.equals(elementName) : name.equalsIgnoreCase(elementName);
         return compared && (type.length() == 0 || type.equals(elementType) || type.equals(rawElementType));
      } catch (NullPointerException var8) {
         return false;
      }
   }

   protected static <T extends Element> List<T> getEnclosedElements(TypeElement targetElement, ElementKind... kind) {
      if (kind != null && kind.length >= 1) {
         if (targetElement == null) {
            return Collections.emptyList();
         } else {
            ImmutableList.Builder<T> list = ImmutableList.<T>builder();

            for(Element elem : targetElement.getEnclosedElements()) {
               for(ElementKind ek : kind) {
                  if (elem.getKind() == ek) {
                     list.add(elem);
                     break;
                  }
               }
            }

            return list.build();
         }
      } else {
         return getEnclosedElements(targetElement);
      }
   }

   protected static List<? extends Element> getEnclosedElements(TypeElement targetElement) {
      return targetElement != null ? targetElement.getEnclosedElements() : Collections.emptyList();
   }
}
