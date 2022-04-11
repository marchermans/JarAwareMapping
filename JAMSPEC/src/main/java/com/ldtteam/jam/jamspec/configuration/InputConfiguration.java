package com.ldtteam.jam.jamspec.configuration;

import com.ldtteam.jam.jamspec.identification.IExistingIdentitySupplier;
import com.ldtteam.jam.jamspec.name.IRemapper;

import java.nio.file.Path;
import java.util.Optional;

public record InputConfiguration(String name, Path path, Optional<IRemapper> remapper, Optional<IExistingIdentitySupplier> identifier)
{
    public InputConfiguration(final String name, final Path path, final IExistingIdentitySupplier identitySupplier)
    {
        this(name, path, Optional.empty(), Optional.ofNullable(identitySupplier));
    }

    public InputConfiguration(final String name, final Path path, final IRemapper remapper)
    {
        this(name, path, Optional.ofNullable(remapper), Optional.empty());
    }

    public InputConfiguration(String name, Path path)
    {
        this(name, path, Optional.empty(), Optional.empty());
    }
}
