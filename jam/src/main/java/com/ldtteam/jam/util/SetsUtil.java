package com.ldtteam.jam.util;

import com.google.common.collect.Sets;
import com.ldtteam.jam.spi.asm.ClassData;
import com.ldtteam.jam.spi.asm.FieldData;
import com.ldtteam.jam.spi.asm.MethodData;
import com.ldtteam.jam.spi.asm.ParameterData;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class SetsUtil
{

    private SetsUtil()
    {
        throw new IllegalStateException("Can not instantiate an instance of: SetsUtil. This is a utility class");
    }

    public static Collector<ClassData, ?, Set<ClassData>> classes()
    {
        return Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(classData -> classData.node().name)));
    }

    public static Collector<MethodData, ?, Set<MethodData>> methods()
    {
        return Collectors.toCollection(() -> new TreeSet<>(Comparator.<MethodData, String>comparing(data -> data.owner().node().name).thenComparing(data -> data.node().name + data.node().desc)));
    }

    public static Collector<FieldData, ?, Set<FieldData>> fields()
    {
        return Collectors.toCollection(() -> new TreeSet<>(Comparator.<FieldData, String>comparing(data -> data.owner().node().name).thenComparing(data -> data.node().name + data.node().desc)));
    }

    public static Collector<ParameterData, ?, Set<ParameterData>> parameters()
    {
        return Collectors.toCollection(() -> new TreeSet<>(Comparator.<ParameterData, String>comparing(data -> data.classOwner().node().name).thenComparing(data -> data.owner().node().name + data.owner().node().desc).thenComparing(ParameterData::index)));
    }

    public static <T> Set<T> cloneSet(final Set<T> set)
    {
        if (set instanceof TreeSet<T> treeSet) {
            return new TreeSet<>(treeSet);
        }

        return Sets.newHashSet(set);
    }
}
