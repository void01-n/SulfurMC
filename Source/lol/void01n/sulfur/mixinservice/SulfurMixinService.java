package lol.void01n.sulfur.mixinservice;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lol.void01n.sulfur.classloader.SulfurClassLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.launch.platform.container.ContainerHandleURI;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.IAdviceProvider;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.IFeatureValidator;
import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.IMixinInternal;
import org.spongepowered.asm.service.IPropertyKey;
import org.spongepowered.asm.service.ITransformer;
import org.spongepowered.asm.service.ITransformerProvider;
import org.spongepowered.asm.service.MixinServiceAbstract;

public final class SulfurMixinService extends MixinServiceAbstract implements IClassProvider, IClassBytecodeProvider, ITransformerProvider, IClassTracker, IGlobalPropertyService {
   private static final boolean DEBUG = System.getProperties().containsKey("sulfur.debug");
   private static final Map<String, Object> GLOBAL_PROPERTIES = new ConcurrentHashMap();
   private static volatile SulfurClassLoader classLoader;
   private static volatile IMixinTransformer transformer;
   private final Set<String> neoForgeMixinConfigs = new HashSet();
   private final Set<String> quiltMixinConfigs = new HashSet();

   public static void bind(SulfurClassLoader loader) {
      classLoader = loader;
   }

   private static SulfurClassLoader requireClassLoader() {
      SulfurClassLoader cl = classLoader;
      if (cl == null) {
         throw new IllegalStateException("SulfurMixinService.bind(SulfurClassLoader) must be called by SulfurBootstrap before Mixin subsystem boot — no SulfurClassLoader is bound yet.");
      } else {
         return cl;
      }
   }

   public static byte[] applyMixinTransform(String className, byte[] classBytes) {
      IMixinTransformer t = transformer;
      if (t == null) {
         return classBytes;
      } else {
         try {
            return t.transformClassBytes(className, className, classBytes);
         } catch (Throwable ex) {
            throw new RuntimeException("Mixin transformation of " + className + " failed", ex);
         }
      }
   }

   public void registerNeoForgeMixinConfig(String configPath) {
      if (this.neoForgeMixinConfigs.add(configPath)) {
         try {
            MixinBootstrap.init();
            Mixins.addConfiguration(configPath);
            if (DEBUG) {
               System.out.println("sulfur: registered NeoForge-side mixin config: " + configPath);
            }
         } catch (Exception e) {
            System.err.println("sulfur: failed registering mixin config '" + configPath + "': " + String.valueOf(e));
         }
      }

   }

   public void registerQuiltMixinConfig(String configPath) {
      if (this.quiltMixinConfigs.add(configPath)) {
         try {
            MixinBootstrap.init();
            Mixins.addConfiguration(configPath);
            if (DEBUG) {
               System.out.println("sulfur: registered Quilt-side mixin config: " + configPath);
            }
         } catch (Exception e) {
            System.err.println("sulfur: failed registering mixin config '" + configPath + "': " + String.valueOf(e));
         }
      }

   }

   public Set<String> allMixinConfigs() {
      LinkedHashSet<String> all = new LinkedHashSet();
      all.addAll(this.neoForgeMixinConfigs);
      all.addAll(this.quiltMixinConfigs);
      return Set.copyOf(all);
   }

   public String getName() {
      return "Sulfur";
   }

   public boolean isValid() {
      return true;
   }

   public void offer(IMixinInternal internal) {
      super.offer(internal);
      if (internal instanceof IMixinTransformerFactory) {
         transformer = ((IMixinTransformerFactory)internal).createTransformer();
         if (DEBUG) {
            System.out.println("sulfur: received IMixinTransformerFactory, transformer bound");
         }
      }

   }

   public IClassProvider getClassProvider() {
      return this;
   }

   public IClassBytecodeProvider getBytecodeProvider() {
      return this;
   }

   public ITransformerProvider getTransformerProvider() {
      return this;
   }

   public IClassTracker getClassTracker() {
      return this;
   }

