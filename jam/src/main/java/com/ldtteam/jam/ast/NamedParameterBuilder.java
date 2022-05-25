package com.ldtteam.jam.ast;

import com.google.common.collect.BiMap;
import com.ldtteam.jam.spi.ast.metadata.IMetadataClass;
import com.ldtteam.jam.spi.ast.metadata.IMetadataRecordComponent;
import com.ldtteam.jam.spi.ast.named.INamedParameter;
import com.ldtteam.jam.spi.ast.named.builder.INamedParameterBuilder;
import com.ldtteam.jam.spi.name.INameProvider;
import com.ldtteam.jam.spi.name.IRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class NamedParameterBuilder implements INamedParameterBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(NamedParameterBuilder.class);

    public static INamedParameterBuilder create(
            final IRemapper runtimeToASTRemapper,
            final INameProvider<Integer> parameterIdToNameProvider
    ){
        return new NamedParameterBuilder(runtimeToASTRemapper, parameterIdToNameProvider);
    }

    private final IRemapper runtimeToASTRemapper;
    private final INameProvider<Integer> parameterNameProvider;

    private NamedParameterBuilder(IRemapper runtimeToASTRemapper, INameProvider<Integer> parameterNameProvider) {
        this.runtimeToASTRemapper = runtimeToASTRemapper;
        this.parameterNameProvider = parameterNameProvider;
    }

    @Override
    public INamedParameter build(
            final ClassNode classNode,
            final MethodNode methodNode,
            final ParameterNode parameterNode,
            final int index,
            final IMetadataClass classMetadata,
            final Map<String, String> identifiedFieldNamesByASTName,
            final BiMap<ParameterNode, Integer> parameterIds
    ) {
        final int parameterId = parameterIds.get(parameterNode);
        final String obfuscatedParameterName = runtimeToASTRemapper.remapParameter(
                classNode.name,
                methodNode.name,
                methodNode.desc,
                parameterNode.name,
                index
        ).orElseThrow(() -> new IllegalStateException("Missing the parameter mapping."));

        String mappedParameterName = null;



        if (methodNode.name.equals("<init>") &&
                classMetadata.getMethodsByName() != null &&
                classMetadata.getRecords() != null &&
                !classMetadata.getRecords().isEmpty() &&
                methodNode.parameters.size() == classMetadata.getRecords().size()
        ) {
            final IMetadataRecordComponent recordInfo = classMetadata.getRecords().get(index);
            if (identifiedFieldNamesByASTName.containsKey(recordInfo.getField())) {
                mappedParameterName = identifiedFieldNamesByASTName.get(recordInfo.getField());
                LOGGER.debug("Remapped parameter of %s records constructor for id: %d to: %s".formatted(classNode.name, parameterId, mappedParameterName));
            }
        }

        if (mappedParameterName == null) {
            mappedParameterName = parameterNameProvider.getName(parameterId);
        }

        return new NamedParameter(
                obfuscatedParameterName,
                mappedParameterName,
                parameterId,
                index
        );
    }

    private record NamedParameter(String originalName, String identifiedName, int id, int index) implements INamedParameter {}
}
