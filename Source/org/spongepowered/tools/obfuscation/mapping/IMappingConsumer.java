package org.spongepowered.tools.obfuscation.mapping;

import java.util.LinkedHashSet;
import org.spongepowered.asm.obfuscation.mapping.IMapping;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.include.com.google.common.base.Objects;
import org.spongepowered.tools.obfuscation.ObfuscationType;

public interface IMappingConsumer {
   void clear();

   void addFieldMapping(ObfuscationType var1, MappingField var2, MappingField var3);

   void addMethodMapping(ObfuscationType var1, MappingMethod var2, MappingMethod var3);

   MappingSet<MappingField> getFieldMappings(ObfuscationType var1);

   MappingSet<MappingMethod> getMethodMappings(ObfuscationType var1);

   public static class MappingSet<TMapping extends IMapping<TMapping>> extends LinkedHashSet<Pair<TMapping>> {
      public static class Pair<TMapping extends IMapping<TMapping>> {
         public final TMapping from;
         public final TMapping to;

         public Pair(TMapping from, TMapping to) {
            this.from = from;
            this.to = to;
         }

         public boolean equals(Object obj) {
            if (!(obj instanceof Pair)) {
               return false;
            } else {
               Pair<TMapping> other = (Pair)obj;
               return Objects.equal(this.from, other.from) && Objects.equal(this.to, other.to);
            }
         }

         public int hashCode() {
            return Objects.hashCode(this.from, this.to);
         }

         public String toString() {
            return String.format("%s -> %s", this.from, this.to);
         }
      }
   }
}
