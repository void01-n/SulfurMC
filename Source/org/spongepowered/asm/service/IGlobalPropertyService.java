package org.spongepowered.asm.service;

public interface IGlobalPropertyService {
   IPropertyKey resolveKey(String var1);

   <T> T getProperty(IPropertyKey var1);

   void setProperty(IPropertyKey var1, Object var2);

   <T> T getProperty(IPropertyKey var1, T var2);

   String getPropertyString(IPropertyKey var1, String var2);
}
