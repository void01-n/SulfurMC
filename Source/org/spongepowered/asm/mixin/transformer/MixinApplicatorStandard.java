package org.spongepowered.asm.mixin.transformer;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.extensibility.IActivityContext;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.struct.Constructor;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.throwables.MixinError;
import org.spongepowered.asm.mixin.transformer.ext.extensions.ExtensionClassExporter;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;
import org.spongepowered.asm.mixin.transformer.meta.MixinRenamed;
import org.spongepowered.asm.mixin.transformer.struct.Clinit;
import org.spongepowered.asm.mixin.transformer.struct.Initialiser;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.mixin.transformer.throwables.MixinApplicatorException;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.ConstraintParser;
import org.spongepowered.asm.util.asm.ASM;
import org.spongepowered.asm.util.perf.Profiler;
import org.spongepowered.asm.util.throwables.ConstraintViolationException;
import org.spongepowered.asm.util.throwables.InvalidConstraintException;
import org.spongepowered.include.com.google.common.base.Suppliers;
import org.spongepowered.include.com.google.common.collect.ImmutableList;

class MixinApplicatorStandard {
   protected static final List<Class<? extends Annotation>> CONSTRAINED_ANNOTATIONS = ImmutableList.<Class<? extends Annotation>>of(Overwrite.class, Inject.class, ModifyArg.class, ModifyArgs.class, Redirect.class, ModifyVariable.class, ModifyConstant.class);
   protected final ILogger logger = MixinService.getService().getLogger("mixin");
   protected final TargetClassContext context;
   protected final String targetName;
   protected final ClassNode targetClass;
   protected final ClassInfo targetClassInfo;
   protected final Profiler profiler = Profiler.getProfiler("mixin");
   protected final IMixinAuditTrail auditTrail;
   protected final ActivityStack activities = new ActivityStack();
   protected final boolean mergeSignatures;

   MixinApplicatorStandard(TargetClassContext context) {
      this.context = context;
      this.targetName = context.getClassName();
      this.targetClass = context.getClassNode();
      this.targetClassInfo = context.getClassInfo();
      ExtensionClassExporter exporter = (ExtensionClassExporter)context.getExtensions().getExtension(ExtensionClassExporter.class);
      this.mergeSignatures = exporter.isDecompilerActive() && MixinEnvironment.getCurrentEnvironment().getOption(MixinEnvironment.Option.DEBUG_EXPORT_DECOMPILE_MERGESIGNATURES);
      this.auditTrail = MixinService.getService().getAuditTrail();
   }

   final void apply(SortedSet<MixinInfo> mixins) {
      List<MixinTargetContext> mixinContexts = new ArrayList();
      Iterator<MixinInfo> iter = mixins.iterator();

      while(iter.hasNext()) {
         MixinInfo mixin = (MixinInfo)iter.next();

         try {
            this.logger.log(mixin.getLoggingLevel(), "Mixing {} from {} into {}", mixin.getName(), mixin.getParent(), this.targetName);
            mixinContexts.add(mixin.createContextFor(this.context));
            if (this.auditTrail != null) {
               this.auditTrail.onApply(this.targetName, mixin.toString());
            }
         } catch (InvalidMixinException ex) {
            if (mixin.isRequired()) {
               throw ex;
            }

            this.context.addSuppressed(ex);
            iter.remove();
         }
      }

      MixinTargetContext current = null;
      this.activities.clear();

      try {
         IActivityContext.IActivity activity = this.activities.begin("PreApply Phase");
         IActivityContext.IActivity preApplyActivity = this.activities.begin("Mixin");
         this.preApply(preApplyActivity, mixinContexts);
         preApplyActivity.end();

         for(ApplicatorPass pass : MixinApplicatorStandard.ApplicatorPass.values()) {
            activity.next("%s Applicator Phase", pass);
            Profiler.Section timer = this.profiler.begin("pass", pass.name().toLowerCase(Locale.ROOT));
            this.runApplicatorPass(pass, mixinContexts);
            timer.end();
         }

         activity.next("PostApply Phase");
         IActivityContext.IActivity postApplyActivity = this.activities.begin("Mixin");
         Iterator<MixinTargetContext> iter = mixinContexts.iterator();

         while(iter.hasNext()) {
            current = (MixinTargetContext)iter.next();
            postApplyActivity.next(current.toString());

            try {
               current.postApply(this.targetName, this.targetClass);
            } catch (InvalidMixinException ex) {
               if (current.isRequired()) {
                  throw ex;
               }

               this.context.addSuppressed(ex);
               iter.remove();
            }
         }

         activity.end();
      } catch (InvalidMixinException ex) {
         ex.prepend(this.activities);
         throw ex;
      } catch (Exception ex) {
         throw new MixinApplicatorException(current, "Unexpecteded " + ex.getClass().getSimpleName() + " whilst applying the mixin class:", ex, this.activities);
      }

      this.applySourceMap(this.context);
      this.context.processDebugTasks();
   }

