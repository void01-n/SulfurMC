package org.spongepowered.asm.util;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.asm.IAnnotationHandle;
import org.spongepowered.include.com.google.common.base.Function;
import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.common.collect.Lists;

public final class Annotations {
   private static final Class<?>[] MERGEABLE_MIXIN_ANNOTATIONS = new Class[]{Overwrite.class, Intrinsic.class, Final.class, Debug.class};
   private static Pattern mergeableAnnotationPattern = getMergeableAnnotationPattern();

   private Annotations() {
   }

   public static IAnnotationHandle handleOf(Object annotation) {
      if (annotation instanceof IAnnotationHandle) {
         return (IAnnotationHandle)annotation;
      } else if (annotation instanceof AnnotationNode) {
         return new Handle((AnnotationNode)annotation);
      } else if (annotation == null) {
         return null;
      } else {
         throw new IllegalArgumentException("Unsupported annotation type: " + annotation.getClass().getName());
      }
   }

   public static String getDesc(Class<? extends Annotation> annotationType) {
      return Type.getType(annotationType).getInternalName();
   }

   public static String getSimpleName(Class<? extends Annotation> annotationType) {
      return annotationType.getSimpleName();
   }

   public static String getSimpleName(AnnotationNode annotation) {
      return Bytecode.getSimpleName(annotation.desc);
   }

   public static void setVisible(FieldNode field, Class<? extends Annotation> annotationClass, Object... value) {
      AnnotationNode node = createNode(Type.getDescriptor(annotationClass), value);
      field.visibleAnnotations = add(field.visibleAnnotations, node);
   }

   public static void setInvisible(FieldNode field, Class<? extends Annotation> annotationClass, Object... value) {
      AnnotationNode node = createNode(Type.getDescriptor(annotationClass), value);
      field.invisibleAnnotations = add(field.invisibleAnnotations, node);
   }

   public static void setVisible(MethodNode method, Class<? extends Annotation> annotationClass, Object... value) {
      AnnotationNode node = createNode(Type.getDescriptor(annotationClass), value);
      method.visibleAnnotations = add(method.visibleAnnotations, node);
   }

   public static void setInvisible(MethodNode method, Class<? extends Annotation> annotationClass, Object... value) {
      AnnotationNode node = createNode(Type.getDescriptor(annotationClass), value);
      method.invisibleAnnotations = add(method.invisibleAnnotations, node);
   }

   private static AnnotationNode createNode(String annotationType, Object... value) {
      AnnotationNode node = new AnnotationNode(annotationType);

      for(int pos = 0; pos < value.length - 1; pos += 2) {
         if (!(value[pos] instanceof String)) {
            throw new IllegalArgumentException("Annotation keys must be strings, found " + value[pos].getClass().getSimpleName() + " with " + value[pos].toString() + " at index " + pos + " creating " + annotationType);
         }

         node.visit((String)value[pos], value[pos + 1]);
      }

      return node;
   }

   private static List<AnnotationNode> add(List<AnnotationNode> annotations, AnnotationNode node) {
      if (annotations == null) {
         annotations = new ArrayList(1);
      } else {
         annotations.remove(get(annotations, node.desc));
      }

      annotations.add(node);
      return annotations;
   }

   public static AnnotationNode getVisible(FieldNode field, Class<? extends Annotation> annotationClass) {
      return get(field.visibleAnnotations, Type.getDescriptor(annotationClass));
   }

   public static AnnotationNode getInvisible(FieldNode field, Class<? extends Annotation> annotationClass) {
      return get(field.invisibleAnnotations, Type.getDescriptor(annotationClass));
   }

   public static AnnotationNode getVisible(MethodNode method, Class<? extends Annotation> annotationClass) {
      return get(method.visibleAnnotations, Type.getDescriptor(annotationClass));
   }

   public static AnnotationNode getInvisible(MethodNode method, Class<? extends Annotation> annotationClass) {
      return get(method.invisibleAnnotations, Type.getDescriptor(annotationClass));
   }

   public static AnnotationNode getSingleVisible(MethodNode method, Class<? extends Annotation>... annotationClasses) {
      return getSingle(method.visibleAnnotations, annotationClasses);
   }

   public static AnnotationNode getSingleInvisible(MethodNode method, Class<? extends Annotation>... annotationClasses) {
      return getSingle(method.invisibleAnnotations, annotationClasses);
   }

   public static AnnotationNode getVisible(ClassNode classNode, Class<? extends Annotation> annotationClass) {
      return get(classNode.visibleAnnotations, Type.getDescriptor(annotationClass));
   }

   public static AnnotationNode getInvisible(ClassNode classNode, Class<? extends Annotation> annotationClass) {
      return get(classNode.invisibleAnnotations, Type.getDescriptor(annotationClass));
   }

