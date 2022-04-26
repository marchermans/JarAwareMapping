package com.ldtteam.jam.runtime;

import com.ldtteam.jam.spi.identification.INewIdentitySupplier;
import com.machinezoo.noexception.Exceptions;
import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.INamedMappingFile;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class TSRGNewIdentitySupplier implements INewIdentitySupplier
{

    public static INewIdentitySupplier create(final Path tsrgToIdFilePath) {
        return new TSRGNewIdentitySupplier(tsrgToIdFilePath);
    }

    private int nextFreeId;

    private TSRGNewIdentitySupplier(final Path tsrgToIdFilePath)
    {
        final INamedMappingFile tsrgToIdFile = Exceptions.sneak().get(() -> INamedMappingFile.load(Files.newInputStream(tsrgToIdFilePath)));
        final IMappingFile tsrgToId = tsrgToIdFile.getMap("srg", "id");

        nextFreeId = tsrgToId
          .getClasses()
          .stream().flatMap(
            c -> Stream.concat(Stream.concat(
                Stream.of(c.getMapped()),
                c.getMethods()
                  .stream()
                  .flatMap(
                    m -> Stream.concat(
                      Stream.of(m.getMapped()),
                      m.getParameters().stream().map(IMappingFile.INode::getMapped
                      )
                    )
                  )
              ),
              c.getFields().stream().map(IMappingFile.INode::getMapped)
            ))
           .mapToInt(Integer::parseInt)
           .max()
           .orElse(-1);
    }

    @Override
    public int getClassIdentity(final ClassNode classNode)
    {
        return nextFreeId++;
    }

    @Override
    public int getMethodIdentity(final ClassNode classNode, final MethodNode methodNode)
    {
        return nextFreeId++;
    }

    @Override
    public int getFieldIdentity(final ClassNode classNode, final FieldNode fieldNode)
    {
        return nextFreeId++;
    }

    @Override
    public int getParameterIdentity(final ClassNode classNode, final MethodNode methodNode, final ParameterNode parameterNode, final int index)
    {
        return nextFreeId++;
    }
}
