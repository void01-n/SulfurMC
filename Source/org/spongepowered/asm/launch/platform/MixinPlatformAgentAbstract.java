package org.spongepowered.asm.launch.platform;

import java.lang.reflect.Method;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.IConsumer;

public abstract class MixinPlatformAgentAbstract implements IMixinPlatformAgent {
   protected static final ILogger logger = MixinService.getService().getLogger("mixin");
   protected MixinPlatformManager manager;
   protected IContainerHandle handle;

   protected MixinPlatformAgentAbstract() {
   }

   public IMixinPlatformAgent.AcceptResult accept(MixinPlatformManager manager, IContainerHandle handle) {
      this.manager = manager;
      this.handle = handle;
      return IMixinPlatformAgent.AcceptResult.ACCEPTED;
   }

   public String getPhaseProvider() {
      return null;
   }

   public void prepare() {
   }

   public void initPrimaryContainer() {
   }

   public void inject() {
   }

   public String toString() {
      return String.format("PlatformAgent[%s:%s]", this.getClass().getSimpleName(), this.handle);
   }

   protected static String invokeStringMethod(ClassLoader classLoader, String className, String methodName) {
      try {
         Class<?> clazz = Class.forName(className, false, classLoader);
         Method method = clazz.getDeclaredMethod(methodName);
         return ((Enum)method.invoke((Object)null)).name();
      } catch (Exception var5) {
         return null;
      }
   }

   /** @deprecated */
   @Deprecated
   public void wire(MixinEnvironment.Phase phase, IConsumer<MixinEnvironment.Phase> phaseConsumer) {
   }

   /** @deprecated */
   @Deprecated
   public void unwire() {
   }
}
