package org.spongepowered.asm.mixin.injection.struct;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.IInjectionPointContext;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.modify.LocalVariableDiscriminator;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.InvalidSelectorException;
import org.spongepowered.asm.mixin.injection.selectors.TargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.dynamic.DynamicSelectorDesc;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionPointException;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.IMessageSink;
import org.spongepowered.asm.util.asm.IAnnotationHandle;
import org.spongepowered.include.com.google.common.base.Joiner;
import org.spongepowered.include.com.google.common.base.Strings;
import org.spongepowered.include.com.google.common.primitives.Ints;

public class InjectionPointData {
   private static final Pattern AT_PATTERN = createPattern();
   private final Map<String, String> args = new HashMap();
   private final IInjectionPointContext context;
   private final String at;
   private final String type;
   private final InjectionPoint.Specifier specifier;
   private final InjectionPoint.RestrictTargetLevel targetRestriction;
   private final String target;
   private final String slice;
   private final int ordinal;
   private final int opcode;
   private final String id;
   private final int flags;

   public InjectionPointData(IInjectionPointContext context, String at, List<String> args, String target, String slice, int ordinal, int opcode, String id, int flags) {
      this.context = context;
      this.at = at;
      this.target = target;
      this.slice = Strings.nullToEmpty(slice);
      this.ordinal = Math.max(-1, ordinal);
      this.opcode = opcode;
      this.id = id;
      this.flags = flags;
      this.parseArgs(args);
      this.args.put("target", target);
      this.args.put("ordinal", String.valueOf(ordinal));
      this.args.put("opcode", String.valueOf(opcode));
      Matcher matcher = AT_PATTERN.matcher(at);
      this.type = parseType(matcher, at);
      this.specifier = parseSpecifier(matcher);
      this.targetRestriction = this.isUnsafe() ? InjectionPoint.RestrictTargetLevel.ALLOW_ALL : InjectionPoint.RestrictTargetLevel.METHODS_ONLY;
   }

   private void parseArgs(List<String> args) {
      if (args != null) {
         for(String arg : args) {
            if (arg != null) {
               int eqPos = arg.indexOf(61);
               if (eqPos > -1) {
                  this.args.put(arg.substring(0, eqPos), arg.substring(eqPos + 1));
               } else {
                  this.args.put(arg, "");
               }
            }
         }

      }
   }

   public IMessageSink getMessageSink() {
      return this.context;
   }

   public String getAt() {
      return this.at;
   }

   public String getType() {
      return this.type;
   }

   public InjectionPoint.Specifier getSpecifier() {
      return this.specifier;
   }

   public InjectionPoint.RestrictTargetLevel getTargetRestriction() {
      return this.targetRestriction;
   }

   public IInjectionPointContext getContext() {
      return this.context;
   }

   public IMixinContext getMixin() {
      return this.context.getMixin();
   }

   public MethodNode getMethod() {
      return this.context.getMethod();
   }

   public Type getMethodReturnType() {
      return Type.getReturnType(this.getMethod().desc);
   }

   public AnnotationNode getParent() {
      return this.context.getAnnotationNode();
   }

   public String getSlice() {
      return this.slice;
   }

   public LocalVariableDiscriminator getLocalVariableDiscriminator() {
      return LocalVariableDiscriminator.parse(this.getParent());
   }

   public String get(String key, String defaultValue) {
      String value = (String)this.args.get(key);
      return value != null ? value : defaultValue;
   }

   public int get(String key, int defaultValue) {
      return parseInt(this.get(key, String.valueOf(defaultValue)), defaultValue);
   }

   public boolean get(String key, boolean defaultValue) {
      return parseBoolean(this.get(key, String.valueOf(defaultValue)), defaultValue);
   }

   public <T extends Enum<T>> T get(String key, T defaultValue) {
      return (T)parseEnum(this.get(key, defaultValue.name()), defaultValue);
   }

