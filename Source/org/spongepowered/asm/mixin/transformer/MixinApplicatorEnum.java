package org.spongepowered.asm.mixin.transformer;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.spongepowered.asm.mixin.MixinIntrinsics;
import org.spongepowered.asm.mixin.extensibility.IActivityContext;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.transformer.struct.Clinit;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.include.com.google.common.collect.ArrayListMultimap;
import org.spongepowered.include.com.google.common.collect.Multimap;

class MixinApplicatorEnum extends MixinApplicatorStandard {
   private final Map<IMixinInfo, EnumInfo> extensionInfos = new HashMap();
   private EnumInfo targetInfo;
   private int ordinalShift;
   private FieldNode insertionPoint;

   MixinApplicatorEnum(TargetClassContext context) {
      super(context);
   }

   protected void preApply(IActivityContext.IActivity activity, List<MixinTargetContext> mixins) throws Exception {
      this.gatherEnumExtensions(mixins);
      this.sortEnumExtensions(mixins);
      super.preApply(activity, mixins);
      if (!this.extensionInfos.isEmpty()) {
         this.prepareTargetInfo(mixins);
         this.checkUniqueEnumConstants(mixins);
         this.permitEnumSubclasses(mixins);
         this.replaceValueOf();
      }

   }

   private void gatherEnumExtensions(List<MixinTargetContext> mixins) {
      Iterator<MixinTargetContext> iter = mixins.iterator();

      while(iter.hasNext()) {
         MixinTargetContext mixin = (MixinTargetContext)iter.next();
         if (mixin.getClassInfo().isEnum()) {
            try {
               this.extensionInfos.put(mixin.getInfo(), EnumInfo.forMixin(mixin));
            } catch (InvalidMixinException ex) {
               if (mixin.isRequired()) {
                  throw ex;
               }

               this.context.addSuppressed(ex);
               iter.remove();
            }
         }
      }

   }

   private void sortEnumExtensions(List<MixinTargetContext> mixins) {
      Comparator<MixinTargetContext> byEnumExtension = Comparator.comparing((mixin) -> (EnumInfo)this.extensionInfos.get(mixin.getInfo()), Comparator.nullsLast(Comparator.naturalOrder()));
      mixins.sort(Comparator.comparing(MixinTargetContext::getPriority).thenComparing(byEnumExtension));
   }

   private void prepareTargetInfo(List<MixinTargetContext> mixins) {
      try {
         this.targetInfo = EnumInfo.forTarget(this.context);
         List<FieldNode> targetConstants = this.targetInfo.getConstants();
         this.ordinalShift = targetConstants.size();
         if (!targetConstants.isEmpty()) {
            this.insertionPoint = (FieldNode)targetConstants.get(targetConstants.size() - 1);
         }
      } catch (EnumInfo.AssumptionViolatedException var6) {
         EnumInfo.AssumptionViolatedException e = var6;
         Iterator<MixinTargetContext> iter = mixins.iterator();

         while(iter.hasNext()) {
            MixinTargetContext mixin = (MixinTargetContext)iter.next();
            if (mixin.getClassInfo().isEnum()) {
               InvalidMixinException wrapped = new InvalidMixinException(mixin, e);
               if (mixin.isRequired()) {
                  throw wrapped;
               }

               this.context.addSuppressed(wrapped);
               iter.remove();
            }
         }
      }

   }

   private void checkUniqueEnumConstants(List<MixinTargetContext> mixins) {
      Multimap<String, MixinTargetContext> existingSources = ArrayListMultimap.<String, MixinTargetContext>create();

      for(FieldNode field : this.targetInfo.getSelfTypedFields()) {
         existingSources.put(field.name, (Object)null);
      }

      for(MixinTargetContext mixin : mixins) {
         for(FieldNode field : mixin.getFields()) {
            if (field.desc.equals('L' + this.targetClass.name + ';')) {
               existingSources.put(field.name, mixin);
            }
         }
      }

      Iterator<MixinTargetContext> iter = mixins.iterator();

      while(iter.hasNext()) {
         MixinTargetContext mixin = (MixinTargetContext)iter.next();
         EnumInfo extension = (EnumInfo)this.extensionInfos.get(mixin.getInfo());
         if (extension != null) {
            for(FieldNode constant : extension.getConstants()) {
               Collection<MixinTargetContext> sources = existingSources.get(constant.name);
               if (sources.size() >= 2) {
                  List<String> others = (List)sources.stream().filter((it) -> it != mixin).map((it) -> it == null ? "target class" : it.toString()).collect(Collectors.toList());
                  InvalidMixinException e = new InvalidMixinException(mixin, String.format("Added enum constant %s conflicts with field declared in %s", constant.name, others));
                  if (mixin.isRequired()) {
                     throw e;
                  }

                  this.context.addSuppressed(e);
                  iter.remove();
                  this.extensionInfos.remove(mixin.getInfo());
                  break;
               }
            }
         }
      }

   }

