package org.spongepowered.tools.obfuscation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.spongepowered.include.com.google.common.io.Files;
import org.spongepowered.include.com.google.gson.Gson;
import org.spongepowered.include.com.google.gson.JsonArray;
import org.spongepowered.include.com.google.gson.JsonElement;
import org.spongepowered.include.com.google.gson.JsonObject;
import org.spongepowered.include.com.google.gson.JsonPrimitive;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeReference;

public final class TargetMap extends HashMap<TypeReference, Set<TypeReference>> {
   private static final Gson GSON = new Gson();
   private final String sessionId;

   private TargetMap() {
      this(String.valueOf(System.currentTimeMillis()));
   }

   private TargetMap(String sessionId) {
      this.sessionId = sessionId;
   }

   public String getSessionId() {
      return this.sessionId;
   }

   void registerTargets(AnnotatedMixin mixin) {
      this.registerTargets(mixin.getTargets(), mixin.getHandle());
   }

   void registerTargets(List<TypeHandle> targets, TypeHandle mixin) {
      for(TypeHandle target : targets) {
         this.addMixin(target, mixin);
      }

   }

   void addMixin(TypeHandle target, TypeHandle mixin) {
      this.addMixin(target.getReference(), mixin.getReference());
   }

   void addMixin(String target, String mixin) {
      this.addMixin(new TypeReference(target), new TypeReference(mixin));
   }

   void addMixin(TypeReference target, TypeReference mixin) {
      Set<TypeReference> mixins = this.getMixinsFor(target);
      mixins.add(mixin);
   }

   Collection<TypeReference> getMixinsTargeting(TypeHandle target) {
      return this.getMixinsTargeting(target.getReference());
   }

   Collection<TypeReference> getMixinsTargeting(TypeReference target) {
      return Collections.unmodifiableCollection(this.getMixinsFor(target));
   }

   private Set<TypeReference> getMixinsFor(TypeReference target) {
      Set<TypeReference> mixins = (Set)this.get(target);
      if (mixins == null) {
         mixins = new HashSet();
         this.put(target, mixins);
      }

      return mixins;
   }

   public void readImports(File file) throws IOException {
      if (file.isFile()) {
         for(String line : Files.readLines(file, Charset.defaultCharset())) {
            String[] parts = line.split("\t");
            if (parts.length == 2) {
               this.addMixin(parts[1], parts[0]);
            }
         }

      }
   }

   public void write(boolean temp) {
      JsonObject jsonObject = new JsonObject();

      for(Map.Entry<TypeReference, Set<TypeReference>> entry : this.entrySet()) {
         JsonArray array = new JsonArray();

         for(TypeReference reference : (Set)entry.getValue()) {
            array.add(new JsonPrimitive(reference.getName()));
         }

         jsonObject.add(((TypeReference)entry.getKey()).getName(), array);
      }

      String json = GSON.toJson(jsonObject);
      File sessionFile = getSessionFile(this.sessionId);
      if (temp) {
         sessionFile.deleteOnExit();
      }

      try {
         FileOutputStream outputStream = new FileOutputStream(sessionFile);

         try {
            outputStream.write(json.getBytes(StandardCharsets.UTF_8));
         } catch (Throwable var9) {
            try {
               outputStream.close();
            } catch (Throwable var8) {
               var9.addSuppressed(var8);
            }

            throw var9;
         }

         outputStream.close();
      } catch (Exception ex) {
         ex.printStackTrace();
      }

   }

   private static TargetMap read(File sessionFile) {
      try {
         Reader reader = new BufferedReader(new FileReader(sessionFile));

         TargetMap var12;
         try {
            JsonObject jsonObject = (JsonObject)GSON.fromJson(reader, JsonObject.class);
            TargetMap targetMap = new TargetMap();

            for(Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
               for(JsonElement element : ((JsonElement)entry.getValue()).getAsJsonArray()) {
                  targetMap.addMixin((String)entry.getKey(), element.getAsString());
               }
            }

            var12 = targetMap;
         } catch (Throwable var10) {
            try {
               reader.close();
            } catch (Throwable var9) {
               var10.addSuppressed(var9);
            }

            throw var10;
         }

         reader.close();
         return var12;
      } catch (Exception ex) {
         ex.printStackTrace();
         return null;
      }
   }

   public static TargetMap create(String sessionId) {
      if (sessionId != null) {
         File sessionFile = getSessionFile(sessionId);
         if (sessionFile.exists()) {
            TargetMap map = read(sessionFile);
            if (map != null) {
               return map;
            }
         }
      }

      return new TargetMap();
   }

   private static File getSessionFile(String sessionId) {
      File tempDir = new File(System.getProperty("java.io.tmpdir"));
      return new File(tempDir, String.format("mixin-targetdb-%s.tmp", sessionId));
   }
}
