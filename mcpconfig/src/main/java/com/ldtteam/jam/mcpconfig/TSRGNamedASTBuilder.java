package com.ldtteam.jam.mcpconfig;

import com.ldtteam.jam.ast.*;
import com.ldtteam.jam.spi.ast.named.builder.*;
import com.ldtteam.jam.spi.name.INameProvider;
import com.ldtteam.jam.spi.name.IRemapper;

import java.nio.file.Path;

public class TSRGNamedASTBuilder {

    public static INamedASTBuilder AST(final Path inputMappingPath) {
        final IRemapper officialToObfuscatedRemapper = TSRGRemapper.createOfficialToObfuscated(inputMappingPath);
        final IRemapper obfuscatedToOfficialRemapper = TSRGRemapper.createObfuscatedToOfficial(inputMappingPath);
        final INamedClassBuilder classBuilder = classes(inputMappingPath);

        return NamedASTBuilder.create(
                officialToObfuscatedRemapper,
                obfuscatedToOfficialRemapper,
                classBuilder
        );
    }

    public static INamedClassBuilder classes(final Path inputMappingPath) {
        final IRemapper officialToObfuscatedRemapper = TSRGRemapper.createOfficialToObfuscated(inputMappingPath);
        final INameProvider<Integer> classNameProvider = TSRGIdentityNameProvider.classes();
        final INamedFieldBuilder fieldBuilder = fields(inputMappingPath);
        final INamedMethodBuilder methodBuilder = methods(inputMappingPath);

        return NamedClassBuilder.create(
                officialToObfuscatedRemapper,
                classNameProvider,
                fieldBuilder,
                methodBuilder
        );
    }

    public static INamedFieldBuilder fields(final Path inputMappingPath) {
        final IRemapper officialToObfuscatedRemapper = TSRGRemapper.createOfficialToObfuscated(inputMappingPath);
        final INameProvider<Integer> fieldNameProvider = TSRGIdentityNameProvider.fields();

        return NamedFieldBuilder.create(
                officialToObfuscatedRemapper,
                fieldNameProvider
        );
    }

    public static INamedMethodBuilder methods(final Path inputMappingPath) {
        final IRemapper officialToObfuscatedRemapper = TSRGRemapper.createOfficialToObfuscated(inputMappingPath);
        final INameProvider<Integer> methodNameProvider = TSRGIdentityNameProvider.methods();
        final INamedParameterBuilder parameterBuilder = parameters(inputMappingPath);

        return NamedMethodBuilder.create(
                officialToObfuscatedRemapper,
                methodNameProvider,
                parameterBuilder
        );
    }

    public static INamedParameterBuilder parameters(final Path inputMappingPath) {
        final IRemapper officialToObfuscatedRemapper = TSRGRemapper.createOfficialToObfuscated(inputMappingPath);
        final INameProvider<Integer> parameterNameProvider = TSRGIdentityNameProvider.parameters();

        return NamedParameterBuilder.create(
                officialToObfuscatedRemapper,
                parameterNameProvider
        );
    }
}