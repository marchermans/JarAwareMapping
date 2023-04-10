package com.ldtteam.jam.spi.ast.named.builder.factory;

import com.google.common.collect.BiMap;
import com.ldtteam.jam.spi.asm.IASMData;
import com.ldtteam.jam.spi.ast.named.builder.INamedASTBuilder;
import com.ldtteam.jam.spi.name.IExistingNameSupplier;
import com.ldtteam.jam.spi.name.IRemapper;

import java.util.Optional;

public interface INamedASTBuilderFactory<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> {

    INamedASTBuilder<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> create(final BiMap<IASMData, String> nameByLoadedASMData, final BiMap<String, Optional<IExistingNameSupplier>> remapperByName);
}
