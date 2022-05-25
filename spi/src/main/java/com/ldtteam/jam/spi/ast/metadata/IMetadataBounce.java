package com.ldtteam.jam.spi.ast.metadata;

/**
 * Represents the bounce information of a bouncer method based on an
 * ast build from metadata.
 */
public interface IMetadataBounce {
    IMetadataMethodReference getTarget();

    IMetadataMethodReference getOwner();
}
