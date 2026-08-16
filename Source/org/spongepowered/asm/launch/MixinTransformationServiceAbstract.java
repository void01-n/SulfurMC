package org.spongepowered.asm.launch;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionSpecBuilder;
import org.spongepowered.include.com.google.common.collect.ImmutableList;

public abstract class MixinTransformationServiceAbstract implements ITransformationService {
   private ArgumentAcceptingOptionSpec<String> mixinsArgument;
   private List<String> commandLineMixins = new ArrayList();
   private MixinLaunchPluginLegacy plugin;

   public String name() {
      return "mixin";
   }

   public void arguments(BiFunction<String, String, OptionSpecBuilder> argumentBuilder) {
      this.mixinsArgument = ((OptionSpecBuilder)argumentBuilder.apply("config", "a mixin config to load")).withRequiredArg().ofType(String.class);
   }

   public void argumentValues(ITransformationService.OptionResult option) {
      this.commandLineMixins.addAll(option.values(this.mixinsArgument));
   }

   public void onLoad(IEnvironment environment, Set<String> otherServices) throws IncompatibleEnvironmentException {
   }

   public void initialize(IEnvironment environment) {
      Optional<ILaunchPluginService> plugin = environment.findLaunchPlugin("mixin");
      if (!plugin.isPresent()) {
         throw new MixinInitialisationError("Mixin Launch Plugin Service could not be located");
      } else {
         ILaunchPluginService launchPlugin = (ILaunchPluginService)plugin.get();
         if (!(launchPlugin instanceof MixinLaunchPluginLegacy)) {
            throw new MixinInitialisationError("Mixin Launch Plugin Service is present but not compatible");
         } else {
            this.plugin = (MixinLaunchPluginLegacy)launchPlugin;
            MixinBootstrap.start();
            this.plugin.init(environment, this.commandLineMixins);
         }
      }
   }

   public List<Map.Entry<String, Path>> runScan(IEnvironment environment) {
      return Collections.emptyList();
   }

   public List<ITransformer> transformers() {
      return ImmutableList.<ITransformer>of();
   }
}
