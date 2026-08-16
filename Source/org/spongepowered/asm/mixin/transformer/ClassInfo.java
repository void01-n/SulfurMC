package org.spongepowered.asm.mixin.transformer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.ClassSignature;
import org.spongepowered.asm.util.Locals;
import org.spongepowered.asm.util.asm.ClassNodeAdapter;
import org.spongepowered.asm.util.perf.Profiler;
import org.spongepowered.include.com.google.common.base.Strings;
import org.spongepowered.include.com.google.common.collect.ImmutableList;
import org.spongepowered.include.com.google.common.collect.ImmutableSet;

public final class ClassInfo {
   public static final int INCLUDE_PRIVATE = 2;
   public static final int INCLUDE_STATIC = 8;
   public static final int INCLUDE_ALL = 10;
   public static final int INCLUDE_INITIALISERS = 262144;
   private static final ILogger logger = MixinService.getService().getLogger("mixin");
   private static final Profiler profiler = Profiler.getProfiler("meta");
   private static final Map<String, ClassInfo> cache = new HashMap();
   private static final ClassInfo OBJECT = new ClassInfo();
   private final String name;
   private final String superName;
   private final String outerName;
   private final boolean isInner;
   private final boolean isProbablyStatic;
   private final Set<String> interfaces;
   private final Set<Method> initialisers;
   private final Set<Method> methods;
   private final Set<Field> fields;
   private final Set<MixinInfo> mixins;
   private final Map<ClassInfo, ClassInfo> correspondingTypes = new HashMap();
   private final MixinInfo mixin;
   private final MethodMapper methodMapper;
   private final boolean isMixin;
   private final boolean isInterface;
   private final int access;
   private ClassInfo superClass;
   private ClassInfo outerClass;
   private ClassSignature signature;
   private Set<MixinInfo> appliedMixins;
   private String nestHost;
   private Set<String> nestMembers;

   private ClassInfo() {
      this.name = "java/lang/Object";
      this.superName = null;
      this.outerName = null;
      this.isInner = false;
      this.isProbablyStatic = true;
      this.initialisers = ImmutableSet.<Method>of(new Method("<init>", "()V"));
      this.methods = ImmutableSet.<Method>of(new Method("getClass", "()Ljava/lang/Class;"), new Method("hashCode", "()I"), new Method("equals", "(Ljava/lang/Object;)Z"), new Method("clone", "()Ljava/lang/Object;"), new Method("toString", "()Ljava/lang/String;"), new Method("notify", "()V"), new Method("notifyAll", "()V"), new Method("wait", "(J)V"), new Method("wait", "(JI)V"), new Method("wait", "()V"), new Method("finalize", "()V"));
      this.fields = Collections.emptySet();
      this.isInterface = false;
      this.interfaces = Collections.emptySet();
      this.access = 1;
      this.isMixin = false;
      this.mixin = null;
      this.mixins = Collections.emptySet();
      this.methodMapper = null;
   }

   private ClassInfo(ClassNode classNode) {
      Profiler.Section timer = profiler.begin(1, (String)"class.meta");

      try {
         this.name = classNode.name;
         this.superName = classNode.superName != null ? classNode.superName : "java/lang/Object";
         this.initialisers = new HashSet();
         this.methods = new HashSet();
         this.fields = new HashSet();
         this.isInterface = (classNode.access & 512) != 0;
         this.interfaces = new HashSet();
         this.isMixin = classNode instanceof MixinInfo.MixinClassNode;
         this.mixin = this.isMixin ? ((MixinInfo.MixinClassNode)classNode).getMixin() : null;
         this.mixins = (Set<MixinInfo>)(this.isMixin ? Collections.emptySet() : new HashSet());
         this.interfaces.addAll(classNode.interfaces);

         for(MethodNode method : classNode.methods) {
            this.addMethod(method, this.isMixin);
         }

         boolean isProbablyStatic = true;
         String outerName = classNode.outerClass;

         for(FieldNode field : classNode.fields) {
            if ((field.access & 4096) != 0 && field.name.startsWith("this$")) {
               isProbablyStatic = false;
               if (outerName == null) {
                  outerName = field.desc;
                  if (outerName != null && outerName.startsWith("L") && outerName.endsWith(";")) {
                     outerName = outerName.substring(1, outerName.length() - 1);
                  }
               }
            }

            this.fields.add(new Field(field, this.isMixin));
         }

         this.isProbablyStatic = isProbablyStatic;
         this.methodMapper = new MethodMapper(MixinEnvironment.getCurrentEnvironment(), this);
         this.signature = ClassSignature.ofLazy(classNode);
         int access = classNode.access;
         boolean isInner = outerName != null;

         for(InnerClassNode innerClass : classNode.innerClasses) {
            if (this.name.equals(innerClass.name)) {
               access = innerClass.access;
               isInner = true;
               outerName = innerClass.outerName;
            }
         }

         this.access = access;
         this.isInner = isInner;
         this.outerName = outerName;
         if (MixinEnvironment.getCompatibilityLevel().supports(8)) {
            this.nestHost = ClassNodeAdapter.getNestHostClass(classNode);
            List<String> nestMembers = ClassNodeAdapter.getNestMembers(classNode);
            if (nestMembers != null) {
               this.nestMembers = new LinkedHashSet();
               this.nestMembers.addAll(nestMembers);
            }
         }
      } finally {
         timer.end();
      }

   }

