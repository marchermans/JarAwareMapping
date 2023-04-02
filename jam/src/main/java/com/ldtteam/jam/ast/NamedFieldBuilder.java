package com.ldtteam.jam.ast;

import com.google.common.collect.BiMap;
import com.ldtteam.jam.spi.asm.ClassData;
import com.ldtteam.jam.spi.asm.FieldData;
import com.ldtteam.jam.spi.ast.metadata.IMetadataClass;
import com.ldtteam.jam.spi.ast.metadata.IMetadataField;
import com.ldtteam.jam.spi.ast.named.INamedField;
import com.ldtteam.jam.spi.ast.named.builder.INamedFieldBuilder;
import com.ldtteam.jam.spi.name.INameProvider;
import com.ldtteam.jam.spi.name.IRemapper;
import org.objectweb.asm.Opcodes;

public class NamedFieldBuilder implements INamedFieldBuilder
{
    public record FieldNamingInformation(FieldData target, FieldData mappedFrom, Integer id) {}

    public static INamedFieldBuilder create(IRemapper runtimeToASTRemapper, INameProvider<FieldNamingInformation> fieldNameProvider)
    {
        return new NamedFieldBuilder(runtimeToASTRemapper, fieldNameProvider);
    }

    private static final int ACCESIBILITY_FLAG_FOR_ENUM_VALUES_FIELD = Opcodes.ACC_FINAL | Opcodes.ACC_ENUM | Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;

    private final IRemapper              runtimeToASTRemapper;
    private final INameProvider<FieldNamingInformation> fieldNameProvider;

    protected NamedFieldBuilder(IRemapper runtimeToASTRemapper, INameProvider<FieldNamingInformation> fieldNameProvider)
    {
        this.runtimeToASTRemapper = runtimeToASTRemapper;
        this.fieldNameProvider = fieldNameProvider;
    }

    @Override
    public INamedField build(
      final ClassData classData,
      final FieldData fieldData,
      final IMetadataClass classMetadata,
      final BiMap<FieldData, FieldData> fieldMappings,
      final BiMap<FieldData, Integer> fieldIds
    )
    {
        final String originalClassName = runtimeToASTRemapper.remapClass(classData.node().name)
                                           .orElseThrow(() -> new IllegalStateException("Failed to remap class: %s".formatted(classData.node().name)));
        final String originalFieldName = runtimeToASTRemapper.remapField(classData.node().name, fieldData.node().name, fieldData.node().desc)
                                           .orElseThrow(() -> new IllegalStateException("Failed to remap field: %s in class: %s".formatted(fieldData.node().name, classData.node().name)));

        if (classMetadata.getFieldsByName() == null)
        {
            throw new IllegalStateException("The given class: %s does not have any fields!".formatted(classData.node().name));
        }

        String identifiedFieldName = null;

        final IMetadataField fieldMetadata = classMetadata.getFieldsByName().computeIfAbsent(originalFieldName, (key) -> {
            throw new IllegalStateException("Missing metadata for %s (%s) in %s".formatted(fieldData.node().name, key, classData.node().name));
        });

        if (fieldMetadata.getForce() != null)
        {
            identifiedFieldName = fieldMetadata.getForce();
        }

        if (identifiedFieldName == null && isEnumValuesField(fieldData))
        {
            identifiedFieldName = fieldData.node().name;
        }

        if (identifiedFieldName == null && originalFieldName.equals(fieldData.node().name))
        {
            identifiedFieldName = fieldData.node().name;
        }

        if (identifiedFieldName == null)
        {
            final FieldNamingInformation fieldNamingInformation = new FieldNamingInformation(
                    fieldData,
                    fieldMappings.get(fieldData),
                    fieldIds.get(fieldData)
            );
            identifiedFieldName = fieldNameProvider.getName(fieldNamingInformation);
        }

        return new NamedField(
          originalFieldName, identifiedFieldName, fieldIds.get(fieldData)
        );
    }

    private boolean isEnumValuesField(FieldData fieldData)
    {
        return (fieldData.node().access & ACCESIBILITY_FLAG_FOR_ENUM_VALUES_FIELD) == ACCESIBILITY_FLAG_FOR_ENUM_VALUES_FIELD || "$VALUES".equals(fieldData.node().name);
    }

    private record NamedField(String originalName, String identifiedName, int id) implements INamedField {}
}
