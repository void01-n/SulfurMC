package org.spongepowered.asm.util;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.util.asm.ASM;

public class ClassSignature {
   protected static final String OBJECT = "java/lang/Object";
   private final Map<TypeVar, TokenHandle> types = new LinkedHashMap();
   private Token superClass = new Token("java/lang/Object");
   private final List<Token> interfaces = new ArrayList();
   private final Deque<String> rawInterfaces = new LinkedList();

   ClassSignature() {
   }

   private ClassSignature read(String signature) {
      if (signature != null) {
         try {
            (new SignatureReader(signature)).accept(new SignatureParser());
         } catch (Exception ex) {
            ex.printStackTrace();
         }
      }

      return this;
   }

   protected TypeVar getTypeVar(String varName) {
      for(TypeVar typeVar : this.types.keySet()) {
         if (typeVar.matches(varName)) {
            return typeVar;
         }
      }

      return null;
   }

   protected TokenHandle getType(String varName) {
      for(TypeVar typeVar : this.types.keySet()) {
         if (typeVar.matches(varName)) {
            return (TokenHandle)this.types.get(typeVar);
         }
      }

      TokenHandle handle = new TokenHandle();
      this.types.put(new TypeVar(varName), handle);
      return handle;
   }

   protected String getTypeVar(TokenHandle handle) {
      for(Map.Entry<TypeVar, TokenHandle> type : this.types.entrySet()) {
         TypeVar typeVar = (TypeVar)type.getKey();
         TokenHandle typeHandle = (TokenHandle)type.getValue();
         if (handle == typeHandle || handle.asToken() == typeHandle.asToken()) {
            return "T" + typeVar + ";";
         }
      }

      return handle.token.asType();
   }

   protected void addTypeVar(TypeVar typeVar, TokenHandle handle) throws IllegalArgumentException {
      if (this.types.containsKey(typeVar)) {
         throw new IllegalArgumentException("TypeVar " + typeVar + " is already present on " + this);
      } else {
         this.types.put(typeVar, handle);
      }
   }

   protected void setSuperClass(Token superClass) {
      this.superClass = superClass;
   }

   public String getSuperClass() {
      return this.superClass.asType(true);
   }

   protected void addInterface(Token iface) {
      if (!iface.isRaw()) {
         String raw = iface.asType(true);
         ListIterator<Token> iter = this.interfaces.listIterator();

         while(iter.hasNext()) {
            Token intrface = (Token)iter.next();
            if (intrface.isRaw() && intrface.asType(true).equals(raw)) {
               iter.set(iface);
               return;
            }
         }
      }

      this.interfaces.add(iface);
   }

   public void addInterface(String iface) {
      this.rawInterfaces.add(iface);
   }

   protected void addRawInterface(String iface) {
      Token token = new Token(iface);
      String raw = token.asType(true);

      for(Token intrface : this.interfaces) {
         if (intrface.asType(true).equals(raw)) {
            return;
         }
      }

      this.interfaces.add(token);
   }

   public void merge(ClassSignature other) {
      try {
         Set<String> typeVars = new HashSet();

         for(TypeVar typeVar : this.types.keySet()) {
            typeVars.add(typeVar.toString());
         }

         other.conform(typeVars);
      } catch (IllegalStateException ex) {
         ex.printStackTrace();
         return;
      }

      for(Map.Entry<TypeVar, TokenHandle> type : other.types.entrySet()) {
         this.addTypeVar((TypeVar)type.getKey(), (TokenHandle)type.getValue());
      }

      for(Token iface : other.interfaces) {
         this.addInterface(iface);
      }

   }

   private void conform(Set<String> typeVars) {
      for(TypeVar typeVar : this.types.keySet()) {
         String name = this.findUniqueName(typeVar.getOriginalName(), typeVars);
         typeVar.rename(name);
         typeVars.add(name);
      }

   }

   private String findUniqueName(String typeVar, Set<String> typeVars) {
      if (!typeVars.contains(typeVar)) {
         return typeVar;
      } else {
         if (typeVar.length() == 1) {
            String name = this.findOffsetName(typeVar.charAt(0), typeVars);
            if (name != null) {
               return name;
            }
         }

         String name = this.findOffsetName('T', typeVars, "", typeVar);
         if (name != null) {
            return name;
         } else {
            name = this.findOffsetName('T', typeVars, typeVar, "");
            if (name != null) {
               return name;
            } else {
               name = this.findOffsetName('T', typeVars, "T", typeVar);
               if (name != null) {
                  return name;
               } else {
                  name = this.findOffsetName('T', typeVars, "", typeVar + "Type");
                  if (name != null) {
                     return name;
                  } else {
                     throw new IllegalStateException("Failed to conform type var: " + typeVar);
                  }
               }
            }
         }
      }
   }

