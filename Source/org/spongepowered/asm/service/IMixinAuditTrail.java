package org.spongepowered.asm.service;

public interface IMixinAuditTrail {
   void onApply(String var1, String var2);

   void onPostProcess(String var1);

   void onGenerate(String var1, String var2);
}