   private void permitEnumSubclasses(List<MixinTargetContext> mixins) {
      boolean isSealed;
      if (Bytecode.hasFlag((ClassNode)this.targetClass, 16)) {
         isSealed = false;
      } else {
         if (this.targetClass.permittedSubclasses == null || this.targetClass.permittedSubclasses.isEmpty()) {
            return;
         }

         isSealed = true;
      }

      for(MixinTargetContext mixin : mixins) {
         if (mixin.getClassInfo().isEnum() && !mixin.getClassInfo().isFinal()) {
            if (!isSealed) {
               ClassNode var10000 = this.targetClass;
               var10000.access &= -17;
               return;
            }

            for(Map.Entry<String, String> names : mixin.getInnerClasses().entrySet()) {
               ClassInfo innerClass = ClassInfo.forName((String)names.getKey());
               if (innerClass.getSuperName().equals(mixin.getClassRef())) {
                  this.targetClass.permittedSubclasses.add((String)names.getValue());
               }
            }
         }
      }

   }

   private void replaceValueOf() {
      String valueOfDesc = "(Ljava/lang/String;)L" + this.targetClass.name + ';';
      MethodNode existing = Bytecode.findMethod(this.targetClass, "valueOf", valueOfDesc);
      this.targetClass.methods.remove(existing);
      MethodNode valueOf = new MethodNode(9, "valueOf", valueOfDesc, (String)null, (String[])null);
      this.targetClass.methods.add(valueOf);
      valueOf.visitLdcInsn(Type.getObjectType(this.targetClass.name));
      valueOf.visitVarInsn(25, 0);
      valueOf.visitMethodInsn(184, Type.getInternalName(Enum.class), "valueOf", "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;", false);
      valueOf.visitTypeInsn(192, this.targetClass.name);
      valueOf.visitInsn(176);
      valueOf.visitMaxs(2, 1);
   }

   protected void applyNormalMethod(MixinTargetContext mixin, MethodNode mixinMethod) {
      if (mixin.getClassInfo().isEnum() && "<init>".equals(mixinMethod.name) && Bytecode.hasFlag((MethodNode)mixinMethod, 4096)) {
         mixin.transformMethod(mixinMethod);
         super.mergeMethod(mixin, mixinMethod);
      } else {
         super.applyNormalMethod(mixin, mixinMethod);
      }
   }

   protected void mergeNormalField(MixinTargetContext mixin, FieldNode field, int index) {
      if (mixin.getClassInfo().isEnum()) {
         if (Bytecode.isEnumConstant(field, this.targetClass)) {
            return;
         }

         if (Bytecode.isEnumValuesArray(field, this.targetClass)) {
            return;
         }
      }

      super.mergeNormalField(mixin, field, index);
   }

   protected void applyFields(MixinTargetContext mixin) {
      super.applyFields(mixin);
      EnumInfo extension = (EnumInfo)this.extensionInfos.get(mixin.getInfo());
      if (extension != null) {
         this.applyEnumFields(extension, mixin);
      }

   }

   private void applyEnumFields(EnumInfo extension, MixinTargetContext mixin) {
      this.targetClass.fields.removeAll((Collection)extension.getConstants().stream().map(this::findTargetField).filter(Objects::nonNull).collect(Collectors.toSet()));
      List<FieldNode> targetFields = this.targetClass.fields;
      int insertionIndex = this.insertionPoint == null ? 0 : targetFields.lastIndexOf(this.insertionPoint) + 1;

      for(FieldNode constant : extension.getConstants()) {
         super.mergeNormalField(mixin, constant, insertionIndex++);
         this.insertionPoint = constant;
      }

   }

