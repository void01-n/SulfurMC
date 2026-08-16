package org.spongepowered.include.com.google.common.util.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import org.spongepowered.include.com.google.errorprone.annotations.CanIgnoreReturnValue;

@CanIgnoreReturnValue
public interface ListeningExecutorService extends ExecutorService {
   <T> ListenableFuture<T> submit(Callable<T> var1);

   ListenableFuture<?> submit(Runnable var1);

   <T> ListenableFuture<T> submit(Runnable var1, T var2);
}
