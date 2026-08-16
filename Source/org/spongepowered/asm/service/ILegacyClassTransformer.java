package org.spongepowered.asm.service;

public interface ILegacyClassTransformer extends ITransformer {
   byte[] transformClassBytes(String var1, String var2, byte[] var3);
}
