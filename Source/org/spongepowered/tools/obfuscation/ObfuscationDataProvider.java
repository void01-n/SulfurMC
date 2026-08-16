package org.spongepowered.tools.obfuscation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorRemappable;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.obfuscation.mapping.IMapping;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.interfaces.IObfuscationDataProvider;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;

public class ObfuscationDataProvider implements IObfuscationDataProvider {
   private final IMixinAnnotationProcessor ap;
   private final List<ObfuscationEnvironment> environments;

   public ObfuscationDataProvider(IMixinAnnotationProcessor ap, List<ObfuscationEnvironment> environments) {
      this.ap = ap;
      this.environments = environments;
   }

   public <T> ObfuscationData<T> getObfEntryRecursive(ITargetSelectorRemappable targetMember) {
      ObfuscationData<String> obfTargetNames = this.getObfClass(targetMember.getOwner());
      ObfuscationData<T> obfData = this.getObfEntry(targetMember);

      try {
         if (obfData.isEmpty()) {
            obfData = this.<T>getObfEntryRecursive(targetMember, new HashSet());
         }

         return !obfData.isEmpty() ? applyParents(obfTargetNames, obfData) : obfData;
      } catch (Exception ex) {
         ex.printStackTrace();
         return this.getObfEntry(targetMember);
      }
   }

   private <T> ObfuscationData<T> getObfEntryRecursive(ITargetSelectorRemappable targetMember, Set<String> visited) {
      TypeHandle targetType = this.ap.getTypeProvider().getTypeHandle(targetMember.getOwner());
      if (targetType != null && visited.add(targetType.toString())) {
         TypeHandle superClass = targetType.getSuperclass();

         for(TypeHandle iface : targetType.getInterfaces()) {
            ObfuscationData<T> obfData = this.<T>getObfEntryUsing(targetMember, iface);
            if (!obfData.isEmpty()) {
               return obfData;
            }

            obfData = this.<T>getObfEntryRecursive(targetMember.move(iface.getName()), visited);
            if (!obfData.isEmpty()) {
               return obfData;
            }
         }

         if (superClass != null) {
            ObfuscationData<T> obfData = this.<T>getObfEntryUsing(targetMember, superClass);
            if (!obfData.isEmpty()) {
               return obfData;
            } else {
               return this.<T>getObfEntryRecursive(targetMember.move(superClass.getName()), visited);
            }
         } else {
            return new ObfuscationData<T>();
         }
      } else {
         return new ObfuscationData<T>();
      }
   }

   private <T> ObfuscationData<T> getObfEntryUsing(ITargetSelectorRemappable targetMember, TypeHandle targetClass) {
      return targetClass == null ? new ObfuscationData() : this.getObfEntry(targetMember.move(targetClass.getName()));
   }

   public <T> ObfuscationData<T> getObfEntry(ITargetSelectorRemappable targetMember) {
      return targetMember.isField() ? this.getObfField(targetMember) : this.getObfMethod(targetMember.asMethodMapping());
   }

   public <T> ObfuscationData<T> getObfEntry(IMapping<T> mapping) {
      if (mapping != null) {
         if (mapping.getType() == IMapping.Type.FIELD) {
            return this.getObfField((MappingField)mapping);
         }

         if (mapping.getType() == IMapping.Type.METHOD) {
            return this.getObfMethod((MappingMethod)mapping);
         }
      }

      return new ObfuscationData<T>();
   }

   public ObfuscationData<MappingMethod> getObfMethodRecursive(ITargetSelectorRemappable targetMember) {
      return this.<MappingMethod>getObfEntryRecursive(targetMember);
   }

   public ObfuscationData<MappingMethod> getObfMethod(ITargetSelectorRemappable method) {
      return this.getRemappedMethod(method, method.isConstructor());
   }

   public ObfuscationData<MappingMethod> getRemappedMethod(ITargetSelectorRemappable method) {
      return this.getRemappedMethod(method, true);
   }

   private ObfuscationData<MappingMethod> getRemappedMethod(ITargetSelectorRemappable method, boolean remapDescriptor) {
      ObfuscationData<MappingMethod> data = new ObfuscationData<MappingMethod>();

      for(ObfuscationEnvironment env : this.environments) {
         MappingMethod obfMethod = env.getObfMethod(method);
         if (obfMethod != null) {
            data.put(env.getType(), obfMethod);
         }
      }

      if (data.isEmpty() && remapDescriptor) {
         return this.remapDescriptor(data, method);
      } else {
         return data;
      }
   }

   public ObfuscationData<MappingMethod> getObfMethod(MappingMethod method) {
      return this.getRemappedMethod(method, method.isConstructor());
   }

   public ObfuscationData<MappingMethod> getRemappedMethod(MappingMethod method) {
      return this.getRemappedMethod(method, true);
   }

   private ObfuscationData<MappingMethod> getRemappedMethod(MappingMethod method, boolean remapDescriptor) {
      ObfuscationData<MappingMethod> data = new ObfuscationData<MappingMethod>();

      for(ObfuscationEnvironment env : this.environments) {
         MappingMethod obfMethod = env.getObfMethod(method);
         if (obfMethod != null) {
            data.put(env.getType(), obfMethod);
         }
      }

      if (data.isEmpty() && remapDescriptor) {
         return this.remapDescriptor(data, new MemberInfo(method));
      } else {
         return data;
      }
   }

   public ObfuscationData<MappingMethod> remapDescriptor(ObfuscationData<MappingMethod> data, ITargetSelectorRemappable method) {
      for(ObfuscationEnvironment env : this.environments) {
         ITargetSelectorRemappable obfMethod = env.remapDescriptor(method);
         if (obfMethod != null) {
            data.put(env.getType(), obfMethod.asMethodMapping());
         }
      }

      return data;
   }

   public ObfuscationData<MappingField> getObfFieldRecursive(ITargetSelectorRemappable targetMember) {
      return this.<MappingField>getObfEntryRecursive(targetMember);
   }

   public ObfuscationData<MappingField> getObfField(ITargetSelectorRemappable field) {
      return this.getObfField(field.asFieldMapping());
   }

   public ObfuscationData<MappingField> getObfField(MappingField field) {
      ObfuscationData<MappingField> data = new ObfuscationData<MappingField>();

      for(ObfuscationEnvironment env : this.environments) {
         MappingField obfField = env.getObfField(field);
         if (obfField != null) {
            if (obfField.getDesc() == null && field.getDesc() != null) {
               obfField = obfField.transform(env.remapDescriptor(field.getDesc()));
            }

            data.put(env.getType(), obfField);
         }
      }

      return data;
   }

   public ObfuscationData<String> getObfClass(TypeHandle type) {
      return this.getObfClass(type.getName());
   }

   public ObfuscationData<String> getObfClass(String className) {
      ObfuscationData<String> data = new ObfuscationData<String>(className);

      for(ObfuscationEnvironment env : this.environments) {
         String obfClass = env.getObfClass(className);
         if (obfClass != null) {
            data.put(env.getType(), obfClass);
         }
      }

      return data;
   }

   private static <T> ObfuscationData<T> applyParents(ObfuscationData<String> parents, ObfuscationData<T> members) {
      for(ObfuscationType type : members) {
         String obfClass = parents.get(type);
         T obfMember = members.get(type);
         members.put(type, MemberInfo.fromMapping((IMapping)obfMember).move(obfClass).asMapping());
      }

      return members;
   }
}
