package com.ldtteam.jam.mcpconfig;

import com.google.common.collect.BiMap;
import com.ldtteam.jam.ast.*;
import com.ldtteam.jam.spi.asm.IASMData;
import com.ldtteam.jam.spi.ast.named.builder.*;
import com.ldtteam.jam.spi.ast.named.builder.factory.INamedASTBuilderFactory;
import com.ldtteam.jam.spi.name.IExistingNameSupplier;
import com.ldtteam.jam.spi.name.INameProvider;
import com.ldtteam.jam.spi.name.IRemapper;

import java.nio.file.Path;
import java.util.Optional;

public class TSRGNamedASTBuilder {

    public static INamedASTBuilderFactory ast(final Path inputMappingPath) {
        return (nameByLoadedASMData, remapperByName) -> {
            final IRemapper officialToObfuscatedRemapper = TSRGRemapper.createOfficialToObfuscated(inputMappingPath);
            final IRemapper obfuscatedToOfficialRemapper = TSRGRemapper.createObfuscatedToOfficial(inputMappingPath);
            final INamedClassBuilder classBuilder = classes(inputMappingPath, nameByLoadedASMData, remapperByName);

            return NamedASTBuilder.create(officialToObfuscatedRemapper, obfuscatedToOfficialRemapper, classBuilder);
        };
    }

    public static INamedClassBuilder classes(final Path inputMappingPath, final BiMap<IASMData, String> nameByLoadedASMData, final BiMap<String, Optional<IExistingNameSupplier>> remapperByName) {
        final IRemapper officialToObfuscatedRemapper = TSRGRemapper.createOfficialToObfuscated(inputMappingPath);
        final INameProvider<NamedClassBuilder.ClassNamingInformation> classNameProvider = TSRGIdentityNameProvider.classes(nameByLoadedASMData, remapperByName);
        final INamedFieldBuilder fieldBuilder = fields(inputMappingPath, nameByLoadedASMData, remapperByName);
        final INamedMethodBuilder methodBuilder = methods(inputMappingPath, nameByLoadedASMData, remapperByName);

        return NamedClassBuilder.create(
                officialToObfuscatedRemapper,
                classNameProvider,
                fieldBuilder,
                methodBuilder
        );
    }

    public static INamedFieldBuilder fields(final Path inputMappingPath, final BiMap<IASMData, String> nameByLoadedASMData, final BiMap<String, Optional<IExistingNameSupplier>> remapperByName) {
        final IRemapper officialToObfuscatedRemapper = TSRGRemapper.createOfficialToObfuscated(inputMappingPath);
        final INameProvider<NamedFieldBuilder.FieldNamingInformation> fieldNameProvider = TSRGIdentityNameProvider.fields(nameByLoadedASMData, remapperByName);

        return NamedFieldBuilder.create(
                officialToObfuscatedRemapper,
                fieldNameProvider
        );
    }

    public static INamedMethodBuilder methods(final Path inputMappingPath, final BiMap<IASMData, String> nameByLoadedASMData, final BiMap<String, Optional<IExistingNameSupplier>> remapperByName) {
        final IRemapper officialToObfuscatedRemapper = TSRGRemapper.createOfficialToObfuscated(inputMappingPath);
        final IRemapper metadataToOfficialRemapper = TSRGRemapper.createObfuscatedToOfficial(inputMappingPath);
        final INameProvider<NamedMethodBuilder.MethodNamingInformation> methodNameProvider = TSRGIdentityNameProvider.methods(nameByLoadedASMData, remapperByName);
        final INamedParameterBuilder parameterBuilder = parameters(inputMappingPath, nameByLoadedASMData, remapperByName);

        return NamedMethodBuilder.create(
                officialToObfuscatedRemapper,
                metadataToOfficialRemapper,
                methodNameProvider,
                parameterBuilder
        );
    }

    public static INamedParameterBuilder parameters(final Path inputMappingPath, final BiMap<IASMData, String> nameByLoadedASMData, final BiMap<String, Optional<IExistingNameSupplier>> remapperByName) {
        final IRemapper officialToObfuscatedRemapper = TSRGRemapper.createOfficialToObfuscated(inputMappingPath);
        final INameProvider<NamedParameterBuilder.ParameterNamingInformation> parameterNameProvider = TSRGIdentityNameProvider.parameters(nameByLoadedASMData, remapperByName);

        return NamedParameterBuilder.create(
                officialToObfuscatedRemapper,
                parameterNameProvider
        );
    }
}