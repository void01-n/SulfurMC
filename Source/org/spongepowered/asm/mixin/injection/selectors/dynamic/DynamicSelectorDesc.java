package org.spongepowered.asm.mixin.injection.selectors.dynamic;

import java.util.List;
import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.selectors.ElementNode;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorByName;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorDynamic;
import org.spongepowered.asm.mixin.injection.selectors.InvalidSelectorException;
import org.spongepowered.asm.mixin.injection.selectors.MatchResult;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.Quantifier;
import org.spongepowered.asm.util.SignaturePrinter;
import org.spongepowered.asm.util.asm.IAnnotationHandle;
import org.spongepowered.include.com.google.common.base.Strings;

@ITargetSelectorDynamic.SelectorId("Desc")
@ITargetSelectorDynamic.SelectorAnnotation(Desc.class)
public class DynamicSelectorDesc implements ITargetSelectorByName, ITargetSelectorDynamic {
   private final InvalidSelectorException parseException;
   private final String id;
   private final Type owner;
   private final String name;
   private final Type[] args;
   private final Type returnType;
   private final String methodDesc;
   private final Quantifier matches;
   private final List<IAnnotationHandle> next;
   private final boolean disabled;

   private DynamicSelectorDesc(IResolvedDescriptor desc) {
      this((InvalidSelectorException)null, desc.getId(), desc.getOwner(), desc.getName(), desc.getArgs(), desc.getReturnType(), desc.getMatches(), desc.getNext(), desc.isDebug());
   }

   private DynamicSelectorDesc(DynamicSelectorDesc desc, Quantifier quantifier) {
      this(desc.parseException, desc.id, desc.owner, desc.name, desc.args, desc.returnType, quantifier, desc.next, desc.disabled);
   }

   private DynamicSelectorDesc(DynamicSelectorDesc desc, Type owner) {
      this(desc.parseException, desc.id, owner, desc.name, desc.args, desc.returnType, desc.matches, desc.next, desc.disabled);
   }

   private DynamicSelectorDesc(InvalidSelectorException ex) {
      this(ex, (String)null, (Type)null, (String)null, (Type[])null, (Type)null, Quantifier.NONE, (List)null, true);
   }

   protected DynamicSelectorDesc(InvalidSelectorException ex, String id, Type owner, String name, Type[] args, Type returnType, Quantifier matches, List<IAnnotationHandle> next, boolean disabled) {
      this.parseException = ex;
      this.id = id;
      this.owner = owner;
      this.name = Strings.emptyToNull(name);
      this.args = args;
      this.returnType = returnType;
      this.methodDesc = returnType != null ? Bytecode.getDescriptor(returnType, args) : null;
      this.matches = matches;
      this.next = next;
      this.disabled = disabled;
   }

   public static DynamicSelectorDesc parse(String input, ISelectorContext context) {
      IResolvedDescriptor descriptor = DescriptorResolver.resolve(input, context);
      if (!descriptor.isResolved() && !descriptor.isDebug()) {
         String extra = input.length() == 0 ? ". " + descriptor.getResolutionInfo() : "";
         return new DynamicSelectorDesc(new InvalidSelectorException("Could not resolve @Desc(" + input + ") for " + context + extra));
      } else {
         return of(descriptor);
      }
   }

   public static DynamicSelectorDesc parse(IAnnotationHandle desc, ISelectorContext context) {
      IResolvedDescriptor descriptor = DescriptorResolver.resolve(desc, context);
      return !descriptor.isResolved() && !descriptor.isDebug() ? new DynamicSelectorDesc(new InvalidSelectorException("Invalid descriptor")) : of(descriptor);
   }

   public static DynamicSelectorDesc resolve(ISelectorContext context) {
      IResolvedDescriptor descriptor = DescriptorResolver.resolve("", context);
      return !descriptor.isResolved() ? null : of(descriptor);
   }

   public static DynamicSelectorDesc of(IAnnotationHandle desc, ISelectorContext context) {
      IResolvedDescriptor descriptor = DescriptorResolver.resolve(desc, context);
      return !descriptor.isResolved() ? null : of(descriptor);
   }

   public static DynamicSelectorDesc of(IResolvedDescriptor desc) {
      return new DynamicSelectorDesc(desc);
   }

