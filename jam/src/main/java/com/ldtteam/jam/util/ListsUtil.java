package com.ldtteam.jam.util;

import com.ldtteam.jam.spi.asm.ClassData;
import com.ldtteam.jam.spi.asm.FieldData;
import com.ldtteam.jam.spi.asm.MethodData;
import com.ldtteam.jam.spi.asm.ParameterData;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class ListsUtil
{

    private ListsUtil()
    {
        throw new IllegalStateException("Can not instantiate an instance of: ListsUtil. This is a utility class");
    }

    public static Collector<ClassData, ?, List<ClassData>> classes()
    {
        return Collectors.collectingAndThen(
                SetsUtil.classes(),
                ArrayList::new
        );
    }

    public static Collector<MethodData, ?, List<MethodData>> methods()
    {
        return Collectors.collectingAndThen(
                SetsUtil.methods(),
                ArrayList::new
        );
    }

    public static Collector<FieldData, ?, List<FieldData>> fields()
    {
        return Collectors.collectingAndThen(
                SetsUtil.fields(),
                ArrayList::new
        );
    }

    public static Collector<ParameterData, ?, List<ParameterData>> parameters()
    {
        return Collectors.collectingAndThen(
                SetsUtil.parameters(),
                ArrayList::new
        );
    }
}
