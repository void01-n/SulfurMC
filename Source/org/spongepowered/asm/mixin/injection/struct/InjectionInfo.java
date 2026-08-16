package org.spongepowered.asm.mixin.injection.struct;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.tools.Diagnostic.Kind;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IActivityContext;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.code.ISliceContext;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.code.InjectorTarget;
import org.spongepowered.asm.mixin.injection.code.MethodSlice;
import org.spongepowered.asm.mixin.injection.code.MethodSlices;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.TargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.TargetSelectors;
import org.spongepowered.asm.mixin.injection.selectors.throwables.SelectorException;
import org.spongepowered.asm.mixin.injection.throwables.InjectionError;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.struct.AnnotatedMethodInfo;
import org.spongepowered.asm.mixin.struct.SpecialMethodInfo;
import org.spongepowered.asm.mixin.throwables.MixinError;
import org.spongepowered.asm.mixin.throwables.MixinException;
import org.spongepowered.asm.mixin.transformer.ActivityStack;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.asm.ASM;
import org.spongepowered.asm.util.asm.MethodNodeEx;
import org.spongepowered.asm.util.logging.MessageRouter;
import org.spongepowered.include.com.google.common.base.Joiner;
import org.spongepowered.include.com.google.common.collect.ImmutableSet;

public abstract class InjectionInfo extends SpecialMethodInfo implements ISliceContext {
   private static Map<String, InjectorEntry> registry = new LinkedHashMap();
   private static Class<? extends Annotation>[] registeredAnnotations = new Class[0];
   protected final ActivityStack activities;
   protected final boolean isStatic;
   protected final TargetSelectors targets;
   protected final MethodSlices slices;
   protected final String atKey;
   protected final List<AnnotationNode> injectionPointAnnotations;
   protected final List<InjectionPoint> injectionPoints;
   protected final Map<Target, List<InjectionNodes.InjectionNode>> targetNodes;
   protected int targetCount;
   protected Injector injector;
   protected InjectorGroupInfo group;
   private final List<MethodNode> injectedMethods;
   private int expectedCallbackCount;
   private int requiredCallbackCount;
   private int maxCallbackCount;
   private int injectedCallbackCount;
   private List<String> messages;
   private int order;

