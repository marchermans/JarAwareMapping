package com.ldtteam.jam.spi.ast.named.builder;

import com.google.common.collect.BiMap;
import com.ldtteam.jam.spi.asm.IASMData;
import com.ldtteam.jam.spi.ast.metadata.IMetadataAST;
import com.ldtteam.jam.spi.ast.named.INamedAST;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

public interface INamedASTBuilder {
    INamedAST build(
            BiMap<ClassNode, Integer> classIds,
            BiMap<MethodNode, Integer> methodIds,
            BiMap<FieldNode, Integer> fieldIds,
            BiMap<ParameterNode, Integer> parameterIds,
            IASMData asmData,
            IMetadataAST metadataAST
    );
}
