package org.spongepowered.asm.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.util.asm.ASM;
import org.spongepowered.asm.util.asm.MixinVerifier;
import org.spongepowered.asm.util.throwables.LVTGeneratorError;

public final class Locals {
   private static final String[] FRAME_TYPES = new String[]{"TOP", "INTEGER", "FLOAT", "DOUBLE", "LONG", "NULL", "UNINITIALIZED_THIS"};
   private static final Map<String, List<LocalVariableNode>> calculatedLocalVariables = new HashMap();

   private Locals() {
   }

   public static LocalVariableNode[] getInitialMethodLocals(MethodNode method, ClassNode classNode, int fabricCompatibility) {
      return getInitialMethodLocals(method, classNode, fabricCompatibility, false);
   }

   public static LocalVariableNode[] getInitialMethodLocals(MethodNode method, ClassNode classNode, int fabricCompatibility, boolean fallbackToLvIndex) {
      boolean isStatic = Bytecode.isStatic(method);
      Type[] argTypes = Type.getArgumentTypes(method.desc);
      int initialFrameSize = Bytecode.getFirstNonArgLocalIndex(method);
      LocalVariableNode[] frame = new LocalVariableNode[initialFrameSize];
      int local = 0;
      String[] paramNames = fabricCompatibility >= 17000 ? getParameterNames(method, isStatic) : new String[argTypes.length];
      if (!isStatic) {
         frame[local++] = new LocalVariableNode("this", Type.getObjectType(classNode.name).getDescriptor(), (String)null, (LabelNode)null, (LabelNode)null, 0);
      }

      for(int index = 0; index < argTypes.length; ++index) {
         Type argType = argTypes[index];
         String paramName = paramNames[index];
         if (paramName == null) {
            if (fallbackToLvIndex) {
               paramName = "arg" + local;
            } else {
               paramName = "arg" + index;
            }
         }

         frame[local] = new LocalVariableNode(paramName, argType.getDescriptor(), (String)null, (LabelNode)null, (LabelNode)null, local);
         local += argType.getSize();
      }

      return frame;
   }

   private static String[] getParameterNames(MethodNode method, boolean isStatic) {
      Type[] argTypes = Type.getArgumentTypes(method.desc);
      if (argTypes.length == 0) {
         return new String[0];
      } else {
         String[] paramNames = new String[argTypes.length];
         Map<Integer, Integer> indexToParam = new HashMap();
         int localIndex = isStatic ? 0 : 1;

         for(int arg = 0; arg < argTypes.length; ++arg) {
            indexToParam.put(localIndex, arg);
            localIndex += argTypes[arg].getSize();
         }

         if (method.localVariables != null) {
            for(LocalVariableNode lvNode : method.localVariables) {
               Integer paramIndex = (Integer)indexToParam.get(lvNode.index);
               if (paramIndex != null) {
                  paramNames[paramIndex] = lvNode.name;
               }
            }
         }

         if (method.parameters != null) {
            for(int i = 0; i < Math.min(argTypes.length, method.parameters.size()); ++i) {
               if (paramNames[i] == null) {
                  paramNames[i] = ((ParameterNode)method.parameters.get(i)).name;
               }
            }
         }

         return paramNames;
      }
   }

   public static void loadLocals(Type[] locals, InsnList insns, int pos, int limit) {
      for(; pos < locals.length && limit > 0; ++pos) {
         if (locals[pos] != null) {
            insns.add((AbstractInsnNode)(new VarInsnNode(locals[pos].getOpcode(21), pos)));
            --limit;
         }
      }

   }

   public static LocalVariableNode[] getLocalsAt(ClassNode classNode, MethodNode method, AbstractInsnNode node, int fabricCompatibility) {
      return fabricCompatibility >= 10000 ? getLocalsAt(classNode, method, node, Locals.Settings.DEFAULT, fabricCompatibility) : getLocalsAt092(classNode, method, node);
   }

   public static LocalVariableNode[] getLocalsAt(ClassNode classNode, MethodNode method, AbstractInsnNode node, Settings settings) {
      return getLocalsAt(classNode, method, node, settings, 17001);
   }

