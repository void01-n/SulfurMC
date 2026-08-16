package org.spongepowered.asm.mixin.injection.callback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.Surrogate;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.code.InjectorTarget;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.injection.struct.Constructor;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InjectionError;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.transformer.MixinInheritanceTracker;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.Locals;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.asm.util.SignaturePrinter;
import org.spongepowered.asm.util.asm.MethodNodeEx;
import org.spongepowered.include.com.google.common.base.Strings;

public class CallbackInjector extends Injector {
   private final boolean cancellable;
   private final LocalCapture localCapture;
   private final String identifier;
   private final Map<Integer, String> ids = new HashMap();
   private int totalInjections = 0;
   private int callbackInfoVar = -1;
   private String lastId;
   private String lastDesc;
   private Target lastTarget;
   private String callbackInfoClass;

   public CallbackInjector(InjectionInfo info, boolean cancellable, LocalCapture localCapture, String identifier) {
      super(info, "@Inject");
      this.cancellable = cancellable;
      this.localCapture = localCapture;
      this.identifier = identifier;
   }

   protected void sanityCheck(Target target, List<InjectionPoint> injectionPoints) {
      super.sanityCheck(target, injectionPoints);
      this.checkTargetModifiers(target, false);
   }

   protected void addTargetNode(InjectorTarget injectorTarget, List<InjectionNodes.InjectionNode> myNodes, AbstractInsnNode node, Set<InjectionPoint> nominators) {
      InjectionNodes.InjectionNode injectionNode = injectorTarget.addInjectionNode(node);
      if (this.cancellable && injectorTarget.getTarget() instanceof Constructor) {
         throw new InvalidInjectionException(this.info, String.format("Found cancellable @Inject targetting a constructor in injector %s", this));
      } else {
         for(InjectionPoint ip : nominators) {
            try {
               this.checkTargetForNode(injectorTarget.getTarget(), injectionNode, ip.getTargetRestriction(this.info));
            } catch (InvalidInjectionException ex) {
               throw new InvalidInjectionException(this.info, String.format("%s selector %s", ip, ex.getMessage()));
            }

            String id = ip.getId();
            if (!Strings.isNullOrEmpty(id)) {
               String existingId = (String)this.ids.get(injectionNode.getId());
               if (existingId != null && !existingId.equals(id)) {
                  Injector.logger.warn("Conflicting id for {} insn in {}, found id {} on {}, previously defined as {}", Bytecode.getOpcodeName(node), injectorTarget.toString(), id, this.info, existingId);
                  break;
               }

               this.ids.put(injectionNode.getId(), id);
            }
         }

         myNodes.add(injectionNode);
         ++this.totalInjections;
      }
   }

   protected void preInject(Target target, InjectionNodes.InjectionNode node) {
      int fabricCompatibility = FabricUtil.getCompatibility((ISelectorContext)this.info);
      String decorationKey = "locals:" + fabricCompatibility;
      if ((this.localCapture.isCaptureLocals() || this.localCapture.isPrintLocals()) && !node.hasDecoration(decorationKey)) {
         LocalVariableNode[] locals = Locals.getLocalsAt(this.classNode, target.method, node.getCurrentTarget(), fabricCompatibility);

         for(int j = 0; j < locals.length; ++j) {
            if (locals[j] != null && locals[j].desc != null && locals[j].desc.startsWith("Lorg/spongepowered/asm/mixin/injection/callback/")) {
               locals[j] = null;
            }
         }

         node.decorate(decorationKey, locals);
      }

   }

   protected void inject(Target target, InjectionNodes.InjectionNode node) {
      LocalVariableNode[] locals = (LocalVariableNode[])node.getDecoration("locals:" + FabricUtil.getCompatibility((ISelectorContext)this.info));
      this.inject(new Callback(this.methodNode, target, node, locals, this.localCapture.isCaptureLocals()));
   }

