package org.spongepowered.asm.mixin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.spongepowered.asm.launch.GlobalProperties;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.Level;
import org.spongepowered.asm.mixin.extensibility.IEnvironmentTokenProvider;
import org.spongepowered.asm.mixin.throwables.MixinException;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.obfuscation.RemapperChain;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.ITransformer;
import org.spongepowered.asm.service.ITransformerProvider;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.service.MixinServiceAbstract;
import org.spongepowered.asm.util.IConsumer;
import org.spongepowered.asm.util.ITokenProvider;
import org.spongepowered.asm.util.JavaVersion;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.asm.util.asm.ASM;
import org.spongepowered.asm.util.perf.Profiler;
import org.spongepowered.include.com.google.common.collect.ImmutableList;

public final class MixinEnvironment implements ITokenProvider {
   private static MixinEnvironment currentEnvironment;
   private static Phase currentPhase;
   private static CompatibilityLevel compatibility;
   private static boolean showHeader;
   private static final ILogger logger;
   private static IMixinTransformer transformer;
   private final IMixinService service = MixinService.getService();
   private final Phase phase;
   private final GlobalProperties.Keys configsKey;
   private final boolean[] options;
   private final Set<String> tokenProviderClasses = new HashSet();
   private final List<TokenProviderWrapper> tokenProviders = new ArrayList();
   private final Map<String, Integer> internalTokens = new HashMap();
   private final RemapperChain remappers = new RemapperChain();
   private Side side;
   private String obfuscationContext = null;

   MixinEnvironment(Phase phase) {
      this.phase = phase;
      this.configsKey = GlobalProperties.Keys.of(GlobalProperties.Keys.CONFIGS + "." + this.phase.name.toLowerCase(Locale.ROOT));
      Object version = this.getVersion();
      if (version != null && "0.8.7".equals(version)) {
         this.service.checkEnv(this);
         this.options = new boolean[MixinEnvironment.Option.values().length];

         for(Option option : MixinEnvironment.Option.values()) {
            this.options[option.ordinal()] = option.getBooleanValue();
         }

         if (showHeader) {
            showHeader = false;
            this.printHeader(version);
         }

      } else {
         throw new MixinException("Environment conflict, mismatched versions or you didn't call MixinBootstrap.init()");
      }
   }

   private void printHeader(Object version) {
      String codeSource = this.getCodeSource();
      String serviceName = this.service.getName();
      Side side = this.getSide();
      logger.info("SpongePowered MIXIN Subsystem Version={} Source={} Service={} Env={}", version, codeSource, serviceName, side);
      boolean verbose = this.getOption(MixinEnvironment.Option.DEBUG_VERBOSE);
      if (verbose || this.getOption(MixinEnvironment.Option.DEBUG_EXPORT) || this.getOption(MixinEnvironment.Option.DEBUG_PROFILER)) {
         PrettyPrinter printer = new PrettyPrinter(32);
         printer.add("SpongePowered MIXIN%s", verbose ? " (Verbose debugging enabled)" : "").centre().hr();
         printer.kv("Code source", codeSource);
         printer.kv("Internal Version", version);
         printer.kv("Java Version", "%s (supports compatibility %s)", JavaVersion.current(), MixinEnvironment.CompatibilityLevel.getSupportedVersions());
         printer.kv("Default Compatibility Level", getCompatibilityLevel());
         printer.kv("Max Effective Compatibility Level", MixinEnvironment.CompatibilityLevel.getMaxEffective());
         printer.kv("Detected ASM Version", ASM.getVersionString());
         printer.kv("Detected ASM Supports Java", ASM.getClassVersionString()).hr();
         printer.kv("Service Name", serviceName);
         printer.kv("Mixin Service Class", this.service.getClass().getName());
         printer.kv("Global Property Service Class", MixinService.getGlobalPropertyService().getClass().getName());
         printer.kv("Logger Adapter Type", MixinService.getService().getLogger("mixin").getType()).hr();

         for(Option option : MixinEnvironment.Option.values()) {
            if (!option.isHidden) {
               StringBuilder indent = new StringBuilder();

               for(int i = 0; i < option.depth; ++i) {
                  indent.append("- ");
               }

               printer.kv(option.property, "%s<%s>", indent, option);
            }
         }

         printer.hr();

         for(Feature feature : MixinEnvironment.Feature.values()) {
            printer.kv(feature.name(), "available=<%s> enabled=<%s>", feature.isAvailable(), feature.isEnabled());
         }

         printer.hr().kv("Detected Side", side);
         printer.print(System.err);
      }

   }

