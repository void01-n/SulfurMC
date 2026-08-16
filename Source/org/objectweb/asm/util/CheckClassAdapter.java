package org.objectweb.asm.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.TypeReference;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SimpleVerifier;

public class CheckClassAdapter extends ClassVisitor {
   private static final String USAGE = "Verifies the given class.\nUsage: CheckClassAdapter <fully qualified class name or class file name>";
   private static final String ERROR_AT = ": error at index ";
   private boolean checkDataFlow;
   private int version;
   private boolean visitCalled;
   private boolean visitModuleCalled;
   private boolean visitSourceCalled;
   private boolean visitOuterClassCalled;
   private boolean visitNestHostCalled;
   private String nestMemberPackageName;
   private boolean visitEndCalled;
   private Map<Label, Integer> labelInsnIndices;

   public CheckClassAdapter(ClassVisitor classVisitor) {
      this(classVisitor, true);
   }

   public CheckClassAdapter(ClassVisitor classVisitor, boolean checkDataFlow) {
      this(589824, classVisitor, checkDataFlow);
      if (this.getClass() != CheckClassAdapter.class) {
         throw new IllegalStateException();
      }
   }

   protected CheckClassAdapter(int api, ClassVisitor classVisitor, boolean checkDataFlow) {
      super(api, classVisitor);
      this.labelInsnIndices = new HashMap();
      this.checkDataFlow = checkDataFlow;
   }

