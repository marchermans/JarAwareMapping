package com.ldtteam.jam.spi.ast.named.builder;

import com.google.common.collect.BiMap;
import com.ldtteam.jam.spi.ast.metadata.IMetadataAST;
import com.ldtteam.jam.spi.ast.named.INamedClass;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

import java.util.Map;

public interface INamedClassBuilder {
    INamedClass build(
            ClassNode classNode,
            IMetadataAST metadataAST,
            Map<String, ClassNode> classNodesByAstName,
            Map<MethodNode, MethodNode> rootMethodsByOverride,
            BiMap<ClassNode, Integer> classIds,
            BiMap<FieldNode, Integer> fieldIds,
            BiMap<MethodNode, Integer> methodIds,
            BiMap<ParameterNode, Integer> parameterIds
    );
}