   private String getCodeSource() {
      try {
         return this.getClass().getProtectionDomain().getCodeSource().getLocation().toString();
      } catch (Throwable var2) {
         return "Unknown";
      }
   }

   private Level getVerboseLoggingLevel() {
      return this.getOption(MixinEnvironment.Option.DEBUG_VERBOSE) ? Level.INFO : Level.DEBUG;
   }

   public Phase getPhase() {
      return this.phase;
   }

   /** @deprecated */
   @Deprecated
   public List<String> getMixinConfigs() {
      List<String> mixinConfigs = (List)GlobalProperties.get(this.configsKey);
      if (mixinConfigs == null) {
         mixinConfigs = new ArrayList();
         GlobalProperties.put(this.configsKey, mixinConfigs);
      }

      return mixinConfigs;
   }

   /** @deprecated */
   @Deprecated
   public MixinEnvironment addConfiguration(String config) {
      logger.warn("MixinEnvironment::addConfiguration is deprecated and will be removed. Use Mixins::addConfiguration instead!");
      Mixins.addConfiguration(config, this);
      return this;
   }

   void registerConfig(String config) {
      List<String> configs = this.getMixinConfigs();
      if (!configs.contains(config)) {
         configs.add(config);
      }

   }

   public MixinEnvironment registerTokenProviderClass(String providerName) {
      if (!this.tokenProviderClasses.contains(providerName)) {
         try {
            Class<? extends IEnvironmentTokenProvider> providerClass = this.service.getClassProvider().findClass(providerName, true);
            IEnvironmentTokenProvider provider = (IEnvironmentTokenProvider)providerClass.getDeclaredConstructor().newInstance();
            this.registerTokenProvider(provider);
         } catch (Throwable th) {
            logger.error("Error instantiating " + providerName, th);
         }
      }

      return this;
   }

   public MixinEnvironment registerTokenProvider(IEnvironmentTokenProvider provider) {
      if (provider != null && !this.tokenProviderClasses.contains(provider.getClass().getName())) {
         String providerName = provider.getClass().getName();
         TokenProviderWrapper wrapper = new TokenProviderWrapper(provider, this);
         logger.log(this.getVerboseLoggingLevel(), "Adding new token provider {} to {}", providerName, this);
         this.tokenProviders.add(wrapper);
         this.tokenProviderClasses.add(providerName);
         Collections.sort(this.tokenProviders);
      }

      return this;
   }

   public Integer getToken(String token) {
      token = token.toUpperCase(Locale.ROOT);

      for(TokenProviderWrapper provider : this.tokenProviders) {
         Integer value = provider.getToken(token);
         if (value != null) {
            return value;
         }
      }

      return (Integer)this.internalTokens.get(token);
   }

   /** @deprecated */
   @Deprecated
   public Set<String> getErrorHandlerClasses() {
      return Mixins.getErrorHandlerClasses();
   }

   public Object getActiveTransformer() {
      return transformer;
   }

   public void setActiveTransformer(IMixinTransformer transformer) {
      if (transformer != null) {
         MixinEnvironment.transformer = transformer;
      }

   }

   public MixinEnvironment setSide(Side side) {
      if (side != null && this.getSide() == MixinEnvironment.Side.UNKNOWN && side != MixinEnvironment.Side.UNKNOWN) {
         this.side = side;
      }

      return this;
   }

   public Side getSide() {
      if (this.side == null) {
         for(Side side : MixinEnvironment.Side.values()) {
            if (side.detect()) {
               this.side = side;
               break;
            }
         }
      }

      return this.side != null ? this.side : MixinEnvironment.Side.UNKNOWN;
   }

   public String getVersion() {
      return (String)GlobalProperties.get(GlobalProperties.Keys.INIT);
   }

   public boolean getOption(Option option) {
      return this.options[option.ordinal()];
   }

   public void setOption(Option option, boolean value) {
      this.options[option.ordinal()] = value;
   }

   public String getOptionValue(Option option) {
      return option.getStringValue();
   }

   public <E extends Enum<E>> E getOption(Option option, E defaultValue) {
      return (E)option.getEnumValue(defaultValue);
   }

   public void setObfuscationContext(String context) {
      this.obfuscationContext = context;
   }

   public String getObfuscationContext() {
      return this.obfuscationContext;
   }

