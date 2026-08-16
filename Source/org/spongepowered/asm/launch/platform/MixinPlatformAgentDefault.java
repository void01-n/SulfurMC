package org.spongepowered.asm.launch.platform;

public class MixinPlatformAgentDefault extends MixinPlatformAgentAbstract {
   public void prepare() {
      String compatibilityLevel = this.handle.getAttribute("MixinCompatibilityLevel");
      if (compatibilityLevel != null) {
         this.manager.setCompatibilityLevel(compatibilityLevel);
      }

      String mixinConfigs = this.handle.getAttribute("MixinConfigs");
      if (mixinConfigs != null) {
         for(String config : mixinConfigs.split(",")) {
            this.manager.addConfig(config.trim(), this.handle);
         }
      }

      String tokenProviders = this.handle.getAttribute("MixinTokenProviders");
      if (tokenProviders != null) {
         for(String provider : tokenProviders.split(",")) {
            this.manager.addTokenProvider(provider.trim());
         }
      }

      String connectorClass = this.handle.getAttribute("MixinConnector");
      if (connectorClass != null) {
         this.manager.addConnector(connectorClass.trim());
      }

   }
}
