package com.ldtteam.jammer;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.ldtteam.jam.jamspec.mapping.MappingResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JammerTest
{

    @Test
    void buildTransitiveClassMappings()
    {
        final Jammer jammer = new Jammer();

        final ClassNode classNode = new ClassNode();
        final ClassNode classNode2 = new ClassNode();
        final ClassNode classNode3 = new ClassNode();

        final MethodNode methodNode = new MethodNode();
        final MethodNode methodNode2 = new MethodNode();
        final MethodNode methodNode3 = new MethodNode();

        methodNode.name = "testMethod";
        methodNode2.name = "testMethod";
        methodNode3.name = "testMethod";

        methodNode.desc = "()V";
        methodNode2.desc = "()V";
        methodNode3.desc = "()V";

        methodNode.instructions = new InsnList();
        methodNode.instructions.add(new org.objectweb.asm.tree.MethodInsnNode(org.objectweb.asm.Opcodes.INVOKESTATIC, "java/lang/System", "out", "()Ljava/io/PrintStream;", false));

        methodNode2.instructions = new InsnList();
        methodNode2.instructions.add(new org.objectweb.asm.tree.MethodInsnNode(org.objectweb.asm.Opcodes.INVOKESTATIC, "java/lang/System", "out", "()Ljava/io/PrintStream;", false));

        methodNode3.instructions = new InsnList();
        methodNode3.instructions.add(new org.objectweb.asm.tree.MethodInsnNode(org.objectweb.asm.Opcodes.INVOKESTATIC, "java/lang/System", "out", "()Ljava/io/PrintStream;", false));

        classNode.methods.add(methodNode);
        classNode2.methods.add(methodNode2);
        classNode3.methods.add(methodNode3);

        final BiMap<ClassNode, ClassNode> nextGenMap = HashBiMap.create();
        final MappingResult<ClassNode> nextGen = new MappingResult<>(Sets.newHashSet(classNode), nextGenMap, Sets.newHashSet());

        final BiMap<ClassNode, ClassNode> nextGenMap2 = HashBiMap.create();
        final MappingResult<ClassNode> nextGen2 = new MappingResult<>(Sets.newHashSet(), nextGenMap2, Sets.newHashSet(classNode2));

        final BiMap<ClassNode, ClassNode> nextGenMap3 = HashBiMap.create();
        nextGenMap3.put(classNode2, classNode3);
        final MappingResult<ClassNode> nextGen3 = new MappingResult<>(Sets.newHashSet(), nextGenMap3, Sets.newHashSet());

        final BiMap<ClassNode, ClassNode> nextGenMap4 = HashBiMap.create();
        final MappingResult<ClassNode> nextGen4 = new MappingResult<>(Sets.newHashSet(classNode3), nextGenMap4, Sets.newHashSet());

        final BiMap<ClassNode, ClassNode> mappings = HashBiMap.create();
        mappings.put(classNode, classNode2);

        final JarMappingResult jarMappingResult = new JarMappingResult(
          nextGen,
          new MappingResult<>(Sets.newHashSet(methodNode), HashBiMap.create(), Sets.newHashSet()),
          new MappingResult<>(Sets.newHashSet(), HashBiMap.create(), Sets.newHashSet())
        );
        final JarMappingResult jarMappingResult2 = new JarMappingResult(
          nextGen2,
          new MappingResult<>(Sets.newHashSet(), HashBiMap.create(), Sets.newHashSet(methodNode2)),
          new MappingResult<>(Sets.newHashSet(), HashBiMap.create(), Sets.newHashSet())
        );
        final JarMappingResult jarMappingResult3 = new JarMappingResult(
          nextGen3,
          new MappingResult<>(Sets.newHashSet(), HashBiMap.create(ImmutableMap.of(methodNode2, methodNode3)), Sets.newHashSet()),
          new MappingResult<>(Sets.newHashSet(), HashBiMap.create(), Sets.newHashSet())
        );
        final JarMappingResult jarMappingResult4 = new JarMappingResult(
          nextGen4,
          new MappingResult<>(Sets.newHashSet(methodNode3), HashBiMap.create(), Sets.newHashSet()),
          new MappingResult<>(Sets.newHashSet(), HashBiMap.create(), Sets.newHashSet())
        );

        final LinkedList<JarMappingResult> input = new LinkedList<>();
        input.add(jarMappingResult);
        input.add(jarMappingResult2);
        input.add(jarMappingResult3);
        input.add(jarMappingResult4);

        final Map<ClassNode, LinkedList<Jammer.HistoricalClassMapping>> result = jammer.buildTransitiveClassMappings(mappings, input);

        final Map<ClassNode, LinkedList<Jammer.HistoricalClassMapping>> expected = new HashMap<>();
        expected.put(classNode, new LinkedList<>());
        expected.get(classNode).add(new Jammer.HistoricalClassMapping(classNode2, Sets.newHashSet(methodNode2), Sets.newHashSet()));
        expected.get(classNode).add(new Jammer.HistoricalClassMapping(classNode3, Sets.newHashSet(), Sets.newHashSet()));

        Assertions.assertEquals(expected, result);
    }
}