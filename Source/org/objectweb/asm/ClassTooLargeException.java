package org.objectweb.asm;

public final class ClassTooLargeException extends IndexOutOfBoundsException {
   private static final long serialVersionUID = 160715609518896765L;
   private final String className;
   private final int constantPoolCount;

   public ClassTooLargeException(String className, int constantPoolCount) {
      super(stringConcat$0(className));
      this.className = className;
      this.constantPoolCount = constantPoolCount;
   }

   // $FF: synthetic method
   private static String stringConcat$0(String var0) {
      return "Class too large: " + var0;
   }

   public String getClassName() {
      return this.className;
   }

   public int getConstantPoolCount() {
      return this.constantPoolCount;
   }
}
