package org.spongepowered.asm.mixin.injection.selectors.throwables;

import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;

public class SelectorConstraintException extends SelectorException {
   public SelectorConstraintException(ITargetSelector selector, String message) {
      super(selector, message);
   }

   public SelectorConstraintException(ITargetSelector selector, Throwable cause) {
      super(selector, cause);
   }

   public SelectorConstraintException(ITargetSelector selector, String message, Throwable cause) {
      super(selector, message, cause);
   }
}
