package com.ldtteam.jam.mcpconfig;

import com.ldtteam.jam.spi.asm.ClassData;
import com.ldtteam.jam.spi.asm.FieldData;
import com.ldtteam.jam.spi.asm.MethodData;
import com.ldtteam.jam.spi.asm.ParameterData;
import com.ldtteam.jam.spi.name.IExistingNameSupplier;
import com.machinezoo.noexception.Exceptions;
import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.INamedMappingFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public class TSRGExistingNameSupplier implements IExistingNameSupplier {

    public static IExistingNameSupplier create(final Path obfuscatedToSrgFile, final Path obfuscatedToRuntimeFile) {
        return new TSRGExistingNameSupplier(obfuscatedToSrgFile, obfuscatedToRuntimeFile);
    }

    private final IMappingFile remapper;

    private TSRGExistingNameSupplier(final Path obfuscatedToSrgFile, final Path obfuscatedToRuntimeFile) {
        final INamedMappingFile obfuscatedToSrg = Exceptions.sneak().get(() -> INamedMappingFile.load(Files.newInputStream(obfuscatedToSrgFile)));
        final INamedMappingFile obfuscatedToRuntime = Exceptions.sneak().get(() -> INamedMappingFile.load(Files.newInputStream(obfuscatedToRuntimeFile)));

        final IMappingFile runtimeToObfuscated = obfuscatedToRuntime.getMap("left", "right");

        this.remapper = runtimeToObfuscated.chain(obfuscatedToSrg.getMap("obf", "srg"));
    }

    @Override
    public Optional<String> getClassName(final ClassData classData) {
        final String remapped = this.remapper.remapClass(classData.node().name);
        if (Objects.equals(remapped, classData.node().name)) {
            return Optional.empty();
        }

        return Optional.of(remapped);
    }

    @Override
    public Optional<String> getFieldName(final FieldData fieldData) {
        final Optional<String> remapped = Optional.ofNullable(this.remapper.getClass(fieldData.owner().node().name))
                .map(c -> c.remapField(fieldData.node().name));

        return remapped.filter(name -> !Objects.equals(name, fieldData.node().name));
    }

    @Override
    public Optional<String> getMethodName(final MethodData methodData) {
        final Optional<String> remapped = Optional.ofNullable(remapper.getClass(methodData.owner().node().name))
                .map(c -> c.remapMethod(methodData.node().name, methodData.node().desc));

        return remapped.filter(name -> !Objects.equals(name, methodData.node().name));
    }

    @Override
    public Optional<String> getParameterName(final ParameterData parameterData) {
        final Optional<String> remapped = Optional.ofNullable(remapper.getClass(parameterData.classOwner().node().name))
                .map(c -> c.getMethod(parameterData.owner().node().name, parameterData.owner().node().desc))
                .map(m -> m.remapParameter(parameterData.index(), parameterData.node().name));

        return remapped.filter(name -> !Objects.equals(name, parameterData.node().name));
    }
}