   private static LocalVariableNode[] getLocalsAt(ClassNode classNode, MethodNode method, AbstractInsnNode node, Settings settings, int fabricCompatibility) {
      for(int i = 0; i < 3 && (node instanceof LabelNode || node instanceof LineNumberNode); ++i) {
         AbstractInsnNode nextNode = nextNode(method.instructions, node);
         if (nextNode instanceof FrameNode) {
            break;
         }

         node = nextNode;
      }

      ClassInfo classInfo = ClassInfo.forName(classNode.name);
      if (classInfo == null) {
         throw new LVTGeneratorError("Could not load class metadata for " + classNode.name + " generating LVT for " + method.name);
      } else {
         ClassInfo.Method methodInfo = classInfo.findMethod(method, method.access | 262144);
         if (methodInfo == null) {
            throw new LVTGeneratorError("Could not locate method metadata for " + method.name + " generating LVT in " + classNode.name);
         } else {
            List<ClassInfo.FrameData> frames = methodInfo.getFrames();
            LocalVariableNode[] initialLocals = getInitialMethodLocals(method, classNode, fabricCompatibility);
            LocalVariableNode[] frame = new LocalVariableNode[method.maxLocals];
            System.arraycopy(initialLocals, 0, frame, 0, initialLocals.length);
            int initialFrameSize = initialLocals.length;
            int frameSize = initialFrameSize;
            int frameIndex = -1;
            int lastFrameSize = initialFrameSize;
            int knownFrameSize = initialFrameSize;
            VarInsnNode storeInsn = null;
            Iterator<AbstractInsnNode> iter = method.instructions.iterator();

            while(iter.hasNext()) {
               AbstractInsnNode insn = (AbstractInsnNode)iter.next();

               for(int l = 0; l < frame.length; ++l) {
                  if (frame[l] instanceof ZombieLocalVariableNode) {
                     ZombieLocalVariableNode zombie = (ZombieLocalVariableNode)frame[l];
                     ++zombie.lifetime;
                     if (insn instanceof FrameNode) {
                        ++zombie.frames;
                     }
                  }
               }

               if (storeInsn != null) {
                  LocalVariableNode storedLocal = getLocalVariableAt(classNode, method, insn, storeInsn.var);
                  frame[storeInsn.var] = storedLocal;
                  knownFrameSize = Math.max(knownFrameSize, storeInsn.var + 1);
                  if (storedLocal != null && storeInsn.var < method.maxLocals - 1 && storedLocal.desc != null && Type.getType(storedLocal.desc).getSize() == 2) {
                     frame[storeInsn.var + 1] = null;
                     knownFrameSize = Math.max(knownFrameSize, storeInsn.var + 2);
                     if (settings.hasFlags(Locals.Settings.RESURRECT_EXPOSED_ON_STORE)) {
                        resurrect(frame, knownFrameSize, settings);
                     }
                  }

                  storeInsn = null;
               }

               if (insn instanceof FrameNode) {
                  ++frameIndex;
                  FrameNode frameNode = (FrameNode)insn;
                  if (frameNode.type != 3 && frameNode.type != 4) {
                     int frameNodeSize = computeFrameSize(frameNode, initialFrameSize);
                     ClassInfo.FrameData frameData = frameIndex < frames.size() ? (ClassInfo.FrameData)frames.get(frameIndex) : null;
                     if (frameData != null) {
                        if (frameData.type == 0) {
                           knownFrameSize = lastFrameSize = frameSize = Math.max(initialFrameSize, Math.min(frameNodeSize, frameData.size));
                        } else {
                           frameSize = getAdjustedFrameSize(frameSize, frameData, initialFrameSize);
                        }
                     } else {
                        frameSize = getAdjustedFrameSize(frameSize, frameNode, initialFrameSize);
                     }

                     if (frameSize < initialFrameSize) {
                        throw new IllegalStateException(String.format("Locals entered an invalid state evaluating %s::%s%s at instruction %d (%s). Initial frame size is %d, calculated a frame size of %d with %s", classNode.name, method.name, method.desc, method.instructions.indexOf(insn), Bytecode.describeNode(insn, false), initialFrameSize, frameSize, frameData));
                     }

                     if (frameData == null && (frameNode.type == 2 || frameNode.type == -1) || frameData != null && frameData.type == 2) {
                        for(int framePos = frameSize; framePos < frame.length; ++framePos) {
                           frame[framePos] = Locals.ZombieLocalVariableNode.of(frame[framePos], 'C');
                        }

                        lastFrameSize = frameSize;
                        knownFrameSize = frameSize;
                     } else {
                        int framePos = frameNode.type == 1 ? lastFrameSize : 0;
                        lastFrameSize = frameSize;

                        for(int localPos = 0; framePos < frame.length; ++localPos) {
                           Object localType = localPos < frameNode.local.size() ? frameNode.local.get(localPos) : null;
                           if (localType instanceof String) {
                              frame[framePos] = getLocalVariableAt(classNode, method, insn, framePos);
                           } else if (localType instanceof Integer) {
                              boolean isMarkerType = localType == Opcodes.UNINITIALIZED_THIS || localType == Opcodes.NULL;
                              boolean is32bitValue = localType == Opcodes.INTEGER || localType == Opcodes.FLOAT;
                              boolean is64bitValue = localType == Opcodes.DOUBLE || localType == Opcodes.LONG;
                              if (localType == Opcodes.TOP) {
                                 if (frame[framePos] instanceof ZombieLocalVariableNode && settings.hasFlags(Locals.Settings.RESURRECT_FOR_BOGUS_TOP)) {
                                    ZombieLocalVariableNode zombie = (ZombieLocalVariableNode)frame[framePos];
                                    if (zombie.type == 'X') {
                                       frame[framePos] = zombie.ancestor;
                                    }
                                 }
                              } else if (isMarkerType) {
                                 frame[framePos] = null;
                              } else {
                                 if (!is32bitValue && !is64bitValue) {
                                    throw new LVTGeneratorError("Unrecognised locals opcode " + localType + " in locals array at position " + localPos + " in " + classNode.name + "." + method.name + method.desc);
                                 }

                                 frame[framePos] = getLocalVariableAt(classNode, method, insn, framePos);
                                 if (is64bitValue) {
                                    ++framePos;
                                    frame[framePos] = null;
                                 }
                              }
                           } else if (localType == null) {
                              if (framePos >= initialFrameSize && framePos >= frameSize && frameSize > 0) {
                                 if (framePos < knownFrameSize) {
                                    frame[framePos] = getLocalVariableAt(classNode, method, insn, framePos);
                                 } else {
                                    frame[framePos] = Locals.ZombieLocalVariableNode.of(frame[framePos], 'X');
                                 }
                              }
                           } else if (!(localType instanceof LabelNode)) {
                              throw new LVTGeneratorError("Invalid value " + localType + " in locals array at position " + localPos + " in " + classNode.name + "." + method.name + method.desc);
                           }

                           ++framePos;
                        }
                     }
                  }
               } else if (insn instanceof VarInsnNode) {
                  VarInsnNode varInsn = (VarInsnNode)insn;
                  boolean isLoad = insn.getOpcode() >= 21 && insn.getOpcode() <= 53;
                  if (isLoad) {
                     LocalVariableNode toLoad = getLocalVariableAt(classNode, method, insn, varInsn.var);
                     frame[varInsn.var] = toLoad;
                     int varSize = toLoad != null && toLoad.desc != null ? Type.getType(frame[varInsn.var].desc).getSize() : 1;
                     knownFrameSize = Math.max(knownFrameSize, varInsn.var + varSize);
                     if (settings.hasFlags(Locals.Settings.RESURRECT_EXPOSED_ON_LOAD)) {
                        resurrect(frame, knownFrameSize, settings);
                     }
                  } else {
                     storeInsn = varInsn;
                  }
               }

               if (insn == node) {
                  break;
               }
            }

            for(int l = 0; l < frame.length; ++l) {
               if (frame[l] instanceof ZombieLocalVariableNode) {
                  ZombieLocalVariableNode zombie = (ZombieLocalVariableNode)frame[l];
                  frame[l] = zombie.lifetime > 1 ? null : zombie.ancestor;
               }

               if (frame[l] != null && frame[l].desc == null || frame[l] instanceof SyntheticLocalVariableNode) {
                  frame[l] = null;
               }
            }

            return frame;
         }
      }
   }

