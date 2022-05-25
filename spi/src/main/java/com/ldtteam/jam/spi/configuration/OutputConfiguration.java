package com.ldtteam.jam.spi.configuration;

import com.ldtteam.jam.spi.ast.named.builder.INamedASTBuilder;
import com.ldtteam.jam.spi.identification.INewIdentitySupplier;
import com.ldtteam.jam.spi.metadata.IMetadataProvider;
import com.ldtteam.jam.spi.writer.INamedASTOutputWriter;
import com.ldtteam.jam.spi.writer.IOutputWriter;

import java.nio.file.Path;

public record OutputConfiguration(Path outputDirectory, INewIdentitySupplier identifier, INamedASTBuilder astBuilder, IMetadataProvider metadataProvider, INamedASTOutputWriter writer)
{
}
