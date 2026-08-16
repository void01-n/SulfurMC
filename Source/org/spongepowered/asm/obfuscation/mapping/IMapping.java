package org.spongepowered.asm.obfuscation.mapping;

public interface IMapping<TMapping> {
   Type getType();

   TMapping move(String var1);

   TMapping remap(String var1);

   TMapping transform(String var1);

   TMapping copy();

   String getName();

   String getSimpleName();

   String getOwner();

   String getDesc();

   TMapping getSuper();

   String serialise();

   public static enum Type {
      FIELD,
      METHOD,
      CLASS,
      PACKAGE;

      // $FF: synthetic method
      private static Type[] $values() {
         return new Type[]{FIELD, METHOD, CLASS, PACKAGE};
      }
   }
}