   private static LocalVariableNode[] getLocalsAt092(ClassNode classNode, MethodNode method, AbstractInsnNode node) {
      for(int i = 0; i < 3 && (node instanceof LabelNode || node instanceof LineNumberNode); ++i) {
         node = nextNode(method.instructions, node);
      }

      ClassInfo classInfo = ClassInfo.forName(classNode.name);
      if (classInfo == null) {
         throw new LVTGeneratorError("Could not load class metadata for " + classNode.name + " generating LVT for " + method.name);
      } else {
         ClassInfo.Method methodInfo = classInfo.findMethod(method, method.access | 262144);
         if (methodInfo == null) {
            throw new LVTGeneratorError("Could not locate method metadata for " + method.name + " generating LVT in " + classNode.name);
         } else {
            List<ClassInfo.FrameData> frames = methodInfo.getFrames();
            LocalVariableNode[] frame = new LocalVariableNode[method.maxLocals];
            int local = 0;
            int index = 0;
            if ((method.access & 8) == 0) {
               frame[local++] = new LocalVariableNode("this", Type.getObjectType(classNode.name).toString(), (String)null, (LabelNode)null, (LabelNode)null, 0);
            }

            for(Type argType : Type.getArgumentTypes(method.desc)) {
               frame[local] = new LocalVariableNode("arg" + index++, argType.toString(), (String)null, (LabelNode)null, (LabelNode)null, local);
               local += argType.getSize();
            }

            int initialFrameSize = local;
            int frameSize = local;
            int frameIndex = -1;
            int lastFrameSize = local;
            VarInsnNode storeInsn = null;
            Iterator<AbstractInsnNode> iter = method.instructions.iterator();

            while(iter.hasNext()) {
               AbstractInsnNode insn = (AbstractInsnNode)iter.next();
               if (storeInsn != null) {
                  frame[storeInsn.var] = getLocalVariableAt(classNode, method, insn, storeInsn.var);
                  storeInsn = null;
               }

               if (insn instanceof FrameNode) {
                  ++frameIndex;
                  FrameNode frameNode = (FrameNode)insn;
                  if (frameNode.type != 3 && frameNode.type != 4) {
                     ClassInfo.FrameData frameData = frameIndex < frames.size() ? (ClassInfo.FrameData)frames.get(frameIndex) : null;
                     if (frameData != null) {
                        if (frameData.type == 0) {
                           frameSize = Math.min(frameSize, frameData.locals);
                           lastFrameSize = frameSize;
                        } else {
                           frameSize = getAdjustedFrameSize(frameSize, frameData.type, frameData.rawSize, 0);
                        }
                     } else {
                        frameSize = getAdjustedFrameSize(frameSize, (FrameNode)frameNode, 0);
                     }

                     if (frameNode.type == 2) {
                        for(int framePos = frameSize; framePos < frame.length; ++framePos) {
                           frame[framePos] = null;
                        }

                        lastFrameSize = frameSize;
                     } else {
                        int framePos = frameNode.type == 1 ? lastFrameSize : 0;
                        lastFrameSize = frameSize;

                        for(int localPos = 0; framePos < frame.length; ++localPos) {
                           Object localType = localPos < frameNode.local.size() ? frameNode.local.get(localPos) : null;
                           if (localType instanceof String) {
                              frame[framePos] = getLocalVariableAt(classNode, method, insn, framePos);
                           } else if (localType instanceof Integer) {
                              boolean isMarkerType = localType == Opcodes.UNINITIALIZED_THIS || localType == Opcodes.NULL;
                              boolean is32bitValue = localType == Opcodes.INTEGER || localType == Opcodes.FLOAT;
                              boolean is64bitValue = localType == Opcodes.DOUBLE || localType == Opcodes.LONG;
                              if (localType != Opcodes.TOP) {
                                 if (isMarkerType) {
                                    frame[framePos] = null;
                                 } else {
                                    if (!is32bitValue && !is64bitValue) {
                                       throw new LVTGeneratorError("Unrecognised locals opcode " + localType + " in locals array at position " + localPos + " in " + classNode.name + "." + method.name + method.desc);
                                    }

                                    frame[framePos] = getLocalVariableAt(classNode, method, insn, framePos);
                                    if (is64bitValue) {
                                       ++framePos;
                                       frame[framePos] = null;
                                    }
                                 }
                              }
                           } else if (localType == null) {
                              if (framePos >= initialFrameSize && framePos >= frameSize && frameSize > 0) {
                                 frame[framePos] = null;
                              }
                           } else if (!(localType instanceof LabelNode)) {
                              throw new LVTGeneratorError("Invalid value " + localType + " in locals array at position " + localPos + " in " + classNode.name + "." + method.name + method.desc);
                           }

                           ++framePos;
                        }
                     }
                  }
               } else if (insn instanceof VarInsnNode) {
                  VarInsnNode varNode = (VarInsnNode)insn;
                  boolean isLoad = insn.getOpcode() >= 21 && insn.getOpcode() <= 53;
                  if (isLoad) {
                     frame[varNode.var] = getLocalVariableAt(classNode, method, insn, varNode.var);
                  } else {
                     storeInsn = varNode;
                  }
               }

               if (insn == node) {
                  break;
               }
            }

            for(int l = 0; l < frame.length; ++l) {
               if (frame[l] != null && frame[l].desc == null) {
                  frame[l] = null;
               }
            }

            return frame;
         }
      }
   }

