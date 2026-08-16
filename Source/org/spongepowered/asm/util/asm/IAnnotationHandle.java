package org.spongepowered.asm.util.asm;

import java.util.List;
import org.objectweb.asm.Type;

public interface IAnnotationHandle {
   boolean exists();

   String getDesc();

   List<IAnnotationHandle> getAnnotationList(String var1);

   Type getTypeValue(String var1);

   List<Type> getTypeList(String var1);

   IAnnotationHandle getAnnotation(String var1);

   <T> T getValue(String var1, T var2);

   <T> T getValue();

   <T> T getValue(String var1);

   boolean getBoolean(String var1, boolean var2);

   <T> List<T> getList();

   <T> List<T> getList(String var1);
}
