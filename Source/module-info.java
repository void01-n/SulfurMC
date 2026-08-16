module org.spongepowered.mixin {
   requires transitive java.compiler;
   requires transitive java.instrument;
   requires transitive org.objectweb.asm;
   requires transitive org.objectweb.asm.commons;
   requires transitive org.objectweb.asm.tree;
   requires transitive org.objectweb.asm.tree.analysis;
   requires transitive org.objectweb.asm.util;
   requires java.logging;
   requires static org.apache.logging.log4j.core;
   requires static transitive cpw.mods.modlauncher;
   requires static cpw.mods.securejarhandler;
   requires static transitive org.apache.logging.log4j;
   requires static jopt.simple;
   requires static com.google.common;
   requires static guava;
   requires static com.google.gson;
   requires static gson;
   requires static java.sql;
   requires static jdk.unsupported;

   exports org.spongepowered.asm.launch;
   exports org.spongepowered.asm.launch.platform;
   exports org.spongepowered.asm.launch.platform.container;
   exports org.spongepowered.asm.lib;
   exports org.spongepowered.asm.lib.tree;
   exports org.spongepowered.asm.logging;
   exports org.spongepowered.asm.mixin;
   exports org.spongepowered.asm.mixin.connect;
   exports org.spongepowered.asm.mixin.extensibility;
   exports org.spongepowered.asm.mixin.gen;
   exports org.spongepowered.asm.mixin.gen.throwables;
   exports org.spongepowered.asm.mixin.injection;
   exports org.spongepowered.asm.mixin.injection.callback;
   exports org.spongepowered.asm.mixin.injection.code;
   exports org.spongepowered.asm.mixin.injection.invoke.arg;
   exports org.spongepowered.asm.mixin.injection.points;
   exports org.spongepowered.asm.mixin.injection.selectors;
   exports org.spongepowered.asm.mixin.injection.selectors.dynamic;
   exports org.spongepowered.asm.mixin.injection.selectors.throwables;
   exports org.spongepowered.asm.mixin.injection.struct;
   exports org.spongepowered.asm.mixin.injection.throwables;
   exports org.spongepowered.asm.mixin.refmap;
   exports org.spongepowered.asm.mixin.throwables;
   exports org.spongepowered.asm.mixin.transformer.ext;
   exports org.spongepowered.asm.mixin.transformer.throwables;
   exports org.spongepowered.asm.obfuscation;
   exports org.spongepowered.asm.obfuscation.mapping;
   exports org.spongepowered.asm.obfuscation.mapping.common;
   exports org.spongepowered.asm.obfuscation.mapping.mcp;
   exports org.spongepowered.asm.service;
   exports org.spongepowered.asm.service.modlauncher;
   exports org.spongepowered.asm.util;
   exports org.spongepowered.asm.util.asm;
   exports org.spongepowered.asm.util.perf;
   exports org.spongepowered.tools.agent;
   exports org.spongepowered.tools.obfuscation;
   exports org.spongepowered.tools.obfuscation.ext;
   exports org.spongepowered.tools.obfuscation.fg3;
   exports org.spongepowered.tools.obfuscation.interfaces;
   exports org.spongepowered.tools.obfuscation.mapping;
   exports org.spongepowered.tools.obfuscation.mapping.common;
   exports org.spongepowered.tools.obfuscation.mapping.fg3;
   exports org.spongepowered.tools.obfuscation.mapping.mcp;
   exports org.spongepowered.tools.obfuscation.mcp;
   exports org.spongepowered.tools.obfuscation.mirror;
   exports org.spongepowered.tools.obfuscation.service;

   opens org.spongepowered.asm.mixin.transformer to
      com.google.gson,
      gson;

   uses org.spongepowered.asm.service.IMixinServiceBootstrap;
   uses org.spongepowered.asm.service.IMixinService;
   uses org.spongepowered.asm.service.IGlobalPropertyService;
   uses cpw.mods.modlauncher.api.ITransformationService;
   uses cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
   uses javax.annotation.processing.Processor;
   uses org.spongepowered.tools.obfuscation.service.IObfuscationService;
   uses org.spongepowered.include.com.google.common.base.PatternCompiler;

   provides org.spongepowered.asm.service.IMixinServiceBootstrap with
      org.spongepowered.asm.service.modlauncher.MixinServiceModLauncherBootstrap;
   provides org.spongepowered.asm.service.IMixinService with
      org.spongepowered.asm.service.modlauncher.MixinServiceModLauncher;
   provides org.spongepowered.asm.service.IGlobalPropertyService with
      org.spongepowered.asm.service.modlauncher.Blackboard;
   provides cpw.mods.modlauncher.api.ITransformationService with
      org.spongepowered.asm.launch.MixinTransformationService;
   provides cpw.mods.modlauncher.serviceapi.ILaunchPluginService with
      org.spongepowered.asm.launch.MixinLaunchPlugin;
   provides javax.annotation.processing.Processor with
      org.spongepowered.tools.obfuscation.MixinObfuscationProcessorInjection,
      org.spongepowered.tools.obfuscation.MixinObfuscationProcessorTargets;
   provides org.spongepowered.tools.obfuscation.service.IObfuscationService with
      org.spongepowered.tools.obfuscation.mcp.ObfuscationServiceMCP,
      org.spongepowered.tools.obfuscation.fg3.ObfuscationServiceFG3;
}
