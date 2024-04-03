package com.ldtteam.jam.neoform;

import com.google.common.collect.Lists;
import com.ldtteam.jam.spi.IJammer;
import com.ldtteam.jam.spi.ast.metadata.IMetadataAST;
import com.ldtteam.jam.spi.ast.named.builder.factory.INamedASTBuilderFactory;
import com.ldtteam.jam.spi.configuration.*;
import com.ldtteam.jam.spi.identification.IExistingIdentitySupplier;
import com.ldtteam.jam.spi.identification.INewIdentitySupplier;
import com.ldtteam.jam.spi.metadata.IMetadataASTBuilder;
import com.ldtteam.jam.spi.name.IExistingNameSupplier;
import com.ldtteam.jam.spi.name.IRemapper;
import com.ldtteam.jam.Jammer;
import com.ldtteam.jam.spi.writer.INamedASTOutputWriter;
import com.ldtteam.jam.spi.writer.IStatisticsWriter;
import joptsimple.AbstractOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class JammerRuntime
{
    public static final Logger LOGGER = LoggerFactory.getLogger(JammerRuntime.class);

    private final IRemapperProducer obfuscatedToOfficialRemapperProducer;
    private final IExistingIdentitySupplierProducer existingIdentitySupplierProducer;
    private final IExistingNameSupplierProducer existingNameSupplierProducer;
    private final INewIdentitySupplierProducer newIdentitySupplierProducer;
    private final INamedASTBuilderProducer      namedASTProducer;
    private final IMetadataASTBuilderProducer   metadataASTProducer;
    private final INamedASTOutputWriterProducer namedASTOutputWriterProducer;
    private final IMappingRuntimeConfigurationProducer mappingRuntimeConfigurationProducer;
    private final IStatisticsWriterProducer statisticsWriterProducer;

    public JammerRuntime(
            final IRemapperProducer obfuscatedToOfficialRemapperProducer,
            final IExistingIdentitySupplierProducer existingIdentitySupplierProducer,
            final IExistingNameSupplierProducer existingNameSupplierProducer,
            final INewIdentitySupplierProducer newIdentitySupplierProducer,
            final INamedASTBuilderProducer namedASTProducer,
            final IMetadataASTBuilderProducer metadataASTProducer,
            final INamedASTOutputWriterProducer namedASTOutputWriterProducer,
            final IMappingRuntimeConfigurationProducer mappingRuntimeConfigurationProducer,
            final IStatisticsWriterProducer statisticsWriterProducer) {
        this.obfuscatedToOfficialRemapperProducer = obfuscatedToOfficialRemapperProducer;
        this.existingIdentitySupplierProducer = existingIdentitySupplierProducer;
        this.existingNameSupplierProducer = existingNameSupplierProducer;
        this.newIdentitySupplierProducer = newIdentitySupplierProducer;
        this.namedASTProducer = namedASTProducer;
        this.metadataASTProducer = metadataASTProducer;
        this.namedASTOutputWriterProducer = namedASTOutputWriterProducer;
        this.mappingRuntimeConfigurationProducer = mappingRuntimeConfigurationProducer;
        this.statisticsWriterProducer = statisticsWriterProducer;
    }

    public boolean run(String[] args)
    {
        LOGGER.warn("Starting JammerRuntime with arguments: {}", String.join(" ", args));

        final OptionParser parser = new OptionParser();
        final AbstractOptionSpec<String> existingNamesOption = parser.acceptsAll(
            Lists.newArrayList("existingNames", "en"),
            "The name of the input version.")
          .withRequiredArg()
          .ofType(String.class);
        final AbstractOptionSpec<File> existingJarsOption = parser.acceptsAll(
            Lists.newArrayList("existingJars", "ej"),
            "The jar files to use as an input.")
          .withRequiredArg()
          .ofType(File.class);
        final AbstractOptionSpec<File> existingMappingsOption = parser.acceptsAll(
            Lists.newArrayList("existingMappings", "em"),
            "The mapping of the input version.")
          .withRequiredArg()
          .ofType(File.class);
        final AbstractOptionSpec<File> existingMetadataOption = parser.acceptsAll(
                        Lists.newArrayList("existingMetadata", "emd"),
                        "The metadata of the input version.")
                .withRequiredArg()
                .ofType(File.class);
        final AbstractOptionSpec<File> existingIdentifiersOption = parser.acceptsAll(
            Lists.newArrayList("existingIdentifiers", "ei"),
            "The identifiers of the input version.")
          .withRequiredArg()
          .ofType(File.class);

        final AbstractOptionSpec<String> inputNameOption = parser.acceptsAll(
            Lists.newArrayList("inputName", "in"),
            "The name of the input version.")
          .withRequiredArg()
          .ofType(String.class);
        final AbstractOptionSpec<File> inputJarOption = parser.acceptsAll(
            Lists.newArrayList("inputJar", "ij"),
            "The targeted final input jar to map to.")
          .withRequiredArg()
          .ofType(File.class);
        final AbstractOptionSpec<File> inputMetadataOption = parser.acceptsAll(
          Lists.newArrayList("inputMetadata", "imd"),
          "The targeted final input metadata to map to.")
          .withRequiredArg()
          .ofType(File.class);
        final AbstractOptionSpec<File> inputMappingOption = parser.acceptsAll(
            Lists.newArrayList("inputMapping", "im"),
            "The targeted final input mapping to map to.")
          .withRequiredArg()
          .ofType(File.class);

        final AbstractOptionSpec<File> outputPathOption = parser.acceptsAll(
            Lists.newArrayList("outputPath", "o"),
            "The output path to write the files to.")
          .withRequiredArg()
          .ofType(File.class);

        final AbstractOptionSpec<Boolean> writeLambdaMetaInformationOption = parser.acceptsAll(
          Lists.newArrayList("writeLambdaMetaInformation", "wli"),
          "Indicates if the writer needs to write the lambda meta information.")
         .withOptionalArg()
         .ofType(Boolean.class)
         .defaultsTo(false);

        final AbstractOptionSpec<Integer> mappingMinimalBytecodeSizesOption = parser.acceptsAll(
            Lists.newArrayList("mappingMinimalBytecodeSizes", "mmbcs"),
            "The minimal bytecode sizes of the methods to map.")
                                                                          .withOptionalArg()
                                                                          .ofType(Integer.class);

        final AbstractOptionSpec<Float> mappingMinimalByteCodeMatchPercentageOption = parser.acceptsAll(
            Lists.newArrayList("mappingMinimalByteCodeMatchPercentage", "mmbcm"),
            "The minimal bytecode match percentage of the methods to map.")
                                                                          .withOptionalArg()
                                                                          .ofType(Float.class);

        final AbstractOptionSpec<Integer> minimalByteCodeSizeForFuzzyPatchingOption = parser.acceptsAll(
                Lists.newArrayList("minimalByteCodeSizeForFuzzyPatching", "mbcsfp"),
                "The minimal amount of byte code instructions for a method to apply fuzzy patching to.")
                .withOptionalArg()
                .ofType(Integer.class)
                .defaultsTo(40);


        final AbstractOptionSpec<Boolean> writeStatisticsToDiskOption = parser.acceptsAll(
            Lists.newArrayList("writeStatisticsToDisk", "wsd"),
            "Indicates if the writer needs to write the statistics to disk.")
                                                                          .withOptionalArg()
                                                                          .ofType(boolean.class)
                                                                          .defaultsTo(true);
        final AbstractOptionSpec<Boolean> writeStatisticsToLogOption = parser.acceptsAll(
            Lists.newArrayList("writeStatisticsToLog", "wsl"),
            "Indicates if the writer needs to write the statistics to the log.")
                                                                          .withOptionalArg()
                                                                          .ofType(boolean.class)
                                                                          .defaultsTo(true);

        final OptionSet parsed = parser.parse(args);

        final List<String> existingNames = parsed.valuesOf(existingNamesOption);
        final List<File> existingJars = parsed.valuesOf(existingJarsOption);
        final List<File> existingMappings = parsed.valuesOf(existingMappingsOption);
        final List<File> existingMetadata = parsed.valuesOf(existingMetadataOption);
        final List<File> existingIdentifiers = parsed.valuesOf(existingIdentifiersOption);

        final String inputName = parsed.valueOf(inputNameOption);
        final File inputJar = parsed.valueOf(inputJarOption);
        final File inputMetadata = parsed.valueOf(inputMetadataOption);
        final File inputMapping = parsed.valueOf(inputMappingOption);

        final File outputPath = parsed.valueOf(outputPathOption);

        final boolean writeLambdaMetaInformationValue = parsed.valueOf(writeLambdaMetaInformationOption);

        final List<Integer> mappingMinimalBytecodeSizes = parsed.valuesOf(mappingMinimalBytecodeSizesOption);
        final List<Float> mappingMinimalByteCodeMatchPercentage = parsed.valuesOf(mappingMinimalByteCodeMatchPercentageOption);
        final int minimalByteCodeSizeForFuzzyPatching = parsed.valueOf(minimalByteCodeSizeForFuzzyPatchingOption);

        final boolean shouldWriteStatisticsToDisk = parsed.valueOf(writeStatisticsToDiskOption);
        final boolean shouldWriteStatisticsToLog = parsed.valueOf(writeStatisticsToLogOption);

        if (existingNames.size() != existingJars.size() || existingNames.size() != existingMappings.size() || existingNames.size() != existingIdentifiers.size() || existingNames.size() != existingMetadata.size())
        {
            LOGGER.error("The number of existing names, jars, mappings, metadata and identifiers must be equal.");
            return false;
        }

        if (existingNames.size() == 0)
        {
            LOGGER.error("No existing names were given.");
            return false;
        }

        if (mappingMinimalBytecodeSizes.size() != mappingMinimalByteCodeMatchPercentage.size())
        {
            LOGGER.error("The number of minimal bytecode sizes and minimal bytecode match percentages must be equal.");
            return false;
        }

        final LinkedList<InputConfiguration> inputConfigurations = new LinkedList<>();
        for (int i = 0; i < existingNames.size(); i++)
        {
            final String name = existingNames.get(i);
            final Path jar = existingJars.get(i).toPath();
            final Path metadata = existingMetadata.get(i).toPath();
            final IMetadataAST ast = metadataASTProducer.from(metadata).ast();
            final Optional<IRemapper> remapped = Optional.ofNullable(obfuscatedToOfficialRemapperProducer.from(existingMappings.get(i).toPath(), ast));
            final Optional<IExistingIdentitySupplier> identifier = Optional.ofNullable(existingIdentitySupplierProducer.from(
              existingIdentifiers.get(i).toPath(),
              existingMappings.get(i).toPath()
            ));
            final Optional<IExistingNameSupplier> names = Optional.ofNullable(existingNameSupplierProducer.from(
                    existingIdentifiers.get(i).toPath(),
                    existingMappings.get(i).toPath()
            ));

            inputConfigurations.add(new InputConfiguration(name, jar, remapped, identifier, names));
        }

        final IMetadataAST inputAST = metadataASTProducer.from(inputMetadata.toPath()).ast();

        inputConfigurations.add(
          new InputConfiguration(inputName, inputJar.toPath(), Optional.of(obfuscatedToOfficialRemapperProducer.from(inputMapping.toPath(), inputAST)), Optional.empty(), Optional.empty())
        );

        final OutputConfiguration outputConfiguration = new OutputConfiguration(
          outputPath.toPath(),
          newIdentitySupplierProducer.from(existingIdentifiers.get(existingIdentifiers.size() - 1).toPath()),
          namedASTProducer.from(inputMapping.toPath(), inputAST),
          metadataASTProducer.from(inputMetadata.toPath()),
          namedASTOutputWriterProducer.create(),
          statisticsWriterProducer.create(),
          new MetadataWritingConfiguration(writeLambdaMetaInformationValue),
          new StatisticsWritingConfiguration(shouldWriteStatisticsToDisk, shouldWriteStatisticsToLog)
        );

        final Map<Integer, Float> mappingThresholdPercentages = IntStream.range(0, mappingMinimalBytecodeSizes.size())
                                                                  .boxed()
                                                                  .collect(Collectors.toMap(mappingMinimalBytecodeSizes::get,
                                                                    mappingMinimalByteCodeMatchPercentage::get,
                                                                    (a, b) -> b));

        final MappingConfiguration mappingConfiguration = new MappingConfiguration(mappingThresholdPercentages, minimalByteCodeSizeForFuzzyPatching);

        final MappingRuntimeConfiguration runtimeConfiguration = mappingRuntimeConfigurationProducer.create(mappingConfiguration);

        final Configuration configuration = new Configuration(
          inputConfigurations,
          outputConfiguration,
          runtimeConfiguration
        );

        final IJammer jammer = new Jammer();

        try
        {
            jammer.run(configuration);
            return true;
        }
        catch (Exception ex)
        {
            LOGGER.error("An error occurred while running the jammer.", ex);
            return false;
        }
    }

    @FunctionalInterface
    public interface IRemapperProducer {

        IRemapper from(final Path mappingsPath, final IMetadataAST metadata);
    }

    @FunctionalInterface
    public interface IExistingIdentitySupplierProducer {
        IExistingIdentitySupplier from(final Path existingIdentifiers, final Path existingMappings);
    }

    @FunctionalInterface
    public interface IExistingNameSupplierProducer {
        IExistingNameSupplier from(final Path runtimeMappings, final Path existingMappings);
    }

    @FunctionalInterface
    public interface INewIdentitySupplierProducer {
        INewIdentitySupplier from(final Path existingIdentifiers);
    }

    @FunctionalInterface
    public interface INamedASTBuilderProducer
    {
        INamedASTBuilderFactory from(final Path inputMappingPath, IMetadataAST ast);
    }

    @FunctionalInterface
    public interface IMetadataASTBuilderProducer
    {
        IMetadataASTBuilder from(final Path metadata);
    }

    @FunctionalInterface
    public interface INamedASTOutputWriterProducer
    {
        INamedASTOutputWriter create();
    }

    @FunctionalInterface
    public interface IMappingRuntimeConfigurationProducer
    {
        MappingRuntimeConfiguration create(MappingConfiguration configuration);
    }

    @FunctionalInterface
    public interface IStatisticsWriterProducer
    {
        IStatisticsWriter create();
    }
}