   private ClassInfo(Class<?> cls) {
      this.name = getName(cls);
      this.superName = cls.getSuperclass() != null ? getName(cls.getSuperclass()) : "java/lang/Object";
      this.initialisers = new HashSet();
      this.methods = new HashSet();
      this.fields = new HashSet();
      this.isInterface = cls.isInterface();
      Class<?>[] interfaces = cls.getInterfaces();
      this.interfaces = new HashSet(interfaces.length);
      this.isMixin = false;
      this.mixin = null;
      this.mixins = Collections.emptySet();

      for(Class<?> iface : interfaces) {
         this.interfaces.add(getName(iface));
      }

      for(Constructor<?> ctor : cls.getDeclaredConstructors()) {
         if ((ctor.getModifiers() & 5) != 0) {
            this.initialisers.add(new Method(ctor.getName(), org.objectweb.asm.Type.getConstructorDescriptor(ctor), ctor.getModifiers()));
         }
      }

      for(java.lang.reflect.Method method : cls.getDeclaredMethods()) {
         if ((method.getModifiers() & 5) != 0) {
            this.methods.add(new Method(method.getName(), org.objectweb.asm.Type.getMethodDescriptor(method), method.getModifiers()));
         }
      }

      for(java.lang.reflect.Field field : cls.getDeclaredFields()) {
         if ((field.getModifiers() & 5) != 0) {
            this.fields.add(new Field(field.getName(), org.objectweb.asm.Type.getDescriptor(field.getType()), field.getModifiers()));
         }
      }

      this.isProbablyStatic = cls.getEnclosingClass() == null || Modifier.isStatic(cls.getModifiers());
      this.methodMapper = null;
      this.access = cls.getModifiers();
      this.isInner = cls.getEnclosingClass() != null;
      this.outerName = cls.getDeclaringClass() != null ? getName(cls.getDeclaringClass()) : null;
   }

   private static String getName(Class<?> cls) {
      return cls.getName().replace('.', '/');
   }

   void addInterface(String iface) {
      this.interfaces.add(iface);
      this.getSignature().addInterface(iface);
   }

   void addMethod(MethodNode method) {
      this.addMethod(method, true);
   }

   private void addMethod(MethodNode method, boolean injected) {
      if (method.name.startsWith("<")) {
         this.initialisers.add(new Method(method, injected));
      } else {
         this.methods.add(new Method(method, injected));
      }

   }

   void addMixin(MixinInfo mixin) {
      if (this.isMixin) {
         throw new IllegalArgumentException("Cannot add target " + this.name + " for " + mixin.getClassName() + " because the target is a mixin");
      } else {
         this.mixins.add(mixin);
      }
   }

   void addAppliedMixin(MixinInfo mixin) {
      if (this.appliedMixins == null) {
         this.appliedMixins = new HashSet();
      }

      this.appliedMixins.add(mixin);
   }

   public Set<IMixinInfo> getAppliedMixins() {
      return this.appliedMixins != null ? Collections.unmodifiableSet(this.appliedMixins) : Collections.emptySet();
   }

   public boolean isMixin() {
      return this.isMixin;
   }

   public boolean isLoadable() {
      return this.mixin != null && this.mixin.isLoadable();
   }

   public boolean isPublic() {
      return (this.access & 1) != 0;
   }

   public boolean isReallyPublic() {
      boolean isPublic = this.isPublic();
      if (this.isInner && isPublic) {
         ClassInfo outer = this;

         while(outer != null && outer.outerName != null) {
            outer = forName(outer.outerName);
            if (outer != null && !outer.isPublic()) {
               return false;
            }
         }

         return true;
      } else {
         return isPublic;
      }
   }

   public boolean isProtected() {
      return (this.access & 4) != 0;
   }

   public boolean isPrivate() {
      return (this.access & 2) != 0;
   }

   public boolean isAbstract() {
      return (this.access & 1024) != 0;
   }

   public boolean isSynthetic() {
      return (this.access & 4096) != 0;
   }

   public boolean isFinal() {
      return (this.access & 16) != 0;
   }

   public boolean isProbablyStatic() {
      return this.isProbablyStatic;
   }

   public boolean isInner() {
      return this.isInner;
   }

   public boolean isInterface() {
      return this.isInterface;
   }

   public boolean isEnum() {
      return (this.access & 16384) != 0 && this.superName.equals("java/lang/Enum");
   }

   public Set<String> getInterfaces() {
      return Collections.unmodifiableSet(this.interfaces);
   }

   public String toString() {
      return this.name;
   }

   MethodMapper getMethodMapper() {
      return this.methodMapper;
   }

   public int getAccess() {
      return this.access;
   }

   public String getName() {
      return this.name;
   }

   public String getClassName() {
      return this.name.replace('/', '.');
   }

   public String getSimpleName() {
      int pos = this.name.lastIndexOf(47);
      return pos < 0 ? this.name : this.name.substring(pos + 1);
   }

