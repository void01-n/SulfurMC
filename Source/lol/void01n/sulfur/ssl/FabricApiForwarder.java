package lol.void01n.sulfur.ssl;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public final class FabricApiForwarder {
   private static final boolean DEBUG = System.getProperties().containsKey("sulfur.debug");
   private static volatile boolean initialized = false;
   private static final ConcurrentHashMap<String, byte[]> STUB_BYTES = new ConcurrentHashMap();

   public static byte[] getStubBytes(String internalName) {
      return (byte[])STUB_BYTES.get(internalName);
   }

   private FabricApiForwarder() {
   }

   public static synchronized void initialize() {
      if (!initialized) {
         if (DEBUG) {
            System.out.println("sulfur/ssl: FabricApiForwarder initializing");
         }

         generateEntrypointStubs();
         generateEnvTypeStub();
         generateEnvironmentAnnotationStub();
         generateFabricLoaderStub();
         initialized = true;
         if (DEBUG) {
            System.out.println("sulfur/ssl: FabricApiForwarder initialized");
         }

      }
   }

   private static void generateFabricLoaderStub() {
      try {
         String N = "lol/void01n/sulfur/ssl/fabricstubs/FabricLoader";
         String DESC = "Llol/void01n/sulfur/ssl/fabricstubs/FabricLoader;";
         String BRIDGE = "lol/void01n/sulfur/ssl/SulfurFabricLoaderBridge";
         String ENV_TYPE = "Llol/void01n/sulfur/ssl/fabricstubs/EnvType;";
         String ENV_TYPE_N = "lol/void01n/sulfur/ssl/fabricstubs/EnvType";
         ClassWriter cw = new ClassWriter(2);
         cw.visit(61, 33, "lol/void01n/sulfur/ssl/fabricstubs/FabricLoader", (String)null, "java/lang/Object", (String[])null);
         cw.visitField(26, "INSTANCE", "Llol/void01n/sulfur/ssl/fabricstubs/FabricLoader;", (String)null, (Object)null).visitEnd();
         MethodVisitor mv = cw.visitMethod(8, "<clinit>", "()V", (String)null, (String[])null);
         mv.visitCode();
         mv.visitTypeInsn(187, "lol/void01n/sulfur/ssl/fabricstubs/FabricLoader");
         mv.visitInsn(89);
         mv.visitMethodInsn(183, "lol/void01n/sulfur/ssl/fabricstubs/FabricLoader", "<init>", "()V", false);
         mv.visitFieldInsn(179, "lol/void01n/sulfur/ssl/fabricstubs/FabricLoader", "INSTANCE", "Llol/void01n/sulfur/ssl/fabricstubs/FabricLoader;");
         mv.visitInsn(177);
         mv.visitMaxs(0, 0);
         mv.visitEnd();
         mv = cw.visitMethod(2, "<init>", "()V", (String)null, (String[])null);
         mv.visitCode();
         mv.visitVarInsn(25, 0);
         mv.visitMethodInsn(183, "java/lang/Object", "<init>", "()V", false);
         mv.visitInsn(177);
         mv.visitMaxs(0, 0);
         mv.visitEnd();
         mv = cw.visitMethod(9, "getInstance", "()Llol/void01n/sulfur/ssl/fabricstubs/FabricLoader;", (String)null, (String[])null);
         mv.visitCode();
         mv.visitFieldInsn(178, "lol/void01n/sulfur/ssl/fabricstubs/FabricLoader", "INSTANCE", "Llol/void01n/sulfur/ssl/fabricstubs/FabricLoader;");
         mv.visitInsn(176);
         mv.visitMaxs(0, 0);
         mv.visitEnd();
         emitDelegateMethod(cw, "lol/void01n/sulfur/ssl/fabricstubs/FabricLoader", "isModLoaded", "(Ljava/lang/String;)Z", "lol/void01n/sulfur/ssl/SulfurFabricLoaderBridge", "isModLoaded", "(Ljava/lang/String;)Z", 172);
         emitDelegateMethod(cw, "lol/void01n/sulfur/ssl/fabricstubs/FabricLoader", "getGameDir", "()Ljava/nio/file/Path;", "lol/void01n/sulfur/ssl/SulfurFabricLoaderBridge", "getGameDir", "()Ljava/nio/file/Path;", 176);
         emitDelegateMethod(cw, "lol/void01n/sulfur/ssl/fabricstubs/FabricLoader", "getConfigDir", "()Ljava/nio/file/Path;", "lol/void01n/sulfur/ssl/SulfurFabricLoaderBridge", "getConfigDir", "()Ljava/nio/file/Path;", 176);
         emitDelegateMethod(cw, "lol/void01n/sulfur/ssl/fabricstubs/FabricLoader", "isDevelopmentEnvironment", "()Z", "lol/void01n/sulfur/ssl/SulfurFabricLoaderBridge", "isDevelopmentEnvironment", "()Z", 172);
         emitDelegateMethod(cw, "lol/void01n/sulfur/ssl/fabricstubs/FabricLoader", "getModContainer", "(Ljava/lang/String;)Ljava/util/Optional;", "lol/void01n/sulfur/ssl/SulfurFabricLoaderBridge", "getModContainer", "(Ljava/lang/String;)Ljava/util/Optional;", 176);
         emitDelegateMethod(cw, "lol/void01n/sulfur/ssl/fabricstubs/FabricLoader", "getAllMods", "()Ljava/util/List;", "lol/void01n/sulfur/ssl/SulfurFabricLoaderBridge", "getAllMods", "()Ljava/util/List;", 176);
         mv = cw.visitMethod(1, "getEnvironmentType", "()Llol/void01n/sulfur/ssl/fabricstubs/EnvType;", (String)null, (String[])null);
         mv.visitCode();
         mv.visitMethodInsn(184, "lol/void01n/sulfur/ssl/SulfurFabricLoaderBridge", "getEnvironmentOrdinal", "()I", false);
         mv.visitMethodInsn(184, "lol/void01n/sulfur/ssl/fabricstubs/EnvType", "values", "()[Llol/void01n/sulfur/ssl/fabricstubs/EnvType;", false);
         mv.visitInsn(95);
         mv.visitInsn(50);
         mv.visitInsn(176);
         mv.visitMaxs(0, 0);
         mv.visitEnd();
         cw.visitEnd();
         byte[] bytes = cw.toByteArray();
         injectIntoMemoryFilesystem("lol/void01n/sulfur/ssl/fabricstubs/FabricLoader", bytes);
         if (DEBUG) {
            System.out.println("sulfur/ssl: generated FabricLoader stub class");
         }
      } catch (Exception e) {
         System.err.println("sulfur/ssl: failed generating FabricLoader stub: " + String.valueOf(e));
      }

   }

   private static void emitDelegateMethod(ClassWriter cw, String owner, String name, String desc, String bridgeOwner, String bridgeName, String bridgeDesc, int returnOpcode) {
      MethodVisitor mv = cw.visitMethod(1, name, desc, (String)null, (String[])null);
      mv.visitCode();
      int argCount = countDescriptorArgs(desc);

      for(int i = 1; i <= argCount; ++i) {
         mv.visitVarInsn(25, i);
      }

      mv.visitMethodInsn(184, bridgeOwner, bridgeName, bridgeDesc, false);
      mv.visitInsn(returnOpcode);
      mv.visitMaxs(0, 0);
      mv.visitEnd();
   }

   private static int countDescriptorArgs(String desc) {
      int count = 0;

      for(int i = 1; i < desc.length() && desc.charAt(i) != ')'; ++count) {
         char c = desc.charAt(i);
         if (c == 'L') {
            i = desc.indexOf(59, i) + 1;
         } else if (c == '[') {
            ++i;
         } else {
            ++i;
         }
      }

      return count;
   }

   private static void generateEntrypointStubs() {
      String[] names = new String[]{"lol/void01n/sulfur/ssl/fabricstubs/ModInitializer", "lol/void01n/sulfur/ssl/fabricstubs/ClientModInitializer", "lol/void01n/sulfur/ssl/fabricstubs/DedicatedServerModInitializer"};

      for(String internalName : names) {
         byte[] bytes = generateSingleMethodInterface(internalName, "onInitialize", "()V");
         injectIntoMemoryFilesystem(internalName, bytes);
         if (DEBUG) {
            System.out.println("sulfur/ssl: generated stub: " + internalName);
         }
      }

   }

   private static void generateEnvTypeStub() {
      try {
         ClassWriter cw = new ClassWriter(2);
         String N = "lol/void01n/sulfur/ssl/fabricstubs/EnvType";
         String DESC = "Llol/void01n/sulfur/ssl/fabricstubs/EnvType;";
         String ADESC = "[Llol/void01n/sulfur/ssl/fabricstubs/EnvType;";
         cw.visit(61, 16433, "lol/void01n/sulfur/ssl/fabricstubs/EnvType", "Ljava/lang/Enum<Llol/void01n/sulfur/ssl/fabricstubs/EnvType;>;", "java/lang/Enum", (String[])null);
         cw.visitField(16409, "CLIENT", "Llol/void01n/sulfur/ssl/fabricstubs/EnvType;", (String)null, (Object)null).visitEnd();
         cw.visitField(16409, "SERVER", "Llol/void01n/sulfur/ssl/fabricstubs/EnvType;", (String)null, (Object)null).visitEnd();
         cw.visitField(4122, "$VALUES", "[Llol/void01n/sulfur/ssl/fabricstubs/EnvType;", (String)null, (Object)null).visitEnd();
         MethodVisitor mv = cw.visitMethod(2, "<init>", "(Ljava/lang/String;I)V", (String)null, (String[])null);
         mv.visitCode();
         mv.visitVarInsn(25, 0);
         mv.visitVarInsn(25, 1);
         mv.visitVarInsn(21, 2);
         mv.visitMethodInsn(183, "java/lang/Enum", "<init>", "(Ljava/lang/String;I)V", false);
         mv.visitInsn(177);
         mv.visitMaxs(0, 0);
         mv.visitEnd();
         mv = cw.visitMethod(8, "<clinit>", "()V", (String)null, (String[])null);
         mv.visitCode();
         mv.visitTypeInsn(187, "lol/void01n/sulfur/ssl/fabricstubs/EnvType");
         mv.visitInsn(89);
         mv.visitLdcInsn("CLIENT");
         mv.visitInsn(3);
         mv.visitMethodInsn(183, "lol/void01n/sulfur/ssl/fabricstubs/EnvType", "<init>", "(Ljava/lang/String;I)V", false);
         mv.visitFieldInsn(179, "lol/void01n/sulfur/ssl/fabricstubs/EnvType", "CLIENT", "Llol/void01n/sulfur/ssl/fabricstubs/EnvType;");
         mv.visitTypeInsn(187, "lol/void01n/sulfur/ssl/fabricstubs/EnvType");
         mv.visitInsn(89);
         mv.visitLdcInsn("SERVER");
         mv.visitInsn(4);
         mv.visitMethodInsn(183, "lol/void01n/sulfur/ssl/fabricstubs/EnvType", "<init>", "(Ljava/lang/String;I)V", false);
         mv.visitFieldInsn(179, "lol/void01n/sulfur/ssl/fabricstubs/EnvType", "SERVER", "Llol/void01n/sulfur/ssl/fabricstubs/EnvType;");
         mv.visitInsn(5);
         mv.visitTypeInsn(189, "lol/void01n/sulfur/ssl/fabricstubs/EnvType");
         mv.visitInsn(89);
         mv.visitInsn(3);
         mv.visitFieldInsn(178, "lol/void01n/sulfur/ssl/fabricstubs/EnvType", "CLIENT", "Llol/void01n/sulfur/ssl/fabricstubs/EnvType;");
         mv.visitInsn(83);
         mv.visitInsn(89);
         mv.visitInsn(4);
         mv.visitFieldInsn(178, "lol/void01n/sulfur/ssl/fabricstubs/EnvType", "SERVER", "Llol/void01n/sulfur/ssl/fabricstubs/EnvType;");
         mv.visitInsn(83);
         mv.visitFieldInsn(179, "lol/void01n/sulfur/ssl/fabricstubs/EnvType", "$VALUES", "[Llol/void01n/sulfur/ssl/fabricstubs/EnvType;");
         mv.visitInsn(177);
         mv.visitMaxs(0, 0);
         mv.visitEnd();
         mv = cw.visitMethod(9, "values", "()[Llol/void01n/sulfur/ssl/fabricstubs/EnvType;", (String)null, (String[])null);
         mv.visitCode();
         mv.visitFieldInsn(178, "lol/void01n/sulfur/ssl/fabricstubs/EnvType", "$VALUES", "[Llol/void01n/sulfur/ssl/fabricstubs/EnvType;");
         mv.visitMethodInsn(182, "java/lang/Object", "clone", "()Ljava/lang/Object;", false);
         mv.visitTypeInsn(192, "[Llol/void01n/sulfur/ssl/fabricstubs/EnvType;");
         mv.visitInsn(176);
         mv.visitMaxs(0, 0);
         mv.visitEnd();
         mv = cw.visitMethod(9, "valueOf", "(Ljava/lang/String;)Llol/void01n/sulfur/ssl/fabricstubs/EnvType;", (String)null, (String[])null);
         mv.visitCode();
         mv.visitLdcInsn(Type.getType("Llol/void01n/sulfur/ssl/fabricstubs/EnvType;"));
         mv.visitVarInsn(25, 0);
         mv.visitMethodInsn(184, "java/lang/Enum", "valueOf", "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;", false);
         mv.visitTypeInsn(192, "lol/void01n/sulfur/ssl/fabricstubs/EnvType");
         mv.visitInsn(176);
         mv.visitMaxs(0, 0);
         mv.visitEnd();
         cw.visitEnd();
         byte[] bytes = cw.toByteArray();
         injectIntoMemoryFilesystem("lol/void01n/sulfur/ssl/fabricstubs/EnvType", bytes);
         if (DEBUG) {
            System.out.println("sulfur/ssl: generated complete EnvType enum stub");
         }
      } catch (Exception e) {
         System.err.println("sulfur/ssl: failed generating EnvType stub: " + String.valueOf(e));
      }

   }

   private static void generateEnvironmentAnnotationStub() {
      try {
         ClassWriter cw = new ClassWriter(2);
         String N = "lol/void01n/sulfur/ssl/fabricstubs/Environment";
         String ENV_TYPE = "Llol/void01n/sulfur/ssl/fabricstubs/EnvType;";
         cw.visit(61, 9729, "lol/void01n/sulfur/ssl/fabricstubs/Environment", (String)null, "java/lang/Object", new String[]{"java/lang/annotation/Annotation"});
         AnnotationVisitor retAnn = cw.visitAnnotation("Ljava/lang/annotation/Retention;", true);
         retAnn.visitEnum("value", "Ljava/lang/annotation/RetentionPolicy;", "RUNTIME");
         retAnn.visitEnd();
         AnnotationVisitor targetAnn = cw.visitAnnotation("Ljava/lang/annotation/Target;", true);
         AnnotationVisitor targetArr = targetAnn.visitArray("value");
         targetArr.visitEnum((String)null, "Ljava/lang/annotation/ElementType;", "TYPE");
         targetArr.visitEnum((String)null, "Ljava/lang/annotation/ElementType;", "METHOD");
         targetArr.visitEnum((String)null, "Ljava/lang/annotation/ElementType;", "FIELD");
         targetArr.visitEnum((String)null, "Ljava/lang/annotation/ElementType;", "CONSTRUCTOR");
         targetArr.visitEnd();
         targetAnn.visitEnd();
         cw.visitMethod(1025, "value", "()Llol/void01n/sulfur/ssl/fabricstubs/EnvType;", (String)null, (String[])null).visitEnd();
         cw.visitEnd();
         byte[] bytes = cw.toByteArray();
         injectIntoMemoryFilesystem("lol/void01n/sulfur/ssl/fabricstubs/Environment", bytes);
         if (DEBUG) {
            System.out.println("sulfur/ssl: generated @Environment annotation stub");
         }
      } catch (Exception e) {
         System.err.println("sulfur/ssl: failed generating @Environment annotation stub: " + String.valueOf(e));
      }

   }

   private static byte[] generateSingleMethodInterface(String internalName, String methodName, String descriptor) {
      ClassWriter cw = new ClassWriter(0);
      cw.visit(61, 1537, internalName, (String)null, "java/lang/Object", (String[])null);
      cw.visitMethod(1025, methodName, descriptor, (String)null, (String[])null).visitEnd();
      cw.visitEnd();
      return cw.toByteArray();
   }

   private static void injectIntoMemoryFilesystem(String internalName, byte[] bytes) {
      STUB_BYTES.put(internalName, bytes);

      try {
         URI fsUri = URI.create("quilt.mfs://ssl-stubs");

         FileSystem mfs;
         try {
            mfs = FileSystems.getFileSystem(fsUri);
         } catch (FileSystemNotFoundException var6) {
            mfs = FileSystems.newFileSystem(fsUri, Map.of());
         }

         String path = "/" + internalName + ".class";
         Path target = mfs.getPath(path);
         Files.createDirectories(target.getParent());
         Files.write(target, bytes, new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING});
      } catch (Exception e) {
         System.err.println("sulfur/ssl: failed injecting " + internalName + " into memory filesystem: " + String.valueOf(e));
      }

   }

   public static URI getSslStubsFilesystemUri() {
      return URI.create("quilt.mfs://ssl-stubs");
   }
}
