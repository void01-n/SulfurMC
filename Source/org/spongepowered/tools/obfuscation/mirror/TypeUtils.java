package org.spongepowered.tools.obfuscation.mirror;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.objectweb.asm.Type;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.SignaturePrinter;

public abstract class TypeUtils {
   private TypeUtils() {
   }

   public static PackageElement getPackage(TypeMirror type) {
      return !(type instanceof DeclaredType) ? null : getPackage((TypeElement)((DeclaredType)type).asElement());
   }

   public static PackageElement getPackage(TypeElement type) {
      Element parent;
      for(parent = type.getEnclosingElement(); parent != null && !(parent instanceof PackageElement); parent = parent.getEnclosingElement()) {
      }

      return (PackageElement)parent;
   }

   public static String getElementType(Element element) {
      if (element instanceof TypeElement) {
         return "TypeElement";
      } else if (element instanceof ExecutableElement) {
         return "ExecutableElement";
      } else if (element instanceof VariableElement) {
         return "VariableElement";
      } else if (element instanceof PackageElement) {
         return "PackageElement";
      } else {
         return element instanceof TypeParameterElement ? "TypeParameterElement" : element.getClass().getSimpleName();
      }
   }

   public static String stripGenerics(String type) {
      StringBuilder sb = new StringBuilder();
      int pos = 0;

      for(int depth = 0; pos < type.length(); ++pos) {
         char c = type.charAt(pos);
         if (c == '<') {
            ++depth;
         }

         if (depth == 0) {
            sb.append(c);
         } else if (c == '>') {
            --depth;
         }
      }

      return sb.toString();
   }

   public static String getName(VariableElement field) {
      return field != null ? field.getSimpleName().toString() : null;
   }

   public static String getName(ExecutableElement method) {
      return method != null ? method.getSimpleName().toString() : null;
   }

   public static String getJavaSignature(Element element) {
      if (element == null) {
         return "";
      } else if (element instanceof ExecutableElement) {
         ExecutableElement method = (ExecutableElement)element;
         StringBuilder desc = (new StringBuilder()).append("(");
         boolean extra = false;

         for(TypeName arg : getAllParameterTypes(method)) {
            if (extra) {
               desc.append(',');
            }

            desc.append(arg.name);
            extra = true;
         }

         desc.append(')').append(getTypeName(method.getReturnType()));
         return desc.toString();
      } else {
         return getTypeName(element.asType());
      }
   }

   public static String getJavaSignature(String descriptor) {
      return !descriptor.contains("(") ? SignaturePrinter.getTypeName(Type.getType(descriptor), false, true) : (new SignaturePrinter("", descriptor)).setFullyQualified(true).toDescriptor();
   }

   public static String getSimpleName(TypeMirror type) {
      String name = getTypeName(type);
      int pos = name.lastIndexOf(46);
      return pos > 0 ? name.substring(pos + 1) : name;
   }

   public static String getTypeName(TypeMirror type) {
      switch (type.getKind()) {
         case ARRAY:
            return getTypeName(((ArrayType)type).getComponentType()) + "[]";
         case DECLARED:
            return getTypeName((DeclaredType)type);
         case TYPEVAR:
            return getTypeName(getUpperBound(type));
         case ERROR:
            return "java.lang.Object";
         default:
            return type.toString();
      }
   }

   public static String getTypeName(DeclaredType type) {
      return type == null ? "java.lang.Object" : getInternalName((TypeElement)type.asElement()).replace('/', '.');
   }

   public static String getDescriptor(Element element) {
      if (element instanceof ExecutableElement) {
         return getDescriptor((ExecutableElement)element);
      } else {
         return element instanceof VariableElement ? getInternalName((VariableElement)element) : getInternalName(element.asType());
      }
   }

   public static String getDescriptor(ExecutableElement method) {
      if (method == null) {
         return null;
      } else {
         StringBuilder signature = new StringBuilder();

         for(TypeName var : getAllParameterTypes(method)) {
            signature.append(var.descriptor);
         }

         String returnType = getInternalName(method.getReturnType());
         return String.format("(%s)%s", signature, returnType);
      }
   }

   public static String getInternalName(VariableElement field) {
      return field == null ? null : getInternalName(field.asType());
   }

   public static String getInternalName(TypeMirror type) {
      switch (type.getKind()) {
         case ARRAY:
            return "[" + getInternalName(((ArrayType)type).getComponentType());
         case DECLARED:
            return "L" + getInternalName((DeclaredType)type) + ";";
         case TYPEVAR:
            return "L" + getInternalName(getUpperBound(type)) + ";";
         case ERROR:
            return "Ljava/lang/Object;";
         case BOOLEAN:
            return "Z";
         case BYTE:
            return "B";
         case CHAR:
            return "C";
         case DOUBLE:
            return "D";
         case FLOAT:
            return "F";
         case INT:
            return "I";
         case LONG:
            return "J";
         case SHORT:
            return "S";
         case VOID:
            return "V";
         default:
            throw new IllegalArgumentException("Unable to parse type symbol " + type + " with " + type.getKind() + " to equivalent bytecode type");
      }
   }

