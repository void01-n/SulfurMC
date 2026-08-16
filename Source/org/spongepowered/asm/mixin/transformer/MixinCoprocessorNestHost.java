package org.spongepowered.asm.mixin.transformer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.throwables.MixinTransformerError;
import org.spongepowered.asm.util.asm.ClassNodeAdapter;

class MixinCoprocessorNestHost extends MixinCoprocessor {
   private final Map<String, Set<String>> nestHosts = new HashMap();

   void registerNestMember(String hostName, String memberName) {
      Set<String> nestMembers = (Set)this.nestHosts.get(hostName);
      if (nestMembers == null) {
         this.nestHosts.put(hostName, nestMembers = new HashSet());
      }

      nestMembers.add(memberName);
   }

   String getName() {
      return "nesthost";
   }

   boolean postProcess(String className, ClassNode classNode) {
      if (!this.nestHosts.containsKey(className)) {
         return false;
      } else {
         Set<String> newMembers = (Set)this.nestHosts.get(className);
         if (MixinEnvironment.getCompatibilityLevel().supports(8) && !newMembers.isEmpty()) {
            String nestHost = ClassNodeAdapter.getNestHostClass(classNode);
            if (nestHost != null) {
               throw new MixinTransformerError(String.format("Nest host candidate %s is a nest member", classNode.name));
            } else {
               List<String> nestMembers = ClassNodeAdapter.getNestMembers(classNode);
               if (nestMembers == null) {
                  nestMembers = new ArrayList(newMembers);
               } else {
                  LinkedHashSet<String> combinedMembers = new LinkedHashSet(nestMembers);
                  combinedMembers.addAll(newMembers);
                  nestMembers.clear();
                  nestMembers.addAll(combinedMembers);
               }

               ClassNodeAdapter.setNestMembers(classNode, nestMembers);
               return true;
            }
         } else {
            return false;
         }
      }
   }

   public boolean couldTransform(String className) {
      return this.nestHosts.containsKey(className);
   }
}
