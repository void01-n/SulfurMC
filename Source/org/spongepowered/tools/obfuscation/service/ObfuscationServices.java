package org.spongepowered.tools.obfuscation.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import javax.tools.Diagnostic.Kind;
import org.spongepowered.include.com.google.common.base.Joiner;
import org.spongepowered.tools.obfuscation.ObfuscationType;
import org.spongepowered.tools.obfuscation.interfaces.IMessagerEx;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;

public final class ObfuscationServices {
   private static ObfuscationServices instance;
   private final ServiceLoader<IObfuscationService> serviceLoader = ServiceLoader.load(IObfuscationService.class, this.getClass().getClassLoader());
   private final Set<IObfuscationService> services = new HashSet();
   private boolean providerInitDone = false;

   private ObfuscationServices() {
   }

   public static ObfuscationServices getInstance() {
      if (instance == null) {
         instance = new ObfuscationServices();
      }

      return instance;
   }

   public void initProviders(IMixinAnnotationProcessor ap) {
      if (!this.providerInitDone) {
         this.providerInitDone = true;
         boolean defaultIsPresent = false;
         Map<String, Set<String>> supportedTypes = new LinkedHashMap();

         try {
            for(IObfuscationService service : this.serviceLoader) {
               if (!this.services.contains(service)) {
                  this.services.add(service);
                  String serviceName = service.getClass().getSimpleName();
                  Collection<ObfuscationTypeDescriptor> obfTypes = service.getObfuscationTypes(ap);
                  if (obfTypes != null) {
                     for(ObfuscationTypeDescriptor obfType : obfTypes) {
                        try {
                           ObfuscationType type = ObfuscationType.create(obfType, ap);
                           Set<String> types = (Set)supportedTypes.get(serviceName);
                           if (types == null) {
                              supportedTypes.put(serviceName, types = new LinkedHashSet());
                           }

                           types.add(type.getKey());
                           defaultIsPresent |= type.isDefault();
                        } catch (Exception ex) {
                           ex.printStackTrace();
                        }
                     }
                  }
               }
            }
         } catch (ServiceConfigurationError serviceError) {
            ap.printMessage(Kind.ERROR, serviceError.getClass().getSimpleName() + ": " + serviceError.getMessage());
            serviceError.printStackTrace();
         }

         if (supportedTypes.size() > 0) {
            StringBuilder sb = new StringBuilder("Supported obfuscation types:");

            for(Map.Entry<String, Set<String>> supportedType : supportedTypes.entrySet()) {
               sb.append(' ').append((String)supportedType.getKey()).append(" supports [").append(Joiner.on(',').join((Iterable)supportedType.getValue())).append(']');
            }

            ap.printMessage(IMessagerEx.MessageType.INFO, sb.toString());
         }

         if (!defaultIsPresent) {
            String defaultEnv = ap.getOption("defaultObfuscationEnv");
            if (defaultEnv == null) {
               ap.printMessage(Kind.WARNING, "No default obfuscation environment was specified and \"searge\" is not available. Please ensure defaultObfuscationEnv is specified in your build configuration");
            } else {
               ap.printMessage(Kind.WARNING, "Specified default obfuscation environment \"" + defaultEnv.toLowerCase(Locale.ROOT) + "\" was not defined. This probably means your build configuration is out of date or a required service is missing");
            }
         }

      }
   }

   public Set<String> getSupportedOptions() {
      Set<String> supportedOptions = new HashSet();

      for(IObfuscationService provider : this.services) {
         Set<String> options = provider.getSupportedOptions();
         if (options != null) {
            supportedOptions.addAll(options);
         }
      }

      return supportedOptions;
   }

   public IObfuscationService getService(Class<? extends IObfuscationService> serviceClass) {
      for(IObfuscationService service : this.services) {
         if (serviceClass.getName().equals(service.getClass().getName())) {
            return service;
         }
      }

      return null;
   }
}
