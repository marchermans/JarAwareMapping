package com.ldtteam.jam.ast;

import com.google.common.collect.BiMap;
import com.google.common.collect.Maps;
import com.ldtteam.jam.spi.ast.metadata.IMetadataAST;
import com.ldtteam.jam.spi.ast.metadata.IMetadataClass;
import com.ldtteam.jam.spi.ast.named.INamedClass;
import com.ldtteam.jam.spi.ast.named.INamedField;
import com.ldtteam.jam.spi.ast.named.INamedMethod;
import com.ldtteam.jam.spi.ast.named.builder.INamedClassBuilder;
import com.ldtteam.jam.spi.ast.named.builder.INamedFieldBuilder;
import com.ldtteam.jam.spi.ast.named.builder.INamedMethodBuilder;
import com.ldtteam.jam.spi.name.INameProvider;
import com.ldtteam.jam.spi.name.IRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

import java.util.*;

public class NamedClassBuilder implements com.ldtteam.jam.spi.ast.named.builder.INamedClassBuilder {

    public static INamedClassBuilder create(
            IRemapper runtimeToASTRemapper, INameProvider<Integer> classNameProvider, INamedFieldBuilder namedFieldBuilder, INamedMethodBuilder namedMethodBuilder
    ) {
        return new NamedClassBuilder(runtimeToASTRemapper, classNameProvider, namedFieldBuilder, namedMethodBuilder);
    }

    private final IRemapper runtimeToASTRemapper;
    private final INameProvider<Integer> classNameProvider;
    private final INamedFieldBuilder namedFieldBuilder;
    private final INamedMethodBuilder namedMethodBuilder;

    private NamedClassBuilder(IRemapper runtimeToASTRemapper, INameProvider<Integer> classNameProvider, INamedFieldBuilder namedFieldBuilder, INamedMethodBuilder namedMethodBuilder) {
        this.runtimeToASTRemapper = runtimeToASTRemapper;
        this.classNameProvider = classNameProvider;
        this.namedFieldBuilder = namedFieldBuilder;
        this.namedMethodBuilder = namedMethodBuilder;
    }

    @Override
    public INamedClass build(
            final ClassNode classNode,
            final IMetadataAST metadataAST,
            final Map<String, ClassNode> classNodesByAstName,
            final Map<MethodNode, MethodNode> rootMethodsByOverride,
            final BiMap<ClassNode, Integer> classIds,
            final BiMap<FieldNode, Integer> fieldIds,
            final BiMap<MethodNode, Integer> methodIds,
            final BiMap<ParameterNode, Integer> parameterIds
    ) {
        final String originalClassName = runtimeToASTRemapper.remapClass(classNode.name)
                .orElseThrow(() -> new IllegalStateException("Failed to remap class: %s".formatted(classNode.name)));

        final IMetadataClass classMetadata = metadataAST.getClassesByName().computeIfAbsent(originalClassName, (key) -> {
            throw new IllegalStateException("Missing metadata for %s (%s)".formatted(classNode.name, key));
        });

        final Map<String, String> identifiedFieldNamesByOriginalFieldName = Maps.newHashMap();
        final Collection<INamedField> fields = new ArrayList<>();
        classNode.fields.forEach(fieldNode -> {
            final INamedField field = namedFieldBuilder.build(
                    classNode,
                    fieldNode,
                    classMetadata,
                    fieldIds
            );

            fields.add(field);
            identifiedFieldNamesByOriginalFieldName.put(field.originalName(), field.identifiedName());
        });

        final Collection<INamedMethod> methods = new ArrayList<>();
        classNode.methods.forEach(methodNode -> {
            final INamedMethod method = namedMethodBuilder.build(
                    classNode,
                    methodNode,
                    classMetadata,
                    classNodesByAstName,
                    rootMethodsByOverride,
                    identifiedFieldNamesByOriginalFieldName,
                    methodIds,
                    parameterIds
            );

            methods.add(method);
        });

        return new NamedClass(
                originalClassName,
                classNameProvider.getName(classIds.get(classNode)),
                classIds.get(classNode),
                fields,
                methods
        );
    }

    private record NamedClass(String originalName, String identifiedName, int id, Collection<INamedField> fields, Collection<INamedMethod> methods) implements INamedClass {}
}
