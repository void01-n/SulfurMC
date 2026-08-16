package org.spongepowered.asm.mixin.transformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.util.Bytecode;

public enum MixinInheritanceTracker implements MixinConfig.IListener {
   INSTANCE;

   private final Map<String, List<MixinInfo>> parentMixins = new HashMap();

   public void onPrepare(MixinInfo mixin) {
   }

   public void onInit(MixinInfo mixin) {
      ClassInfo mixinInfo = mixin.getClassInfo();

      assert mixinInfo.isMixin();

      for(ClassInfo superType = mixinInfo.getSuperClass(); superType != null && superType.isMixin(); superType = superType.getSuperClass()) {
         List<MixinInfo> children = (List)this.parentMixins.get(superType.getName());
         if (children == null) {
            this.parentMixins.put(superType.getName(), children = new ArrayList());
         }

         children.add(mixin);
      }

   }

   public List<MethodNode> findOverrides(ClassInfo owner, String name, String desc) {
      return this.findOverrides(owner.getName(), name, desc);
   }

   public List<MethodNode> findOverrides(String owner, String name, String desc) {
      List<MixinInfo> children = (List)this.parentMixins.get(owner);
      if (children == null) {
         return Collections.emptyList();
      } else {
         List<MethodNode> out = new ArrayList(children.size());

         for(MixinInfo child : children) {
            ClassNode node = child.getClassNode(6);
            MethodNode method = Bytecode.findMethod(node, name, desc);
            if (method != null && !Bytecode.isStatic(method)) {
               switch (Bytecode.getVisibility(method)) {
                  case PRIVATE:
                     break;
                  case PACKAGE:
                     int ownerSplit = owner.lastIndexOf(47);
                     int childSplit = node.name.lastIndexOf(47);
                     if (ownerSplit == childSplit && (ownerSplit <= 0 || owner.regionMatches(0, node.name, 0, ownerSplit + 1))) {
                        out.add(method);
                     }
                     break;
                  default:
                     out.add(method);
               }
            }
         }

         return out.isEmpty() ? Collections.emptyList() : out;
      }
   }

   // $FF: synthetic method
   private static MixinInheritanceTracker[] $values() {
      return new MixinInheritanceTracker[]{INSTANCE};
   }
}
