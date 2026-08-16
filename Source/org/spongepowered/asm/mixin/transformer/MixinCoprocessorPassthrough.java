package org.spongepowered.asm.mixin.transformer;

import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;

class MixinCoprocessorPassthrough extends MixinCoprocessor {
   private final Set<String> loadable = new HashSet();

   String getName() {
      return "passthrough";
   }

   public void onPrepare(MixinInfo mixin) {
      if (mixin.isLoadable()) {
         this.registerLoadable(mixin.getClassName());
      }

   }

   void registerLoadable(String className) {
      this.loadable.add(className);
   }

   MixinCoprocessor.ProcessResult process(String className, ClassNode classNode) {
      return this.loadable.contains(className) ? MixinCoprocessor.ProcessResult.PASSTHROUGH_NONE : MixinCoprocessor.ProcessResult.NONE;
   }

   public boolean couldTransform(String className) {
      return false;
   }
}
