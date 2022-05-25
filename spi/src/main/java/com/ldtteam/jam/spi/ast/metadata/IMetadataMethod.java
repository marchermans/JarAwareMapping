package com.ldtteam.jam.spi.ast.metadata;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Set;

/**
 * Represents a methods in the AST built from metadata.
 */
public interface IMetadataMethod extends IMetadataWithAccessInformation {

    /**
     * Gives access to the generic signature of the method.
     *
     * @return The generic signature of the method, null or empty if unknown.
     */
    @Nullable
    String getSignature();

    /**
     * Gives access to the bounce information if this method is a bouncer.
     *
     * @return The bounce information of this method, in case it is a bouncer, null otherwise.
     */
    @Nullable
    IMetadataBounce getBouncer();

    /**
     * Gives access to the forced name of the method (for example the {@code values()} method in enums).
     *
     * @return The forced method name, null or empty if unknown.
     */
    @Nullable
    String getForce();

    /**
     * Gives access to the method references this method overrides.
     *
     * @return The method references for the method that this method overrides, null or empty if this method does not override any other method.
     */
    @Nullable
    Set<? extends IMetadataMethodReference> getOverrides();

    /**
     * Gives access to the parent method of this method.
     *
     * @return The parent method of this method, or null if unknown.
     */
    @Nullable
    IMetadataMethodReference getParent();
}
