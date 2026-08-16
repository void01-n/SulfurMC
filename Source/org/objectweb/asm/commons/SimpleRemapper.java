package org.objectweb.asm.commons;

import java.util.Collections;
import java.util.Map;

public class SimpleRemapper extends Remapper {
   private final Map<String, String> mapping;

   public SimpleRemapper(Map<String, String> mapping) {
      this.mapping = mapping;
   }

   public SimpleRemapper(String oldName, String newName) {
      this.mapping = Collections.singletonMap(oldName, newName);
   }

   public String mapMethodName(String owner, String name, String descriptor) {
      String remappedName = this.map(stringConcat$0(owner, name, descriptor));
      return remappedName == null ? name : remappedName;
   }

   // $FF: synthetic method
   private static String stringConcat$0(String var0, String var1, String var2) {
      return var0 + "." + var1 + var2;
   }

   public String mapInvokeDynamicMethodName(String name, String descriptor) {
      String remappedName = this.map(stringConcat$1(name, descriptor));
      return remappedName == null ? name : remappedName;
   }

   // $FF: synthetic method
   private static String stringConcat$1(String var0, String var1) {
      return "." + var0 + var1;
   }

   public String mapAnnotationAttributeName(String descriptor, String name) {
      String remappedName = this.map(stringConcat$2(descriptor, name));
      return remappedName == null ? name : remappedName;
   }

   // $FF: synthetic method
   private static String stringConcat$2(String var0, String var1) {
      return var0 + "." + var1;
   }

   public String mapFieldName(String owner, String name, String descriptor) {
      String remappedName = this.map(stringConcat$3(owner, name));
      return remappedName == null ? name : remappedName;
   }

   // $FF: synthetic method
   private static String stringConcat$3(String var0, String var1) {
      return var0 + "." + var1;
   }

   public String map(String key) {
      return (String)this.mapping.get(key);
   }
}
