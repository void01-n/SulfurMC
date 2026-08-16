package lol.void01n.sulfur.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FabricModMetadata {
   private static final boolean DEBUG = System.getProperties().containsKey("sulfur.debug");
   public final int schemaVersion;
   public final String id;
   public final String version;
   public final String name;
   public final Map<String, List<String>> entrypoints;
   public final List<String> mixinConfigs;
   public final Map<String, String> dependencies;

   private FabricModMetadata(int schemaVersion, String id, String version, String name, Map<String, List<String>> entrypoints, List<String> mixinConfigs, Map<String, String> dependencies) {
      this.schemaVersion = schemaVersion;
      this.id = id;
      this.version = version;
      this.name = name;
      this.entrypoints = Map.copyOf(entrypoints);
      this.mixinConfigs = List.copyOf(mixinConfigs);
      this.dependencies = Map.copyOf(dependencies);
   }

   public static FabricModMetadata parse(InputStream in, String source) throws IOException {
      String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      return parseJson(json, source);
   }

   private static FabricModMetadata parseJson(String json, String source) {
      int schemaVersion = parseInt(json, "schemaVersion", 1);
      String id = parseString(json, "id", "unknown");
      String version = parseString(json, "version", "0.0.0");
      String name = parseString(json, "name", id);
      Map<String, List<String>> entrypoints = parseEntrypoints(json, source);
      List<String> mixinConfigs = parseMixinConfigs(json, source);
      Map<String, String> dependencies = parseDependencies(json, source);
      if (System.getProperties().containsKey("sulfur.debug")) {
         System.out.println("sulfur/ssl: parsed fabric.mod.json from " + source + ": id=" + id + " v" + version + " entrypoints=" + String.valueOf(entrypoints.keySet()) + " mixins=" + mixinConfigs.size());
      }

      return new FabricModMetadata(schemaVersion, id, version, name, entrypoints, mixinConfigs, dependencies);
   }

   private static String parseString(String json, String key, String defaultValue) {
      String search = "\"" + key + "\"";
      int idx = json.indexOf(search);
      if (idx < 0) {
         return defaultValue;
      } else {
         int colon = json.indexOf(58, idx + search.length());
         if (colon < 0) {
            return defaultValue;
         } else {
            int q1 = json.indexOf(34, colon + 1);
            if (q1 < 0) {
               return defaultValue;
            } else {
               int q2 = json.indexOf(34, q1 + 1);
               return q2 < 0 ? defaultValue : json.substring(q1 + 1, q2);
            }
         }
      }
   }

   private static int parseInt(String json, String key, int defaultValue) {
      String search = "\"" + key + "\"";
      int idx = json.indexOf(search);
      if (idx < 0) {
         return defaultValue;
      } else {
         int colon = json.indexOf(58, idx + search.length());
         if (colon < 0) {
            return defaultValue;
         } else {
            int start;
            for(start = colon + 1; start < json.length() && Character.isWhitespace(json.charAt(start)); ++start) {
            }

            int end;
            for(end = start; end < json.length() && Character.isDigit(json.charAt(end)); ++end) {
            }

            if (end == start) {
               return defaultValue;
            } else {
               try {
                  return Integer.parseInt(json.substring(start, end));
               } catch (NumberFormatException var9) {
                  return defaultValue;
               }
            }
         }
      }
   }

   private static Map<String, List<String>> parseEntrypoints(String json, String source) {
      Map<String, List<String>> result = new HashMap();
      int idx = json.indexOf("\"entrypoints\"");
      if (idx < 0) {
         return result;
      } else {
         int colon = json.indexOf(58, idx + "\"entrypoints\"".length());
         if (colon < 0) {
            return result;
         } else {
            int braceOpen = json.indexOf(123, colon + 1);
            if (braceOpen < 0) {
               return result;
            } else {
               int braceClose = findClosing(json, braceOpen, '{', '}');
               String block = json.substring(braceOpen + 1, braceClose);

               int arrEnd;
               for(int i = 0; i < block.length(); i = arrEnd + 1) {
                  int q1 = block.indexOf(34, i);
                  if (q1 < 0) {
                     break;
                  }

                  int q2 = block.indexOf(34, q1 + 1);
                  if (q2 < 0) {
                     break;
                  }

                  String category = block.substring(q1 + 1, q2);
                  int colonIdx = block.indexOf(58, q2 + 1);
                  if (colonIdx < 0) {
                     break;
                  }

                  int arrStart = block.indexOf(91, colonIdx + 1);
                  if (arrStart < 0) {
                     break;
                  }

                  arrEnd = findClosing(block, arrStart, '[', ']');
                  String arr = block.substring(arrStart + 1, arrEnd);
                  result.put(category, extractStrings(arr));
               }

               return result;
            }
         }
      }
   }

   private static List<String> parseMixinConfigs(String json, String source) {
      List<String> result = new ArrayList();

      for(String key : new String[]{"\"mixins\"", "\"mixin\""}) {
         int idx = json.indexOf(key);
         if (idx >= 0) {
            int colon = json.indexOf(58, idx + key.length());
            if (colon >= 0) {
               int start;
               for(start = colon + 1; start < json.length() && Character.isWhitespace(json.charAt(start)); ++start) {
               }

               if (start < json.length()) {
                  char first = json.charAt(start);
                  if (first == '[') {
                     int end = findClosing(json, start, '[', ']');
                     String arr = json.substring(start + 1, end);
                     int j = 0;

                     while(j < arr.length()) {
                        int q1 = arr.indexOf(34, j);
                        if (q1 < 0) {
                           return result;
                        }

                        int q2 = arr.indexOf(34, q1 + 1);
                        if (q2 < 0) {
                           return result;
                        }

                        String token = arr.substring(q1 + 1, q2);
                        if (token.equals("config")) {
                           int c2 = arr.indexOf(58, q2 + 1);
                           if (c2 >= 0) {
                              int vq1 = arr.indexOf(34, c2 + 1);
                              int vq2 = arr.indexOf(34, vq1 + 1);
                              if (vq1 >= 0 && vq2 > vq1) {
                                 result.add(arr.substring(vq1 + 1, vq2));
                                 j = vq2 + 1;
                                 continue;
                              }
                           }
                        } else if (token.endsWith(".json")) {
                           result.add(token);
                        }

                        j = q2 + 1;
                     }
                  } else if (first == '"') {
                     int end = json.indexOf(34, start + 1);
                     if (end > start) {
                        result.add(json.substring(start + 1, end));
                     }
                  }
                  break;
               }
            }
         }
      }

      return result;
   }

   private static Map<String, String> parseDependencies(String json, String source) {
      Map<String, String> result = new HashMap();
      int idx = json.indexOf("\"depends\"");
      if (idx < 0) {
         return result;
      } else {
         int colon = json.indexOf(58, idx + "\"depends\"".length());
         if (colon < 0) {
            return result;
         } else {
            int braceOpen = json.indexOf(123, colon + 1);
            if (braceOpen < 0) {
               return result;
            } else {
               int braceClose = findClosing(json, braceOpen, '{', '}');
               String block = json.substring(braceOpen + 1, braceClose);

               int vq2;
               for(int i = 0; i < block.length(); i = vq2 + 1) {
                  int q1 = block.indexOf(34, i);
                  if (q1 < 0) {
                     break;
                  }

                  int q2 = block.indexOf(34, q1 + 1);
                  if (q2 < 0) {
                     break;
                  }

                  String depId = block.substring(q1 + 1, q2);
                  int colonIdx = block.indexOf(58, q2 + 1);
                  if (colonIdx < 0) {
                     break;
                  }

                  int vq1 = block.indexOf(34, colonIdx + 1);
                  if (vq1 < 0) {
                     break;
                  }

                  vq2 = block.indexOf(34, vq1 + 1);
                  if (vq2 < 0) {
                     break;
                  }

                  result.put(depId, block.substring(vq1 + 1, vq2));
               }

               return result;
            }
         }
      }
   }

   private static List<String> extractStrings(String fragment) {
      List<String> result = new ArrayList();

      int q2;
      for(int i = 0; i < fragment.length(); i = q2 + 1) {
         int q1 = fragment.indexOf(34, i);
         if (q1 < 0) {
            break;
         }

         q2 = fragment.indexOf(34, q1 + 1);
         if (q2 < 0) {
            break;
         }

         result.add(fragment.substring(q1 + 1, q2));
      }

      return result;
   }

   private static int findClosing(String s, int openIdx, char open, char close) {
      int depth = 0;

      for(int i = openIdx; i < s.length(); ++i) {
         if (s.charAt(i) == open) {
            ++depth;
         } else if (s.charAt(i) == close) {
            --depth;
            if (depth == 0) {
               return i;
            }
         }
      }

      return s.length() - 1;
   }

   public String toString() {
      return "FabricModMetadata{id=" + this.id + ", version=" + this.version + "}";
   }
}