   public static AnnotationNode getVisibleParameter(MethodNode method, Class<? extends Annotation> annotationClass, int paramIndex) {
      return paramIndex < 0 ? getVisible(method, annotationClass) : getParameter(method.visibleParameterAnnotations, Type.getDescriptor(annotationClass), paramIndex);
   }

   public static AnnotationNode getInvisibleParameter(MethodNode method, Class<? extends Annotation> annotationClass, int paramIndex) {
      return paramIndex < 0 ? getInvisible(method, annotationClass) : getParameter(method.invisibleParameterAnnotations, Type.getDescriptor(annotationClass), paramIndex);
   }

   public static AnnotationNode getParameter(List<AnnotationNode>[] parameterAnnotations, String annotationType, int paramIndex) {
      return parameterAnnotations != null && paramIndex >= 0 && paramIndex < parameterAnnotations.length ? get(parameterAnnotations[paramIndex], annotationType) : null;
   }

   public static AnnotationNode get(List<AnnotationNode> annotations, String annotationType) {
      if (annotations == null) {
         return null;
      } else {
         for(AnnotationNode annotation : annotations) {
            if (annotationType.equals(annotation.desc)) {
               return annotation;
            }
         }

         return null;
      }
   }

   private static AnnotationNode getSingle(List<AnnotationNode> annotations, Class<? extends Annotation>[] annotationClasses) {
      List<AnnotationNode> nodes = new ArrayList();

      for(Class<? extends Annotation> annotationClass : annotationClasses) {
         AnnotationNode annotation = get(annotations, Type.getDescriptor(annotationClass));
         if (annotation != null) {
            nodes.add(annotation);
         }
      }

      int foundNodes = nodes.size();
      if (foundNodes > 1) {
         throw new IllegalArgumentException("Conflicting annotations found: " + Lists.transform(nodes, new Function<AnnotationNode, String>() {
            public String apply(AnnotationNode input) {
               return input.desc;
            }
         }));
      } else {
         return foundNodes == 0 ? null : (AnnotationNode)nodes.get(0);
      }
   }

   public static <T> T getValue(AnnotationNode annotation) {
      return (T)getValue(annotation, "value");
   }

   public static <T> T getValue(AnnotationNode annotation, String key, T defaultValue) {
      T returnValue = (T)getValue(annotation, key);
      return (T)(returnValue != null ? returnValue : defaultValue);
   }

   public static <T> T getValue(AnnotationNode annotation, String key, Class<?> annotationClass) {
      Preconditions.checkNotNull(annotationClass, "annotationClass cannot be null");
      T value = (T)getValue(annotation, key);
      if (value == null) {
         try {
            value = (T)annotationClass.getDeclaredMethod(key).getDefaultValue();
         } catch (NoSuchMethodException var5) {
         }
      }

      return value;
   }