   private String findOffsetName(char c, Set<String> typeVars) {
      return this.findOffsetName(c, typeVars, "", "");
   }

   private String findOffsetName(char c, Set<String> typeVars, String prefix, String suffix) {
      String name = String.format("%s%s%s", prefix, c, suffix);
      if (!typeVars.contains(name)) {
         return name;
      } else {
         if (c > '@' && c < '[') {
            for(int s = c - 64; s + 65 != c; s %= 26) {
               name = String.format("%s%s%s", prefix, (char)(s + 65), suffix);
               if (!typeVars.contains(name)) {
                  return name;
               }

               ++s;
            }
         }

         return null;
      }
   }

   public SignatureVisitor getRemapper() {
      return new SignatureRemapper();
   }

   public String toString() {
      while(this.rawInterfaces.size() > 0) {
         this.addRawInterface((String)this.rawInterfaces.remove());
      }

      StringBuilder sb = new StringBuilder();
      if (this.types.size() > 0) {
         boolean valid = false;
         StringBuilder types = new StringBuilder();

         for(Map.Entry<TypeVar, TokenHandle> type : this.types.entrySet()) {
            String bound = ((TokenHandle)type.getValue()).asBound();
            if (!bound.isEmpty()) {
               types.append(type.getKey()).append(':').append(bound);
               valid = true;
            }
         }

         if (valid) {
            sb.append('<').append(types).append('>');
         }
      }

      sb.append(this.superClass.asType());

      for(Token iface : this.interfaces) {
         sb.append(iface.asType());
      }

      return sb.toString();
   }

   public ClassSignature wake() {
      return this;
   }

   public static ClassSignature of(String signature) {
      return (new ClassSignature()).read(signature);
   }

   public static ClassSignature of(ClassNode classNode) {
      return classNode.signature != null ? of(classNode.signature) : generate(classNode);
   }

   public static ClassSignature ofLazy(ClassNode classNode) {
      return (ClassSignature)(classNode.signature != null ? new Lazy(classNode.signature) : generate(classNode));
   }

   private static ClassSignature generate(ClassNode classNode) {
      ClassSignature generated = new ClassSignature();
      generated.setSuperClass(new Token(classNode.superName != null ? classNode.superName : "java/lang/Object"));

      for(String iface : classNode.interfaces) {
         generated.addInterface(new Token(iface));
      }

      return generated;
   }

   static class Lazy extends ClassSignature {
      private final String sig;
      private ClassSignature generated;

      Lazy(String sig) {
         this.sig = sig;
      }

      public ClassSignature wake() {
         if (this.generated == null) {
            this.generated = ClassSignature.of(this.sig);
         }

         return this.generated;
      }
   }

   static class TypeVar implements Comparable<TypeVar> {
      private final String originalName;
      private String currentName;

      TypeVar(String name) {
         this.currentName = this.originalName = name;
      }

      public int compareTo(TypeVar other) {
         return this.currentName.compareTo(other.currentName);
      }

      public String toString() {
         return this.currentName;
      }

      String getOriginalName() {
         return this.originalName;
      }

      void rename(String name) {
         this.currentName = name;
      }

      public boolean matches(String originalName) {
         return this.originalName.equals(originalName);
      }

      public boolean equals(Object obj) {
         return this.currentName.equals(obj);
      }

      public int hashCode() {
         return this.currentName.hashCode();
      }
   }

   static class Token implements IToken {
      private final boolean inner;
      private boolean array;
      private char symbol;
      private String type;
      private List<Token> classBound;
      private List<Token> ifaceBound;
      private List<IToken> signature;
      private List<IToken> suffix;
      private Token tail;

      Token() {
         this(false);
      }

      Token(String type) {
         this(type, false);
      }

      Token(char symbol) {
         this();
         this.symbol = symbol;
      }

      Token(boolean inner) {
         this((String)null, inner);
      }

      Token(String type, boolean inner) {
         this.symbol = 0;
         this.inner = inner;
         this.type = type;
      }

      Token setSymbol(char symbol) {
         if (this.symbol == 0 && "+-*".indexOf(symbol) > -1) {
            this.symbol = symbol;
         }

         return this;
      }

      Token setType(String type) {
         if (this.type == null) {
            this.type = type;
         }

         return this;
      }

      public IToken setArray(boolean array) {
         this.array |= array;
         return this;
      }

      public IToken setWildcard(char wildcard) {
         return "+-".indexOf(wildcard) == -1 ? this : this.setSymbol(wildcard);
      }