   protected void preApply(IActivityContext.IActivity activity, List<MixinTargetContext> mixins) throws Exception {
      for(MixinTargetContext context : mixins) {
         activity.next(context.toString());
         context.preApply(this.targetName, this.targetClass);
      }

   }

   private void runApplicatorPass(ApplicatorPass pass, List<MixinTargetContext> mixinContexts) {
      switch (pass.ordinal()) {
         case 0:
            this.processMixins(mixinContexts, (activity, mixin) -> {
               activity.next("Apply Signature");
               this.applySignature(mixin);
               activity.next("Apply Interfaces");
               this.applyInterfaces(mixin);
               activity.next("Apply Attributess");
               this.applyAttributes(mixin);
               activity.next("Apply Annotations");
               this.applyAnnotations(mixin);
               activity.next("Apply Fields");
               this.applyFields(mixin);
               activity.next("Apply Methods");
               this.applyMethods(mixin);
            });
            break;
         case 1:
            this.processMixins(mixinContexts, (activity, mixin) -> {
               if (FabricUtil.getCompatibility((IMixinContext)mixin) >= 17001) {
                  activity.next("Prepare Injections");
                  this.prepareInjections(mixin);
               }

            });
            break;
         case 2:
            this.processMixins(mixinContexts, (activity, mixin) -> {
               if (FabricUtil.getCompatibility((IMixinContext)mixin) < 17001) {
                  activity.next("Apply Legacy Initialisers");
                  this.applyInitialisers(mixin);
                  activity.next("Apply Legacy CLINIT");
                  this.applyClinitLegacy(mixin);
               }

            });
            break;
         case 3:
            this.processMixins(mixinContexts, (activity, mixin) -> {
               if (FabricUtil.getCompatibility((IMixinContext)mixin) < 17001) {
                  activity.next("Prepare Legacy Injections");
                  this.prepareInjections(mixin);
               }

            });
            break;
         case 4:
            Supplier<Clinit> targetClinit = Suppliers.<Clinit>memoize(this::prepareOrCreateClinit);
            this.processMixins(mixinContexts, (activity, mixin) -> {
               if (FabricUtil.getCompatibility((IMixinContext)mixin) >= 17001) {
                  activity.next("Apply Initialisers");
                  this.applyInitialisers(mixin);
                  activity.next("Apply CLINIT");
                  this.applyClinit(mixin, targetClinit);
               }

            });
            break;
         case 5:
            this.processMixins(mixinContexts, (activity, mixin) -> {
               activity.next("Apply Accessors");
               this.applyAccessors(mixin);
            });
            break;
         case 6:
            this.processMixins(mixinContexts, (activity, mixin) -> {
               activity.next("Pre-Apply Injections");
               this.applyPreInjections(mixin);
            });
            break;
         case 7:
            Set<Integer> orders = new TreeSet();

            for(MixinTargetContext context : mixinContexts) {
               context.getInjectorOrders(orders);
            }

            for(int injectorOrder : orders) {
               this.processMixins(mixinContexts, (activity, mixin) -> {
                  activity.next("Apply Injections");
                  this.applyInjections(mixin, injectorOrder);
               });
            }
            break;
         default:
            throw new IllegalStateException("Invalid pass specified " + pass);
      }

   }

