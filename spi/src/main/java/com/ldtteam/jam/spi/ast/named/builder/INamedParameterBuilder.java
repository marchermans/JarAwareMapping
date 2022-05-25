package com.ldtteam.jam.spi.ast.named.builder;

import com.google.common.collect.BiMap;
import com.ldtteam.jam.spi.ast.metadata.IMetadataClass;
import com.ldtteam.jam.spi.ast.named.INamedParameter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

import java.util.Map;

public interface INamedParameterBuilder {
    INamedParameter build(
            ClassNode classNode,
            MethodNode methodNode,
            ParameterNode parameterNode,
            int index,
            IMetadataClass classMetadata,
            Map<String, String> identifiedFieldNamesByASTName,
            BiMap<ParameterNode, Integer> parameterIds
    );
}