   public String getRefmapObfuscationContext() {
      String overrideObfuscationType = MixinEnvironment.Option.OBFUSCATION_TYPE.getStringValue();
      return overrideObfuscationType != null ? overrideObfuscationType : this.obfuscationContext;
   }

   public RemapperChain getRemappers() {
      return this.remappers;
   }

   public void audit() {
      Object activeTransformer = this.getActiveTransformer();
      if (activeTransformer instanceof IMixinTransformer) {
         ((IMixinTransformer)activeTransformer).audit(this);
      }

   }

   /** @deprecated */
   @Deprecated
   public List<ITransformer> getTransformers() {
      logger.warn("MixinEnvironment::getTransformers is deprecated!");
      ITransformerProvider transformers = this.service.getTransformerProvider();
      return transformers != null ? (List)transformers.getTransformers() : Collections.emptyList();
   }

   /** @deprecated */
   @Deprecated
   public void addTransformerExclusion(String name) {
      logger.warn("MixinEnvironment::addTransformerExclusion is deprecated!");
      ITransformerProvider transformers = this.service.getTransformerProvider();
      if (transformers != null) {
         transformers.addTransformerExclusion(name);
      }

   }

   public String toString() {
      return String.format("%s[%s]", this.getClass().getSimpleName(), this.phase);
   }

   private static Phase getCurrentPhase() {
      if (currentPhase == MixinEnvironment.Phase.NOT_INITIALISED) {
         init(MixinEnvironment.Phase.PREINIT);
      }

      return currentPhase;
   }

   public static void init(Phase phase) {
      if (currentPhase == MixinEnvironment.Phase.NOT_INITIALISED) {
         currentPhase = phase;
         MixinEnvironment env = getEnvironment(phase);
         Profiler.setActive(env.getOption(MixinEnvironment.Option.DEBUG_PROFILER));
         IMixinService service = MixinService.getService();
         if (service instanceof MixinServiceAbstract) {
            ((MixinServiceAbstract)service).wire(phase, new PhaseConsumer());
         }
      }

   }

   public static MixinEnvironment getEnvironment(Phase phase) {
      return phase == null ? MixinEnvironment.Phase.DEFAULT.getEnvironment() : phase.getEnvironment();
   }

   public static MixinEnvironment getDefaultEnvironment() {
      return getEnvironment(MixinEnvironment.Phase.DEFAULT);
   }

   public static MixinEnvironment getCurrentEnvironment() {
      if (currentEnvironment == null) {
         currentEnvironment = getEnvironment(getCurrentPhase());
      }

      return currentEnvironment;
   }

   public static CompatibilityLevel getCompatibilityLevel() {
      if (compatibility == null) {
         CompatibilityLevel minLevel = getMinCompatibilityLevel();
         CompatibilityLevel optionLevel = (CompatibilityLevel)MixinEnvironment.Option.DEFAULT_COMPATIBILITY_LEVEL.getEnumValue(minLevel);
         compatibility = optionLevel.isAtLeast(minLevel) ? optionLevel : minLevel;
      }

      return compatibility;
   }

   public static CompatibilityLevel getMinCompatibilityLevel() {
      CompatibilityLevel minLevel = MixinService.getService().getMinCompatibilityLevel();
      return minLevel == null ? MixinEnvironment.CompatibilityLevel.DEFAULT : minLevel;
   }

   /** @deprecated */
   @Deprecated
   public static void setCompatibilityLevel(CompatibilityLevel level) throws IllegalArgumentException {
      StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
      if (!"org.spongepowered.asm.mixin.transformer.MixinConfig".equals(stackTrace[2].getClassName())) {
         logger.warn("MixinEnvironment::setCompatibilityLevel is deprecated and will be removed. Set level via config instead!");
      }

      CompatibilityLevel currentLevel = getCompatibilityLevel();
      if (level != currentLevel && level.isAtLeast(currentLevel)) {
         if (!level.isSupported()) {
            throw new IllegalArgumentException(String.format("The requested compatibility level %s could not be set. Level is not supported by the active JRE or ASM version (Java %s, %s)", level, JavaVersion.current(), ASM.getVersionString()));
         }

         IMixinService service = MixinService.getService();
         CompatibilityLevel maxLevel = service.getMaxCompatibilityLevel();
         if (maxLevel != null && maxLevel.isLessThan(level)) {
            logger.warn("The requested compatibility level {} is higher than the level supported by the active subsystem '{}' which supports {}. This is not a supported configuration and instability may occur.", level, service.getName(), maxLevel);
         }

         compatibility = level;
         logger.info("Compatibility level set to {}", level);
      }

   }