   private void processMixins(List<MixinTargetContext> mixinContexts, BiConsumer<IActivityContext.IActivity, MixinTargetContext> processor) throws InvalidMixinException {
      IActivityContext.IActivity applyActivity = this.activities.begin("Mixin");
      Iterator<MixinTargetContext> iter = mixinContexts.iterator();

      while(iter.hasNext()) {
         MixinTargetContext current = (MixinTargetContext)iter.next();
         applyActivity.next(current.toString());

         try {
            IActivityContext.IActivity individualActivity = this.activities.begin("Apply");
            processor.accept(individualActivity, current);
            individualActivity.end();
         } catch (InvalidMixinException ex) {
            if (current.isRequired()) {
               throw ex;
            }

            this.context.addSuppressed(ex);
            iter.remove();
         }
      }

      applyActivity.end();
   }

   protected void applySignature(MixinTargetContext mixin) {
      if (this.mergeSignatures) {
         this.context.mergeSignature(mixin.getSignature());
      }

   }

   protected void applyInterfaces(MixinTargetContext mixin) {
      for(String interfaceName : mixin.getInterfaces()) {
         if (!this.targetClass.interfaces.contains(interfaceName)) {
            this.targetClass.interfaces.add(interfaceName);
            this.targetClassInfo.addInterface(interfaceName);
         }
      }

   }

   protected void applyAttributes(MixinTargetContext mixin) {
      if (mixin.shouldSetSourceFile()) {
         this.targetClass.sourceFile = mixin.getSourceFile();
      }

      int requiredVersion = mixin.getMinRequiredClassVersion();
      if ((requiredVersion & '\uffff') > (this.targetClass.version & '\uffff')) {
         this.targetClass.version = requiredVersion;
      }

   }

   protected void applyAnnotations(MixinTargetContext mixin) {
      ClassNode sourceClass = mixin.getClassNode();
      Annotations.merge(sourceClass, this.targetClass);
   }

   protected void applyFields(MixinTargetContext mixin) {
      this.mergeShadowFields(mixin);
      this.mergeNewFields(mixin);
   }

   protected void mergeShadowFields(MixinTargetContext mixin) {
      for(Map.Entry<FieldNode, ClassInfo.Field> entry : mixin.getShadowFields()) {
         FieldNode shadow = (FieldNode)entry.getKey();
         FieldNode target = this.findTargetField(shadow);
         if (target != null) {
            Annotations.merge(shadow, target);
            if (((ClassInfo.Field)entry.getValue()).isDecoratedMutable()) {
               target.access &= -17;
            }
         }
      }

   }

   protected void mergeNewFields(MixinTargetContext mixin) {
      for(FieldNode field : mixin.getFields()) {
         this.mergeNormalField(mixin, field, this.targetClass.fields.size());
      }

   }

   protected void mergeNormalField(MixinTargetContext mixin, FieldNode field, int index) {
      FieldNode target = this.findTargetField(field);
      if (target == null) {
         this.targetClass.fields.add(index, field);
         mixin.fieldMerged(field);
         if (field.signature != null) {
            if (this.mergeSignatures) {
               SignatureVisitor sv = mixin.getSignature().getRemapper();
               (new SignatureReader(field.signature)).accept(sv);
               field.signature = sv.toString();
            } else {
               field.signature = null;
            }
         }

      }
   }

   protected void applyMethods(MixinTargetContext mixin) {
      IActivityContext.IActivity activity = this.activities.begin("?");

      for(MethodNode shadow : mixin.getShadowMethods()) {
         activity.next("@Shadow %s:%s", shadow.desc, shadow.name);
         this.applyShadowMethod(mixin, shadow);
      }

      for(MethodNode mixinMethod : mixin.getMethods()) {
         activity.next("%s:%s", mixinMethod.desc, mixinMethod.name);
         this.applyNormalMethod(mixin, mixinMethod);
      }

      activity.end();
   }

   protected void applyShadowMethod(MixinTargetContext mixin, MethodNode shadow) {
      MethodNode target = this.findTargetMethod(shadow);
      if (target != null) {
         Annotations.merge(shadow, target);
      }

   }

   protected void applyNormalMethod(MixinTargetContext mixin, MethodNode mixinMethod) {
      mixin.transformMethod(mixinMethod);
      if (!mixinMethod.name.startsWith("<")) {
         this.checkMethodVisibility(mixin, mixinMethod);
         this.checkMethodConstraints(mixin, mixinMethod);
         this.mergeMethod(mixin, mixinMethod);
      }

   }

