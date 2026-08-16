package org.spongepowered.asm.mixin.transformer;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.Level;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.SoftOverride;
import org.spongepowered.asm.mixin.extensibility.IActivityContext;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.gen.AccessorInfo;
import org.spongepowered.asm.mixin.injection.struct.Constructor;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectorGroupInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InjectionValidationException;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.refmap.IReferenceMapper;
import org.spongepowered.asm.mixin.struct.MemberRef;
import org.spongepowered.asm.mixin.struct.SourceMap;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;
import org.spongepowered.asm.mixin.transformer.struct.Initialiser;
import org.spongepowered.asm.mixin.transformer.struct.InsnRange;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.mixin.transformer.throwables.MixinTransformerError;
import org.spongepowered.asm.obfuscation.RemapperChain;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.ClassSignature;
import org.spongepowered.asm.util.asm.ASM;
import org.spongepowered.include.com.google.common.collect.BiMap;

public class MixinTargetContext extends ClassContext implements IMixinContext {
   private static final ILogger logger = MixinService.getService().getLogger("mixin");
   protected final ActivityStack activities = new ActivityStack((String)null);
   private final MixinInfo mixin;
   private final ClassNode classNode;
   private final TargetClassContext targetClass;
   private final String sessionId;
   private final ClassInfo targetClassInfo;
   private final BiMap<String, String> innerClasses;
   private final List<MethodNode> shadowMethods = new ArrayList();
   private final Map<FieldNode, ClassInfo.Field> shadowFields = new LinkedHashMap();
   private final List<MethodNode> mergedMethods = new ArrayList();
   private final InjectorGroupInfo.Map injectorGroups = new InjectorGroupInfo.Map();
   private final List<InjectionInfo> injectors = new ArrayList();
   private final List<AccessorInfo> accessors = new ArrayList();
   private final boolean inheritsFromMixin;
   private final boolean detachedSuper;
   private final SourceMap.File stratum;
   private int minRequiredClassVersion;
   private Initialiser initialiser;

   MixinTargetContext(MixinInfo mixin, ClassNode classNode, TargetClassContext context) {
      this.minRequiredClassVersion = MixinEnvironment.CompatibilityLevel.JAVA_6.getClassVersion();
      this.mixin = mixin;
      this.classNode = classNode;
      this.targetClass = context;
      this.targetClassInfo = context.getClassInfo();
      this.stratum = context.getSourceMap().addFile(this.classNode);
      this.inheritsFromMixin = mixin.getClassInfo().hasMixinInHierarchy() || this.targetClassInfo.hasMixinTargetInHierarchy();
      this.detachedSuper = !this.classNode.superName.equals(this.getTarget().getClassNode().superName);
      this.sessionId = context.getSessionId();
      this.requireVersion(classNode.version);
      InnerClassGenerator icg = (InnerClassGenerator)context.getExtensions().getGenerator(InnerClassGenerator.class);
      this.innerClasses = icg.getInnerClasses(this.mixin, this.getTargetClassRef());
      this.initialiser = this.findInitialiser();
   }

   void addShadowMethod(MethodNode method) {
      this.shadowMethods.add(method);
   }

   void addShadowField(FieldNode fieldNode, ClassInfo.Field fieldInfo) {
      this.shadowFields.put(fieldNode, fieldInfo);
   }

   void addAccessorMethod(MethodNode method, Class<? extends Annotation> type) {
      this.accessors.add(AccessorInfo.of(this, method, type));
   }

   void addMixinMethod(MethodNode method) {
      Annotations.setVisible(method, MixinMerged.class, "mixin", this.getClassName());
      this.getTarget().addMixinMethod(method);
   }

   void methodMerged(MethodNode method) {
      this.mergedMethods.add(method);
      this.targetClassInfo.addMethod(method);
      this.getTarget().methodMerged(method);
      Annotations.setVisible(method, MixinMerged.class, "mixin", this.getClassName(), "priority", this.getPriority(), "sessionId", this.sessionId);
   }

   void addMixinField(FieldNode field) {
      Annotations.setVisible(field, MixinMerged.class, "mixin", this.getClassName());
      this.getTarget().addMixinField(field);
   }

   void fieldMerged(FieldNode field) {
      this.getTarget().fieldMerged(field);
      Annotations.setVisible(field, MixinMerged.class, "mixin", this.getClassName(), "priority", this.getPriority(), "sessionId", this.sessionId);
   }

   public String toString() {
      return this.mixin.toString();
   }

   public MixinEnvironment getEnvironment() {
      return this.mixin.getParent().getEnvironment();
   }

