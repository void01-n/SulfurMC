package org.spongepowered.asm.util.asm;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.tree.ClassNode;

public final class ClassNodeAdapter {
   private static final Field fdNestHost = getField("nestHostClass");
   private static final Field fdNestMembers = getField("nestMembers");
   private static boolean notSupported = false;

   private ClassNodeAdapter() {
   }

   public static String getNestHostClass(ClassNode classNode) {
      if (ASM.isAtLeastVersion(7)) {
         return classNode.nestHostClass;
      } else if (fdNestHost != null && !notSupported) {
         try {
            return (String)fdNestHost.get(classNode);
         } catch (ReflectiveOperationException var2) {
            notSupported = true;
            return null;
         }
      } else {
         return null;
      }
   }

   public static void setNestHostClass(ClassNode classNode, String nestHostClass) {
      if (ASM.isAtLeastVersion(7)) {
         classNode.nestHostClass = nestHostClass;
      }

      if (fdNestHost != null && !notSupported) {
         try {
            fdNestHost.set(classNode, nestHostClass);
         } catch (ReflectiveOperationException var3) {
            notSupported = true;
         }

      }
   }

   public static List<String> getNestMembers(ClassNode classNode) {
      if (ASM.isAtLeastVersion(7)) {
         return classNode.nestMembers;
      } else if (fdNestMembers != null && !notSupported) {
         try {
            return (List)fdNestMembers.get(classNode);
         } catch (ReflectiveOperationException var2) {
            notSupported = true;
            return null;
         }
      } else {
         return null;
      }
   }

   public static List<String> getNestMembersAsList(ClassNode classNode) {
      List<String> nestMembers = getNestMembers(classNode);
      if (nestMembers == null) {
         nestMembers = new ArrayList();
         setNestMembers(classNode, nestMembers);
      }

      return nestMembers;
   }

   public static void setNestMembers(ClassNode classNode, List<String> nestMembers) {
      if (ASM.isAtLeastVersion(7)) {
         classNode.nestMembers = nestMembers;
      } else if (fdNestMembers != null && !notSupported) {
         try {
            fdNestMembers.set(classNode, nestMembers);
         } catch (ReflectiveOperationException var3) {
            notSupported = true;
         }

      }
   }

   private static Field getField(String fieldBaseName) {
      try {
         return ClassNode.class.getDeclaredField(fieldBaseName);
      } catch (NoSuchFieldException var4) {
         try {
            return ClassNode.class.getDeclaredField(fieldBaseName + "Experimental");
         } catch (NoSuchFieldException var3) {
            notSupported = true;
            return null;
         }
      }
   }
}