   protected InjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
      this(mixin, method, annotation, "at");
   }

   protected InjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation, String atKey) {
      super(mixin, method, annotation);
      this.activities = new ActivityStack((String)null);
      this.injectionPointAnnotations = new ArrayList();
      this.injectionPoints = new ArrayList();
      this.targetNodes = new LinkedHashMap();
      this.targetCount = 0;
      this.injectedMethods = new ArrayList(0);
      this.expectedCallbackCount = 1;
      this.requiredCallbackCount = 0;
      this.maxCallbackCount = Integer.MAX_VALUE;
      this.injectedCallbackCount = 0;
      this.order = 1000;
      this.isStatic = Bytecode.isStatic(method);
      this.targets = new TargetSelectors(this, mixin.getTargetClassNode());
      this.slices = MethodSlices.parse(this);
      this.atKey = atKey;
      this.readAnnotation();
   }

   protected void readAnnotation() {
      if (this.annotation != null) {
         this.activities.clear();

         try {
            this.targets.setPermissivePass(this.mixin.getOption(MixinEnvironment.Option.REFMAP_REMAP));
            IActivityContext.IActivity activity = this.activities.begin("Read Injection Points");
            this.readInjectionPoints();
            activity.next("Parse Requirements");
            this.parseRequirements();
            activity.next("Parse Order");
            this.parseOrder();
            activity.next("Parse Selectors");
            this.parseSelectors();
            activity.next("Find Targets");
            this.targets.find();
            activity.next("Validate Targets");
            this.targets.validate(this.expectedCallbackCount, this.requiredCallbackCount);
            activity.next("Parse Injection Points");
            this.parseInjectionPoints(this.injectionPointAnnotations);
            activity.next("Parse Injector");
            this.injector = this.parseInjector(this.annotation);
            activity.end();
         } catch (InvalidMixinException ex) {
            ex.prepend(this.activities);
            throw ex;
         } catch (Exception ex) {
            throw new InvalidMixinException(this.mixin, "Unexpected " + ex.getClass().getSimpleName() + " parsing " + this.getElementDescription(), ex, this.activities);
         }
      }
   }

   protected void readInjectionPoints() {
      List<AnnotationNode> ats = Annotations.<AnnotationNode>getValue(this.annotation, this.atKey, false);
      if (ats == null) {
         throw new InvalidInjectionException(this, String.format("%s is missing '%s' value(s)", this.getElementDescription(), this.atKey));
      } else {
         this.injectionPointAnnotations.addAll(ats);
      }
   }

   protected void parseRequirements() {
      this.group = this.mixin.getInjectorGroups().parseGroup(this.method, this.mixin.getDefaultInjectorGroup()).add(this);
      Integer expect = (Integer)Annotations.getValue(this.annotation, "expect");
      if (expect != null) {
         this.expectedCallbackCount = expect;
      }

      Integer require = (Integer)Annotations.getValue(this.annotation, "require");
      if (require != null && require > -1) {
         this.requiredCallbackCount = require;
      } else if (this.group.isDefault()) {
         this.requiredCallbackCount = this.mixin.getDefaultRequiredInjections();
      }

      Integer allow = (Integer)Annotations.getValue(this.annotation, "allow");
      if (allow != null) {
         this.maxCallbackCount = Math.max(Math.max(this.requiredCallbackCount, 1), allow);
      }

   }

   protected void parseOrder() {
      Integer userOrder = (Integer)Annotations.getValue(this.annotation, "order");
      if (userOrder != null) {
         this.order = userOrder;
      } else {
         InjectorOrder injectorDefault = (InjectorOrder)this.getClass().getAnnotation(InjectorOrder.class);
         this.order = injectorDefault != null ? injectorDefault.value() : 1000;
      }
   }

   protected void parseSelectors() {
      Set<ITargetSelector> selectors = new LinkedHashSet();
      TargetSelector.parse(Annotations.getValue(this.annotation, "method", false), this, selectors);
      TargetSelector.parse(Annotations.getValue(this.annotation, "target", false), this, selectors);
      if (selectors.size() == 0) {
         throw new InvalidInjectionException(this, String.format("%s is missing 'method' or 'target' to specify targets", this.getElementDescription()));
      } else {
         this.targets.parse(selectors);
      }
   }

   protected void parseInjectionPoints(List<AnnotationNode> ats) {
      this.injectionPoints.addAll(InjectionPoint.parse(this, ats));
   }

   protected abstract Injector parseInjector(AnnotationNode var1);

   public boolean isValid() {
      return this.targets.size() > 0 && this.injectionPoints.size() > 0;
   }

   public int getOrder() {
      return this.order;
   }

   public void prepare() {
      this.activities.clear();

      try {
         this.targetNodes.clear();
         IActivityContext.IActivity activity = this.activities.begin("?");

         for(TargetSelectors.SelectedMethod targetMethod : this.targets) {
            activity.next("{ target: %s }", targetMethod);
            Target target = this.mixin.getTargetMethod(targetMethod.getMethod());
            InjectorTarget injectorTarget = new InjectorTarget(this, target, targetMethod);

            try {
               this.targetNodes.put(target, this.injector.find(injectorTarget, this.injectionPoints));
            } catch (SelectorException ex) {
               throw new InvalidInjectionException(this, String.format("Injection validation failed: %s: %s. %s%s", this.getElementDescription(), ex.getMessage(), this.mixin.getReferenceMapper().getStatus(), AnnotatedMethodInfo.getDynamicInfo(this.method)));
            } finally {
               injectorTarget.dispose();
            }
         }

         activity.end();
      } catch (InvalidMixinException ex) {
         ex.prepend(this.activities);
         throw ex;
      } catch (Exception ex) {
         throw new InvalidMixinException(this.mixin, "Unexpecteded " + ex.getClass().getSimpleName() + " preparing " + this.getElementDescription(), ex, this.activities);
      }
   }

   public void preInject() {
      for(Map.Entry<Target, List<InjectionNodes.InjectionNode>> entry : this.targetNodes.entrySet()) {
         this.injector.preInject((Target)entry.getKey(), (List)entry.getValue());
      }

   }

   public void inject() {
      for(Map.Entry<Target, List<InjectionNodes.InjectionNode>> entry : this.targetNodes.entrySet()) {
         this.injector.inject((Target)entry.getKey(), (List)entry.getValue());
      }

      this.targets.clear();
   }

   public void postInject() {
      for(MethodNode method : this.injectedMethods) {
         this.classNode.methods.add(method);
      }

      String description = this.getDescription();
      String refMapStatus = this.mixin.getReferenceMapper().getStatus();
      String extraInfo = AnnotatedMethodInfo.getDynamicInfo(this.method) + this.getMessages();
      if (this.mixin.getOption(MixinEnvironment.Option.DEBUG_INJECTORS) && this.injectedCallbackCount < this.expectedCallbackCount) {
         throw new InvalidInjectionException(this, String.format("Injection validation failed: %s %s%s in %s expected %d invocation(s) but %d succeeded. Scanned %d target(s). %s%s", description, this.methodName, this.method.desc, this.mixin, this.expectedCallbackCount, this.injectedCallbackCount, this.targetCount, refMapStatus, extraInfo));
      } else if (this.injectedCallbackCount < this.requiredCallbackCount) {
         throw new InjectionError(String.format("Critical injection failure: %s %s%s in %s failed injection check, (%d/%d) succeeded. Scanned %d target(s). %s%s", description, this.methodName, this.method.desc, this.mixin, this.injectedCallbackCount, this.requiredCallbackCount, this.targetCount, refMapStatus, extraInfo));
      } else if (this.injectedCallbackCount > this.maxCallbackCount) {
         throw new InjectionError(String.format("Critical injection failure: %s %s%s in %s failed injection check, %d succeeded of %d allowed.%s", description, this.methodName, this.method.desc, this.mixin, this.injectedCallbackCount, this.maxCallbackCount, extraInfo));
      } else {
         this.slices.postInject();
      }
   }

   public void notifyInjected(Target target) {
   }

   protected String getDescription() {
      return "Callback method";
   }

   public String toString() {
      return describeInjector(this.mixin, this.annotation, this.method);
   }

   public int getTargetCount() {
      return this.targets.size();
   }

   public MethodSlice getSlice(String id) {
      return this.slices.get(this.getSliceId(id));
   }

   public String getSliceId(String id) {
      return "";
   }

   public int getInjectedCallbackCount() {
      return this.injectedCallbackCount;
   }

   public MethodNode addMethod(int access, String name, String desc) {
      MethodNode method = new MethodNode(ASM.API_VERSION, access | 4096, name, desc, (String)null, (String[])null);
      this.injectedMethods.add(method);
      return method;
   }

   public void addCallbackInvocation(MethodNode handler) {
      ++this.injectedCallbackCount;
   }

   public void addMessage(String format, Object... args) {
      super.addMessage(format, args);
      if (this.messages == null) {
         this.messages = new ArrayList();
      }

      String message = String.format(format, args);
      this.messages.add(message);
   }

   protected String getMessages() {
      return this.messages != null ? " Messages: { " + Joiner.on(" ").join(this.messages) + "}" : "";
   }

   public static InjectionInfo parse(MixinTargetContext mixin, MethodNode method) {
      AnnotationNode annotation = getInjectorAnnotation(mixin.getMixin(), method);
      if (annotation == null) {
         return null;
      } else {
         for(InjectorEntry injector : registry.values()) {
            if (annotation.desc.equals(injector.annotationDesc)) {
               return injector.create(mixin, method, annotation);
            }
         }

         return null;
      }
   }

   public static AnnotationNode getInjectorAnnotation(IMixinInfo mixin, MethodNode method) {
      AnnotationNode annotation = null;

      try {
         annotation = Annotations.getSingleVisible(method, registeredAnnotations);
         return annotation;
      } catch (IllegalArgumentException ex) {
         throw new InvalidMixinException(mixin, String.format("Error parsing annotations on %s in %s: %s", method.name, mixin.getClassName(), ex.getMessage()));
      }
   }

   public static String getInjectorPrefix(AnnotationNode annotation) {
      if (annotation == null) {
         return "handler";
      } else {
         for(InjectorEntry injector : registry.values()) {
            if (annotation.desc.endsWith(injector.annotationDesc)) {
               return injector.prefix;
            }
         }

         return "handler";
      }
   }

   static String describeInjector(IMixinContext mixin, AnnotationNode annotation, MethodNode method) {
      return String.format("%s->@%s::%s%s", mixin.toString(), Annotations.getSimpleName(annotation), MethodNodeEx.getName(method), method.desc);
   }

   public static void register(Class<? extends InjectionInfo> type) {
      AnnotationType annotationType = (AnnotationType)type.getAnnotation(AnnotationType.class);
      if (annotationType == null) {
         throw new IllegalArgumentException("Injection info class " + type + " is not annotated with @AnnotationType");
      } else {
         InjectorEntry entry;
         try {
            entry = new InjectorEntry(annotationType.value(), type);
         } catch (NoSuchMethodException var7) {
            throw new MixinError("InjectionInfo class " + type.getName() + " is missing a valid constructor");
         }

         InjectorEntry existing = (InjectorEntry)registry.get(entry.annotationDesc);
         if (existing != null) {
            MessageRouter.getMessager().printMessage(Kind.WARNING, String.format("Overriding InjectionInfo for @%s with %s (previously %s)", annotationType.value().getSimpleName(), type.getName(), existing.injectorType.getName()));
         } else {
            MessageRouter.getMessager().printMessage(Kind.OTHER, String.format("Registering new injector for @%s with %s", annotationType.value().getSimpleName(), type.getName()));
         }

         registry.put(entry.annotationDesc, entry);
         ArrayList<Class<? extends Annotation>> annotations = new ArrayList();

         for(InjectorEntry injector : registry.values()) {
            annotations.add(injector.annotationType);
         }

         registeredAnnotations = (Class[])annotations.toArray(registeredAnnotations);
      }
   }

   public static Set<Class<? extends Annotation>> getRegisteredAnnotations() {
      return ImmutableSet.<Class<? extends Annotation>>copyOf(registeredAnnotations);
   }

   static {
      register(CallbackInjectionInfo.class);
      register(ModifyArgInjectionInfo.class);
      register(ModifyArgsInjectionInfo.class);
      register(RedirectInjectionInfo.class);
      register(ModifyVariableInjectionInfo.class);
      register(ModifyConstantInjectionInfo.class);
   }

   static class InjectorEntry {
      final Class<? extends Annotation> annotationType;
      final Class<? extends InjectionInfo> injectorType;
      final java.lang.reflect.Constructor<? extends InjectionInfo> ctor;
      final String annotationDesc;
      final String prefix;

      InjectorEntry(Class<? extends Annotation> annotationType, Class<? extends InjectionInfo> type) throws NoSuchMethodException {
         this.annotationType = annotationType;
         this.injectorType = type;
         this.ctor = type.getDeclaredConstructor(MixinTargetContext.class, MethodNode.class, AnnotationNode.class);
         this.annotationDesc = Type.getDescriptor(annotationType);
         HandlerPrefix handlerPrefix = (HandlerPrefix)type.getAnnotation(HandlerPrefix.class);
         this.prefix = handlerPrefix != null ? handlerPrefix.value() : "handler";
      }

      InjectionInfo create(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
         try {
            return (InjectionInfo)this.ctor.newInstance(mixin, method, annotation);
         } catch (InvocationTargetException itex) {
            Throwable cause = itex.getCause();
            if (cause instanceof MixinException) {
               throw (MixinException)cause;
            } else {
               Throwable ex = (Throwable)(cause != null ? cause : itex);
               throw new MixinError("Error initialising injector metaclass [" + this.injectorType + "] for annotation " + annotation.desc, ex);
            }
         } catch (ReflectiveOperationException ex) {
            throw new MixinError("Failed to instantiate injector metaclass [" + this.injectorType + "] for annotation " + annotation.desc, ex);
         }
      }
   }

   @Retention(RetentionPolicy.RUNTIME)
   @java.lang.annotation.Target({ElementType.TYPE})
   public @interface AnnotationType {
      Class<? extends Annotation> value();
   }

   @Retention(RetentionPolicy.RUNTIME)
   @java.lang.annotation.Target({ElementType.TYPE})
   public @interface HandlerPrefix {
      String DEFAULT = "handler";

      String value();
   }

   @Retention(RetentionPolicy.RUNTIME)
   @java.lang.annotation.Target({ElementType.TYPE})
   public @interface InjectorOrder {
      int EARLY = 0;
      int DEFAULT = 1000;
      int LATE = 2000;
      int REDIRECT = 10000;
      int AFTER_REDIRECT = 20000;

      int value() default 1000;
   }
}