   public boolean getOption(MixinEnvironment.Option option) {
      return this.getEnvironment().getOption(option);
   }

   public ClassNode getClassNode() {
      return this.classNode;
   }

   public String getClassName() {
      return this.mixin.getClassName();
   }

   public String getClassRef() {
      return this.mixin.getClassRef();
   }

   public TargetClassContext getTarget() {
      return this.targetClass;
   }

   public String getTargetClassName() {
      return this.getTarget().getClassName();
   }

   public String getTargetClassRef() {
      return this.getTarget().getClassRef();
   }

   public ClassNode getTargetClassNode() {
      return this.getTarget().getClassNode();
   }

   public ClassInfo getTargetClassInfo() {
      return this.targetClassInfo;
   }

   public ClassInfo getClassInfo() {
      return this.mixin.getClassInfo();
   }

   public ClassSignature getSignature() {
      return this.getClassInfo().getSignature();
   }

   public SourceMap.File getStratum() {
      return this.stratum;
   }

   public int getMinRequiredClassVersion() {
      return this.minRequiredClassVersion;
   }

   public int getDefaultRequiredInjections() {
      return this.mixin.getParent().getDefaultRequiredInjections();
   }

   public String getDefaultInjectorGroup() {
      return this.mixin.getParent().getDefaultInjectorGroup();
   }

   public int getMaxShiftByValue() {
      return this.mixin.getParent().getMaxShiftByValue();
   }

   public InjectorGroupInfo.Map getInjectorGroups() {
      return this.injectorGroups;
   }

   public boolean requireOverwriteAnnotations() {
      return this.mixin.getParent().requireOverwriteAnnotations();
   }

   void transformMethod(MethodNode method) {
      this.activities.clear();

      try {
         IActivityContext.IActivity activity = this.activities.begin("Validate");
         this.validateMethod(method);
         activity.next("Transform Descriptor");
         this.transformDescriptor(method);
         activity.next("Transform LVT");
         this.transformLVT(method);
         activity.next("Transform Line Numbers");
         this.stratum.applyOffset(method);
         activity.next("Transform Instructions");
         AbstractInsnNode lastInsn = null;
         Iterator<AbstractInsnNode> iter = method.instructions.iterator();

         while(iter.hasNext()) {
            AbstractInsnNode insn = (AbstractInsnNode)iter.next();
            IActivityContext.IActivity insnActivity = this.activities.begin(Bytecode.getOpcodeName(insn) + " ");
            if (insn instanceof MethodInsnNode) {
               MethodInsnNode methodNode = (MethodInsnNode)insn;
               insnActivity.append("%s::%s%s", methodNode.owner, methodNode.name, methodNode.desc);
               this.transformMethodRef(method, iter, new MemberRef.Method(methodNode));
            } else if (insn instanceof FieldInsnNode) {
               FieldInsnNode fieldNode = (FieldInsnNode)insn;
               insnActivity.append("%s::%s:%s", fieldNode.owner, fieldNode.name, fieldNode.desc);
               this.transformFieldRef(method, iter, new MemberRef.Field(fieldNode));
               this.checkFinal(method, iter, fieldNode);
            } else if (insn instanceof TypeInsnNode) {
               TypeInsnNode typeNode = (TypeInsnNode)insn;
               insnActivity.append(typeNode.desc);
               this.transformTypeNode(method, iter, typeNode, lastInsn);
            } else if (insn instanceof LdcInsnNode) {
               this.transformConstantNode(method, iter, (LdcInsnNode)insn);
            } else if (insn instanceof InvokeDynamicInsnNode) {
               InvokeDynamicInsnNode invokeNode = (InvokeDynamicInsnNode)insn;
               insnActivity.append("%s %s", invokeNode.name, invokeNode.desc);
               this.transformInvokeDynamicNode(method, iter, invokeNode);
            }

            lastInsn = insn;
            insnActivity.end();
         }

         activity.end();
      } catch (InvalidMixinException ex) {
         ex.prepend(this.activities);
         throw ex;
      } catch (Exception ex) {
         throw new InvalidMixinException(this, "Unexpecteded " + ex.getClass().getSimpleName() + " whilst transforming the mixin class:", ex, this.activities);
      }
   }