   private static void resurrect(LocalVariableNode[] frame, int knownFrameSize, Settings settings) {
      for(int l = 0; l < knownFrameSize && l < frame.length; ++l) {
         if (frame[l] instanceof ZombieLocalVariableNode) {
            ZombieLocalVariableNode zombie = (ZombieLocalVariableNode)frame[l];
            if (zombie.checkResurrect(settings)) {
               frame[l] = zombie.ancestor;
            }
         }
      }

   }

   public static LocalVariableNode getLocalVariableAt(ClassNode classNode, MethodNode method, AbstractInsnNode node, int var) {
      return getLocalVariableAt(classNode, method, method.instructions.indexOf(node), var);
   }

   private static LocalVariableNode getLocalVariableAt(ClassNode classNode, MethodNode method, int pos, int var) {
      LocalVariableNode localVariableNode = null;
      LocalVariableNode fallbackNode = null;

      for(LocalVariableNode local : getLocalVariableTable(classNode, method)) {
         if (local.index == var) {
            if (isOpcodeInRange(method.instructions, local, pos)) {
               localVariableNode = local;
            } else if (localVariableNode == null) {
               fallbackNode = local;
            }
         }
      }

      if (localVariableNode == null && !method.localVariables.isEmpty()) {
         for(LocalVariableNode local : getGeneratedLocalVariableTable(classNode, method)) {
            if (local.index == var && isOpcodeInRange(method.instructions, local, pos)) {
               localVariableNode = local;
            }
         }
      }

      return localVariableNode != null ? localVariableNode : fallbackNode;
   }