      private List<Token> getClassBound() {
         if (this.classBound == null) {
            this.classBound = new ArrayList();
         }

         return this.classBound;
      }

      private List<Token> getIfaceBound() {
         if (this.ifaceBound == null) {
            this.ifaceBound = new ArrayList();
         }

         return this.ifaceBound;
      }

      private List<IToken> getSignature() {
         if (this.signature == null) {
            this.signature = new ArrayList();
         }

         return this.signature;
      }

      private List<IToken> getSuffix() {
         if (this.suffix == null) {
            this.suffix = new ArrayList();
         }

         return this.suffix;
      }

      IToken addTypeArgument(char symbol) {
         if (this.tail != null) {
            return this.tail.addTypeArgument(symbol);
         } else {
            Token token = new Token(symbol);
            this.getSignature().add(token);
            return token;
         }
      }

      IToken addTypeArgument(String name) {
         if (this.tail != null) {
            return this.tail.addTypeArgument(name);
         } else {
            Token token = new Token(name);
            this.getSignature().add(token);
            return token;
         }
      }

      IToken addTypeArgument(TokenHandle token) {
         if (this.tail != null) {
            return this.tail.addTypeArgument(token);
         } else {
            TokenHandle handle = token.clone();
            this.getSignature().add(handle);
            return handle;
         }
      }

      Token addBound(String bound, boolean classBound) {
         return classBound ? this.addClassBound(bound) : this.addInterfaceBound(bound);
      }

      Token addClassBound(String bound) {
         Token token = new Token(bound);
         this.getClassBound().add(token);
         return token;
      }

      Token addInterfaceBound(String bound) {
         Token token = new Token(bound);
         this.getIfaceBound().add(token);
         return token;
      }

      Token addInnerClass(String name) {
         this.tail = new Token(name, true);
         this.getSuffix().add(this.tail);
         return this.tail;
      }

      public String toString() {
         return this.asType();
      }

      public String asBound() {
         StringBuilder sb = new StringBuilder();
         if (this.type != null) {
            sb.append(this.type);
         }

         if (this.classBound != null) {
            for(Token token : this.classBound) {
               sb.append(token.asType());
            }
         }

         if (this.ifaceBound != null) {
            for(Token token : this.ifaceBound) {
               sb.append(':').append(token.asType());
            }
         }

         return sb.toString();
      }

      public String asType() {
         return this.asType(false);
      }

      public String asType(boolean raw) {
         StringBuilder sb = new StringBuilder();
         if (this.array) {
            sb.append('[');
         }

         if (this.symbol != 0) {
            sb.append(this.symbol);
         }

         if (this.type == null) {
            return sb.toString();
         } else {
            if (!this.inner) {
               sb.append('L');
            }

            sb.append(this.type);
            if (!raw) {
               if (this.signature != null) {
                  sb.append('<');

                  for(IToken token : this.signature) {
                     sb.append(token.asType());
                  }

                  sb.append('>');
               }

               if (this.suffix != null) {
                  for(IToken token : this.suffix) {
                     sb.append('.').append(token.asType());
                  }
               }
            }

            if (!this.inner) {
               sb.append(';');
            }

            return sb.toString();
         }
      }

      boolean isRaw() {
         return this.signature == null;
      }

      public Token asToken() {
         return this;
      }
   }

   class TokenHandle implements IToken {
      final Token token;
      boolean array;
      char wildcard;

      TokenHandle() {
         this(new Token());
      }

      TokenHandle(Token token) {
         this.token = token;
      }

      public IToken setArray(boolean array) {
         this.array |= array;
         return this;
      }

      public IToken setWildcard(char wildcard) {
         if ("+-".indexOf(wildcard) > -1) {
            this.wildcard = wildcard;
         }

         return this;
      }

      public String asBound() {
         return this.token.asBound();
      }

      public String asType() {
         StringBuilder sb = new StringBuilder();
         if (this.wildcard > 0) {
            sb.append(this.wildcard);
         }

         if (this.array) {
            sb.append('[');
         }

         return sb.append(ClassSignature.this.getTypeVar(this)).toString();
      }

      public Token asToken() {
         return this.token;
      }

      public String toString() {
         return this.token.toString();
      }

      public TokenHandle clone() {
         return ClassSignature.this.new TokenHandle(this.token);
      }
   }

   class SignatureParser extends SignatureVisitor {
      private FormalParamElement param;

      SignatureParser() {
         super(ASM.API_VERSION);
      }

      public void visitFormalTypeParameter(String name) {
         this.param = new FormalParamElement(name);
      }

      public SignatureVisitor visitClassBound() {
         return this.param.visitClassBound();
      }

