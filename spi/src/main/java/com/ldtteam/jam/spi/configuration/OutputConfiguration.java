package com.ldtteam.jam.spi.configuration;

import com.ldtteam.jam.spi.ast.named.builder.factory.INamedASTBuilderFactory;
import com.ldtteam.jam.spi.identification.INewIdentitySupplier;
import com.ldtteam.jam.spi.metadata.IMetadataASTBuilder;
import com.ldtteam.jam.spi.writer.INamedASTOutputWriter;
import com.ldtteam.jam.spi.writer.IStatisticsWriter;

import java.nio.file.Path;

public record OutputConfiguration(Path outputDirectory, INewIdentitySupplier identifier, INamedASTBuilderFactory astBuilderFactory, IMetadataASTBuilder metadataProvider, INamedASTOutputWriter writer, IStatisticsWriter statisticsWriter, MetadataWritingConfiguration metadataWritingConfiguration, StatisticsWritingConfiguration statisticsWritingConfiguration)
{
}
