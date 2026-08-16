package org.spongepowered.tools.obfuscation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import javax.tools.Diagnostic.Kind;
import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.util.ITokenProvider;
import org.spongepowered.asm.util.VersionNumber;
import org.spongepowered.asm.util.logging.MessageRouter;
import org.spongepowered.include.com.google.common.collect.ImmutableList;
import org.spongepowered.tools.obfuscation.interfaces.IJavadocProvider;
import org.spongepowered.tools.obfuscation.interfaces.IMessagerEx;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.interfaces.IMixinValidator;
import org.spongepowered.tools.obfuscation.interfaces.IObfuscationManager;
import org.spongepowered.tools.obfuscation.interfaces.ITypeHandleProvider;
import org.spongepowered.tools.obfuscation.mirror.AnnotationHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeHandleASM;
import org.spongepowered.tools.obfuscation.mirror.TypeHandleSimulated;
import org.spongepowered.tools.obfuscation.mirror.TypeReference;
import org.spongepowered.tools.obfuscation.mirror.TypeUtils;
import org.spongepowered.tools.obfuscation.struct.InjectorRemap;
import org.spongepowered.tools.obfuscation.validation.ParentValidator;
import org.spongepowered.tools.obfuscation.validation.TargetValidator;

final class AnnotatedMixins implements ITokenProvider, IJavadocProvider, IMixinAnnotationProcessor, ITypeHandleProvider {
   private static Map<ProcessingEnvironment, AnnotatedMixins> instances = new HashMap();
   private final IMixinAnnotationProcessor.CompilerEnvironment env;
   private final ProcessingEnvironment processingEnv;
   private final Map<String, AnnotatedMixin> mixins = new HashMap();
   private final List<AnnotatedMixin> mixinsForPass = new ArrayList();
   private final IObfuscationManager obf;
   private final List<IMixinValidator> validators;
   private final Map<String, Integer> tokenCache = new HashMap();
   private final TargetMap targets;
   private Properties properties;

   private AnnotatedMixins(ProcessingEnvironment processingEnv) {
      this.env = IMixinAnnotationProcessor.CompilerEnvironment.detect(processingEnv);
      this.processingEnv = processingEnv;
      IMessagerEx.MessageType.applyOptions(this.env, this);
      MessageRouter.setMessager(processingEnv.getMessager());
      String pluginVersion = this.checkPluginVersion(this.getOption("pluginVersion"));
      String pluginVersionString = pluginVersion != null ? String.format(" (MixinGradle Version=%s)", pluginVersion) : "";
      this.printMessage((IMessagerEx.MessageType)IMessagerEx.MessageType.INFO, "SpongePowered MIXIN Annotation Processor Version=0.8.7" + pluginVersionString);
      this.targets = this.initTargetMap();
      this.obf = new ObfuscationManager(this);
      this.obf.init();
      this.validators = ImmutableList.<IMixinValidator>of(new ParentValidator(this), new TargetValidator(this));
      this.initTokenCache(this.getOption("tokens"));
   }

   private String checkPluginVersion(String version) {
      if (version == null) {
         return null;
      } else {
         VersionNumber pluginVersion = VersionNumber.parse(version);
         VersionNumber recommendedVersion = VersionNumber.parse("0.7");
         if (pluginVersion.compareTo(recommendedVersion) < 0) {
            this.printMessage((Diagnostic.Kind)Kind.WARNING, String.format("MixinGradle version %s is out of date. Update to the recommended version %s", pluginVersion, recommendedVersion));
         }

         return pluginVersion.toString();
      }
   }

   protected TargetMap initTargetMap() {
      TargetMap targets = TargetMap.create(System.getProperty("fabric.mixin.target.mapid"));
      System.setProperty("fabric.mixin.target.mapid", targets.getSessionId());
      String targetsFileName = this.getOption("dependencyTargetsFile");
      if (targetsFileName != null) {
         try {
            targets.readImports(new File(targetsFileName));
         } catch (IOException var4) {
            this.printMessage((Diagnostic.Kind)Kind.WARNING, "Could not read from specified imports file: " + targetsFileName);
         }
      }

      return targets;
   }

