package org.spongepowered.asm.launch;

import cpw.mods.jarhandling.JarMetadata;
import cpw.mods.jarhandling.SecureJar;
import cpw.mods.jarhandling.VirtualJar;
import cpw.mods.modlauncher.api.IModuleLayerManager;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.IModuleLayerManager.Layer;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.include.com.google.common.collect.ImmutableList;
import org.spongepowered.include.com.google.common.collect.ImmutableSet;

public class MixinTransformationService extends MixinTransformationServiceAbstract {
   public List<ITransformationService.Resource> completeScan(IModuleLayerManager layerManager) {
      try {
         Path codeSource = Path.of(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
         if (this.detectVirtualJar(layerManager)) {
            try {
               return ImmutableList.<ITransformationService.Resource>of(this.createVirtualJar(codeSource));
            } catch (URISyntaxException e) {
               throw new RuntimeException(e);
            }
         } else {
            try {
               return ImmutableList.<ITransformationService.Resource>of(this.createShim(codeSource));
            } catch (URISyntaxException e) {
               throw new RuntimeException(e);
            }
         }
      } catch (Throwable th) {
         th.printStackTrace();
         return super.completeScan(layerManager);
      }
   }

   private boolean detectVirtualJar(IModuleLayerManager layerManager) {
      try {
         MixinService.getService().getClassProvider().findClass("cpw.mods.jarhandling.VirtualJar", false);
         return true;
      } catch (ClassNotFoundException var3) {
         return false;
      }
   }

   private ITransformationService.Resource createVirtualJar(Path codeSource) throws URISyntaxException {
      VirtualJar jar = new VirtualJar("mixin_synthetic", codeSource, new String[]{"org.spongepowered.asm.synthetic", "org.spongepowered.asm.synthetic.args"});
      return new ITransformationService.Resource(Layer.GAME, ImmutableList.of(jar));
   }

   private ITransformationService.Resource createShim(Path codeSource) throws URISyntaxException {
      Path path = codeSource.resolve("mixin_synthetic");
      Set<String> packages = ImmutableSet.<String>of("org.spongepowered.asm.synthetic", "org.spongepowered.asm.synthetic.args");
      SecureJar jar = SecureJar.from((sj) -> JarMetadata.fromFileName(path, packages, ImmutableList.of()), new Path[]{codeSource});
      return new ITransformationService.Resource(Layer.GAME, ImmutableList.of(jar));
   }
}
