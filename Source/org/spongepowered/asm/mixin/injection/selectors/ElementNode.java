package org.spongepowered.asm.mixin.injection.selectors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.util.Handles;
import org.spongepowered.include.com.google.common.base.Strings;

public abstract class ElementNode<TNode> {
   public boolean isField() {
      return false;
   }

   public abstract NodeType getType();

   public MethodNode getMethod() {
      return null;
   }

   public FieldNode getField() {
      return null;
   }

   public AbstractInsnNode getInsn() {
      return null;
   }

   public abstract String getOwner();

   public abstract String getName();

   public String getSyntheticName() {
      return this.getName();
   }

   public abstract String getDesc();

   public String getDelegateDesc() {
      return this.getDesc();
   }

   public String getImplDesc() {
      return this.getDesc();
   }

   public abstract String getSignature();

   public abstract TNode get();

   public String toString() {
      String desc = Strings.nullToEmpty(this.getDesc());
      if (!desc.isEmpty() && this.isField()) {
         desc = ":" + desc;
      }

      String owner = Strings.nullToEmpty(this.getOwner());
      if (!owner.isEmpty()) {
         owner = "L" + owner + ";";
      }

      return String.format("%s%s%s", owner, Strings.nullToEmpty(this.getName()), desc);
   }

   public static ElementNode<MethodNode> of(ClassNode owner, MethodNode method) {
      return new ElementNodeMethod(owner, method);
   }

   public static ElementNode<FieldNode> of(ClassNode owner, FieldNode field) {
      return new ElementNodeField(owner, field);
   }

   public static <TNode> ElementNode<TNode> of(ClassNode owner, TNode node) {
      if (node instanceof ElementNode) {
         return (ElementNode)node;
      } else if (node instanceof MethodNode) {
         return new ElementNodeMethod(owner, (MethodNode)node);
      } else if (node instanceof FieldNode) {
         return new ElementNodeField(owner, (FieldNode)node);
      } else if (node instanceof MethodInsnNode) {
         return new ElementNodeMethodInsn((MethodInsnNode)node);
      } else if (node instanceof InvokeDynamicInsnNode) {
         return new ElementNodeInvokeDynamicInsn((InvokeDynamicInsnNode)node);
      } else if (node instanceof FieldInsnNode) {
         return new ElementNodeFieldInsn((FieldInsnNode)node);
      } else {
         throw new IllegalArgumentException("Could not create ElementNode for unknown node type: " + node.getClass().getName());
      }
   }

   public static <TNode extends AbstractInsnNode> ElementNode<TNode> of(TNode node) {
      if (node instanceof MethodInsnNode) {
         return new ElementNodeMethodInsn((MethodInsnNode)node);
      } else if (node instanceof InvokeDynamicInsnNode) {
         return new ElementNodeInvokeDynamicInsn((InvokeDynamicInsnNode)node);
      } else {
         return node instanceof FieldInsnNode ? new ElementNodeFieldInsn((FieldInsnNode)node) : null;
      }
   }

   public static <TNode> List<ElementNode<TNode>> listOf(ClassNode owner, List<TNode> list) {
      List<ElementNode<TNode>> nodes = new ArrayList();

      for(TNode node : list) {
         nodes.add(of(owner, node));
      }

      return nodes;
   }

   public static List<ElementNode<FieldNode>> fieldList(ClassNode owner) {
      List<ElementNode<FieldNode>> fields = new ArrayList();

      for(FieldNode field : owner.fields) {
         fields.add(new ElementNodeField(owner, field));
      }

      return fields;
   }

   public static List<ElementNode<MethodNode>> methodList(ClassNode owner) {
      List<ElementNode<MethodNode>> methods = new ArrayList();

      for(MethodNode method : owner.methods) {
         methods.add(new ElementNodeMethod(owner, method));
      }

      return methods;
   }

   public static Iterable<ElementNode<AbstractInsnNode>> insnList(InsnList insns) {
      return new ElementNodeIterable(insns, false);
   }

