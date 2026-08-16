package org.spongepowered.asm.mixin.injection.selectors;

public interface ITargetSelectorConstructor extends ITargetSelectorByName {
   String toCtorType();

   String toCtorDesc();
}
