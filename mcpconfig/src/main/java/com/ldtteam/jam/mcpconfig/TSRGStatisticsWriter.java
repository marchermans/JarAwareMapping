package com.ldtteam.jam.mcpconfig;

import com.ldtteam.jam.spi.configuration.Configuration;
import com.ldtteam.jam.spi.configuration.InputConfiguration;
import com.ldtteam.jam.spi.statistics.IMappingStatistics;
import com.ldtteam.jam.spi.writer.IStatisticsWriter;
import net.steppschuh.markdowngenerator.list.UnorderedList;
import net.steppschuh.markdowngenerator.table.Table;
import net.steppschuh.markdowngenerator.text.heading.Heading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TSRGStatisticsWriter implements IStatisticsWriter
{
    public static IStatisticsWriter create() {
        return new TSRGStatisticsWriter();
    }

    private static final String STATISTICS_MD_FILE_NAME = "statistics.md";
    private static final Logger LOGGER = LoggerFactory.getLogger(TSRGStatisticsWriter.class);

    private TSRGStatisticsWriter()
    {
    }

    @Override
    public void write(final Path outputDirectory, final IMappingStatistics mappingStatistics, Configuration configuration)
    {
        final String data = buildOutput(mappingStatistics, configuration.inputs());

        if(configuration.outputConfiguration().statisticsWritingConfiguration().writeToConsole()) {
            Arrays.stream(data.split("\n")).forEach(LOGGER::info);
        }

        if (configuration.outputConfiguration().statisticsWritingConfiguration().writeToFile()) {
            writeDataToFile(outputDirectory, data);
        }
    }

    private String buildOutput(final IMappingStatistics mappingStatistics, final List<InputConfiguration> inputConfigurations)
    {
        final StringBuilder outputBuilder = new StringBuilder();

        outputBuilder.append(new Heading("Jammer Statistics:", 2)).append("\r\n");
        outputBuilder.append(new Heading("Input version(s):", 3)).append("\r\n");
        List<String> list = getInputVersions(inputConfigurations);
        outputBuilder.append(new UnorderedList<>(list)).append("\r\n");

        outputBuilder.append(new Heading("Output version:", 3)).append("\r\n");
        outputBuilder.append(inputConfigurations.get(inputConfigurations.size() - 1).name()).append("\r\n");

        outputBuilder.append(new Heading("Primary mapping statistics:", 3)).append("\r\n");
        Table.Builder primaryTableBuilder = new Table.Builder()
                                       .withAlignments(Table.ALIGN_RIGHT, Table.ALIGN_CENTER, Table.ALIGN_CENTER, Table.ALIGN_CENTER)
                                       .addRow("", "Not mapped", "Mapped", "Newly found")
                                       .addRow("Classes", mappingStatistics.getDirectClassStatistics().getLost(), mappingStatistics.getDirectClassStatistics().getMapped(), mappingStatistics.getDirectClassStatistics().getFound())
                                       .addRow("Methods", mappingStatistics.getDirectMethodStatistics().getLost(), mappingStatistics.getDirectMethodStatistics().getMapped(), mappingStatistics.getDirectMethodStatistics().getFound())
                                       .addRow("Fields", mappingStatistics.getDirectFieldStatistics().getLost(), mappingStatistics.getDirectFieldStatistics().getMapped(), mappingStatistics.getDirectFieldStatistics().getFound());
        outputBuilder.append(primaryTableBuilder.build()).append("\r\n");

        outputBuilder.append(new Heading("Rejuvenated mapping statistics:", 3)).append("\r\n");
        Table.Builder rejuvenatedTableBuilder = new Table.Builder()
                                       .withAlignments(Table.ALIGN_RIGHT, Table.ALIGN_CENTER)
                                       .addRow("", "Mapped")
                                       .addRow("Classes", mappingStatistics.getRejuvenatedClassStatistics().getMapped())
                                       .addRow("Methods", mappingStatistics.getRejuvenatedMethodStatistics().getMapped())
                                       .addRow("Fields", mappingStatistics.getRejuvenatedFieldStatistics().getMapped());
        outputBuilder.append(rejuvenatedTableBuilder.build()).append("\r\n");

        outputBuilder.append(new Heading("Renamed mapping statistics:", 3)).append("\r\n");
        Table.Builder renamedTableBuilder = new Table.Builder()
                                      .withAlignments(Table.ALIGN_RIGHT, Table.ALIGN_CENTER)
                                      .addRow("", "Mapped")
                                      .addRow("Methods", mappingStatistics.getRenamedMethodStatistics().getMapped())
                                      .addRow("Fields", mappingStatistics.getRenamedFieldStatistics().getMapped());
        outputBuilder.append(renamedTableBuilder.build()).append("\r\n");

        outputBuilder.append(new Heading("Total mapping statistics:", 3)).append("\r\n");
        Table.Builder totalTableBuilder = new Table.Builder()
                                              .withAlignments(Table.ALIGN_RIGHT, Table.ALIGN_CENTER)
                                              .addRow("", "Mapped", "Newly found")
                                              .addRow("Classes", mappingStatistics.getTotalClassStatistics().getMapped(), mappingStatistics.getTotalClassStatistics().getFound())
                                              .addRow("Methods", mappingStatistics.getTotalMethodStatistics().getMapped(), mappingStatistics.getTotalMethodStatistics().getFound())
                                              .addRow("Fields", mappingStatistics.getTotalFieldStatistics().getMapped(), mappingStatistics.getTotalFieldStatistics().getFound());
        outputBuilder.append(totalTableBuilder.build());

        return outputBuilder.toString();
    }

    private List<String> getInputVersions(final List<InputConfiguration> inputConfigurations)
    {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < inputConfigurations.size(); i++)
        {
            if (i != (inputConfigurations.size() - 1)) {
                final InputConfiguration inputConfiguration = inputConfigurations.get(i);
                list.add(inputConfiguration.name());
            }
        }
        return list;
    }

    private void writeDataToFile(final Path outputDirectory, final String data)
    {
        final Path outputFile = outputDirectory.resolve(STATISTICS_MD_FILE_NAME);
        try
        {
            Files.deleteIfExists(outputFile);
            Files.write(outputFile, Arrays.stream(data.split("\r\n")).toList(), StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
        }
        catch (IOException e)
        {
            LOGGER.error("Failed to write statistics to file: {}", outputFile, e);
        }
    }
}