   public static Iterable<ElementNode<AbstractInsnNode>> dynamicInsnList(InsnList insns) {
      return new ElementNodeIterable(insns, true);
   }

   public static enum NodeType {
      UNDEFINED(false, false, false),
      METHOD(true, false, false),
      FIELD(false, true, false),
      METHOD_INSN(false, false, true),
      FIELD_INSN(false, false, true),
      INVOKEDYNAMIC_INSN(false, false, true);

      public final boolean hasMethod;
      public final boolean hasField;
      public final boolean hasInsn;

      private NodeType(boolean isMethod, boolean isField, boolean isInsn) {
         this.hasMethod = isMethod;
         this.hasField = isField;
         this.hasInsn = isInsn;
      }

      // $FF: synthetic method
      private static NodeType[] $values() {
         return new NodeType[]{UNDEFINED, METHOD, FIELD, METHOD_INSN, FIELD_INSN, INVOKEDYNAMIC_INSN};
      }
   }

   static class ElementNodeMethod extends ElementNode<MethodNode> {
      private final ClassNode owner;
      private final MethodNode method;

      ElementNodeMethod(ClassNode owner, MethodNode method) {
         this.owner = owner;
         this.method = method;
      }

      public NodeType getType() {
         return ElementNode.NodeType.METHOD;
      }

      public MethodNode getMethod() {
         return this.method;
      }

      public String getOwner() {
         return this.owner != null ? this.owner.name : null;
      }

      public String getName() {
         return this.method.name;
      }

      public String getDesc() {
         return this.method.desc;
      }

      public String getSignature() {
         return this.method.signature;
      }

      public MethodNode get() {
         return this.method;
      }

      public boolean equals(Object obj) {
         return this.method.equals(obj);
      }

      public int hashCode() {
         return this.method.hashCode();
      }
   }

   static class ElementNodeField extends ElementNode<FieldNode> {
      private final ClassNode owner;
      private final FieldNode field;

      ElementNodeField(ClassNode owner, FieldNode field) {
         this.owner = owner;
         this.field = field;
      }

      public NodeType getType() {
         return ElementNode.NodeType.FIELD;
      }

      public boolean isField() {
         return true;
      }

      public FieldNode getField() {
         return this.field;
      }

      public String getOwner() {
         return this.owner != null ? this.owner.name : null;
      }

      public String getName() {
         return this.field.name;
      }

      public String getDesc() {
         return this.field.desc;
      }

      public String getSignature() {
         return this.field.signature;
      }

      public FieldNode get() {
         return this.field;
      }

      public boolean equals(Object obj) {
         return this.field.equals(obj);
      }

      public int hashCode() {
         return this.field.hashCode();
      }
   }

   static class ElementNodeMethodInsn extends ElementNode<MethodInsnNode> {
      private MethodInsnNode insn;

      ElementNodeMethodInsn(MethodInsnNode method) {
         this.insn = method;
      }

      public NodeType getType() {
         return ElementNode.NodeType.METHOD_INSN;
      }

      public AbstractInsnNode getInsn() {
         return this.insn;
      }

      public String getOwner() {
         return this.insn.owner;
      }

      public String getName() {
         return this.insn.name;
      }

      public String getDesc() {
         return this.insn.desc;
      }

      public String getSignature() {
         return null;
      }

      public MethodInsnNode get() {
         return this.insn;
      }

      public boolean equals(Object obj) {
         return this.insn.equals(obj);
      }

      public int hashCode() {
         return this.insn.hashCode();
      }
   }

   static class ElementNodeInvokeDynamicInsn extends ElementNode<InvokeDynamicInsnNode> {
      private InvokeDynamicInsnNode insn;
      private Type samMethodType;
      private Handle implMethod;
      private Type instantiatedMethodType;

