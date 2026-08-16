package org.spongepowered.asm.mixin.transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.stream.Collectors;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.util.Bytecode;

final class EnumInfo implements Comparable<EnumInfo> {
   private final ClassContext enumClass;
   private final ClassContext targetClass;
   private final String description;
   private final List<FieldNode> selfTypedFields;
   private final List<FieldNode> constants;
   private final Set<String> constantNames;
   private final FieldNode valuesField;
   private final MethodNode clinit;
   private final FieldInsnNode valuesAssignment;
   private final String tieBreakString;

   private EnumInfo(ClassContext enumClass, ClassContext targetClass, String description) throws AssumptionViolatedException {
      this.enumClass = enumClass;
      this.targetClass = targetClass;
      this.description = description;
      this.clinit = this.findClinit();
      this.selfTypedFields = this.findSelfTypedFields();
      this.constants = this.findEnumConstants();
      this.constantNames = (Set)this.constants.stream().map((it) -> it.name).collect(Collectors.toSet());
      this.valuesField = this.findValuesField();
      this.valuesAssignment = this.findValuesAssignment();
      this.tieBreakString = this.constants.isEmpty() ? "" : ((FieldNode)this.constants.get(0)).name;
   }

   public static EnumInfo forTarget(TargetClassContext target) throws AssumptionViolatedException {
      return new EnumInfo(target, target, "target class");
   }

   public static EnumInfo forMixin(MixinTargetContext mixin) throws InvalidMixinException {
      try {
         return new EnumInfo(mixin, mixin.getTarget(), "mixin class");
      } catch (AssumptionViolatedException ex) {
         throw new InvalidMixinException(mixin, ex);
      }
   }

   public List<FieldNode> getSelfTypedFields() {
      return this.selfTypedFields;
   }

   public List<FieldNode> getConstants() {
      return this.constants;
   }

   public Set<String> getConstantNames() {
      return this.constantNames;
   }

   public FieldInsnNode getValuesAssignment() {
      return this.valuesAssignment;
   }

   public MethodNode getClinit() {
      return this.clinit;
   }

   public boolean isEnumConstantAssignment(AbstractInsnNode insn) {
      if (!(insn instanceof FieldInsnNode)) {
         return false;
      } else {
         FieldInsnNode fieldInsn = (FieldInsnNode)insn;
         return this.constantNames.contains(fieldInsn.name) && this.isAssignmentToOurStaticField(insn, fieldInsn.name, false);
      }
   }

   public int compareTo(EnumInfo other) {
      return this.tieBreakString.compareTo(other.tieBreakString);
   }

   private MethodNode findClinit() throws AssumptionViolatedException {
      MethodNode clinit = Bytecode.findMethod(this.enumClass.getClassNode(), "<clinit>", "()V");
      if (clinit == null) {
         throw assumptionViolated("Failed to find <clinit> in %s", this.description);
      } else {
         return clinit;
      }
   }

   private FieldNode findValuesField() throws AssumptionViolatedException {
      List<FieldNode> candidates = new ArrayList();

      for(FieldNode field : this.enumClass.getClassNode().fields) {
         if (Bytecode.isEnumValuesArray(field, this.targetClass.getClassNode())) {
            candidates.add(field);
         }
      }

      if (candidates.size() != 1) {
         throw assumptionViolated("Failed to determine enum values array in %s, candidates: %s", this.description, candidates.stream().map((it) -> it.name).collect(Collectors.toList()));
      } else {
         return (FieldNode)candidates.get(0);
      }
   }

   private FieldInsnNode findValuesAssignment() throws AssumptionViolatedException {
      FieldInsnNode candidate = null;
      ListIterator var2 = this.clinit.instructions.iterator();

      while(var2.hasNext()) {
         AbstractInsnNode insn = (AbstractInsnNode)var2.next();
         if (this.isAssignmentToOurStaticField(insn, this.valuesField.name, true)) {
            if (candidate != null) {
               throw assumptionViolated("Duplicate enum values assignment in %s (%s)", this.description, this.valuesField.name);
            }

            candidate = (FieldInsnNode)insn;
         }
      }

      return candidate;
   }

   private List<FieldNode> findSelfTypedFields() {
      String selfDesc = 'L' + this.targetClass.getClassRef() + ';';
      List<FieldNode> result = new ArrayList();

      for(FieldNode field : this.enumClass.getClassNode().fields) {
         if (field.desc.equals(selfDesc) && !isStubEnumConstant(field)) {
            result.add(field);
         }
      }

      return result;
   }

   private List<FieldNode> findEnumConstants() {
      List<FieldNode> result = new ArrayList();

      for(FieldNode field : this.selfTypedFields) {
         if (Bytecode.isEnumConstant(field, this.targetClass.getClassNode())) {
            result.add(field);
         }
      }

      return result;
   }

   private boolean isAssignmentToOurStaticField(AbstractInsnNode insn, String fieldName, boolean isArray) {
      if (insn.getOpcode() != 179) {
         return false;
      } else {
         FieldInsnNode fieldInsn = (FieldInsnNode)insn;

         for(ClassContext context : new ClassContext[]{this.enumClass, this.targetClass}) {
            if (fieldInsn.owner.equals(context.getClassRef()) && fieldInsn.name.equals(fieldName) && fieldInsn.desc.equals((isArray ? "[L" : "L") + context.getClassRef() + ';')) {
               return true;
            }
         }

         return false;
      }
   }

   private static boolean isStubEnumConstant(FieldNode field) {
      return field.attrs != null && field.attrs.stream().anyMatch((attr) -> attr.type.equals("org.spongepowered.asm.mixin.StubEnumConstant"));
   }

   private static AssumptionViolatedException assumptionViolated(String template, Object... args) {
      return new AssumptionViolatedException(String.format(template, args));
   }

   public static final class AssumptionViolatedException extends Exception {
      private AssumptionViolatedException(String message) {
         super(message);
      }
   }
}
