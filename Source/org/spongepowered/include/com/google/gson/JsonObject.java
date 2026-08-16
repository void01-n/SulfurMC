package org.spongepowered.include.com.google.gson;

import java.util.Map;
import java.util.Set;
import org.spongepowered.include.com.google.gson.internal.LinkedTreeMap;

public final class JsonObject extends JsonElement {
   private final LinkedTreeMap<String, JsonElement> members = new LinkedTreeMap<String, JsonElement>();

   public void add(String property, JsonElement value) {
      if (value == null) {
         value = JsonNull.INSTANCE;
      }

      this.members.put(property, value);
   }

   public Set<Map.Entry<String, JsonElement>> entrySet() {
      return this.members.entrySet();
   }

   public boolean equals(Object o) {
      return o == this || o instanceof JsonObject && ((JsonObject)o).members.equals(this.members);
   }

   public int hashCode() {
      return this.members.hashCode();
   }
}