   public static String getInternalName(DeclaredType type) {
      return type == null ? "java/lang/Object" : getInternalName((TypeElement)type.asElement());
   }

   public static String getInternalName(TypeElement element) {
      if (element == null) {
         return null;
      } else {
         StringBuilder reference = new StringBuilder();
         reference.append(element.getSimpleName());

         for(Element parent = element.getEnclosingElement(); parent != null; parent = parent.getEnclosingElement()) {
            if (parent instanceof TypeElement) {
               reference.insert(0, "$").insert(0, parent.getSimpleName());
            } else if (parent instanceof PackageElement) {
               reference.insert(0, "/").insert(0, ((PackageElement)parent).getQualifiedName().toString().replace('.', '/'));
            }
         }

         return reference.toString();
      }
   }

   private static DeclaredType getUpperBound(TypeMirror type) {
      try {
         return getUpperBound0(type, 5);
      } catch (IllegalStateException ex) {
         throw new IllegalArgumentException("Type symbol \"" + type + "\" is too complex", ex);
      } catch (IllegalArgumentException ex) {
         throw new IllegalArgumentException("Unable to compute upper bound of type symbol " + type, ex);
      }
   }

   private static DeclaredType getUpperBound0(TypeMirror type, int depth) {
      if (depth == 0) {
         throw new IllegalStateException("Generic symbol \"" + type + "\" is too complex, exceeded " + 5 + " iterations attempting to determine upper bound");
      } else if (type instanceof IntersectionType) {
         TypeMirror first = (TypeMirror)((IntersectionType)type).getBounds().get(0);
         --depth;
         return getUpperBound0(first, depth);
      } else if (type instanceof DeclaredType) {
         return (DeclaredType)type;
      } else if (type instanceof TypeVariable) {
         try {
            TypeMirror upper = ((TypeVariable)type).getUpperBound();
            --depth;
            return getUpperBound0(upper, depth);
         } catch (IllegalStateException ex) {
            throw ex;
         } catch (IllegalArgumentException ex) {
            throw ex;
         } catch (Exception var5) {
            throw new IllegalArgumentException("Unable to compute upper bound of type symbol " + type);
         }
      } else {
         return null;
      }
   }

   private static String describeGenericBound(TypeMirror type) {
      if (type instanceof TypeVariable) {
         StringBuilder description = new StringBuilder("<");
         TypeVariable typeVar = (TypeVariable)type;
         description.append(typeVar.toString());
         TypeMirror lowerBound = typeVar.getLowerBound();
         if (lowerBound.getKind() != TypeKind.NULL) {
            description.append(" super ").append(lowerBound);
         }

         TypeMirror upperBound = typeVar.getUpperBound();
         if (upperBound.getKind() != TypeKind.NULL) {
            description.append(" extends ").append(upperBound);
         }

         return description.append(">").toString();
      } else {
         return type.toString();
      }
   }

   public static boolean isAssignable(ProcessingEnvironment processingEnv, TypeMirror targetType, TypeMirror superClass) {
      boolean assignable = processingEnv.getTypeUtils().isAssignable(targetType, superClass);
      if (!assignable && targetType instanceof DeclaredType && superClass instanceof DeclaredType) {
         TypeMirror rawTargetType = toRawType(processingEnv, (DeclaredType)targetType);
         TypeMirror rawSuperType = toRawType(processingEnv, (DeclaredType)superClass);
         return processingEnv.getTypeUtils().isAssignable(rawTargetType, rawSuperType);
      } else {
         return assignable;
      }
   }

