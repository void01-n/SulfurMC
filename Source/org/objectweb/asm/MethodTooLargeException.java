package org.objectweb.asm;

public final class MethodTooLargeException extends IndexOutOfBoundsException {
   private static final long serialVersionUID = 6807380416709738314L;
   private final String className;
   private final String methodName;
   private final String descriptor;
   private final int codeSize;

   public MethodTooLargeException(String className, String methodName, String descriptor, int codeSize) {
      super(stringConcat$0(className, methodName, descriptor));
      this.className = className;
      this.methodName = methodName;
      this.descriptor = descriptor;
      this.codeSize = codeSize;
   }

   // $FF: synthetic method
   private static String stringConcat$0(String var0, String var1, String var2) {
      return "Method too large: " + var0 + "." + var1 + " " + var2;
   }

   public String getClassName() {
      return this.className;
   }

   public String getMethodName() {
      return this.methodName;
   }

   public String getDescriptor() {
      return this.descriptor;
   }

   public int getCodeSize() {
      return this.codeSize;
   }
}
