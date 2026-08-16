package org.spongepowered.include.com.google.gson.internal.bind;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.spongepowered.include.com.google.gson.Gson;
import org.spongepowered.include.com.google.gson.TypeAdapter;
import org.spongepowered.include.com.google.gson.TypeAdapterFactory;
import org.spongepowered.include.com.google.gson.internal.LinkedTreeMap;
import org.spongepowered.include.com.google.gson.reflect.TypeToken;
import org.spongepowered.include.com.google.gson.stream.JsonReader;
import org.spongepowered.include.com.google.gson.stream.JsonToken;
import org.spongepowered.include.com.google.gson.stream.JsonWriter;

public final class ObjectTypeAdapter extends TypeAdapter<Object> {
   public static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
      public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
         return type.getRawType() == Object.class ? new ObjectTypeAdapter(gson) : null;
      }
   };
   private final Gson gson;

   private ObjectTypeAdapter(Gson gson) {
      this.gson = gson;
   }

   public Object read(JsonReader in) throws IOException {
      JsonToken token = in.peek();
      switch (token) {
         case BEGIN_ARRAY:
            List<Object> list = new ArrayList();
            in.beginArray();

            while(in.hasNext()) {
               list.add(this.read(in));
            }

            in.endArray();
            return list;
         case BEGIN_OBJECT:
            Map<String, Object> map = new LinkedTreeMap<String, Object>();
            in.beginObject();

            while(in.hasNext()) {
               map.put(in.nextName(), this.read(in));
            }

            in.endObject();
            return map;
         case STRING:
            return in.nextString();
         case NUMBER:
            return in.nextDouble();
         case BOOLEAN:
            return in.nextBoolean();
         case NULL:
            in.nextNull();
            return null;
         default:
            throw new IllegalStateException();
      }
   }

   public void write(JsonWriter out, Object value) throws IOException {
      if (value == null) {
         out.nullValue();
      } else {
         TypeAdapter<Object> typeAdapter = this.gson.getAdapter(value.getClass());
         if (typeAdapter instanceof ObjectTypeAdapter) {
            out.beginObject();
            out.endObject();
         } else {
            typeAdapter.write(out, value);
         }
      }
   }
}