   public ITargetSelector get(String key) {
      try {
         return TargetSelector.parseAndValidate((String)this.get(key, ""), this.context);
      } catch (InvalidSelectorException ex) {
         throw new InvalidInjectionPointException(this.getMixin(), ex, "Failed parsing @At(\"%s\").%s \"%s\" on %s", new Object[]{this.at, key, this.target, this.getDescription()});
      }
   }

   public ITargetSelector getTarget() {
      try {
         if (Strings.isNullOrEmpty(this.target)) {
            IAnnotationHandle selectorAnnotation = this.context.getSelectorAnnotation();
            AnnotationNode desc = (AnnotationNode)Annotations.getValue(((Annotations.Handle)selectorAnnotation).getNode(), "desc");
            if (desc != null) {
               String id = (String)Annotations.getValue(desc, "id", "at");
               if ("at".equalsIgnoreCase(id)) {
                  return DynamicSelectorDesc.of(Annotations.handleOf(desc), this.context);
               }
            }
         }

         return TargetSelector.parseAndValidate((String)this.target, this.context);
      } catch (InvalidSelectorException ex) {
         throw new InvalidInjectionPointException(this.getMixin(), ex, "Failed validating @At(\"%s\").target \"%s\" on %s", new Object[]{this.at, this.target, this.getDescription()});
      }
   }

   public String getDescription() {
      return InjectionInfo.describeInjector(this.context.getMixin(), this.context.getAnnotationNode(), this.context.getMethod());
   }

   public int getOrdinal() {
      return this.ordinal;
   }

   public int getOpcode() {
      return this.opcode;
   }

   public int getOpcode(int defaultOpcode) {
      return this.opcode > 0 ? this.opcode : defaultOpcode;
   }

   public int getOpcode(int defaultOpcode, int... validOpcodes) {
      for(int validOpcode : validOpcodes) {
         if (this.opcode == validOpcode) {
            return this.opcode;
         }
      }

      return defaultOpcode;
   }

   public int[] getOpcodeList(String key, int[] defaultValue) {
      String value = (String)this.args.get(key);
      if (value == null) {
         return defaultValue;
      } else {
         Set<Integer> parsed = new TreeSet();
         String[] values = value.split("[ ,;]");

         for(String strOpcode : values) {
            int opcode = Bytecode.parseOpcodeName(strOpcode.trim());
            if (opcode > 0) {
               parsed.add(opcode);
            }
         }

         return Ints.toArray(parsed);
      }
   }

   public String getId() {
      return this.id;
   }

   public boolean isUnsafe() {
      return (this.flags & 1) != 0;
   }

   public String toString() {
      return this.type;
   }

   private static Pattern createPattern() {
      return Pattern.compile(String.format("^(.+?)(:(%s))?$", Joiner.on('|').join(InjectionPoint.Specifier.values())));
   }

   public static String parseType(String at) {
      Matcher matcher = AT_PATTERN.matcher(at);
      return parseType(matcher, at);
   }

   private static String parseType(Matcher matcher, String at) {
      return matcher.matches() ? matcher.group(1) : at;
   }

   private static InjectionPoint.Specifier parseSpecifier(Matcher matcher) {
      return matcher.matches() && matcher.group(3) != null ? InjectionPoint.Specifier.valueOf(matcher.group(3)) : InjectionPoint.Specifier.DEFAULT;
   }

   private static int parseInt(String string, int defaultValue) {
      try {
         return Integer.parseInt(string);
      } catch (Exception var3) {
         return defaultValue;
      }
   }

   private static boolean parseBoolean(String string, boolean defaultValue) {
      try {
         return Boolean.parseBoolean(string);
      } catch (Exception var3) {
         return defaultValue;
      }
   }

   private static <T extends Enum<T>> T parseEnum(String string, T defaultValue) {
      try {
         return (T)Enum.valueOf(defaultValue.getClass(), string);
      } catch (Exception var3) {
         return defaultValue;
      }
   }
}