   /** @deprecated */
   @Deprecated
   public static Profiler getProfiler() {
      return Profiler.getProfiler("mixin");
   }

   static void gotoPhase(Phase phase) {
      if (phase != null && phase.ordinal >= 0) {
         IMixinService service = MixinService.getService();
         if (phase.ordinal > getCurrentPhase().ordinal) {
            service.beginPhase();
         }

         currentPhase = phase;
         currentEnvironment = getEnvironment(getCurrentPhase());
         if (service instanceof MixinServiceAbstract && phase == MixinEnvironment.Phase.DEFAULT) {
            ((MixinServiceAbstract)service).unwire();
         }

      } else {
         throw new IllegalArgumentException("Cannot go to the specified phase, phase is null or invalid");
      }
   }

   static {
      currentPhase = MixinEnvironment.Phase.NOT_INITIALISED;
      showHeader = true;
      logger = MixinService.getService().getLogger("mixin");
   }

   public static final class Phase {
      static final Phase NOT_INITIALISED = new Phase(-1, "NOT_INITIALISED");
      public static final Phase PREINIT = new Phase(0, "PREINIT");
      public static final Phase INIT = new Phase(1, "INIT");
      public static final Phase DEFAULT = new Phase(2, "DEFAULT");
      static final List<Phase> phases;
      final int ordinal;
      final String name;
      private MixinEnvironment environment;

      private Phase(int ordinal, String name) {
         this.ordinal = ordinal;
         this.name = name;
      }

      public String toString() {
         return this.name;
      }

      public static Phase forName(String name) {
         for(Phase phase : phases) {
            if (phase.name.equals(name)) {
               return phase;
            }
         }

         return null;
      }

      MixinEnvironment getEnvironment() {
         if (this.ordinal < 0) {
            throw new IllegalArgumentException("Cannot access the NOT_INITIALISED environment");
         } else {
            if (this.environment == null) {
               this.environment = new MixinEnvironment(this);
            }

            return this.environment;
         }
      }

      static {
         phases = ImmutableList.<Phase>of(PREINIT, INIT, DEFAULT);
      }
   }

   public static enum Side {
      UNKNOWN {
         protected boolean detect() {
            return false;
         }
      },
      CLIENT {
         protected boolean detect() {
            String sideName = MixinService.getService().getSideName();
            return "CLIENT".equals(sideName);
         }
      },
      SERVER {
         protected boolean detect() {
            String sideName = MixinService.getService().getSideName();
            return "SERVER".equals(sideName) || "DEDICATEDSERVER".equals(sideName);
         }
      };

      private Side() {
      }

      protected abstract boolean detect();

      // $FF: synthetic method
      private static Side[] $values() {
         return new Side[]{UNKNOWN, CLIENT, SERVER};
      }
   }