   public IMixinAuditTrail getAuditTrail() {
      return null;
   }

   public IFeatureValidator getFeatureValidator() {
      return IFeatureValidator.ALLOW_ALL;
   }

   public IAdviceProvider getAdviceProvider() {
      return IAdviceProvider.GENERIC;
   }

   public Collection<String> getPlatformAgents() {
      return Collections.singletonList("org.spongepowered.asm.launch.platform.MixinPlatformAgentDefault");
   }

   public IContainerHandle getPrimaryContainer() {
      try {
         return new ContainerHandleURI(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
      } catch (URISyntaxException e) {
         throw new RuntimeException(e);
      }
   }

   public InputStream getResourceAsStream(String name) {
      return requireClassLoader().getResourceAsStream(name);
   }

   public byte[] getClassBytes(String name, String transformedName) throws IOException {
      return requireClassLoader().getRawClassBytes(name);
   }

   public byte[] getClassBytes(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
      byte[] bytes = requireClassLoader().getRawClassBytes(name);
      if (bytes != null) {
         return bytes;
      } else {
         throw new ClassNotFoundException(name);
      }
   }

   public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
      return this.getClassNode(name, true);
   }

   public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
      return this.getClassNode(name, runTransformers, 0);
   }

   public ClassNode getClassNode(String name, boolean runTransformers, int readerFlags) throws ClassNotFoundException, IOException {
      ClassReader reader = new ClassReader(this.getClassBytes(name, runTransformers));
      ClassNode node = new ClassNode();
      reader.accept(node, readerFlags);
      return node;
   }

   public URL[] getClassPath() {
      return new URL[0];
   }

   public Class<?> findClass(String name) throws ClassNotFoundException {
      return requireClassLoader().loadClass(name);
   }

   public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
      return Class.forName(name, initialize, requireClassLoader());
   }

   public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
      return Class.forName(name, initialize, SulfurMixinService.class.getClassLoader());
   }

   public Collection<ITransformer> getTransformers() {
      return Collections.emptyList();
   }

   public Collection<ITransformer> getDelegatedTransformers() {
      return Collections.emptyList();
   }

   public void addTransformerExclusion(String name) {
   }

   public void registerInvalidClass(String className) {
   }

   public boolean isClassLoaded(String className) {
      return requireClassLoader().findLoadedSulfurClass(className) != null;
   }

   public String getClassRestrictions(String className) {
      return "";
   }

   public IPropertyKey resolveKey(String name) {
      return new SulfurPropertyKey(name);
   }

   public <T> T getProperty(IPropertyKey key) {
      return (T)GLOBAL_PROPERTIES.get(key.toString());
   }

   public void setProperty(IPropertyKey key, Object value) {
      GLOBAL_PROPERTIES.put(key.toString(), value);
   }

   public <T> T getProperty(IPropertyKey key, T defaultValue) {
      Object v = GLOBAL_PROPERTIES.get(key.toString());
      return (T)(v != null ? v : defaultValue);
   }

   public String getPropertyString(IPropertyKey key, String defaultValue) {
      Object v = GLOBAL_PROPERTIES.get(key.toString());
      return v != null ? v.toString() : defaultValue;
   }

   public MixinEnvironment.CompatibilityLevel getMinCompatibilityLevel() {
      return MixinEnvironment.CompatibilityLevel.JAVA_8;
   }

   public MixinEnvironment.CompatibilityLevel getMaxCompatibilityLevel() {
      return MixinEnvironment.CompatibilityLevel.JAVA_21;
   }

   private static final class SulfurPropertyKey implements IPropertyKey {
      final String name;

      SulfurPropertyKey(String name) {
         this.name = name;
      }

      public String toString() {
         return this.name;
      }

      public boolean equals(Object o) {
         return o instanceof SulfurPropertyKey && ((SulfurPropertyKey)o).name.equals(this.name);
      }

      public int hashCode() {
         return this.name.hashCode();
      }
   }
}