   private void validateMethod(MethodNode method) {
      if (Annotations.getInvisible(method, SoftOverride.class) != null) {
         if (Bytecode.getVisibility(method) == Bytecode.Visibility.PRIVATE) {
            throw new InvalidMixinException(this, "Mixin method " + method.name + method.desc + " is tagged with @SoftOverride but the method is PRIVATE");
         }

         ClassInfo.Method superMethod = this.targetClassInfo.findMethodInHierarchy(method.name, method.desc, ClassInfo.SearchType.SUPER_CLASSES_ONLY, ClassInfo.Traversal.SUPER);
         if (superMethod == null || !superMethod.isInjected()) {
            throw new InvalidMixinException(this, "Mixin method " + method.name + method.desc + " is tagged with @SoftOverride but no valid method was found in superclasses of " + this.getTarget().getClassName());
         }
      }

      if (this.getEnvironment().getOption(MixinEnvironment.Option.DEBUG_VERIFY) && Bytecode.isVirtual(method)) {
         ClassInfo.Method superMethod = this.targetClassInfo.findMethodInHierarchy((MethodNode)method, (ClassInfo.SearchType)ClassInfo.SearchType.SUPER_CLASSES_ONLY, (ClassInfo.Traversal)ClassInfo.Traversal.ALL, 0);
         if (superMethod != null && superMethod.isFinal()) {
            throw new InvalidMixinException(this.mixin, String.format("%s%s in %s overrides a final method from %s", method.name, method.desc, this.mixin, superMethod.getOwner().getClassName()));
         }
      }

   }

   private void transformLVT(MethodNode method) {
      if (method.localVariables != null) {
         IActivityContext.IActivity localVarActivity = this.activities.begin("?");

         for(LocalVariableNode local : method.localVariables) {
            if (local != null && local.desc != null) {
               localVarActivity.next("var=%s", local.name);
               local.desc = this.transformSingleDescriptor(Type.getType(local.desc));
               local.signature = null;
            }
         }

         localVarActivity.end();
      }
   }

   private void transformMethodRef(MethodNode method, Iterator<AbstractInsnNode> iter, MemberRef methodRef) {
      this.transformDescriptor(methodRef);
      if (methodRef.getOwner().equals(this.getClassRef())) {
         methodRef.setOwner(this.getTarget().getClassRef());
         ClassInfo.Method md = this.getClassInfo().findMethod(methodRef.getName(), methodRef.getDesc(), 10);
         if (md != null && md.isRenamed() && md.getOriginalName().equals(methodRef.getName()) && (md.isSynthetic() || md.isConformed())) {
            methodRef.setName(md.getName());
         }

         this.upgradeMethodRef(method, methodRef, md);
      } else if (this.innerClasses.containsKey(methodRef.getOwner())) {
         methodRef.setOwner((String)this.innerClasses.get(methodRef.getOwner()));
         methodRef.setDesc(this.transformMethodDescriptor(methodRef.getDesc()));
      } else if (this.detachedSuper || this.inheritsFromMixin) {
         if (methodRef.getOpcode() == 183) {
            this.updateStaticBinding(method, methodRef);
         } else if (methodRef.getOpcode() == 182 && methodRef.ownerIsMixin()) {
            this.updateDynamicBinding(method, methodRef);
         }
      }

   }

   private void transformFieldRef(MethodNode method, Iterator<AbstractInsnNode> iter, MemberRef fieldRef) {
      if ("super$".equals(fieldRef.getName())) {
         if (!(fieldRef instanceof MemberRef.Field)) {
            throw new InvalidMixinException(this.mixin, "Cannot call imaginary super from method handle.");
         }

         this.processImaginarySuper(method, ((MemberRef.Field)fieldRef).insn);
         iter.remove();
      }

      this.transformDescriptor(fieldRef);
      if (fieldRef.getOwner().equals(this.getClassRef())) {
         fieldRef.setOwner(this.getTarget().getClassRef());
         ClassInfo.Field field = this.getClassInfo().findField(fieldRef.getName(), fieldRef.getDesc(), 10);
         if (field != null && field.isRenamed() && field.getOriginalName().equals(fieldRef.getName()) && field.isStatic()) {
            fieldRef.setName(field.getName());
         }
      } else if (this.innerClasses.containsKey(fieldRef.getOwner())) {
         fieldRef.setOwner((String)this.innerClasses.get(fieldRef.getOwner()));
      } else if (ClassInfo.isMixin(fieldRef.getOwner())) {
         ClassInfo fieldOwner = ClassInfo.forName(fieldRef.getOwner());
         ClassInfo actualOwner = this.targetClassInfo.findCorrespondingType(fieldOwner);
         fieldRef.setOwner(actualOwner != null ? actualOwner.getName() : this.getTarget().getClassRef());
      }

   }

