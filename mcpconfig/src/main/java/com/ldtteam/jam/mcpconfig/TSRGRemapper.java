package com.ldtteam.jam.mcpconfig;

import com.ldtteam.jam.spi.name.IRemapper;
import com.machinezoo.noexception.Exceptions;
import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.INamedMappingFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class TSRGRemapper implements IRemapper
{

    public static IRemapper createObfuscatedToOfficial(Path path) {
        return new TSRGRemapper(path, "right", "left");
    }

    public static IRemapper createOfficialToObfuscated(Path path) {
        return new TSRGRemapper(path, "left", "right");
    }

    private TSRGRemapper(final Path mappingFilePath, final String sourceSide, final String targetSide) {
        INamedMappingFile mappingFile = Exceptions.sneak().get(() -> INamedMappingFile.load(Files.newInputStream(mappingFilePath)));
        remapper = mappingFile.getMap(sourceSide, targetSide);
    }

    private TSRGRemapper(final IMappingFile remapper) {
        this.remapper = remapper;
    }

    private final IMappingFile remapper;

    @Override
    public Optional<String> remapClass(final String className)
    {
        return Optional.ofNullable(remapper.remapClass(className));
    }

    @Override
    public Optional<String> remapMethod(final String className, final String name, final String descriptor)
    {
        return Optional.ofNullable(remapper.getClass(className))
                 .map(c -> c.remapMethod(name, descriptor));
    }

    @Override
    public Optional<String> remapDescriptor(String descriptor) {
        return Optional.ofNullable(remapper.remapDescriptor(descriptor));
    }

    @Override
    public Optional<String> remapField(final String className, final String name, final String type)
    {
        return Optional.ofNullable(remapper.getClass(className))
          .map(c -> c.remapField(name));
    }

    @Override
    public Optional<String> remapParameter(final String className, final String methodName, final String descriptor, final String parameterName, final int index)
    {
        return Optional.of("o");
    }

    @Override
    public Optional<String> remapPackage(final String packageName)
    {
        return Optional.ofNullable(remapper.remapPackage(packageName));
    }

    @Override
    public IRemapper reverse() {
        return new TSRGRemapper(remapper.reverse());
    }
}
