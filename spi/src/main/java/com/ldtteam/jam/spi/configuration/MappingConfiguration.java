package com.ldtteam.jam.spi.configuration;

import java.util.Map;

public record MappingConfiguration(Map<Integer, Float> mappingThresholdPercentage, int minimalInstructionCount)
{
}