   private void initTokenCache(String tokens) {
      if (tokens != null) {
         Pattern tokenPattern = Pattern.compile("^([A-Z0-9\\-_\\.]+)=([0-9]+)$");
         String[] tokenValues = tokens.replaceAll("\\s", "").toUpperCase(Locale.ROOT).split("[;,]");

         for(String tokenValue : tokenValues) {
            Matcher tokenMatcher = tokenPattern.matcher(tokenValue);
            if (tokenMatcher.matches()) {
               this.tokenCache.put(tokenMatcher.group(1), Integer.parseInt(tokenMatcher.group(2)));
            }
         }
      }

   }

   public ITypeHandleProvider getTypeProvider() {
      return this;
   }

   public ITokenProvider getTokenProvider() {
      return this;
   }

   public IObfuscationManager getObfuscationManager() {
      return this.obf;
   }

   public IJavadocProvider getJavadocProvider() {
      return this;
   }

   public ProcessingEnvironment getProcessingEnvironment() {
      return this.processingEnv;
   }

   public IMixinAnnotationProcessor.CompilerEnvironment getCompilerEnvironment() {
      return this.env;
   }

   public Integer getToken(String token) {
      if (this.tokenCache.containsKey(token)) {
         return (Integer)this.tokenCache.get(token);
      } else {
         String option = this.getOption(token);
         Integer value = null;

         try {
            value = Integer.parseInt(option);
         } catch (Exception var5) {
         }

         this.tokenCache.put(token, value);
         return value;
      }
   }

   public String getOption(String option) {
      if (option == null) {
         return null;
      } else {
         String value = (String)this.processingEnv.getOptions().get(option);
         return value != null ? value : this.getProperties().getProperty(option);
      }
   }

   public String getOption(String option, String defaultValue) {
      String value = this.getOption(option);
      return value != null ? value : defaultValue;
   }

   public boolean getOption(String option, boolean defaultValue) {
      String value = this.getOption(option);
      return value != null ? Boolean.parseBoolean(value) : defaultValue;
   }

   public List<String> getOptions(String option) {
      ImmutableList.Builder<String> list = ImmutableList.<String>builder();
      String value = this.getOption(option);
      if (value != null) {
         for(String part : value.split(",")) {
            list.add(part);
         }
      }

      return list.build();
   }

   public Properties getProperties() {
      if (this.properties == null) {
         this.properties = new Properties();

         try {
            Filer filer = this.processingEnv.getFiler();
            FileObject propertyFile = filer.getResource(StandardLocation.SOURCE_PATH, "", "mixin.properties");
            if (propertyFile != null) {
               InputStream inputStream = propertyFile.openInputStream();
               this.properties.load(inputStream);
               inputStream.close();
            }
         } catch (Exception var4) {
         }
      }

      return this.properties;
   }

   public void writeMappings() {
      this.obf.writeMappings();
   }

   public void writeReferences() {
      this.obf.writeReferences();
   }

   public void registerMixin(TypeElement mixinType) {
      String name = mixinType.getQualifiedName().toString();
      if (!this.mixins.containsKey(name)) {
         AnnotatedMixin mixin = new AnnotatedMixin(this, mixinType);
         this.targets.registerTargets(mixin);
         mixin.runValidators(IMixinValidator.ValidationPass.EARLY, this.validators);
         this.mixins.put(name, mixin);
         this.mixinsForPass.add(mixin);
      }

   }

   public AnnotatedMixin getMixin(TypeElement mixinType) {
      return this.getMixin(mixinType.getQualifiedName().toString());
   }

   public AnnotatedMixin getMixin(String mixinType) {
      return (AnnotatedMixin)this.mixins.get(mixinType);
   }

   public Collection<TypeHandle> getMixinsTargeting(TypeHandle targetType) {
      List<TypeHandle> minions = new ArrayList();

      for(TypeReference mixin : this.targets.getMixinsTargeting(targetType)) {
         TypeHandle handle = mixin.getHandle(this);
         if (handle != null) {
            minions.add(handle);
         }
      }

      return minions;
   }

