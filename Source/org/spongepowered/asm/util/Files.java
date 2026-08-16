package org.spongepowered.asm.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public final class Files {
   private Files() {
   }

   public static File toFile(URL url) throws URISyntaxException {
      return url != null ? toFile(url.toURI()) : null;
   }

   public static File toFile(URI uri) {
      if (uri == null) {
         return null;
      } else {
         if ("file".equals(uri.getScheme()) && uri.getAuthority() != null) {
            String strUri = uri.toString();
            if (strUri.startsWith("file://") && !strUri.startsWith("file:///")) {
               try {
                  uri = new URI("file:////" + strUri.substring(7));
               } catch (URISyntaxException ex) {
                  throw new IllegalArgumentException(ex.getMessage());
               }
            }
         }

         return new File(uri);
      }
   }

   public static void deleteRecursively(File dir) throws IOException {
      if (dir != null && dir.isDirectory()) {
         try {
            File[] files = dir.listFiles();
            if (files == null) {
               throw new IOException("Error enumerating directory during recursive delete operation: " + dir.getAbsolutePath());
            } else {
               for(File child : files) {
                  if (child.isDirectory()) {
                     deleteRecursively(child);
                  } else if (child.isFile() && !child.delete()) {
                     throw new IOException("Error deleting file during recursive delete operation: " + child.getAbsolutePath());
                  }
               }

               if (!dir.delete()) {
                  throw new IOException("Error deleting directory during recursive delete operation: " + dir.getAbsolutePath());
               }
            }
         } catch (SecurityException ex) {
            throw new IOException("Security error during recursive delete operation", ex);
         }
      }
   }
}
