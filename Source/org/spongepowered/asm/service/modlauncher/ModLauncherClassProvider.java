package org.spongepowered.asm.service.modlauncher;

import cpw.mods.modlauncher.Launcher;
import java.lang.reflect.Method;
import java.net.URL;
import org.spongepowered.asm.service.IClassProvider;

class ModLauncherClassProvider implements IClassProvider {
   /** @deprecated */
   @Deprecated
   public URL[] getClassPath() {
      try {
         Class<?> clJava9ClassLoaderUtil = this.findClass("cpw.mods.gross.Java9ClassLoaderUtil");
         Method mdGetSystemClassPathURLs = clJava9ClassLoaderUtil.getDeclaredMethod("getSystemClassPathURLs");
         return (URL[])mdGetSystemClassPathURLs.invoke((Object)null);
      } catch (ReflectiveOperationException var3) {
         return new URL[0];
      }
   }

   public Class<?> findClass(String name) throws ClassNotFoundException {
      return Class.forName(name, true, Thread.currentThread().getContextClassLoader());
   }

   public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
      return Class.forName(name, initialize, Thread.currentThread().getContextClassLoader());
   }

   public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
      return Class.forName(name, initialize, Launcher.class.getClassLoader());
   }
}
