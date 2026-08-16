package org.spongepowered.asm.mixin.transformer;

import java.lang.reflect.Constructor;
import java.util.List;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.MixinInitialisationError;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.throwables.MixinException;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.ext.IExtensionRegistry;
import org.spongepowered.asm.mixin.transformer.ext.IHotSwap;
import org.spongepowered.asm.transformers.TreeTransformer;
import org.spongepowered.asm.util.asm.ASM;

final class MixinTransformer extends TreeTransformer implements IMixinTransformer {
   private final SyntheticClassRegistry syntheticClassRegistry;
   private final Extensions extensions;
   private final IHotSwap hotSwapper;
   private final MixinCoprocessorNestHost nestHostCoprocessor;
   private final MixinProcessor processor;
   private final MixinClassGenerator generator;

   MixinTransformer() {
      MixinEnvironment environment = MixinEnvironment.getCurrentEnvironment();
      Object globalMixinTransformer = environment.getActiveTransformer();
      if (globalMixinTransformer instanceof IMixinTransformer) {
         throw new MixinException("Terminating MixinTransformer instance " + this);
      } else {
         environment.setActiveTransformer(this);
         this.syntheticClassRegistry = new SyntheticClassRegistry();
         this.extensions = new Extensions(this.syntheticClassRegistry);
         this.hotSwapper = this.initHotSwapper(environment);
         this.nestHostCoprocessor = new MixinCoprocessorNestHost();
         this.processor = new MixinProcessor(environment, this.extensions, this.hotSwapper, this.nestHostCoprocessor);
         this.generator = new MixinClassGenerator(environment, this.extensions);
         DefaultExtensions.create(environment, this.extensions, this.syntheticClassRegistry, this.nestHostCoprocessor);
      }
   }

   private IHotSwap initHotSwapper(MixinEnvironment environment) {
      if (!environment.getOption(MixinEnvironment.Option.HOT_SWAP)) {
         return null;
      } else {
         try {
            MixinProcessor.logger.info("Attempting to load Hot-Swap agent");
            Class<? extends IHotSwap> clazz = Class.forName("org.spongepowered.tools.agent.MixinAgent");
            Constructor<? extends IHotSwap> ctor = clazz.getDeclaredConstructor(IMixinTransformer.class);
            return (IHotSwap)ctor.newInstance(this);
         } catch (Throwable th) {
            MixinProcessor.logger.info("Hot-swap agent could not be loaded, hot swapping of mixins won't work. {}: {}", th.getClass().getSimpleName(), th.getMessage());
            return null;
         }
      }
   }

   public IExtensionRegistry getExtensions() {
      return this.extensions;
   }

   public String getName() {
      return this.getClass().getName();
   }

   public boolean isDelegationExcluded() {
      return true;
   }

   public void audit(MixinEnvironment environment) {
      this.processor.audit(environment);
   }

   public List<String> reload(String mixinClass, ClassNode classNode) {
      return this.processor.reload(mixinClass, classNode);
   }

   public byte[] transformClassBytes(String name, String transformedName, byte[] basicClass) {
      if (transformedName == null) {
         return basicClass;
      } else {
         MixinEnvironment environment = MixinEnvironment.getCurrentEnvironment();
         return basicClass == null ? this.generateClass(environment, transformedName) : this.transformClass(environment, transformedName, basicClass);
      }
   }

   public boolean computeFramesForClass(MixinEnvironment environment, String name, ClassNode classNode) {
      return false;
   }

   public synchronized byte[] transformClass(MixinEnvironment environment, String name, byte[] classBytes) {
      if (!this.couldTransformClass(environment, name)) {
         return classBytes;
      } else {
         ClassNode classNode = this.readClass(name, classBytes);
         return this.processor.applyMixins(environment, name, classNode) ? this.writeClass(classNode) : classBytes;
      }
   }

   public synchronized boolean transformClass(MixinEnvironment environment, String name, ClassNode classNode) {
      return this.processor.applyMixins(environment, name, classNode);
   }

   public synchronized boolean couldTransformClass(MixinEnvironment environment, String name) {
      return this.processor.couldTransformClass(environment, name);
   }

   public synchronized byte[] generateClass(MixinEnvironment environment, String name) {
      ClassNode classNode = createEmptyClass(name);
      return this.generator.generateClass(environment, name, classNode) ? this.writeClass(classNode) : null;
   }

   public synchronized boolean generateClass(MixinEnvironment environment, String name, ClassNode classNode) {
      return this.generator.generateClass(environment, name, classNode);
   }

   private static ClassNode createEmptyClass(String name) {
      ClassNode classNode = new ClassNode(ASM.API_VERSION);
      classNode.name = name.replace('.', '/');
      classNode.version = MixinEnvironment.getCompatibilityLevel().getClassVersion();
      classNode.superName = "java/lang/Object";
      return classNode;
   }

   static class Factory implements IMixinTransformerFactory {
      public IMixinTransformer createTransformer() throws MixinInitialisationError {
         return new MixinTransformer();
      }
   }
}
