package com.ldtteam.jam.spi.name;

import com.ldtteam.jam.spi.asm.ClassData;
import com.ldtteam.jam.spi.asm.FieldData;
import com.ldtteam.jam.spi.asm.MethodData;
import com.ldtteam.jam.spi.asm.ParameterData;

import java.util.function.Function;

@FunctionalInterface
public interface INameProvider<T> extends Function<T, String>
{
    static INameProvider<ClassData> classes() {
        return data -> data.node().name;
    }

    static INameProvider<MethodData> methods() {
        return data -> data.node().name + data.node().desc;
    }

    static INameProvider<FieldData> fields() {
        return data -> data.node().name;
    }

    static INameProvider<ParameterData> parameters() {
        return data -> data.node().name;
    }

    String getName(T t);

    @Override
    default String apply(T t) {
        return getName(t);
    }
}