   public String toString() {
      StringBuilder sb = new StringBuilder("@Desc(");
      boolean started = false;
      if (!Strings.isNullOrEmpty(this.id)) {
         sb.append("id = \"").append(this.id).append("\"");
         started = true;
      }

      if (this.owner != Type.VOID_TYPE) {
         if (started) {
            sb.append(", ");
         }

         sb.append("owner = ").append(SignaturePrinter.getTypeName(this.owner, false, false)).append(".class");
         started = true;
      }

      if (started) {
         sb.append(", ");
      }

      if (this.name != null) {
         sb.append("value = \"").append(this.name).append("\"");
      }

      if (this.args.length > 0) {
         sb.append(", args = { ");

         for(int i = 0; i < this.args.length; ++i) {
            if (i > 0) {
               sb.append(", ");
            }

            sb.append(SignaturePrinter.getTypeName(this.args[i], false, false)).append(".class");
         }

         sb.append(" }");
      }

      if (this.returnType != Type.VOID_TYPE) {
         sb.append(", ret = ").append(SignaturePrinter.getTypeName(this.returnType, false, false)).append(".class");
      }

      sb.append(")");
      return sb.toString();
   }

   public String getId() {
      return this.id;
   }

   public String getOwner() {
      return this.owner.getInternalName();
   }

   public String getName() {
      return this.name;
   }

   public Type[] getArgs() {
      return this.args;
   }

   public Type getReturnType() {
      return this.returnType;
   }

   public String getDesc() {
      return this.methodDesc;
   }

   public String toDescriptor() {
      return (new SignaturePrinter(this)).setFullyQualified(true).toDescriptor();
   }

   public ITargetSelector validate() throws InvalidSelectorException {
      if (this.parseException != null) {
         throw this.parseException;
      } else {
         return this;
      }
   }

   public ITargetSelector next() {
      return this.next(0);
   }

   protected ITargetSelector next(int index) {
      if (index >= 0 && index < this.next.size()) {
         IAnnotationHandle nextAnnotation = (IAnnotationHandle)this.next.get(index);
         IResolvedDescriptor descriptor = DescriptorResolver.resolve((IAnnotationHandle)nextAnnotation, (ISelectorContext)null);
         return new Next(index, descriptor);
      } else {
         return null;
      }
   }

   public ITargetSelector configure(ITargetSelector.Configure request, String... args) {
      request.checkArgs(args);
      switch (request) {
         case SELECT_MEMBER:
            if (this.matches.isDefault()) {
               return new DynamicSelectorDesc(this, Quantifier.SINGLE);
            }
            break;
         case SELECT_INSTRUCTION:
            if (this.matches.isDefault()) {
               return new DynamicSelectorDesc(this, Quantifier.ANY);
            }
            break;
         case MOVE:
            return new DynamicSelectorDesc(this, Type.getObjectType(args[0]));
         case CLEAR_LIMITS:
            if (this.getMinMatchCount() != 0 || this.getMaxMatchCount() < Integer.MAX_VALUE) {
               return new DynamicSelectorDesc(this, Quantifier.ANY);
            }
      }

      return this;
   }

   public ITargetSelector attach(ISelectorContext context) throws InvalidSelectorException {
      return this;
   }

   public int getMinMatchCount() {
      return this.matches.getClampedMin();
   }

   public int getMaxMatchCount() {
      return this.matches.getClampedMax();
   }

   public MatchResult matches(String owner, String name, String desc) {
      return this.matches(owner, name, desc, this.methodDesc);
   }

   public <TNode> MatchResult match(ElementNode<TNode> node) {
      if (node != null && !this.disabled) {
         return node.isField() ? this.matches(node.getOwner(), node.getName(), node.getDesc(), this.returnType.getDescriptor()) : this.matches(node.getOwner(), node.getName(), node.getDesc(), this.methodDesc);
      } else {
         return MatchResult.NONE;
      }
   }

   private MatchResult matches(String owner, String name, String desc, String compareWithDesc) {
      if (!compareWithDesc.equals(desc)) {
         return MatchResult.NONE;
      } else if (this.owner != Type.VOID_TYPE && !this.owner.getInternalName().equals(owner)) {
         return MatchResult.NONE;
      } else if (this.name != null && this.name.equals(name)) {
         return MatchResult.EXACT_MATCH;
      } else if (this.name != null && this.name.equalsIgnoreCase(name)) {
         return MatchResult.MATCH;
      } else {
         return this.name == null ? MatchResult.EXACT_MATCH : MatchResult.NONE;
      }
   }

   final class Next extends DynamicSelectorDesc {
      private final int index;

      Next(int index, IResolvedDescriptor next) {
         super((InvalidSelectorException)null, (String)null, next.getOwner(), next.getName(), next.getArgs(), next.getReturnType(), next.getMatches(), (List)null, next.isDebug());
         this.index = index;
      }

      public ITargetSelector next() {
         return DynamicSelectorDesc.this.next(this.index + 1);
      }
   }
}
