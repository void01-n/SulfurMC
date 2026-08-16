package org.spongepowered.asm.mixin.transformer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.spongepowered.asm.launch.MixinInitialisationError;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.Level;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigSource;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorDynamic;
import org.spongepowered.asm.mixin.injection.selectors.TargetSelector;
import org.spongepowered.asm.mixin.refmap.IClassReferenceMapper;
import org.spongepowered.asm.mixin.refmap.IReferenceMapper;
import org.spongepowered.asm.mixin.refmap.ReferenceMapper;
import org.spongepowered.asm.mixin.refmap.RemappingReferenceMapper;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.VersionNumber;
import org.spongepowered.include.com.google.common.base.Joiner;
import org.spongepowered.include.com.google.common.base.Strings;
import org.spongepowered.include.com.google.common.collect.ImmutableList;
import org.spongepowered.include.com.google.gson.Gson;
import org.spongepowered.include.com.google.gson.annotations.SerializedName;

final class MixinConfig implements Comparable<MixinConfig>, IMixinConfig {
   private static int configOrder = 0;
   private static final Set<String> globalMixinList = new HashSet();
   private final ILogger logger = MixinService.getService().getLogger("mixin");
   private final transient Map<String, List<MixinInfo>> mixinMapping = new HashMap();
   private final transient Set<String> unhandledTargets = new HashSet();
   private final transient List<MixinInfo> pendingMixins = new ArrayList();
   private final transient List<MixinInfo> mixins = new ArrayList();
   private transient Config handle;
   private transient MixinConfig parent;
   @SerializedName("parent")
   private String parentName;
   @SerializedName("target")
   private String selector;
   @SerializedName("minVersion")
   private String version;
   @SerializedName("requiredFeatures")
   private List<String> requiredFeatures;
   @SerializedName("compatibilityLevel")
   private String compatibility;
   @SerializedName("required")
   private Boolean requiredValue;
   private transient boolean required;
   @SerializedName("priority")
   private int priority = -1;
   @SerializedName("mixinPriority")
   private int mixinPriority = -1;
   @SerializedName("package")
   private String mixinPackage;
   @SerializedName("mixins")
   private List<String> mixinClasses;
   @SerializedName("client")
   private List<String> mixinClassesClient;
   @SerializedName("server")
   private List<String> mixinClassesServer;
   @SerializedName("setSourceFile")
   private boolean setSourceFile = false;
   @SerializedName("refmap")
   private String refMapperConfig;
   @SerializedName("refmapWrapper")
   private String refMapperWrapper;
   @SerializedName("verbose")
   private boolean verboseLogging;
   private final transient int order;
   private final transient List<IListener> listeners;
   private transient IMixinService service;
   private transient MixinEnvironment env;
   private transient String name;
   private transient IMixinConfigSource source;
   @SerializedName("plugin")
   private String pluginClassName;
   @SerializedName("injectors")
   private InjectorOptions injectorOptions;
   @SerializedName("overwrites")
   private OverwriteOptions overwriteOptions;
   private transient PluginHandle plugin;
   private transient IReferenceMapper refMapper;
   private transient boolean initialised;
   private transient boolean prepared;
   private transient boolean visited;
   private transient MixinEnvironment.CompatibilityLevel compatibilityLevel;
   private transient int warnedClassVersion;
   private transient Map<String, Object> decorations;

   private MixinConfig() {
      this.order = configOrder++;
      this.listeners = new ArrayList();
      this.initialised = false;
      this.prepared = false;
      this.visited = false;
      this.compatibilityLevel = MixinEnvironment.CompatibilityLevel.DEFAULT;
      this.warnedClassVersion = 0;
   }

   private boolean onLoad(IMixinService service, String name, MixinEnvironment fallbackEnvironment, IMixinConfigSource source) {
      this.service = service;
      this.name = name;
      this.source = source;
      if (!Strings.isNullOrEmpty(this.parentName)) {
         return true;
      } else {
         this.env = this.parseSelector(this.selector, fallbackEnvironment);
         this.verboseLogging |= this.env.getOption(MixinEnvironment.Option.DEBUG_VERBOSE);
         this.required = this.requiredValue != null && this.requiredValue && !this.env.getOption(MixinEnvironment.Option.IGNORE_REQUIRED);
         this.initPriority(1000, 1000);
         if (this.injectorOptions == null) {
            this.injectorOptions = new InjectorOptions();
         }

         if (this.overwriteOptions == null) {
            this.overwriteOptions = new OverwriteOptions();
         }

         return this.postInit();
      }
   }

