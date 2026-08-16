package org.spongepowered.asm.mixin.transformer;

import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.injection.invoke.arg.ArgsClassGenerator;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.ext.IClassGenerator;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.extensions.ExtensionCheckClass;
import org.spongepowered.asm.mixin.transformer.ext.extensions.ExtensionCheckInterfaces;
import org.spongepowered.asm.mixin.transformer.ext.extensions.ExtensionClassExporter;
import org.spongepowered.asm.mixin.transformer.ext.extensions.ExtensionLVTCleaner;
import org.spongepowered.asm.service.ISyntheticClassInfo;
import org.spongepowered.asm.util.IConsumer;

final class DefaultExtensions {
   static void create(MixinEnvironment environment, Extensions extensions, final SyntheticClassRegistry registry, MixinCoprocessorNestHost nestHostCoprocessor) {
      IConsumer<ISyntheticClassInfo> registryDelegate = new IConsumer<ISyntheticClassInfo>() {
         public void accept(ISyntheticClassInfo item) {
            registry.registerSyntheticClass(item);
         }
      };
      extensions.add((IClassGenerator)(new ArgsClassGenerator(registryDelegate)));
      extensions.add((IClassGenerator)(new InnerClassGenerator(registryDelegate, nestHostCoprocessor)));
      extensions.add((IExtension)(new ExtensionClassExporter(environment)));
      extensions.add((IExtension)(new ExtensionLVTCleaner()));
      extensions.add((IExtension)(new ExtensionCheckClass()));
      extensions.add((IExtension)(new ExtensionCheckInterfaces()));
   }
}
