package org.spongepowered.asm.mixin.injection.selectors;

public interface ITargetSelector {
   ITargetSelector next();

   ITargetSelector configure(Configure var1, String... var2);

   ITargetSelector validate() throws InvalidSelectorException;

   ITargetSelector attach(ISelectorContext var1) throws InvalidSelectorException;

   int getMinMatchCount();

   int getMaxMatchCount();

   <TNode> MatchResult match(ElementNode<TNode> var1);

   public static enum Configure {
      SELECT_MEMBER(0),
      SELECT_INSTRUCTION(0),
      MOVE(1),
      ORPHAN(0),
      TRANSFORM(1),
      PERMISSIVE(0),
      CLEAR_LIMITS(0);

      private int requiredArgs;

      private Configure(int requiredArgs) {
         this.requiredArgs = requiredArgs;
      }

      public void checkArgs(String... args) throws IllegalArgumentException {
         int argc = args == null ? 0 : args.length;
         if (argc < this.requiredArgs) {
            throw new IllegalArgumentException("Insufficient arguments for " + this.name() + " mutation. Required " + this.requiredArgs + " but received " + argc);
         }
      }

      // $FF: synthetic method
      private static Configure[] $values() {
         return new Configure[]{SELECT_MEMBER, SELECT_INSTRUCTION, MOVE, ORPHAN, TRANSFORM, PERMISSIVE, CLEAR_LIMITS};
      }
   }
}