   String getParentName() {
      return this.parentName;
   }

   boolean assignParent(Config parentConfig) {
      if (this.parent != null) {
         throw new MixinInitialisationError("Mixin config " + this.name + " was already initialised");
      } else if (parentConfig.get() == this) {
         throw new MixinInitialisationError("Mixin config " + this.name + " cannot be its own parent");
      } else {
         this.parent = parentConfig.get();
         if (!this.parent.initialised) {
            throw new MixinInitialisationError("Mixin config " + this.name + " attempted to assign uninitialised parent config. This probably means that there is an indirect loop in the mixin configs: child -> parent -> child");
         } else {
            this.env = this.parseSelector(this.selector, this.parent.env);
            this.verboseLogging |= this.env.getOption(MixinEnvironment.Option.DEBUG_VERBOSE);
            this.required = this.requiredValue == null ? this.parent.required : this.requiredValue && !this.env.getOption(MixinEnvironment.Option.IGNORE_REQUIRED);
            this.initPriority(this.parent.priority, this.parent.mixinPriority);
            if (this.injectorOptions == null) {
               this.injectorOptions = this.parent.injectorOptions;
            } else {
               this.injectorOptions.mergeFrom(this.parent.injectorOptions);
            }

            if (this.overwriteOptions == null) {
               this.overwriteOptions = this.parent.overwriteOptions;
            } else {
               this.overwriteOptions.mergeFrom(this.parent.overwriteOptions);
            }

            this.setSourceFile |= this.parent.setSourceFile;
            this.verboseLogging |= this.parent.verboseLogging;
            return this.postInit();
         }
      }
   }

   private void initPriority(int defaultPriority, int defaultMixinPriority) {
      if (this.priority < 0) {
         this.priority = defaultPriority;
      }

      if (this.mixinPriority < 0) {
         this.mixinPriority = defaultMixinPriority;
      }

   }

   private boolean postInit() throws MixinInitialisationError {
      if (this.initialised) {
         throw new MixinInitialisationError("Mixin config " + this.name + " was already initialised.");
      } else {
         this.initialised = true;
         this.initCompatibilityLevel();
         this.initExtensions();
         return this.checkVersion() && this.checkFeatures();
      }
   }

   private void initCompatibilityLevel() {
      this.compatibilityLevel = MixinEnvironment.getCompatibilityLevel();
      if (this.compatibility != null) {
         String strCompatibility = this.compatibility.trim().toUpperCase(Locale.ROOT);

         try {
            this.compatibilityLevel = MixinEnvironment.CompatibilityLevel.valueOf(strCompatibility);
         } catch (IllegalArgumentException var4) {
            throw new MixinInitialisationError(String.format("Mixin config %s specifies compatibility level %s which is not recognised", this.name, strCompatibility));
         }

         MixinEnvironment.CompatibilityLevel currentLevel = MixinEnvironment.getCompatibilityLevel();
         if (this.compatibilityLevel != currentLevel) {
            if (currentLevel.isAtLeast(this.compatibilityLevel) && !currentLevel.canSupport(this.compatibilityLevel)) {
               throw new MixinInitialisationError(String.format("Mixin config %s requires compatibility level %s which is too old", this.name, this.compatibilityLevel));
            } else if (!currentLevel.canElevateTo(this.compatibilityLevel)) {
               throw new MixinInitialisationError(String.format("Mixin config %s requires compatibility level %s which is prohibited by %s", this.name, this.compatibilityLevel, currentLevel));
            } else {
               MixinEnvironment.CompatibilityLevel minCompatibilityLevel = MixinEnvironment.getMinCompatibilityLevel();
               if (this.compatibilityLevel.isLessThan(minCompatibilityLevel)) {
                  this.logger.log(this.verboseLogging ? Level.INFO : Level.DEBUG, "Compatibility level {} specified by {} is lower than the default level supported by the current mixin service ({}).", this.compatibilityLevel, this, minCompatibilityLevel);
               }

               if (MixinEnvironment.CompatibilityLevel.MAX_SUPPORTED.isLessThan(this.compatibilityLevel)) {
                  this.logger.log(this.verboseLogging ? Level.WARN : Level.DEBUG, "Compatibility level {} specified by {} is higher than the maximum level supported by this version of mixin ({}).", this.compatibilityLevel, this, MixinEnvironment.CompatibilityLevel.MAX_SUPPORTED);
               }

               MixinEnvironment.setCompatibilityLevel(this.compatibilityLevel);
            }
         }
      }
   }

