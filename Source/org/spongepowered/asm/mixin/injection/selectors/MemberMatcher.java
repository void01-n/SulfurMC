package org.spongepowered.asm.mixin.injection.selectors;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class MemberMatcher implements ITargetSelector {
   private static final Pattern PATTERN = Pattern.compile("((owner|name|desc)\\s*=\\s*)?/(.*?)(?<!\\\\)/");
   private static final String[] PATTERN_SOURCE_NAMES = new String[]{"owner", "name", "desc"};
   private final Pattern[] patterns;
   private final Exception parseException;
   private final String input;

   private MemberMatcher(Pattern[] patterns, Exception parseException, String input) {
      this.patterns = patterns;
      this.parseException = parseException;
      this.input = input;
   }

   public static MemberMatcher parse(String input, ISelectorContext context) {
      Matcher matcher = PATTERN.matcher(input);
      Pattern[] patterns = new Pattern[3];

      Exception parseException;
      Pattern pattern;
      int patternId;
      for(parseException = null; matcher.find(); patterns[patternId] = pattern) {
         try {
            pattern = Pattern.compile(matcher.group(3));
         } catch (PatternSyntaxException ex) {
            parseException = ex;
            pattern = Pattern.compile(".*");
            ex.printStackTrace();
         }

         patternId = "owner".equals(matcher.group(2)) ? 0 : ("desc".equals(matcher.group(2)) ? 2 : 1);
         if (patterns[patternId] != null) {
            parseException = new InvalidSelectorException("Pattern for '" + PATTERN_SOURCE_NAMES[patternId] + "' specified multiple times: Old=/" + patterns[patternId].pattern() + "/ New=/" + pattern.pattern() + "/");
         }
      }

      return new MemberMatcher(patterns, parseException, input);
   }

   public ITargetSelector validate() throws InvalidSelectorException {
      if (this.parseException != null) {
         if (this.parseException instanceof InvalidSelectorException) {
            throw (InvalidSelectorException)this.parseException;
         } else {
            throw new InvalidSelectorException("Error parsing regex selector", this.parseException);
         }
      } else {
         boolean validPattern = false;

         for(Pattern pattern : this.patterns) {
            validPattern |= pattern != null;
         }

         if (!validPattern) {
            throw new InvalidSelectorException("Error parsing regex selector, the input was in an unexpected format: " + this.input);
         } else {
            return this;
         }
      }
   }

   public String toString() {
      return this.input;
   }

   public ITargetSelector next() {
      return this;
   }

   public ITargetSelector configure(ITargetSelector.Configure request, String... args) {
      request.checkArgs(args);
      return this;
   }

   public ITargetSelector attach(ISelectorContext context) throws InvalidSelectorException {
      return this;
   }

   public int getMinMatchCount() {
      return 0;
   }

   public int getMaxMatchCount() {
      return Integer.MAX_VALUE;
   }

   public <TNode> MatchResult match(ElementNode<TNode> node) {
      return node == null ? MatchResult.NONE : this.matches(node.getOwner(), node.getName(), node.getDesc());
   }

   private MatchResult matches(String... args) {
      MatchResult result = MatchResult.NONE;

      for(int i = 0; i < this.patterns.length; ++i) {
         if (this.patterns[i] != null && args[i] != null) {
            if (this.patterns[i].matcher(args[i]).find()) {
               result = MatchResult.EXACT_MATCH;
            } else {
               result = MatchResult.NONE;
            }
         }
      }

      return result;
   }
}
