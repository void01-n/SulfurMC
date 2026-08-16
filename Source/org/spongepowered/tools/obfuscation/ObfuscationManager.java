package org.spongepowered.tools.obfuscation;

import java.util.ArrayList;
import java.util.List;
import javax.tools.Diagnostic.Kind;
import org.spongepowered.tools.obfuscation.interfaces.IMessagerEx;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.interfaces.IObfuscationDataProvider;
import org.spongepowered.tools.obfuscation.interfaces.IObfuscationManager;
import org.spongepowered.tools.obfuscation.interfaces.IReferenceManager;
import org.spongepowered.tools.obfuscation.mapping.IMappingConsumer;
import org.spongepowered.tools.obfuscation.service.ObfuscationServices;

public class ObfuscationManager implements IObfuscationManager {
   private final IMixinAnnotationProcessor ap;
   private final List<ObfuscationEnvironment> environments = new ArrayList();
   private final IObfuscationDataProvider obfs;
   private final IReferenceManager refs;
   private final List<IMappingConsumer> consumers = new ArrayList();
   private boolean initDone;

   public ObfuscationManager(IMixinAnnotationProcessor ap) {
      this.ap = ap;
      this.obfs = new ObfuscationDataProvider(ap, this.environments);
      this.refs = new ReferenceManager(ap, this.environments);
   }

   public void init() {
      if (!this.initDone) {
         this.initDone = true;
         ObfuscationServices.getInstance().initProviders(this.ap);

         for(ObfuscationType obfType : ObfuscationType.types()) {
            if (obfType.isSupported()) {
               this.environments.add(obfType.createEnvironment());
            }
         }

         IMixinAnnotationProcessor.CompilerEnvironment compilerEnv = this.ap.getCompilerEnvironment();
         if (this.environments.size() == 0 && compilerEnv.isDevelopmentEnvironment()) {
            IMessagerEx.MessageType.setPrefix("(Mixin AP) ");
            this.ap.printMessage(IMessagerEx.MessageType.NOTE, "No obfuscation data are available and an IDE (" + compilerEnv.getFriendlyName() + ") was detected, quenching error levels ");
            IMessagerEx.MessageType.NO_OBFDATA_FOR_CLASS.quench(Kind.NOTE);
            IMessagerEx.MessageType.NO_OBFDATA_FOR_ACCESSOR.quench(Kind.NOTE);
            IMessagerEx.MessageType.NO_OBFDATA_FOR_CTOR.quench(Kind.NOTE);
            IMessagerEx.MessageType.NO_OBFDATA_FOR_TARGET.quench(Kind.NOTE);
            IMessagerEx.MessageType.NO_OBFDATA_FOR_OVERWRITE.quench(Kind.NOTE);
            IMessagerEx.MessageType.NO_OBFDATA_FOR_STATIC_OVERWRITE.quench(Kind.NOTE);
            IMessagerEx.MessageType.NO_OBFDATA_FOR_FIELD.quench(Kind.NOTE);
            IMessagerEx.MessageType.NO_OBFDATA_FOR_METHOD.quench(Kind.NOTE);
            IMessagerEx.MessageType.NO_OBFDATA_FOR_SHADOW.quench(Kind.NOTE);
            IMessagerEx.MessageType.NO_OBFDATA_FOR_SIMULATED_SHADOW.quench(Kind.NOTE);
            IMessagerEx.MessageType.NO_OBFDATA_FOR_SOFT_IMPLEMENTS.quench(Kind.NOTE);
            IMessagerEx.MessageType.PARENT_VALIDATOR.quench(Kind.WARNING);
            IMessagerEx.MessageType.TARGET_VALIDATOR.quench(Kind.WARNING);
         }

      }
   }

   public IObfuscationDataProvider getDataProvider() {
      return this.obfs;
   }

   public IReferenceManager getReferenceManager() {
      return this.refs;
   }

   public IMappingConsumer createMappingConsumer() {
      Mappings mappings = new Mappings();
      this.consumers.add(mappings);
      return mappings;
   }

   public List<ObfuscationEnvironment> getEnvironments() {
      return this.environments;
   }

   public void writeMappings() {
      for(ObfuscationEnvironment env : this.environments) {
         env.writeMappings(this.consumers);
      }

   }

   public void writeReferences() {
      this.refs.write();
   }
}
