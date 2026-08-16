package org.spongepowered.asm.mixin.transformer;

import java.util.Map;
import java.util.function.Supplier;
import org.objectweb.asm.tree.FieldNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.transformer.struct.Clinit;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidInterfaceMixinException;
import org.spongepowered.asm.util.Annotations;

class MixinApplicatorInterface extends MixinApplicatorStandard {
   MixinApplicatorInterface(TargetClassContext context) {
      super(context);
   }

   protected void applyInterfaces(MixinTargetContext mixin) {
      for(String interfaceName : mixin.getInterfaces()) {
         if (!this.targetClass.name.equals(interfaceName) && !this.targetClass.interfaces.contains(interfaceName)) {
            this.targetClass.interfaces.add(interfaceName);
            mixin.getTargetClassInfo().addInterface(interfaceName);
         }
      }

   }

   protected void mergeShadowFields(MixinTargetContext mixin) {
      for(Map.Entry<FieldNode, ClassInfo.Field> entry : mixin.getShadowFields()) {
         FieldNode shadow = (FieldNode)entry.getKey();
         FieldNode target = this.findTargetField(shadow);
         if (target != null) {
            Annotations.merge(shadow, target);
            if (((ClassInfo.Field)entry.getValue()).isDecoratedMutable()) {
               this.logger.error("Ignoring illegal @Mutable on {}:{} in {}", shadow.name, shadow.desc, mixin);
            }

            if (shadow.value != null) {
               this.logger.warn("@Shadow field {}:{} in {} has an inlinable value set, is this intended?", shadow.name, shadow.desc, mixin);
            }
         } else {
            this.logger.warn("Unable to find target for @Shadow {}:{} in {}", shadow.name, shadow.desc, mixin);
         }
      }

   }

   protected void mergeNewFields(MixinTargetContext mixin) {
   }

   protected void applyClinitLegacy(MixinTargetContext mixin) {
   }

   protected void applyClinit(MixinTargetContext mixin, Supplier<Clinit> clinit) {
   }

   protected void applyInitialisers(MixinTargetContext mixin) {
   }

   protected void prepareInjections(MixinTargetContext mixin) {
      if (MixinEnvironment.Feature.INJECTORS_IN_INTERFACE_MIXINS.isEnabled()) {
         try {
            super.prepareInjections(mixin);
         } catch (InvalidInjectionException var4) {
            String description = var4.getContext() != null ? var4.getContext().toString() : "Injection";
            throw new InvalidInterfaceMixinException(mixin, description + " is not supported in interface mixin", var4);
         }
      } else {
         InjectionInfo injectInfo = mixin.getFirstInjectionInfo();
         if (injectInfo != null) {
            throw new InvalidInterfaceMixinException(mixin, injectInfo + " is not supported on interface mixin method " + injectInfo.getMethodName());
         }
      }
   }

   protected void applyPreInjections(MixinTargetContext mixin) {
      if (MixinEnvironment.Feature.INJECTORS_IN_INTERFACE_MIXINS.isEnabled()) {
         super.applyPreInjections(mixin);
      }
   }

   protected void applyInjections(MixinTargetContext mixin, int injectorOrder) {
      if (MixinEnvironment.Feature.INJECTORS_IN_INTERFACE_MIXINS.isEnabled()) {
         super.applyInjections(mixin, injectorOrder);
      }
   }
}
