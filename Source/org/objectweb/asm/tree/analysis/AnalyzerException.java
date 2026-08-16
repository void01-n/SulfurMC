package org.objectweb.asm.tree.analysis;

import org.objectweb.asm.tree.AbstractInsnNode;

public class AnalyzerException extends Exception {
   private static final long serialVersionUID = 3154190448018943333L;
   public final transient AbstractInsnNode node;

   public AnalyzerException(AbstractInsnNode insn, String message) {
      super(message);
      this.node = insn;
   }

   public AnalyzerException(AbstractInsnNode insn, String message, Throwable cause) {
      super(message, cause);
      this.node = insn;
   }

   public AnalyzerException(AbstractInsnNode insn, String message, Object expected, Value actual) {
      super(stringConcat$1(message == null ? "Expected " : stringConcat$0(message), String.valueOf(expected), String.valueOf(actual)));
      this.node = insn;
   }

   // $FF: synthetic method
   private static String stringConcat$0(String var0) {
      return var0 + ": expected ";
   }

   // $FF: synthetic method
   private static String stringConcat$1(String var0, String var1, String var2) {
      return var0 + var1 + ", but found " + var2;
   }
}
