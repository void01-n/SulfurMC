package org.spongepowered.include.com.google.common.collect;

import java.util.NoSuchElementException;
import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.errorprone.annotations.CanIgnoreReturnValue;

public abstract class AbstractIterator<T> extends UnmodifiableIterator<T> {
   private State state;
   private T next;

   protected AbstractIterator() {
      this.state = AbstractIterator.State.NOT_READY;
   }

   protected abstract T computeNext();

   @CanIgnoreReturnValue
   protected final T endOfData() {
      this.state = AbstractIterator.State.DONE;
      return null;
   }

   @CanIgnoreReturnValue
   public final boolean hasNext() {
      Preconditions.checkState(this.state != AbstractIterator.State.FAILED);
      switch (this.state) {
         case DONE:
            return false;
         case READY:
            return true;
         default:
            return this.tryToComputeNext();
      }
   }

   private boolean tryToComputeNext() {
      this.state = AbstractIterator.State.FAILED;
      this.next = (T)this.computeNext();
      if (this.state != AbstractIterator.State.DONE) {
         this.state = AbstractIterator.State.READY;
         return true;
      } else {
         return false;
      }
   }

   @CanIgnoreReturnValue
   public final T next() {
      if (!this.hasNext()) {
         throw new NoSuchElementException();
      } else {
         this.state = AbstractIterator.State.NOT_READY;
         T result = this.next;
         this.next = null;
         return result;
      }
   }

   private static enum State {
      READY,
      NOT_READY,
      DONE,
      FAILED;
   }
}
