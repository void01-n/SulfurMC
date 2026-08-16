package org.spongepowered.asm.mixin.refmap;

import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.Type;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IRemapper;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Quantifier;

public final class RemappingReferenceMapper implements IClassReferenceMapper, IReferenceMapper {
   private static final ILogger logger = MixinService.getService().getLogger("mixin");
   private final IReferenceMapper refMap;
   private final IRemapper remapper;
   private final Map<String, String> mappedReferenceCache = new HashMap();

   private RemappingReferenceMapper(MixinEnvironment env, IReferenceMapper refMap) {
      this.refMap = refMap;
      this.remapper = env.getRemappers();
      logger.debug("Remapping refMap {} using remapper chain", refMap.getResourceName());
   }

   public boolean isDefault() {
      return this.refMap.isDefault();
   }

   public String getResourceName() {
      return this.refMap.getResourceName();
   }

   public String getStatus() {
      return this.refMap.getStatus();
   }

   public String getContext() {
      return this.refMap.getContext();
   }

   public void setContext(String context) {
      this.refMap.setContext(context);
   }

   public String remap(String className, String reference) {
      return this.remapWithContext(this.getContext(), className, reference);
   }

   private static String remapMethodDescriptor(IRemapper remapper, String desc) {
      StringBuilder newDesc = new StringBuilder();
      newDesc.append('(');

      for(Type arg : Type.getArgumentTypes(desc)) {
         newDesc.append(remapper.mapDesc(arg.getDescriptor()));
      }

      return newDesc.append(')').append(remapper.mapDesc(Type.getReturnType(desc).getDescriptor())).toString();
   }

   public String remapWithContext(String context, String className, String reference) {
      if (reference.isEmpty()) {
         return reference;
      } else {
         String origInfoString = this.refMap.remapWithContext(context, className, reference);
         String remappedCached = (String)this.mappedReferenceCache.get(origInfoString);
         if (remappedCached != null) {
            return remappedCached;
         } else {
            MemberInfo info = MemberInfo.parse(origInfoString, (ISelectorContext)null);
            if (info.getName() == null && info.getDesc() == null) {
               return info.getOwner() != null ? (new MemberInfo(this.remapper.map(info.getOwner()), Quantifier.DEFAULT)).toString() : info.toString();
            } else {
               String remapped;
               if (info.isField()) {
                  remapped = (new MemberInfo(this.remapper.mapFieldName(info.getOwner(), info.getName(), info.getDesc()), info.getOwner() == null ? null : this.remapper.map(info.getOwner()), info.getDesc() == null ? null : this.remapper.mapDesc(info.getDesc()))).toString();
               } else {
                  remapped = (new MemberInfo(this.remapper.mapMethodName(info.getOwner(), info.getName(), info.getDesc()), info.getOwner() == null ? null : this.remapper.map(info.getOwner()), info.getDesc() == null ? null : remapMethodDescriptor(this.remapper, info.getDesc()))).toString();
               }

               this.mappedReferenceCache.put(origInfoString, remapped);
               return remapped;
            }
         }
      }
   }

   public static IReferenceMapper of(MixinEnvironment env, IReferenceMapper refMap) {
      return (IReferenceMapper)(!refMap.isDefault() ? new RemappingReferenceMapper(env, refMap) : refMap);
   }

   public String remapClassName(String className, String inputClassName) {
      return this.remapClassNameWithContext(this.getContext(), className, inputClassName);
   }

   public String remapClassNameWithContext(String context, String className, String remapped) {
      String origInfoString;
      if (this.refMap instanceof IClassReferenceMapper) {
         origInfoString = ((IClassReferenceMapper)this.refMap).remapClassNameWithContext(context, className, remapped);
      } else {
         origInfoString = this.refMap.remapWithContext(context, className, remapped);
      }

      return this.remapper.map(origInfoString.replace('.', '/'));
   }
}
