package org.spongepowered.asm.mixin.injection.selectors.dynamic;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Descriptors;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.asm.util.Quantifier;
import org.spongepowered.asm.util.asm.IAnnotatedElement;
import org.spongepowered.asm.util.asm.IAnnotationHandle;
import org.spongepowered.include.com.google.common.base.Joiner;
import org.spongepowered.include.com.google.common.base.Strings;

public final class DescriptorResolver {
   public static String PRINT_ID = "?";

   private DescriptorResolver() {
   }

   public static IResolvedDescriptor resolve(IAnnotationHandle desc, ISelectorContext context) {
      return new Descriptor(Collections.emptySet(), desc, context);
   }

   public static IResolvedDescriptor resolve(String id, ISelectorContext context) {
      boolean debug = false;
      IResolverObserver observer = new ResolverObserverBasic();
      if (!Strings.isNullOrEmpty(id)) {
         if (PRINT_ID.equals(id)) {
            observer = new ResolverObserverDebug(context);
            id = "";
            debug = true;
         } else {
            observer.visit(id, "", "");
         }
      }

      IAnnotationHandle desc = resolve(id, context, observer, context.getSelectorCoordinate(true));
      observer.postResolve();
      return new Descriptor(observer.getSearched(), desc, context, debug);
   }

   private static IAnnotationHandle resolve(String id, ISelectorContext context, IResolverObserver observer, String coordinate) {
      IAnnotationHandle annotation = Annotations.handleOf(context.getSelectorAnnotation());
      observer.visit(coordinate, annotation, annotation.toString() + ".desc");
      IAnnotationHandle resolved = resolve(id, context, observer, coordinate, annotation.getAnnotationList("desc"));
      if (resolved != null) {
         return resolved;
      } else {
         resolved = resolve(id, context, observer, coordinate, context.getMethod(), "method");
         if (resolved != null) {
            return resolved;
         } else {
            ISelectorContext root = getRoot(context);
            String rootCoordinate = root.getSelectorCoordinate(false);
            String mixinCoordinate = (root != context || !coordinate.contains(".")) && !rootCoordinate.equals(coordinate) ? rootCoordinate + "." + coordinate : coordinate;
            resolved = resolve(id, context, observer, mixinCoordinate, context.getMixin(), "mixin");
            if (resolved != null) {
               return resolved;
            } else {
               ISelectorContext parent = context.getParent();
               if (parent != null) {
                  String parentCoordinate = parent.getSelectorCoordinate(false) + "." + coordinate;
                  return resolve(id, parent, observer, parentCoordinate);
               } else {
                  return null;
               }
            }
         }
      }
   }

   private static IAnnotationHandle resolve(String id, ISelectorContext context, IResolverObserver observer, String coordinate, Object element, String detail) {
      observer.visit(coordinate, element, detail);
      IAnnotationHandle descriptors = getVisibleAnnotation(element, Descriptors.class);
      if (descriptors != null) {
         IAnnotationHandle resolved = resolve(id, context, observer, coordinate, descriptors.getAnnotationList("value"));
         if (resolved != null) {
            return resolved;
         }
      }

      IAnnotationHandle descriptor = getVisibleAnnotation(element, Desc.class);
      if (descriptor != null) {
         IAnnotationHandle resolved = resolve(id, context, observer, coordinate, descriptor);
         if (resolved != null) {
            return resolved;
         }
      }

      return null;
   }

   private static IAnnotationHandle resolve(String id, ISelectorContext context, IResolverObserver observer, String coordinate, List<IAnnotationHandle> availableDescriptors) {
      if (availableDescriptors != null) {
         for(IAnnotationHandle desc : availableDescriptors) {
            IAnnotationHandle resolved = resolve(id, context, observer, coordinate, desc);
            if (resolved != null) {
               return resolved;
            }
         }
      }

      return null;
   }

   private static IAnnotationHandle resolve(String id, ISelectorContext context, IResolverObserver observer, String coordinate, IAnnotationHandle desc) {
      if (desc != null) {
         String descriptorId = (String)desc.getValue("id", coordinate);
         boolean implicit = Strings.isNullOrEmpty(id);
         if (implicit && descriptorId.equalsIgnoreCase(coordinate) || !implicit && descriptorId.equalsIgnoreCase(id)) {
            return desc;
         }
      }

      return null;
   }

