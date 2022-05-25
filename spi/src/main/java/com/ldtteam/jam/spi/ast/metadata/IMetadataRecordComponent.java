package com.ldtteam.jam.spi.ast.metadata;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

/**
 * Represents the record component information in the AST built from metadata.
 */
public interface IMetadataRecordComponent {

    /**
     * Gives access to the name of field which this record component targets.
     *
     * @return The name of the field which this record component targets.
     */
    @NonNull
    String getField();

    /**
     * Gives access to the type descriptor of the field which this record component targets.
     *
     * @return The descriptor of the field which this record component targets.
     */
    @NonNull
    String getDesc();

    /**
     * Gives access to the names of the methods that this record component targets.
     *
     * @return A collection of names of methods that this record component targets, null or empty if unknown. Potentially contains multiple methods if unclear which method is targeted.
     */
    @Nullable
    List<String> getMethods();
}