   private void inject(Callback callback) {
      if (this.localCapture.isPrintLocals()) {
         this.printLocals(callback);
         this.info.addCallbackInvocation(this.methodNode);
      } else {
         MethodNode callbackMethod = this.methodNode;
         if (!callback.checkDescriptor(this.methodNode.desc)) {
            if (this.info.getTargetCount() > 1) {
               return;
            }

            if (callback.canCaptureLocals) {
               MethodNode surrogateHandler = Bytecode.findMethod(this.classNode, this.methodNode.name, callback.getDescriptor());
               if (surrogateHandler != null && Annotations.getVisible(surrogateHandler, Surrogate.class) != null) {
                  callbackMethod = surrogateHandler;
               } else {
                  String message = this.generateBadLVTMessage(callback);
                  switch (this.localCapture) {
                     case CAPTURE_FAILEXCEPTION:
                        Injector.logger.error("Injection error: {}", message);
                        callbackMethod = this.generateErrorMethod(callback, "org/spongepowered/asm/mixin/injection/throwables/InjectionError", message);
                        break;
                     case CAPTURE_FAILSOFT:
                        Injector.logger.warn("Injection warning: {}", message);
                        return;
                     default:
                        Injector.logger.error("Critical injection failure: {}", message);
                        throw new InjectionError(message);
                  }
               }
            } else {
               String returnableSig = this.methodNode.desc.replace("Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;", "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;");
               if (callback.checkDescriptor(returnableSig)) {
                  throw new InvalidInjectionException(this.info, "Invalid descriptor on " + this.info + "! CallbackInfoReturnable is required!");
               }

               MethodNode surrogateHandler = Bytecode.findMethod(this.classNode, this.methodNode.name, callback.getDescriptor());
               if (surrogateHandler == null || Annotations.getVisible(surrogateHandler, Surrogate.class) == null) {
                  throw new InvalidInjectionException(this.info, "Invalid descriptor on " + this.info + "! Expected " + callback.getDescriptor() + " but found " + this.methodNode.desc);
               }

               callbackMethod = surrogateHandler;
            }
         }

         if (callback.usesCallbackInfo) {
            this.dupReturnValue(callback);
            if (this.cancellable || this.totalInjections > 1) {
               this.createCallbackInfo(callback, true);
            }
         }

         this.invokeCallback(callback, callbackMethod);
         if (callback.usesCallbackInfo) {
            this.injectCancellationCode(callback);
         }

         callback.inject();
         this.info.notifyInjected(callback.target);
      }
   }

   private String generateBadLVTMessage(Callback callback) {
      int position = callback.target.indexOf(callback.node);
      int targetArgc = callback.target.arguments.length + 1;
      List<String> expected = summariseLocals((String)this.methodNode.desc, targetArgc, 255);
      List<String> found = summariseLocals(callback.getDescriptorWithAllLocals(), targetArgc, expected.size());
      if (expected.equals(found)) {
         return String.format("Invalid descriptor on %s! Expected %s but found %s", this.info, callback.getDescriptor(), this.methodNode.desc);
      } else {
         List<String> available = summariseLocals((String)callback.getDescriptorWithAllLocals(), targetArgc, 255);
         return String.format("LVT in %s has incompatible changes at opcode %d in callback %s.\n Expected: %s\n    Found: %s\nAvailable: %s", callback.target, position, this.info, expected, found, available);
      }
   }

   private MethodNode generateErrorMethod(Callback callback, String errorClass, String message) {
      MethodNode method = this.info.addMethod(this.methodNode.access, this.methodNode.name + "$missing", callback.getDescriptor());
      method.maxLocals = Bytecode.getFirstNonArgLocalIndex(Type.getArgumentTypes(callback.getDescriptor()), !this.isStatic);
      method.maxStack = 3;
      InsnList insns = method.instructions;
      insns.add((AbstractInsnNode)(new TypeInsnNode(187, errorClass)));
      insns.add((AbstractInsnNode)(new InsnNode(89)));
      insns.add((AbstractInsnNode)(new LdcInsnNode(message)));
      insns.add((AbstractInsnNode)(new MethodInsnNode(183, errorClass, "<init>", "(Ljava/lang/String;)V", false)));
      insns.add((AbstractInsnNode)(new InsnNode(191)));
      return method;
   }

