package com.ldtteam.jam.mcpconfig;

import com.ldtteam.jam.spi.asm.ClassData;
import com.ldtteam.jam.spi.asm.FieldData;
import com.ldtteam.jam.spi.asm.MethodData;
import com.ldtteam.jam.spi.asm.ParameterData;
import com.ldtteam.jam.spi.identification.IExistingIdentitySupplier;
import com.machinezoo.noexception.Exceptions;
import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.INamedMappingFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class TSRGIdentitySupplier implements IExistingIdentitySupplier
{

    public static IExistingIdentitySupplier create(final Path tsrgIdSourceMappingFile, final Path officialToObfuscatedMappingFile)
    {
        return new TSRGIdentitySupplier(tsrgIdSourceMappingFile, officialToObfuscatedMappingFile);
    }

    private final IMappingFile officialToObfuscatedMapping;
    private final IMappingFile officialToIdMapping;
    private final IMappingFile obfuscatedToIdMapping;

    private TSRGIdentitySupplier(final Path tsrgIdSourceMappingFile, final Path officialToObfuscatedMappingFile)
    {
        INamedMappingFile tsrgIdSourceMappingFile1 = Exceptions.sneak().get(() -> INamedMappingFile.load(Files.newInputStream(tsrgIdSourceMappingFile)));
        INamedMappingFile officialToObfuscatedMappingFile1 = Exceptions.sneak().get(() -> INamedMappingFile.load(Files.newInputStream(officialToObfuscatedMappingFile)));
        this.officialToObfuscatedMapping = officialToObfuscatedMappingFile1.getMap("left", "right");

        this.obfuscatedToIdMapping = tsrgIdSourceMappingFile1.getMap("obf", "id");
        this.officialToIdMapping = this.officialToObfuscatedMapping.chain(this.obfuscatedToIdMapping);
    }

    @Override
    public int getClassIdentity(final ClassData classData)
    {
        return Integer.parseInt(this.officialToIdMapping.remapClass(classData.node().name));
    }

    @Override
    public int getMethodIdentity(final MethodData methodData)
    {
        return Integer.parseInt(this.officialToIdMapping.getClass(methodData.owner().node().name)
                 .remapMethod(methodData.node().name, methodData.node().desc));
    }

    @Override
    public int getFieldIdentity(final FieldData fieldData)
    {
        return Integer.parseInt(this.officialToIdMapping.getClass(fieldData.owner().node().name)
          .remapField(fieldData.node().name));
    }

    @Override
    public int getParameterIdentity(final ParameterData parameterData)
    {
        final String remappedClass = this.officialToObfuscatedMapping.remapClass(parameterData.classOwner().node().name);
        final String remappedMethod = this.officialToObfuscatedMapping.getClass(parameterData.classOwner().node().name)
                .remapMethod(parameterData.owner().node().name, parameterData.owner().node().desc);
        final String remappedMethodDescriptor = this.officialToObfuscatedMapping.remapDescriptor(parameterData.owner().node().desc);

        final String id = Objects.requireNonNull(this.obfuscatedToIdMapping.getClass(remappedClass)
            .getMethod(remappedMethod, remappedMethodDescriptor))
          .remapParameter(parameterData.index(), parameterData.node().name);

        if (Objects.equals(id, parameterData.node().name))
            return -1;

        return Integer.parseInt(id);
    }
}
