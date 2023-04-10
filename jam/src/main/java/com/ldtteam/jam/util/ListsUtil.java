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

    public static <TClassPayload>  Collector<ClassData<TClassPayload>, ?, List<ClassData<TClassPayload>>> classes()
    {
        return Collectors.collectingAndThen(
                SetsUtil.classes(),
                ArrayList::new
        );
    }

    public static <TClassPayload, TMethodPayload>  Collector<MethodData<TClassPayload, TMethodPayload>, ?, List<MethodData<TClassPayload, TMethodPayload>>> methods()
    {
        return Collectors.collectingAndThen(
                SetsUtil.methods(),
                ArrayList::new
        );
    }

    public static <TClassPayload, TFieldPayload>  Collector<FieldData<TClassPayload, TFieldPayload>, ?, List<FieldData<TClassPayload, TFieldPayload>>> fields()
    {
        return Collectors.collectingAndThen(
                SetsUtil.fields(),
                ArrayList::new
        );
    }

    public static <TClassPayload, TMethodPayload, TParameterPayload>  Collector<ParameterData<TClassPayload, TMethodPayload, TParameterPayload>, ?, List<ParameterData<TClassPayload, TMethodPayload, TParameterPayload>>> parameters()
    {
        return Collectors.collectingAndThen(
                SetsUtil.parameters(),
                ArrayList::new
        );
    }
}
