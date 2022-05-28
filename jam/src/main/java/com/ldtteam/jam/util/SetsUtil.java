package com.ldtteam.jam.util;

import com.google.common.collect.Sets;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class SetsUtil
{

    private SetsUtil()
    {
        throw new IllegalStateException("Can not instantiate an instance of: SetsUtil. This is a utility class");
    }

    public static Collector<ClassNode, ?, Set<ClassNode>> classes()
    {
        return Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(classNode -> classNode.name)));
    }

    public static Collector<MethodNode, ?, Set<MethodNode>> methods(final Map<MethodNode, ClassNode> classNodesByMethodNodes)
    {
        return Collectors.toCollection(() -> new TreeSet<>(Comparator.<MethodNode, String>comparing(node -> classNodesByMethodNodes.get(node).name).thenComparing(node -> node.name + node.desc)));
    }

    public static Collector<FieldNode, ?, Set<FieldNode>> fields(final Map<FieldNode, ClassNode> classNodeByFieldNodes)
    {
        return Collectors.toCollection(() -> new TreeSet<>(Comparator.<FieldNode, String>comparing(node -> classNodeByFieldNodes.get(node).name).thenComparing(node -> node.name + node.desc)));
    }

    public static Collector<MethodNode, ?, Set<MethodNode>> methods(final Function<MethodNode, ClassNode> classNodeByMethodNodes)
    {
        return Collectors.toCollection(() -> new TreeSet<>(Comparator.<MethodNode, String>comparing(node -> classNodeByMethodNodes.apply(node).name).thenComparing(node -> node.name + node.desc)));
    }

    public static Collector<FieldNode, ?, Set<FieldNode>> fields(final Function<FieldNode, ClassNode> classNodeByFieldNodes)
    {
        return Collectors.toCollection(() -> new TreeSet<>(Comparator.<FieldNode, String>comparing(node -> classNodeByFieldNodes.apply(node).name).thenComparing(node -> node.name + node.desc)));
    }

    public static <T> Set<T> cloneSet(final Set<T> set)
    {
        if (set instanceof TreeSet<T> treeSet) {
            return new TreeSet<>(treeSet);
        }

        return Sets.newHashSet(set);
    }
}
