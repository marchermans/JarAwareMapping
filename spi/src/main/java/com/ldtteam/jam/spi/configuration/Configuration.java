package com.ldtteam.jam.spi.configuration;

import java.util.List;

public record Configuration(List<InputConfiguration> inputs, OutputConfiguration outputConfiguration, MappingRuntimeConfiguration runtimeConfiguration)
{
}
