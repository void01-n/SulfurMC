package org.spongepowered.asm.launch.platform.container;

import cpw.mods.jarhandling.SecureJar;

public class ContainerHandleModLauncherEx extends ContainerHandleModLauncher {
   public ContainerHandleModLauncherEx(String name) {
      super(name);
   }

   public void addResource(Object resource) {
      if (resource instanceof SecureJar) {
         this.add(new SecureJarResource((SecureJar)resource));
      } else {
         super.addResource(resource);
      }

   }

   static class SecureJarResource extends ContainerHandleURI {
      private SecureJar jar;

      public SecureJarResource(SecureJar resource) {
         super(resource.getPrimaryPath().toUri());
         this.jar = resource;
      }

      public String getId() {
         String name = this.jar.name();
         int lastDotPos = name.lastIndexOf(46);
         if (lastDotPos > 0) {
            name = name.substring(0, lastDotPos);
         }

         return name;
      }

      public String getDescription() {
         return this.jar.getRootPath().toAbsolutePath().toString();
      }

      public String getName() {
         return this.jar.name();
      }

      public String toString() {
         return String.format("SecureJarResource(%s)", this.getName());
      }
   }
}
