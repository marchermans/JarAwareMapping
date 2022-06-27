package com.ldtteam.jam.mcpconfig;

import com.google.common.collect.BiMap;
import com.ldtteam.jam.ast.NamedClassBuilder;
import com.ldtteam.jam.ast.NamedFieldBuilder;
import com.ldtteam.jam.ast.NamedMethodBuilder;
import com.ldtteam.jam.ast.NamedParameterBuilder;
import com.ldtteam.jam.spi.asm.*;
import com.ldtteam.jam.spi.name.IExistingNameSupplier;
import com.ldtteam.jam.spi.name.INameProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public class TSRGIdentityNameProvider<T, N> implements INameProvider<T> {

    public static INameProvider<NamedClassBuilder.ClassNamingInformation> classes(final BiMap<IASMData, String> nameByLoadedASMData,
                                                                                  final BiMap<String, Optional<IExistingNameSupplier>> existingNameSuppliersByName) {
        return new TSRGIdentityNameProvider<>(
                classExistingNameSupplier(nameByLoadedASMData, existingNameSuppliersByName),
                (data) -> "net/minecraft/src/C_%d_".formatted(data.id()),
                NamedClassBuilder.ClassNamingInformation::mappedFrom,
                IExistingNameSupplier::getClassName
        );
    }

    public static INameProvider<NamedFieldBuilder.FieldNamingInformation> fields(
            final BiMap<IASMData, String> nameByLoadedASMData,
            final BiMap<String, Optional<IExistingNameSupplier>> existingNameSuppliersByName) {
        return new TSRGIdentityNameProvider<>(
                fieldExistingNameSupplier(nameByLoadedASMData, existingNameSuppliersByName),
                (data) -> "f_%d_".formatted(data.id()),
                NamedFieldBuilder.FieldNamingInformation::mappedFrom,
                IExistingNameSupplier::getFieldName
        );
    }

    public static INameProvider<NamedMethodBuilder.MethodNamingInformation> methods(final BiMap<IASMData, String> nameByLoadedASMData,
                                                                                    final BiMap<String, Optional<IExistingNameSupplier>> existingNameSuppliersByName) {
        return new TSRGIdentityNameProvider<>(
                methodExistingNameSupplier(nameByLoadedASMData, existingNameSuppliersByName),
                (data) -> "m_%d_".formatted(data.id()),
                NamedMethodBuilder.MethodNamingInformation::mappedFrom,
                IExistingNameSupplier::getMethodName
        );
    }

    public static INameProvider<NamedParameterBuilder.ParameterNamingInformation> parameters(final BiMap<IASMData, String> nameByLoadedASMData,
                                                                                             final BiMap<String, Optional<IExistingNameSupplier>> existingNameSuppliersByName) {
        return new TSRGIdentityNameProvider<>(
                parameterExistingNameSupplier(nameByLoadedASMData, existingNameSuppliersByName),
                (data) -> "p_%d_".formatted(data.id()),
                NamedParameterBuilder.ParameterNamingInformation::mappedFrom,
                IExistingNameSupplier::getParameterName
        );
    }

    private static Map<ClassData, Optional<IExistingNameSupplier>> classExistingNameSupplier(final BiMap<IASMData, String> nameByLoadedASMData,
                                                                                             final BiMap<String, Optional<IExistingNameSupplier>> existingNameSuppliersByName) {
        return existingNameSupplier(IASMData::classes, nameByLoadedASMData, existingNameSuppliersByName);
    }

    private static Map<FieldData, Optional<IExistingNameSupplier>> fieldExistingNameSupplier(final BiMap<IASMData, String> nameByLoadedASMData,
                                                                                             final BiMap<String, Optional<IExistingNameSupplier>> existingNameSuppliersByName) {
        return existingNameSupplier(IASMData::fields, nameByLoadedASMData, existingNameSuppliersByName);
    }

    private static Map<MethodData, Optional<IExistingNameSupplier>> methodExistingNameSupplier(final BiMap<IASMData, String> nameByLoadedASMData,
                                                                                               final BiMap<String, Optional<IExistingNameSupplier>> existingNameSuppliersByName) {
        return existingNameSupplier(IASMData::methods, nameByLoadedASMData, existingNameSuppliersByName);
    }

    private static Map<ParameterData, Optional<IExistingNameSupplier>> parameterExistingNameSupplier(final BiMap<IASMData, String> nameByLoadedASMData,
                                                                                                 final BiMap<String, Optional<IExistingNameSupplier>> existingNameSuppliersByName) {
        return existingNameSupplier(IASMData::parameters, nameByLoadedASMData, existingNameSuppliersByName);
    }

    private static <N> Map<N, Optional<IExistingNameSupplier>> existingNameSupplier(
            final Function<IASMData, Set<N>> nodesSelector,
            final BiMap<IASMData, String> nameByLoadedASMData,
            final BiMap<String, Optional<IExistingNameSupplier>> existingNameSuppliersByName) {
        final Map<N, Optional<IExistingNameSupplier>> result = new HashMap<>();
        nameByLoadedASMData.forEach((data, name) -> {
            final Set<N> nodes = nodesSelector.apply(data);
            final Optional<IExistingNameSupplier> nameSupplier = existingNameSuppliersByName.getOrDefault(name, Optional.empty());
            nodes.forEach(node -> result.put(node, nameSupplier));
        });
        return result;
    }

    private final Map<N, Optional<IExistingNameSupplier>> nameSuppliersForNode;

    private final Function<T, String> defaultNameFormatter;
    private final Function<T, N> mappedFromGetter;
    private final BiFunction<IExistingNameSupplier, N, Optional<String>> existingNameFormatter;

    private TSRGIdentityNameProvider(final Map<N, Optional<IExistingNameSupplier>> nameSuppliersForNode,
                                     final Function<T, String> defaultNameFormatter,
                                     final Function<T, N> mappedFromGetter,
                                     final BiFunction<IExistingNameSupplier, N, Optional<String>> existingNameFormatter) {
        this.nameSuppliersForNode = nameSuppliersForNode;
        this.defaultNameFormatter = defaultNameFormatter;
        this.mappedFromGetter = mappedFromGetter;
        this.existingNameFormatter = existingNameFormatter;
    }


    @Override
    public String getName(final T t) {
        final String defaultName = defaultNameFormatter.apply(t);
        if (mappedFromGetter.apply(t) == null) {
            return defaultName;
        }

        final Optional<IExistingNameSupplier> remapper = nameSuppliersForNode.get(mappedFromGetter.apply(t));
        if (remapper.isEmpty()) {
            return defaultName;
        }

        final Optional<String> existingName = existingNameFormatter.apply(remapper.get(), mappedFromGetter.apply(t));
        return existingName.orElse(defaultName);
    }
}