   private void checkFinal(MethodNode method, Iterator<AbstractInsnNode> iter, FieldInsnNode fieldNode) {
      if (fieldNode.owner.equals(this.getTarget().getClassRef())) {
         int opcode = fieldNode.getOpcode();
         if (opcode != 180 && opcode != 178) {
            for(Map.Entry<FieldNode, ClassInfo.Field> shadow : this.shadowFields.entrySet()) {
               FieldNode shadowFieldNode = (FieldNode)shadow.getKey();
               if (shadowFieldNode.desc.equals(fieldNode.desc) && shadowFieldNode.name.equals(fieldNode.name)) {
                  ClassInfo.Field shadowField = (ClassInfo.Field)shadow.getValue();
                  if (shadowField.isDecoratedFinal()) {
                     if (shadowField.isDecoratedMutable()) {
                        if (this.mixin.getParent().getEnvironment().getOption(MixinEnvironment.Option.DEBUG_VERBOSE)) {
                           logger.warn("Write access to @Mutable @Final field {} in {}::{}", shadowField, this.mixin, method.name);
                        }
                     } else if (!"<init>".equals(method.name) && !"<clinit>".equals(method.name)) {
                        logger.error("Write access detected to @Final field {} in {}::{}", shadowField, this.mixin, method.name);
                        if (this.mixin.getParent().getEnvironment().getOption(MixinEnvironment.Option.DEBUG_VERIFY)) {
                           throw new InvalidMixinException(this.mixin, "Write access detected to @Final field " + shadowField + " in " + this.mixin + "::" + method.name);
                        }
                     } else {
                        logger.warn("@Final field {} in {} should be final", shadowField, this.mixin);
                     }
                  }

                  return;
               }
            }

         }
      }
   }

   private void transformTypeNode(MethodNode method, Iterator<AbstractInsnNode> iter, TypeInsnNode typeInsn, AbstractInsnNode lastNode) {
      if (typeInsn.getOpcode() == 192 && typeInsn.desc.equals(this.getTarget().getClassRef()) && lastNode.getOpcode() == 25 && ((VarInsnNode)lastNode).var == 0 && !Bytecode.isStatic(method)) {
         iter.remove();
      } else {
         if (typeInsn.desc.equals(this.getClassRef())) {
            typeInsn.desc = this.getTarget().getClassRef();
         } else {
            String newName = (String)this.innerClasses.get(typeInsn.desc);
            if (newName != null) {
               typeInsn.desc = newName;
            }
         }

         this.transformDescriptor(typeInsn);
      }
   }

   private void transformConstantNode(MethodNode method, Iterator<AbstractInsnNode> iter, LdcInsnNode ldcInsn) {
      ldcInsn.cst = this.transformConstant(method, iter, ldcInsn.cst);
   }

   private void transformInvokeDynamicNode(MethodNode method, Iterator<AbstractInsnNode> iter, InvokeDynamicInsnNode dynInsn) {
      this.requireVersion(51);
      dynInsn.desc = this.transformMethodDescriptor(dynInsn.desc);
      dynInsn.bsm = this.transformHandle(method, iter, dynInsn.bsm);

      for(int i = 0; i < dynInsn.bsmArgs.length; ++i) {
         dynInsn.bsmArgs[i] = this.transformConstant(method, iter, dynInsn.bsmArgs[i]);
      }

   }

   private Object transformConstant(MethodNode method, Iterator<AbstractInsnNode> iter, Object constant) {
      if (constant instanceof Type) {
         Type type = (Type)constant;
         String desc = this.transformDescriptor(type);
         return !type.toString().equals(desc) ? Type.getType(desc) : constant;
      } else if (constant instanceof Handle) {
         return this.transformHandle(method, iter, (Handle)constant);
      } else {
         return ASM.isAtLeastVersion(6) && constant instanceof ConstantDynamic ? this.transformDynamicConstant(method, iter, (ConstantDynamic)constant) : constant;
      }
   }

   private Handle transformHandle(MethodNode method, Iterator<AbstractInsnNode> iter, Handle handle) {
      MemberRef.Handle memberRef = new MemberRef.Handle(handle);
      if (memberRef.isField()) {
         this.transformFieldRef(method, iter, memberRef);
      } else {
         this.transformMethodRef(method, iter, memberRef);
      }

      return memberRef.getMethodHandle();
   }

