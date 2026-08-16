package org.spongepowered.asm.mixin.transformer;

import org.spongepowered.asm.mixin.extensibility.IActivityContext;

public class ActivityStack implements IActivityContext {
   public static final String GLUE_STRING = " -> ";
   private final Activity head;
   private Activity tail;
   private String glue;

   public ActivityStack() {
      this((String)null, " -> ");
   }

   public ActivityStack(String root) {
      this(root, " -> ");
   }

   public ActivityStack(String root, String glue) {
      this.head = this.tail = new Activity((Activity)null, root);
      this.glue = glue;
   }

   public void clear() {
      this.tail = this.head;
      this.head.next = null;
   }

   public IActivityContext.IActivity begin(String description) {
      return this.tail = new Activity(this.tail, description != null ? description : "null");
   }

   public IActivityContext.IActivity begin(String descriptionFormat, Object... args) {
      if (descriptionFormat == null) {
         descriptionFormat = "null";
      }

      return this.tail = new Activity(this.tail, String.format(descriptionFormat, args));
   }

   void end(Activity activity) {
      this.tail = activity.last;
      this.tail.next = null;
   }

   public String toString() {
      return this.toString(this.glue);
   }

   public String toString(String glue) {
      if (this.head.description == null && this.head.next == null) {
         return "Unknown";
      } else {
         StringBuilder sb = new StringBuilder();

         for(Activity activity = this.head; activity != null; activity = activity.next) {
            if (activity.description != null) {
               sb.append(activity.description);
               if (activity.next != null) {
                  sb.append(glue);
               }
            }
         }

         return sb.toString();
      }
   }

   public class Activity implements IActivityContext.IActivity {
      public String description;
      Activity last;
      Activity next;

      Activity(Activity last, String description) {
         if (last != null) {
            last.next = this;
         }

         this.last = last;
         this.description = description;
      }

      public void append(String text) {
         this.description = this.description != null ? this.description + text : text;
      }

      public void append(String textFormat, Object... args) {
         this.append(String.format(textFormat, args));
      }

      public void end() {
         if (this.last != null) {
            ActivityStack.this.end(this);
            this.last = null;
         }

      }

      public void next(String description) {
         if (this.next != null) {
            this.next.end();
         }

         this.description = description;
      }

      public void next(String descriptionFormat, Object... args) {
         if (descriptionFormat == null) {
            descriptionFormat = "null";
         }

         this.next(String.format(descriptionFormat, args));
      }
   }
}
