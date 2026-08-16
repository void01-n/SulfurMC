package org.spongepowered.asm.mixin.injection.modify;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.Locals;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.asm.util.SignaturePrinter;

public class LocalVariableDiscriminator {
   private final boolean argsOnly;
   private final int ordinal;
   private final int index;
   private final Set<String> names;
   private final boolean print;

   public LocalVariableDiscriminator(boolean argsOnly, int ordinal, int index, Set<String> names, boolean print) {
      this.argsOnly = argsOnly;
      this.ordinal = ordinal;
      this.index = index;
      this.names = Collections.unmodifiableSet(names);
      this.print = print;
   }

   public boolean isArgsOnly() {
      return this.argsOnly;
   }

   public int getOrdinal() {
      return this.ordinal;
   }

   public int getIndex() {
      return this.index;
   }

   public Set<String> getNames() {
      return this.names;
   }

   public boolean hasNames() {
      return !this.names.isEmpty();
   }

   public boolean printLVT() {
      return this.print;
   }

   public String toString() {
      return String.format("ordinal=%d index=%d", this.ordinal, this.index);
   }

   public String toString(Context context) {
      String typeName = SignaturePrinter.getTypeName(context.returnType, false, false);
      return this.isImplicit(context) ? "implicit " + typeName : String.format("explicit %s at ordinal=%d index=%d", typeName, this.ordinal, this.index);
   }

   protected boolean isImplicit(Context context) {
      return this.ordinal < 0 && this.index < context.baseArgIndex && this.names.isEmpty();
   }

   public int findLocal(Context context) {
      return this.isImplicit(context) ? this.findImplicitLocal(context) : this.findExplicitLocal(context);
   }

   private int findImplicitLocal(Context context) {
      int found = 0;
      int count = 0;

      for(int index = context.baseArgIndex; index < context.locals.length; ++index) {
         Context.Local local = context.locals[index];
         if (local != null && local.type.equals(context.returnType)) {
            ++count;
            found = index;
         }
      }

      if (count == 1) {
         return found;
      } else {
         throw new InvalidImplicitDiscriminatorException("Found " + count + " candidate variables but exactly 1 is required.");
      }
   }

   private int findExplicitLocal(Context context) {
      for(int index = context.baseArgIndex; index < context.locals.length; ++index) {
         Context.Local local = context.locals[index];
         if (local != null && local.type.equals(context.returnType)) {
            if (this.ordinal > -1) {
               if (this.ordinal == local.getOrdinal()) {
                  return index;
               }
            } else if (this.index >= context.baseArgIndex) {
               if (this.index == index) {
                  return index;
               }
            } else if (this.names.contains(local.name)) {
               return index;
            }
         }
      }

      return -1;
   }

   public static LocalVariableDiscriminator parse(AnnotationNode annotation) {
      boolean argsOnly = (Boolean)Annotations.getValue(annotation, "argsOnly", Boolean.FALSE);
      int ordinal = (Integer)Annotations.getValue(annotation, "ordinal", -1);
      int index = (Integer)Annotations.getValue(annotation, "index", -1);
      boolean print = (Boolean)Annotations.getValue(annotation, "print", Boolean.FALSE);
      Set<String> names = new HashSet();
      List<String> namesList = (List)Annotations.getValue(annotation, "name", (List)null);
      if (namesList != null) {
         names.addAll(namesList);
      }

      return new LocalVariableDiscriminator(argsOnly, ordinal, index, names, print);
   }

   public static class Context implements PrettyPrinter.IPrettyPrintable {
      final InjectionInfo info;
      final Target target;
      final Type returnType;
      final AbstractInsnNode node;
      final int baseArgIndex;
      final Local[] locals;
      private final boolean isStatic;

      public Context(InjectionInfo info, Type returnType, boolean argsOnly, Target target, AbstractInsnNode node) {
         this.info = info;
         this.isStatic = Bytecode.isStatic(target.method);
         this.returnType = returnType;
         this.target = target;
         this.node = node;
         this.baseArgIndex = this.isStatic ? 0 : 1;
         this.locals = this.initLocals(target, argsOnly, node);
         this.initOrdinals();
      }

      private Local[] initLocals(Target target, boolean argsOnly, AbstractInsnNode node) {
         if (!argsOnly) {
            LocalVariableNode[] locals = Locals.getLocalsAt(target.classNode, target.method, node, FabricUtil.getCompatibility((ISelectorContext)this.info));
            if (locals != null) {
               return this.getLocals(locals);
            }
         }

         int fabricCompatibility = FabricUtil.getCompatibility((ISelectorContext)this.info);
         boolean fallbackToLvIndex = fabricCompatibility < 17000;
         LocalVariableNode[] initialLocals = Locals.getInitialMethodLocals(target.method, target.classNode, fabricCompatibility, fallbackToLvIndex);
         return this.getLocals(initialLocals);
      }

      private Local[] getLocals(LocalVariableNode[] initialLocals) {
         Local[] lvt = new Local[initialLocals.length];

         for(int l = 0; l < initialLocals.length; ++l) {
            if (initialLocals[l] != null) {
               lvt[l] = new Local(initialLocals[l].name, Type.getType(initialLocals[l].desc));
            }
         }

         return lvt;
      }

      private void initOrdinals() {
         Map<Type, Integer> ordinalMap = new HashMap();

         for(int l = 0; l < this.locals.length; ++l) {
            Integer ordinal = 0;
            if (this.locals[l] != null) {
               ordinal = (Integer)ordinalMap.get(this.locals[l].type);
               Integer var5;
               ordinalMap.put(this.locals[l].type, var5 = ordinal == null ? 0 : ordinal + 1);
               this.locals[l].setOrdinal(var5);
            }
         }

      }

      public int getCandidateCount() {
         int candidateCount = 0;

         for(int l = this.baseArgIndex; l < this.locals.length; ++l) {
            if (this.locals[l] != null && this.returnType.equals(this.locals[l].type)) {
               ++candidateCount;
            }
         }

         return candidateCount;
      }

      public void print(PrettyPrinter printer) {
         printer.add("%5s  %7s  %30s  %-50s  %s", "INDEX", "ORDINAL", "TYPE", "NAME", "CANDIDATE");

         for(int l = this.baseArgIndex; l < this.locals.length; ++l) {
            Local local = this.locals[l];
            if (local != null) {
               Type localType = local.type;
               String localName = local.name;
               int ordinal = local.getOrdinal();
               String candidate = this.returnType.equals(localType) ? "YES" : "-";
               printer.add("[%3d]    [%3d]  %30s  %-50s  %s", l, ordinal, SignaturePrinter.getTypeName(localType, false), localName, candidate);
            } else if (l > 0) {
               Local prevLocal = this.locals[l - 1];
               boolean isTop = prevLocal != null && prevLocal.type != null && prevLocal.type.getSize() > 1;
               printer.add("[%3d]           %30s", l, isTop ? "<top>" : "-");
            }
         }

      }

      public class Local {
         private int ord = -1;
         final String name;
         final Type type;

         public Local(String name, Type type) {
            this.name = name;
            this.type = type;
         }

         public String toString() {
            return String.format("Local[ordinal=%d, name=%s, type=%s]", this.ord, this.name, this.type);
         }

         void setOrdinal(int ordinal) {
            if (this.ord > -1 && this.ord != ordinal) {
               throw new IllegalStateException("Attempted to reset ordinal for computed local");
            } else {
               this.ord = ordinal;
            }
         }

         int getOrdinal() {
            return this.ord;
         }
      }
   }
}