   private ConstantDynamic transformDynamicConstant(MethodNode method, Iterator<AbstractInsnNode> iter, ConstantDynamic constant) {
      this.requireVersion(55);
      if (!MixinEnvironment.getCompatibilityLevel().supports(16)) {
         throw new InvalidMixinException(this, String.format("%s%s in %s contains a dynamic constant, which is not supported by the current compatibility level", method.name, method.desc, this));
      } else {
         String desc = this.transformSingleDescriptor(constant.getDescriptor(), false);
         Handle bsm = this.transformHandle(method, iter, constant.getBootstrapMethod());
         Object[] bsmArgs = new Object[constant.getBootstrapMethodArgumentCount()];

         for(int i = 0; i < bsmArgs.length; ++i) {
            bsmArgs[i] = this.transformConstant(method, iter, constant.getBootstrapMethodArgument(i));
         }

         return new ConstantDynamic(constant.getName(), desc, bsm, bsmArgs);
      }
   }

   private void processImaginarySuper(MethodNode method, FieldInsnNode fieldInsn) {
      if (fieldInsn.getOpcode() != 180) {
         if ("<init>".equals(method.name)) {
            throw new InvalidMixinException(this, "Illegal imaginary super declaration: field " + fieldInsn.name + " must not specify an initialiser");
         } else {
            throw new InvalidMixinException(this, "Illegal imaginary super access: found " + Bytecode.getOpcodeName(fieldInsn.getOpcode()) + " opcode in " + method.name + method.desc);
         }
      } else if ((method.access & 2) == 0 && (method.access & 8) == 0) {
         if (Annotations.getInvisible(method, SoftOverride.class) == null) {
            throw new InvalidMixinException(this, "Illegal imaginary super access: method " + method.name + method.desc + " is not decorated with @SoftOverride");
         } else {
            Iterator<AbstractInsnNode> methodIter = method.instructions.iterator(method.instructions.indexOf(fieldInsn));

            while(methodIter.hasNext()) {
               AbstractInsnNode insn = (AbstractInsnNode)methodIter.next();
               if (insn instanceof MethodInsnNode) {
                  MethodInsnNode methodNode = (MethodInsnNode)insn;
                  if (methodNode.owner.equals(this.getClassRef()) && methodNode.name.equals(method.name) && methodNode.desc.equals(method.desc)) {
                     methodNode.setOpcode(183);
                     this.updateStaticBinding(method, new MemberRef.Method(methodNode));
                     return;
                  }
               }
            }

            throw new InvalidMixinException(this, "Illegal imaginary super access: could not find INVOKE for " + method.name + method.desc);
         }
      } else {
         throw new InvalidMixinException(this, "Illegal imaginary super access: method " + method.name + method.desc + " is private or static");
      }
   }

   private void updateStaticBinding(MethodNode method, MemberRef methodRef) {
      if (methodRef.getOwner().equals(this.classNode.superName)) {
         this.updateBinding(method, methodRef, ClassInfo.Traversal.SUPER);
      }
   }

   private void updateDynamicBinding(MethodNode method, MemberRef methodRef) {
      this.updateBinding(method, methodRef, ClassInfo.Traversal.ALL);
   }

   private void updateBinding(MethodNode method, MemberRef methodRef, ClassInfo.Traversal traversal) {
      if (!"<init>".equals(methodRef.getName()) && !methodRef.getOwner().equals(this.getTarget().getClassRef())) {
         ClassInfo.Method superMethod = this.targetClassInfo.findMethodInHierarchy(methodRef.getName(), methodRef.getDesc(), traversal.getSearchType(), traversal);
         if (superMethod != null) {
            if (superMethod.getOwner().isMixin()) {
               throw new InvalidMixinException(this, "Invalid " + methodRef + " in " + this + " resolved " + superMethod.getOwner() + " but is mixin.");
            }

            methodRef.setOwner(superMethod.getImplementor().getName());
         } else if (methodRef.ownerIsMixin()) {
            throw new MixinTransformerError("Error resolving " + methodRef + " in " + this);
         }

      }
   }

   void transformDescriptor(FieldNode field) {
      field.desc = this.transformSingleDescriptor(field.desc, false);
   }

   void transformDescriptor(MethodNode method) {
      method.desc = this.transformMethodDescriptor(method.desc);
   }

   void transformDescriptor(MemberRef member) {
      if (member.isField()) {
         member.setDesc(this.transformSingleDescriptor(member.getDesc(), false));
      } else {
         member.setDesc(this.transformMethodDescriptor(member.getDesc()));
      }

   }

   void transformDescriptor(TypeInsnNode typeInsn) {
      typeInsn.desc = this.transformSingleDescriptor(typeInsn.desc, true);
   }

