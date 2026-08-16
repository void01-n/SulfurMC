package org.spongepowered.tools.obfuscation.mirror;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.objectweb.asm.Type;
import org.spongepowered.asm.util.asm.IAnnotationHandle;
import org.spongepowered.include.com.google.common.collect.ImmutableList;

public final class AnnotationHandle implements IAnnotationHandle {
   public static final AnnotationHandle MISSING = new AnnotationHandle((AnnotationMirror)null);
   private final AnnotationMirror annotation;

   private AnnotationHandle(AnnotationMirror annotation) {
      this.annotation = annotation;
   }

   public AnnotationMirror asMirror() {
      return this.annotation;
   }

   public boolean exists() {
      return this.annotation != null;
   }

   public String getDesc() {
      return this.annotation == null ? "java/lang/Annotation" : TypeUtils.getInternalName(this.annotation.getAnnotationType());
   }

   public String toString() {
      return this.annotation == null ? "@{UnknownAnnotation}" : "@" + this.annotation.getAnnotationType().asElement().getSimpleName();
   }

   public <T> T getValue(String key, T defaultValue) {
      if (this.annotation == null) {
         return defaultValue;
      } else {
         AnnotationValue value = this.getAnnotationValue(key);
         if (defaultValue instanceof Enum && value != null) {
            VariableElement varValue = (VariableElement)value.getValue();
            return (T)(varValue == null ? defaultValue : Enum.valueOf(defaultValue.getClass(), varValue.getSimpleName().toString()));
         } else {
            return (T)(value != null ? value.getValue() : defaultValue);
         }
      }
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

   public IAnnotationHandle getAnnotation(String key) {
      Object value = this.getValue(key);
      if (value instanceof AnnotationMirror) {
         return of((AnnotationMirror)value);
      } else {
         if (value instanceof AnnotationValue) {
            Object mirror = ((AnnotationValue)value).getValue();
            if (mirror instanceof AnnotationMirror) {
               return of((AnnotationMirror)mirror);
            }
         }

         return null;
      }
   }

   public <T> List<T> getList() {
      return this.<T>getList("value");
   }

   public <T> List<T> getList(String key) {
      List<AnnotationValue> list = (List)this.getValue(key, Collections.emptyList());
      return unwrapAnnotationValueList(list);
   }

   public List<IAnnotationHandle> getAnnotationList(String key) {
      Object val = this.getValue(key, (Object)null);
      if (val == null) {
         return Collections.emptyList();
      } else if (val instanceof AnnotationMirror) {
         return ImmutableList.<IAnnotationHandle>of(of((AnnotationMirror)val));
      } else {
         List<AnnotationValue> list = (List)val;
         List<AnnotationHandle> annotations = new ArrayList(list.size());

         for(AnnotationValue value : list) {
            annotations.add(new AnnotationHandle((AnnotationMirror)value.getValue()));
         }

         return Collections.unmodifiableList(annotations);
      }
   }

   public Type getTypeValue(String key) {
      TypeMirror typeMirror = (TypeMirror)this.getValue(key);
      return typeMirror == null ? Type.VOID_TYPE : Type.getType(TypeUtils.getInternalName(typeMirror));
   }

   public List<Type> getTypeList(String key) {
      List<Type> list = this.<Type>getList(key);
      ListIterator<Type> iter = list.listIterator();

      while(iter.hasNext()) {
         Object next = iter.next();
         if (next instanceof TypeMirror) {
            iter.set(Type.getType(TypeUtils.getInternalName((TypeMirror)next)));
         }
      }

      return list;
   }

   protected AnnotationValue getAnnotationValue(String key) {
      for(ExecutableElement elem : this.annotation.getElementValues().keySet()) {
         if (elem.getSimpleName().contentEquals(key)) {
            return (AnnotationValue)this.annotation.getElementValues().get(elem);
         }
      }

      return null;
   }

   protected static <T> List<T> unwrapAnnotationValueList(List<AnnotationValue> list) {
      if (list == null) {
         return Collections.emptyList();
      } else {
         List<T> unfolded = new ArrayList(list.size());

         for(AnnotationValue value : list) {
            unfolded.add(value.getValue());
         }

         return unfolded;
      }
   }

   protected static AnnotationMirror getAnnotation(Element elem, Class<? extends Annotation> annotationClass) {
      if (elem == null) {
         return null;
      } else {
         List<? extends AnnotationMirror> annotations = elem.getAnnotationMirrors();
         if (annotations == null) {
            return null;
         } else {
            for(AnnotationMirror annotation : annotations) {
               Element element = annotation.getAnnotationType().asElement();
               if (element instanceof TypeElement) {
                  TypeElement annotationElement = (TypeElement)element;
                  if (annotationElement.getQualifiedName().contentEquals(annotationClass.getName())) {
                     return annotation;
                  }
               }
            }

            return null;
         }
      }
   }

   public static AnnotationMirror asMirror(IAnnotationHandle handle) {
      return handle instanceof AnnotationHandle ? ((AnnotationHandle)handle).asMirror() : null;
   }

   public static AnnotationHandle of(AnnotationMirror annotation) {
      return new AnnotationHandle(annotation);
   }

   public static AnnotationHandle of(Element elem, Class<? extends Annotation> annotationClass) {
      return new AnnotationHandle(getAnnotation(elem, annotationClass));
   }
}
