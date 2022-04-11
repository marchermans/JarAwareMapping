package com.ldtteam.jam.jammer;

import com.ldtteam.jam.jamspec.name.IRemapper;
import com.machinezoo.noexception.Exceptions;
import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.INamedMappingFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class TSRGMapper implements IRemapper
{
    public static IRemapper create(Path path) {
        return new TSRGMapper(path);
    }

    private TSRGMapper(Path mappingFilePath) {
        INamedMappingFile mappingFile = Exceptions.sneak().get(() -> INamedMappingFile.load(Files.newInputStream(mappingFilePath)));
        obfuscatedToOfficial = mappingFile.getMap("right", "left");
    }

    private final IMappingFile obfuscatedToOfficial;

    @Override
    public Optional<String> remapClass(final String className)
    {
        return Optional.ofNullable(obfuscatedToOfficial.remapClass(className));
    }

    @Override
    public Optional<String> remapMethod(final String className, final String name, final String descriptor)
    {
        return Optional.ofNullable(obfuscatedToOfficial.getClass(className))
                 .map(c -> c.remapMethod(name, descriptor));
    }

    @Override
    public Optional<String> remapField(final String className, final String name, final String type)
    {
        return Optional.ofNullable(obfuscatedToOfficial.getClass(className))
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
        return Optional.ofNullable(obfuscatedToOfficial.remapPackage(packageName));
    }
}
