package com.ldtteam.jam.spi.ast.metadata;

import java.util.Map;

/**
 * Presents an AST of the jar build from metadata.
 */
public interface IMetadataAST {

    /**
     * The classes in the ast of the jar identifiable by name.
     *
     * @return The classes in the jar.
     */
    Map<String, ? extends IMetadataClass> getClassesByName();
}