   protected void mergeMethod(MixinTargetContext mixin, MethodNode method) {
      boolean isOverwrite = Annotations.getVisible(method, Overwrite.class) != null;
      MethodNode target = this.findTargetMethod(method);
      if (target != null) {
         if (this.isAlreadyMerged(mixin, method, isOverwrite, target)) {
            return;
         }

         AnnotationNode intrinsic = Annotations.getInvisible(method, Intrinsic.class);
         if (intrinsic != null) {
            if (this.mergeIntrinsic(mixin, method, isOverwrite, target, intrinsic)) {
               mixin.getTarget().methodMerged(method);
               return;
            }
         } else {
            if (mixin.requireOverwriteAnnotations() && !isOverwrite) {
               throw new InvalidMixinException(mixin, String.format("%s%s in %s cannot overwrite method in %s because @Overwrite is required by the parent configuration", method.name, method.desc, mixin, mixin.getTarget().getClassName()));
            }

            this.targetClass.methods.remove(target);
         }
      } else if (isOverwrite) {
         throw new InvalidMixinException(mixin, String.format("Overwrite target \"%s\" was not located in target class %s", method.name, mixin.getTargetClassRef()));
      }

      this.targetClass.methods.add(method);
      mixin.methodMerged(method);
      if (method.signature != null) {
         if (this.mergeSignatures) {
            SignatureVisitor sv = mixin.getSignature().getRemapper();
            (new SignatureReader(method.signature)).accept(sv);
            method.signature = sv.toString();
         } else {
            method.signature = null;
         }
      }

   }

   protected boolean isAlreadyMerged(MixinTargetContext mixin, MethodNode method, boolean isOverwrite, MethodNode target) {
      AnnotationNode merged = Annotations.getVisible(target, MixinMerged.class);
      if (merged == null) {
         if (Annotations.getVisible(target, Final.class) != null) {
            this.logger.warn("Overwrite prohibited for @Final method {} in {}. Skipping method.", method.name, mixin);
            return true;
         } else {
            return false;
         }
      } else {
         String sessionId = (String)Annotations.getValue(merged, "sessionId");
         if (!this.context.getSessionId().equals(sessionId)) {
            throw new ClassFormatError("Invalid @MixinMerged annotation found in" + mixin + " at " + method.name + " in " + this.targetClass.name);
         } else if (Bytecode.hasFlag((MethodNode)target, 4160) && Bytecode.hasFlag((MethodNode)method, 4160)) {
            if (mixin.getEnvironment().getOption(MixinEnvironment.Option.DEBUG_VERBOSE)) {
               this.logger.warn("Synthetic bridge method clash for {} in {}", method.name, mixin);
            }

            return true;
         } else {
            String owner = (String)Annotations.getValue(merged, "mixin");
            int priority = (Integer)Annotations.getValue(merged, "priority");
            AnnotationNode accMethod = Annotations.getSingleVisible(method, Accessor.class, Invoker.class);
            if (accMethod != null) {
               AnnotationNode accTarget = Annotations.getSingleVisible(target, Accessor.class, Invoker.class);
               if (accTarget != null) {
                  String myTarget = (String)Annotations.getValue(accMethod, "target");
                  String trTarget = (String)Annotations.getValue(accTarget, "target");
                  if (myTarget == null) {
                     throw new MixinError("Encountered undecorated Accessor method in " + mixin + " applying to " + this.targetName);
                  }

                  if (myTarget.equals(trTarget)) {
                     return true;
                  }

                  throw new InvalidMixinException(mixin, String.format("Incompatible @%s %s (for %s) in %s previously written by %s (for %s)", Annotations.getSimpleName(accMethod), method.name, myTarget, mixin, owner, trTarget));
               }
            }

            if (priority >= mixin.getPriority() && !owner.equals(mixin.getClassName())) {
               this.logger.warn("Method overwrite conflict for {} in {}, previously written by {}. Skipping method.", method.name, mixin, owner);
               return true;
            } else if (Annotations.getVisible(target, Final.class) != null) {
               this.logger.warn("Method overwrite conflict for @Final method {} in {} declared by {}. Skipping method.", method.name, mixin, owner);
               return true;
            } else {
               return false;
            }
         }
      }
   }

