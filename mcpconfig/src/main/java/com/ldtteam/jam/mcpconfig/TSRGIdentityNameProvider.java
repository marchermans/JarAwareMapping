package com.ldtteam.jam.mcpconfig;

import com.ldtteam.jam.ast.NamedClassBuilder;
import com.ldtteam.jam.ast.NamedFieldBuilder;
import com.ldtteam.jam.ast.NamedMethodBuilder;
import com.ldtteam.jam.ast.NamedParameterBuilder;
import com.ldtteam.jam.spi.name.INameProvider;

import java.util.function.Function;

public class TSRGIdentityNameProvider<T, N> implements INameProvider<T> {

    public static INameProvider<NamedClassBuilder.ClassNamingInformation> classes() {
        return new TSRGIdentityNameProvider<>(
                (data) -> "net/minecraft/src/C_%d_".formatted(data.id())
        );
    }

    public static INameProvider<NamedFieldBuilder.FieldNamingInformation> fields() {
        return new TSRGIdentityNameProvider<>(
                (data) -> "f_%d_".formatted(data.id())
        );
    }

    public static INameProvider<NamedMethodBuilder.MethodNamingInformation> methods() {
        return new TSRGIdentityNameProvider<>(
                (data) -> "m_%d_".formatted(data.id())
        );
    }

    public static INameProvider<NamedParameterBuilder.ParameterNamingInformation> parameters() {
        return new TSRGIdentityNameProvider<>(
                (data) -> "p_%d_".formatted(data.id())
        );
    }

    private final Function<T, String> defaultNameFormatter;

    private TSRGIdentityNameProvider(final Function<T, String> defaultNameFormatter) {
        this.defaultNameFormatter = defaultNameFormatter;
    }


    @Override
    public String getName(final T t) {
        return defaultNameFormatter.apply(t);
    }
}
