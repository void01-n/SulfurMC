package org.spongepowered.asm.transformers;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.ILegacyClassTransformer;

public abstract class TreeTransformer implements ILegacyClassTransformer {
   private ClassReader classReader;
   private ClassNode classNode;

   protected final ClassNode readClass(String className, byte[] basicClass) {
      return this.readClass(className, basicClass, true);
   }

   protected final ClassNode readClass(String className, byte[] basicClass, boolean cacheReader) {
      ClassReader classReader = new MixinClassReader(basicClass, className);
      if (cacheReader) {
         this.classReader = classReader;
      }

      ClassNode classNode = new ClassNode();
      classReader.accept(classNode, 8);
      return classNode;
   }

   protected final byte[] writeClass(ClassNode classNode) {
      if (this.classReader != null && this.classNode == classNode) {
         this.classNode = null;
         ClassWriter writer = new MixinClassWriter(this.classReader, 3);
         this.classReader = null;
         classNode.accept(writer);
         return writer.toByteArray();
      } else {
         this.classNode = null;
         ClassWriter writer = new MixinClassWriter(3);
         classNode.accept(writer);
         return writer.toByteArray();
      }
   }
}
