package org.spongepowered.tools.obfuscation.interfaces;

import java.util.List;

public interface IOptionProvider {
   String getOption(String var1);

   String getOption(String var1, String var2);

   boolean getOption(String var1, boolean var2);

   List<String> getOptions(String var1);
}
