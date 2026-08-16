package org.spongepowered.asm.mixin.extensibility;

public interface IActivityContext {
   String toString(String var1);

   IActivity begin(String var1, Object... var2);

   IActivity begin(String var1);

   void clear();

   public interface IActivity {
      void next(String var1, Object... var2);

      void next(String var1);

      void end();

      void append(String var1, Object... var2);

      void append(String var1);
   }
}