   public static enum Option {
      DEBUG_ALL("debug"),
      DEBUG_EXPORT(DEBUG_ALL, "export"),
      DEBUG_EXPORT_FILTER(DEBUG_EXPORT, "filter", false),
      DEBUG_EXPORT_DECOMPILE(DEBUG_EXPORT, MixinEnvironment.Option.Inherit.ALLOW_OVERRIDE, "decompile"),
      DEBUG_EXPORT_DECOMPILE_THREADED(DEBUG_EXPORT_DECOMPILE, MixinEnvironment.Option.Inherit.ALLOW_OVERRIDE, "async"),
      DEBUG_EXPORT_DECOMPILE_MERGESIGNATURES(DEBUG_EXPORT_DECOMPILE, MixinEnvironment.Option.Inherit.ALLOW_OVERRIDE, "mergeGenericSignatures"),
      DEBUG_VERIFY(DEBUG_ALL, "verify"),
      DEBUG_VERBOSE(DEBUG_ALL, "verbose"),
      DEBUG_INJECTORS(DEBUG_ALL, "countInjections"),
      DEBUG_STRICT(DEBUG_ALL, MixinEnvironment.Option.Inherit.INDEPENDENT, "strict"),
      DEBUG_UNIQUE(DEBUG_STRICT, "unique"),
      DEBUG_TARGETS(DEBUG_STRICT, "targets"),
      DEBUG_PROFILER(DEBUG_ALL, MixinEnvironment.Option.Inherit.ALLOW_OVERRIDE, "profiler"),
      DUMP_TARGET_ON_FAILURE("dumpTargetOnFailure"),
      CHECK_ALL("checks"),
      CHECK_IMPLEMENTS(CHECK_ALL, "interfaces"),
      CHECK_IMPLEMENTS_STRICT(CHECK_IMPLEMENTS, MixinEnvironment.Option.Inherit.ALLOW_OVERRIDE, "strict"),
      IGNORE_CONSTRAINTS("ignoreConstraints"),
      HOT_SWAP("hotSwap"),
      ENVIRONMENT(MixinEnvironment.Option.Inherit.ALWAYS_FALSE, true, "env"),
      OBFUSCATION_TYPE(ENVIRONMENT, MixinEnvironment.Option.Inherit.ALWAYS_FALSE, "obf"),
      DISABLE_REFMAP(ENVIRONMENT, MixinEnvironment.Option.Inherit.INDEPENDENT, "disableRefMap"),
      REFMAP_REMAP(ENVIRONMENT, MixinEnvironment.Option.Inherit.INDEPENDENT, "remapRefMap"),
      REFMAP_REMAP_RESOURCE(ENVIRONMENT, MixinEnvironment.Option.Inherit.INDEPENDENT, "refMapRemappingFile", ""),
      REFMAP_REMAP_SOURCE_ENV(ENVIRONMENT, MixinEnvironment.Option.Inherit.INDEPENDENT, "refMapRemappingEnv", "searge"),
      REFMAP_REMAP_ALLOW_PERMISSIVE(ENVIRONMENT, MixinEnvironment.Option.Inherit.INDEPENDENT, "allowPermissiveMatch", true, "true"),
      IGNORE_REQUIRED(ENVIRONMENT, MixinEnvironment.Option.Inherit.INDEPENDENT, "ignoreRequired"),
      DEFAULT_COMPATIBILITY_LEVEL(ENVIRONMENT, MixinEnvironment.Option.Inherit.INDEPENDENT, "compatLevel"),
      SHIFT_BY_VIOLATION_BEHAVIOUR(ENVIRONMENT, MixinEnvironment.Option.Inherit.INDEPENDENT, "shiftByViolation", "warn"),
      INITIALISER_INJECTION_MODE("initialiserInjectionMode", "default"),
      TUNABLE(MixinEnvironment.Option.Inherit.ALWAYS_FALSE, true, "tunable"),
      CLASSREADER_EXPAND_FRAMES(TUNABLE, MixinEnvironment.Option.Inherit.INDEPENDENT, "classReaderExpandFrames", true, "false");

      final Option parent;
      final Inherit inheritance;
      final boolean isHidden;
      final String property;
      final String defaultValue;
      final boolean isFlag;
      final int depth;

      private Option(String property) {
         this((Option)null, property, true);
      }

      private Option(Inherit inheritance, boolean hidden, String property) {
         this((Option)null, inheritance, hidden, property, true);
      }

      private Option(String property, String defaultStringValue) {
         this((Option)null, MixinEnvironment.Option.Inherit.INDEPENDENT, property, false, defaultStringValue);
      }

      private Option(Option parent, String property) {
         this(parent, MixinEnvironment.Option.Inherit.INHERIT, property, true);
      }

      private Option(Option parent, Inherit inheritance, String property) {
         this(parent, inheritance, property, true);
      }

      private Option(Option parent, String property, boolean isFlag) {
         this(parent, MixinEnvironment.Option.Inherit.INHERIT, property, isFlag, (String)null);
      }

      private Option(Option parent, Inherit inheritance, String property, boolean isFlag) {
         this(parent, inheritance, property, isFlag, (String)null);
      }

      private Option(Option parent, Inherit inheritance, boolean hidden, String property, boolean isFlag) {
         this(parent, inheritance, hidden, property, isFlag, (String)null);
      }

      private Option(Option parent, Inherit inheritance, String property, String defaultStringValue) {
         this(parent, inheritance, property, false, defaultStringValue);
      }

      private Option(Option parent, Inherit inheritance, String property, boolean isFlag, String defaultStringValue) {
         this(parent, inheritance, false, property, isFlag, defaultStringValue);
      }

      private Option(Option parent, Inherit inheritance, boolean hidden, String property, boolean isFlag, String defaultStringValue) {
         this.parent = parent;
         this.inheritance = inheritance;
         this.isHidden = hidden;
         this.property = (parent != null ? parent.property : "mixin") + "." + property;
         this.defaultValue = defaultStringValue;
         this.isFlag = isFlag;

         int depth;
         for(depth = 0; parent != null; ++depth) {
            parent = parent.parent;
         }

         this.depth = depth;
      }

