package com.ldtteam.jam.spi.ast.named.builder;

import com.google.common.collect.BiMap;
import com.ldtteam.jam.spi.ast.metadata.IMetadataClass;
import com.ldtteam.jam.spi.ast.named.INamedField;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

public interface INamedFieldBuilder {
    INamedField build(
            ClassNode classNode,
            FieldNode fieldNode,
            IMetadataClass classMetadata,
            BiMap<FieldNode, Integer> fieldIds
    );
}
