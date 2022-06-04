package com.ldtteam.jam.spi.writer;

import com.ldtteam.jam.spi.configuration.Configuration;
import com.ldtteam.jam.spi.statistics.IMappingStatistics;

import java.nio.file.Path;

public interface IStatisticsWriter
{
    void write(Path outputDirectory, IMappingStatistics mappingStatistics, Configuration configuration);
}