   void checkCompatibilityLevel(MixinInfo mixin, int majorVersion, int minorVersion) {
      if (majorVersion > this.compatibilityLevel.getClassMajorVersion()) {
         Level logLevel = this.verboseLogging && majorVersion > this.warnedClassVersion ? Level.WARN : Level.DEBUG;
         String message = majorVersion > MixinEnvironment.CompatibilityLevel.MAX_SUPPORTED.getClassMajorVersion() ? "the current version of Mixin" : "the declared compatibility level";
         this.warnedClassVersion = majorVersion;
         this.logger.log(logLevel, "{}: Class version {} required is higher than the class version supported by {} ({} supports class version {})", mixin, majorVersion, message, this.compatibilityLevel, this.compatibilityLevel.getClassMajorVersion());
      }
   }

   private MixinEnvironment parseSelector(String target, MixinEnvironment fallbackEnvironment) {
      if (target != null) {
         String[] selectors = target.split("[&\\| ]");

         for(String sel : selectors) {
            sel = sel.trim();
            Pattern environmentSelector = Pattern.compile("^@env(?:ironment)?\\(([A-Z]+)\\)$");
            Matcher environmentSelectorMatcher = environmentSelector.matcher(sel);
            if (environmentSelectorMatcher.matches()) {
               return MixinEnvironment.getEnvironment(MixinEnvironment.Phase.forName(environmentSelectorMatcher.group(1)));
            }
         }

         MixinEnvironment.Phase phase = MixinEnvironment.Phase.forName(target);
         if (phase != null) {
            return MixinEnvironment.getEnvironment(phase);
         }
      }

      return fallbackEnvironment;
   }

   private void initExtensions() {
      if (this.injectorOptions.injectionPoints != null) {
         for(String injectionPointClassName : this.injectorOptions.injectionPoints) {
            this.initInjectionPoint(injectionPointClassName, this.injectorOptions.namespace);
         }
      }

      if (this.injectorOptions.dynamicSelectors != null) {
         for(String dynamicSelectorClassName : this.injectorOptions.dynamicSelectors) {
            this.initDynamicSelector(dynamicSelectorClassName, this.injectorOptions.namespace);
         }
      }

   }

   private void initInjectionPoint(String className, String namespace) {
      try {
         Class<?> injectionPointClass = this.findExtensionClass(className, InjectionPoint.class, "injection point");
         if (injectionPointClass != null) {
            try {
               injectionPointClass.getMethod("find", String.class, InsnList.class, Collection.class);
            } catch (NoSuchMethodException cnfe) {
               this.logger.error("Unable to register injection point {} for {}, the class is not compatible with this version of Mixin", className, this, cnfe);
               return;
            }

            InjectionPoint.register(injectionPointClass, namespace);
         }
      } catch (Throwable th) {
         this.logger.catching(th);
      }

   }

   private void initDynamicSelector(String className, String namespace) {
      try {
         Class<?> dynamicSelectorClass = this.findExtensionClass(className, ITargetSelectorDynamic.class, "dynamic selector");
         if (dynamicSelectorClass != null) {
            TargetSelector.register(dynamicSelectorClass, namespace);
         }
      } catch (Throwable th) {
         this.logger.catching(th);
      }

   }

   private Class<?> findExtensionClass(String className, Class<?> superType, String extensionType) {
      Class<?> extensionClass = null;

      try {
         extensionClass = this.service.getClassProvider().findClass(className, true);
      } catch (ClassNotFoundException cnfe) {
         this.logger.error("Unable to register {} {} for {}, the specified class was not found", extensionType, className, this, cnfe);
         return null;
      }

      if (!superType.isAssignableFrom(extensionClass)) {
         this.logger.error("Unable to register {} {} for {}, class is not assignable to {}", extensionType, className, this, superType);
         return null;
      } else {
         return extensionClass;
      }
   }