   public void registerAccessor(TypeElement mixinType, ExecutableElement method) {
      AnnotatedMixin mixinClass = this.getMixin(mixinType);
      if (mixinClass == null) {
         this.printMessage((IMessagerEx.MessageType)IMessagerEx.MessageType.ACCESSOR_ON_NON_MIXIN_METHOD, "Found @Accessor annotation on a non-mixin method", method);
      } else {
         AnnotationHandle accessor = AnnotationHandle.of(method, Accessor.class);
         mixinClass.registerAccessor(method, accessor, shouldRemap(mixinClass, accessor));
      }
   }

   public void registerInvoker(TypeElement mixinType, ExecutableElement method) {
      AnnotatedMixin mixinClass = this.getMixin(mixinType);
      if (mixinClass == null) {
         this.printMessage((IMessagerEx.MessageType)IMessagerEx.MessageType.ACCESSOR_ON_NON_MIXIN_METHOD, "Found @Invoker annotation on a non-mixin method", method);
      } else {
         AnnotationHandle invoker = AnnotationHandle.of(method, Invoker.class);
         mixinClass.registerInvoker(method, invoker, shouldRemap(mixinClass, invoker));
      }
   }

   public void registerOverwrite(TypeElement mixinType, ExecutableElement method) {
      AnnotatedMixin mixinClass = this.getMixin(mixinType);
      if (mixinClass == null) {
         this.printMessage((IMessagerEx.MessageType)IMessagerEx.MessageType.OVERWRITE_ON_NON_MIXIN_METHOD, "Found @Overwrite annotation on a non-mixin method", method);
      } else {
         AnnotationHandle overwrite = AnnotationHandle.of(method, Overwrite.class);
         mixinClass.registerOverwrite(method, overwrite, shouldRemap(mixinClass, overwrite));
      }
   }

   public void registerShadow(TypeElement mixinType, VariableElement field, AnnotationHandle shadow) {
      AnnotatedMixin mixinClass = this.getMixin(mixinType);
      if (mixinClass == null) {
         this.printMessage((IMessagerEx.MessageType)IMessagerEx.MessageType.SHADOW_ON_NON_MIXIN_ELEMENT, "Found @Shadow annotation on a non-mixin field", field);
      } else {
         mixinClass.registerShadow(field, shadow, shouldRemap(mixinClass, shadow));
      }
   }

   public void registerShadow(TypeElement mixinType, ExecutableElement method, AnnotationHandle shadow) {
      AnnotatedMixin mixinClass = this.getMixin(mixinType);
      if (mixinClass == null) {
         this.printMessage((IMessagerEx.MessageType)IMessagerEx.MessageType.SHADOW_ON_NON_MIXIN_ELEMENT, "Found @Shadow annotation on a non-mixin method", method);
      } else {
         mixinClass.registerShadow(method, shadow, shouldRemap(mixinClass, shadow));
      }
   }

   public void registerInjector(TypeElement mixinType, ExecutableElement method, AnnotationHandle inject) {
      AnnotatedMixin mixinClass = this.getMixin(mixinType);
      if (mixinClass == null) {
         this.printMessage((IMessagerEx.MessageType)IMessagerEx.MessageType.INJECTOR_ON_NON_MIXIN_METHOD, "Found " + inject + " annotation on a non-mixin method", method);
      } else {
         InjectorRemap remap = new InjectorRemap(shouldRemap(mixinClass, inject));
         mixinClass.registerInjector(method, inject, remap);
         remap.dispatchPendingMessages(this);
      }
   }

   public void registerSoftImplements(TypeElement mixin, AnnotationHandle implementsAnnotation) {
      AnnotatedMixin mixinClass = this.getMixin(mixin);
      if (mixinClass == null) {
         this.printMessage((IMessagerEx.MessageType)IMessagerEx.MessageType.SOFT_IMPLEMENTS_ON_NON_MIXIN, "Found @Implements annotation on a non-mixin class");
      } else {
         mixinClass.registerSoftImplements(implementsAnnotation);
      }
   }

