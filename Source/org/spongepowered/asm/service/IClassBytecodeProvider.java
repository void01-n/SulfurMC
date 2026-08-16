package org.spongepowered.asm.service;

import java.io.IOException;
import org.objectweb.asm.tree.ClassNode;

public interface IClassBytecodeProvider {
   ClassNode getClassNode(String var1) throws ClassNotFoundException, IOException;

   ClassNode getClassNode(String var1, boolean var2) throws ClassNotFoundException, IOException;

   ClassNode getClassNode(String var1, boolean var2, int var3) throws ClassNotFoundException, IOException;
}
