package com.ldtteam.jam.ast;

import com.google.common.collect.BiMap;

import com.ldtteam.jam.spi.ast.metadata.IMetadataClass;
import com.ldtteam.jam.spi.ast.metadata.IMetadataField;
import com.ldtteam.jam.spi.ast.named.INamedField;
import com.ldtteam.jam.spi.ast.named.builder.INamedFieldBuilder;
import com.ldtteam.jam.spi.name.INameProvider;
import com.ldtteam.jam.spi.name.IRemapper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

public class NamedFieldBuilder implements com.ldtteam.jam.spi.ast.named.builder.INamedFieldBuilder {

    public static INamedFieldBuilder create(IRemapper runtimeToASTRemapper, INameProvider<Integer> fieldNameProvider) {
        return new NamedFieldBuilder(runtimeToASTRemapper, fieldNameProvider);
    }

    private static final int ACCESIBILITY_FLAG_FOR_ENUM_VALUES_FIELD = Opcodes.ACC_FINAL | Opcodes.ACC_ENUM | Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;

    private final IRemapper runtimeToASTRemapper;
    private final INameProvider<Integer> fieldNameProvider;
    protected NamedFieldBuilder(IRemapper runtimeToASTRemapper, INameProvider<Integer> fieldNameProvider) {
        this.runtimeToASTRemapper = runtimeToASTRemapper;
        this.fieldNameProvider = fieldNameProvider;
    }

    @Override
    public INamedField build(
            final ClassNode classNode,
            final FieldNode fieldNode,
            final IMetadataClass classMetadata,
            final BiMap<FieldNode, Integer> fieldIds
    ) {
        final String originalClassName = runtimeToASTRemapper.remapClass(classNode.name)
                .orElseThrow(() -> new IllegalStateException("Failed to remap class: %s".formatted(classNode.name)));
        final String originalFieldName = runtimeToASTRemapper.remapField(classNode.name, fieldNode.name, fieldNode.name)
                .orElseThrow(() -> new IllegalStateException("Failed to remap field: %s in class: %s".formatted(fieldNode.name, classNode.name)));

        if (classMetadata.getFieldsByName() == null) {
            throw new IllegalStateException("The given class: %s does not have any fields!".formatted(classNode.name));
        }

        String identifiedFieldName = null;

        final IMetadataField fieldMetadata = classMetadata.getFieldsByName().computeIfAbsent(originalFieldName, (key) -> {
            throw new IllegalStateException("Missing metadata for %s (%s) in %s".formatted(fieldNode.name, key, classNode.name));
        });

        if (fieldMetadata.getForce() != null)
        {
            identifiedFieldName = fieldMetadata.getForce();
        }

        if (identifiedFieldName == null && isEnumValuesField(fieldNode))
        {
            identifiedFieldName = fieldNode.name;
        }

        if (identifiedFieldName == null && originalClassName.equals(fieldNode.name))
        {
            identifiedFieldName = fieldNode.name;
        }

        if (identifiedFieldName == null)
        {
            identifiedFieldName = fieldNameProvider.getName(fieldIds.get(fieldNode));
        }

        return new NamedField(
                originalFieldName, identifiedFieldName, fieldIds.get(fieldNode)
        );
    }

    private boolean isEnumValuesField(FieldNode fieldNode) {
        return (fieldNode.access & ACCESIBILITY_FLAG_FOR_ENUM_VALUES_FIELD) == ACCESIBILITY_FLAG_FOR_ENUM_VALUES_FIELD || "$VALUES".equals(fieldNode.name);
    }

    private record NamedField(String originalName, String identifiedName, int id) implements INamedField {}
}
