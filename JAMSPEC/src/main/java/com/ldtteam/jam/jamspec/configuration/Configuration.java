package com.ldtteam.jam.jamspec.configuration;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public record Configuration(LinkedList<InputConfiguration> inputs, OutputConfiguration outputConfiguration, MappingRuntimeConfiguration runtimeConfiguration)
{
}