   private boolean checkVersion() throws MixinInitialisationError {
      if (this.version == null) {
         if (this.parent != null && this.parent.version != null) {
            return true;
         }

         if (this.requiredFeatures == null || this.requiredFeatures.isEmpty()) {
            this.logger.debug("Mixin config {} does not specify \"minVersion\" or \"requiredFeatures\" property", this.name);
         }
      }

      VersionNumber minVersion = VersionNumber.parse(this.version);
      VersionNumber curVersion = VersionNumber.parse(this.env.getVersion());
      if (minVersion.compareTo(curVersion) > 0) {
         this.logger.warn("Mixin config {} requires mixin subsystem version {} but {} was found. The mixin config will not be applied.", this.name, minVersion, curVersion);
         if (this.required) {
            throw new MixinInitialisationError("Required mixin config " + this.name + " requires mixin subsystem version " + minVersion);
         } else {
            return false;
         }
      } else {
         return true;
      }
   }

   private boolean checkFeatures() throws MixinInitialisationError {
      if (this.requiredFeatures != null && !this.requiredFeatures.isEmpty()) {
         Set<String> missingFeatures = new LinkedHashSet();

         for(String featureId : this.requiredFeatures) {
            featureId = featureId.trim().toUpperCase(Locale.ROOT);
            if (!MixinEnvironment.Feature.isActive(featureId)) {
               missingFeatures.add(featureId);
            }
         }

         if (missingFeatures.isEmpty()) {
            return true;
         } else {
            String strMissingFeatures = Joiner.on(", ").join(missingFeatures);
            this.logger.warn("Mixin config {} requires features [{}] which are not available. The mixin config will not be applied.", this.name, strMissingFeatures);
            if (this.required) {
               throw new MixinInitialisationError("Required mixin config " + this.name + " requires features [" + strMissingFeatures + " which are not available");
            } else {
               return false;
            }
         }
      } else {
         return true;
      }
   }

   void addListener(IListener listener) {
      this.listeners.add(listener);
   }

   void onSelect() {
      this.plugin = new PluginHandle(this, this.service, this.pluginClassName);
      this.plugin.onLoad(Strings.nullToEmpty(this.mixinPackage));
      if (!Strings.isNullOrEmpty(this.mixinPackage)) {
         if (!this.mixinPackage.endsWith(".")) {
            this.mixinPackage = this.mixinPackage + ".";
         }

         boolean suppressRefMapWarning = false;
         if (this.refMapperConfig == null) {
            this.refMapperConfig = this.plugin.getRefMapperConfig();
            if (this.refMapperConfig == null) {
               suppressRefMapWarning = true;
               this.refMapperConfig = "mixin.refmap.json";
            }
         }

         this.refMapper = ReferenceMapper.read(this.refMapperConfig);
         if (!suppressRefMapWarning && this.refMapper.isDefault() && !this.env.getOption(MixinEnvironment.Option.DISABLE_REFMAP)) {
            this.logger.warn("Reference map '{}' for {} could not be read. If this is a development environment you can ignore this message", this.refMapperConfig, this);
         }

         if (this.env.getOption(MixinEnvironment.Option.REFMAP_REMAP)) {
            this.refMapper = RemappingReferenceMapper.of(this.env, this.refMapper);
         }

         if (this.refMapperWrapper != null) {
            String wrapperName = this.mixinPackage + this.refMapperWrapper;

            try {
               Class<IReferenceMapper> wrapperCls = this.service.getClassProvider().findClass(wrapperName, true);
               Constructor<IReferenceMapper> ctr = wrapperCls.getConstructor(MixinEnvironment.class, IReferenceMapper.class);
               this.refMapper = (IReferenceMapper)ctr.newInstance(this.env, this.refMapper);
            } catch (ClassNotFoundException e) {
               this.logger.error("Reference map wrapper '{}' could not be found: ", wrapperName, e);
            } catch (ReflectiveOperationException e) {
               this.logger.error("Reference map wrapper '{}' could not be created: ", wrapperName, e);
            } catch (SecurityException e) {
               this.logger.error("Reference map wrapper '{}' could not be created: ", wrapperName, e);
            }
         }

      }
   }

