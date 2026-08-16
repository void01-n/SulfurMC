package org.spongepowered.asm.launch.platform.container;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import org.spongepowered.asm.launch.platform.MainAttributes;
import org.spongepowered.asm.util.Files;

public class ContainerHandleURI implements IContainerHandle {
   private final URI uri;
   private final MainAttributes attributes;

   public ContainerHandleURI(URI uri) {
      this.uri = uri;
      this.attributes = MainAttributes.of(uri);
   }

   public String getId() {
      return null;
   }

   public String getDescription() {
      return this.uri.toString();
   }

   public URI getURI() {
      return this.uri;
   }

   /** @deprecated */
   @Deprecated
   public File getFile() {
      return this.uri != null && "file".equals(this.uri.getScheme()) ? Files.toFile(this.uri) : null;
   }

   public String getAttribute(String name) {
      return this.attributes.get(name);
   }

   public Collection<IContainerHandle> getNestedContainers() {
      return Collections.emptyList();
   }

   public boolean equals(Object other) {
      return !(other instanceof ContainerHandleURI) ? false : this.uri.equals(((ContainerHandleURI)other).uri);
   }

   public int hashCode() {
      return this.uri.hashCode();
   }

   public String toString() {
      return String.format("ContainerHandleURI(%s)", this.uri);
   }
}
