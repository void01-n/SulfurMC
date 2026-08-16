package org.spongepowered.include.com.google.gson;

import java.io.IOException;
import org.spongepowered.include.com.google.gson.internal.$Gson$Preconditions;
import org.spongepowered.include.com.google.gson.internal.Streams;
import org.spongepowered.include.com.google.gson.reflect.TypeToken;
import org.spongepowered.include.com.google.gson.stream.JsonReader;
import org.spongepowered.include.com.google.gson.stream.JsonWriter;

final class TreeTypeAdapter<T> extends TypeAdapter<T> {
   private final JsonSerializer<T> serializer;
   private final JsonDeserializer<T> deserializer;
   private final Gson gson;
   private final TypeToken<T> typeToken;
   private final TypeAdapterFactory skipPast;
   private TypeAdapter<T> delegate;

   private TreeTypeAdapter(JsonSerializer<T> serializer, JsonDeserializer<T> deserializer, Gson gson, TypeToken<T> typeToken, TypeAdapterFactory skipPast) {
      this.serializer = serializer;
      this.deserializer = deserializer;
      this.gson = gson;
      this.typeToken = typeToken;
      this.skipPast = skipPast;
   }

   public T read(JsonReader in) throws IOException {
      if (this.deserializer == null) {
         return (T)this.delegate().read(in);
      } else {
         JsonElement value = Streams.parse(in);
         return (T)(value.isJsonNull() ? null : this.deserializer.deserialize(value, this.typeToken.getType(), this.gson.deserializationContext));
      }
   }

   public void write(JsonWriter out, T value) throws IOException {
      if (this.serializer == null) {
         this.delegate().write(out, value);
      } else if (value == null) {
         out.nullValue();
      } else {
         JsonElement tree = this.serializer.serialize(value, this.typeToken.getType(), this.gson.serializationContext);
         Streams.write(tree, out);
      }
   }

   private TypeAdapter<T> delegate() {
      TypeAdapter<T> d = this.delegate;
      return d != null ? d : (this.delegate = this.gson.<T>getDelegateAdapter(this.skipPast, this.typeToken));
   }

   public static TypeAdapterFactory newFactory(TypeToken<?> exactType, Object typeAdapter) {
      return new SingleTypeFactory(typeAdapter, exactType, false, (Class)null);
   }

   private static class SingleTypeFactory implements TypeAdapterFactory {
      private final TypeToken<?> exactType;
      private final boolean matchRawType;
      private final Class<?> hierarchyType;
      private final JsonSerializer<?> serializer;
      private final JsonDeserializer<?> deserializer;

      private SingleTypeFactory(Object typeAdapter, TypeToken<?> exactType, boolean matchRawType, Class<?> hierarchyType) {
         this.serializer = typeAdapter instanceof JsonSerializer ? (JsonSerializer)typeAdapter : null;
         this.deserializer = typeAdapter instanceof JsonDeserializer ? (JsonDeserializer)typeAdapter : null;
         $Gson$Preconditions.checkArgument(this.serializer != null || this.deserializer != null);
         this.exactType = exactType;
         this.matchRawType = matchRawType;
         this.hierarchyType = hierarchyType;
      }

      public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
         boolean matches = this.exactType != null ? this.exactType.equals(type) || this.matchRawType && this.exactType.getType() == type.getRawType() : this.hierarchyType.isAssignableFrom(type.getRawType());
         return matches ? new TreeTypeAdapter(this.serializer, this.deserializer, gson, type, this) : null;
      }
   }
}
