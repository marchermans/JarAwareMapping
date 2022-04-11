package com.ldtteam.jam.jamspec.configuration;

import com.ldtteam.jam.jamspec.identification.INewIdentitySupplier;
import com.ldtteam.jam.jamspec.writer.IOutputWriter;

import java.nio.file.Path;

public record OutputConfiguration(Path outputDirectory, INewIdentitySupplier identifier, IOutputWriter writer)
{
}
