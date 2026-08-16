package org.spongepowered.include.com.google.common.base;

import java.io.Serializable;
import java.util.Collection;
import javax.annotation.Nullable;

public final class Predicates {
   private static final Joiner COMMA_JOINER = Joiner.on(',');

   public static <T> Predicate<T> isNull() {
      return Predicates.ObjectPredicate.IS_NULL.<T>withNarrowedType();
   }

   public static <T> Predicate<T> equalTo(@Nullable T target) {
      return (Predicate<T>)(target == null ? isNull() : new IsEqualToPredicate(target));
   }

   public static <T> Predicate<T> in(Collection<? extends T> target) {
      return new InPredicate<T>(target);
   }

   static enum ObjectPredicate implements Predicate<Object> {
      ALWAYS_TRUE {
         public boolean apply(@Nullable Object o) {
            return true;
         }

         public String toString() {
            return "Predicates.alwaysTrue()";
         }
      },
      ALWAYS_FALSE {
         public boolean apply(@Nullable Object o) {
            return false;
         }

         public String toString() {
            return "Predicates.alwaysFalse()";
         }
      },
      IS_NULL {
         public boolean apply(@Nullable Object o) {
            return o == null;
         }

         public String toString() {
            return "Predicates.isNull()";
         }
      },
      NOT_NULL {
         public boolean apply(@Nullable Object o) {
            return o != null;
         }

         public String toString() {
            return "Predicates.notNull()";
         }
      };

      private ObjectPredicate() {
      }

      <T> Predicate<T> withNarrowedType() {
         return this;
      }
   }

   private static class IsEqualToPredicate<T> implements Serializable, Predicate<T> {
      private final T target;

      private IsEqualToPredicate(T target) {
         this.target = target;
      }

      public boolean apply(T t) {
         return this.target.equals(t);
      }

      public int hashCode() {
         return this.target.hashCode();
      }

      public boolean equals(@Nullable Object obj) {
         if (obj instanceof IsEqualToPredicate) {
            IsEqualToPredicate<?> that = (IsEqualToPredicate)obj;
            return this.target.equals(that.target);
         } else {
            return false;
         }
      }

      public String toString() {
         return "Predicates.equalTo(" + this.target + ")";
      }
   }

   private static class InPredicate<T> implements Serializable, Predicate<T> {
      private final Collection<?> target;

      private InPredicate(Collection<?> target) {
         this.target = (Collection)Preconditions.checkNotNull(target);
      }

      public boolean apply(@Nullable T t) {
         try {
            return this.target.contains(t);
         } catch (NullPointerException var3) {
            return false;
         } catch (ClassCastException var4) {
            return false;
         }
      }

      public boolean equals(@Nullable Object obj) {
         if (obj instanceof InPredicate) {
            InPredicate<?> that = (InPredicate)obj;
            return this.target.equals(that.target);
         } else {
            return false;
         }
      }

      public int hashCode() {
         return this.target.hashCode();
      }

      public String toString() {
         return "Predicates.in(" + this.target + ")";
      }
   }
}
