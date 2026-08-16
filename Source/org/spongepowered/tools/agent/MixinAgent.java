package org.spongepowered.tools.agent;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.logging.Level;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.ext.IHotSwap;
import org.spongepowered.asm.mixin.transformer.throwables.MixinReloadException;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.service.ServiceNotAvailableError;
import org.spongepowered.asm.transformers.MixinClassReader;
import org.spongepowered.asm.util.asm.ASM;

public class MixinAgent implements IHotSwap {
   public static final byte[] ERROR_BYTECODE = new byte[]{1};
   static final MixinAgentClassLoader classLoader = new MixinAgentClassLoader();
   static Instrumentation instrumentation = null;
   private static List<MixinAgent> agents = new ArrayList();
   final IMixinTransformer classTransformer;

   public MixinAgent(IMixinTransformer classTransformer) {
      this.classTransformer = classTransformer;
      agents.add(this);
      if (instrumentation != null) {
         this.initTransformer();
      }

   }

   private void initTransformer() {
      instrumentation.addTransformer(new Transformer(), true);
   }

   public void registerMixinClass(String name) {
      classLoader.addMixinClass(name);
   }

   public void registerTargetClass(String name, ClassNode classNode) {
      classLoader.addTargetClass(name, classNode);
   }

   public static void init(Instrumentation instrumentation) {
      MixinAgent.instrumentation = instrumentation;
      if (!MixinAgent.instrumentation.isRedefineClassesSupported()) {
         log(Level.ERROR, "The instrumentation doesn't support re-definition of classes");
      }

      for(MixinAgent agent : agents) {
         agent.initTransformer();
      }

   }

   public static void premain(String arg, Instrumentation instrumentation) {
      System.setProperty("mixin.hotSwap", "true");
      init(instrumentation);
   }

   public static void agentmain(String arg, Instrumentation instrumentation) {
      init(instrumentation);
   }

   public static void log(Level level, String message, Object... params) {
      try {
         MixinService.getService().getLogger("mixin.agent").log(level, message, params);
      } catch (ServiceNotAvailableError var4) {
         System.err.printf("MixinAgent: %s: %s", level.name(), String.format(message, params));
      }

   }

   class Transformer implements ClassFileTransformer {
      public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain domain, byte[] classfileBuffer) throws IllegalClassFormatException {
         if (classBeingRedefined == null) {
            return null;
         } else {
            byte[] mixinBytecode = MixinAgent.classLoader.getFakeMixinBytecode(classBeingRedefined);
            if (mixinBytecode != null) {
               ClassNode classNode = new ClassNode(ASM.API_VERSION);
               ClassReader cr = new MixinClassReader(classfileBuffer, className);
               cr.accept(classNode, 8);
               List<String> targets = this.reloadMixin(className, classNode);
               return targets != null && this.reApplyMixins(targets) ? mixinBytecode : MixinAgent.ERROR_BYTECODE;
            } else {
               try {
                  MixinAgent.log(Level.INFO, "Redefining class {}", className);
                  return MixinAgent.this.classTransformer.transformClassBytes((String)null, className.replace('/', '.'), classfileBuffer);
               } catch (Throwable th) {
                  MixinAgent.log(Level.ERROR, "Error while re-transforming class {}", className, th);
                  return MixinAgent.ERROR_BYTECODE;
               }
            }
         }
      }

      private List<String> reloadMixin(String className, ClassNode classNode) {
         MixinAgent.log(Level.INFO, "Redefining mixin {}", className);

         try {
            return MixinAgent.this.classTransformer.reload(className.replace('/', '.'), classNode);
         } catch (MixinReloadException e) {
            MixinAgent.log(Level.ERROR, "Mixin {} cannot be reloaded, needs a restart to be applied: {} ", e.getMixinInfo(), e.getMessage());
         } catch (Throwable th) {
            MixinAgent.log(Level.ERROR, "Error while finding targets for mixin {}", className, th);
         }

         return null;
      }

      private boolean reApplyMixins(List<String> targets) {
         IMixinService service = MixinService.getService();

         for(String target : targets) {
            String targetName = target.replace('/', '.');
            MixinAgent.log(Level.DEBUG, "Re-transforming target class {}", target);

            try {
               Class<?> targetClass = service.getClassProvider().findClass(targetName);
               byte[] targetBytecode = MixinAgent.classLoader.getOriginalTargetBytecode(targetName);
               if (targetBytecode == null) {
                  MixinAgent.log(Level.ERROR, "Target class {} bytecode is not registered", targetName);
                  return false;
               }

               targetBytecode = MixinAgent.this.classTransformer.transformClassBytes((String)null, targetName, targetBytecode);
               MixinAgent.instrumentation.redefineClasses(new ClassDefinition[]{new ClassDefinition(targetClass, targetBytecode)});
            } catch (Throwable th) {
               MixinAgent.log(Level.ERROR, "Error while re-transforming target class {}", target, th);
               return false;
            }
         }

         return true;
      }
   }
}