   private void printLocals(Callback callback) {
      Type[] args = Type.getArgumentTypes(callback.getDescriptorWithAllLocals());
      SignaturePrinter methodSig = new SignaturePrinter(callback.target.method, callback.argNames);
      SignaturePrinter handlerSig = new SignaturePrinter(this.info.getMethodName(), callback.target.returnType, args, callback.argNames);
      handlerSig.setModifiers(this.methodNode);
      PrettyPrinter printer = new PrettyPrinter();
      printer.kv("Target Class", this.classNode.name.replace('/', '.'));
      printer.kv("Target Method", methodSig);
      printer.kv("Target Max LOCALS", callback.target.getMaxLocals());
      printer.kv("Initial Frame Size", callback.frameSize);
      printer.kv("Callback Name", this.info.getMethodName());
      printer.kv("Instruction", "%s %s", callback.node.getCurrentTarget().getClass().getSimpleName(), Bytecode.describeNode(callback.node.getCurrentTarget()));
      printer.hr();
      if (callback.locals.length > callback.frameSize) {
         printer.add("  %s  %20s  %s", "LOCAL", "TYPE", "NAME");

         for(int l = 0; l < callback.locals.length; ++l) {
            String marker = l == callback.frameSize ? ">" : " ";
            if (callback.locals[l] != null) {
               printer.add("%s [%3d]  %20s  %-50s %s", marker, l, SignaturePrinter.getTypeName(callback.localTypes[l], false), meltSnowman(l, callback.locals[l].name), l >= callback.frameSize ? "<capture>" : "");
            } else {
               boolean isTop = l > 0 && callback.localTypes[l - 1] != null && callback.localTypes[l - 1].getSize() > 1;
               printer.add("%s [%3d]  %20s", marker, l, isTop ? "<top>" : "-");
            }
         }

         printer.hr();
      }

      printer.add().add("/**").add(" * Expected callback signature").add(" * /");
      printer.add("%s {", handlerSig);
      printer.add("    // Method body").add("}").add().print(System.err);
   }

   private void createCallbackInfo(Callback callback, boolean store) {
      if (callback.target != this.lastTarget) {
         this.lastId = null;
         this.lastDesc = null;
      }

      this.lastTarget = callback.target;
      String id = this.getIdentifier(callback);
      String desc = callback.getCallbackInfoConstructorDescriptor();
      if (!id.equals(this.lastId) || !desc.equals(this.lastDesc) || callback.isAtReturn || this.cancellable) {
         this.instanceCallbackInfo(callback, id, desc, store);
      }
   }

   private void loadOrCreateCallbackInfo(Callback callback) {
      if (!callback.usesCallbackInfo) {
         callback.add(new InsnNode(1));
      } else if (!this.cancellable && this.totalInjections <= 1) {
         this.createCallbackInfo(callback, false);
      } else {
         callback.add(new VarInsnNode(25, this.callbackInfoVar), false, true);
      }

   }

   private void dupReturnValue(Callback callback) {
      if (callback.isAtReturn) {
         int dupCode = callback.target.returnType.getSize() == 1 ? 89 : 92;
         callback.add(new InsnNode(dupCode));
         callback.add(new VarInsnNode(callback.target.returnType.getOpcode(54), callback.marshalVar()));
      }
   }

