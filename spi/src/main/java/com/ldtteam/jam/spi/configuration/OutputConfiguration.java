package com.ldtteam.jam.spi.configuration;

import com.ldtteam.jam.spi.identification.INewIdentitySupplier;
import com.ldtteam.jam.spi.writer.IOutputWriter;

import java.nio.file.Path;

public record OutputConfiguration(Path outputDirectory, INewIdentitySupplier identifier, IOutputWriter writer)
{
}
