package org.spongepowered.asm.util;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorByName;
import org.spongepowered.include.com.google.common.base.Strings;

public class SignaturePrinter {
   private final String name;
   private final Type returnType;
   private final Type[] argTypes;
   private final String[] argNames;
   private String modifiers;
   private boolean fullyQualified;

   public SignaturePrinter(MethodNode method) {
      this(method.name, Type.VOID_TYPE, Type.getArgumentTypes(method.desc));
      this.setModifiers(method);
   }

   public SignaturePrinter(MethodNode method, String[] argNames) {
      this(method.name, Type.VOID_TYPE, Type.getArgumentTypes(method.desc), argNames);
      this.setModifiers(method);
   }

   public SignaturePrinter(ITargetSelectorByName member) {
      this(member.getName(), member.getDesc());
   }

   public SignaturePrinter(String name, String desc) {
      this(name, Type.getReturnType(desc), Type.getArgumentTypes(desc));
   }

   public SignaturePrinter(Type[] args) {
      this((String)null, (Type)null, (Type[])args);
   }

   public SignaturePrinter(Type returnType, Type[] args) {
      this((String)null, returnType, (Type[])args);
   }

   public SignaturePrinter(String name, Type returnType, Type[] args) {
      this.modifiers = "private void";
      this.name = name;
      this.returnType = returnType;
      this.argTypes = new Type[args.length];
      this.argNames = new String[args.length];
      int l = 0;

      for(int v = 0; l < args.length; ++l) {
         if (args[l] != null) {
            this.argTypes[l] = args[l];
            this.argNames[l] = "var" + v++;
         }
      }

   }

   public SignaturePrinter(String name, Type returnType, LocalVariableNode[] args) {
      this.modifiers = "private void";
      this.name = name;
      this.returnType = returnType;
      this.argTypes = new Type[args.length];
      this.argNames = new String[args.length];

      for(int l = 0; l < args.length; ++l) {
         if (args[l] != null) {
            this.argTypes[l] = Type.getType(args[l].desc);
            this.argNames[l] = args[l].name;
         }
      }

   }

   public SignaturePrinter(String name, Type returnType, Type[] argTypes, String[] argNames) {
      this.modifiers = "private void";
      this.name = name;
      this.returnType = returnType;
      this.argTypes = argTypes;
      this.argNames = argNames;
   }

   public String getFormattedArgs() {
      return this.appendArgs(new StringBuilder(), true, true).toString();
   }

   public String getReturnType() {
      return getTypeName(this.returnType, false, this.fullyQualified);
   }

   public void setModifiers(MethodNode method) {
      String returnType = getTypeName(Type.getReturnType(method.desc), false, this.fullyQualified);
      String staticType = (method.access & 8) != 0 ? "static " : "";
      if ((method.access & 1) != 0) {
         this.setModifiers("public " + staticType + returnType);
      } else if ((method.access & 4) != 0) {
         this.setModifiers("protected " + staticType + returnType);
      } else if ((method.access & 2) != 0) {
         this.setModifiers("private " + staticType + returnType);
      } else {
         this.setModifiers(staticType + returnType);
      }

   }

   public SignaturePrinter setModifiers(String modifiers) {
      this.modifiers = modifiers.replace("${returnType}", this.getReturnType());
      return this;
   }

   public SignaturePrinter setFullyQualified(boolean fullyQualified) {
      this.fullyQualified = fullyQualified;
      return this;
   }

   public boolean isFullyQualified() {
      return this.fullyQualified;
   }

   public String toString() {
      String name = this.name != null ? this.name : "method";
      return this.appendArgs((new StringBuilder()).append(this.modifiers).append(" ").append(name), false, true).toString();
   }

   public String toDescriptor() {
      StringBuilder args = this.appendArgs(new StringBuilder(), true, false);
      return args.append(getTypeName(this.returnType, false, this.fullyQualified)).toString();
   }

   private StringBuilder appendArgs(StringBuilder sb, boolean typesOnly, boolean pretty) {
      sb.append('(');

      for(int var = 0; var < this.argTypes.length; ++var) {
         if (this.argTypes[var] != null) {
            if (var > 0) {
               sb.append(',');
               if (pretty) {
                  sb.append(' ');
               }
            }

            try {
               String name = typesOnly ? null : (var < this.argNames.length && !Strings.isNullOrEmpty(this.argNames[var]) ? this.argNames[var] : "unnamed" + var);
               this.appendType(sb, this.argTypes[var], name);
            } catch (Exception ex) {
               throw new RuntimeException(ex);
            }
         }
      }

      return sb.append(")");
   }

   private StringBuilder appendType(StringBuilder sb, Type type, String name) {
      switch (type.getSort()) {
         case 9:
            return appendArraySuffix(this.appendType(sb, getElementType(type), name), type);
         case 10:
            return this.appendType(sb, getClassName(type), name);
         default:
            sb.append(getTypeName(type, false, this.fullyQualified));
            if (name != null) {
               sb.append(' ').append(name);
            }

            return sb;
      }
   }

   private StringBuilder appendType(StringBuilder sb, String typeName, String name) {
      if (!this.fullyQualified) {
         typeName = typeName.substring(typeName.lastIndexOf(46) + 1);
      }

      sb.append(typeName);
      if (typeName.endsWith("CallbackInfoReturnable")) {
         sb.append('<').append(getTypeName(this.returnType, true, this.fullyQualified)).append('>');
      }

      if (name != null) {
         sb.append(' ').append(name);
      }

      return sb;
   }

   public static String getTypeName(Type type) {
      return getTypeName(type, false, true);
   }

   public static String getTypeName(Type type, boolean box) {
      return getTypeName(type, box, false);
   }

   public static String getTypeName(Type type, boolean box, boolean fullyQualified) {
      if (type == null) {
         return "{null?}";
      } else {
         switch (type.getSort()) {
            case 0:
               return box ? "Void" : "void";
            case 1:
               return box ? "Boolean" : "boolean";
            case 2:
               return box ? "Character" : "char";
            case 3:
               return box ? "Byte" : "byte";
            case 4:
               return box ? "Short" : "short";
            case 5:
               return box ? "Integer" : "int";
            case 6:
               return box ? "Float" : "float";
            case 7:
               return box ? "Long" : "long";
            case 8:
               return box ? "Double" : "double";
            case 9:
               return getTypeName(getElementType(type), box, fullyQualified) + arraySuffix(type);
            case 10:
               String typeName = getClassName(type);
               if (!fullyQualified) {
                  typeName = typeName.substring(typeName.lastIndexOf(46) + 1);
               }

               return typeName;
            default:
               return "Object";
         }
      }
   }

   private static Type getElementType(Type type) {
      try {
         return type.getElementType();
      } catch (Exception var2) {
         return Type.getObjectType("InvalidType");
      }
   }

   private static String getClassName(Type type) {
      try {
         return type.getClassName();
      } catch (Exception var2) {
         return "InvalidType";
      }
   }

   private static String arraySuffix(Type type) {
      return Strings.repeat("[]", type.getDimensions());
   }

   private static StringBuilder appendArraySuffix(StringBuilder sb, Type type) {
      for(int i = 0; i < type.getDimensions(); ++i) {
         sb.append("[]");
      }

      return sb;
   }
}