   protected void instanceCallbackInfo(Callback callback, String id, String desc, boolean store) {
      this.lastId = id;
      this.lastDesc = desc;
      this.callbackInfoVar = callback.marshalVar();
      this.callbackInfoClass = callback.target.getCallbackInfoClass();
      boolean head = store && this.totalInjections > 1 && !callback.isAtReturn && !this.cancellable;
      callback.add(new TypeInsnNode(187, this.callbackInfoClass), true, !store, head);
      callback.add(new InsnNode(89), true, true, head);
      callback.add(new LdcInsnNode(id), true, !store, head);
      callback.add(new InsnNode(this.cancellable ? 4 : 3), true, !store, head);
      if (callback.isAtReturn) {
         callback.add(new VarInsnNode(callback.target.returnType.getOpcode(21), callback.marshalVar()), true, !store);
         callback.add(new MethodInsnNode(183, this.callbackInfoClass, "<init>", desc, false));
      } else {
         callback.add(new MethodInsnNode(183, this.callbackInfoClass, "<init>", desc, false), false, false, head);
      }

      if (store) {
         callback.target.addLocalVariable(this.callbackInfoVar, "callbackInfo" + this.callbackInfoVar, "L" + this.callbackInfoClass + ";");
         callback.add(new VarInsnNode(58, this.callbackInfoVar), false, false, head);
      } else if (callback.isAtReturn) {
         callback.target.addLocalVariable(this.callbackInfoVar, "returnValue" + this.callbackInfoVar, callback.target.returnType.getDescriptor());
      }

   }

   private void invokeCallback(Callback callback, MethodNode callbackMethod) {
      if (!this.isStatic) {
         callback.add(new VarInsnNode(25, 0), false, true);
      }

      if (callback.captureArgs()) {
         Bytecode.loadArgs(callback.target.arguments, callback, callback.target.isStatic ? 0 : 1, -1);
      }

      this.loadOrCreateCallbackInfo(callback);
      if (callback.canCaptureLocals) {
         Locals.loadLocals(callback.localTypes, callback, callback.frameSize, callback.extraArgs);
      }

      this.invokeHandler(callback, callbackMethod);
   }

   private String getIdentifier(Callback callback) {
      String baseId = Strings.isNullOrEmpty(this.identifier) ? callback.target.method.name : this.identifier;
      String locationId = (String)this.ids.get(callback.node.getId());
      return baseId + (Strings.isNullOrEmpty(locationId) ? "" : ":" + locationId);
   }

   protected void injectCancellationCode(Callback callback) {
      if (this.cancellable) {
         callback.add(new VarInsnNode(25, this.callbackInfoVar));
         callback.add(new MethodInsnNode(182, this.callbackInfoClass, CallbackInfo.getIsCancelledMethodName(), CallbackInfo.getIsCancelledMethodSig(), false));
         LabelNode notCancelled = new LabelNode();
         callback.add(new JumpInsnNode(153, notCancelled));
         this.injectReturnCode(callback);
         callback.add(notCancelled);
      }
   }

   protected void injectReturnCode(Callback callback) {
      if (callback.target.returnType.equals(Type.VOID_TYPE)) {
         callback.add(new InsnNode(177));
      } else {
         callback.add(new VarInsnNode(25, callback.marshalVar()));
         String accessor = CallbackInfoReturnable.getReturnAccessor(callback.target.returnType);
         String descriptor = CallbackInfoReturnable.getReturnDescriptor(callback.target.returnType);
         callback.add(new MethodInsnNode(182, this.callbackInfoClass, accessor, descriptor, false));
         if (callback.target.returnType.getSort() >= 9) {
            callback.add(new TypeInsnNode(192, callback.target.returnType.getInternalName()));
         }

         callback.add(new InsnNode(callback.target.returnType.getOpcode(172)));
      }

   }

   protected boolean isStatic() {
      return this.isStatic;
   }

   private static List<String> summariseLocals(String desc, int pos, int count) {
      return summariseLocals(Type.getArgumentTypes(desc), pos, count);
   }