   void prepare(Extensions extensions) {
      if (!this.prepared) {
         this.prepared = true;
         this.prepareMixins("mixins", this.mixinClasses, false, extensions);
         switch (this.env.getSide()) {
            case CLIENT:
               this.prepareMixins("client", this.mixinClassesClient, false, extensions);
               break;
            case SERVER:
               this.prepareMixins("server", this.mixinClassesServer, false, extensions);
               break;
            case UNKNOWN:
            default:
               this.logger.warn("Mixin environment was unable to detect the current side, sided mixins will not be applied");
         }

      }
   }

   void postInitialise(Extensions extensions) {
      if (this.plugin != null) {
         List<String> pluginMixins = this.plugin.getMixins();
         this.prepareMixins("companion plugin", pluginMixins, true, extensions);
      }

      Iterator<MixinInfo> iter = this.mixins.iterator();

      while(iter.hasNext()) {
         MixinInfo mixin = (MixinInfo)iter.next();

         try {
            mixin.validate();

            for(IListener listener : this.listeners) {
               listener.onInit(mixin);
            }
         } catch (InvalidMixinException ex) {
            this.logger.error(ex.getMixin() + ": " + ex.getMessage(), (Throwable)ex);
            this.removeMixin(mixin);
            iter.remove();
         } catch (Exception ex) {
            this.logger.error(ex.getMessage(), (Throwable)ex);
            this.removeMixin(mixin);
            iter.remove();
         }
      }

   }

   private void removeMixin(MixinInfo remove) {
      for(List<MixinInfo> mixinsFor : this.mixinMapping.values()) {
         Iterator<MixinInfo> iter = mixinsFor.iterator();

         while(iter.hasNext()) {
            if (remove == iter.next()) {
               iter.remove();
            }
         }
      }

   }

   private void prepareMixins(String collectionName, List<String> mixinClasses, boolean ignorePlugin, Extensions extensions) {
      if (mixinClasses != null) {
         if (Strings.isNullOrEmpty(this.mixinPackage)) {
            if (mixinClasses.size() > 0) {
               this.logger.error("{} declares mixin classes in {} but does not specify a package, {} orphaned mixins will not be loaded: {}", this, collectionName, mixinClasses.size(), mixinClasses);
            }

         } else {
            for(String mixinClass : mixinClasses) {
               String fqMixinClass = this.mixinPackage + mixinClass;
               if (mixinClass != null && !globalMixinList.contains(fqMixinClass)) {
                  MixinInfo mixin = null;

                  try {
                     this.pendingMixins.add(new MixinInfo(this.service, this, mixinClass, this.plugin, ignorePlugin, extensions));
                     globalMixinList.add(fqMixinClass);
                  } catch (InvalidMixinException ex) {
                     if (this.required) {
                        throw ex;
                     }

                     this.logger.error(ex.getMessage(), (Throwable)ex);
                  } catch (Exception ex) {
                     if (this.required) {
                        throw new InvalidMixinException(mixin, "Error initialising mixin " + mixin + " - " + ex.getClass() + ": " + ex.getMessage(), ex);
                     }

                     this.logger.error(ex.getMessage(), (Throwable)ex);
                  }
               }
            }

            for(MixinInfo mixin : this.pendingMixins) {
               try {
                  mixin.parseTargets();
                  if (mixin.getTargetClasses().size() > 0) {
                     for(String targetClass : mixin.getTargetClasses()) {
                        String targetClassName = targetClass.replace('/', '.');
                        this.mixinsFor(targetClassName).add(mixin);
                        this.unhandledTargets.add(targetClassName);
                     }

                     for(IListener listener : this.listeners) {
                        listener.onPrepare(mixin);
                     }

                     this.mixins.add(mixin);
                  }
               } catch (InvalidMixinException ex) {
                  if (this.required) {
                     throw ex;
                  }

                  this.logger.error(ex.getMessage(), (Throwable)ex);
               } catch (Exception ex) {
                  if (this.required) {
                     throw new InvalidMixinException(mixin, "Error initialising mixin " + mixin + " - " + ex.getClass() + ": " + ex.getMessage(), ex);
                  }

                  this.logger.error(ex.getMessage(), (Throwable)ex);
               }
            }

            this.pendingMixins.clear();
         }
      }
   }

   void postApply(String transformedName, ClassNode targetClass) {
      this.unhandledTargets.remove(transformedName);
   }

   public Config getHandle() {
      if (this.handle == null) {
         this.handle = new Config(this);
      }

      return this.handle;
   }

