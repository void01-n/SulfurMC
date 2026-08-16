package org.spongepowered.asm.mixin.injection.selectors;

public class InvalidSelector implements ITargetSelector {
   private String input;
   private Throwable cause;

   public InvalidSelector(Throwable cause) {
      this(cause, (String)null);
   }

   public InvalidSelector(String input) {
      this((Throwable)null, input);
   }

   public InvalidSelector(Throwable cause, String input) {
      this.input = input;
      this.cause = cause;
   }

   public String toString() {
      return this.cause != null ? String.format("%s: %s", this.cause.getClass().getName(), this.cause.getMessage()) : this.input;
   }

   public ITargetSelector next() {
      return null;
   }

   public ITargetSelector configure(ITargetSelector.Configure request, String... args) {
      return this;
   }

   public ITargetSelector validate() throws InvalidSelectorException {
      if (this.cause instanceof InvalidSelectorException) {
         throw (InvalidSelectorException)this.cause;
      } else {
         String message = "Error parsing target selector";
         if (this.input != null) {
            message = message + ", the input was in an unexpected format: " + this.input;
         }

         if (this.cause != null) {
            throw new InvalidSelectorException(message, this.cause);
         } else {
            throw new InvalidSelectorException(message);
         }
      }
   }

   public ITargetSelector attach(ISelectorContext context) throws InvalidSelectorException {
      return this;
   }

   public int getMinMatchCount() {
      return 0;
   }

   public int getMaxMatchCount() {
      return 0;
   }

   public <TNode> MatchResult match(ElementNode<TNode> node) {
      this.validate();
      return MatchResult.NONE;
   }
}
