package org.spongepowered.asm.launch;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.platform.CommandLineOptions;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.service.modlauncher.MixinServiceModLauncher;
import org.spongepowered.asm.service.modlauncher.ModLauncherAuditTrail;
import org.spongepowered.asm.transformers.MixinClassReader;
import org.spongepowered.include.com.google.common.io.Resources;

public class MixinLaunchPluginLegacy implements ILaunchPluginService, IClassBytecodeProvider {
   public static final String NAME = "mixin";
   private final List<IClassProcessor> processors = new ArrayList();
   private List<String> commandLineMixins;
   private ILaunchPluginService.ITransformerLoader transformerLoader;
   private MixinServiceModLauncher service;
   private ModLauncherAuditTrail auditTrail;

   public String name() {
      return "mixin";
   }

   public EnumSet<ILaunchPluginService.Phase> handlesClass(Type classType, boolean isEmpty) {
      throw new IllegalStateException("Outdated ModLauncher");
   }

   public boolean processClass(ILaunchPluginService.Phase phase, ClassNode classNode, Type classType) {
      throw new IllegalStateException("Outdated ModLauncher");
   }

   public EnumSet<ILaunchPluginService.Phase> handlesClass(Type classType, boolean isEmpty, String reason) {
      if ("mixin".equals(reason)) {
         return Phases.NONE;
      } else {
         EnumSet<ILaunchPluginService.Phase> phases = EnumSet.noneOf(ILaunchPluginService.Phase.class);
         synchronized(this.processors) {
            for(IClassProcessor postProcessor : this.processors) {
               EnumSet<ILaunchPluginService.Phase> processorVote = postProcessor.handlesClass(classType, isEmpty, reason);
               if (processorVote != null) {
                  phases.addAll(processorVote);
               }
            }

            return phases;
         }
      }
   }

   public boolean processClass(ILaunchPluginService.Phase phase, ClassNode classNode, Type classType, String reason) {
      boolean processed = false;
      synchronized(this.processors) {
         for(IClassProcessor processor : this.processors) {
            processed |= processor.processClass(phase, classNode, classType, reason);
         }

         return processed;
      }
   }

   void init(IEnvironment environment, List<String> commandLineMixins) {
      IMixinService service = MixinService.getService();
      if (!(service instanceof MixinServiceModLauncher)) {
         throw new IllegalStateException("Unsupported service type for ModLauncher Mixin Service");
      } else {
         this.service = (MixinServiceModLauncher)service;
         this.auditTrail = (ModLauncherAuditTrail)this.service.getAuditTrail();
         synchronized(this.processors) {
            this.processors.addAll(this.service.getProcessors());
         }

         this.commandLineMixins = commandLineMixins;
         this.service.onInit(this);
      }
   }

   public void customAuditConsumer(String className, Consumer<String[]> auditDataAcceptor) {
      if (this.auditTrail != null) {
         this.auditTrail.setConsumer(className, auditDataAcceptor);
      }

   }

   /** @deprecated */
   @Deprecated
   public void addResource(Path resource, String name) {
      this.service.getPrimaryContainer().addResource(name, resource);
   }

   public void offerResource(Path resource, String name) {
      this.service.getPrimaryContainer().addResource(name, resource);
   }

   public void addResources(List resources) {
      this.service.getPrimaryContainer().addResources(resources);
   }

   public <T> T getExtension() {
      return null;
   }

   public void initializeLaunch(ILaunchPluginService.ITransformerLoader transformerLoader, Path[] specialPaths) {
      this.initializeLaunch(transformerLoader);
   }

   protected void initializeLaunch(ILaunchPluginService.ITransformerLoader transformerLoader) {
      this.transformerLoader = transformerLoader;
      MixinBootstrap.doInit(CommandLineOptions.of(this.commandLineMixins));
      MixinBootstrap.inject();
      this.service.onStartup();
   }

   public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
      return this.getClassNode(name, true, 0);
   }

   public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
      return this.getClassNode(name, runTransformers, 8);
   }

   public ClassNode getClassNode(String name, boolean runTransformers, int readerFlags) throws ClassNotFoundException, IOException {
      if (!runTransformers) {
         throw new IllegalArgumentException("ModLauncher service does not currently support retrieval of untransformed bytecode");
      } else {
         String canonicalName = name.replace('/', '.');
         String internalName = name.replace('.', '/');

         byte[] classBytes;
         try {
            classBytes = this.transformerLoader.buildTransformedClassNodeFor(canonicalName);
         } catch (ClassNotFoundException ex) {
            URL url = Thread.currentThread().getContextClassLoader().getResource(internalName + ".class");
            if (url == null) {
               throw ex;
            }

            try {
               classBytes = Resources.asByteSource(url).read();
            } catch (IOException var13) {
               throw ex;
            }
         }

         if (classBytes != null && classBytes.length != 0) {
            ClassNode classNode = new ClassNode();
            ClassReader classReader = new MixinClassReader(classBytes, canonicalName);
            classReader.accept(classNode, readerFlags);
            return classNode;
         } else {
            Type classType = Type.getObjectType(internalName);
            synchronized(this.processors) {
               for(IClassProcessor processor : this.processors) {
                  if (processor.generatesClass(classType)) {
                     ClassNode classNode = new ClassNode();
                     if (processor.generateClass(classType, classNode)) {
                        return classNode;
                     }
                  }
               }
            }

            throw new ClassNotFoundException(canonicalName);
         }
      }
   }
}