   private static boolean isOpcodeInRange(InsnList insns, LocalVariableNode local, int pos) {
      return insns.indexOf(local.start) <= pos && insns.indexOf(local.end) > pos;
   }

   public static List<LocalVariableNode> getLocalVariableTable(ClassNode classNode, MethodNode method) {
      return method.localVariables.isEmpty() ? getGeneratedLocalVariableTable(classNode, method) : Collections.unmodifiableList(method.localVariables);
   }

   public static List<LocalVariableNode> getGeneratedLocalVariableTable(ClassNode classNode, MethodNode method) {
      String methodId = String.format("%s.%s%s", classNode.name, method.name, method.desc);
      List<LocalVariableNode> localVars = (List)calculatedLocalVariables.get(methodId);
      if (localVars != null) {
         return localVars;
      } else {
         localVars = generateLocalVariableTable(classNode, method);
         calculatedLocalVariables.put(methodId, localVars);
         return Collections.unmodifiableList(localVars);
      }
   }

   public static List<LocalVariableNode> generateLocalVariableTable(ClassNode classNode, MethodNode method) {
      List<Type> interfaces = null;
      if (classNode.interfaces != null) {
         interfaces = new ArrayList();

         for(String iface : classNode.interfaces) {
            interfaces.add(Type.getObjectType(iface));
         }
      }

      Type objectType = null;
      if (classNode.superName != null) {
         objectType = Type.getObjectType(classNode.superName);
      }

      Analyzer<BasicValue> analyzer = new Analyzer<BasicValue>(new MixinVerifier(ASM.API_VERSION, Type.getObjectType(classNode.name), objectType, interfaces, false));

      try {
         analyzer.analyze(classNode.name, method);
      } catch (AnalyzerException ex) {
         ex.printStackTrace();
      }

      Frame<BasicValue>[] frames = analyzer.getFrames();
      int methodSize = method.instructions.size();
      List<LocalVariableNode> localVariables = new ArrayList();
      LocalVariableNode[] localNodes = new LocalVariableNode[method.maxLocals];
      BasicValue[] locals = new BasicValue[method.maxLocals];
      LabelNode[] labels = new LabelNode[methodSize];
      String[] lastKnownType = new String[method.maxLocals];

      for(int i = 0; i < methodSize; ++i) {
         Frame<BasicValue> f = frames[i];
         if (f != null) {
            LabelNode label = null;

            for(int j = 0; j < f.getLocals(); ++j) {
               BasicValue local = f.getLocal(j);
               if ((local != null || locals[j] != null) && (local == null || !local.equals(locals[j]))) {
                  if (label == null) {
                     AbstractInsnNode existingLabel = method.instructions.get(i);
                     if (existingLabel instanceof LabelNode) {
                        label = (LabelNode)existingLabel;
                     } else {
                        labels[i] = label = new LabelNode();
                     }
                  }

                  if (local == null && locals[j] != null) {
                     localVariables.add(localNodes[j]);
                     localNodes[j].end = label;
                     localNodes[j] = null;
                  } else if (local != null) {
                     if (locals[j] != null) {
                        localVariables.add(localNodes[j]);
                        localNodes[j].end = label;
                        localNodes[j] = null;
                     }

                     String desc = lastKnownType[j];
                     Type localType = local.getType();
                     if (localType != null) {
                        desc = localType.getSort() >= 9 && "null".equals(localType.getInternalName()) ? "Ljava/lang/Object;" : localType.getDescriptor();
                     }

                     localNodes[j] = new LocalVariableNode("var" + j, desc, (String)null, label, (LabelNode)null, j);
                     if (desc != null) {
                        lastKnownType[j] = desc;
                     }
                  }

                  locals[j] = local;
               }
            }
         }
      }

      LabelNode label = null;

      for(int k = 0; k < localNodes.length; ++k) {
         if (localNodes[k] != null) {
            if (label == null) {
               label = new LabelNode();
               method.instructions.add((AbstractInsnNode)label);
            }

            localNodes[k].end = label;
            localVariables.add(localNodes[k]);
         }
      }

      for(int n = methodSize - 1; n >= 0; --n) {
         if (labels[n] != null) {
            method.instructions.insert(method.instructions.get(n), (AbstractInsnNode)labels[n]);
         }
      }

      return localVariables;
   }

