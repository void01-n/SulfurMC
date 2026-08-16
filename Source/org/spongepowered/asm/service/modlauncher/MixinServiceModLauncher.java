package org.spongepowered.asm.service.modlauncher;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.TypesafeMap;
import cpw.mods.modlauncher.api.IEnvironment.Keys;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Optional;
import org.spongepowered.asm.launch.IClassProcessor;
import org.spongepowered.asm.launch.platform.container.ContainerHandleModLauncher;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.IAdviceProvider;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.IFeatureValidator;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.IMixinInternal;
import org.spongepowered.asm.service.ITransformerProvider;
import org.spongepowered.asm.service.MixinServiceAbstract;
import org.spongepowered.asm.util.IConsumer;
import org.spongepowered.asm.util.VersionNumber;
import org.spongepowered.include.com.google.common.collect.ImmutableList;

public class MixinServiceModLauncher extends MixinServiceAbstract {
   private static final VersionNumber MODLAUNCHER_4_SPECIFICATION_VERSION = VersionNumber.parse("4.0");
   private static final VersionNumber MODLAUNCHER_9_SPECIFICATION_VERSION = VersionNumber.parse("8.0");
   private IClassProvider classProvider;
   private IClassBytecodeProvider bytecodeProvider;
   private MixinTransformationHandler transformationHandler;
   private ModLauncherClassTracker classTracker;
   private ModLauncherAuditTrail auditTrail;
   private IConsumer<MixinEnvironment.Phase> phaseConsumer;
   private volatile boolean initialised;
   private ContainerHandleModLauncher rootContainer;
   private MixinEnvironment.CompatibilityLevel minCompatibilityLevel;

   public MixinServiceModLauncher() {
      this.minCompatibilityLevel = MixinEnvironment.CompatibilityLevel.JAVA_8;
      VersionNumber apiVersion = getModLauncherApiVersion();
      if (apiVersion.compareTo(MODLAUNCHER_9_SPECIFICATION_VERSION) >= 0) {
         this.createRootContainer("org.spongepowered.asm.launch.platform.container.ContainerHandleModLauncherEx");
         this.minCompatibilityLevel = MixinEnvironment.CompatibilityLevel.JAVA_16;
      } else {
         this.createRootContainer("org.spongepowered.asm.launch.platform.container.ContainerHandleModLauncher");
      }

   }

   public void onInit(IClassBytecodeProvider bytecodeProvider) {
      if (this.initialised) {
         throw new IllegalStateException("Already initialised");
      } else {
         this.initialised = true;
         this.bytecodeProvider = bytecodeProvider;
      }
   }

   private void createRootContainer(String rootContainerClassName) {
      try {
         Class<?> clRootContainer = this.getClassProvider().findClass(rootContainerClassName);
         Constructor<?> ctor = clRootContainer.getDeclaredConstructor(String.class);
         this.rootContainer = (ContainerHandleModLauncher)ctor.newInstance(this.getName());
      } catch (ReflectiveOperationException ex) {
         ex.printStackTrace();
      }

   }

   public void onStartup() {
      this.phaseConsumer.accept(MixinEnvironment.Phase.DEFAULT);
   }

   public void offer(IMixinInternal internal) {
      if (internal instanceof IMixinTransformerFactory) {
         this.getTransformationHandler().offer((IMixinTransformerFactory)internal);
      }

      super.offer(internal);
   }

   public void wire(MixinEnvironment.Phase phase, IConsumer<MixinEnvironment.Phase> phaseConsumer) {
      super.wire(phase, phaseConsumer);
      this.phaseConsumer = phaseConsumer;
   }

   public String getName() {
      return "ModLauncher";
   }

   public MixinEnvironment.CompatibilityLevel getMinCompatibilityLevel() {
      return this.minCompatibilityLevel;
   }

   protected ILogger createLogger(String name) {
      return new LoggerAdapterLog4j2(name);
   }

   public boolean isValid() {
      try {
         VersionNumber apiVersion = getModLauncherApiVersion();
         return apiVersion.compareTo(MODLAUNCHER_4_SPECIFICATION_VERSION) >= 0;
      } catch (Throwable var2) {
         return false;
      }
   }

   public IClassProvider getClassProvider() {
      if (this.classProvider == null) {
         this.classProvider = new ModLauncherClassProvider();
      }

      return this.classProvider;
   }

   public IClassBytecodeProvider getBytecodeProvider() {
      if (this.bytecodeProvider == null) {
         throw new IllegalStateException("Service initialisation incomplete");
      } else {
         return this.bytecodeProvider;
      }
   }

   public ITransformerProvider getTransformerProvider() {
      return null;
   }

   public IClassTracker getClassTracker() {
      if (this.classTracker == null) {
         this.classTracker = new ModLauncherClassTracker();
      }

      return this.classTracker;
   }

   public IMixinAuditTrail getAuditTrail() {
      if (this.auditTrail == null) {
         this.auditTrail = new ModLauncherAuditTrail();
      }

      return this.auditTrail;
   }

   public IFeatureValidator getFeatureValidator() {
      return IFeatureValidator.ALLOW_ALL;
   }

   public IAdviceProvider getAdviceProvider() {
      return IAdviceProvider.GENERIC;
   }

   private MixinTransformationHandler getTransformationHandler() {
      if (this.transformationHandler == null) {
         this.transformationHandler = new MixinTransformationHandler();
      }

      return this.transformationHandler;
   }

   public Collection<String> getPlatformAgents() {
      return ImmutableList.<String>of("org.spongepowered.asm.launch.platform.MixinPlatformAgentMinecraftForge");
   }

   public ContainerHandleModLauncher getPrimaryContainer() {
      return this.rootContainer;
   }

   public InputStream getResourceAsStream(String name) {
      return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
   }

   public Collection<IClassProcessor> getProcessors() {
      return ImmutableList.<IClassProcessor>of(this.getTransformationHandler(), (IClassProcessor)this.getClassTracker());
   }

   private static VersionNumber getModLauncherApiVersion() {
      TypesafeMap.Key<String> versionProperty = (TypesafeMap.Key)Keys.MLSPEC_VERSION.get();
      Optional<String> version = Launcher.INSTANCE.environment().getProperty(versionProperty);
      if (!version.isPresent()) {
         version = Optional.ofNullable(ITransformationService.class.getPackage().getSpecificationVersion());
      }

      return (VersionNumber)version.map(VersionNumber::parse).orElse(VersionNumber.NONE);
   }
}