   public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
      if (this.visitCalled) {
         throw new IllegalStateException("visit must be called only once");
      } else {
         this.visitCalled = true;
         this.checkState();
         checkAccess(access, 259633);
         if (name == null) {
            throw new IllegalArgumentException("Illegal class name (null)");
         } else {
            if (!name.endsWith("package-info") && !name.endsWith("module-info")) {
               CheckMethodAdapter.checkInternalName(version, name, "class name");
            }

            if ("java/lang/Object".equals(name)) {
               if (superName != null) {
                  throw new IllegalArgumentException("The super class name of the Object class must be 'null'");
               }
            } else if (name.endsWith("module-info")) {
               if (superName != null) {
                  throw new IllegalArgumentException("The super class name of a module-info class must be 'null'");
               }
            } else {
               CheckMethodAdapter.checkInternalName(version, superName, "super class name");
            }

            if (signature != null) {
               checkClassSignature(signature);
            }

            if ((access & 512) != 0 && !"java/lang/Object".equals(superName)) {
               throw new IllegalArgumentException("The super class name of interfaces must be 'java/lang/Object'");
            } else {
               if (interfaces != null) {
                  for(int i = 0; i < interfaces.length; ++i) {
                     CheckMethodAdapter.checkInternalName(version, interfaces[i], stringConcat$0(i));
                  }
               }

               this.version = version;
               super.visit(version, access, name, signature, superName, interfaces);
            }
         }
      }
   }

   // $FF: synthetic method
   private static String stringConcat$0(int var0) {
      return "interface name at index " + var0;
   }

   public void visitSource(String file, String debug) {
      this.checkState();
      if (this.visitSourceCalled) {
         throw new IllegalStateException("visitSource can be called only once.");
      } else {
         this.visitSourceCalled = true;
         super.visitSource(file, debug);
      }
   }

   public ModuleVisitor visitModule(String name, int access, String version) {
      this.checkState();
      if (this.visitModuleCalled) {
         throw new IllegalStateException("visitModule can be called only once.");
      } else {
         this.visitModuleCalled = true;
         checkFullyQualifiedName(this.version, name, "module name");
         checkAccess(access, 36896);
         CheckModuleAdapter checkModuleAdapter = new CheckModuleAdapter(this.api, super.visitModule(name, access, version), (access & 32) != 0);
         checkModuleAdapter.classVersion = this.version;
         return checkModuleAdapter;
      }
   }

   public void visitNestHost(String nestHost) {
      this.checkState();
      CheckMethodAdapter.checkInternalName(this.version, nestHost, "nestHost");
      if (this.visitNestHostCalled) {
         throw new IllegalStateException("visitNestHost can be called only once.");
      } else if (this.nestMemberPackageName != null) {
         throw new IllegalStateException("visitNestHost and visitNestMember are mutually exclusive.");
      } else {
         this.visitNestHostCalled = true;
         super.visitNestHost(nestHost);
      }
   }

   public void visitNestMember(String nestMember) {
      this.checkState();
      CheckMethodAdapter.checkInternalName(this.version, nestMember, "nestMember");
      if (this.visitNestHostCalled) {
         throw new IllegalStateException("visitMemberOfNest and visitNestHost are mutually exclusive.");
      } else {
         String packageName = packageName(nestMember);
         if (this.nestMemberPackageName == null) {
            this.nestMemberPackageName = packageName;
         } else if (!this.nestMemberPackageName.equals(packageName)) {
            throw new IllegalStateException(stringConcat$1(nestMember, this.nestMemberPackageName));
         }

         super.visitNestMember(nestMember);
      }
   }

   // $FF: synthetic method
   private static String stringConcat$1(String var0, String var1) {
      return "nest member " + var0 + " should be in the package " + var1;
   }

   public void visitPermittedSubclass(String permittedSubclass) {
      this.checkState();
      CheckMethodAdapter.checkInternalName(this.version, permittedSubclass, "permittedSubclass");
      super.visitPermittedSubclass(permittedSubclass);
   }

   public void visitOuterClass(String owner, String name, String descriptor) {
      this.checkState();
      if (this.visitOuterClassCalled) {
         throw new IllegalStateException("visitOuterClass can be called only once.");
      } else {
         this.visitOuterClassCalled = true;
         if (owner == null) {
            throw new IllegalArgumentException("Illegal outer class owner");
         } else {
            if (descriptor != null) {
               CheckMethodAdapter.checkMethodDescriptor(this.version, descriptor);
            }

            super.visitOuterClass(owner, name, descriptor);
         }
      }
   }

   public void visitInnerClass(String name, String outerName, String innerName, int access) {
      this.checkState();
      CheckMethodAdapter.checkInternalName(this.version, name, "class name");
      if (outerName != null) {
         CheckMethodAdapter.checkInternalName(this.version, outerName, "outer class name");
      }

      if (innerName != null) {
         int startIndex;
         for(startIndex = 0; startIndex < innerName.length() && Character.isDigit(innerName.charAt(startIndex)); ++startIndex) {
         }

         if (startIndex == 0 || startIndex < innerName.length()) {
            CheckMethodAdapter.checkIdentifier(this.version, innerName, startIndex, -1, "inner class name");
         }
      }

      checkAccess(access, 30239);
      super.visitInnerClass(name, outerName, innerName, access);
   }

   public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
      this.checkState();
      CheckMethodAdapter.checkUnqualifiedName(this.version, name, "record component name");
      CheckMethodAdapter.checkDescriptor(this.version, descriptor, false);
      if (signature != null) {
         checkFieldSignature(signature);
      }

      return new CheckRecordComponentAdapter(this.api, super.visitRecordComponent(name, descriptor, signature));
   }

   public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
      this.checkState();
      checkAccess(access, 184543);
      CheckMethodAdapter.checkUnqualifiedName(this.version, name, "field name");
      CheckMethodAdapter.checkDescriptor(this.version, descriptor, false);
      if (signature != null) {
         checkFieldSignature(signature);
      }

      if (value != null) {
         CheckMethodAdapter.checkConstant(value);
      }

      return new CheckFieldAdapter(this.api, super.visitField(access, name, descriptor, signature, value));
   }

   public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
      this.checkState();
      checkMethodAccess(this.version, access, 171519);
      if (!"<init>".equals(name) && !"<clinit>".equals(name)) {
         CheckMethodAdapter.checkMethodIdentifier(this.version, name, "method name");
      }

      CheckMethodAdapter.checkMethodDescriptor(this.version, descriptor);
      if (signature != null) {
         checkMethodSignature(signature);
      }

      if (exceptions != null) {
         for(int i = 0; i < exceptions.length; ++i) {
            CheckMethodAdapter.checkInternalName(this.version, exceptions[i], stringConcat$2(i));
         }
      }

      MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
      CheckMethodAdapter checkMethodAdapter;
      if (this.checkDataFlow) {
         if (this.cv instanceof ClassWriter) {
            methodVisitor = new CheckMethodAdapter.MethodWriterWrapper(this.api, this.version, (ClassWriter)this.cv, methodVisitor);
         }

         checkMethodAdapter = new CheckMethodAdapter(this.api, access, name, descriptor, methodVisitor, this.labelInsnIndices);
      } else {
         checkMethodAdapter = new CheckMethodAdapter(this.api, methodVisitor, this.labelInsnIndices);
      }

      checkMethodAdapter.version = this.version;
      return checkMethodAdapter;
   }

   // $FF: synthetic method
   private static String stringConcat$2(int var0) {
      return "exception name at index " + var0;
   }

   public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
      this.checkState();
      CheckMethodAdapter.checkDescriptor(this.version, descriptor, false);
      return new CheckAnnotationAdapter(super.visitAnnotation(descriptor, visible));
   }

   public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
      this.checkState();
      int sort = (new TypeReference(typeRef)).getSort();
      if (sort != 0 && sort != 17 && sort != 16) {
         throw new IllegalArgumentException(stringConcat$3(Integer.toHexString(sort)));
      } else {
         checkTypeRef(typeRef);
         CheckMethodAdapter.checkDescriptor(this.version, descriptor, false);
         return new CheckAnnotationAdapter(super.visitTypeAnnotation(typeRef, typePath, descriptor, visible));
      }
   }

   // $FF: synthetic method
   private static String stringConcat$3(String var0) {
      return "Invalid type reference sort 0x" + var0;
   }

   public void visitAttribute(Attribute attribute) {
      this.checkState();
      if (attribute == null) {
         throw new IllegalArgumentException("Invalid attribute (must not be null)");
      } else {
         super.visitAttribute(attribute);
      }
   }

   public void visitEnd() {
      this.checkState();
      this.visitEndCalled = true;
      super.visitEnd();
   }

   private void checkState() {
      if (!this.visitCalled) {
         throw new IllegalStateException("Cannot visit member before visit has been called.");
      } else if (this.visitEndCalled) {
         throw new IllegalStateException("Cannot visit member after visitEnd has been called.");
      }
   }

   static void checkAccess(int access, int possibleAccess) {
      if ((access & ~possibleAccess) != 0) {
         throw new IllegalArgumentException(stringConcat$4(access));
      } else {
         int publicProtectedPrivate = 7;
         if (Integer.bitCount(access & publicProtectedPrivate) > 1) {
            throw new IllegalArgumentException(stringConcat$5(access));
         } else if (Integer.bitCount(access & 1040) > 1) {
            throw new IllegalArgumentException(stringConcat$6(access));
         }
      }
   }

   // $FF: synthetic method
   private static String stringConcat$4(int var0) {
      return "Invalid access flags: " + var0;
   }

   // $FF: synthetic method
   private static String stringConcat$5(int var0) {
      return "public, protected and private are mutually exclusive: " + var0;
   }

   // $FF: synthetic method
   private static String stringConcat$6(int var0) {
      return "final and abstract are mutually exclusive: " + var0;
   }

   private static void checkMethodAccess(int version, int access, int possibleAccess) {
      checkAccess(access, possibleAccess);
      if ((version & '\uffff') < 61 && Integer.bitCount(access & 3072) > 1) {
         throw new IllegalArgumentException(stringConcat$7(access));
      }
   }

   // $FF: synthetic method
   private static String stringConcat$7(int var0) {
      return "strictfp and abstract are mutually exclusive: " + var0;
   }

   static void checkFullyQualifiedName(int version, String name, String source) {
      try {
         int startIndex;
         int dotIndex;
         for(startIndex = 0; (dotIndex = name.indexOf(46, startIndex + 1)) != -1; startIndex = dotIndex + 1) {
            CheckMethodAdapter.checkIdentifier(version, name, startIndex, dotIndex, (String)null);
         }

         CheckMethodAdapter.checkIdentifier(version, name, startIndex, name.length(), (String)null);
      } catch (IllegalArgumentException e) {
         throw new IllegalArgumentException(stringConcat$8(source, name), e);
      }
   }

   // $FF: synthetic method
   private static String stringConcat$8(String var0, String var1) {
      return "Invalid " + var0 + " (must be a fully qualified name): " + var1;
   }

   public static void checkClassSignature(String signature) {
      int pos = 0;
      if (getChar(signature, 0) == '<') {
         pos = checkTypeParameters(signature, pos);
      }

      for(pos = checkClassTypeSignature(signature, pos); getChar(signature, pos) == 'L'; pos = checkClassTypeSignature(signature, pos)) {
      }

      if (pos != signature.length()) {
         throw new IllegalArgumentException(stringConcat$9(signature, pos));
      }
   }

   // $FF: synthetic method
   private static String stringConcat$9(String var0, int var1) {
      return var0 + ": error at index " + var1;
   }

   public static void checkMethodSignature(String signature) {
      int pos = 0;
      if (getChar(signature, 0) == '<') {
         pos = checkTypeParameters(signature, pos);
      }

      for(pos = checkChar('(', signature, pos); "ZCBSIFJDL[T".indexOf(getChar(signature, pos)) != -1; pos = checkJavaTypeSignature(signature, pos)) {
      }

      pos = checkChar(')', signature, pos);
      if (getChar(signature, pos) == 'V') {
         ++pos;
      } else {
         pos = checkJavaTypeSignature(signature, pos);
      }

      while(getChar(signature, pos) == '^') {
         ++pos;
         if (getChar(signature, pos) == 'L') {
            pos = checkClassTypeSignature(signature, pos);
         } else {
            pos = checkTypeVariableSignature(signature, pos);
         }
      }

      if (pos != signature.length()) {
         throw new IllegalArgumentException(stringConcat$10(signature, pos));
      }
   }

   // $FF: synthetic method
   private static String stringConcat$10(String var0, int var1) {
      return var0 + ": error at index " + var1;
   }

   public static void checkFieldSignature(String signature) {
      int pos = checkReferenceTypeSignature(signature, 0);
      if (pos != signature.length()) {
         throw new IllegalArgumentException(stringConcat$11(signature, pos));
      }
   }

   // $FF: synthetic method
   private static String stringConcat$11(String var0, int var1) {
      return var0 + ": error at index " + var1;
   }

   private static int checkTypeParameters(String signature, int startPos) {
      int pos = checkChar('<', signature, startPos);

      for(pos = checkTypeParameter(signature, pos); getChar(signature, pos) != '>'; pos = checkTypeParameter(signature, pos)) {
      }

      return pos + 1;
   }

   private static int checkTypeParameter(String signature, int startPos) {
      int pos = checkSignatureIdentifier(signature, startPos);
      pos = checkChar(':', signature, pos);
      if ("L[T".indexOf(getChar(signature, pos)) != -1) {
         pos = checkReferenceTypeSignature(signature, pos);
      }

      while(getChar(signature, pos) == ':') {
         pos = checkReferenceTypeSignature(signature, pos + 1);
      }

      return pos;
   }

   private static int checkReferenceTypeSignature(String signature, int pos) {
      switch (getChar(signature, pos)) {
         case 'L':
            return checkClassTypeSignature(signature, pos);
         case '[':
            return checkJavaTypeSignature(signature, pos + 1);
         default:
            return checkTypeVariableSignature(signature, pos);
      }
   }

   private static int checkClassTypeSignature(String signature, int startPos) {
      int pos = checkChar('L', signature, startPos);

      for(pos = checkSignatureIdentifier(signature, pos); getChar(signature, pos) == '/'; pos = checkSignatureIdentifier(signature, pos + 1)) {
      }

      if (getChar(signature, pos) == '<') {
         pos = checkTypeArguments(signature, pos);
      }

      while(getChar(signature, pos) == '.') {
         pos = checkSignatureIdentifier(signature, pos + 1);
         if (getChar(signature, pos) == '<') {
            pos = checkTypeArguments(signature, pos);
         }
      }

      return checkChar(';', signature, pos);
   }

   private static int checkTypeArguments(String signature, int startPos) {
      int pos = checkChar('<', signature, startPos);

      for(pos = checkTypeArgument(signature, pos); getChar(signature, pos) != '>'; pos = checkTypeArgument(signature, pos)) {
      }

      return pos + 1;
   }

   private static int checkTypeArgument(String signature, int startPos) {
      int pos = startPos;
      char c = getChar(signature, startPos);
      if (c == '*') {
         return startPos + 1;
      } else {
         if (c == '+' || c == '-') {
            pos = startPos + 1;
         }

         return checkReferenceTypeSignature(signature, pos);
      }
   }

   private static int checkTypeVariableSignature(String signature, int startPos) {
      int pos = checkChar('T', signature, startPos);
      pos = checkSignatureIdentifier(signature, pos);
      return checkChar(';', signature, pos);
   }

   private static int checkJavaTypeSignature(String signature, int startPos) {
      switch (getChar(signature, startPos)) {
         case 'B':
         case 'C':
         case 'D':
         case 'F':
         case 'I':
         case 'J':
         case 'S':
         case 'Z':
            return startPos + 1;
         case 'E':
         case 'G':
         case 'H':
         case 'K':
         case 'L':
         case 'M':
         case 'N':
         case 'O':
         case 'P':
         case 'Q':
         case 'R':
         case 'T':
         case 'U':
         case 'V':
         case 'W':
         case 'X':
         case 'Y':
         default:
            return checkReferenceTypeSignature(signature, startPos);
      }
   }

   private static int checkSignatureIdentifier(String signature, int startPos) {
      int pos;
      for(pos = startPos; pos < signature.length() && ".;[/<>:".indexOf(signature.codePointAt(pos)) == -1; pos = signature.offsetByCodePoints(pos, 1)) {
      }

      if (pos == startPos) {
         throw new IllegalArgumentException(stringConcat$12(signature, startPos));
      } else {
         return pos;
      }
   }

   // $FF: synthetic method
   private static String stringConcat$12(String var0, int var1) {
      return var0 + ": identifier expected at index " + var1;
   }

   private static int checkChar(char c, String signature, int pos) {
      if (getChar(signature, pos) == c) {
         return pos + 1;
      } else {
         throw new IllegalArgumentException(stringConcat$13(signature, c, pos));
      }
   }

   // $FF: synthetic method
   private static String stringConcat$13(String var0, char var1, int var2) {
      return var0 + ": '" + var1 + "' expected at index " + var2;
   }

   private static char getChar(String string, int pos) {
      return pos < string.length() ? string.charAt(pos) : '\u0000';
   }

   static void checkTypeRef(int typeRef) {
      int mask = 0;
      switch (typeRef >>> 24) {
         case 0:
         case 1:
         case 22:
            mask = -65536;
         case 2:
         case 3:
         case 4:
         case 5:
         case 6:
         case 7:
         case 8:
         case 9:
         case 10:
         case 11:
         case 12:
         case 13:
         case 14:
         case 15:
         case 24:
         case 25:
         case 26:
         case 27:
         case 28:
         case 29:
         case 30:
         case 31:
         case 32:
         case 33:
         case 34:
         case 35:
         case 36:
         case 37:
         case 38:
         case 39:
         case 40:
         case 41:
         case 42:
         case 43:
         case 44:
         case 45:
         case 46:
         case 47:
         case 48:
         case 49:
         case 50:
         case 51:
         case 52:
         case 53:
         case 54:
         case 55:
         case 56:
         case 57:
         case 58:
         case 59:
         case 60:
         case 61:
         case 62:
         case 63:
         default:
            break;
         case 16:
         case 17:
         case 18:
         case 23:
         case 66:
            mask = -256;
            break;
         case 19:
         case 20:
         case 21:
         case 64:
         case 65:
         case 67:
         case 68:
         case 69:
         case 70:
            mask = -16777216;
            break;
         case 71:
         case 72:
         case 73:
         case 74:
         case 75:
            mask = -16776961;
      }

      if (mask == 0 || (typeRef & ~mask) != 0) {
         throw new IllegalArgumentException(stringConcat$14(Integer.toHexString(typeRef)));
      }
   }

   // $FF: synthetic method
   private static String stringConcat$14(String var0) {
      return "Invalid type reference 0x" + var0;
   }

   private static String packageName(String name) {
      int index = name.lastIndexOf(47);
      return index == -1 ? "" : name.substring(0, index);
   }

   public static void main(String[] args) throws IOException {
      main(args, new PrintWriter(System.err, true));
   }

   static void main(String[] args, PrintWriter logger) throws IOException {
      if (args.length != 1) {
         logger.println("Verifies the given class.\nUsage: CheckClassAdapter <fully qualified class name or class file name>");
      } else {
         ClassReader classReader;
         if (args[0].endsWith(".class")) {
            InputStream inputStream = new FileInputStream(args[0]);

            try {
               classReader = new ClassReader(inputStream);
            } catch (Throwable var7) {
               try {
                  inputStream.close();
               } catch (Throwable var6) {
               }

               throw var7;
            }

            inputStream.close();
         } else {
            classReader = new ClassReader(args[0]);
         }

         verify(classReader, false, logger);
      }
   }

   public static void verify(ClassReader classReader, boolean printResults, PrintWriter printWriter) {
      verify(classReader, (ClassLoader)null, printResults, printWriter);
   }

   public static void verify(ClassReader classReader, ClassLoader loader, boolean printResults, PrintWriter printWriter) {
      ClassNode classNode = new ClassNode();
      classReader.accept(new CheckClassAdapter(589824, classNode, false) {
      }, 2);
      Type syperType = classNode.superName == null ? null : Type.getObjectType(classNode.superName);
      List<MethodNode> methods = classNode.methods;
      List<Type> interfaces = new ArrayList();

      for(String interfaceName : classNode.interfaces) {
         interfaces.add(Type.getObjectType(interfaceName));
      }

      for(MethodNode method : methods) {
         SimpleVerifier verifier = new SimpleVerifier(Type.getObjectType(classNode.name), syperType, interfaces, (classNode.access & 512) != 0);
         Analyzer<BasicValue> analyzer = new Analyzer<BasicValue>(verifier);
         if (loader != null) {
            verifier.setClassLoader(loader);
         }

         try {
            analyzer.analyze(classNode.name, method);
         } catch (AnalyzerException e) {
            e.printStackTrace(printWriter);
         }

         if (printResults) {
            printAnalyzerResult(method, analyzer, printWriter);
         }
      }

      printWriter.flush();
   }

   static void printAnalyzerResult(MethodNode method, Analyzer<BasicValue> analyzer, PrintWriter printWriter) {
      Textifier textifier = new Textifier();
      TraceMethodVisitor traceMethodVisitor = new TraceMethodVisitor(textifier);
      printWriter.println(stringConcat$15(method.name, method.desc));

      for(int i = 0; i < method.instructions.size(); ++i) {
         method.instructions.get(i).accept(traceMethodVisitor);
         StringBuilder stringBuilder = new StringBuilder();
         Frame<BasicValue> frame = analyzer.getFrames()[i];
         if (frame == null) {
            stringBuilder.append('?');
         } else {
            for(int j = 0; j < frame.getLocals(); ++j) {
               stringBuilder.append(getUnqualifiedName(((BasicValue)frame.getLocal(j)).toString())).append(' ');
            }

            stringBuilder.append(" : ");

            for(int j = 0; j < frame.getStackSize(); ++j) {
               stringBuilder.append(getUnqualifiedName(((BasicValue)frame.getStack(j)).toString())).append(' ');
            }
         }

         while(stringBuilder.length() < method.maxStack + method.maxLocals + 1) {
            stringBuilder.append(' ');
         }

         printWriter.print(Integer.toString(i + 100000).substring(1));
         printWriter.print(stringConcat$16(String.valueOf(stringBuilder), String.valueOf(textifier.text.get(textifier.text.size() - 1))));
      }

      for(TryCatchBlockNode tryCatchBlock : method.tryCatchBlocks) {
         tryCatchBlock.accept(traceMethodVisitor);
         printWriter.print(stringConcat$17(String.valueOf(textifier.text.get(textifier.text.size() - 1))));
      }

      printWriter.println();
   }

   // $FF: synthetic method
   private static String stringConcat$15(String var0, String var1) {
      return var0 + var1;
   }

   // $FF: synthetic method
   private static String stringConcat$16(String var0, String var1) {
      return " " + var0 + " : " + var1;
   }

   // $FF: synthetic method
   private static String stringConcat$17(String var0) {
      return " " + var0;
   }

   private static String getUnqualifiedName(String name) {
      int lastSlashIndex = name.lastIndexOf(47);
      if (lastSlashIndex == -1) {
         return name;
      } else {
         int endIndex = name.length();
         if (name.charAt(endIndex - 1) == ';') {
            --endIndex;
         }

         int lastBracketIndex = name.lastIndexOf(91);
         return lastBracketIndex == -1 ? name.substring(lastSlashIndex + 1, endIndex) : stringConcat$18(name.substring(0, lastBracketIndex + 1), name.substring(lastSlashIndex + 1, endIndex));
      }
   }

   // $FF: synthetic method
   private static String stringConcat$18(String var0, String var1) {
      return var0 + var1;
   }
}