   private String transformDescriptor(Type type) {
      return type.getSort() == 11 ? this.transformMethodDescriptor(type.getDescriptor()) : this.transformSingleDescriptor(type);
   }

   private String transformSingleDescriptor(Type type) {
      return type.getSort() < 9 ? type.toString() : this.transformSingleDescriptor(type.toString(), false);
   }

   private String transformSingleDescriptor(String desc, boolean isObject) {
      IActivityContext.IActivity descriptorActivity = this.activities.begin("desc=%s", desc);
      boolean isArray = false;
      String type = desc;

      while(type.startsWith("[") || type.startsWith("L")) {
         if (type.startsWith("[")) {
            type = type.substring(1);
            isArray = true;
         } else {
            type = type.substring(1, type.indexOf(";"));
            isObject = true;
         }
      }

      if (!isObject) {
         descriptorActivity.end();
         return desc;
      } else {
         if (isArray && type.length() == 1) {
            Type parsedType = Type.getType(type);
            if (parsedType.getSort() <= 8) {
               descriptorActivity.end();
               return desc;
            }
         }

         String innerClassName = (String)this.innerClasses.get(type);
         if (innerClassName != null) {
            descriptorActivity.end();
            return desc.replace(type, innerClassName);
         } else if (this.innerClasses.inverse().containsKey(type)) {
            descriptorActivity.end();
            return desc;
         } else {
            ClassInfo typeInfo;
            if (ClassInfo.isMixin(type) && !(typeInfo = ClassInfo.forName(type)).isLoadable()) {
               String realDesc = desc.replace(type, this.findRealType(typeInfo).toString());
               descriptorActivity.end();
               return realDesc;
            } else {
               descriptorActivity.end();
               return desc;
            }
         }
      }
   }

   private String transformMethodDescriptor(String desc) {
      StringBuilder newDesc = new StringBuilder();
      newDesc.append('(');

      for(Type arg : Type.getArgumentTypes(desc)) {
         newDesc.append(this.transformSingleDescriptor(arg));
      }

      return newDesc.append(')').append(this.transformSingleDescriptor(Type.getReturnType(desc))).toString();
   }

   final Initialiser getInitialiser() {
      return this.initialiser;
   }

   private Initialiser findInitialiser() {
      MethodNode ctor = this.getConstructor();
      if (ctor == null) {
         return null;
      } else {
         InsnRange init = Constructor.getRange(ctor);
         if (!init.isValid()) {
            return null;
         } else {
            Initialiser initialiser = new Initialiser(this, ctor, init);

            for(Constructor targetCtor : this.getTarget().getConstructors()) {
               if (targetCtor.isInjectable()) {
                  targetCtor.inspect(initialiser);
               }
            }

            return initialiser;
         }
      }
   }

   private MethodNode getConstructor() {
      MethodNode ctor = null;

      for(MethodNode method : this.getMethods()) {
         if ("<init>".equals(method.name) && Bytecode.methodHasLineNumbers(method)) {
            if (ctor == null) {
               ctor = method;
            } else {
               logger.warn("Mixin {} has multiple constructors, <init>{} was selected\n", this, ctor.desc);
            }
         }
      }

      return ctor;
   }

   public Target getTargetMethod(MethodNode method) {
      return this.getTarget().getTargetMethod(method);
   }

   MethodNode findMethod(MethodNode method, AnnotationNode annotation) {
      Deque<String> aliases = new LinkedList();
      aliases.add(method.name);
      if (annotation != null) {
         List<String> aka = (List)Annotations.getValue(annotation, "aliases");
         if (aka != null) {
            aliases.addAll(aka);
         }
      }

      return this.getTarget().findMethod(aliases, method.desc);
   }

   MethodNode findRemappedMethod(MethodNode method) {
      RemapperChain remapperChain = this.getEnvironment().getRemappers();
      String remappedName = remapperChain.mapMethodName(this.getTarget().getClassRef(), method.name, method.desc);
      if (remappedName.equals(method.name)) {
         return null;
      } else {
         Deque<String> aliases = new LinkedList();
         aliases.add(remappedName);
         return this.getTarget().findAliasedMethod(aliases, method.desc);
      }
   }

   FieldNode findField(FieldNode field, AnnotationNode shadow) {
      Deque<String> aliases = new LinkedList();
      aliases.add(field.name);
      if (shadow != null) {
         List<String> aka = (List)Annotations.getValue(shadow, "aliases");
         if (aka != null) {
            aliases.addAll(aka);
         }
      }

      return this.getTarget().findField(aliases, field.desc);
   }