      ElementNodeInvokeDynamicInsn(InvokeDynamicInsnNode invokeDynamic) {
         this.insn = invokeDynamic;
         if (invokeDynamic.bsmArgs != null && invokeDynamic.bsmArgs.length > 1) {
            Object samMethodType = invokeDynamic.bsmArgs[0];
            Object implMethod = invokeDynamic.bsmArgs[1];
            Object instantiatedMethodType = invokeDynamic.bsmArgs[2];
            if (samMethodType instanceof Type && implMethod instanceof Handle && instantiatedMethodType instanceof Type) {
               this.samMethodType = (Type)samMethodType;
               this.implMethod = (Handle)implMethod;
               this.instantiatedMethodType = (Type)instantiatedMethodType;
            }
         }

      }

      public NodeType getType() {
         return ElementNode.NodeType.INVOKEDYNAMIC_INSN;
      }

      public boolean isField() {
         return this.implMethod != null && Handles.isField(this.implMethod);
      }

      public AbstractInsnNode getInsn() {
         return this.insn;
      }

      public String getOwner() {
         return this.implMethod != null ? this.implMethod.getOwner() : this.insn.name;
      }

      public String getName() {
         return this.insn.name;
      }

      public String getSyntheticName() {
         return this.implMethod != null ? this.implMethod.getName() : this.insn.name;
      }

      public String getDesc() {
         return this.implMethod != null ? this.implMethod.getDesc() : this.insn.desc;
      }

      public String getDelegateDesc() {
         return this.samMethodType != null ? this.samMethodType.getDescriptor() : this.getDesc();
      }

      public String getImplDesc() {
         return this.instantiatedMethodType != null ? this.instantiatedMethodType.getDescriptor() : this.getDesc();
      }

      public String getSignature() {
         return null;
      }

      public InvokeDynamicInsnNode get() {
         return this.insn;
      }

      public boolean equals(Object obj) {
         return this.insn.equals(obj);
      }

      public int hashCode() {
         return this.insn.hashCode();
      }
   }

   static class ElementNodeFieldInsn extends ElementNode<FieldInsnNode> {
      private FieldInsnNode insn;

      ElementNodeFieldInsn(FieldInsnNode field) {
         this.insn = field;
      }

      public NodeType getType() {
         return ElementNode.NodeType.FIELD_INSN;
      }

      public boolean isField() {
         return true;
      }

      public AbstractInsnNode getInsn() {
         return this.insn;
      }

      public String getOwner() {
         return this.insn.owner;
      }

      public String getName() {
         return this.insn.name;
      }

      public String getDesc() {
         return this.insn.desc;
      }

      public String getSignature() {
         return null;
      }

      public FieldInsnNode get() {
         return this.insn;
      }

      public boolean equals(Object obj) {
         return this.insn.equals(obj);
      }

      public int hashCode() {
         return this.insn.hashCode();
      }
   }

   static class ElementNodeIterator implements Iterator<ElementNode<AbstractInsnNode>> {
      private final Iterator<AbstractInsnNode> iter;
      private final boolean filterDynamic;

      ElementNodeIterator(Iterator<AbstractInsnNode> iter, boolean filterDynamic) {
         this.iter = iter;
         this.filterDynamic = filterDynamic;
      }

      public boolean hasNext() {
         return this.iter.hasNext();
      }

      public ElementNode<AbstractInsnNode> next() {
         AbstractInsnNode elem = (AbstractInsnNode)this.iter.next();
         return this.filterDynamic && (elem == null || elem.getOpcode() != 186) ? null : ElementNode.of(elem);
      }
   }

   static class ElementNodeIterable implements Iterable<ElementNode<AbstractInsnNode>> {
      private final Iterable<AbstractInsnNode> iterable;
      private final boolean filterDynamic;

      public ElementNodeIterable(Iterable<AbstractInsnNode> iterable, boolean filterDynamic) {
         this.iterable = iterable;
         this.filterDynamic = filterDynamic;
      }

      public Iterator<ElementNode<AbstractInsnNode>> iterator() {
         return new ElementNodeIterator(this.iterable.iterator(), this.filterDynamic);
      }
   }
}
