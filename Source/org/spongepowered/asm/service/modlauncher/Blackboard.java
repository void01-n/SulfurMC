package org.spongepowered.asm.service.modlauncher;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.TypesafeMap;
import java.util.HashMap;
import java.util.Map;
import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

public class Blackboard implements IGlobalPropertyService {
   private final Map<String, IPropertyKey> keys = new HashMap();
   private final TypesafeMap blackboard;

   public Blackboard() {
      this.blackboard = Launcher.INSTANCE.blackboard();
   }

   public IPropertyKey resolveKey(String name) {
      return (IPropertyKey)this.keys.computeIfAbsent(name, (key) -> new Key(this.blackboard, key, Object.class));
   }

   public <T> T getProperty(IPropertyKey key) {
      return (T)this.getProperty(key, (Object)null);
   }

   public void setProperty(IPropertyKey key, Object value) {
      this.blackboard.computeIfAbsent(((Key)key).key, (k) -> value);
   }

   public String getPropertyString(IPropertyKey key, String defaultValue) {
      return (String)this.getProperty(key, defaultValue);
   }

   public <T> T getProperty(IPropertyKey key, T defaultValue) {
      return (T)this.blackboard.get(((Key)key).key).orElse(defaultValue);
   }

   class Key<V> implements IPropertyKey {
      final TypesafeMap.Key<V> key;

      public Key(TypesafeMap owner, String name, Class<V> clazz) {
         this.key = cpw.mods.modlauncher.api.TypesafeMap.Key.getOrCreate(owner, name, clazz);
      }
   }
}
