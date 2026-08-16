package org.spongepowered.tools.obfuscation;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.List;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorRemappable;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.refmap.ReferenceMapper;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.interfaces.IMessagerEx;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.interfaces.IReferenceManager;

public class ReferenceManager implements IReferenceManager {
   private final IMixinAnnotationProcessor ap;
   private final String outRefMapFileName;
   private final List<ObfuscationEnvironment> environments;
   private final ReferenceMapper refMapper = new ReferenceMapper();
   private boolean allowConflicts;

   public ReferenceManager(IMixinAnnotationProcessor ap, List<ObfuscationEnvironment> environments) {
      this.ap = ap;
      this.environments = environments;
      this.outRefMapFileName = this.ap.getOption("outRefMapFile");
   }

   public boolean getAllowConflicts() {
      return this.allowConflicts;
   }

   public void setAllowConflicts(boolean allowConflicts) {
      this.allowConflicts = allowConflicts;
   }

   public void write() {
      if (this.outRefMapFileName != null) {
         PrintWriter writer = null;

         try {
            writer = this.newWriter(this.outRefMapFileName, "refmap");
            if (writer != null) {
               this.refMapper.write(writer);
            }
         } catch (IOException ex) {
            ex.printStackTrace();
         } finally {
            if (writer != null) {
               try {
                  writer.close();
               } catch (Exception var10) {
               }
            }

         }

      }
   }

   private PrintWriter newWriter(String fileName, String description) throws IOException {
      if (fileName.matches("^.*[\\\\/:].*$")) {
         File outFile = new File(fileName);
         outFile.getParentFile().mkdirs();
         this.ap.printMessage(IMessagerEx.MessageType.INFO, "Writing " + description + " to " + outFile.getAbsolutePath());
         return new PrintWriter(outFile);
      } else {
         try {
            Filer filer = this.ap.getProcessingEnvironment().getFiler();
            FileObject outResource = null;

            try {
               outResource = filer.createResource(StandardLocation.CLASS_OUTPUT, "", fileName, new Element[0]);
            } catch (Exception var8) {
               outResource = filer.createResource(StandardLocation.CLASS_OUTPUT, "", (new File(fileName)).getName(), new Element[0]);
            }

            URI resourceUri = outResource.toUri();
            String absolutePath = "file".equals(resourceUri.getScheme()) ? (new File(resourceUri)).getAbsolutePath() : resourceUri.toString();
            PrintWriter writer = new PrintWriter(outResource.openWriter());
            this.ap.printMessage(IMessagerEx.MessageType.INFO, "Writing " + description + " to (" + resourceUri.getScheme() + ") " + absolutePath);
            return writer;
         } catch (Exception ex) {
            this.ap.printMessage(IMessagerEx.MessageType.ERROR, "Cannot write " + description + " to (" + fileName + "): " + ex.getClass().getName() + ": " + ex.getMessage());
            return null;
         }
      }
   }

   public ReferenceMapper getMapper() {
      return this.refMapper;
   }

   public void addMethodMapping(String className, String reference, ObfuscationData<MappingMethod> obfMethodData) {
      for(ObfuscationEnvironment env : this.environments) {
         MappingMethod obfMethod = obfMethodData.get(env.getType());
         if (obfMethod != null) {
            MemberInfo remappedReference = new MemberInfo(obfMethod);
            this.addMapping(env.getType(), className, reference, remappedReference.toString());
         }
      }

   }

   public void addMethodMapping(String className, String reference, ITargetSelectorRemappable context, ObfuscationData<MappingMethod> obfMethodData) {
      for(ObfuscationEnvironment env : this.environments) {
         MappingMethod obfMethod = obfMethodData.get(env.getType());
         if (obfMethod != null) {
            ITargetSelectorRemappable remappedReference = context.remapUsing(obfMethod, true);
            this.addMapping(env.getType(), className, reference, remappedReference.toString());
         }
      }

   }

   public void addFieldMapping(String className, String reference, ITargetSelectorRemappable context, ObfuscationData<MappingField> obfFieldData) {
      for(ObfuscationEnvironment env : this.environments) {
         MappingField obfField = obfFieldData.get(env.getType());
         if (obfField != null) {
            MemberInfo remappedReference = MemberInfo.fromMapping(obfField.transform(env.remapDescriptor(context.getDesc())));
            this.addMapping(env.getType(), className, reference, remappedReference.toString());
         }
      }

   }

   public void addClassMapping(String className, String reference, ObfuscationData<String> obfClassData) {
      for(ObfuscationEnvironment env : this.environments) {
         String remapped = obfClassData.get(env.getType());
         if (remapped != null) {
            this.addMapping(env.getType(), className, reference, remapped);
         }
      }

   }

   protected void addMapping(ObfuscationType type, String className, String reference, String newReference) {
      String oldReference = this.refMapper.addMapping(type.getKey(), className, reference, newReference);
      if (type.isDefault()) {
         this.refMapper.addMapping((String)null, className, reference, newReference);
      }

      if (!this.allowConflicts && oldReference != null && !oldReference.equals(newReference)) {
         throw new ReferenceConflictException(oldReference, newReference);
      }
   }

   public static class ReferenceConflictException extends RuntimeException {
      private final String oldReference;
      private final String newReference;

      public ReferenceConflictException(String oldReference, String newReference) {
         this.oldReference = oldReference;
         this.newReference = newReference;
      }

      public String getOld() {
         return this.oldReference;
      }

      public String getNew() {
         return this.newReference;
      }
   }
}
