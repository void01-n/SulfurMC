package lol.void01n.sulfur.ecosystem.neoforge;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NeoForgeModMetadata {
   private static final boolean DEBUG = System.getProperties().containsKey("sulfur.debug");
   public final String id;
   public final String version;
   public final String displayName;
   public final List<String> mixinConfigs;
   public final Map<String, String> dependencies;

   private NeoForgeModMetadata(String id, String version, String displayName, List<String> mixinConfigs, Map<String, String> dependencies) {
      this.id = id;
      this.version = version;
      this.displayName = displayName != null && !displayName.isBlank() ? displayName : id;
      this.mixinConfigs = List.copyOf(mixinConfigs);
      this.dependencies = Map.copyOf(dependencies);
   }

   public static NeoForgeModMetadata parse(InputStream in, String source) throws IOException {
      String toml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      return parseToml(toml, source);
   }

   private static NeoForgeModMetadata parseToml(String toml, String source) {
      String id = null;
      String version = "0.0.0";
      String displayName = null;
      List<String> mixinConfigs = new ArrayList();
      Map<String, String> dependencies = new HashMap();
      String[] lines = toml.split("\n");
      String currentSection = "";
      String currentDepId = null;

      for(String raw : lines) {
         String line = raw.strip();
         if (!line.isEmpty() && !line.startsWith("#")) {
            if (line.startsWith("[[") && line.endsWith("]]")) {
               currentSection = line.substring(2, line.length() - 2).strip();
               currentDepId = null;
               if (DEBUG) {
                  System.out.println("sulfur/neoforge/toml: section [[" + currentSection + "]]");
               }
            } else if (line.startsWith("[") && line.endsWith("]")) {
               currentSection = line.substring(1, line.length() - 1).strip();
               currentDepId = null;
            } else {
               int eq = line.indexOf(61);
               if (eq >= 0) {
                  String key = line.substring(0, eq).strip();
                  String value = extractValue(line.substring(eq + 1).strip());
                  switch (currentSection) {
                     case "mods":
                        switch (key) {
                           case "modId":
                              id = value;
                              continue;
                           case "version":
                              version = value;
                              continue;
                           case "displayName":
                              displayName = value;
                           default:
                              continue;
                        }
                     case "mixins":
                        if ("config".equals(key) && !value.isBlank()) {
                           mixinConfigs.add(value);
                           if (DEBUG) {
                              System.out.println("sulfur/neoforge/toml: mixin config: " + value);
                           }
                        }
                        break;
                     default:
                        if (currentSection.startsWith("dependencies.")) {
                           switch (key) {
                              case "modId":
                                 currentDepId = value;
                                 break;
                              case "versionRange":
                              case "version":
                                 if (currentDepId != null && !currentDepId.isBlank()) {
                                    dependencies.put(currentDepId, value);
                                 }
                           }
                        }
                  }
               }
            }
         }
      }

      if (id == null) {
         if (DEBUG) {
            System.out.println("sulfur/neoforge/toml: no modId found in " + source + " — using filename fallback");
         }

         id = "unknown";
      }

      if (DEBUG) {
         System.out.println("sulfur/neoforge/toml: parsed " + source + ": id=" + id + " v" + version + " mixins=" + mixinConfigs.size() + " deps=" + dependencies.size());
      }

      return new NeoForgeModMetadata(id, version, displayName, mixinConfigs, dependencies);
   }

   static String extractValue(String raw) {
      String s = raw.strip();
      if (s.startsWith("\"\"\"")) {
         int end = s.indexOf("\"\"\"", 3);
         return end >= 0 ? s.substring(3, end) : s.substring(3);
      } else if (!s.startsWith("\"")) {
         if (s.startsWith("'")) {
            int end = s.indexOf(39, 1);
            return end > 0 ? s.substring(1, end) : s.substring(1);
         } else {
            int hash = s.indexOf(35);
            if (hash >= 0) {
               s = s.substring(0, hash).strip();
            }

            return s;
         }
      } else {
         StringBuilder sb = new StringBuilder();
         int i = 1;

         while(i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
               char next = s.charAt(i + 1);
               switch (next) {
                  case '"':
                     sb.append('"');
                     i += 2;
                     break;
                  case '\\':
                     sb.append('\\');
                     i += 2;
                     break;
                  case 'n':
                     sb.append('\n');
                     i += 2;
                     break;
                  case 't':
                     sb.append('\t');
                     i += 2;
                     break;
                  default:
                     sb.append(next);
                     i += 2;
               }
            } else {
               if (c == '"') {
                  break;
               }

               sb.append(c);
               ++i;
            }
         }

         return sb.toString();
      }
   }

   public String toString() {
      String var10000 = this.id;
      return "NeoForgeModMetadata{id=" + var10000 + ", version=" + this.version + ", mixins=" + this.mixinConfigs.size() + "}";
   }
}