   protected Clinit prepareOrCreateClinit() {
      return (Clinit)(this.targetInfo == null ? super.prepareOrCreateClinit() : new EnumClinit());
   }

   private class EnumClinit extends Clinit {
      public EnumClinit() {
         this(MixinApplicatorEnum.this.context.getTargetMethod(MixinApplicatorEnum.this.targetInfo.getClinit()));
      }

      private EnumClinit(Target clinit) {
         super(clinit.method, Clinit.prepareClinit(clinit.method, clinit));
      }

      protected void appendInsns(IMixinInfo mixinInfo, MethodNode mixinClinit, Map<LabelNode, LabelNode> labels) {
         EnumInfo extension = (EnumInfo)MixinApplicatorEnum.this.extensionInfos.get(mixinInfo);
         if (extension == null) {
            super.appendInsns(mixinInfo, mixinClinit, labels);
         } else {
            this.spliceEnumClinit(mixinInfo, extension, labels);
         }
      }

      private void spliceEnumClinit(IMixinInfo mixinInfo, EnumInfo extension, Map<LabelNode, LabelNode> labels) {
         Set<String> remainingConstants = new HashSet(extension.getConstantNames());
         InsnList dest = this.clinit.instructions;
         AbstractInsnNode insertPoint = MixinApplicatorEnum.this.targetInfo.getValuesAssignment();
         InsnList insns = extension.getClinit().instructions;
         Integer currentOrdinal = null;

         for(AbstractInsnNode insn = insns.getFirst(); insn != extension.getValuesAssignment(); insn = insn.getNext()) {
            if (currentOrdinal == null) {
               if (!remainingConstants.isEmpty()) {
                  Object ordinal = Bytecode.getConstant(insn);
                  if (ordinal instanceof Integer) {
                     currentOrdinal = (Integer)ordinal + MixinApplicatorEnum.this.ordinalShift;
                     dest.insertBefore(insertPoint, Bytecode.loadIntConstant(currentOrdinal));
                     continue;
                  }
               }
            } else if (this.isCurrentOrdinalCall(insn)) {
               dest.insertBefore(insertPoint, Bytecode.loadIntConstant(currentOrdinal));
               continue;
            }

            dest.insertBefore(insertPoint, insn.clone(labels));
            if (extension.isEnumConstantAssignment(insn)) {
               String constantName = ((FieldInsnNode)insn).name;
               if (!remainingConstants.remove(constantName)) {
                  throw new InvalidMixinException(mixinInfo, "Duplicate assignment to enum constant " + constantName);
               }

               currentOrdinal = null;
            }
         }

         if (!remainingConstants.isEmpty()) {
            throw new InvalidMixinException(mixinInfo, "Enum constants not assigned: " + remainingConstants);
         } else {
            insns.insertBefore(insertPoint, this.concatEnumValues());

            for(AbstractInsnNode insn = extension.getValuesAssignment().getNext(); insn != null; insn = insn.getNext()) {
               if (insn.getOpcode() != 177) {
                  dest.insertBefore(this.finalReturn, insn.clone(labels));
               }
            }

            MixinApplicatorEnum.this.ordinalShift = extension.getConstants().size();
         }
      }

      private InsnList concatEnumValues() {
         InsnList result = new InsnList();
         result.add((AbstractInsnNode)(new MethodInsnNode(184, Type.getInternalName(MixinHooks.class), "concatEnumValues", "([Ljava/lang/Enum;[Ljava/lang/Enum;)[Ljava/lang/Enum;", false)));
         result.add((AbstractInsnNode)(new TypeInsnNode(192, "[L" + MixinApplicatorEnum.this.targetClass.name + ';')));
         return result;
      }

      private boolean isCurrentOrdinalCall(AbstractInsnNode insn) {
         if (insn.getOpcode() != 184) {
            return false;
         } else {
            MethodInsnNode call = (MethodInsnNode)insn;
            return call.owner.equals(Type.getInternalName(MixinIntrinsics.class)) && call.name.equals("currentEnumOrdinal");
         }
      }
   }
}
