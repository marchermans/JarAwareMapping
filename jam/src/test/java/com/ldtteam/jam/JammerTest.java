package com.ldtteam.jam;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.ldtteam.jam.spi.asm.ClassData;
import com.ldtteam.jam.spi.asm.MethodData;
import com.ldtteam.jam.spi.mapping.MappingResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

class JammerTest
{

    @Test
    void buildTransitiveClassMappings()
    {
        final Jammer jammer = new Jammer();

        final ClassData classNode = new ClassData(new ClassNode());
        final ClassData classNode2 = new ClassData(new ClassNode());
        final ClassData classNode3 = new ClassData(new ClassNode());

        classNode.node().name = "test";
        classNode2.node().name = "test";
        classNode3.node().name = "test";

        final MethodData methodNode = new MethodData(classNode, new MethodNode());
        final MethodData methodNode2 = new MethodData(classNode2, new MethodNode());
        final MethodData methodNode3 = new MethodData(classNode3, new MethodNode());

        methodNode.node().name = "testMethod";
        methodNode2.node().name = "testMethod";
        methodNode3.node().name = "testMethod";

        methodNode.node().desc = "()V";
        methodNode2.node().desc = "()V";
        methodNode3.node().desc = "()V";

        methodNode.node().instructions = new InsnList();
        methodNode.node().instructions.add(new org.objectweb.asm.tree.MethodInsnNode(org.objectweb.asm.Opcodes.INVOKESTATIC, "java/lang/System", "out", "()Ljava/io/PrintStream;", false));

        methodNode2.node().instructions = new InsnList();
        methodNode2.node().instructions.add(new org.objectweb.asm.tree.MethodInsnNode(org.objectweb.asm.Opcodes.INVOKESTATIC, "java/lang/System", "out", "()Ljava/io/PrintStream;", false));

        methodNode3.node().instructions = new InsnList();
        methodNode3.node().instructions.add(new org.objectweb.asm.tree.MethodInsnNode(org.objectweb.asm.Opcodes.INVOKESTATIC, "java/lang/System", "out", "()Ljava/io/PrintStream;", false));

        classNode.node().methods.add(methodNode.node());
        classNode2.node().methods.add(methodNode2.node());
        classNode3.node().methods.add(methodNode3.node());

        final BiMap<ClassData, ClassData> nextGenMap = HashBiMap.create();
        final MappingResult<ClassData> nextGen = new MappingResult<>(Sets.newHashSet(classNode), nextGenMap, Sets.newHashSet());

        final BiMap<ClassData, ClassData> nextGenMap2 = HashBiMap.create();
        final MappingResult<ClassData> nextGen2 = new MappingResult<>(Sets.newHashSet(), nextGenMap2, Sets.newHashSet(classNode2));

        final BiMap<ClassData, ClassData> nextGenMap3 = HashBiMap.create();
        nextGenMap3.put(classNode2, classNode3);
        final MappingResult<ClassData> nextGen3 = new MappingResult<>(Sets.newHashSet(), nextGenMap3, Sets.newHashSet());

        final BiMap<ClassData, ClassData> nextGenMap4 = HashBiMap.create();
        final MappingResult<ClassData> nextGen4 = new MappingResult<>(Sets.newHashSet(classNode3), nextGenMap4, Sets.newHashSet());

        final BiMap<ClassData, ClassData> mappings = HashBiMap.create();
        mappings.put(classNode, classNode2);

        final JarMappingResult jarMappingResult = new JarMappingResult(
          nextGen,
          new MappingResult<>(Sets.newHashSet(methodNode), HashBiMap.create(), Sets.newHashSet()),
          new MappingResult<>(Sets.newHashSet(), HashBiMap.create(), Sets.newHashSet()),
          new MappingResult<>(Sets.newHashSet(), HashBiMap.create(), Sets.newHashSet())
        );
        final JarMappingResult jarMappingResult2 = new JarMappingResult(
          nextGen2,
          new MappingResult<>(Sets.newHashSet(), HashBiMap.create(), Sets.newHashSet(methodNode2)),
          new MappingResult<>(Sets.newHashSet(), HashBiMap.create(), Sets.newHashSet()),
          new MappingResult<>(Sets.newHashSet(), HashBiMap.create(), Sets.newHashSet())
        );
        final JarMappingResult jarMappingResult3 = new JarMappingResult(
          nextGen3,
          new MappingResult<>(Sets.newHashSet(), HashBiMap.create(ImmutableMap.of(methodNode2, methodNode3)), Sets.newHashSet()),
          new MappingResult<>(Sets.newHashSet(), HashBiMap.create(), Sets.newHashSet()),
          new MappingResult<>(Sets.newHashSet(), HashBiMap.create(), Sets.newHashSet())
        );
        final JarMappingResult jarMappingResult4 = new JarMappingResult(
          nextGen4,
          new MappingResult<>(Sets.newHashSet(methodNode3), HashBiMap.create(), Sets.newHashSet()),
          new MappingResult<>(Sets.newHashSet(), HashBiMap.create(), Sets.newHashSet()),
          new MappingResult<>(Sets.newHashSet(), HashBiMap.create(), Sets.newHashSet())
        );

        final LinkedList<JarMappingResult> input = new LinkedList<>();
        input.add(jarMappingResult);
        input.add(jarMappingResult2);
        input.add(jarMappingResult3);
        input.add(jarMappingResult4);

        final Map<ClassData, List<Jammer.HistoricalClassMapping>> result = jammer.buildTransitiveClassMappings(mappings, input);

        final Map<ClassData, LinkedList<Jammer.HistoricalClassMapping>> expected = new HashMap<>();
        expected.put(classNode, new LinkedList<>());
        expected.get(classNode).add(new Jammer.HistoricalClassMapping(classNode2, Sets.newHashSet(methodNode2), Sets.newHashSet()));
        expected.get(classNode).add(new Jammer.HistoricalClassMapping(classNode3, Sets.newHashSet(), Sets.newHashSet()));

        Assertions.assertEquals(expected, result);
    }
}