      public String toString() {
         return this.isFlag ? String.valueOf(this.getBooleanValue()) : this.getStringValue();
      }

      private boolean getLocalBooleanValue(boolean defaultValue) {
         return Boolean.parseBoolean(System.getProperty(this.property, Boolean.toString(defaultValue)));
      }

      private boolean getInheritedBooleanValue() {
         return this.parent != null && this.parent.getBooleanValue();
      }

      final boolean getBooleanValue() {
         if (this.inheritance == MixinEnvironment.Option.Inherit.ALWAYS_FALSE) {
            return false;
         } else {
            boolean local = this.getLocalBooleanValue(false);
            if (this.inheritance == MixinEnvironment.Option.Inherit.INDEPENDENT) {
               return local;
            } else {
               boolean inherited = local || this.getInheritedBooleanValue();
               return this.inheritance == MixinEnvironment.Option.Inherit.INHERIT ? inherited : this.getLocalBooleanValue(inherited);
            }
         }
      }

      final String getStringValue() {
         return this.inheritance != MixinEnvironment.Option.Inherit.INDEPENDENT && this.parent != null && !this.parent.getBooleanValue() ? this.defaultValue : System.getProperty(this.property, this.defaultValue);
      }

      <E extends Enum<E>> E getEnumValue(E defaultValue) {
         String value = System.getProperty(this.property, defaultValue.name());

         try {
            return (E)Enum.valueOf(defaultValue.getDeclaringClass(), value.toUpperCase(Locale.ROOT));
         } catch (IllegalArgumentException var4) {
            return defaultValue;
         }
      }

      // $FF: synthetic method
      private static Option[] $values() {
         return new Option[]{DEBUG_ALL, DEBUG_EXPORT, DEBUG_EXPORT_FILTER, DEBUG_EXPORT_DECOMPILE, DEBUG_EXPORT_DECOMPILE_THREADED, DEBUG_EXPORT_DECOMPILE_MERGESIGNATURES, DEBUG_VERIFY, DEBUG_VERBOSE, DEBUG_INJECTORS, DEBUG_STRICT, DEBUG_UNIQUE, DEBUG_TARGETS, DEBUG_PROFILER, DUMP_TARGET_ON_FAILURE, CHECK_ALL, CHECK_IMPLEMENTS, CHECK_IMPLEMENTS_STRICT, IGNORE_CONSTRAINTS, HOT_SWAP, ENVIRONMENT, OBFUSCATION_TYPE, DISABLE_REFMAP, REFMAP_REMAP, REFMAP_REMAP_RESOURCE, REFMAP_REMAP_SOURCE_ENV, REFMAP_REMAP_ALLOW_PERMISSIVE, IGNORE_REQUIRED, DEFAULT_COMPATIBILITY_LEVEL, SHIFT_BY_VIOLATION_BEHAVIOUR, INITIALISER_INJECTION_MODE, TUNABLE, CLASSREADER_EXPAND_FRAMES};
      }

      private static enum Inherit {
         INHERIT,
         ALLOW_OVERRIDE,
         INDEPENDENT,
         ALWAYS_FALSE;

         // $FF: synthetic method
         private static Inherit[] $values() {
            return new Inherit[]{INHERIT, ALLOW_OVERRIDE, INDEPENDENT, ALWAYS_FALSE};
         }
      }
   }