   public org.objectweb.asm.Type getType() {
      return org.objectweb.asm.Type.getObjectType(this.name);
   }

   public String getSuperName() {
      return this.superName;
   }

   public ClassInfo getSuperClass() {
      if (this.superClass == null && this.superName != null) {
         this.superClass = forName(this.superName);
      }

      return this.superClass;
   }

   public String getOuterName() {
      return this.outerName;
   }

   public ClassInfo getOuterClass() {
      if (this.outerClass == null && this.outerName != null) {
         this.outerClass = forName(this.outerName);
      }

      return this.outerClass;
   }

   public ClassSignature getSignature() {
      return this.signature.wake();
   }

   public String getNestHost() {
      return this.nestHost;
   }

   public Set<String> getNestMembers() {
      return this.nestMembers != null ? Collections.unmodifiableSet(this.nestMembers) : Collections.emptySet();
   }

   public ClassInfo resolveNestHost() {
      return !Strings.isNullOrEmpty(this.nestHost) ? forName(this.nestHost) : this;
   }

   List<ClassInfo> getTargets() {
      if (this.mixin != null) {
         List<ClassInfo> targets = new ArrayList();
         targets.add(this);
         targets.addAll(this.mixin.getTargets());
         return targets;
      } else {
         return ImmutableList.<ClassInfo>of(this);
      }
   }

   public Set<Method> getMethods() {
      return Collections.unmodifiableSet(this.methods);
   }

   public Set<Method> getInterfaceMethods(boolean includeMixins) {
      Set<Method> methods = new HashSet();
      ClassInfo supClass = this.addMethodsRecursive(methods, includeMixins);
      if (!this.isInterface) {
         while(supClass != null && supClass != OBJECT) {
            supClass = supClass.addMethodsRecursive(methods, includeMixins);
         }
      }

      Iterator<Method> it = methods.iterator();

      while(it.hasNext()) {
         if (!((Method)it.next()).isAbstract()) {
            it.remove();
         }
      }

      return Collections.unmodifiableSet(methods);
   }

   private ClassInfo addMethodsRecursive(Set<Method> methods, boolean includeMixins) {
      if (this.isInterface) {
         for(Method method : this.methods) {
            if (!method.isAbstract()) {
               methods.remove(method);
            }

            methods.add(method);
         }
      } else if (!this.isMixin && includeMixins) {
         for(MixinInfo mixin : this.mixins) {
            mixin.getClassInfo().addMethodsRecursive(methods, includeMixins);
         }
      }

      for(String iface : this.interfaces) {
         forName(iface).addMethodsRecursive(methods, includeMixins);
      }

      return this.getSuperClass();
   }

   public boolean hasSuperClass(Class<?> superClass) {
      return this.hasSuperClass(superClass, ClassInfo.Traversal.NONE, superClass.isInterface());
   }

   public boolean hasSuperClass(Class<?> superClass, Traversal traversal) {
      return this.hasSuperClass(superClass, traversal, superClass.isInterface());
   }

   public boolean hasSuperClass(Class<?> superClass, Traversal traversal, boolean includeInterfaces) {
      String internalName = org.objectweb.asm.Type.getInternalName(superClass);
      if ("java/lang/Object".equals(internalName)) {
         return true;
      } else {
         return this.findSuperClass(internalName, traversal) != null;
      }
   }

   public boolean hasSuperClass(String superClass) {
      return this.hasSuperClass(superClass, ClassInfo.Traversal.NONE, false);
   }

   public boolean hasSuperClass(String superClass, Traversal traversal) {
      return this.hasSuperClass(superClass, traversal, false);
   }

   public boolean hasSuperClass(String superClass, Traversal traversal, boolean includeInterfaces) {
      if ("java/lang/Object".equals(superClass)) {
         return true;
      } else {
         return this.findSuperClass(superClass, traversal) != null;
      }
   }

   public boolean hasSuperClass(ClassInfo superClass) {
      return this.hasSuperClass(superClass, ClassInfo.Traversal.NONE, false);
   }

   public boolean hasSuperClass(ClassInfo superClass, Traversal traversal) {
      return this.hasSuperClass(superClass, traversal, false);
   }

   public boolean hasSuperClass(ClassInfo superClass, Traversal traversal, boolean includeInterfaces) {
      if (OBJECT == superClass) {
         return true;
      } else {
         return this.findSuperClass(superClass.name, traversal, includeInterfaces) != null;
      }
   }

   public ClassInfo findSuperClass(String superClass) {
      return this.findSuperClass(superClass, ClassInfo.Traversal.NONE);
   }

   public ClassInfo findSuperClass(String superClass, Traversal traversal) {
      return this.findSuperClass(superClass, traversal, false, new HashSet());
   }

   public ClassInfo findSuperClass(String superClass, Traversal traversal, boolean includeInterfaces) {
      return OBJECT.name.equals(superClass) ? null : this.findSuperClass(superClass, traversal, includeInterfaces, new HashSet());
   }

