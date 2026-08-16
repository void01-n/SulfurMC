package lol.void01n.sulfur.filesystem;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Set;
import java.util.regex.Pattern;

public abstract class SulfurBaseFileSystem<FS extends SulfurBaseFileSystem<FS, P>, P extends FileSystemProvider> extends FileSystem {
   public abstract String name();

   public PathMatcher getPathMatcher(String syntaxAndPattern) {
      int colon = syntaxAndPattern.indexOf(58);
      if (colon < 0) {
         throw new IllegalArgumentException("Missing syntax prefix (use 'glob:' or 'regex:'): " + syntaxAndPattern);
      } else {
         String syntax = syntaxAndPattern.substring(0, colon).toLowerCase();
         String pattern = syntaxAndPattern.substring(colon + 1);
         Pattern compiled;
         switch (syntax) {
            case "glob" -> compiled = Pattern.compile(globToRegex(pattern));
            case "regex" -> compiled = Pattern.compile(pattern);
            default -> throw new UnsupportedOperationException("Unsupported syntax: " + syntax);
         }

         return (path) -> compiled.matcher(path.toString()).matches();
      }
   }

   private static String globToRegex(String glob) {
      StringBuilder sb = new StringBuilder("^");

      for(int i = 0; i < glob.length(); ++i) {
         char c = glob.charAt(i);
         switch (c) {
            case '*':
               if (i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                  sb.append(".*");
                  ++i;
                  break;
               }

               sb.append("[^/]*");
               break;
            case '?':
               sb.append("[^/]");
               break;
            case '[':
               sb.append('[');
               ++i;

               while(i < glob.length() && glob.charAt(i) != ']') {
                  sb.append(glob.charAt(i++));
               }

               sb.append(']');
               break;
            default:
               if ("\\^$.|+(){}".indexOf(c) >= 0) {
                  sb.append('\\');
               }

               sb.append(c);
         }
      }

      sb.append('$');
      return sb.toString();
   }

   public UserPrincipalLookupService getUserPrincipalLookupService() {
      throw new UnsupportedOperationException();
   }

   public WatchService newWatchService() throws IOException {
      throw new UnsupportedOperationException();
   }

   public boolean isReadOnly() {
      return true;
   }

   public String getSeparator() {
      return "/";
   }

   public Set<String> supportedFileAttributeViews() {
      return Set.of("basic");
   }
}
