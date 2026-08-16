package org.spongepowered.tools.obfuscation.mirror;

import java.lang.annotation.Annotation;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorByName;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.asm.util.SignaturePrinter;
import org.spongepowered.tools.obfuscation.interfaces.ITypeHandleProvider;

public class TypeHandleSimulated extends TypeHandle {
   private final TypeElement simulatedType;

   public TypeHandleSimulated(String name, TypeMirror type, ITypeHandleProvider typeProvider) {
      this(TypeUtils.getPackage(type), name, type, typeProvider);
   }

   public TypeHandleSimulated(PackageElement pkg, String name, TypeMirror type, ITypeHandleProvider typeProvider) {
      super(pkg, name, typeProvider);
      this.simulatedType = (TypeElement)((DeclaredType)type).asElement();
   }

   protected TypeElement getTargetElement() {
      return this.simulatedType;
   }

   public boolean isPublic() {
      return true;
   }

   public boolean isImaginary() {
      return false;
   }

   public boolean isSimulated() {
      return true;
   }

   public AnnotationHandle getAnnotation(Class<? extends Annotation> annotationClass) {
      return null;
   }

   public TypeHandle getSuperclass() {
      return null;
   }

   public String findDescriptor(ITargetSelectorByName memberInfo) {
      return memberInfo != null ? memberInfo.getDesc() : null;
   }

   public FieldHandle findField(String name, String type, boolean caseSensitive) {
      return new FieldHandle((String)null, name, type);
   }

   public MethodHandle findMethod(String name, String desc, boolean caseSensitive) {
      return new MethodHandle((TypeHandle)null, name, desc);
   }

   public MappingMethod getMappingMethod(String name, String desc) {
      String signature = (new SignaturePrinter(name, desc)).setFullyQualified(true).toDescriptor();
      String rawSignature = TypeUtils.stripGenerics(signature);
      MethodHandle method = findMethodRecursive((TypeHandle)this, name, signature, rawSignature, true, this.typeProvider);
      return method != null ? method.asMapping(true) : super.getMappingMethod(name, desc);
   }

   private static MethodHandle findMethodRecursive(TypeHandle target, String name, String signature, String rawSignature, boolean matchCase, ITypeHandleProvider typeProvider) {
      TypeElement elem = target.getTargetElement();
      if (elem == null) {
         return null;
      } else {
         MethodHandle method = TypeHandle.findMethod(target, name, signature, rawSignature, matchCase);
         if (method != null) {
            return method;
         } else {
            for(TypeMirror iface : elem.getInterfaces()) {
               method = findMethodRecursive(iface, name, signature, rawSignature, matchCase, typeProvider);
               if (method != null) {
                  return method;
               }
            }

            TypeMirror superClass = elem.getSuperclass();
            if (superClass != null && superClass.getKind() != TypeKind.NONE) {
               return findMethodRecursive(superClass, name, signature, rawSignature, matchCase, typeProvider);
            } else {
               return null;
            }
         }
      }
   }

   private static MethodHandle findMethodRecursive(TypeMirror target, String name, String signature, String rawSignature, boolean matchCase, ITypeHandleProvider typeProvider) {
      return !(target instanceof DeclaredType) ? null : findMethodRecursive(typeProvider.getTypeHandle((Object)target), name, signature, rawSignature, matchCase, typeProvider);
   }
}
