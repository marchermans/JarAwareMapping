package com.ldtteam.jam.spi.configuration;

import com.ldtteam.jam.spi.identification.IExistingIdentitySupplier;
import com.ldtteam.jam.spi.name.IExistingNameSupplier;
import com.ldtteam.jam.spi.name.IRemapper;
import com.ldtteam.jam.spi.payload.IPayloadSupplier;

import java.nio.file.Path;
import java.util.Optional;

public record InputConfiguration<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload>(String name, Path path, Optional<IRemapper> remapper, Optional<IExistingIdentitySupplier> identifier, Optional<IExistingNameSupplier> names, Optional<IPayloadSupplier<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload>> payloads)
{
}
