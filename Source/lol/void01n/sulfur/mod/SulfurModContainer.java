package lol.void01n.sulfur.mod;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SulfurModContainer {
   public final String id;
   public final String version;
   public final String ecosystem;
   public final List<Path> jars;
   public final String displayName;
   public final Map<String, String> dependencies;

   public SulfurModContainer(String id, String version, String ecosystem, List<Path> jars, String displayName, Map<String, String> dependencies) {
      this.id = (String)Objects.requireNonNull(id, "id");
      this.version = (String)Objects.requireNonNull(version, "version");
      this.ecosystem = (String)Objects.requireNonNull(ecosystem, "ecosystem");
      this.jars = List.copyOf((Collection)Objects.requireNonNull(jars, "jars"));
      this.displayName = displayName != null && !displayName.isBlank() ? displayName : id;
      this.dependencies = Map.copyOf((Map)Objects.requireNonNull(dependencies, "dependencies"));
   }

   public SulfurModContainer(String id, String version, String ecosystem, List<Path> jars, String displayName) {
      this(id, version, ecosystem, jars, displayName, Map.of());
   }

   public String toString() {
      String var10000 = this.id;
      return "SulfurModContainer{id=" + var10000 + ", version=" + this.version + ", ecosystem=" + this.ecosystem + ", jars=" + this.jars.size() + "}";
   }

   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else if (!(obj instanceof SulfurModContainer)) {
         return false;
      } else {
         SulfurModContainer other = (SulfurModContainer)obj;
         return this.id.equals(other.id);
      }
   }

   public int hashCode() {
      return Objects.hash(new Object[]{this.id});
   }
}
