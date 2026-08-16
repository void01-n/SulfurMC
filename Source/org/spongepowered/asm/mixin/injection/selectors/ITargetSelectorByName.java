package org.spongepowered.asm.mixin.injection.selectors;

public interface ITargetSelectorByName extends ITargetSelector {
   String getOwner();

   String getName();

   String getDesc();

   String toDescriptor();

   MatchResult matches(String var1, String var2, String var3);
}