   protected boolean mergeIntrinsic(MixinTargetContext mixin, MethodNode method, boolean isOverwrite, MethodNode target, AnnotationNode intrinsic) {
      if (isOverwrite) {
         throw new InvalidMixinException(mixin, "@Intrinsic is not compatible with @Overwrite, remove one of these annotations on " + method.name + " in " + mixin);
      } else {
         String methodName = method.name + method.desc;
         if (Bytecode.hasFlag((MethodNode)method, 8)) {
            throw new InvalidMixinException(mixin, "@Intrinsic method cannot be static, found " + methodName + " in " + mixin);
         } else {
            if (!Bytecode.hasFlag((MethodNode)method, 4096)) {
               AnnotationNode renamed = Annotations.getVisible(method, MixinRenamed.class);
               if (renamed == null || !(Boolean)Annotations.getValue(renamed, "isInterfaceMember", Boolean.FALSE)) {
                  throw new InvalidMixinException(mixin, "@Intrinsic method must be prefixed interface method, no rename encountered on " + methodName + " in " + mixin);
               }
            }

            if (!(Boolean)Annotations.getValue(intrinsic, "displace", Boolean.FALSE)) {
               this.logger.log(mixin.getLoggingLevel(), "Skipping Intrinsic mixin method {} for {}", methodName, mixin.getTargetClassRef());
               return true;
            } else {
               this.displaceIntrinsic(mixin, method, target);
               return false;
            }
         }
      }
   }

   protected void displaceIntrinsic(MixinTargetContext mixin, MethodNode method, MethodNode target) {
      String proxyName = "proxy+" + target.name;
      Iterator<AbstractInsnNode> iter = method.instructions.iterator();

      while(iter.hasNext()) {
         AbstractInsnNode insn = (AbstractInsnNode)iter.next();
         if (insn instanceof MethodInsnNode && insn.getOpcode() != 184) {
            MethodInsnNode methodNode = (MethodInsnNode)insn;
            if (methodNode.owner.equals(this.targetClass.name) && methodNode.name.equals(target.name) && methodNode.desc.equals(target.desc)) {
               methodNode.name = proxyName;
            }
         }
      }

      target.name = proxyName;
   }

   protected final void appendInsns(MixinTargetContext mixin, MethodNode method) {
      if (Type.getReturnType(method.desc) != Type.VOID_TYPE) {
         throw new IllegalArgumentException("Attempted to merge insns from a method which does not return void");
      } else {
         MethodNode target = this.findTargetMethod(method);
         if (target == null) {
            this.targetClass.methods.add(method);
         } else {
            AbstractInsnNode returnNode = Bytecode.findInsn(target, 177);
            if (returnNode != null) {
               Iterator<AbstractInsnNode> injectIter = method.instructions.iterator();

               while(injectIter.hasNext()) {
                  AbstractInsnNode insn = (AbstractInsnNode)injectIter.next();
                  if (!(insn instanceof LineNumberNode) && insn.getOpcode() != 177) {
                     target.instructions.insertBefore(returnNode, insn);
                  }
               }

               target.maxLocals = Math.max(target.maxLocals, method.maxLocals);
               target.maxStack = Math.max(target.maxStack, method.maxStack);
            }

         }
      }
   }

   protected void applyInitialisers(MixinTargetContext mixin) {
      Initialiser initialiser = mixin.getInitialiser();
      if (initialiser != null && initialiser.size() != 0) {
         for(Constructor ctor : this.context.getConstructors()) {
            if (ctor.isInjectable()) {
               int extraStack = initialiser.getMaxStack() - ctor.getMaxStack();
               if (extraStack > 0) {
                  ctor.extendStack().add(extraStack);
               }

               initialiser.injectInto(ctor);
            }
         }

      }
   }

   protected void applyClinitLegacy(MixinTargetContext mixin) {
      MethodNode clinit = Bytecode.findMethod(mixin.getClassNode(), "<clinit>", "()V");
      if (clinit != null) {
         this.appendInsns(mixin, clinit);
      }

   }

