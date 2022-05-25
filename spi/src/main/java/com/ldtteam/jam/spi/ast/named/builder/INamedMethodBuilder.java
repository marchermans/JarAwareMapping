package com.ldtteam.jam.spi.ast.named.builder;

import com.google.common.collect.BiMap;
import com.ldtteam.jam.spi.ast.metadata.IMetadataClass;
import com.ldtteam.jam.spi.ast.named.INamedMethod;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

import java.util.Map;

public interface INamedMethodBuilder {
    INamedMethod build(
            ClassNode classNode,
            MethodNode methodNode,
            IMetadataClass classMetadata,
            Map<String, ClassNode> classNodesByAstName,
            Map<MethodNode, MethodNode> rootMethodsByOverride, Map<String, String> identifiedFieldNamesByASTName, BiMap<MethodNode, Integer> methodIds,
            BiMap<ParameterNode, Integer> parameterIds
    );
}