      public SignatureVisitor visitInterfaceBound() {
         return this.param.visitInterfaceBound();
      }

      public SignatureVisitor visitSuperclass() {
         return new SuperClassElement();
      }

      public SignatureVisitor visitInterface() {
         return new InterfaceElement();
      }

      abstract class SignatureElement extends SignatureVisitor {
         public SignatureElement() {
            super(ASM.API_VERSION);
         }
      }

      abstract class TokenElement extends SignatureElement {
         protected Token token;
         private boolean array;

         public Token getToken() {
            if (this.token == null) {
               this.token = new Token();
            }

            return this.token;
         }

         protected void setArray() {
            this.array = true;
         }

         private boolean getArray() {
            boolean array = this.array;
            this.array = false;
            return array;
         }

         public void visitClassType(String name) {
            this.getToken().setType(name);
         }

         public SignatureVisitor visitClassBound() {
            this.getToken();
            return SignatureParser.this.new BoundElement(this, true);
         }

         public SignatureVisitor visitInterfaceBound() {
            this.getToken();
            return SignatureParser.this.new BoundElement(this, false);
         }

         public void visitInnerClassType(String name) {
            this.token.addInnerClass(name);
         }

         public SignatureVisitor visitArrayType() {
            this.setArray();
            return this;
         }

         public SignatureVisitor visitTypeArgument(char wildcard) {
            return SignatureParser.this.new TypeArgElement(this, wildcard);
         }

         IToken addTypeArgument(char symbol) {
            return this.token.addTypeArgument(symbol).setArray(this.getArray());
         }

         IToken addTypeArgument(String name) {
            return this.token.addTypeArgument(name).setArray(this.getArray());
         }

         IToken addTypeArgument(TokenHandle token) {
            return this.token.addTypeArgument(token).setArray(this.getArray());
         }
      }

      class FormalParamElement extends TokenElement {
         private final TokenHandle handle;

         FormalParamElement(String param) {
            this.handle = ClassSignature.this.getType(param);
            this.token = this.handle.asToken();
         }
      }

      class TypeArgElement extends TokenElement {
         private final TokenElement type;
         private final char wildcard;

         TypeArgElement(TokenElement type, char wildcard) {
            this.type = type;
            this.wildcard = wildcard;
         }

         public SignatureVisitor visitArrayType() {
            this.type.setArray();
            return this;
         }

         public void visitBaseType(char descriptor) {
            this.token = this.type.addTypeArgument(descriptor).asToken();
         }

         public void visitTypeVariable(String name) {
            TokenHandle token = ClassSignature.this.getType(name);
            this.token = this.type.addTypeArgument(token).setWildcard(this.wildcard).asToken();
         }

         public void visitClassType(String name) {
            this.token = this.type.addTypeArgument(name).setWildcard(this.wildcard).asToken();
         }

         public void visitTypeArgument() {
            this.token.addTypeArgument('*');
         }

         public SignatureVisitor visitTypeArgument(char wildcard) {
            return SignatureParser.this.new TypeArgElement(this, wildcard);
         }

         public void visitEnd() {
         }
      }

      class BoundElement extends TokenElement {
         private final TokenElement type;
         private final boolean classBound;

         BoundElement(TokenElement type, boolean classBound) {
            this.type = type;
            this.classBound = classBound;
         }

         public void visitClassType(String name) {
            this.token = this.type.token.addBound(name, this.classBound);
         }

         public void visitTypeArgument() {
            this.token.addTypeArgument('*');
         }

         public SignatureVisitor visitTypeArgument(char wildcard) {
            return SignatureParser.this.new TypeArgElement(this, wildcard);
         }
      }

      class SuperClassElement extends TokenElement {
         public void visitEnd() {
            ClassSignature.this.setSuperClass(this.token);
         }
      }

      class InterfaceElement extends TokenElement {
         public void visitEnd() {
            ClassSignature.this.addInterface(this.token);
         }
      }
   }

   class SignatureRemapper extends SignatureWriter {
      private final Set<String> localTypeVars = new HashSet();

      public void visitFormalTypeParameter(String name) {
         this.localTypeVars.add(name);
         super.visitFormalTypeParameter(name);
      }

      public void visitTypeVariable(String name) {
         if (!this.localTypeVars.contains(name)) {
            TypeVar typeVar = ClassSignature.this.getTypeVar(name);
            if (typeVar != null) {
               super.visitTypeVariable(typeVar.toString());
               return;
            }
         }

         super.visitTypeVariable(name);
      }
   }

   interface IToken {
      String asType();

      Token asToken();

      IToken setArray(boolean var1);

      IToken setWildcard(char var1);
   }
}