   public void onPassStarted() {
      this.mixinsForPass.clear();
   }

   public void onPassCompleted(RoundEnvironment roundEnv) {
      if (!"true".equalsIgnoreCase(this.getOption("disableTargetExport"))) {
         this.targets.write(true);
      }

      for(AnnotatedMixin mixin : roundEnv.processingOver() ? this.mixins.values() : this.mixinsForPass) {
         mixin.runValidators(roundEnv.processingOver() ? IMixinValidator.ValidationPass.FINAL : IMixinValidator.ValidationPass.LATE, this.validators);
      }

   }

   private static boolean shouldRemap(AnnotatedMixin mixinClass, AnnotationHandle annotation) {
      return annotation.getBoolean("remap", mixinClass.remap());
   }

   private static boolean shouldSuppress(Element element, SuppressedBy suppressedBy) {
      if (element != null && suppressedBy != null) {
         return AnnotationHandle.of(element, SuppressWarnings.class).getList().contains(suppressedBy.getToken()) ? true : shouldSuppress(element.getEnclosingElement(), suppressedBy);
      } else {
         return false;
      }
   }

   public void printMessage(IMessagerEx.MessageType type, CharSequence msg) {
      if (type.isEnabled()) {
         this.printMessage(type.getKind(), type.decorate(msg));
      }

   }

   public void printMessage(Diagnostic.Kind kind, CharSequence msg) {
      this.processingEnv.getMessager().printMessage(kind, msg);
   }

   public void printMessage(IMessagerEx.MessageType type, CharSequence msg, Element element) {
      if (type.isEnabled()) {
         this.printMessage(type.getKind(), type.decorate(msg), element);
      }

   }

