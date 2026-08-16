package org.spongepowered.asm.mixin.transformer.struct;

import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.injection.struct.Constructor;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Bytecode;

public class Initialiser {
   static final ILogger logger = MixinService.getService().getLogger("mixin");
   protected static final int[] OPCODE_BLACKLIST = new int[]{177, 21, 22, 23, 24, 54, 55, 56, 57, 58};
   private final MixinTargetContext mixin;
   private final MethodNode ctor;
   private Deque<AbstractInsnNode> insns;

   public Initialiser(MixinTargetContext mixin, MethodNode ctor, InsnRange range) {
      this.mixin = mixin;
      this.ctor = ctor;
      this.initInstructions(range);
   }

   private void initInstructions(InsnRange range) {
      this.insns = range.apply(this.ctor.instructions, false);

      for(AbstractInsnNode insn : this.insns) {
         int opcode = insn.getOpcode();

         for(int ivalidOp : OPCODE_BLACKLIST) {
            if (opcode == ivalidOp) {
               throw new InvalidMixinException(this.mixin, "Cannot handle " + Bytecode.getOpcodeName(opcode) + " opcode (0x" + Integer.toHexString(opcode).toUpperCase(Locale.ROOT) + ") in class initialiser");
            }
         }
      }

      AbstractInsnNode last = (AbstractInsnNode)this.insns.peekLast();
      if (last != null && last.getOpcode() != 181) {
         throw new InvalidMixinException(this.mixin, "Could not parse initialiser, expected 0xB5, found 0x" + Integer.toHexString(last.getOpcode()) + " in " + this);
      }
   }

   public int size() {
      return this.insns.size();
   }

   public int getMaxStack() {
      return this.ctor.maxStack;
   }

   public MethodNode getCtor() {
      return this.ctor;
   }

   public Deque<AbstractInsnNode> getInsns() {
      return this.insns;
   }

   public void injectInto(Constructor ctor) {
      AbstractInsnNode marker = ctor.findInitialiserInjectionPoint(Initialiser.InjectionMode.ofEnvironment(this.mixin.getEnvironment()));
      if (marker == null) {
         logger.warn("Failed to locate initialiser injection point in <init>{}, initialiser was not mixed in.", ctor.getDesc());
      } else {
         Map<LabelNode, LabelNode> labels = Bytecode.cloneLabels(ctor.insns);

         for(AbstractInsnNode node : this.insns) {
            if (node instanceof LabelNode) {
               labels.put((LabelNode)node, new LabelNode());
            }
         }

         for(AbstractInsnNode node : this.insns) {
            if (node instanceof LabelNode) {
            }

            if (node instanceof JumpInsnNode) {
            }

            ctor.insertBefore(marker, node.clone(labels));
         }

      }
   }

   public static enum InjectionMode {
      DEFAULT,
      SAFE;

      public static InjectionMode ofEnvironment(MixinEnvironment env) {
         String strMode = env.getOptionValue(MixinEnvironment.Option.INITIALISER_INJECTION_MODE);
         if (strMode == null) {
            return DEFAULT;
         } else {
            try {
               return valueOf(strMode.toUpperCase(Locale.ROOT));
            } catch (Exception var3) {
               Initialiser.logger.warn("Could not parse unexpected value \"{}\" for mixin.initialiserInjectionMode, reverting to DEFAULT", strMode);
               return DEFAULT;
            }
         }
      }

      // $FF: synthetic method
      private static InjectionMode[] $values() {
         return new InjectionMode[]{DEFAULT, SAFE};
      }
   }
}
