package com.ldtteam.jam.spi.configuration;

import java.util.List;

public record Configuration<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload>(List<InputConfiguration<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload>> inputs, OutputConfiguration<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> outputConfiguration, MappingRuntimeConfiguration<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> runtimeConfiguration)
{
}
