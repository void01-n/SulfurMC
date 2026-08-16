package org.spongepowered.include.com.google.gson;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class JsonArray extends JsonElement implements Iterable<JsonElement> {
   private final List<JsonElement> elements = new ArrayList();

   public void add(JsonElement element) {
      if (element == null) {
         element = JsonNull.INSTANCE;
      }

      this.elements.add(element);
   }

   public Iterator<JsonElement> iterator() {
      return this.elements.iterator();
   }

   public Number getAsNumber() {
      if (this.elements.size() == 1) {
         return ((JsonElement)this.elements.get(0)).getAsNumber();
      } else {
         throw new IllegalStateException();
      }
   }

   public String getAsString() {
      if (this.elements.size() == 1) {
         return ((JsonElement)this.elements.get(0)).getAsString();
      } else {
         throw new IllegalStateException();
      }
   }

   public double getAsDouble() {
      if (this.elements.size() == 1) {
         return ((JsonElement)this.elements.get(0)).getAsDouble();
      } else {
         throw new IllegalStateException();
      }
   }

   public long getAsLong() {
      if (this.elements.size() == 1) {
         return ((JsonElement)this.elements.get(0)).getAsLong();
      } else {
         throw new IllegalStateException();
      }
   }

   public int getAsInt() {
      if (this.elements.size() == 1) {
         return ((JsonElement)this.elements.get(0)).getAsInt();
      } else {
         throw new IllegalStateException();
      }
   }

   public boolean getAsBoolean() {
      if (this.elements.size() == 1) {
         return ((JsonElement)this.elements.get(0)).getAsBoolean();
      } else {
         throw new IllegalStateException();
      }
   }

   public boolean equals(Object o) {
      return o == this || o instanceof JsonArray && ((JsonArray)o).elements.equals(this.elements);
   }

   public int hashCode() {
      return this.elements.hashCode();
   }
}
