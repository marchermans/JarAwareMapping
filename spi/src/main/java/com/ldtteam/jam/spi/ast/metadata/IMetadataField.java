package com.ldtteam.jam.spi.ast.metadata;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a field in a class in the AST build from metadata.
 */
public interface IMetadataField extends IMetadataWithAccessInformation {

    /**
     * Gives access to the type descriptor of the field.
     *
     * @return The type descriptor of the field, null or empty if unknown.
     */
    @Nullable
    String getDesc();

    /**
     * Gives access to the full generic signature of the field.
     *
     * @return The generic signature of the field, null or empty if unknown.
     */
    @Nullable
    String getSignature();

    /**
     * Gives access to the forced name of the field (for example enum entries).
     *
     * @return The forced name of the field, null or empty if unknown.
     */
    @Nullable
    String getForce();
}