   FieldNode findRemappedField(FieldNode field) {
      RemapperChain remapperChain = this.getEnvironment().getRemappers();
      String remappedName = remapperChain.mapFieldName(this.getTarget().getClassRef(), field.name, field.desc);
      if (remappedName.equals(field.name)) {
         return null;
      } else {
         Deque<String> aliases = new LinkedList();
         aliases.add(remappedName);
         return this.getTarget().findAliasedField(aliases, field.desc);
      }
   }

   protected void requireVersion(int version) {
      int majorVersion = version & '\uffff';
      int minorVersion = version >> 16 & '\uffff';
      if (majorVersion > (this.minRequiredClassVersion & '\uffff')) {
         this.minRequiredClassVersion = version;
         if (majorVersion > ASM.getMaxSupportedClassVersionMajor()) {
            throw new InvalidMixinException(this, String.format("Unsupported mixin class version %d.%d. ASM supports %s", majorVersion, minorVersion, ASM.getClassVersionString()));
         } else {
            this.mixin.getParent().checkCompatibilityLevel(this.mixin, majorVersion, minorVersion);
         }
      }
   }

   public Extensions getExtensions() {
      return this.targetClass.getExtensions();
   }

   public IMixinInfo getMixin() {
      return this.mixin;
   }

   MixinInfo getInfo() {
      return this.mixin;
   }

   boolean isRequired() {
      return this.mixin.isRequired();
   }

   public int getPriority() {
      return this.mixin.getPriority();
   }

   Set<String> getInterfaces() {
      return this.mixin.getInterfaces();
   }

   Collection<MethodNode> getShadowMethods() {
      return this.shadowMethods;
   }

   List<MethodNode> getMethods() {
      return this.classNode.methods;
   }

   Set<Map.Entry<FieldNode, ClassInfo.Field>> getShadowFields() {
      return this.shadowFields.entrySet();
   }

   List<FieldNode> getFields() {
      return this.classNode.fields;
   }

   Level getLoggingLevel() {
      return this.mixin.getLoggingLevel();
   }

   boolean shouldSetSourceFile() {
      return this.mixin.getParent().shouldSetSourceFile();
   }

   String getSourceFile() {
      return this.classNode.sourceFile;
   }

   BiMap<String, String> getInnerClasses() {
      return this.innerClasses;
   }

   public IReferenceMapper getReferenceMapper() {
      return this.mixin.getParent().getReferenceMapper();
   }

   void preApply(String transformedName, ClassNode targetClass) throws Exception {
      this.mixin.preApply(transformedName, targetClass);
   }

   void postApply(String transformedName, ClassNode targetClass) {
      this.activities.clear();

      try {
         IActivityContext.IActivity activity = this.activities.begin("Validating Injector Groups");
         this.injectorGroups.validateAll();
         activity.next("Plugin Post-Application");
         this.mixin.postApply(transformedName, targetClass);
         activity.end();
      } catch (InjectionValidationException ex) {
         InjectorGroupInfo group = ex.getGroup();
         throw new InvalidInjectionException(((InjectionInfo)group.getMembers().iterator().next()).getMixin(), String.format("Critical injection failure: Callback group %s in %s failed injection check: %s", group, this.mixin, ex.getMessage()), ex);
      } catch (InvalidMixinException ex) {
         ex.prepend(this.activities);
         throw ex;
      } catch (Exception ex) {
         throw new InvalidMixinException(this, "Unexpecteded " + ex.getClass().getSimpleName() + " whilst transforming the mixin class:", ex, this.activities);
      }
   }

   String getUniqueName(MethodNode method, boolean preservePrefix) {
      return this.targetClassInfo.getMethodMapper().getUniqueName(this.mixin, method, this.sessionId, preservePrefix);
   }

   String getUniqueName(FieldNode field) {
      return this.targetClassInfo.getMethodMapper().getUniqueName(this.mixin, field, this.sessionId);
   }

   void prepareInjections() {
      this.activities.clear();

      try {
         this.injectors.clear();
         IActivityContext.IActivity prepareActivity = this.activities.begin("?");

         for(MethodNode method : this.mergedMethods) {
            prepareActivity.next("%s%s", method.name, method.desc);
            IActivityContext.IActivity methodActivity = this.activities.begin("Parse");
            InjectionInfo injectInfo = InjectionInfo.parse(this, method);
            if (injectInfo != null) {
               methodActivity.next("Validate");
               if (injectInfo.isValid()) {
                  methodActivity.next("Prepare");
                  injectInfo.prepare();
                  this.injectors.add(injectInfo);
               }

               methodActivity.next("Undecorate");
               method.visibleAnnotations.remove(injectInfo.getAnnotationNode());
               methodActivity.end();
            }
         }

         prepareActivity.end();
      } catch (InvalidMixinException ex) {
         ex.prepend(this.activities);
         throw ex;
      } catch (Exception ex) {
         throw new InvalidMixinException(this, "Unexpecteded " + ex.getClass().getSimpleName() + " whilst transforming the mixin class:", ex, this.activities);
      }
   }