   private ClassInfo findSuperClass(String superClass, Traversal traversal, boolean includeInterfaces, Set<String> traversed) {
      ClassInfo superClassInfo = this.getSuperClass();
      if (superClassInfo != null) {
         for(ClassInfo superTarget : superClassInfo.getTargets()) {
            if (superClass.equals(superTarget.getName())) {
               return superClassInfo;
            }

            ClassInfo found = superTarget.findSuperClass(superClass, traversal.next(), includeInterfaces, traversed);
            if (found != null) {
               return found;
            }
         }
      }

      if (includeInterfaces) {
         ClassInfo iface = this.findInterface(superClass);
         if (iface != null) {
            return iface;
         }
      }

      if (traversal.canTraverse()) {
         for(MixinInfo mixin : this.mixins) {
            String mixinClassName = mixin.getClassName();
            if (!traversed.contains(mixinClassName)) {
               traversed.add(mixinClassName);
               ClassInfo mixinClass = mixin.getClassInfo();
               if (superClass.equals(mixinClass.getName())) {
                  return mixinClass;
               }

               ClassInfo targetSuper = mixinClass.findSuperClass(superClass, ClassInfo.Traversal.ALL, includeInterfaces, traversed);
               if (targetSuper != null) {
                  return targetSuper;
               }
            }
         }
      }

      return null;
   }

   private ClassInfo findInterface(String superClass) {
      for(String ifaceName : this.getInterfaces()) {
         ClassInfo iface = forName(ifaceName);
         if (superClass.equals(ifaceName)) {
            return iface;
         }

         ClassInfo superIface = iface.findInterface(superClass);
         if (superIface != null) {
            return superIface;
         }
      }

      return null;
   }

   ClassInfo findCorrespondingType(ClassInfo mixin) {
      if (mixin != null && mixin.isMixin && !this.isMixin) {
         ClassInfo correspondingType = (ClassInfo)this.correspondingTypes.get(mixin);
         if (correspondingType == null) {
            correspondingType = this.findSuperTypeForMixin(mixin);
            this.correspondingTypes.put(mixin, correspondingType);
         }

         return correspondingType;
      } else {
         return null;
      }
   }

   private ClassInfo findSuperTypeForMixin(ClassInfo mixin) {
      for(ClassInfo superClass = this; superClass != null && superClass != OBJECT; superClass = superClass.getSuperClass()) {
         for(MixinInfo minion : superClass.mixins) {
            if (minion.getClassInfo().equals(mixin)) {
               return superClass;
            }
         }
      }

      return null;
   }

