package org.objectweb.asm.commons;

import org.objectweb.asm.AnnotationVisitor;

public class AnnotationRemapper extends AnnotationVisitor {
   protected final String descriptor;
   protected final Remapper remapper;

   /** @deprecated */
   @Deprecated
   public AnnotationRemapper(AnnotationVisitor annotationVisitor, Remapper remapper) {
      this((String)null, annotationVisitor, remapper);
   }

   public AnnotationRemapper(String descriptor, AnnotationVisitor annotationVisitor, Remapper remapper) {
      this(589824, descriptor, annotationVisitor, remapper);
   }

   /** @deprecated */
   @Deprecated
   protected AnnotationRemapper(int api, AnnotationVisitor annotationVisitor, Remapper remapper) {
      this(api, (String)null, annotationVisitor, remapper);
   }

   protected AnnotationRemapper(int api, String descriptor, AnnotationVisitor annotationVisitor, Remapper remapper) {
      super(api, annotationVisitor);
      this.descriptor = descriptor;
      this.remapper = remapper;
   }

   public void visit(String name, Object value) {
      super.visit(this.mapAnnotationAttributeName(name), this.remapper.mapValue(value));
   }

   public void visitEnum(String name, String descriptor, String value) {
      super.visitEnum(this.mapAnnotationAttributeName(name), this.remapper.mapDesc(descriptor), value);
   }

   public AnnotationVisitor visitAnnotation(String name, String descriptor) {
      AnnotationVisitor annotationVisitor = super.visitAnnotation(this.mapAnnotationAttributeName(name), this.remapper.mapDesc(descriptor));
      if (annotationVisitor == null) {
         return null;
      } else {
         return (AnnotationVisitor)(annotationVisitor == this.av ? this : this.createAnnotationRemapper(descriptor, annotationVisitor));
      }
   }

   public AnnotationVisitor visitArray(String name) {
      AnnotationVisitor annotationVisitor = super.visitArray(this.mapAnnotationAttributeName(name));
      if (annotationVisitor == null) {
         return null;
      } else {
         return (AnnotationVisitor)(annotationVisitor == this.av ? this : this.createAnnotationRemapper((String)null, annotationVisitor));
      }
   }

   /** @deprecated */
   @Deprecated
   protected AnnotationVisitor createAnnotationRemapper(AnnotationVisitor annotationVisitor) {
      return new AnnotationRemapper(this.api, (String)null, annotationVisitor, this.remapper);
   }

   protected AnnotationVisitor createAnnotationRemapper(String descriptor, AnnotationVisitor annotationVisitor) {
      return (new AnnotationRemapper(this.api, descriptor, annotationVisitor, this.remapper)).orDeprecatedValue(this.createAnnotationRemapper(annotationVisitor));
   }

   final AnnotationVisitor orDeprecatedValue(AnnotationVisitor deprecatedAnnotationVisitor) {
      if (deprecatedAnnotationVisitor.getClass() == this.getClass()) {
         AnnotationRemapper deprecatedAnnotationRemapper = (AnnotationRemapper)deprecatedAnnotationVisitor;
         if (deprecatedAnnotationRemapper.api == this.api && deprecatedAnnotationRemapper.av == this.av && deprecatedAnnotationRemapper.remapper == this.remapper) {
            return this;
         }
      }

      return deprecatedAnnotationVisitor;
   }

   private String mapAnnotationAttributeName(String name) {
      return this.descriptor == null ? name : this.remapper.mapAnnotationAttributeName(this.descriptor, name);
   }
}