   protected Clinit prepareOrCreateClinit() {
      MethodNode clinit = Bytecode.findMethod(this.targetClass, "<clinit>", "()V");
      if (clinit != null) {
         return Clinit.prepare(this.context.getTargetMethod(clinit));
      } else {
         clinit = new MethodNode(ASM.API_VERSION, 8, "<clinit>", "()V", (String)null, (String[])null);
         InsnNode finalReturn = new InsnNode(177);
         clinit.instructions.add((AbstractInsnNode)finalReturn);
         this.targetClass.methods.add(clinit);
         return new Clinit(clinit, finalReturn);
      }
   }

   protected void applyClinit(MixinTargetContext mixin, Supplier<Clinit> clinit) {
      MethodNode mixinClinit = Bytecode.findMethod(mixin.getClassNode(), "<clinit>", "()V");
      if (mixinClinit != null) {
         ((Clinit)clinit.get()).append(mixin.getMixin(), mixinClinit);
      }
   }

   protected void prepareInjections(MixinTargetContext mixin) {
      mixin.prepareInjections();
   }

   protected void applyPreInjections(MixinTargetContext mixin) {
      mixin.applyPreInjections();
   }

   protected void applyInjections(MixinTargetContext mixin, int injectorOrder) {
      mixin.applyInjections(injectorOrder);
   }

   protected void applyAccessors(MixinTargetContext mixin) {
      for(MethodNode method : mixin.generateAccessors()) {
         if (!method.name.startsWith("<")) {
            this.mergeMethod(mixin, method);
         }
      }

   }

   protected void checkMethodVisibility(MixinTargetContext mixin, MethodNode mixinMethod) {
      if (Bytecode.hasFlag((MethodNode)mixinMethod, 8) && !Bytecode.hasFlag((MethodNode)mixinMethod, 2) && !Bytecode.hasFlag((MethodNode)mixinMethod, 4096) && Annotations.getVisible(mixinMethod, Overwrite.class) == null) {
         throw new InvalidMixinException(mixin, String.format("Mixin %s contains non-private static method %s", mixin, mixinMethod));
      }
   }

   protected void applySourceMap(TargetClassContext context) {
      this.targetClass.sourceDebug = context.getSourceMap().toString();
   }

   protected void checkMethodConstraints(MixinTargetContext mixin, MethodNode method) {
      for(Class<? extends Annotation> annotationType : CONSTRAINED_ANNOTATIONS) {
         AnnotationNode annotation = Annotations.getVisible(method, annotationType);
         if (annotation != null) {
            this.checkConstraints(mixin, method, annotation);
         }
      }

   }

   protected final void checkConstraints(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
      try {
         ConstraintParser.Constraint constraint = ConstraintParser.parse(annotation);

         try {
            constraint.check(mixin.getEnvironment());
         } catch (ConstraintViolationException ex) {
            String message = String.format("Constraint violation: %s on %s in %s", ex.getMessage(), method, mixin);
            this.logger.warn(message);
            if (!mixin.getEnvironment().getOption(MixinEnvironment.Option.IGNORE_CONSTRAINTS)) {
               throw new InvalidMixinException(mixin, message, ex);
            }
         }

      } catch (InvalidConstraintException ex) {
         throw new InvalidMixinException(mixin, ex.getMessage());
      }
   }

   protected final MethodNode findTargetMethod(MethodNode searchFor) {
      for(MethodNode target : this.targetClass.methods) {
         if (target.name.equals(searchFor.name) && target.desc.equals(searchFor.desc)) {
            return target;
         }
      }

      return null;
   }

   protected final FieldNode findTargetField(FieldNode searchFor) {
      for(FieldNode target : this.targetClass.fields) {
         if (target.name.equals(searchFor.name) && target.desc.equals(searchFor.desc)) {
            return target;
         }
      }

      return null;
   }

   static enum ApplicatorPass {
      MAIN,
      INJECT_PREPARE,
      INITIALISER_APPLY_LEGACY,
      INJECT_PREPARE_LEGACY,
      INITIALISER_APPLY,
      ACCESSOR,
      INJECT_PREINJECT,
      INJECT_APPLY;

      // $FF: synthetic method
      private static ApplicatorPass[] $values() {
         return new ApplicatorPass[]{MAIN, INJECT_PREPARE, INITIALISER_APPLY_LEGACY, INJECT_PREPARE_LEGACY, INITIALISER_APPLY, ACCESSOR, INJECT_PREINJECT, INJECT_APPLY};
      }
   }
}