   public boolean hasMixinInHierarchy() {
      if (!this.isMixin) {
         return false;
      } else {
         for(ClassInfo supClass = this.getSuperClass(); supClass != null && supClass != OBJECT; supClass = supClass.getSuperClass()) {
            if (supClass.isMixin) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean hasMixinTargetInHierarchy() {
      if (this.isMixin) {
         return false;
      } else {
         for(ClassInfo supClass = this.getSuperClass(); supClass != null && supClass != OBJECT; supClass = supClass.getSuperClass()) {
            if (supClass.mixins.size() > 0) {
               return true;
            }
         }

         return false;
      }
   }

   public Method findMethodInHierarchy(MethodNode method, SearchType searchType) {
      return this.findMethodInHierarchy(method.name, method.desc, searchType, ClassInfo.Traversal.NONE);
   }

   public Method findMethodInHierarchy(MethodNode method, SearchType searchType, Traversal traversal) {
      return this.findMethodInHierarchy(method.name, method.desc, searchType, traversal, 0);
   }

   public Method findMethodInHierarchy(MethodNode method, SearchType searchType, int flags) {
      return this.findMethodInHierarchy(method.name, method.desc, searchType, ClassInfo.Traversal.NONE, flags);
   }

   public Method findMethodInHierarchy(MethodNode method, SearchType searchType, Traversal traversal, int flags) {
      return this.findMethodInHierarchy(method.name, method.desc, searchType, traversal, flags);
   }

   public Method findMethodInHierarchy(MethodInsnNode method, SearchType searchType) {
      return this.findMethodInHierarchy(method.name, method.desc, searchType, ClassInfo.Traversal.NONE);
   }

   public Method findMethodInHierarchy(MethodInsnNode method, SearchType searchType, int flags) {
      return this.findMethodInHierarchy(method.name, method.desc, searchType, ClassInfo.Traversal.NONE, flags);
   }

   public Method findMethodInHierarchy(String name, String desc, SearchType searchType, int flags) {
      return this.findMethodInHierarchy(name, desc, searchType, ClassInfo.Traversal.NONE, flags);
   }

   public Method findMethodInHierarchy(String name, String desc, SearchType searchType, Traversal traversal) {
      return this.findMethodInHierarchy(name, desc, searchType, traversal, 0);
   }

   public Method findMethodInHierarchy(String name, String desc, SearchType searchType, Traversal traversal, int flags) {
      return (Method)this.findInHierarchy(name, desc, searchType, traversal, flags, ClassInfo.Member.Type.METHOD);
   }

   public Field findFieldInHierarchy(FieldNode field, SearchType searchType) {
      return this.findFieldInHierarchy(field.name, field.desc, searchType, ClassInfo.Traversal.NONE);
   }

   public Field findFieldInHierarchy(FieldNode field, SearchType searchType, int flags) {
      return this.findFieldInHierarchy(field.name, field.desc, searchType, ClassInfo.Traversal.NONE, flags);
   }

   public Field findFieldInHierarchy(FieldInsnNode field, SearchType searchType) {
      return this.findFieldInHierarchy(field.name, field.desc, searchType, ClassInfo.Traversal.NONE);
   }

   public Field findFieldInHierarchy(FieldInsnNode field, SearchType searchType, int flags) {
      return this.findFieldInHierarchy(field.name, field.desc, searchType, ClassInfo.Traversal.NONE, flags);
   }

   public Field findFieldInHierarchy(String name, String desc, SearchType searchType) {
      return this.findFieldInHierarchy(name, desc, searchType, ClassInfo.Traversal.NONE);
   }

   public Field findFieldInHierarchy(String name, String desc, SearchType searchType, int flags) {
      return this.findFieldInHierarchy(name, desc, searchType, ClassInfo.Traversal.NONE, flags);
   }

   public Field findFieldInHierarchy(String name, String desc, SearchType searchType, Traversal traversal) {
      return this.findFieldInHierarchy(name, desc, searchType, traversal, 0);
   }

   public Field findFieldInHierarchy(String name, String desc, SearchType searchType, Traversal traversal, int flags) {
      return (Field)this.findInHierarchy(name, desc, searchType, traversal, flags, ClassInfo.Member.Type.FIELD);
   }

   private <M extends Member> M findInHierarchy(String name, String desc, SearchType searchType, Traversal traversal, int flags, Member.Type type) {
      if (searchType == ClassInfo.SearchType.ALL_CLASSES) {
         M member = this.findMember(name, desc, flags, type);
         if (member != null) {
            return member;
         }

         if (traversal.canTraverse()) {
            for(MixinInfo mixin : this.mixins) {
               M mixinMember = mixin.getClassInfo().findMember(name, desc, flags, type);
               if (mixinMember != null) {
                  return (M)this.cloneMember(mixinMember);
               }
            }
         }
      }

      ClassInfo superClassInfo = this.getSuperClass();
      if (superClassInfo != null) {
         for(ClassInfo superTarget : superClassInfo.getTargets()) {
            M member = superTarget.findInHierarchy(name, desc, ClassInfo.SearchType.ALL_CLASSES, traversal.next(), flags & -3, type);
            if (member != null) {
               return member;
            }
         }
      }

      if (type == ClassInfo.Member.Type.METHOD && (this.isInterface || MixinEnvironment.getCompatibilityLevel().supports(1))) {
         for(String implemented : this.interfaces) {
            ClassInfo iface = forName(implemented);
            if (iface == null) {
               logger.debug("Failed to resolve declared interface {} on {}", implemented, this.name);
            } else {
               M member = iface.findInHierarchy(name, desc, ClassInfo.SearchType.ALL_CLASSES, traversal.next(), flags & -3, type);
               if (member != null) {
                  return (M)(this.isInterface ? member : new InterfaceMethod(member));
               }
            }
         }
      }

      return null;
   }

   private <M extends Member> M cloneMember(M member) {
      return (M)(member instanceof Method ? new Method(member) : new Field(member));
   }

   public Method findMethod(MethodNode method) {
      return this.findMethod(method.name, method.desc, method.access);
   }

   public Method findMethod(MethodNode method, int flags) {
      return this.findMethod(method.name, method.desc, flags);
   }

   public Method findMethod(MethodInsnNode method) {
      return this.findMethod(method.name, method.desc, 0);
   }

   public Method findMethod(MethodInsnNode method, int flags) {
      return this.findMethod(method.name, method.desc, flags);
   }

   public Method findMethod(String name, String desc, int flags) {
      return (Method)this.findMember(name, desc, flags, ClassInfo.Member.Type.METHOD);
   }

   public Field findField(FieldNode field) {
      return this.findField(field.name, field.desc, field.access);
   }

   public Field findField(FieldInsnNode field, int flags) {
      return this.findField(field.name, field.desc, flags);
   }

   public Field findField(String name, String desc, int flags) {
      return (Field)this.findMember(name, desc, flags, ClassInfo.Member.Type.FIELD);
   }

   private <M extends Member> M findMember(String name, String desc, int flags, Member.Type memberType) {
      for(M member : memberType == ClassInfo.Member.Type.METHOD ? this.methods : this.fields) {
         if (member.equals(name, desc) && member.matchesFlags(flags)) {
            return member;
         }
      }

      if (memberType == ClassInfo.Member.Type.METHOD && (flags & 262144) != 0) {
         for(Method ctor : this.initialisers) {
            if (ctor.equals(name, desc) && ctor.matchesFlags(flags)) {
               return (M)ctor;
            }
         }
      }

      return null;
   }

   public boolean equals(Object other) {
      return !(other instanceof ClassInfo) ? false : ((ClassInfo)other).name.equals(this.name);
   }

   public int hashCode() {
      return this.name.hashCode();
   }

   static ClassInfo fromClassNode(ClassNode classNode) {
      ClassInfo info = (ClassInfo)cache.get(classNode.name);
      if (info == null) {
         info = new ClassInfo(classNode);
         cache.put(classNode.name, info);
      }

      return info;
   }

   public static ClassInfo forName(String className) {
      className = className.replace('.', '/');
      ClassInfo info = (ClassInfo)cache.get(className);
      if (!cache.containsKey(className)) {
         try {
            if (className.startsWith("java/")) {
               info = new ClassInfo(Class.forName(className.replace('/', '.'), false, ClassInfo.class.getClassLoader()));
            } else {
               int flags = MixinEnvironment.getCurrentEnvironment().getOption(MixinEnvironment.Option.CLASSREADER_EXPAND_FRAMES) ? 8 : 0;
               ClassNode classNode = MixinService.getService().getBytecodeProvider().getClassNode(className, true, flags);
               info = new ClassInfo(classNode);
            }
         } catch (Exception ex) {
            logger.catching(Level.TRACE, ex);
            logger.warn("Error loading class: {} ({}: {})", className, ex.getClass().getName(), ex.getMessage());
         }

         cache.put(className, info);
         logger.trace("Added class metadata for {} to metadata cache", className);
      }

      return info;
   }

   public static ClassInfo forDescriptor(String descriptor, TypeLookup lookup) {
      org.objectweb.asm.Type type;
      try {
         type = org.objectweb.asm.Type.getObjectType(descriptor);
      } catch (IllegalArgumentException var4) {
         logger.warn("Error resolving type from descriptor: {}", descriptor);
         return null;
      }

      return forType(type, lookup);
   }

   public static ClassInfo forType(org.objectweb.asm.Type type, TypeLookup lookup) {
      if (type.getSort() == 9) {
         return lookup == ClassInfo.TypeLookup.ELEMENT_TYPE ? forType(type.getElementType(), ClassInfo.TypeLookup.ELEMENT_TYPE) : OBJECT;
      } else {
         return type.getSort() < 9 ? null : forName(type.getClassName().replace('.', '/'));
      }
   }

   public static ClassInfo fromCache(String className) {
      return (ClassInfo)cache.get(className.replace('.', '/'));
   }

   public static ClassInfo fromCache(org.objectweb.asm.Type type, TypeLookup lookup) {
      if (type.getSort() == 9) {
         return lookup == ClassInfo.TypeLookup.ELEMENT_TYPE ? fromCache(type.getElementType(), ClassInfo.TypeLookup.ELEMENT_TYPE) : OBJECT;
      } else {
         return type.getSort() < 9 ? null : fromCache(type.getClassName());
      }
   }

   public static ClassInfo getCommonSuperClass(String type1, String type2) {
      return type1 != null && type2 != null ? getCommonSuperClass(forName(type1), forName(type2)) : OBJECT;
   }

   public static ClassInfo getCommonSuperClass(org.objectweb.asm.Type type1, org.objectweb.asm.Type type2) {
      return type1 != null && type2 != null && type1.getSort() == 10 && type2.getSort() == 10 ? getCommonSuperClass(forType(type1, ClassInfo.TypeLookup.DECLARED_TYPE), forType(type2, ClassInfo.TypeLookup.DECLARED_TYPE)) : OBJECT;
   }

   private static ClassInfo getCommonSuperClass(ClassInfo type1, ClassInfo type2) {
      return type1 != null && type2 != null ? getCommonSuperClass(type1, type2, false) : OBJECT;
   }

   public static ClassInfo getCommonSuperClassOrInterface(String type1, String type2) {
      return type1 != null && type2 != null ? getCommonSuperClassOrInterface(forName(type1), forName(type2)) : OBJECT;
   }

   public static ClassInfo getCommonSuperClassOrInterface(org.objectweb.asm.Type type1, org.objectweb.asm.Type type2) {
      return type1 != null && type2 != null && type1.getSort() == 10 && type2.getSort() == 10 ? getCommonSuperClassOrInterface(forType(type1, ClassInfo.TypeLookup.DECLARED_TYPE), forType(type2, ClassInfo.TypeLookup.DECLARED_TYPE)) : OBJECT;
   }

   public static ClassInfo getCommonSuperClassOrInterface(ClassInfo type1, ClassInfo type2) {
      return getCommonSuperClass(type1, type2, true);
   }

   private static ClassInfo getCommonSuperClass(ClassInfo type1, ClassInfo type2, boolean includeInterfaces) {
      if (type1.hasSuperClass(type2, ClassInfo.Traversal.NONE, includeInterfaces)) {
         return type2;
      } else if (type2.hasSuperClass(type1, ClassInfo.Traversal.NONE, includeInterfaces)) {
         return type1;
      } else if (!type1.isInterface() && !type2.isInterface()) {
         do {
            type1 = type1.getSuperClass();
            if (type1 == null) {
               return OBJECT;
            }
         } while(!type2.hasSuperClass(type1, ClassInfo.Traversal.NONE, includeInterfaces));

         return type1;
      } else {
         return OBJECT;
      }
   }

   public static boolean isMixin(String className) {
      ClassInfo cachedInfo = fromCache(className);
      return cachedInfo == null ? false : cachedInfo.isMixin();
   }

   static {
      cache.put("java/lang/Object", OBJECT);
   }

   public static enum SearchType {
      ALL_CLASSES,
      SUPER_CLASSES_ONLY;

      // $FF: synthetic method
      private static SearchType[] $values() {
         return new SearchType[]{ALL_CLASSES, SUPER_CLASSES_ONLY};
      }
   }

   public static enum TypeLookup {
      DECLARED_TYPE,
      ELEMENT_TYPE;

      // $FF: synthetic method
      private static TypeLookup[] $values() {
         return new TypeLookup[]{DECLARED_TYPE, ELEMENT_TYPE};
      }
   }

   public static enum Traversal {
      NONE((Traversal)null, false, ClassInfo.SearchType.SUPER_CLASSES_ONLY),
      ALL((Traversal)null, true, ClassInfo.SearchType.ALL_CLASSES),
      IMMEDIATE(NONE, true, ClassInfo.SearchType.SUPER_CLASSES_ONLY),
      SUPER(ALL, false, ClassInfo.SearchType.SUPER_CLASSES_ONLY);

      private final Traversal next;
      private final boolean traverse;
      private final SearchType searchType;

      private Traversal(Traversal next, boolean traverse, SearchType searchType) {
         this.next = next != null ? next : this;
         this.traverse = traverse;
         this.searchType = searchType;
      }

      public Traversal next() {
         return this.next;
      }

      public boolean canTraverse() {
         return this.traverse;
      }

      public SearchType getSearchType() {
         return this.searchType;
      }

      // $FF: synthetic method
      private static Traversal[] $values() {
         return new Traversal[]{NONE, ALL, IMMEDIATE, SUPER};
      }
   }

   public static class FrameData {
      private static final String[] FRAMETYPES = new String[]{"NEW", "FULL", "APPEND", "CHOP", "SAME", "SAME1"};
      public final int index;
      public final int type;
      public final int locals;
      public final int size;
      public final int rawSize;

      FrameData(int index, FrameNode frameNode, int initialFrameSize) {
         this.index = index;
         this.type = frameNode.type;
         this.locals = frameNode.local != null ? frameNode.local.size() : 0;
         this.rawSize = Locals.computeFrameSize(frameNode, 0);
         this.size = Math.max(this.rawSize, initialFrameSize);
      }

      public String toString() {
         return String.format("FrameData[index=%d, type=%s, locals=%d size=%d]", this.index, FRAMETYPES[this.type + 1], this.locals, this.size);
      }
   }

   abstract static class Member {
      private final Type type;
      private final String memberName;
      private final String memberDesc;
      private final boolean isInjected;
      private final int modifiers;
      private String currentName;
      private String currentDesc;
      private boolean decoratedFinal;
      private boolean decoratedMutable;
      private boolean unique;

      protected Member(Member member) {
         this(member.type, member.memberName, member.memberDesc, member.modifiers, member.isInjected);
         this.currentName = member.currentName;
         this.currentDesc = member.currentDesc;
         this.unique = member.unique;
      }

      protected Member(Type type, String name, String desc, int access) {
         this(type, name, desc, access, false);
      }

      protected Member(Type type, String name, String desc, int access, boolean injected) {
         this.type = type;
         this.memberName = name;
         this.memberDesc = desc;
         this.isInjected = injected;
         this.currentName = name;
         this.currentDesc = desc;
         this.modifiers = access;
      }

      public String getOriginalName() {
         return this.memberName;
      }

      public String getName() {
         return this.currentName;
      }

      public String getOriginalDesc() {
         return this.memberDesc;
      }

      public String getDesc() {
         return this.currentDesc;
      }

      public boolean isInjected() {
         return this.isInjected;
      }

      public boolean isRenamed() {
         return !this.currentName.equals(this.memberName);
      }

      public boolean isRemapped() {
         return !this.currentDesc.equals(this.memberDesc);
      }

      public boolean isPrivate() {
         return (this.modifiers & 2) != 0;
      }

      public boolean isStatic() {
         return (this.modifiers & 8) != 0;
      }

      public boolean isAbstract() {
         return (this.modifiers & 1024) != 0;
      }

      public boolean isFinal() {
         return (this.modifiers & 16) != 0;
      }

      public boolean isSynthetic() {
         return (this.modifiers & 4096) != 0;
      }

      public boolean isUnique() {
         return this.unique;
      }

      public void setUnique(boolean unique) {
         this.unique = unique;
      }

      public boolean isDecoratedFinal() {
         return this.decoratedFinal;
      }

      public boolean isDecoratedMutable() {
         return this.decoratedMutable;
      }

      protected void setDecoratedFinal(boolean decoratedFinal, boolean decoratedMutable) {
         this.decoratedFinal = decoratedFinal;
         this.decoratedMutable = decoratedMutable;
      }

      public boolean matchesFlags(int flags) {
         return ((~this.modifiers | flags & 2) & 2) != 0 && ((~this.modifiers | flags & 8) & 8) != 0;
      }

      public abstract ClassInfo getOwner();

      public ClassInfo getImplementor() {
         return this.getOwner();
      }

      public int getAccess() {
         return this.modifiers;
      }

      public String renameTo(String name) {
         this.currentName = name;
         return name;
      }

      public String remapTo(String desc) {
         this.currentDesc = desc;
         return desc;
      }

      public boolean equals(String name, String desc) {
         return (this.memberName.equals(name) || this.currentName.equals(name)) && (this.memberDesc.equals(desc) || this.currentDesc.equals(desc));
      }

      public boolean equals(Object obj) {
         if (!(obj instanceof Member)) {
            return false;
         } else {
            Member other = (Member)obj;
            return (other.memberName.equals(this.memberName) || other.currentName.equals(this.currentName)) && (other.memberDesc.equals(this.memberDesc) || other.currentDesc.equals(this.currentDesc));
         }
      }

      public int hashCode() {
         return this.toString().hashCode();
      }

      public String toString() {
         return String.format(this.getDisplayFormat(), this.memberName, this.memberDesc);
      }

      protected String getDisplayFormat() {
         return "%s%s";
      }

      static enum Type {
         METHOD,
         FIELD;

         // $FF: synthetic method
         private static Type[] $values() {
            return new Type[]{METHOD, FIELD};
         }
      }
   }

   public class Method extends Member {
      private final List<FrameData> frames;
      private boolean isAccessor;
      private boolean conformed;

      public Method(Member member) {
         super(member);
         this.frames = member instanceof Method ? ((Method)member).frames : null;
      }

      public Method(MethodNode method) {
         this(method, false);
      }

      public Method(MethodNode method, boolean injected) {
         super(ClassInfo.Member.Type.METHOD, method.name, method.desc, method.access, injected);
         this.frames = this.gatherFrames(method);
         this.setUnique(Annotations.getVisible(method, Unique.class) != null);
         this.isAccessor = Annotations.getSingleVisible(method, Accessor.class, Invoker.class) != null;
         boolean decoratedFinal = Annotations.getVisible(method, Final.class) != null;
         boolean decoratedMutable = Annotations.getVisible(method, Mutable.class) != null;
         this.setDecoratedFinal(decoratedFinal, decoratedMutable);
      }

      public Method(String name, String desc) {
         super(ClassInfo.Member.Type.METHOD, name, desc, 1, false);
         this.frames = null;
      }

      public Method(String name, String desc, int access) {
         super(ClassInfo.Member.Type.METHOD, name, desc, access, false);
         this.frames = null;
      }

      public Method(String name, String desc, int access, boolean injected) {
         super(ClassInfo.Member.Type.METHOD, name, desc, access, injected);
         this.frames = null;
      }

      private List<FrameData> gatherFrames(MethodNode method) {
         List<FrameData> frames = new ArrayList();
         Iterator<AbstractInsnNode> iter = method.instructions.iterator();

         while(iter.hasNext()) {
            AbstractInsnNode insn = (AbstractInsnNode)iter.next();
            if (insn instanceof FrameNode) {
               frames.add(new FrameData(method.instructions.indexOf(insn), (FrameNode)insn, Bytecode.getFirstNonArgLocalIndex(method)));
            }
         }

         return frames;
      }

      public List<FrameData> getFrames() {
         return this.frames;
      }

      public ClassInfo getOwner() {
         return ClassInfo.this;
      }

      public boolean isAccessor() {
         return this.isAccessor;
      }

      public boolean isConformed() {
         return this.conformed;
      }

      public String renameTo(String name) {
         this.conformed = false;
         return super.renameTo(name);
      }

      public String conform(String name) {
         boolean nameChanged = !name.equals(this.getName());
         if (this.conformed && nameChanged) {
            throw new IllegalStateException("Method " + this + " was already conformed. Original= " + this.getOriginalName() + " Current=" + this.getName() + " New=" + name);
         } else {
            if (nameChanged) {
               this.renameTo(name);
               this.conformed = true;
            }

            return name;
         }
      }

      public boolean equals(Object obj) {
         return !(obj instanceof Method) ? false : super.equals(obj);
      }
   }

   public class InterfaceMethod extends Method {
      private final ClassInfo owner;

      public InterfaceMethod(Member member) {
         super((Member)member);
         this.owner = member.getOwner();
      }

      public ClassInfo getOwner() {
         return this.owner;
      }

      public ClassInfo getImplementor() {
         return ClassInfo.this;
      }
   }

   public class Field extends Member {
      public Field(Member member) {
         super(member);
      }

      public Field(FieldNode field) {
         this(field, false);
      }

      public Field(FieldNode field, boolean injected) {
         super(ClassInfo.Member.Type.FIELD, field.name, field.desc, field.access, injected);
         this.setUnique(Annotations.getVisible(field, Unique.class) != null);
         if (Annotations.getVisible(field, Shadow.class) != null) {
            boolean decoratedFinal = Annotations.getVisible(field, Final.class) != null;
            boolean decoratedMutable = Annotations.getVisible(field, Mutable.class) != null;
            this.setDecoratedFinal(decoratedFinal, decoratedMutable);
         }

      }

      public Field(String name, String desc, int access) {
         super(ClassInfo.Member.Type.FIELD, name, desc, access, false);
      }

      public Field(String name, String desc, int access, boolean injected) {
         super(ClassInfo.Member.Type.FIELD, name, desc, access, injected);
      }

      public ClassInfo getOwner() {
         return ClassInfo.this;
      }

      public boolean equals(Object obj) {
         return !(obj instanceof Field) ? false : super.equals(obj);
      }

      protected String getDisplayFormat() {
         return "%s:%s";
      }
   }
}
