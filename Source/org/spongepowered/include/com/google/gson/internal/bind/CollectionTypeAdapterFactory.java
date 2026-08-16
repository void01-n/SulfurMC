package org.spongepowered.include.com.google.gson.internal.bind;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import org.spongepowered.include.com.google.gson.Gson;
import org.spongepowered.include.com.google.gson.TypeAdapter;
import org.spongepowered.include.com.google.gson.TypeAdapterFactory;
import org.spongepowered.include.com.google.gson.internal.$Gson$Types;
import org.spongepowered.include.com.google.gson.internal.ConstructorConstructor;
import org.spongepowered.include.com.google.gson.internal.ObjectConstructor;
import org.spongepowered.include.com.google.gson.reflect.TypeToken;
import org.spongepowered.include.com.google.gson.stream.JsonReader;
import org.spongepowered.include.com.google.gson.stream.JsonToken;
import org.spongepowered.include.com.google.gson.stream.JsonWriter;

public final class CollectionTypeAdapterFactory implements TypeAdapterFactory {
   private final ConstructorConstructor constructorConstructor;

   public CollectionTypeAdapterFactory(ConstructorConstructor constructorConstructor) {
      this.constructorConstructor = constructorConstructor;
   }

   public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
      Type type = typeToken.getType();
      Class<? super T> rawType = typeToken.getRawType();
      if (!Collection.class.isAssignableFrom(rawType)) {
         return null;
      } else {
         Type elementType = $Gson$Types.getCollectionElementType(type, rawType);
         TypeAdapter<?> elementTypeAdapter = gson.getAdapter(TypeToken.get(elementType));
         ObjectConstructor<T> constructor = this.constructorConstructor.<T>get(typeToken);
         TypeAdapter<T> result = new Adapter<T>(gson, elementType, elementTypeAdapter, constructor);
         return result;
      }
   }

   private static final class Adapter<E> extends TypeAdapter<Collection<E>> {
      private final TypeAdapter<E> elementTypeAdapter;
      private final ObjectConstructor<? extends Collection<E>> constructor;

      public Adapter(Gson context, Type elementType, TypeAdapter<E> elementTypeAdapter, ObjectConstructor<? extends Collection<E>> constructor) {
         this.elementTypeAdapter = new TypeAdapterRuntimeTypeWrapper<E>(context, elementTypeAdapter, elementType);
         this.constructor = constructor;
      }

      public Collection<E> read(JsonReader in) throws IOException {
         if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
         } else {
            Collection<E> collection = this.constructor.construct();
            in.beginArray();

            while(in.hasNext()) {
               E instance = this.elementTypeAdapter.read(in);
               collection.add(instance);
            }

            in.endArray();
            return collection;
         }
      }

      public void write(JsonWriter out, Collection<E> collection) throws IOException {
         if (collection == null) {
            out.nullValue();
         } else {
            out.beginArray();

            for(E element : collection) {
               this.elementTypeAdapter.write(out, element);
            }

            out.endArray();
         }
      }
   }
}