   private static List<String> summariseLocals(Type[] locals, int pos, int count) {
      List<String> list = new ArrayList();
      if (locals != null) {
         for(; pos < locals.length && list.size() < count; ++pos) {
            if (locals[pos] != null) {
               list.add(locals[pos].toString());
            }
         }
      }

      return list;
   }

   static String meltSnowman(int index, String varName) {
      return varName != null && 9731 == varName.charAt(0) ? "var" + index : varName;
   }

   private class Callback extends InsnList {
      private final MethodNode handler;
      private final AbstractInsnNode head;
      final Target target;
      final InjectionNodes.InjectionNode node;
      final LocalVariableNode[] locals;
      final Type[] localTypes;
      final int frameSize;
      final int extraArgs;
      final boolean canCaptureLocals;
      final boolean isAtReturn;
      final String desc;
      final String descl;
      final String[] argNames;
      Target.Extension ctor;
      Target.Extension invoke;
      private int marshalVar = -1;
      private boolean captureArgs = true;
      final boolean usesCallbackInfo;

      Callback(MethodNode handler, Target target, final InjectionNodes.InjectionNode node, final LocalVariableNode[] locals, boolean captureLocals) {
         this.handler = handler;
         this.target = target;
         this.head = target.insns.getFirst();
         this.node = node;
         this.locals = locals;
         this.localTypes = locals != null ? new Type[locals.length] : null;
         this.frameSize = Bytecode.getFirstNonArgLocalIndex(target.arguments, !target.isStatic);
         List<String> argNames = null;
         if (locals != null) {
            int baseArgIndex = target.isStatic ? 0 : 1;
            argNames = new ArrayList();

            for(int l = 0; l <= locals.length; ++l) {
               if (l == this.frameSize) {
                  argNames.add(target.returnType == Type.VOID_TYPE ? "ci" : "cir");
               }

               if (l < locals.length && locals[l] != null) {
                  this.localTypes[l] = Type.getType(locals[l].desc);
                  if (l >= baseArgIndex) {
                     argNames.add(CallbackInjector.meltSnowman(l, locals[l].name));
                  }
               }
            }
         }

         Type[] handlerArgs = Type.getArgumentTypes(this.handler.desc);
         this.extraArgs = Math.max(0, handlerArgs.length - target.arguments.length - 1);
         this.argNames = argNames != null ? (String[])argNames.toArray(new String[argNames.size()]) : null;
         this.canCaptureLocals = captureLocals && locals != null && locals.length > this.frameSize;
         this.isAtReturn = this.node.getCurrentTarget() instanceof InsnNode && this.isValueReturnOpcode(this.node.getCurrentTarget().getOpcode());
         this.desc = target.getCallbackDescriptor(this.localTypes, target.arguments);
         this.descl = target.getCallbackDescriptor(true, this.localTypes, target.arguments, this.frameSize, this.extraArgs);
         this.invoke = target.extendStack();
         this.ctor = target.extendStack();
         this.invoke.add().add(handlerArgs);
         int callbackInfoSlot = Bytecode.isStatic(handler) ? 0 : 1;
         if (handlerArgs.length != 1) {
            callbackInfoSlot += Bytecode.getArgsSize(this.target.arguments);
         }

         boolean seenCallbackInfoUse = false;
         ListIterator var11 = handler.instructions.iterator();

         while(var11.hasNext()) {
            AbstractInsnNode insn = (AbstractInsnNode)var11.next();
            if (insn.getType() == 2 && insn.getOpcode() == 25 && ((VarInsnNode)insn).var == callbackInfoSlot) {
               seenCallbackInfoUse = true;
               break;
            }
         }

         CallbackInjector.logger.debug("{} does{} use it's CallbackInfo{}", CallbackInjector.this.info, seenCallbackInfoUse ? "" : "n't", Type.VOID_TYPE == target.returnType ? "" : "Returnable");
         if (!seenCallbackInfoUse && !Bytecode.isStatic(handler) && (handler.access & 16) == 0 && (target.classNode.access & 16) == 0) {
            String handlerName = handler instanceof MethodNodeEx ? ((MethodNodeEx)handler).getOriginalName() : handler.name;
            List<MethodNode> childHandlers = MixinInheritanceTracker.INSTANCE.findOverrides(CallbackInjector.this.info.getClassInfo(), handlerName, handler.desc);
            CallbackInjector.logger.debug("{} has {} override(s) in child classes", CallbackInjector.this.info, childHandlers.size());

            label119:
            for(MethodNode childHandle : childHandlers) {
               ListIterator var15 = childHandle.instructions.iterator();

               while(var15.hasNext()) {
                  AbstractInsnNode insn = (AbstractInsnNode)var15.next();
                  if (insn.getType() == 2 && insn.getOpcode() == 25 && ((VarInsnNode)insn).var == callbackInfoSlot) {
                     seenCallbackInfoUse = true;
                     break label119;
                  }
               }
            }

            CallbackInjector.logger.debug("{} w{} be passed a CallbackInfo{} as a result", CallbackInjector.this.info, seenCallbackInfoUse ? "ill" : "on't", Type.VOID_TYPE == target.returnType ? "" : "Returnable");
         }

         this.usesCallbackInfo = seenCallbackInfoUse;
      }

