package lol.void01n.sulfur.transformengine;

import java.util.List;
import java.util.Set;

public interface SulfurTransformationService {
   String name();

   void onLoad(Set<String> var1);

   List<? extends SulfurTransformer> transformers();

   public interface SulfurTransformer {
      boolean matches(String var1);

      byte[] transform(byte[] var1, String var2);
   }
}
