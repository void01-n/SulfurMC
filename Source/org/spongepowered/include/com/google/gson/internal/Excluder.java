package org.spongepowered.include.com.google.gson.internal;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import org.spongepowered.include.com.google.gson.ExclusionStrategy;
import org.spongepowered.include.com.google.gson.FieldAttributes;
import org.spongepowered.include.com.google.gson.Gson;
import org.spongepowered.include.com.google.gson.TypeAdapter;
import org.spongepowered.include.com.google.gson.TypeAdapterFactory;
import org.spongepowered.include.com.google.gson.annotations.Expose;
import org.spongepowered.include.com.google.gson.annotations.Since;
import org.spongepowered.include.com.google.gson.annotations.Until;
import org.spongepowered.include.com.google.gson.reflect.TypeToken;
import org.spongepowered.include.com.google.gson.stream.JsonReader;
import org.spongepowered.include.com.google.gson.stream.JsonWriter;

public final class Excluder implements Cloneable, TypeAdapterFactory {
   public static final Excluder DEFAULT = new Excluder();
   private double version = (double)-1.0F;
   private int modifiers = 136;
   private boolean serializeInnerClasses = true;
   private boolean requireExpose;
   private List<ExclusionStrategy> serializationStrategies = Collections.emptyList();
   private List<ExclusionStrategy> deserializationStrategies = Collections.emptyList();

   protected Excluder clone() {
      try {
         return (Excluder)super.clone();
      } catch (CloneNotSupportedException var2) {
         throw new AssertionError();
      }
   }

   public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
      Class<?> rawType = type.getRawType();
      final boolean skipSerialize = this.excludeClass(rawType, true);
      final boolean skipDeserialize = this.excludeClass(rawType, false);
      return !skipSerialize && !skipDeserialize ? null : new TypeAdapter<T>() {
         private TypeAdapter<T> delegate;

         public T read(JsonReader in) throws IOException {
            if (skipDeserialize) {
               in.skipValue();
               return null;
            } else {
               return (T)this.delegate().read(in);
            }
         }

         public void write(JsonWriter out, T value) throws IOException {
            if (skipSerialize) {
               out.nullValue();
            } else {
               this.delegate().write(out, value);
            }
         }

         private TypeAdapter<T> delegate() {
            TypeAdapter<T> d = this.delegate;
            return d != null ? d : (this.delegate = gson.<T>getDelegateAdapter(Excluder.this, type));
         }
      };
   }

   public boolean excludeField(Field field, boolean serialize) {
      if ((this.modifiers & field.getModifiers()) != 0) {
         return true;
      } else if (this.version != (double)-1.0F && !this.isValidVersion((Since)field.getAnnotation(Since.class), (Until)field.getAnnotation(Until.class))) {
         return true;
      } else if (field.isSynthetic()) {
         return true;
      } else {
         if (this.requireExpose) {
            Expose annotation = (Expose)field.getAnnotation(Expose.class);
            if (annotation == null) {
               return true;
            }

            if (serialize) {
               if (!annotation.serialize()) {
                  return true;
               }
            } else if (!annotation.deserialize()) {
               return true;
            }
         }

         if (!this.serializeInnerClasses && this.isInnerClass(field.getType())) {
            return true;
         } else if (this.isAnonymousOrLocal(field.getType())) {
            return true;
         } else {
            List<ExclusionStrategy> list = serialize ? this.serializationStrategies : this.deserializationStrategies;
            if (!list.isEmpty()) {
               FieldAttributes fieldAttributes = new FieldAttributes(field);

               for(ExclusionStrategy exclusionStrategy : list) {
                  if (exclusionStrategy.shouldSkipField(fieldAttributes)) {
                     return true;
                  }
               }
            }

            return false;
         }
      }
   }

   public boolean excludeClass(Class<?> clazz, boolean serialize) {
      if (this.version != (double)-1.0F && !this.isValidVersion((Since)clazz.getAnnotation(Since.class), (Until)clazz.getAnnotation(Until.class))) {
         return true;
      } else if (!this.serializeInnerClasses && this.isInnerClass(clazz)) {
         return true;
      } else if (this.isAnonymousOrLocal(clazz)) {
         return true;
      } else {
         for(ExclusionStrategy exclusionStrategy : serialize ? this.serializationStrategies : this.deserializationStrategies) {
            if (exclusionStrategy.shouldSkipClass(clazz)) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean isAnonymousOrLocal(Class<?> clazz) {
      return !Enum.class.isAssignableFrom(clazz) && (clazz.isAnonymousClass() || clazz.isLocalClass());
   }

   private boolean isInnerClass(Class<?> clazz) {
      return clazz.isMemberClass() && !this.isStatic(clazz);
   }

   private boolean isStatic(Class<?> clazz) {
      return (clazz.getModifiers() & 8) != 0;
   }

   private boolean isValidVersion(Since since, Until until) {
      return this.isValidSince(since) && this.isValidUntil(until);
   }

   private boolean isValidSince(Since annotation) {
      if (annotation != null) {
         double annotationVersion = annotation.value();
         if (annotationVersion > this.version) {
            return false;
         }
      }

      return true;
   }

   private boolean isValidUntil(Until annotation) {
      if (annotation != null) {
         double annotationVersion = annotation.value();
         if (annotationVersion <= this.version) {
            return false;
         }
      }

      return true;
   }
}
