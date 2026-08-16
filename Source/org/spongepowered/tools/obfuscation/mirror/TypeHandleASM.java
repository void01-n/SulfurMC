package org.spongepowered.tools.obfuscation.mirror;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Filer;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.StandardLocation;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorByName;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.asm.IAnnotationHandle;
import org.spongepowered.include.com.google.common.collect.ImmutableList;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.interfaces.ITypeHandleProvider;

public class TypeHandleASM extends TypeHandle {
   private static final Map<String, TypeHandleASM> cache = new HashMap();
   private final ClassNode classNode;

   protected TypeHandleASM(PackageElement pkg, String name, ClassNode classNode, ITypeHandleProvider typeProvider) {
      super(pkg, name, typeProvider);
      this.classNode = classNode;
   }

   public IAnnotationHandle getAnnotation(Class<? extends Annotation> annotationClass) {
      AnnotationNode visibleAnnotation = Annotations.getVisible(this.classNode, annotationClass);
      if (visibleAnnotation != null) {
         return Annotations.handleOf(visibleAnnotation);
      } else {
         AnnotationNode invisibleAnnotation = Annotations.getInvisible(this.classNode, annotationClass);
         return (IAnnotationHandle)(invisibleAnnotation != null ? Annotations.handleOf(invisibleAnnotation) : AnnotationHandle.of((AnnotationMirror)null));
      }
   }

   public <T extends Element> List<T> getEnclosedElements(ElementKind... kind) {
      return super.getEnclosedElements(kind);
   }

   public boolean hasTypeMirror() {
      return false;
   }

   public TypeMirror getTypeMirror() {
      return null;
   }

   public TypeHandle getSuperclass() {
      return this.classNode.superName == null ? null : this.typeProvider.getTypeHandle(this.classNode.superName);
   }

   public List<TypeHandle> getInterfaces() {
      ImmutableList.Builder<TypeHandle> list = ImmutableList.<TypeHandle>builder();

      for(String ifaceName : this.classNode.interfaces) {
         TypeHandle iface = this.typeProvider.getTypeHandle(ifaceName);
         if (iface != null) {
            list.add(iface);
         }
      }

      return list.build();
   }

   public List<MethodHandle> getMethods() {
      ImmutableList.Builder<MethodHandle> methods = ImmutableList.<MethodHandle>builder();

      for(MethodNode method : this.classNode.methods) {
         if (!method.name.startsWith("<") && (method.access & 4096) == 0) {
            methods.add(new MethodHandleASM(this, method));
         }
      }

      return methods.build();
   }

   public boolean isPublic() {
      return (this.classNode.access & 1) != 0;
   }

   public boolean isImaginary() {
      return false;
   }

   public boolean isNotInterface() {
      return (this.classNode.access & 512) == 0;
   }

   public String findDescriptor(ITargetSelectorByName selector) {
      String desc = selector.getDesc();
      if (desc == null) {
         for(MethodNode method : this.classNode.methods) {
            if (method.name.equals(selector.getName())) {
               desc = method.desc;
               break;
            }
         }
      }

      return desc;
   }

   public FieldHandle findField(String name, String type, boolean matchCase) {
      for(FieldNode field : this.classNode.fields) {
         if (compareElement(field.name, TypeUtils.getJavaSignature(field.desc), name, type, matchCase)) {
            return new FieldHandleASM(this, field);
         }
      }

      return null;
   }

   public MethodHandle findMethod(String name, String signature, boolean matchCase) {
      for(MethodNode method : this.classNode.methods) {
         if (compareElement(method.name, TypeUtils.getJavaSignature(method.desc), name, signature, matchCase)) {
            return new MethodHandleASM(this, method);
         }
      }

      return null;
   }

   protected static boolean compareElement(String elementName, String elementType, String name, String type, boolean matchCase) {
      try {
         boolean compared = matchCase ? name.equals(elementName) : name.equalsIgnoreCase(elementName);
         return compared && (type.length() == 0 || type.equals(elementType));
      } catch (NullPointerException var6) {
         return false;
      }
   }

   public static TypeHandle of(PackageElement pkg, String name, IMixinAnnotationProcessor ap) {
      String fqName = pkg.getQualifiedName() + "." + name;
      if (cache.containsKey(fqName)) {
         return (TypeHandle)cache.get(fqName);
      } else {
         InputStream is = null;

         try {
            Filer filer = ap.getProcessingEnvironment().getFiler();

            try {
               is = filer.getResource(StandardLocation.CLASS_PATH, pkg.getQualifiedName(), name + ".class").openInputStream();
            } catch (FileNotFoundException var21) {
               is = filer.getResource(StandardLocation.PLATFORM_CLASS_PATH, pkg.getQualifiedName(), name + ".class").openInputStream();
            }

            ClassNode classNode = new ClassNode();
            (new ClassReader(is)).accept(classNode, 0);
            TypeHandleASM typeHandle = new TypeHandleASM(pkg, fqName, classNode, ap.getTypeProvider());
            cache.put(fqName, typeHandle);
            TypeHandleASM var8 = typeHandle;
            return var8;
         } catch (FileNotFoundException var22) {
            cache.put(fqName, (Object)null);
         } catch (Exception var23) {
         } finally {
            if (is != null) {
               try {
                  is.close();
               } catch (IOException ex) {
                  ex.printStackTrace();
               }
            }

         }

         return null;
      }
   }
}
