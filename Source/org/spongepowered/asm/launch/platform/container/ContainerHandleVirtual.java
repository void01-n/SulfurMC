package org.spongepowered.asm.launch.platform.container;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ContainerHandleVirtual implements IContainerHandle {
   private final String name;
   private final Map<String, String> attributes = new HashMap();
   private final Set<IContainerHandle> nestedContainers = new LinkedHashSet();

   public ContainerHandleVirtual(String name) {
      this.name = name;
   }

   public String getId() {
      return this.name;
   }

   public String getDescription() {
      return this.toString();
   }

   public String getName() {
      return this.name;
   }

   public ContainerHandleVirtual setAttribute(String key, String value) {
      this.attributes.put(key, value);
      return this;
   }

   public ContainerHandleVirtual add(IContainerHandle nested) {
      this.nestedContainers.add(nested);
      return this;
   }

   public String getAttribute(String name) {
      return (String)this.attributes.get(name);
   }

   public Collection<IContainerHandle> getNestedContainers() {
      return Collections.unmodifiableSet(this.nestedContainers);
   }

   public boolean equals(Object obj) {
      return obj instanceof String && obj.toString().equals(this.name);
   }

   public int hashCode() {
      return this.name.hashCode();
   }

   public String toString() {
      return String.format("ContainerHandleVirtual(%s:%x)", this.name, this.hashCode());
   }
}
