package org.spongepowered.include.com.google.gson;

import org.spongepowered.include.com.google.gson.reflect.TypeToken;

public interface TypeAdapterFactory {
   <T> TypeAdapter<T> create(Gson var1, TypeToken<T> var2);
}
