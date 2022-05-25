package com.ldtteam.jam.spi.metadata;

import com.ldtteam.jam.spi.ast.metadata.IMetadataAST;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Provides jar metadata which the system can use to process the mappings.
 */
public interface IMetadataProvider {

    /**
     * Gives access to the syntax tree built from metadata that this provider can provide.
     *
     * @return The metadata AST.
     */
    @NonNull
    IMetadataAST getAST();
}
