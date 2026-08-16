package org.spongepowered.include.com.google.common.io;

import java.io.IOException;
import org.spongepowered.include.com.google.errorprone.annotations.CanIgnoreReturnValue;

public interface LineProcessor<T> {
   @CanIgnoreReturnValue
   boolean processLine(String var1) throws IOException;

   T getResult();
}
