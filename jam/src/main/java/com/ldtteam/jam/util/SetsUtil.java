package com.ldtteam.jam.util;

import com.google.common.collect.Sets;
import com.ldtteam.jam.spi.asm.ClassData;
import com.ldtteam.jam.spi.asm.FieldData;
import com.ldtteam.jam.spi.asm.MethodData;
import com.ldtteam.jam.spi.asm.ParameterData;

import java.time.temporal.TemporalField;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class SetsUtil
{

    private SetsUtil()
    {
        throw new IllegalStateException("Can not instantiate an instance of: SetsUtil. This is a utility class");
    }

    public static <TClassPayload> Collector<ClassData<TClassPayload>, ?, Set<ClassData<TClassPayload>>> classes()
    {
        return Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(classData -> classData.node().name)));
    }

    public static <TClassPayload, TMethodPayload> Collector<MethodData<TClassPayload, TMethodPayload>, ?, Set<MethodData<TClassPayload, TMethodPayload>>> methods()
    {
        return Collectors.toCollection(() -> new TreeSet<>(Comparator.<MethodData<TClassPayload, TMethodPayload>, String>comparing(data -> data.owner().node().name).thenComparing(data -> data.node().name + data.node().desc)));
    }

    public static <TClassPayload, TFieldPayload> Collector<FieldData<TClassPayload, TFieldPayload>, ?, Set<FieldData<TClassPayload, TFieldPayload>>> fields()
    {
        return Collectors.toCollection(() -> new TreeSet<>(Comparator.<FieldData<TClassPayload, TFieldPayload>, String>comparing(data -> data.owner().node().name).thenComparing(data -> data.node().name + data.node().desc)));
    }

    public static <TClassPayload, TMethodPayload, TParameterPayload> Collector<ParameterData<TClassPayload, TMethodPayload, TParameterPayload>, ?, Set<ParameterData<TClassPayload, TMethodPayload, TParameterPayload>>> parameters()
    {
        return Collectors.toCollection(() -> new TreeSet<>(Comparator.<ParameterData<TClassPayload, TMethodPayload, TParameterPayload>, String>comparing(data -> data.classOwner().node().name).thenComparing(data -> data.owner().node().name + data.owner().node().desc).thenComparing(ParameterData::index)));
    }

    public static <T> Set<T> cloneSet(final Set<T> set)
    {
        if (set instanceof TreeSet<T> treeSet) {
            return new TreeSet<>(treeSet);
        }

        return Sets.newHashSet(set);
    }
}
