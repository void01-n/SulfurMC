package org.spongepowered.asm.mixin.transformer;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.service.MixinService;

abstract class MixinCoprocessor implements MixinConfig.IListener {
   private static final ILogger logger = MixinService.getService().getLogger("mixin");
   private boolean willLogUnimplementedCouldTransform = true;

   abstract String getName();

   public void onPrepare(MixinInfo mixin) {
   }

   public void onInit(MixinInfo mixin) {
   }

   ProcessResult process(String className, ClassNode classNode) {
      return MixinCoprocessor.ProcessResult.NONE;
   }

   public boolean couldTransform(String className) {
      if (this.willLogUnimplementedCouldTransform) {
         this.willLogUnimplementedCouldTransform = false;
         logger.error("MixinCoprocessor {} ({}) does not implement couldTransform, which may lead to unnecessary transformation", this.getName(), this.getClass().getName());
      }

      return true;
   }

   boolean postProcess(String className, ClassNode classNode) {
      return false;
   }

   static enum ProcessResult {
      NONE(false, false),
      TRANSFORMED(false, true),
      PASSTHROUGH_NONE(true, false),
      PASSTHROUGH_TRANSFORMED(true, true);

      private boolean passthrough;
      private boolean transformed;

      private ProcessResult(boolean passthrough, boolean transformed) {
         this.passthrough = passthrough;
         this.transformed = transformed;
      }

      boolean isPassthrough() {
         return this.passthrough;
      }

      boolean isTransformed() {
         return this.transformed;
      }

      ProcessResult with(ProcessResult other) {
         return other == this ? this : of(this.passthrough || other.passthrough, this.transformed || other.transformed);
      }

      static ProcessResult of(boolean passthrough, boolean transformed) {
         if (passthrough) {
            return transformed ? PASSTHROUGH_TRANSFORMED : PASSTHROUGH_NONE;
         } else {
            return transformed ? TRANSFORMED : NONE;
         }
      }

      // $FF: synthetic method
      private static ProcessResult[] $values() {
         return new ProcessResult[]{NONE, TRANSFORMED, PASSTHROUGH_NONE, PASSTHROUGH_TRANSFORMED};
      }
   }
}
