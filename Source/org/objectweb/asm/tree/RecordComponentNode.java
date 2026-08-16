package org.objectweb.asm.tree;

import java.util.List;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.TypePath;

public class RecordComponentNode extends RecordComponentVisitor {
   public String name;
   public String descriptor;
   public String signature;
   public List<AnnotationNode> visibleAnnotations;
   public List<AnnotationNode> invisibleAnnotations;
   public List<TypeAnnotationNode> visibleTypeAnnotations;
   public List<TypeAnnotationNode> invisibleTypeAnnotations;
   public List<Attribute> attrs;

   public RecordComponentNode(String name, String descriptor, String signature) {
      this(589824, name, descriptor, signature);
      if (this.getClass() != RecordComponentNode.class) {
         throw new IllegalStateException();
      }
   }

   public RecordComponentNode(int api, String name, String descriptor, String signature) {
      super(api);
      this.name = name;
      this.descriptor = descriptor;
      this.signature = signature;
   }

   public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
      AnnotationNode annotation = new AnnotationNode(descriptor);
      if (visible) {
         this.visibleAnnotations = Util.<AnnotationNode>add(this.visibleAnnotations, annotation);
      } else {
         this.invisibleAnnotations = Util.<AnnotationNode>add(this.invisibleAnnotations, annotation);
      }

      return annotation;
   }

   public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
      TypeAnnotationNode typeAnnotation = new TypeAnnotationNode(typeRef, typePath, descriptor);
      if (visible) {
         this.visibleTypeAnnotations = Util.<TypeAnnotationNode>add(this.visibleTypeAnnotations, typeAnnotation);
      } else {
         this.invisibleTypeAnnotations = Util.<TypeAnnotationNode>add(this.invisibleTypeAnnotations, typeAnnotation);
      }

      return typeAnnotation;
   }

   public void visitAttribute(Attribute attribute) {
      this.attrs = Util.<Attribute>add(this.attrs, attribute);
   }

   public void visitEnd() {
   }

   public void check(int api) {
      if (api < 524288) {
         throw new UnsupportedClassVersionException();
      }
   }

   public void accept(ClassVisitor classVisitor) {
      RecordComponentVisitor recordComponentVisitor = classVisitor.visitRecordComponent(this.name, this.descriptor, this.signature);
      if (recordComponentVisitor != null) {
         if (this.visibleAnnotations != null) {
            int i = 0;

            for(int n = this.visibleAnnotations.size(); i < n; ++i) {
               AnnotationNode annotation = (AnnotationNode)this.visibleAnnotations.get(i);
               annotation.accept(recordComponentVisitor.visitAnnotation(annotation.desc, true));
            }
         }

         if (this.invisibleAnnotations != null) {
            int i = 0;

            for(int n = this.invisibleAnnotations.size(); i < n; ++i) {
               AnnotationNode annotation = (AnnotationNode)this.invisibleAnnotations.get(i);
               annotation.accept(recordComponentVisitor.visitAnnotation(annotation.desc, false));
            }
         }

         if (this.visibleTypeAnnotations != null) {
            int i = 0;

            for(int n = this.visibleTypeAnnotations.size(); i < n; ++i) {
               TypeAnnotationNode typeAnnotation = (TypeAnnotationNode)this.visibleTypeAnnotations.get(i);
               typeAnnotation.accept(recordComponentVisitor.visitTypeAnnotation(typeAnnotation.typeRef, typeAnnotation.typePath, typeAnnotation.desc, true));
            }
         }

         if (this.invisibleTypeAnnotations != null) {
            int i = 0;

            for(int n = this.invisibleTypeAnnotations.size(); i < n; ++i) {
               TypeAnnotationNode typeAnnotation = (TypeAnnotationNode)this.invisibleTypeAnnotations.get(i);
               typeAnnotation.accept(recordComponentVisitor.visitTypeAnnotation(typeAnnotation.typeRef, typeAnnotation.typePath, typeAnnotation.desc, false));
            }
         }

         if (this.attrs != null) {
            int i = 0;

            for(int n = this.attrs.size(); i < n; ++i) {
               recordComponentVisitor.visitAttribute((Attribute)this.attrs.get(i));
            }
         }

         recordComponentVisitor.visitEnd();
      }
   }
}
