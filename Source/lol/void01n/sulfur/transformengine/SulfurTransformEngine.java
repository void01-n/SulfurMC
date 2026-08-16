package lol.void01n.sulfur.transformengine;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SulfurTransformEngine {
   private static final boolean DEBUG = System.getProperties().containsKey("sulfur.debug");
   private final List<SulfurTransformationService> adapters = new ArrayList();
   private final List<SulfurTransformationService.SulfurTransformer> transformers = new ArrayList();
   private final Set<String> adapterNames = ConcurrentHashMap.newKeySet();
   private volatile boolean initialized = false;

   public synchronized void initialize() {
      if (!this.initialized) {
         for(SulfurTransformationService adapter : ServiceLoader.load(SulfurTransformationService.class)) {
            this.adapters.add(adapter);
            this.adapterNames.add(adapter.name());
         }

         if (DEBUG) {
            PrintStream var10000 = System.out;
            int var10001 = this.adapters.size();
            var10000.println("sulfur: discovered " + var10001 + " transformation-service adapter(s): " + String.valueOf(this.adapterNames));
         }

         for(SulfurTransformationService adapter : this.adapters) {
            HashSet<String> others = new HashSet(this.adapterNames);
            others.remove(adapter.name());
            adapter.onLoad(others);
         }

         for(SulfurTransformationService adapter : this.adapters) {
            this.transformers.addAll(adapter.transformers());
         }

         this.initialized = true;
      }
   }

   public byte[] maybeTransform(byte[] classBytes, String className) {
      if (!this.initialized) {
         throw new IllegalStateException("SulfurTransformEngine.initialize() must be called before maybeTransform()");
      } else {
         byte[] result = classBytes;

         for(SulfurTransformationService.SulfurTransformer transformer : this.transformers) {
            if (transformer.matches(className)) {
               if (DEBUG) {
                  System.out.println("sulfur: transforming " + className + " via " + transformer.getClass().getName());
               }

               result = transformer.transform(result, className);
            }
         }

         return result;
      }
   }

   public Set<String> registeredAdapterNames() {
      return Set.copyOf(this.adapterNames);
   }

   public synchronized void registerLateTransformer(SulfurTransformationService.SulfurTransformer transformer) {
      if (!this.initialized) {
         throw new IllegalStateException("registerLateTransformer() called before initialize() — ensure SulfurTransformEngine.initialize() has completed first.");
      } else {
         this.transformers.add(transformer);
         if (DEBUG) {
            System.out.println("sulfur: late transformer registered: " + transformer.getClass().getName());
         }

      }
   }
}
