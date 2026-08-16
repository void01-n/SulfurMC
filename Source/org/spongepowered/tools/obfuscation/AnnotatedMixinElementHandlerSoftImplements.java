package org.spongepowered.tools.obfuscation;

import java.util.List;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.asm.util.asm.IAnnotationHandle;
import org.spongepowered.tools.obfuscation.interfaces.IMessagerEx;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.mirror.AnnotationHandle;
import org.spongepowered.tools.obfuscation.mirror.MethodHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;

class AnnotatedMixinElementHandlerSoftImplements extends AnnotatedMixinElementHandler {
   AnnotatedMixinElementHandlerSoftImplements(IMixinAnnotationProcessor ap, AnnotatedMixin mixin) {
      super(ap, mixin);
   }

   public void process(AnnotationHandle implementsAnnotation) {
      if (this.mixin.remap()) {
         List<IAnnotationHandle> interfaces = implementsAnnotation.getAnnotationList("value");
         if (interfaces.size() < 1) {
            this.ap.printMessage(IMessagerEx.MessageType.SOFT_IMPLEMENTS_EMPTY, "Empty @Implements annotation", this.mixin.getMixinElement(), implementsAnnotation.asMirror());
         } else {
            for(IAnnotationHandle interfaceAnnotation : interfaces) {
               Interface.Remap remap = (Interface.Remap)interfaceAnnotation.getValue("remap", Interface.Remap.ALL);
               if (remap != Interface.Remap.NONE) {
                  try {
                     TypeHandle iface = this.ap.getTypeProvider().getTypeHandle(interfaceAnnotation.getValue("iface"));
                     String prefix = (String)interfaceAnnotation.getValue("prefix");
                     this.processSoftImplements(remap, iface, prefix);
                  } catch (Exception ex) {
                     this.ap.printMessage(IMessagerEx.MessageType.ERROR, "Unexpected error: " + ex.getClass().getName() + ": " + ex.getMessage(), this.mixin.getMixinElement(), ((AnnotationHandle)interfaceAnnotation).asMirror());
                  }
               }
            }

         }
      }
   }

   private void processSoftImplements(Interface.Remap remap, TypeHandle iface, String prefix) {
      for(MethodHandle method : iface.getMethods()) {
         this.processMethod(remap, iface, prefix, method);
      }

      for(TypeHandle superInterface : iface.getInterfaces()) {
         this.processSoftImplements(remap, superInterface, prefix);
      }

   }

   private void processMethod(Interface.Remap remap, TypeHandle iface, String prefix, MethodHandle method) {
      String name = method.getName();
      String sig = method.getJavaSignature();
      String desc = method.getDesc();
      if (remap != Interface.Remap.ONLY_PREFIXED) {
         MethodHandle mixinMethod = this.mixin.getHandle().findMethod(name, sig);
         if (mixinMethod != null) {
            this.addInterfaceMethodMapping(remap, iface, (String)null, mixinMethod, name, desc);
         }
      }

      if (prefix != null) {
         MethodHandle prefixedMixinMethod = this.mixin.getHandle().findMethod(prefix + name, sig);
         if (prefixedMixinMethod != null) {
            this.addInterfaceMethodMapping(remap, iface, prefix, prefixedMixinMethod, name, desc);
         }
      }

   }

   private void addInterfaceMethodMapping(Interface.Remap remap, TypeHandle iface, String prefix, MethodHandle method, String name, String desc) {
      MappingMethod mapping = new MappingMethod(iface.getName(), name, desc);
      ObfuscationData<MappingMethod> obfData = this.obf.getDataProvider().getObfMethod(mapping);
      if (obfData.isEmpty()) {
         if (remap.forceRemap()) {
            this.ap.printMessage(IMessagerEx.MessageType.NO_OBFDATA_FOR_SOFT_IMPLEMENTS, "No obfuscation mapping for soft-implementing method", method.getElement());
         }

      } else {
         this.addMethodMappings(method.getName(), desc, this.applyPrefix(obfData, prefix));
      }
   }

   private ObfuscationData<MappingMethod> applyPrefix(ObfuscationData<MappingMethod> data, String prefix) {
      if (prefix == null) {
         return data;
      } else {
         ObfuscationData<MappingMethod> prefixed = new ObfuscationData<MappingMethod>();

         for(ObfuscationType type : data) {
            MappingMethod mapping = data.get(type);
            prefixed.put(type, mapping.addPrefix(prefix));
         }

         return prefixed;
      }
   }
}