   public boolean isRequired() {
      return this.required;
   }

   public MixinEnvironment getEnvironment() {
      return this.env;
   }

   MixinConfig getParent() {
      return this.parent;
   }

   public String getName() {
      return this.name;
   }

   public IMixinConfigSource getSource() {
      return this.source;
   }

   public String getCleanSourceId() {
      if (this.source == null) {
         return null;
      } else {
         String sourceId = this.source.getId();
         return sourceId == null ? null : sourceId.replaceAll("[^A-Za-z]", "");
      }
   }

   public String getMixinPackage() {
      return Strings.nullToEmpty(this.mixinPackage);
   }

   public int getPriority() {
      return this.priority;
   }

   public int getDefaultMixinPriority() {
      return this.mixinPriority;
   }

   public int getDefaultRequiredInjections() {
      return this.injectorOptions.defaultRequireValue;
   }

   public String getDefaultInjectorGroup() {
      String defaultGroup = this.injectorOptions.defaultGroup;
      return defaultGroup != null && !defaultGroup.isEmpty() ? defaultGroup : "default";
   }

   public boolean conformOverwriteVisibility() {
      return this.overwriteOptions.conformAccessModifiers;
   }

   public boolean requireOverwriteAnnotations() {
      return this.overwriteOptions.requireOverwriteAnnotations;
   }

   public int getMaxShiftByValue() {
      return Math.min(Math.max(this.injectorOptions.maxShiftBy, 0), 5);
   }

   public boolean select(MixinEnvironment environment) {
      this.visited = true;
      return this.env == environment;
   }

   boolean isVisited() {
      return this.visited;
   }

   int getDeclaredMixinCount() {
      return getCollectionSize(this.mixinClasses, this.mixinClassesClient, this.mixinClassesServer);
   }

   int getMixinCount() {
      return this.mixins.size();
   }

   public List<String> getClasses() {
      if (Strings.isNullOrEmpty(this.mixinPackage)) {
         return Collections.emptyList();
      } else {
         ImmutableList.Builder<String> list = ImmutableList.<String>builder();

         for(List<String> classes : new List[]{this.mixinClasses, this.mixinClassesClient, this.mixinClassesServer}) {
            if (classes != null) {
               for(String className : classes) {
                  list.add(this.mixinPackage + className);
               }
            }
         }

         return list.build();
      }
   }

   public boolean shouldSetSourceFile() {
      return this.setSourceFile;
   }

   public IReferenceMapper getReferenceMapper() {
      if (this.env.getOption(MixinEnvironment.Option.DISABLE_REFMAP)) {
         return ReferenceMapper.DEFAULT_MAPPER;
      } else {
         this.refMapper.setContext(this.env.getRefmapObfuscationContext());
         return this.refMapper;
      }
   }

   String remapClassName(String className, String reference) {
      IReferenceMapper mapper = this.getReferenceMapper();
      return mapper instanceof IClassReferenceMapper ? ((IClassReferenceMapper)mapper).remapClassName(className, reference) : mapper.remap(className, reference);
   }

   public IMixinConfigPlugin getPlugin() {
      return this.plugin.get();
   }

   public Set<String> getTargetsSet() {
      return this.mixinMapping.keySet();
   }

   public Set<String> getTargets() {
      return Collections.unmodifiableSet(this.mixinMapping.keySet());
   }

   public Set<String> getUnhandledTargets() {
      return Collections.unmodifiableSet(this.unhandledTargets);
   }

   public <V> void decorate(String key, V value) {
      if (this.decorations == null) {
         this.decorations = new HashMap();
      }

      if (this.decorations.containsKey(key)) {
         throw new IllegalArgumentException(String.format("Decoration with key '%s' already exists on config %s", key, this));
      } else {
         this.decorations.put(key, value);
      }
   }

   public boolean hasDecoration(String key) {
      return this.decorations != null && this.decorations.get(key) != null;
   }

   public <V> V getDecoration(String key) {
      return (V)(this.decorations == null ? null : this.decorations.get(key));
   }

   public Level getLoggingLevel() {
      return this.verboseLogging ? Level.INFO : Level.DEBUG;
   }

   public boolean isVerboseLogging() {
      return this.verboseLogging;
   }