   public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element element) {
      this.processingEnv.getMessager().printMessage(kind, msg, element);
   }

   public void printMessage(IMessagerEx.MessageType type, CharSequence msg, Element element, SuppressedBy suppressedBy) {
      if (type.isEnabled()) {
         this.printMessage(type.getKind(), type.decorate(msg), element, suppressedBy);
      }

   }

   public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element element, SuppressedBy suppressedBy) {
      if (kind != Kind.WARNING || !shouldSuppress(element, suppressedBy)) {
         this.processingEnv.getMessager().printMessage(kind, msg, element);
      }

   }

   public void printMessage(IMessagerEx.MessageType type, CharSequence msg, Element element, AnnotationMirror annotation) {
      if (type.isEnabled()) {
         this.printMessage(type.getKind(), type.decorate(msg), element, annotation);
      }

   }

   public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element element, AnnotationMirror annotation) {
      this.processingEnv.getMessager().printMessage(kind, msg, element, annotation);
   }

   public void printMessage(IMessagerEx.MessageType type, CharSequence msg, Element element, AnnotationMirror annotation, SuppressedBy suppressedBy) {
      if (type.isEnabled()) {
         this.printMessage(type.getKind(), type.decorate(msg), element, annotation, suppressedBy);
      }

   }

   public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element element, AnnotationMirror annotation, SuppressedBy suppressedBy) {
      if (kind != Kind.WARNING || !shouldSuppress(element, suppressedBy)) {
         this.processingEnv.getMessager().printMessage(kind, msg, element, annotation);
      }

   }

   public void printMessage(IMessagerEx.MessageType type, CharSequence msg, Element element, AnnotationMirror annotation, AnnotationValue value) {
      if (type.isEnabled()) {
         this.printMessage(type.getKind(), type.decorate(msg), element, annotation, value);
      }

   }

   public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element element, AnnotationMirror annotation, AnnotationValue value) {
      this.processingEnv.getMessager().printMessage(kind, msg, element, annotation, value);
   }

   public void printMessage(IMessagerEx.MessageType type, CharSequence msg, Element element, AnnotationMirror annotation, AnnotationValue value, SuppressedBy suppressedBy) {
      if (type.isEnabled()) {
         this.printMessage(type.getKind(), type.decorate(msg), element, annotation, value, suppressedBy);
      }

   }

   public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element element, AnnotationMirror annotation, AnnotationValue value, SuppressedBy suppressedBy) {
      if (kind != Kind.WARNING || !shouldSuppress(element, suppressedBy)) {
         this.processingEnv.getMessager().printMessage(kind, msg, element, annotation, value);
      }

   }

   public TypeHandle getTypeHandle(String name) {
      name = name.replace('/', '.');
      Elements elements = this.processingEnv.getElementUtils();
      PackageElement pkg = null;
      int lastDotPos = name.lastIndexOf(46);
      if (lastDotPos > -1) {
         String pkgName = name.substring(0, lastDotPos);
         pkg = elements.getPackageElement(pkgName);
      }

      if (pkg != null) {
         TypeHandle asmTypeHandle = TypeHandleASM.of(pkg, name.substring(lastDotPos + 1), this);
         if (asmTypeHandle != null) {
            return asmTypeHandle;
         }
      }

      TypeElement element = this.getTypeElement(name, elements);
      if (element != null) {
         try {
            return new TypeHandle(element, this);
         } catch (NullPointerException var7) {
         }
      }

      return pkg != null ? new TypeHandle(pkg, name, this) : null;
   }

   public TypeHandle getTypeHandle(Object type) {
      if (type instanceof TypeHandle) {
         return (TypeHandle)type;
      } else if (type instanceof DeclaredType) {
         return this.getTypeHandle(TypeUtils.getInternalName((DeclaredType)type));
      } else if (type instanceof Type) {
         return this.getTypeHandle(((Type)type).getClassName());
      } else if (type instanceof TypeElement) {
         return this.getTypeHandle(TypeUtils.getInternalName((TypeElement)type));
      } else {
         return type instanceof String ? this.getTypeHandle(type.toString()) : null;
      }
   }

   private TypeElement getTypeElement(String name, Elements elements) {
      TypeElement element = elements.getTypeElement(name);
      if (element == null && name.indexOf(36) >= 0) {
         int lastDotPos = name.lastIndexOf(46);
         String pkg = lastDotPos > -1 ? name.substring(0, lastDotPos) : "";
         name = name.substring(pkg.length());
         element = elements.getTypeElement(pkg + name.replace('$', '.'));
         if (element != null) {
            return element;
         } else {
            char[] source = name.toCharArray();
            char[] dest = new char[source.length];
            int occurs = 0;

            for(int offset = 0; offset < source.length; ++offset) {
               if (source[offset] == '$') {
                  ++occurs;
               }
            }

            if (occurs <= 10 && occurs >= 2) {
               for(int mask = 1; mask < 1 << occurs && element == null; ++mask) {
                  int offset = source.length - 1;

                  for(int index = 0; offset >= 0; --offset) {
                     dest[offset] = source[offset] == '$' && (mask & 1 << index++) != 0 ? 46 : source[offset];
                  }

                  element = elements.getTypeElement(pkg + new String(dest));
               }

               return element;
            } else {
               return null;
            }
         }
      } else {
         return element;
      }
   }

   public TypeHandle getSimulatedHandle(String name, TypeMirror simulatedTarget) {
      name = name.replace('/', '.');
      int lastDotPos = name.lastIndexOf(46);
      if (lastDotPos > -1) {
         String pkg = name.substring(0, lastDotPos);
         PackageElement packageElement = this.processingEnv.getElementUtils().getPackageElement(pkg);
         if (packageElement != null) {
            return new TypeHandleSimulated(packageElement, name, simulatedTarget, this);
         }
      }

      return new TypeHandleSimulated(name, simulatedTarget, this);
   }

   public String getJavadoc(Element element) {
      Elements elements = this.processingEnv.getElementUtils();
      return elements.getDocComment(element);
   }

   public static AnnotatedMixins getMixinsForEnvironment(ProcessingEnvironment processingEnv) {
      AnnotatedMixins mixins = (AnnotatedMixins)instances.get(processingEnv);
      if (mixins == null) {
         mixins = new AnnotatedMixins(processingEnv);
         instances.put(processingEnv, mixins);
      }

      return mixins;
   }
}
