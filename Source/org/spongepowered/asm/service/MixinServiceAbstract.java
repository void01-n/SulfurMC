package org.spongepowered.asm.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.spongepowered.asm.launch.platform.IMixinPlatformAgent;
import org.spongepowered.asm.launch.platform.IMixinPlatformServiceAgent;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.LoggerAdapterDefault;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.util.IConsumer;
import org.spongepowered.asm.util.ReEntranceLock;
import org.spongepowered.include.com.google.common.collect.ImmutableList;

public abstract class MixinServiceAbstract implements IMixinService {
   protected static final String LAUNCH_PACKAGE = "org.spongepowered.asm.launch.";
   protected static final String MIXIN_PACKAGE = "org.spongepowered.asm.mixin.";
   protected static final String SERVICE_PACKAGE = "org.spongepowered.asm.service.";
   private static final Map<String, ILogger> loggers = new HashMap();
   protected final ReEntranceLock lock = new ReEntranceLock(1);
   private final Map<Class<IMixinInternal>, IMixinInternal> internals = new HashMap();
   private List<IMixinPlatformServiceAgent> serviceAgents;
   private String sideName;

   public void prepare() {
   }

   public MixinEnvironment.Phase getInitialPhase() {
      return MixinEnvironment.Phase.PREINIT;
   }

   public MixinEnvironment.CompatibilityLevel getMinCompatibilityLevel() {
      return null;
   }

   public MixinEnvironment.CompatibilityLevel getMaxCompatibilityLevel() {
      return null;
   }

   public void offer(IMixinInternal internal) {
      this.registerInternal(internal, internal.getClass());
   }

   private void registerInternal(IMixinInternal internal, Class<?> clazz) {
      for(Class<?> iface : clazz.getInterfaces()) {
         if (iface == IMixinInternal.class) {
            this.internals.put(clazz, internal);
         }

         this.registerInternal(internal, iface);
      }

   }

   protected final <T extends IMixinInternal> T getInternal(Class<T> type) {
      for(Class<IMixinInternal> internalType : this.internals.keySet()) {
         if (type.isAssignableFrom(internalType)) {
            return (T)(this.internals.get(internalType));
         }
      }

      return null;
   }

   public void init() {
      for(IMixinPlatformServiceAgent agent : this.getServiceAgents()) {
         agent.init();
      }

   }

   public void beginPhase() {
   }

   public void checkEnv(Object bootSource) {
   }

   public ReEntranceLock getReEntranceLock() {
      return this.lock;
   }

   public Collection<IContainerHandle> getMixinContainers() {
      ImmutableList.Builder<IContainerHandle> list = ImmutableList.<IContainerHandle>builder();
      this.getContainersFromAgents(list);
      return list.build();
   }

   protected final void getContainersFromAgents(ImmutableList.Builder<IContainerHandle> list) {
      for(IMixinPlatformServiceAgent agent : this.getServiceAgents()) {
         Collection<IContainerHandle> containers = agent.getMixinContainers();
         if (containers != null) {
            list.addAll(containers);
         }
      }

   }

   public final String getSideName() {
      if (this.sideName != null) {
         return this.sideName;
      } else {
         for(IMixinPlatformServiceAgent agent : this.getServiceAgents()) {
            try {
               String side = agent.getSideName();
               if (side != null) {
                  return this.sideName = side;
               }
            } catch (Exception ex) {
               this.getLogger("mixin").catching(ex);
            }
         }

         return "UNKNOWN";
      }
   }

   private List<IMixinPlatformServiceAgent> getServiceAgents() {
      if (this.serviceAgents != null) {
         return this.serviceAgents;
      } else {
         this.serviceAgents = new ArrayList();

         for(String agentClassName : this.getPlatformAgents()) {
            try {
               Class<IMixinPlatformAgent> agentClass = this.getClassProvider().findClass(agentClassName, false);
               IMixinPlatformAgent agent = (IMixinPlatformAgent)agentClass.getDeclaredConstructor().newInstance();
               if (agent instanceof IMixinPlatformServiceAgent) {
                  this.serviceAgents.add((IMixinPlatformServiceAgent)agent);
               }
            } catch (Exception ex) {
               ex.printStackTrace();
            }
         }

         return this.serviceAgents;
      }
   }

   public synchronized ILogger getLogger(String name) {
      ILogger logger = (ILogger)loggers.get(name);
      if (logger == null) {
         loggers.put(name, logger = this.createLogger(name));
      }

      return logger;
   }

   protected ILogger createLogger(String name) {
      return new LoggerAdapterDefault(name);
   }

   /** @deprecated */
   @Deprecated
   public void wire(MixinEnvironment.Phase phase, IConsumer<MixinEnvironment.Phase> phaseConsumer) {
      for(IMixinPlatformServiceAgent agent : this.getServiceAgents()) {
         agent.wire(phase, phaseConsumer);
      }

   }

   /** @deprecated */
   @Deprecated
   public void unwire() {
      for(IMixinPlatformServiceAgent agent : this.getServiceAgents()) {
         agent.unwire();
      }

   }
}