   public boolean packageMatch(String className) {
      return !Strings.isNullOrEmpty(this.mixinPackage) && className.startsWith(this.mixinPackage);
   }

   public boolean hasMixinsFor(String targetClass) {
      return this.mixinMapping.containsKey(targetClass);
   }

   boolean hasPendingMixinsFor(String targetClass) {
      if (this.packageMatch(targetClass)) {
         return false;
      } else {
         for(MixinInfo pendingMixin : this.pendingMixins) {
            if (pendingMixin.hasDeclaredTarget(targetClass)) {
               return true;
            }
         }

         return false;
      }
   }

   public List<MixinInfo> getMixinsFor(String targetClass) {
      return this.mixinsFor(targetClass);
   }

   private List<MixinInfo> mixinsFor(String targetClass) {
      List<MixinInfo> mixins = (List)this.mixinMapping.get(targetClass);
      if (mixins == null) {
         mixins = new ArrayList();
         this.mixinMapping.put(targetClass, mixins);
      }

      return mixins;
   }

   public List<String> reloadMixin(String mixinClass, ClassNode classNode) {
      for(MixinInfo mixin : this.mixins) {
         if (mixin.getClassName().equals(mixinClass)) {
            mixin.reloadMixin(classNode);
            return mixin.getTargetClasses();
         }
      }

      return Collections.emptyList();
   }

   public String toString() {
      return this.name;
   }

   public int compareTo(MixinConfig other) {
      if (other == null) {
         return 0;
      } else if (other.priority == this.priority) {
         return Integer.compare(this.order, other.order);
      } else {
         return this.priority < other.priority ? -1 : 1;
      }
   }

   static Config create(String configFile, MixinEnvironment outer, IMixinConfigSource source) {
      try {
         IMixinService service = MixinService.getService();
         InputStream resource = service.getResourceAsStream(configFile);
         if (resource == null) {
            throw new IllegalArgumentException(String.format("The specified resource '%s' was invalid or could not be read", configFile));
         } else {
            InputStreamReader reader = new InputStreamReader(resource);

            Config var7;
            label37: {
               try {
                  MixinConfig config = (MixinConfig)(new Gson()).fromJson((Reader)reader, MixinConfig.class);
                  if (config.onLoad(service, configFile, outer, source)) {
                     var7 = config.getHandle();
                     break label37;
                  }
               } catch (Throwable var9) {
                  try {
                     reader.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }

                  throw var9;
               }

               reader.close();
               return null;
            }

            reader.close();
            return var7;
         }
      } catch (IllegalArgumentException ex) {
         throw ex;
      } catch (Exception ex) {
         throw new IllegalArgumentException(String.format("The specified resource '%s' was invalid or could not be read", configFile), ex);
      }
   }

   private static int getCollectionSize(Collection<?>... collections) {
      int total = 0;

      for(Collection<?> collection : collections) {
         if (collection != null) {
            total += collection.size();
         }
      }

      return total;
   }

   static class InjectorOptions {
      @SerializedName("defaultRequire")
      int defaultRequireValue = 0;
      @SerializedName("defaultGroup")
      String defaultGroup = "default";
      @SerializedName("namespace")
      String namespace;
      @SerializedName("injectionPoints")
      List<String> injectionPoints;
      @SerializedName("dynamicSelectors")
      List<String> dynamicSelectors;
      @SerializedName("maxShiftBy")
      int maxShiftBy = 0;

      void mergeFrom(InjectorOptions parent) {
         if (this.defaultRequireValue == 0) {
            this.defaultRequireValue = parent.defaultRequireValue;
         }

         if ("default".equals(this.defaultGroup)) {
            this.defaultGroup = parent.defaultGroup;
         }

         if (this.maxShiftBy == 0) {
            this.maxShiftBy = parent.maxShiftBy;
         }

      }
   }

   static class OverwriteOptions {
      @SerializedName("conformVisibility")
      boolean conformAccessModifiers;
      @SerializedName("requireAnnotations")
      boolean requireOverwriteAnnotations;

      void mergeFrom(OverwriteOptions parent) {
         this.conformAccessModifiers |= parent.conformAccessModifiers;
         this.requireOverwriteAnnotations |= parent.requireOverwriteAnnotations;
      }
   }

   interface IListener {
      void onPrepare(MixinInfo var1);

      void onInit(MixinInfo var1);
   }
}