      private boolean isValueReturnOpcode(int opcode) {
         return opcode >= 172 && opcode < 177;
      }

      String getDescriptor() {
         return this.canCaptureLocals ? this.descl : this.desc;
      }

      String getDescriptorWithAllLocals() {
         return this.target.getCallbackDescriptor(true, this.localTypes, this.target.arguments, this.frameSize, 32767);
      }

      String getCallbackInfoConstructorDescriptor() {
         return this.isAtReturn ? CallbackInfo.getConstructorDescriptor(this.target.returnType) : CallbackInfo.getConstructorDescriptor();
      }

      void add(AbstractInsnNode insn, boolean ctorStack, boolean invokeStack) {
         this.add(insn, ctorStack, invokeStack, false);
      }

      void add(AbstractInsnNode insn, boolean ctorStack, boolean invokeStack, boolean head) {
         if (head) {
            this.target.insns.insertBefore(this.head, insn);
         } else {
            this.add(insn);
         }

         if (ctorStack) {
            this.ctor.add();
         }

         if (invokeStack) {
            this.invoke.add();
         }

      }

      void inject() {
         this.target.insertBefore((InjectionNodes.InjectionNode)this.node, (InsnList)this);
         this.invoke.apply();
         this.ctor.apply();
      }

      boolean checkDescriptor(String desc) {
         if (this.getDescriptor().equals(desc)) {
            return true;
         } else if (this.target.getSimpleCallbackDescriptor().equals(desc) && !this.canCaptureLocals) {
            this.captureArgs = false;
            return true;
         } else {
            Type[] inTypes = Type.getArgumentTypes(desc);
            Type[] myTypes = Type.getArgumentTypes(this.descl);
            if (inTypes.length != myTypes.length) {
               return false;
            } else {
               for(int arg = 0; arg < myTypes.length; ++arg) {
                  Type type = inTypes[arg];
                  if (!type.equals(myTypes[arg])) {
                     if (type.getSort() == 9) {
                        return false;
                     }

                     if (Annotations.getInvisibleParameter(this.handler, Coerce.class, arg) == null) {
                        return false;
                     }

                     if (!Injector.canCoerce(inTypes[arg], myTypes[arg])) {
                        return false;
                     }
                  }
               }

               return true;
            }
         }
      }

      boolean captureArgs() {
         return this.captureArgs;
      }

      int marshalVar() {
         if (this.marshalVar < 0) {
            this.marshalVar = this.target.allocateLocal();
         }

         return this.marshalVar;
      }
   }
}
