package com.ldtteam.jam.spi.writer;

import com.google.common.collect.BiMap;
import com.ldtteam.jam.spi.asm.IASMData;
import com.ldtteam.jam.spi.ast.named.INamedAST;
import com.ldtteam.jam.spi.ast.named.builder.INamedASTBuilder;
import com.ldtteam.jam.spi.metadata.IMetadataProvider;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

import java.nio.file.Path;

public final class ASTBasedWriter implements IOutputWriter {

    public IOutputWriter create(INamedASTBuilder astBuilder, INamedASTOutputWriter delegate, IMetadataProvider provider) {
        return new ASTBasedWriter(astBuilder, delegate, provider);
    }

    private final INamedASTBuilder astBuilder;
    private final INamedASTOutputWriter delegate;
    private final IMetadataProvider provider;

    private ASTBasedWriter(INamedASTBuilder astBuilder, INamedASTOutputWriter delegate, IMetadataProvider provider) {
        this.astBuilder = astBuilder;
        this.delegate = delegate;
        this.provider = provider;
    }

    @Override
    public void write(Path outputDirectory, BiMap<ClassNode, Integer> classIds, BiMap<MethodNode, Integer> methodIds, BiMap<FieldNode, Integer> fieldIds, BiMap<ParameterNode, Integer> parameterIds, IASMData asmData) {
        final INamedAST namedAST = astBuilder.build(
                classIds,
                methodIds,
                fieldIds,
                parameterIds,
                asmData,
                provider.getAST()
        );

        delegate.write(outputDirectory, namedAST);
    }
}
