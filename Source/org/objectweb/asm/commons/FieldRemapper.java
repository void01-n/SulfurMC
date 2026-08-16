package org.objectweb.asm.commons;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.TypePath;

public class FieldRemapper extends FieldVisitor {
   protected final Remapper remapper;

   public FieldRemapper(FieldVisitor fieldVisitor, Remapper remapper) {
      this(589824, fieldVisitor, remapper);
   }

   protected FieldRemapper(int api, FieldVisitor fieldVisitor, Remapper remapper) {
      super(api, fieldVisitor);
      this.remapper = remapper;
   }

   public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
      AnnotationVisitor annotationVisitor = super.visitAnnotation(this.remapper.mapDesc(descriptor), visible);
      return annotationVisitor == null ? null : this.createAnnotationRemapper(descriptor, annotationVisitor);
   }

   public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
      AnnotationVisitor annotationVisitor = super.visitTypeAnnotation(typeRef, typePath, this.remapper.mapDesc(descriptor), visible);
      return annotationVisitor == null ? null : this.createAnnotationRemapper(descriptor, annotationVisitor);
   }

   /** @deprecated */
   @Deprecated
   protected AnnotationVisitor createAnnotationRemapper(AnnotationVisitor annotationVisitor) {
      return new AnnotationRemapper(this.api, (String)null, annotationVisitor, this.remapper);
   }

   protected AnnotationVisitor createAnnotationRemapper(String descriptor, AnnotationVisitor annotationVisitor) {
      return (new AnnotationRemapper(this.api, descriptor, annotationVisitor, this.remapper)).orDeprecatedValue(this.createAnnotationRemapper(annotationVisitor));
   }
}
