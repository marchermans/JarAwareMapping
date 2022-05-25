package com.ldtteam.jam.mcpconfig;

import com.ldtteam.jam.spi.identification.IExistingIdentitySupplier;
import com.machinezoo.noexception.Exceptions;
import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.INamedMappingFile;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

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
    public int getClassIdentity(final ClassNode classNode)
    {
        return Integer.parseInt(this.officialToIdMapping.remapClass(classNode.name));
    }

    @Override
    public int getMethodIdentity(final ClassNode classNode, final MethodNode methodNode)
    {
        return Integer.parseInt(this.officialToIdMapping.getClass(classNode.name)
                 .remapMethod(methodNode.name, methodNode.desc));
    }

    @Override
    public int getFieldIdentity(final ClassNode classNode, final FieldNode fieldNode)
    {
        return Integer.parseInt(this.officialToIdMapping.getClass(classNode.name)
          .remapField(fieldNode.name));
    }

    @Override
    public int getParameterIdentity(final ClassNode classNode, final MethodNode methodNode, final ParameterNode parameterNode, final int index)
    {
        final String remappedClass = this.officialToObfuscatedMapping.remapClass(classNode.name);
        final String remappedMethod = this.officialToObfuscatedMapping.getClass(classNode.name).remapMethod(methodNode.name, methodNode.desc);
        final String remappedMethodDescriptor = this.officialToObfuscatedMapping.remapDescriptor(methodNode.desc);

        final String id = Objects.requireNonNull(this.obfuscatedToIdMapping.getClass(remappedClass)
            .getMethod(remappedMethod, remappedMethodDescriptor))
          .remapParameter(index, parameterNode.name);

        if (Objects.equals(id, parameterNode.name))
            return -1;

        return Integer.parseInt(id);
    }
}
