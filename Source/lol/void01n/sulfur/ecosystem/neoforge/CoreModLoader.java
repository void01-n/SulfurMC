package lol.void01n.sulfur.ecosystem.neoforge;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lol.void01n.sulfur.transformengine.SulfurTransformationService;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public final class CoreModLoader {
   private static final boolean DEBUG = System.getProperties().containsKey("sulfur.debug");
   private final List<CoreModEntry> coremods = new ArrayList();

   public void registerCoremod(String name, List<String> targets, Consumer<ClassNode> fn) {
      this.coremods.add(new CoreModEntry(name, targets, fn));
      if (DEBUG) {
         System.out.println("sulfur/coremod: registered coremod '" + name + "' targeting " + targets.size() + " class(es)");
      }

   }

   public List<SulfurTransformationService.SulfurTransformer> asTransformers() {
      return this.coremods.stream().map((entry) -> new SulfurTransformationService.SulfurTransformer() {
            public boolean matches(String className) {
               return entry.targets.contains(className);
            }

            public byte[] transform(byte[] classBytes, String className) {
               ClassReader reader = new ClassReader(classBytes);
               ClassNode node = new ClassNode();
               reader.accept(node, 0);
               if (CoreModLoader.DEBUG) {
                  System.out.println("sulfur/coremod: '" + entry.name + "' transforming " + className);
               }

               entry.fn.accept(node);
               ClassWriter writer = new ClassWriter(1);
               node.accept(writer);
               return writer.toByteArray();
            }
         }).toList();
   }

   public boolean isEmpty() {
      return this.coremods.isEmpty();
   }

   private static record CoreModEntry(String name, List<String> targets, Consumer<ClassNode> fn) {
   }
}
