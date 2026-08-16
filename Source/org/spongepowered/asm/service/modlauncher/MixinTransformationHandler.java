package org.spongepowered.asm.service.modlauncher;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService.Phase;
import java.util.EnumSet;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.IClassProcessor;
import org.spongepowered.asm.launch.Phases;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.ISyntheticClassRegistry;
import org.spongepowered.include.com.google.common.base.Preconditions;

public class MixinTransformationHandler implements IClassProcessor {
   private IMixinTransformerFactory transformerFactory;
   private IMixinTransformer transformer;
   private ISyntheticClassRegistry registry;

   void offer(IMixinTransformerFactory transformerFactory) {
      Preconditions.checkNotNull(transformerFactory, "transformerFactory");
      this.transformerFactory = transformerFactory;
   }

   public EnumSet<ILaunchPluginService.Phase> handlesClass(Type classType, boolean isEmpty, String reason) {
      if (!isEmpty) {
         return Phases.AFTER_ONLY;
      } else if (this.registry == null) {
         return null;
      } else {
         return this.generatesClass(classType) ? Phases.AFTER_ONLY : null;
      }
   }

   public boolean generatesClass(Type classType) {
      return this.registry.findSyntheticClass(classType.getClassName()) != null;
   }

   public synchronized boolean processClass(ILaunchPluginService.Phase phase, ClassNode classNode, Type classType, String reason) {
      if (phase == Phase.BEFORE) {
         return false;
      } else {
         if (this.transformer == null) {
            if (this.transformerFactory == null) {
               throw new IllegalStateException("processClass called before transformer factory offered to transformation handler");
            }

            this.transformer = this.transformerFactory.createTransformer();
            this.registry = this.transformer.getExtensions().getSyntheticClassRegistry();
         }

         if ("mixin".equals(reason)) {
            return false;
         } else if (this.generatesClass(classType)) {
            return this.generateClass(classType, classNode);
         } else {
            MixinEnvironment environment = MixinEnvironment.getCurrentEnvironment();
            return "computing_frames".equals(reason) ? this.transformer.computeFramesForClass(environment, classType.getClassName(), classNode) : this.transformer.transformClass(environment, classType.getClassName(), classNode);
         }
      }
   }

   public boolean generateClass(Type classType, ClassNode classNode) {
      return this.transformer.generateClass(MixinEnvironment.getCurrentEnvironment(), classType.getClassName(), classNode);
   }
}
