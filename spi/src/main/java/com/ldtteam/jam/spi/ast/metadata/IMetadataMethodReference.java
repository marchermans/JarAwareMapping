package com.ldtteam.jam.spi.ast.metadata;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A method reference in the AST built from metadata.
 */
public interface IMetadataMethodReference {

    /**
     * Gives access to the name of the class which houses (owns) the method referenced by this reference.
     *
     * @return The name of the class which contains the referenced method.
     */
    @NonNull
    String getOwner();

    /**
     * Gives access to the name of the referenced method.
     *
     * @return The name of the referenced method.
     */
    @NonNull
    String getName();

    /**
     * Gives access to the descriptor of the referenced method.
     *
     * @return The descriptor of the referenced method.
     */
    @NonNull
    String getDesc();
}