   private static AbstractInsnNode nextNode(InsnList insns, AbstractInsnNode insn) {
      int index = insns.indexOf(insn) + 1;
      return index > 0 && index < insns.size() ? insns.get(index) : insn;
   }

   private static int getAdjustedFrameSize(int currentSize, FrameNode frameNode, int initialFrameSize) {
      return getAdjustedFrameSize(currentSize, frameNode.type, computeFrameSize(frameNode, initialFrameSize), initialFrameSize);
   }

   private static int getAdjustedFrameSize(int currentSize, ClassInfo.FrameData frameData, int initialFrameSize) {
      return getAdjustedFrameSize(currentSize, frameData.type, frameData.size, initialFrameSize);
   }

   private static int getAdjustedFrameSize(int currentSize, int type, int size, int initialFrameSize) {
      switch (type) {
         case -1:
         case 0:
            return Math.max(initialFrameSize, size);
         case 1:
            return currentSize + size;
         case 2:
            return Math.max(initialFrameSize, currentSize - size);
         case 3:
         case 4:
            return currentSize;
         default:
            return currentSize;
      }
   }

   public static int computeFrameSize(FrameNode frameNode, int initialFrameSize) {
      if (frameNode.local == null) {
         return initialFrameSize;
      } else {
         int size = 0;

         for(Object local : frameNode.local) {
            if (local instanceof Integer) {
               size += local != Opcodes.DOUBLE && local != Opcodes.LONG ? 1 : 2;
            } else {
               ++size;
            }
         }

         return Math.max(initialFrameSize, size);
      }
   }

