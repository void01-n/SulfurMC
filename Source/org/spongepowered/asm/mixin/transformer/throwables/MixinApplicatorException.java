package org.spongepowered.asm.mixin.transformer.throwables;

import org.spongepowered.asm.mixin.extensibility.IActivityContext;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.transformer.ActivityStack;

public class MixinApplicatorException extends InvalidMixinException {
   public MixinApplicatorException(IMixinInfo context, String message) {
      super((IMixinInfo)context, (String)message, (IActivityContext)((ActivityStack)null));
   }

   public MixinApplicatorException(IMixinInfo context, String message, IActivityContext activityContext) {
      super(context, message, activityContext);
   }

   public MixinApplicatorException(IMixinContext context, String message) {
      super((IMixinContext)context, (String)message, (IActivityContext)((ActivityStack)null));
   }

   public MixinApplicatorException(IMixinContext context, String message, IActivityContext activityContext) {
      super(context, message, activityContext);
   }

   public MixinApplicatorException(IMixinInfo mixin, String message, Throwable cause) {
      super((IMixinInfo)mixin, message, cause, (ActivityStack)null);
   }

   public MixinApplicatorException(IMixinInfo mixin, String message, Throwable cause, IActivityContext activityContext) {
      super(mixin, message, cause, activityContext);
   }

   public MixinApplicatorException(IMixinContext mixin, String message, Throwable cause) {
      super((IMixinContext)mixin, message, cause, (ActivityStack)null);
   }

   public MixinApplicatorException(IMixinContext mixin, String message, Throwable cause, IActivityContext activityContext) {
      super(mixin, message, cause, activityContext);
   }

   public MixinApplicatorException(IMixinInfo mixin, Throwable cause) {
      super((IMixinInfo)mixin, (Throwable)cause, (IActivityContext)((ActivityStack)null));
   }

   public MixinApplicatorException(IMixinInfo mixin, Throwable cause, IActivityContext activityContext) {
      super(mixin, cause, activityContext);
   }

   public MixinApplicatorException(IMixinContext mixin, Throwable cause) {
      super((IMixinContext)mixin, (Throwable)cause, (IActivityContext)((ActivityStack)null));
   }

   public MixinApplicatorException(IMixinContext mixin, Throwable cause, IActivityContext activityContext) {
      super(mixin, cause, activityContext);
   }
}