   private static IAnnotationHandle getVisibleAnnotation(Object element, Class<? extends Annotation> annotationClass) {
      if (element instanceof MethodNode) {
         return Annotations.handleOf(Annotations.getVisible((MethodNode)element, annotationClass));
      } else if (element instanceof ClassNode) {
         return Annotations.handleOf(Annotations.getVisible((ClassNode)element, annotationClass));
      } else if (element instanceof MixinTargetContext) {
         return Annotations.handleOf(Annotations.getVisible(((MixinTargetContext)element).getClassNode(), annotationClass));
      } else if (!(element instanceof IAnnotatedElement)) {
         if (element == null) {
            return null;
         } else {
            throw new IllegalStateException("Cannot read visible annotations from element with unknown type: " + element.getClass().getName());
         }
      } else {
         IAnnotationHandle annotation = ((IAnnotatedElement)element).getAnnotation(annotationClass);
         return annotation != null && annotation.exists() ? annotation : null;
      }
   }

   private static ISelectorContext getRoot(ISelectorContext context) {
      for(ISelectorContext parent = context.getParent(); parent != null; parent = parent.getParent()) {
         context = parent;
      }

      return context;
   }

   static final class Descriptor implements IResolvedDescriptor {
      private final Set<String> searched;
      private final IAnnotationHandle desc;
      private final ISelectorContext context;
      private final boolean debug;

      Descriptor(Set<String> searched, IAnnotationHandle desc, ISelectorContext context) {
         this(searched, desc, context, false);
      }

      Descriptor(Set<String> searched, IAnnotationHandle desc, ISelectorContext context, boolean debug) {
         this.searched = searched;
         this.desc = desc;
         this.context = context;
         this.debug = debug;
      }

      public boolean isResolved() {
         return this.desc != null;
      }

      public boolean isDebug() {
         return this.debug;
      }

      public String getResolutionInfo() {
         return this.searched == null ? "" : String.format("Searched coordinates [ \"%s\" ]", Joiner.on("\", \"").join(this.searched));
      }

      public IAnnotationHandle getAnnotation() {
         return this.desc;
      }

      public String getId() {
         return this.desc != null ? (String)this.desc.getValue("id", "") : "";
      }

      public Type getOwner() {
         if (this.desc == null) {
            return Type.VOID_TYPE;
         } else {
            Type ownerClass = this.desc.getTypeValue("owner");
            if (ownerClass != Type.VOID_TYPE) {
               return ownerClass;
            } else {
               return this.context != null ? Type.getObjectType(this.context.getMixin().getTargetClassRef()) : ownerClass;
            }
         }
      }

      public String getName() {
         if (this.desc == null) {
            return "";
         } else {
            String value = (String)this.desc.getValue("value", "");
            return !value.isEmpty() ? value : (String)this.desc.getValue("name", "");
         }
      }

      public Type[] getArgs() {
         if (this.desc == null) {
            return new Type[0];
         } else {
            List<Type> args = this.desc.getTypeList("args");
            return (Type[])args.toArray(new Type[args.size()]);
         }
      }

      public Type getReturnType() {
         return this.desc == null ? Type.VOID_TYPE : this.desc.getTypeValue("ret");
      }

      public Quantifier getMatches() {
         if (this.desc == null) {
            return Quantifier.DEFAULT;
         } else {
            int min = Math.max(0, this.desc != null ? (Integer)this.desc.getValue("min", 0) : 0);
            Integer max = this.desc != null ? (Integer)this.desc.getValue("max", (Object)null) : null;
            return new Quantifier(min, max != null ? (max > 0 ? max : Integer.MAX_VALUE) : -1);
         }
      }

      public List<IAnnotationHandle> getNext() {
         return this.desc != null ? this.desc.getAnnotationList("next") : Collections.emptyList();
      }
   }

   static class ResolverObserverBasic implements IResolverObserver {
      private final Set<String> searched = new LinkedHashSet();

      public void visit(String coordinate, Object element, String detail) {
         this.searched.add(coordinate);
      }

      public Set<String> getSearched() {
         return this.searched;
      }

      public void postResolve() {
      }
   }

   static class ResolverObserverDebug extends ResolverObserverBasic {
      private final PrettyPrinter printer = new PrettyPrinter();

      ResolverObserverDebug(ISelectorContext context) {
         this.printer.add("Searching for implicit descriptor").add((Object)context).hr().table();
         this.printer.tr("Context Coordinate:", context.getSelectorCoordinate(true) + " (" + context.getSelectorCoordinate(false) + ")");
         this.printer.tr("Selector Annotation:", context.getSelectorAnnotation());
         this.printer.tr("Root Annotation:", context.getAnnotation());
         this.printer.tr("Method:", context.getMethod()).hr();
         this.printer.table("Search Coordinate", "Search Element", "Detail").th().hr();
      }

      public void visit(String coordinate, Object element, String detail) {
         super.visit(coordinate, element, detail);
         this.printer.tr(coordinate, element, detail);
      }

      public void postResolve() {
         this.printer.print();
      }
   }

   interface IResolverObserver {
      void visit(String var1, Object var2, String var3);

      Set<String> getSearched();

      void postResolve();
   }
}