   public static String getFrameTypeName(Object frameEntry) {
      if (frameEntry == null) {
         return "NULL";
      } else if (frameEntry instanceof String) {
         return Bytecode.getSimpleName(frameEntry.toString());
      } else if (frameEntry instanceof Integer) {
         int type = (Integer)frameEntry;
         return type >= FRAME_TYPES.length ? "INVALID" : FRAME_TYPES[type];
      } else {
         return "?";
      }
   }

   public static class SyntheticLocalVariableNode extends LocalVariableNode {
      public SyntheticLocalVariableNode(String name, String descriptor, String signature, LabelNode start, LabelNode end, int index) {
         super(name, descriptor, signature, start, end, index);
      }
   }

   static class ZombieLocalVariableNode extends LocalVariableNode {
      final LocalVariableNode ancestor;
      final char type;
      int lifetime;
      int frames;

      ZombieLocalVariableNode(LocalVariableNode ancestor, char type) {
         super(ancestor.name, ancestor.desc, ancestor.signature, ancestor.start, ancestor.end, ancestor.index);
         this.ancestor = ancestor;
         this.type = type;
      }

      boolean checkResurrect(Settings settings) {
         int insnThreshold = this.type == 'C' ? settings.choppedInsnThreshold : settings.trimmedInsnThreshold;
         if (insnThreshold > -1 && this.lifetime > insnThreshold) {
            return false;
         } else {
            int frameThreshold = this.type == 'C' ? settings.choppedFrameThreshold : settings.trimmedFrameThreshold;
            return frameThreshold == -1 || this.frames <= frameThreshold;
         }
      }

      static ZombieLocalVariableNode of(LocalVariableNode ancestor, char type) {
         if (ancestor instanceof ZombieLocalVariableNode) {
            return (ZombieLocalVariableNode)ancestor;
         } else {
            return ancestor != null ? new ZombieLocalVariableNode(ancestor, type) : null;
         }
      }

      public String toString() {
         return String.format("Z(%s,%-2d)", this.type, this.lifetime);
      }
   }

   public static class Settings {
      public static int RESURRECT_FOR_BOGUS_TOP = 1;
      public static int RESURRECT_EXPOSED_ON_LOAD = 2;
      public static int RESURRECT_EXPOSED_ON_STORE = 4;
      public static int DEFAULT_FLAGS;
      public static Settings DEFAULT;
      final int flags;
      final int flagsCustom;
      final int choppedInsnThreshold;
      final int choppedFrameThreshold;
      final int trimmedInsnThreshold;
      final int trimmedFrameThreshold;

      public Settings(int flags, int flagsCustom, int insnThreshold, int frameThreshold) {
         this(flags, flagsCustom, insnThreshold, frameThreshold, insnThreshold, frameThreshold);
      }

      public Settings(int flags, int flagsCustom, int choppedInsnThreshold, int choppedFrameThreshold, int trimmedInsnThreshold, int trimmedFrameThreshold) {
         this.flags = flags;
         this.flagsCustom = flagsCustom;
         this.choppedInsnThreshold = choppedInsnThreshold;
         this.choppedFrameThreshold = choppedFrameThreshold;
         this.trimmedInsnThreshold = trimmedInsnThreshold;
         this.trimmedFrameThreshold = trimmedFrameThreshold;
      }

      boolean hasFlags(int flags) {
         return (this.flags & flags) == flags;
      }

      static {
         DEFAULT_FLAGS = RESURRECT_FOR_BOGUS_TOP | RESURRECT_EXPOSED_ON_LOAD | RESURRECT_EXPOSED_ON_STORE;
         DEFAULT = new Settings(DEFAULT_FLAGS, 0, -1, 1, -1, -1);
      }
   }
}
