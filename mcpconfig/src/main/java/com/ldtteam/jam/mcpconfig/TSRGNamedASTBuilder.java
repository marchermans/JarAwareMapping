package com.ldtteam.jam.mcpconfig;

import com.ldtteam.jam.ast.*;
import com.ldtteam.jam.spi.ast.named.builder.*;
import com.ldtteam.jam.spi.ast.named.builder.factory.INamedASTBuilderFactory;
import com.ldtteam.jam.spi.name.INameProvider;
import com.ldtteam.jam.spi.name.INotObfuscatedFilter;
import com.ldtteam.jam.spi.name.IRemapper;

import java.nio.file.Path;

public class TSRGNamedASTBuilder {

    public static final String[] DONT_OBFUSCATE_ANNOTATIONS = new String[] {
            "Lcom/mojang/blaze3d/DontObfuscate;",
            "Lnet/minecraft/obfuscate/DontObfuscate;"
    };

    public static INamedASTBuilderFactory ast(final Path inputMappingPath) {
        return (nameByLoadedASMData, remapperByName) -> {
            final IRemapper officialToObfuscatedRemapper = TSRGRemapper.createOfficialToObfuscated(inputMappingPath);
            final IRemapper obfuscatedToOfficialRemapper = TSRGRemapper.createObfuscatedToOfficial(inputMappingPath);
            final INamedClassBuilder classBuilder = classes(inputMappingPath);

            return NamedASTBuilder.create(officialToObfuscatedRemapper, obfuscatedToOfficialRemapper, classBuilder);
        };
    }

    public static INamedClassBuilder classes(final Path inputMappingPath) {
        final IRemapper officialToObfuscatedRemapper = TSRGRemapper.createOfficialToObfuscated(inputMappingPath);
        final INameProvider<NamedClassBuilder.ClassNamingInformation> classNameProvider = TSRGIdentityNameProvider.classes();
        final INamedFieldBuilder fieldBuilder = fields(inputMappingPath);
        final INamedMethodBuilder methodBuilder = methods(inputMappingPath);

        return NamedClassBuilder.create(
                officialToObfuscatedRemapper,
                classNameProvider,
                fieldBuilder,
                methodBuilder,
                INotObfuscatedFilter.notObfuscatedClassIfAnnotatedBy(DONT_OBFUSCATE_ANNOTATIONS)
        );
    }

    public static INamedFieldBuilder fields(final Path inputMappingPath) {
        final IRemapper officialToObfuscatedRemapper = TSRGRemapper.createOfficialToObfuscated(inputMappingPath);
        final INameProvider<NamedFieldBuilder.FieldNamingInformation> fieldNameProvider = TSRGIdentityNameProvider.fields();

        return NamedFieldBuilder.create(
                officialToObfuscatedRemapper,
                fieldNameProvider,
                INotObfuscatedFilter.notObfuscatedClassIfAnnotatedBy(DONT_OBFUSCATE_ANNOTATIONS),
                INotObfuscatedFilter.notObfuscatedFieldIfAnnotatedBy(DONT_OBFUSCATE_ANNOTATIONS)
        );
    }

    public static INamedMethodBuilder methods(final Path inputMappingPath) {
        final IRemapper officialToObfuscatedRemapper = TSRGRemapper.createOfficialToObfuscated(inputMappingPath);
        final IRemapper metadataToOfficialRemapper = TSRGRemapper.createObfuscatedToOfficial(inputMappingPath);
        final INameProvider<NamedMethodBuilder.MethodNamingInformation> methodNameProvider = TSRGIdentityNameProvider.methods();
        final INamedParameterBuilder parameterBuilder = parameters(inputMappingPath);

        return NamedMethodBuilder.create(
                officialToObfuscatedRemapper,
                metadataToOfficialRemapper,
                methodNameProvider,
                parameterBuilder,
                INotObfuscatedFilter.notObfuscatedClassIfAnnotatedBy(DONT_OBFUSCATE_ANNOTATIONS),
                INotObfuscatedFilter.notObfuscatedMethodIfAnnotatedBy(DONT_OBFUSCATE_ANNOTATIONS)
        );
    }

    public static INamedParameterBuilder parameters(final Path inputMappingPath) {
        final IRemapper officialToObfuscatedRemapper = TSRGRemapper.createOfficialToObfuscated(inputMappingPath);
        final INameProvider<NamedParameterBuilder.ParameterNamingInformation> parameterNameProvider = TSRGIdentityNameProvider.parameters();

        return NamedParameterBuilder.create(
                officialToObfuscatedRemapper,
                parameterNameProvider
        );
    }
}