   public static <T> T getValue(AnnotationNode annotation, String key) {
      if (annotation != null && annotation.values != null) {
         for(int i = 0; i < annotation.values.size() - 1; i += 2) {
            if (annotation.values.get(i).equals(key)) {
               return (T)annotation.values.get(i + 1);
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public static <T extends Enum<T>> T getValue(AnnotationNode annotation, String key, Class<T> enumClass, T defaultValue) {
      String[] value = (String[])getValue(annotation, key);
      return (T)(value == null ? defaultValue : toEnumValue(enumClass, value));
   }

   public static <T> List<T> getValue(AnnotationNode annotation, String key, boolean notNull) {
      Object value = getValue(annotation, key);
      if (value instanceof List) {
         return (List)value;
      } else if (value != null) {
         List<T> list = new ArrayList();
         list.add(value);
         return list;
      } else {
         return Collections.emptyList();
      }
   }

   public static <T extends Enum<T>> List<T> getValue(AnnotationNode annotation, String key, boolean notNull, Class<T> enumClass) {
      Object value = getValue(annotation, key);
      if (!(value instanceof List)) {
         if (value instanceof String[]) {
            List<T> list = new ArrayList();
            list.add(toEnumValue(enumClass, (String[])value));
            return list;
         } else {
            return Collections.emptyList();
         }
      } else {
         ListIterator<Object> iter = ((List)value).listIterator();

         while(iter.hasNext()) {
            iter.set(toEnumValue(enumClass, (String[])iter.next()));
         }

         return (List)value;
      }
   }

   public static void setValue(AnnotationNode annotation, String key, Object value) {
      if (annotation != null) {
         int existingIndex = 0;
         if (annotation.values != null) {
            for(int pos = 0; pos < annotation.values.size() - 1; pos += 2) {
               String keyName = annotation.values.get(pos).toString();
               if (key.equals(keyName)) {
                  existingIndex = pos + 1;
                  break;
               }
            }
         } else {
            annotation.values = new ArrayList();
         }

         if (existingIndex > 0) {
            annotation.values.set(existingIndex, packValue(value));
         } else {
            annotation.values.add(key);
            annotation.values.add(packValue(value));
         }
      }
   }

   private static Object packValue(Object value) {
      Class<? extends Object> type = value.getClass();
      return type.isEnum() ? new String[]{Type.getDescriptor(type), value.toString()} : value;
   }

   public static void merge(ClassNode from, ClassNode to) {
      to.visibleAnnotations = merge(from.visibleAnnotations, to.visibleAnnotations, "class", from.name);
      to.invisibleAnnotations = merge(from.invisibleAnnotations, to.invisibleAnnotations, "class", from.name);
   }

   public static void merge(MethodNode from, MethodNode to) {
      to.visibleAnnotations = merge(from.visibleAnnotations, to.visibleAnnotations, "method", from.name);
      to.invisibleAnnotations = merge(from.invisibleAnnotations, to.invisibleAnnotations, "method", from.name);
   }

   public static void merge(FieldNode from, FieldNode to) {
      to.visibleAnnotations = merge(from.visibleAnnotations, to.visibleAnnotations, "field", from.name);
      to.invisibleAnnotations = merge(from.invisibleAnnotations, to.invisibleAnnotations, "field", from.name);
   }

   private static List<AnnotationNode> merge(List<AnnotationNode> from, List<AnnotationNode> to, String type, String name) {
      try {
         if (from == null) {
            return to;
         }

         if (to == null) {
            to = new ArrayList();
         }

         for(AnnotationNode annotation : from) {
            if (isMergeableAnnotation(annotation)) {
               Iterator<AnnotationNode> iter = to.iterator();

               while(iter.hasNext()) {
                  if (((AnnotationNode)iter.next()).desc.equals(annotation.desc)) {
                     iter.remove();
                     break;
                  }
               }

               to.add(annotation);
            }
         }
      } catch (Exception var7) {
         MixinService.getService().getLogger("mixin").warn("Exception encountered whilst merging annotations for {} {}", type, name);
      }

      return to;
   }

   private static boolean isMergeableAnnotation(AnnotationNode annotation) {
      return annotation.desc.startsWith("L" + Constants.MIXIN_PACKAGE_REF) ? mergeableAnnotationPattern.matcher(annotation.desc).matches() : true;
   }

   private static Pattern getMergeableAnnotationPattern() {
      StringBuilder sb = new StringBuilder("^L(");

      for(int i = 0; i < MERGEABLE_MIXIN_ANNOTATIONS.length; ++i) {
         if (i > 0) {
            sb.append('|');
         }

         sb.append(MERGEABLE_MIXIN_ANNOTATIONS[i].getName().replace('.', '/'));
      }

      return Pattern.compile(sb.append(");$").toString());
   }

   private static <T extends Enum<T>> T toEnumValue(Class<T> enumClass, String[] value) {
      if (!enumClass.getName().equals(Type.getType(value[0]).getClassName())) {
         throw new IllegalArgumentException("The supplied enum class does not match the stored enum value");
      } else {
         return (T)Enum.valueOf(enumClass, value[1]);
      }
   }

   public static class Handle implements IAnnotationHandle {
      private final AnnotationNode annotation;

      Handle(AnnotationNode annotation) {
         Preconditions.checkNotNull(annotation, "annotation");
         this.annotation = annotation;
      }

      public boolean exists() {
         return true;
      }

      public AnnotationNode getNode() {
         return this.annotation;
      }

      public String getDesc() {
         return Type.getType(this.annotation.desc).getInternalName();
      }

      public List<IAnnotationHandle> getAnnotationList(String key) {
         List<AnnotationNode> value = Annotations.<AnnotationNode>getValue(this.annotation, key, false);
         List<IAnnotationHandle> list = new ArrayList();
         if (value != null) {
            for(AnnotationNode node : value) {
               list.add(new Handle(node));
            }
         }

         return list;
      }

      public Type getTypeValue(String key) {
         return (Type)this.getValue(key, Type.VOID_TYPE);
      }

      public List<Type> getTypeList(String key) {
         return this.<Type>getList(key);
      }

      public IAnnotationHandle getAnnotation(String key) {
         AnnotationNode value = (AnnotationNode)Annotations.getValue(this.annotation, key);
         return value != null ? new Handle(value) : null;
      }

      public <T> T getValue(String key, T defaultValue) {
         return (T)Annotations.getValue(this.annotation, key, defaultValue);
      }

      public <T> T getValue() {
         return (T)this.getValue("value", (Object)null);
      }

      public <T> T getValue(String key) {
         return (T)this.getValue(key, (Object)null);
      }

      public boolean getBoolean(String key, boolean defaultValue) {
         return (Boolean)this.getValue(key, defaultValue);
      }

      public <T> List<T> getList() {
         return this.<T>getList("value");
      }

      public <T> List<T> getList(String key) {
         return Annotations.<T>getValue(this.annotation, key, false);
      }

      public String toString() {
         return "@" + Annotations.getSimpleName(this.annotation);
      }
   }
}