   public static enum CompatibilityLevel {
      JAVA_6(6, 50, 0),
      JAVA_7(7, 51, 0) {
         boolean isSupported() {
            return JavaVersion.current() >= 1.7;
         }
      },
      JAVA_8(8, 52, 3) {
         boolean isSupported() {
            return JavaVersion.current() >= 1.8;
         }
      },
      JAVA_9(9, 53, 7) {
         boolean isSupported() {
            return JavaVersion.current() >= (double)9.0F && ASM.isAtLeastVersion(6);
         }
      },
      JAVA_10(10, 54, 7) {
         boolean isSupported() {
            return JavaVersion.current() >= (double)10.0F && ASM.isAtLeastVersion(6, 1);
         }
      },
      JAVA_11(11, 55, 31) {
         boolean isSupported() {
            return JavaVersion.current() >= (double)11.0F && ASM.isAtLeastVersion(7);
         }
      },
      JAVA_12(12, 56, 31) {
         boolean isSupported() {
            return JavaVersion.current() >= (double)12.0F && ASM.isAtLeastVersion(7);
         }
      },
      JAVA_13(13, 57, 31) {
         boolean isSupported() {
            return JavaVersion.current() >= (double)13.0F && ASM.isAtLeastVersion(7);
         }
      },
      JAVA_14(14, 58, 63) {
         boolean isSupported() {
            return JavaVersion.current() >= (double)14.0F && ASM.isAtLeastVersion(8);
         }
      },
      JAVA_15(15, 59, 127) {
         boolean isSupported() {
            return JavaVersion.current() >= (double)15.0F && ASM.isAtLeastVersion(9);
         }
      },
      JAVA_16(16, 60, 127) {
         boolean isSupported() {
            return JavaVersion.current() >= (double)16.0F && ASM.isAtLeastVersion(9);
         }
      },
      JAVA_17(17, 61, 127) {
         boolean isSupported() {
            return JavaVersion.current() >= (double)17.0F && ASM.isAtLeastVersion(9, 1);
         }
      },
      JAVA_18(18, 62, 127) {
         boolean isSupported() {
            return JavaVersion.current() >= (double)18.0F && ASM.isAtLeastVersion(9, 2);
         }
      },
      JAVA_19(19, 63, 127) {
         boolean isSupported() {
            return JavaVersion.current() >= (double)19.0F && ASM.isAtLeastVersion(9, 3);
         }
      },
      JAVA_20(20, 64, 127) {
         boolean isSupported() {
            return JavaVersion.current() >= (double)20.0F && ASM.isAtLeastVersion(9, 4);
         }
      },
      JAVA_21(21, 65, 127) {
         boolean isSupported() {
            return JavaVersion.current() >= (double)21.0F && ASM.isAtLeastVersion(9, 5);
         }
      },
      JAVA_22(22, 66, 127) {
         boolean isSupported() {
            return JavaVersion.current() >= (double)22.0F && ASM.isAtLeastVersion(9, 6);
         }
      },
      JAVA_23(23, 67, 127) {
         boolean isSupported() {
            return JavaVersion.current() >= (double)23.0F && ASM.isAtLeastVersion(9, 7);
         }
      },
      JAVA_24(24, 68, 127) {
         boolean isSupported() {
            return JavaVersion.current() >= (double)24.0F && ASM.isAtLeastVersion(9, 7, 1);
         }
      },
      JAVA_25(25, 69, 127) {
         boolean isSupported() {
            return JavaVersion.current() >= (double)25.0F && ASM.isAtLeastVersion(9, 8);
         }
      };

      public static CompatibilityLevel DEFAULT = JAVA_6;
      public static CompatibilityLevel MAX_SUPPORTED = JAVA_13;
      private final int ver;
      private final int classVersion;
      private final int languageFeatures;
      private CompatibilityLevel maxCompatibleLevel;

      private CompatibilityLevel(int ver, int classVersion, int languageFeatures) {
         this.ver = ver;
         this.classVersion = classVersion;
         this.languageFeatures = languageFeatures;
      }

      boolean isSupported() {
         return true;
      }

      /** @deprecated */
      @Deprecated
      public int classVersion() {
         return this.classVersion;
      }

      public int getClassVersion() {
         return this.classVersion;
      }

      public int getClassMajorVersion() {
         return this.classVersion & '\uffff';
      }

      public int getLanguageFeatures() {
         return this.languageFeatures;
      }

      /** @deprecated */
      @Deprecated
      public boolean supportsMethodsInInterfaces() {
         return (this.languageFeatures & 1) != 0;
      }

      public boolean supports(int languageFeatures) {
         return (this.languageFeatures & languageFeatures) == languageFeatures;
      }

      public boolean isAtLeast(CompatibilityLevel level) {
         return level == null || this.ver >= level.ver;
      }

      public boolean isLessThan(CompatibilityLevel level) {
         return level == null || this.ver < level.ver;
      }

      public boolean canElevateTo(CompatibilityLevel level) {
         if (level != null && this.maxCompatibleLevel != null) {
            return level.ver <= this.maxCompatibleLevel.ver;
         } else {
            return true;
         }
      }

      public boolean canSupport(CompatibilityLevel level) {
         return level == null ? true : level.canElevateTo(this);
      }

