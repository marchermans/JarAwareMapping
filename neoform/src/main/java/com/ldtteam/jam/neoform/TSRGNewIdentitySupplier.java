package com.ldtteam.jam.neoform;

import com.ldtteam.jam.spi.asm.ClassData;
import com.ldtteam.jam.spi.asm.FieldData;
import com.ldtteam.jam.spi.asm.MethodData;
import com.ldtteam.jam.spi.asm.ParameterData;
import com.ldtteam.jam.spi.identification.INewIdentitySupplier;
import com.machinezoo.noexception.Exceptions;
import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.INamedMappingFile;

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
           .orElse(-1) + 1;
    }

    @Override
    public int getClassIdentity(final ClassData classData)
    {
        return nextFreeId++;
    }

    @Override
    public int getMethodIdentity(final MethodData methodData)
    {
        return nextFreeId++;
    }

    @Override
    public int getFieldIdentity(final FieldData fieldData)
    {
        return nextFreeId++;
    }

    @Override
    public int getParameterIdentity(final ParameterData parameterData)
    {
        return nextFreeId++;
    }
}
