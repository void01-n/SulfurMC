package lol.void01n.sulfur.ecosystem.neoforge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public final class AccessTransformerApplier {
   private static final boolean DEBUG = System.getProperties().containsKey("sulfur.debug");
   private final Map<String, Set<AtEntry>> entries = new HashMap();

   public void loadAtFile(InputStream source, String sourceName) throws IOException {
      BufferedReader reader = new BufferedReader(new InputStreamReader(source, StandardCharsets.UTF_8));

      try {
         int lineNum = 0;

         String line;
         while((line = reader.readLine()) != null) {
            ++lineNum;
            int hash = line.indexOf(35);
            if (hash >= 0) {
               line = line.substring(0, hash);
            }

            line = line.strip();
            if (!line.isEmpty()) {
               String[] tokens = line.split("\\s+", 3);
               if (tokens.length < 2) {
                  if (DEBUG) {
                     System.out.println("sulfur/at: " + sourceName + ":" + lineNum + " — skipping malformed line: " + line);
                  }
               } else {
                  String accessToken = tokens[0];
                  String binaryClass = tokens[1].replace('.', '/');
                  String memberDescriptor = tokens.length >= 3 ? tokens[2].strip() : null;
                  boolean clearFinal = accessToken.endsWith("-f");
                  int targetAccess;
                  switch (clearFinal ? accessToken.substring(0, accessToken.length() - 2) : accessToken) {
                     case "public":
                        targetAccess = 1;
                        break;
                     case "protected":
                        targetAccess = 4;
                        break;
                     case "private":
                        targetAccess = 2;
                        break;
                     case "default":
                        targetAccess = 0;
                        break;
                     default:
                        if (DEBUG) {
                           System.out.println("sulfur/at: " + sourceName + ":" + lineNum + " — unknown access modifier '" + baseAccess + "', skipping");
                        }
                        continue;
                  }

                  AtEntry entry = new AtEntry(targetAccess, clearFinal, memberDescriptor);
                  ((Set)this.entries.computeIfAbsent(binaryClass, (k) -> new HashSet())).add(entry);
                  if (DEBUG) {
                     System.out.println("sulfur/at: registered AT: " + accessToken + " " + binaryClass + (memberDescriptor != null ? " " + memberDescriptor : ""));
                  }
               }
            }
         }
      } catch (Throwable var17) {
         try {
            reader.close();
         } catch (Throwable var16) {
            var17.addSuppressed(var16);
         }

         throw var17;
      }

      reader.close();
   }

   public boolean matches(String binaryClassName) {
      return this.entries.containsKey(binaryClassName.replace('.', '/'));
   }

   public byte[] transform(byte[] classBytes, String dotClassName) {
      String binaryName = dotClassName.replace('.', '/');
      Set<AtEntry> classEntries = (Set)this.entries.get(binaryName);
      if (classEntries != null && !classEntries.isEmpty()) {
         ClassReader reader = new ClassReader(classBytes);
         ClassNode node = new ClassNode();
         reader.accept(node, 0);

         for(AtEntry entry : classEntries) {
            if (entry.memberDescriptor == null) {
               node.access = applyAccess(node.access, entry.targetAccess, entry.clearFinal);
               if (DEBUG) {
                  System.out.println("sulfur/at: applied class AT to " + binaryName);
               }
            } else if (entry.memberDescriptor.contains("(")) {
               this.applyMethodAt(node, entry);
            } else {
               this.applyFieldAt(node, entry);
            }
         }

         ClassWriter writer = new ClassWriter(0);
         node.accept(writer);
         return writer.toByteArray();
      } else {
         return classBytes;
      }
   }

   private void applyMethodAt(ClassNode classNode, AtEntry entry) {
      int parenOpen = entry.memberDescriptor.indexOf(40);
      String name = entry.memberDescriptor.substring(0, parenOpen);
      String desc = entry.memberDescriptor.substring(parenOpen);

      for(MethodNode method : classNode.methods) {
         if (method.name.equals(name) && method.desc.equals(desc)) {
            method.access = applyAccess(method.access, entry.targetAccess, entry.clearFinal);
            if (DEBUG) {
               System.out.println("sulfur/at: applied method AT to " + classNode.name + "." + name + desc);
            }

            return;
         }
      }

      if (DEBUG) {
         System.out.println("sulfur/at: method not found for AT: " + classNode.name + "." + entry.memberDescriptor);
      }

   }

   private void applyFieldAt(ClassNode classNode, AtEntry entry) {
      for(FieldNode field : classNode.fields) {
         if (field.name.equals(entry.memberDescriptor)) {
            field.access = applyAccess(field.access, entry.targetAccess, entry.clearFinal);
            if (DEBUG) {
               System.out.println("sulfur/at: applied field AT to " + classNode.name + "." + field.name);
            }

            return;
         }
      }

      if (DEBUG) {
         System.out.println("sulfur/at: field not found for AT: " + classNode.name + "." + entry.memberDescriptor);
      }

   }

   private static int applyAccess(int existing, int targetAccess, boolean clearFinal) {
      int result = existing & -8;
      result |= targetAccess;
      if (clearFinal) {
         result &= -17;
      }

      return result;
   }

   public boolean isEmpty() {
      return this.entries.isEmpty();
   }

   private static record AtEntry(int targetAccess, boolean clearFinal, String memberDescriptor) {
   }
}
