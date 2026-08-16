package org.objectweb.asm.util;

import java.util.Map;
import org.objectweb.asm.Label;

public interface TextifierSupport {
   void textify(StringBuilder var1, Map<Label, String> var2);
}