   InjectionInfo getFirstInjectionInfo() {
      return this.injectors.isEmpty() ? null : (InjectionInfo)this.injectors.get(0);
   }

   void getInjectorOrders(Set<Integer> orders) {
      for(InjectionInfo injectInfo : this.injectors) {
         orders.add(injectInfo.getOrder());
      }

   }

   void applyPreInjections() {
      this.activities.clear();

      try {
         IActivityContext.IActivity applyActivity = this.activities.begin("PreInject");
         IActivityContext.IActivity preInjectActivity = this.activities.begin("?");

         for(InjectionInfo injectInfo : this.injectors) {
            preInjectActivity.next(injectInfo.toString());
            injectInfo.preInject();
         }

         applyActivity.end();
      } catch (InvalidMixinException ex) {
         ex.prepend(this.activities);
         throw ex;
      } catch (Exception ex) {
         throw new InvalidMixinException(this, "Unexpecteded " + ex.getClass().getSimpleName() + " whilst transforming the mixin class:", ex, this.activities);
      }
   }

   void applyInjections(int injectorOrder) {
      this.activities.clear();
      List<InjectionInfo> injectors = new ArrayList();

      for(InjectionInfo injectInfo : this.injectors) {
         if (injectInfo.getOrder() == injectorOrder) {
            injectors.add(injectInfo);
         }
      }

      try {
         IActivityContext.IActivity applyActivity = this.activities.begin("Inject");
         IActivityContext.IActivity injectActivity = this.activities.begin("?");

         for(InjectionInfo injectInfo : injectors) {
            injectActivity.next(injectInfo.toString());
            injectInfo.inject();
         }

         applyActivity.next("PostInject");
         IActivityContext.IActivity postInjectActivity = this.activities.begin("?");

         for(InjectionInfo injectInfo : injectors) {
            postInjectActivity.next(injectInfo.toString());
            injectInfo.postInject();
         }

         applyActivity.end();
         this.injectors.removeAll(injectors);
      } catch (InvalidMixinException ex) {
         ex.prepend(this.activities);
         throw ex;
      } catch (Exception ex) {
         throw new InvalidMixinException(this, "Unexpecteded " + ex.getClass().getSimpleName() + " whilst transforming the mixin class:", ex, this.activities);
      }
   }

   List<MethodNode> generateAccessors() {
      this.activities.clear();
      List<MethodNode> methods = new ArrayList();

      try {
         IActivityContext.IActivity accessorActivity = this.activities.begin("Locate");
         IActivityContext.IActivity locateActivity = this.activities.begin("?");

         for(AccessorInfo accessor : this.accessors) {
            locateActivity.next(accessor.toString());
            accessor.locate();
         }

         accessorActivity.next("Validate");
         IActivityContext.IActivity validateActivity = this.activities.begin("?");

         for(AccessorInfo accessor : this.accessors) {
            validateActivity.next(accessor.toString());
            accessor.validate();
         }

         accessorActivity.next("Generate");
         IActivityContext.IActivity generateActivity = this.activities.begin("?");

         for(AccessorInfo accessor : this.accessors) {
            generateActivity.next(accessor.toString());
            MethodNode generated = accessor.generate();
            this.getTarget().addMixinMethod(generated);
            methods.add(generated);
         }

         accessorActivity.end();
         return methods;
      } catch (InvalidMixinException ex) {
         ex.prepend(this.activities);
         throw ex;
      } catch (Exception ex) {
         throw new InvalidMixinException(this, "Unexpecteded " + ex.getClass().getSimpleName() + " whilst transforming the mixin class:", ex, this.activities);
      }
   }

   private ClassInfo findRealType(ClassInfo mixin) {
      if (mixin == this.getClassInfo()) {
         return this.targetClassInfo;
      } else {
         ClassInfo type = this.targetClassInfo.findCorrespondingType(mixin);
         if (type == null) {
            throw new InvalidMixinException(this, "Resolution error: unable to find corresponding type for " + mixin + " in hierarchy of " + this.targetClassInfo);
         } else {
            return type;
         }
      }
   }
}
