package org.spongepowered.asm.util;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.TraceClassVisitor;
import org.spongepowered.asm.util.asm.ASM;
import org.spongepowered.asm.util.asm.MarkerNode;
import org.spongepowered.asm.util.throwables.SyntheticBridgeException;
import org.spongepowered.include.com.google.common.base.Joiner;
import org.spongepowered.include.com.google.common.collect.Iterators;
import org.spongepowered.include.com.google.common.primitives.Ints;

public final class Bytecode {
   public static final int[] CONSTANTS_INT = new int[]{2, 3, 4, 5, 6, 7, 8};
   public static final int[] CONSTANTS_FLOAT = new int[]{11, 12, 13};
   public static final int[] CONSTANTS_DOUBLE = new int[]{14, 15};
   public static final int[] CONSTANTS_LONG = new int[]{9, 10};
   public static final int[] CONSTANTS_ALL = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 192, 193};
   private static final Object[] CONSTANTS_VALUES;
   private static final String[] CONSTANTS_TYPES;
   private static final String[] BOXING_TYPES;
   private static final String[] UNBOXING_METHODS;

   private Bytecode() {
   }

   public static MethodNode findMethod(ClassNode classNode, String name, String desc) {
      for(MethodNode method : classNode.methods) {
         if (method.name.equals(name) && method.desc.equals(desc)) {
            return method;
         }
      }

      return null;
   }

   public static AbstractInsnNode findInsn(MethodNode method, int opcode) {
      Iterator<AbstractInsnNode> findReturnIter = method.instructions.iterator();

      while(findReturnIter.hasNext()) {
         AbstractInsnNode insn = (AbstractInsnNode)findReturnIter.next();
         if (insn.getOpcode() == opcode) {
            return insn;
         }
      }

      return null;
   }

   public static DelegateInitialiser findDelegateInit(MethodNode ctor, String superName, String ownerName) {
      if (!"<init>".equals(ctor.name)) {
         return Bytecode.DelegateInitialiser.NONE;
      } else {
         int news = 0;
         Iterator<AbstractInsnNode> iter = ctor.instructions.iterator();

         while(iter.hasNext()) {
            AbstractInsnNode insn = (AbstractInsnNode)iter.next();
            if (!(insn instanceof TypeInsnNode) || insn.getOpcode() != 187) {
               if (insn instanceof MethodInsnNode && insn.getOpcode() == 183) {
                  MethodInsnNode methodNode = (MethodInsnNode)insn;
                  if ("<init>".equals(methodNode.name)) {
                     if (news <= 0) {
                        boolean isSuper = methodNode.owner.equals(superName);
                        if (isSuper || methodNode.owner.equals(ownerName)) {
                           return new DelegateInitialiser(methodNode, isSuper);
                        }
                     } else {
                        --news;
                     }
                  }
               }
            } else {
               ++news;
            }
         }

         return Bytecode.DelegateInitialiser.NONE;
      }
   }

   public static void textify(ClassNode classNode, OutputStream out) {
      classNode.accept(new TraceClassVisitor(new PrintWriter(out)));
   }

   public static void textify(MethodNode methodNode, OutputStream out) {
      TraceClassVisitor trace = new TraceClassVisitor(new PrintWriter(out));
      MethodVisitor mv = trace.visitMethod(methodNode.access, methodNode.name, methodNode.desc, methodNode.signature, (String[])methodNode.exceptions.toArray(new String[0]));
      methodNode.accept(mv);
      trace.visitEnd();
   }

   public static void dumpClass(ClassNode classNode) {
      ClassWriter cw = new ClassWriter(3);
      classNode.accept(cw);
      dumpClass(cw.toByteArray());
   }

   public static void dumpClass(byte[] bytes) {
      ClassReader cr = new ClassReader(bytes);
      CheckClassAdapter.verify(cr, true, new PrintWriter(System.out));
   }

   public static void printMethodWithOpcodeIndices(MethodNode method) {
      System.err.printf("%s%s\n", method.name, method.desc);
      int i = 0;
      Iterator<AbstractInsnNode> iter = method.instructions.iterator();

      while(iter.hasNext()) {
         System.err.printf("[%4d] %s\n", i++, describeNode((AbstractInsnNode)iter.next()));
      }

   }

   public static void printMethod(MethodNode method) {
      System.err.printf("%s%s maxStack=%d maxLocals=%d\n", method.name, method.desc, method.maxStack, method.maxLocals);
      int index = 0;
      Iterator<AbstractInsnNode> iter = method.instructions.iterator();

      while(iter.hasNext()) {
         System.err.printf("%-4d  ", index++);
         printNode((AbstractInsnNode)iter.next());
      }

   }

   public static void printNode(AbstractInsnNode node) {
      System.err.printf("%s\n", describeNode(node));
   }

   public static String describeNode(AbstractInsnNode node) {
      return describeNode(node, true);
   }

   public static String describeNode(AbstractInsnNode node, boolean listFormat) {
      if (node == null) {
         return listFormat ? String.format("   %-14s ", "null") : "null";
      } else if (node instanceof MarkerNode) {
         MarkerNode marker = (MarkerNode)node;
         return String.format("[%s] Marker type=%d", marker.getLabel(), marker.type);
      } else if (node instanceof LabelNode) {
         return String.format("[%s]", ((LabelNode)node).getLabel());
      } else {
         String out = String.format(listFormat ? "   %-14s " : "%s ", node.getClass().getSimpleName().replace("Node", ""));
         if (node instanceof JumpInsnNode) {
            out = out + String.format("[%s] [%s]", getOpcodeName(node), ((JumpInsnNode)node).label.getLabel());
         } else if (node instanceof VarInsnNode) {
            out = out + String.format("[%s] %d", getOpcodeName(node), ((VarInsnNode)node).var);
         } else if (node instanceof MethodInsnNode) {
            MethodInsnNode mth = (MethodInsnNode)node;
            out = out + String.format("[%s] %s::%s%s", getOpcodeName(node), mth.owner, mth.name, mth.desc);
         } else if (node instanceof FieldInsnNode) {
            FieldInsnNode fld = (FieldInsnNode)node;
            out = out + String.format("[%s] %s::%s:%s", getOpcodeName(node), fld.owner, fld.name, fld.desc);
         } else if (node instanceof InvokeDynamicInsnNode) {
            InvokeDynamicInsnNode idc = (InvokeDynamicInsnNode)node;
            out = out + String.format("[%s] %s%s { %s %s::%s%s }", getOpcodeName(node), idc.name, idc.desc, getOpcodeName(idc.bsm.getTag(), Printer.HANDLE_TAG), idc.bsm.getOwner(), idc.bsm.getName(), idc.bsm.getDesc());
         } else if (node instanceof LineNumberNode) {
            LineNumberNode ln = (LineNumberNode)node;
            out = out + String.format("LINE=[%d] LABEL=[%s]", ln.line, ln.start.getLabel());
         } else if (node instanceof LdcInsnNode) {
            out = out + ((LdcInsnNode)node).cst;
         } else if (node instanceof IntInsnNode) {
            out = out + ((IntInsnNode)node).operand;
         } else if (node instanceof FrameNode) {
            out = out + String.format("[%s] ", getFrameTypeName((FrameNode)node));
         } else if (node instanceof TypeInsnNode) {
            out = out + String.format("[%s] %s", getOpcodeName(node), ((TypeInsnNode)node).desc);
         } else {
            out = out + String.format("[%s] ", getOpcodeName(node));
         }

         return out;
      }
   }

   public static String getOpcodeName(AbstractInsnNode node) {
      return node != null ? getOpcodeName(node.getOpcode()) : "";
   }

   public static String getOpcodeName(int opcode) {
      return getOpcodeName(opcode, Printer.OPCODES);
   }

   private static String getOpcodeName(int opcode, String[] names) {
      if (opcode < 0) {
         return "UNKNOWN";
      } else {
         return opcode < names.length ? names[opcode] : String.valueOf(opcode);
      }
   }

   private static String getFrameTypeName(FrameNode node) {
      switch (node.type) {
         case -1:
            return "F_NEW";
         case 0:
            return "F_FULL";
         case 1:
            return "F_APPEND";
         case 2:
            return "F_CHOP";
         case 3:
            return "F_SAME";
         case 4:
            return "F_SAME1";
         default:
            return "UNKNOWN";
      }
   }

   public static int parseOpcodeName(String opcodeName) {
      if (opcodeName == null) {
         return -1;
      } else if (opcodeName.matches("^1[0-9]{0,2}|[1-9][0-9]?$")) {
         return Integer.parseInt(opcodeName);
      } else {
         if (opcodeName.startsWith("Opcodes.")) {
            opcodeName = opcodeName.substring(8);
         }

         return !opcodeName.matches("^[A-Z][A-Z0-9_]+$") ? -1 : parseOpcodeName(opcodeName, Printer.OPCODES);
      }
   }

   private static int parseOpcodeName(String name, String[] names) {
      for(int i = 0; i < names.length; ++i) {
         if (name.equalsIgnoreCase(names[i])) {
            return i;
         }
      }

      return -1;
   }

   public static boolean methodHasLineNumbers(MethodNode method) {
      Iterator<AbstractInsnNode> iter = method.instructions.iterator();

      while(iter.hasNext()) {
         if (iter.next() instanceof LineNumberNode) {
            return true;
         }
      }

      return false;
   }

   public static boolean isStatic(MethodNode method) {
      return (method.access & 8) == 8;
   }

   public static boolean isStatic(FieldNode field) {
      return (field.access & 8) == 8;
   }

   public static int getFirstNonArgLocalIndex(MethodNode method) {
      return getFirstNonArgLocalIndex(Type.getArgumentTypes(method.desc), !isStatic(method));
   }

   public static int getFirstNonArgLocalIndex(Type[] args, boolean includeThis) {
      return getArgsSize(args) + (includeThis ? 1 : 0);
   }

   public static int getArgsSize(Type[] args) {
      return getArgsSize(args, 0, args.length);
   }

   public static int getArgsSize(Type[] args, int startIndex, int endIndex) {
      int size = 0;

      for(int index = startIndex; index < args.length && index < endIndex; ++index) {
         size += args[index].getSize();
      }

      return size;
   }

   public static void loadArgs(Type[] args, InsnList insns, int pos) {
      loadArgs(args, insns, pos, -1);
   }

   public static void loadArgs(Type[] args, InsnList insns, int start, int end) {
      loadArgs(args, insns, start, end, (Type[])null);
   }

   public static void loadArgs(Type[] args, InsnList insns, int start, int end, Type[] casts) {
      int pos = start;

      for(int index = 0; index < args.length; ++index) {
         insns.add((AbstractInsnNode)(new VarInsnNode(args[index].getOpcode(21), pos)));
         if (casts != null && index < casts.length && casts[index] != null) {
            insns.add((AbstractInsnNode)(new TypeInsnNode(192, casts[index].getInternalName())));
         }

         pos += args[index].getSize();
         if (end >= start && pos >= end) {
            return;
         }
      }

   }

   public static Type[] getTypes(Class<?>... classes) {
      Type[] types = new Type[classes.length];

      for(int index = 0; index < classes.length; ++index) {
         types[index] = Type.getType(classes[index]);
      }

      return types;
   }

   public static Map<LabelNode, LabelNode> cloneLabels(InsnList source) {
      Map<LabelNode, LabelNode> labels = new HashMap();
      Iterator<AbstractInsnNode> iter = source.iterator();

      while(iter.hasNext()) {
         AbstractInsnNode insn = (AbstractInsnNode)iter.next();
         if (insn instanceof LabelNode) {
            labels.put((LabelNode)insn, new LabelNode(((LabelNode)insn).getLabel()));
         }
      }

      return labels;
   }

   public static String generateDescriptor(Type returnType, Type... args) {
      return generateDescriptor((Object)returnType, (Object[])(args));
   }

   public static String generateDescriptor(Object returnType, Object... args) {
      StringBuilder sb = (new StringBuilder()).append('(');

      for(Object arg : args) {
         sb.append(toDescriptor(arg));
      }

      return sb.append(')').append(returnType != null ? toDescriptor(returnType) : "V").toString();
   }

   private static String toDescriptor(Object arg) {
      if (arg instanceof String) {
         return (String)arg;
      } else if (arg instanceof Type) {
         return arg.toString();
      } else if (arg instanceof Class) {
         return Type.getDescriptor((Class)arg);
      } else {
         return arg == null ? "" : arg.toString();
      }
   }

   public static String getDescriptor(Type... args) {
      return "(" + Joiner.on("").join(args) + ")";
   }

   public static String getDescriptor(Type returnType, Type... args) {
      return getDescriptor(args) + returnType.toString();
   }

   public static String changeDescriptorReturnType(String desc, String returnType) {
      if (desc != null && desc.startsWith("(") && desc.lastIndexOf(41) >= 1) {
         return returnType == null ? desc : desc.substring(0, desc.lastIndexOf(41) + 1) + returnType;
      } else {
         return null;
      }
   }

   public static String getSimpleName(Type type) {
      return type.getSort() < 9 ? type.getDescriptor() : getSimpleName(type.getClassName());
   }

   public static String getSimpleName(String desc) {
      int pos = Math.max(desc.lastIndexOf(47), 0);
      return desc.substring(pos + 1).replace(";", "");
   }

   public static boolean isConstant(AbstractInsnNode insn) {
      return insn == null ? false : Ints.contains(CONSTANTS_ALL, insn.getOpcode());
   }

   public static Object getConstant(AbstractInsnNode insn) {
      if (insn == null) {
         return null;
      } else if (insn instanceof LdcInsnNode) {
         return ((LdcInsnNode)insn).cst;
      } else if (insn instanceof IntInsnNode) {
         int value = ((IntInsnNode)insn).operand;
         return insn.getOpcode() != 16 && insn.getOpcode() != 17 ? null : value;
      } else if (insn instanceof TypeInsnNode) {
         return insn.getOpcode() < 192 ? null : Type.getObjectType(((TypeInsnNode)insn).desc);
      } else {
         int index = Ints.indexOf(CONSTANTS_ALL, insn.getOpcode());
         return index < 0 ? null : CONSTANTS_VALUES[index];
      }
   }

   public static Type getConstantType(AbstractInsnNode insn) {
      if (insn == null) {
         return null;
      } else if (insn instanceof LdcInsnNode) {
         Object cst = ((LdcInsnNode)insn).cst;
         if (cst instanceof Integer) {
            return Type.getType("I");
         } else if (cst instanceof Float) {
            return Type.getType("F");
         } else if (cst instanceof Long) {
            return Type.getType("J");
         } else if (cst instanceof Double) {
            return Type.getType("D");
         } else if (cst instanceof String) {
            return Type.getType("Ljava/lang/String;");
         } else if (cst instanceof Type) {
            return Type.getType("Ljava/lang/Class;");
         } else {
            throw new IllegalArgumentException("LdcInsnNode with invalid payload type " + cst.getClass() + " in getConstant");
         }
      } else if (insn instanceof TypeInsnNode) {
         return insn.getOpcode() < 192 ? null : Type.getType("Ljava/lang/Class;");
      } else {
         int index = Ints.indexOf(CONSTANTS_ALL, insn.getOpcode());
         return index < 0 ? null : Type.getType(CONSTANTS_TYPES[index]);
      }
   }

   public static boolean hasFlag(ClassNode classNode, int flag) {
      return (classNode.access & flag) == flag;
   }

   public static boolean hasFlag(MethodNode method, int flag) {
      return (method.access & flag) == flag;
   }

   public static boolean hasFlag(FieldNode field, int flag) {
      return (field.access & flag) == flag;
   }

   public static boolean compareFlags(MethodNode m1, MethodNode m2, int flag) {
      return hasFlag(m1, flag) == hasFlag(m2, flag);
   }

   public static boolean compareFlags(FieldNode f1, FieldNode f2, int flag) {
      return hasFlag(f1, flag) == hasFlag(f2, flag);
   }

   public static boolean isVirtual(MethodNode method) {
      return method != null && !isStatic(method) && getVisibility(method).isAtLeast(Bytecode.Visibility.PROTECTED);
   }

   public static Visibility getVisibility(MethodNode method) {
      return getVisibility(method.access & 7);
   }

   public static Visibility getVisibility(FieldNode field) {
      return getVisibility(field.access & 7);
   }

   private static Visibility getVisibility(int flags) {
      if ((flags & 4) != 0) {
         return Bytecode.Visibility.PROTECTED;
      } else if ((flags & 2) != 0) {
         return Bytecode.Visibility.PRIVATE;
      } else {
         return (flags & 1) != 0 ? Bytecode.Visibility.PUBLIC : Bytecode.Visibility.PACKAGE;
      }
   }

   public static void setVisibility(ClassNode classNode, Visibility visibility) {
      classNode.access = setVisibility(classNode.access, visibility.access);
   }

   public static void setVisibility(MethodNode method, Visibility visibility) {
      method.access = setVisibility(method.access, visibility.access);
   }

   public static void setVisibility(FieldNode field, Visibility visibility) {
      field.access = setVisibility(field.access, visibility.access);
   }

   public static void setVisibility(ClassNode classNode, int access) {
      classNode.access = setVisibility(classNode.access, access);
   }

   public static void setVisibility(MethodNode method, int access) {
      method.access = setVisibility(method.access, access);
   }

   public static void setVisibility(FieldNode field, int access) {
      field.access = setVisibility(field.access, access);
   }

   private static int setVisibility(int oldAccess, int newAccess) {
      return oldAccess & -8 | newAccess & 7;
   }

   public static int getMaxLineNumber(ClassNode classNode, int min, int pad) {
      int max = 0;

      for(MethodNode method : classNode.methods) {
         Iterator<AbstractInsnNode> iter = method.instructions.iterator();

         while(iter.hasNext()) {
            AbstractInsnNode insn = (AbstractInsnNode)iter.next();
            if (insn instanceof LineNumberNode) {
               max = Math.max(max, ((LineNumberNode)insn).line);
            }
         }
      }

      return Math.max(min, max + pad);
   }

   public static String getBoxingType(Type type) {
      return type == null ? null : BOXING_TYPES[type.getSort()];
   }

   public static String getUnboxingMethod(Type type) {
      return type == null ? null : UNBOXING_METHODS[type.getSort()];
   }

   public static void compareBridgeMethods(MethodNode a, MethodNode b) {
      Iterator<AbstractInsnNode> ia = Iterators.<AbstractInsnNode>filter(a.instructions.iterator(), Bytecode::isRealInsn);
      Iterator<AbstractInsnNode> ib = Iterators.<AbstractInsnNode>filter(b.instructions.iterator(), Bytecode::isRealInsn);

      int index;
      for(index = 0; ia.hasNext() && ib.hasNext(); ++index) {
         AbstractInsnNode na = (AbstractInsnNode)ia.next();
         AbstractInsnNode nb = (AbstractInsnNode)ib.next();
         if (na instanceof MethodInsnNode) {
            MethodInsnNode ma = (MethodInsnNode)na;
            MethodInsnNode mb = (MethodInsnNode)nb;
            if (!ma.name.equals(mb.name)) {
               throw new SyntheticBridgeException(SyntheticBridgeException.Problem.BAD_INVOKE_NAME, a.name, a.desc, index, na, nb);
            }

            if (!ma.desc.equals(mb.desc)) {
               throw new SyntheticBridgeException(SyntheticBridgeException.Problem.BAD_INVOKE_DESC, a.name, a.desc, index, na, nb);
            }
         } else {
            if (na.getOpcode() != nb.getOpcode()) {
               throw new SyntheticBridgeException(SyntheticBridgeException.Problem.BAD_INSN, a.name, a.desc, index, na, nb);
            }

            if (na instanceof VarInsnNode) {
               VarInsnNode va = (VarInsnNode)na;
               VarInsnNode vb = (VarInsnNode)nb;
               if (va.var != vb.var) {
                  throw new SyntheticBridgeException(SyntheticBridgeException.Problem.BAD_LOAD, a.name, a.desc, index, na, nb);
               }
            } else if (na instanceof TypeInsnNode) {
               TypeInsnNode ta = (TypeInsnNode)na;
               TypeInsnNode tb = (TypeInsnNode)nb;
               if (ta.getOpcode() == 192 && !ta.desc.equals(tb.desc)) {
                  throw new SyntheticBridgeException(SyntheticBridgeException.Problem.BAD_CAST, a.name, a.desc, index, na, nb);
               }
            }
         }
      }

      if (ia.hasNext()) {
         throw new SyntheticBridgeException(SyntheticBridgeException.Problem.BAD_LENGTH, a.name, a.desc, index, (AbstractInsnNode)ia.next(), (AbstractInsnNode)null);
      } else if (ib.hasNext()) {
         throw new SyntheticBridgeException(SyntheticBridgeException.Problem.BAD_LENGTH, a.name, a.desc, index, (AbstractInsnNode)null, (AbstractInsnNode)ib.next());
      }
   }

   private static boolean isRealInsn(AbstractInsnNode insn) {
      return insn.getOpcode() != -1;
   }

   public static void merge(ClassNode source, ClassNode dest) {
      if (source != null) {
         if (dest == null) {
            throw new NullPointerException("Target ClassNode for merge must not be null");
         } else {
            dest.version = Math.max(source.version, dest.version);
            dest.interfaces = merge(source.interfaces, dest.interfaces);
            dest.invisibleAnnotations = merge(source.invisibleAnnotations, dest.invisibleAnnotations);
            dest.visibleAnnotations = merge(source.visibleAnnotations, dest.visibleAnnotations);
            dest.visibleTypeAnnotations = merge(source.visibleTypeAnnotations, dest.visibleTypeAnnotations);
            dest.invisibleTypeAnnotations = merge(source.invisibleTypeAnnotations, dest.invisibleTypeAnnotations);
            dest.attrs = merge(source.attrs, dest.attrs);
            dest.innerClasses = merge(source.innerClasses, dest.innerClasses);
            dest.fields = merge(source.fields, dest.fields);
            dest.methods = merge(source.methods, dest.methods);
         }
      }
   }

   public static void replace(ClassNode source, ClassNode dest) {
      if (source != null) {
         if (dest == null) {
            throw new NullPointerException("Target ClassNode for replace must not be null");
         } else {
            dest.name = source.name;
            dest.signature = source.signature;
            dest.superName = source.superName;
            dest.version = source.version;
            dest.access = source.access;
            dest.sourceDebug = source.sourceDebug;
            dest.sourceFile = source.sourceFile;
            dest.outerClass = source.outerClass;
            dest.outerMethod = source.outerMethod;
            dest.outerMethodDesc = source.outerMethodDesc;
            clear(dest.interfaces);
            clear(dest.visibleAnnotations);
            clear(dest.invisibleAnnotations);
            clear(dest.visibleTypeAnnotations);
            clear(dest.invisibleTypeAnnotations);
            clear(dest.attrs);
            clear(dest.innerClasses);
            clear(dest.fields);
            clear(dest.methods);
            if (ASM.API_VERSION >= 393216) {
               dest.module = source.module;
            }

            merge(source, dest);
         }
      }
   }

   private static <T> void clear(List<T> list) {
      if (list != null) {
         list.clear();
      }

   }

   private static <T> List<T> merge(List<T> source, List<T> destination) {
      if (source != null && !source.isEmpty()) {
         if (destination == null) {
            return new ArrayList(source);
         } else {
            destination.addAll(source);
            return destination;
         }
      } else {
         return destination;
      }
   }

   public static boolean isEnumValuesArray(FieldNode field, ClassNode enumClass) {
      return hasFlag((FieldNode)field, 4104) && field.desc.equals("[L" + enumClass.name + ';');
   }

   public static boolean isEnumConstant(FieldNode field, ClassNode enumClass) {
      return hasFlag((FieldNode)field, 16392) && field.desc.equals('L' + enumClass.name + ';');
   }

   public static AbstractInsnNode loadIntConstant(int intValue) {
      if (-1 <= intValue && intValue <= 5) {
         return new InsnNode(3 + intValue);
      } else if (-128 <= intValue && intValue <= 127) {
         return new IntInsnNode(16, intValue);
      } else {
         return (AbstractInsnNode)(-32768 <= intValue && intValue <= 32767 ? new IntInsnNode(17, intValue) : new LdcInsnNode(intValue));
      }
   }

   static {
      CONSTANTS_VALUES = new Object[]{Type.VOID_TYPE, -1, 0, 1, 2, 3, 4, 5, 0L, 1L, 0.0F, 1.0F, 2.0F, (double)0.0F, (double)1.0F};
      CONSTANTS_TYPES = new String[]{"V", "I", "I", "I", "I", "I", "I", "I", "J", "J", "F", "F", "F", "D", "D", "I", "I"};
      BOXING_TYPES = new String[]{null, "java/lang/Boolean", "java/lang/Character", "java/lang/Byte", "java/lang/Short", "java/lang/Integer", "java/lang/Float", "java/lang/Long", "java/lang/Double", null, null, null};
      UNBOXING_METHODS = new String[]{null, "booleanValue", "charValue", "byteValue", "shortValue", "intValue", "floatValue", "longValue", "doubleValue", null, null, null};
   }

   public static enum Visibility {
      PRIVATE(2),
      PROTECTED(4),
      PACKAGE(0),
      PUBLIC(1);

      final int access;

      private Visibility(int access) {
         this.access = access;
      }

      public boolean isAtLeast(Visibility other) {
         return other == null || other.ordinal() <= this.ordinal();
      }

      public boolean isLessThan(Visibility other) {
         return other != null && this.ordinal() < other.ordinal();
      }

      // $FF: synthetic method
      private static Visibility[] $values() {
         return new Visibility[]{PRIVATE, PROTECTED, PACKAGE, PUBLIC};
      }
   }

   public static class DelegateInitialiser {
      public static final DelegateInitialiser NONE = new DelegateInitialiser((MethodInsnNode)null, false);
      public final MethodInsnNode insn;
      public final boolean isSuper;
      public final boolean isPresent;

      DelegateInitialiser(MethodInsnNode insn, boolean isSuper) {
         this.insn = insn;
         this.isSuper = isSuper;
         this.isPresent = insn != null;
      }

      public String toString() {
         return this.isSuper ? "super" : "this";
      }
   }
}
