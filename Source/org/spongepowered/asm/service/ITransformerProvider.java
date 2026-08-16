package org.spongepowered.asm.service;

import java.util.Collection;

public interface ITransformerProvider {
   Collection<ITransformer> getTransformers();

   Collection<ITransformer> getDelegatedTransformers();

   void addTransformerExclusion(String var1);
}