      public static CompatibilityLevel requiredFor(int languageFeatures) {
         for(CompatibilityLevel level : values()) {
            if (level.supports(languageFeatures)) {
               return level;
            }
         }

         return null;
      }

      public static CompatibilityLevel getMaxEffective() {
         CompatibilityLevel max = JAVA_6;

         for(CompatibilityLevel level : values()) {
            if (level.isSupported()) {
               max = level;
            }

            if (level == MAX_SUPPORTED) {
               break;
            }
         }

         return max;
      }

      static String getSupportedVersions() {
         StringBuilder sb = new StringBuilder();
         boolean comma = false;
         int rangeStart = 0;
         int rangeEnd = 0;

         for(CompatibilityLevel level : values()) {
            if (level.isSupported()) {
               if (level.ver == rangeEnd + 1) {
                  rangeEnd = level.ver;
               } else {
                  if (rangeStart > 0) {
                     sb.append(comma ? "," : "").append(rangeStart);
                     if (rangeEnd > rangeStart) {
                        sb.append((char)(rangeEnd > rangeStart + 1 ? '-' : ',')).append(rangeEnd);
                     }

                     comma = true;
                     int var10000 = level.ver;
                  }

                  rangeStart = rangeEnd = level.ver;
               }
            }
         }

         if (rangeStart > 0) {
            sb.append(comma ? "," : "").append(rangeStart);
            if (rangeEnd > rangeStart) {
               sb.append((char)(rangeEnd > rangeStart + 1 ? '-' : ',')).append(rangeEnd);
            }
         }

         return sb.toString();
      }

      public static CompatibilityLevel forClassVersion(int version) {
         CompatibilityLevel latest = null;

         for(CompatibilityLevel level : values()) {
            if (level.getClassVersion() >= version) {
               return level;
            }

            latest = level;
         }

         return latest;
      }

      // $FF: synthetic method
      private static CompatibilityLevel[] $values() {
         return new CompatibilityLevel[]{JAVA_6, JAVA_7, JAVA_8, JAVA_9, JAVA_10, JAVA_11, JAVA_12, JAVA_13, JAVA_14, JAVA_15, JAVA_16, JAVA_17, JAVA_18, JAVA_19, JAVA_20, JAVA_21, JAVA_22, JAVA_23, JAVA_24, JAVA_25};
      }
   }

   public static enum Feature {
      UNSAFE_INJECTION(true),
      INJECTORS_IN_INTERFACE_MIXINS {
         public boolean isAvailable() {
            return MixinEnvironment.CompatibilityLevel.getMaxEffective().supports(1);
         }

         public boolean isEnabled() {
            return MixinEnvironment.getCompatibilityLevel().supports(1);
         }
      };

      private boolean enabled;

      private Feature() {
         this(false);
      }

      private Feature(boolean enabled) {
         this.enabled = enabled;
      }

      public boolean isAvailable() {
         return true;
      }

      public boolean isEnabled() {
         return this.isAvailable() && this.enabled;
      }

      public static Feature get(String featureId) {
         if (featureId == null) {
            return null;
         } else {
            try {
               return valueOf(featureId);
            } catch (IllegalArgumentException var2) {
               return null;
            }
         }
      }

      public static boolean exists(String featureId) {
         return get(featureId) != null;
      }

      public static boolean isActive(String featureId) {
         Feature feature = get(featureId);
         return feature != null && feature.isEnabled();
      }

      // $FF: synthetic method
      private static Feature[] $values() {
         return new Feature[]{UNSAFE_INJECTION, INJECTORS_IN_INTERFACE_MIXINS};
      }
   }

   static class TokenProviderWrapper implements Comparable<TokenProviderWrapper> {
      private static int nextOrder = 0;
      private final int priority;
      private final int order;
      private final IEnvironmentTokenProvider provider;
      private final MixinEnvironment environment;

      public TokenProviderWrapper(IEnvironmentTokenProvider provider, MixinEnvironment environment) {
         this.provider = provider;
         this.environment = environment;
         this.order = nextOrder++;
         this.priority = provider.getPriority();
      }

      public int compareTo(TokenProviderWrapper other) {
         if (other == null) {
            return 0;
         } else {
            return other.priority == this.priority ? other.order - this.order : other.priority - this.priority;
         }
      }

      Integer getToken(String token) {
         return this.provider.getToken(token, this.environment);
      }
   }

   static class PhaseConsumer implements IConsumer<Phase> {
      public void accept(Phase phase) {
         MixinEnvironment.gotoPhase(phase);
      }
   }
}