   public static EquivalencyResult isEquivalentType(ProcessingEnvironment processingEnv, TypeMirror t1, TypeMirror t2) {
      if (t1 != null && t2 != null) {
         if (processingEnv.getTypeUtils().isSameType(t1, t2)) {
            return TypeUtils.EquivalencyResult.EQUIVALENT;
         } else {
            if (t1 instanceof TypeVariable && t2 instanceof TypeVariable) {
               t1 = getUpperBound(t1);
               t2 = getUpperBound(t2);
               if (processingEnv.getTypeUtils().isSameType(t1, t2)) {
                  return TypeUtils.EquivalencyResult.EQUIVALENT;
               }
            }

            if (t1 instanceof DeclaredType && t2 instanceof DeclaredType) {
               DeclaredType dtT1 = (DeclaredType)t1;
               DeclaredType dtT2 = (DeclaredType)t2;
               TypeMirror rawT1 = toRawType(processingEnv, dtT1);
               TypeMirror rawT2 = toRawType(processingEnv, dtT2);
               if (!processingEnv.getTypeUtils().isSameType(rawT1, rawT2)) {
                  return TypeUtils.EquivalencyResult.notEquivalent("Base types %s and %s are not compatible", rawT1, rawT2);
               } else {
                  List<? extends TypeMirror> argsT1 = dtT1.getTypeArguments();
                  List<? extends TypeMirror> argsT2 = dtT2.getTypeArguments();
                  if (argsT1.size() != argsT2.size()) {
                     if (argsT1.size() == 0) {
                        return TypeUtils.EquivalencyResult.equivalentButRaw(1);
                     } else {
                        return argsT2.size() == 0 ? TypeUtils.EquivalencyResult.equivalentButRaw(2) : TypeUtils.EquivalencyResult.notEquivalent("Mismatched generic argument counts %s<[%d]> and %s<[%d]>", rawT1, argsT1.size(), rawT2, argsT2.size());
                     }
                  } else {
                     for(int arg = 0; arg < argsT1.size(); ++arg) {
                        TypeMirror argT1 = (TypeMirror)argsT1.get(arg);
                        TypeMirror argT2 = (TypeMirror)argsT2.get(arg);
                        if (isEquivalentType(processingEnv, argT1, argT2).type != TypeUtils.Equivalency.EQUIVALENT) {
                           return TypeUtils.EquivalencyResult.boundsMismatch("Generic bounds mismatch between %s and %s", describeGenericBound(argT1), describeGenericBound(argT2));
                        }
                     }

                     return TypeUtils.EquivalencyResult.EQUIVALENT;
                  }
               }
            } else {
               return TypeUtils.EquivalencyResult.notEquivalent("%s and %s do not match", t1, t2);
            }
         }
      } else {
         return TypeUtils.EquivalencyResult.notEquivalent("Invalid types supplied: %s, %s", t1, t2);
      }
   }

   private static TypeMirror toRawType(ProcessingEnvironment processingEnv, DeclaredType targetType) {
      if (targetType.getKind() == TypeKind.INTERSECTION) {
         return targetType;
      } else {
         Name qualifiedName = ((TypeElement)targetType.asElement()).getQualifiedName();
         TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(qualifiedName);
         return (TypeMirror)(typeElement != null ? typeElement.asType() : targetType);
      }
   }

   public static Bytecode.Visibility getVisibility(Element element) {
      if (element == null) {
         return null;
      } else {
         for(Modifier modifier : element.getModifiers()) {
            switch (modifier) {
               case PUBLIC:
                  return Bytecode.Visibility.PUBLIC;
               case PROTECTED:
                  return Bytecode.Visibility.PROTECTED;
               case PRIVATE:
                  return Bytecode.Visibility.PRIVATE;
            }
         }

         return Bytecode.Visibility.PACKAGE;
      }
   }

   private static List<TypeName> getAllParameterTypes(ExecutableElement element) {
      List<TypeName> result = new ArrayList();
      if (element.getKind() == ElementKind.CONSTRUCTOR && element.getEnclosingElement().getKind() == ElementKind.ENUM) {
         result.add(new TypeName("java.lang.String", "Ljava/lang/String;"));
         result.add(new TypeName("int", "I"));
      }

      for(VariableElement param : element.getParameters()) {
         result.add(new TypeName(param.asType()));
      }

      return result;
   }

   public static enum Equivalency {
      NOT_EQUIVALENT,
      EQUIVALENT_BUT_RAW,
      BOUNDS_MISMATCH,
      EQUIVALENT;

      // $FF: synthetic method
      private static Equivalency[] $values() {
         return new Equivalency[]{NOT_EQUIVALENT, EQUIVALENT_BUT_RAW, BOUNDS_MISMATCH, EQUIVALENT};
      }
   }

   public static class EquivalencyResult {
      static final EquivalencyResult EQUIVALENT;
      public final Equivalency type;
      public final String detail;
      public final int rawType;

      EquivalencyResult(Equivalency type, String detail, int rawType) {
         this.type = type;
         this.detail = detail;
         this.rawType = rawType;
      }

      public String toString() {
         return this.detail;
      }

      static EquivalencyResult notEquivalent(String format, Object... args) {
         return new EquivalencyResult(TypeUtils.Equivalency.NOT_EQUIVALENT, String.format(format, args), 0);
      }

      static EquivalencyResult boundsMismatch(String format, Object... args) {
         return new EquivalencyResult(TypeUtils.Equivalency.BOUNDS_MISMATCH, String.format(format, args), 0);
      }

      static EquivalencyResult equivalentButRaw(int rawType) {
         return new EquivalencyResult(TypeUtils.Equivalency.EQUIVALENT_BUT_RAW, String.format("Type %d is raw", rawType), rawType);
      }

      static {
         EQUIVALENT = new EquivalencyResult(TypeUtils.Equivalency.EQUIVALENT, "", 0);
      }
   }

   private static class TypeName {
      final String name;
      final String descriptor;

      public TypeName(String name, String descriptor) {
         this.name = name;
         this.descriptor = descriptor;
      }

      public TypeName(TypeMirror type) {
         this(TypeUtils.getTypeName(type), TypeUtils.getInternalName(type));
      }
   }
}
