package lol.void01n.sulfur.ssl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lol.void01n.sulfur.transformengine.SulfurTransformationService;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

public final class SslTransformer implements SulfurTransformationService.SulfurTransformer {
   private static final boolean DEBUG = System.getProperties().containsKey("sulfur.debug");
   private static final Map<String, String> REMAPPING_TABLE = buildRemappingTable();
   private static final Set<String> FABRIC_PACKAGE_PREFIXES = Set.of("net/fabricmc/api/", "net/fabricmc/fabric/api/", "net/fabricmc/loader/api/");

   private static Map<String, String> buildRemappingTable() {
      Map<String, String> map = new HashMap();
      map.put("net/fabricmc/api/ModInitializer", "lol/void01n/sulfur/ssl/fabricstubs/ModInitializer");
      map.put("net/fabricmc/api/ClientModInitializer", "lol/void01n/sulfur/ssl/fabricstubs/ClientModInitializer");
      map.put("net/fabricmc/api/DedicatedServerModInitializer", "lol/void01n/sulfur/ssl/fabricstubs/DedicatedServerModInitializer");
      map.put("net/fabricmc/api/Environment", "lol/void01n/sulfur/ssl/fabricstubs/Environment");
      map.put("net/fabricmc/api/EnvType", "lol/void01n/sulfur/ssl/fabricstubs/EnvType");
      map.put("net/fabricmc/loader/api/FabricLoader", "lol/void01n/sulfur/ssl/fabricstubs/FabricLoader");
      map.put("net/fabricmc/fabric/api/event/lifecycle/v1/ServerLifecycleEvents", "net/quiltmc/qsl/lifecycle/api/event/ServerLifecycleEvents");
      map.put("net/fabricmc/fabric/api/event/lifecycle/v1/ServerWorldEvents", "net/quiltmc/qsl/lifecycle/api/event/ServerWorldEvents");
      map.put("net/fabricmc/fabric/api/event/lifecycle/v1/ServerEntityEvents", "net/quiltmc/qsl/entity/api/event/ServerEntityEvents");
      map.put("net/fabricmc/fabric/api/networking/v1/ServerPlayNetworking", "net/quiltmc/qsl/networking/api/ServerPlayNetworking");
      map.put("net/fabricmc/fabric/api/networking/v1/ClientPlayNetworking", "net/quiltmc/qsl/networking/api/ClientPlayNetworking");
      map.put("net/fabricmc/fabric/api/networking/v1/PacketByteBufs", "net/quiltmc/qsl/networking/api/PacketByteBufs");
      map.put("net/fabricmc/fabric/api/registry/v1/RegistryAttribute", "net/quiltmc/qsl/registry/api/RegistryAttribute");
      map.put("net/fabricmc/fabric/api/item/v1/FabricItemSettings", "net/quiltmc/qsl/item/api/QuiltItemSettings");
      map.put("net/fabricmc/fabric/api/object/builder/v1/block/FabricBlockSettings", "net/quiltmc/qsl/block/content/api/QuiltBlockSettings");
      map.put("net/fabricmc/fabric/api/client/rendering/v1/HudRenderCallback", "net/quiltmc/qsl/rendering/api/client/HudRenderCallback");
      map.put("net/fabricmc/fabric/api/client/rendering/v1/WorldRenderEvents", "net/quiltmc/qsl/rendering/api/client/WorldRenderEvents");
      map.put("net/fabricmc/fabric/api/client/rendering/v1/EntityModelLayerRegistry", "net/quiltmc/qsl/rendering/entity/api/client/QuiltEntityModelLayerRegistry");
      return Map.copyOf(map);
   }

   public boolean matches(String className) {
      return !className.startsWith("lol.void01n.sulfur.ssl.") && !className.startsWith("net.quiltmc.") && !className.startsWith("net.neoforged.") && !className.startsWith("net.minecraftforge.");
   }

   public byte[] transform(byte[] classBytes, String className) {
      ClassReader reader = new ClassReader(classBytes);
      ClassWriter writer = new ClassWriter(reader, 0);
      FabricRemapper remapper = new FabricRemapper();
      ClassRemapper classRemapper = new ClassRemapper(writer, remapper);
      reader.accept(classRemapper, 0);
      if (remapper.remapped && DEBUG) {
         System.out.println("sulfur/ssl: remapped Fabric API references in " + className);
      }

      return writer.toByteArray();
   }

   private static final class FabricRemapper extends Remapper {
      boolean remapped = false;

      public String map(String internalName) {
         String mapped = (String)SslTransformer.REMAPPING_TABLE.get(internalName);
         if (mapped != null) {
            this.remapped = true;
            if (SslTransformer.DEBUG) {
               System.out.println("sulfur/ssl: remapping " + internalName + " → " + mapped);
            }

            return mapped;
         } else {
            if (internalName.startsWith("net/fabricmc/") && SslTransformer.DEBUG) {
               System.out.println("sulfur/ssl: WARNING — no remap for Fabric API class: " + internalName + " (passthrough; add to SslTransformer.REMAPPING_TABLE if needed)");
            }

            return internalName;
         }
      }
   }
}
