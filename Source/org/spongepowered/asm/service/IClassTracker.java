package org.spongepowered.asm.service;

public interface IClassTracker {
   void registerInvalidClass(String var1);

   boolean isClassLoaded(String var1);

   String getClassRestrictions(String var1);
}
