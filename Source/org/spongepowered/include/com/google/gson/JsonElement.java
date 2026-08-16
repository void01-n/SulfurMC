package org.spongepowered.include.com.google.gson;

import java.io.IOException;
import java.io.StringWriter;
import org.spongepowered.include.com.google.gson.internal.Streams;
import org.spongepowered.include.com.google.gson.stream.JsonWriter;

public abstract class JsonElement {
   public boolean isJsonArray() {
      return this instanceof JsonArray;
   }

   public boolean isJsonObject() {
      return this instanceof JsonObject;
   }

   public boolean isJsonPrimitive() {
      return this instanceof JsonPrimitive;
   }

   public boolean isJsonNull() {
      return this instanceof JsonNull;
   }

   public JsonObject getAsJsonObject() {
      if (this.isJsonObject()) {
         return (JsonObject)this;
      } else {
         throw new IllegalStateException("Not a JSON Object: " + this);
      }
   }

   public JsonArray getAsJsonArray() {
      if (this.isJsonArray()) {
         return (JsonArray)this;
      } else {
         throw new IllegalStateException("This is not a JSON Array.");
      }
   }

   public JsonPrimitive getAsJsonPrimitive() {
      if (this.isJsonPrimitive()) {
         return (JsonPrimitive)this;
      } else {
         throw new IllegalStateException("This is not a JSON Primitive.");
      }
   }

   public boolean getAsBoolean() {
      throw new UnsupportedOperationException(this.getClass().getSimpleName());
   }

   Boolean getAsBooleanWrapper() {
      throw new UnsupportedOperationException(this.getClass().getSimpleName());
   }

   public Number getAsNumber() {
      throw new UnsupportedOperationException(this.getClass().getSimpleName());
   }

   public String getAsString() {
      throw new UnsupportedOperationException(this.getClass().getSimpleName());
   }

   public double getAsDouble() {
      throw new UnsupportedOperationException(this.getClass().getSimpleName());
   }

   public long getAsLong() {
      throw new UnsupportedOperationException(this.getClass().getSimpleName());
   }

   public int getAsInt() {
      throw new UnsupportedOperationException(this.getClass().getSimpleName());
   }

   public String toString() {
      try {
         StringWriter stringWriter = new StringWriter();
         JsonWriter jsonWriter = new JsonWriter(stringWriter);
         jsonWriter.setLenient(true);
         Streams.write(this, jsonWriter);
         return stringWriter.toString();
      } catch (IOException e) {
         throw new AssertionError(e);
      }
   }
}
