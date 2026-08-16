package org.spongepowered.asm.mixin.transformer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.throwables.CompanionPluginError;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.include.com.google.common.base.Strings;

class PluginHandle {
   private static final ILogger logger = MixinService.getService().getLogger("mixin");
   private final MixinConfig parent;
   private final IMixinConfigPlugin plugin;
   private CompatibilityMode mode;
   private Method mdPreApply;
   private Method mdPostApply;

   PluginHandle(MixinConfig parent, IMixinService service, String pluginClassName) {
      this.mode = PluginHandle.CompatibilityMode.NORMAL;
      IMixinConfigPlugin plugin = null;
      if (!Strings.isNullOrEmpty(pluginClassName)) {
         try {
            Class<?> pluginClass = service.getClassProvider().findClass(pluginClassName, true);
            plugin = (IMixinConfigPlugin)pluginClass.getDeclaredConstructor().newInstance();
         } catch (Throwable th) {
            logger.error("Error loading companion plugin class [{}] for mixin config [{}]. The plugin may be out of date: {}:{}", pluginClassName, parent, th.getClass().getSimpleName(), th.getMessage(), th);
            plugin = null;
         }
      }

      this.parent = parent;
      this.plugin = plugin;
   }

   IMixinConfigPlugin get() {
      return this.plugin;
   }

   boolean isAvailable() {
      return this.plugin != null;
   }

   void onLoad(String mixinPackage) {
      if (this.plugin != null) {
         this.plugin.onLoad(mixinPackage);
      }

   }

   String getRefMapperConfig() {
      return this.plugin != null ? this.plugin.getRefMapperConfig() : null;
   }

   List<String> getMixins() {
      return this.plugin != null ? this.plugin.getMixins() : null;
   }

   boolean shouldApplyMixin(String targetName, String className) {
      return this.plugin == null || this.plugin.shouldApplyMixin(targetName, className);
   }

   public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, MixinInfo mixinInfo) throws Exception {
      if (this.plugin != null) {
         if (this.mode == PluginHandle.CompatibilityMode.FAILED) {
            throw new IllegalStateException("Companion plugin failure for [" + this.parent + "] plugin [" + this.plugin.getClass() + "]");
         } else if (this.mode == PluginHandle.CompatibilityMode.COMPATIBLE) {
            try {
               this.applyLegacy(this.mdPreApply, targetClassName, targetClass, mixinClassName, mixinInfo);
            } catch (Exception ex) {
               this.mode = PluginHandle.CompatibilityMode.FAILED;
               throw ex;
            }
         } else {
            try {
               this.plugin.preApply(targetClassName, targetClass, mixinClassName, mixinInfo);
            } catch (AbstractMethodError var7) {
               this.mode = PluginHandle.CompatibilityMode.COMPATIBLE;
               this.initReflection();
               this.preApply(targetClassName, targetClass, mixinClassName, mixinInfo);
            }

         }
      }
   }

   public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, MixinInfo mixinInfo) throws Exception {
      if (this.plugin != null) {
         if (this.mode == PluginHandle.CompatibilityMode.FAILED) {
            throw new IllegalStateException("Companion plugin failure for [" + this.parent + "] plugin [" + this.plugin.getClass() + "]");
         } else if (this.mode == PluginHandle.CompatibilityMode.COMPATIBLE) {
            try {
               this.applyLegacy(this.mdPostApply, targetClassName, targetClass, mixinClassName, mixinInfo);
            } catch (Exception ex) {
               this.mode = PluginHandle.CompatibilityMode.FAILED;
               throw ex;
            }
         } else {
            try {
               this.plugin.postApply(targetClassName, targetClass, mixinClassName, mixinInfo);
            } catch (AbstractMethodError var7) {
               this.mode = PluginHandle.CompatibilityMode.COMPATIBLE;
               this.initReflection();
               this.postApply(targetClassName, targetClass, mixinClassName, mixinInfo);
            }

         }
      }
   }

   private void initReflection() {
      if (this.mdPreApply == null) {
         try {
            Class<?> pluginClass = this.plugin.getClass();
            this.mdPreApply = pluginClass.getMethod("preApply", String.class, org.spongepowered.asm.lib.tree.ClassNode.class, String.class, IMixinInfo.class);
            this.mdPostApply = pluginClass.getMethod("postApply", String.class, org.spongepowered.asm.lib.tree.ClassNode.class, String.class, IMixinInfo.class);
         } catch (Throwable th) {
            logger.catching(th);
         }

      }
   }

   private void applyLegacy(Method method, String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
      try {
         method.invoke(this.plugin, targetClassName, new org.spongepowered.asm.lib.tree.ClassNode(targetClass), mixinClassName, mixinInfo);
      } catch (LinkageError err) {
         throw new CompanionPluginError(this.apiError("Accessing [" + err.getMessage() + "]"), err);
      } catch (IllegalAccessException ex) {
         throw new CompanionPluginError(this.apiError("Fallback failed [" + ex.getMessage() + "]"), ex);
      } catch (IllegalArgumentException ex) {
         throw new CompanionPluginError(this.apiError("Fallback failed [" + ex.getMessage() + "]"), ex);
      } catch (InvocationTargetException ex) {
         Throwable th = (Throwable)(ex.getCause() != null ? ex.getCause() : ex);
         throw new CompanionPluginError(this.apiError("Fallback failed [" + th.getMessage() + "]"), th);
      }
   }

   private String apiError(String message) {
      return String.format("Companion plugin attempted to use a deprected API in [%s] plugin [%s]: %s", this.parent, this.plugin.getClass().getName(), message);
   }

   static enum CompatibilityMode {
      NORMAL,
      COMPATIBLE,
      FAILED;

      // $FF: synthetic method
      private static CompatibilityMode[] $values() {
         return new CompatibilityMode[]{NORMAL, COMPATIBLE, FAILED};
      }
   }
}
