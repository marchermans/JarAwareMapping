package com.ldtteam.jam.mcpconfig;

import com.google.common.collect.Lists;
import com.ldtteam.jam.spi.IJammer;
import com.ldtteam.jam.spi.ast.named.builder.INamedASTBuilder;
import com.ldtteam.jam.spi.configuration.*;
import com.ldtteam.jam.spi.identification.IExistingIdentitySupplier;
import com.ldtteam.jam.spi.identification.INewIdentitySupplier;
import com.ldtteam.jam.spi.metadata.IMetadataASTBuilder;
import com.ldtteam.jam.spi.name.IRemapper;
import com.ldtteam.jam.Jammer;
import com.ldtteam.jam.spi.writer.INamedASTOutputWriter;
import joptsimple.AbstractOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public final class JammerRuntime
{
    public static final Logger LOGGER = LoggerFactory.getLogger(JammerRuntime.class);

    private final IRemapperProducer obfuscatedToOfficialRemapperProducer;
    private final IExistingIdentitySupplierProducer existingIdentitySupplierProducer;
    private final INewIdentitySupplierProducer newIdentitySupplierProducer;
    private final INamedASTBuilderProducer      namedASTProducer;
    private final IMetadataASTBuilderProducer   metadataASTProducer;
    private final INamedASTOutputWriterProducer namedASTOutputWriterProducer;

    private final IMappingRuntimeConfigurationProducer mappingRuntimeConfigurationProducer;

    public JammerRuntime(
      final IRemapperProducer obfuscatedToOfficialRemapperProducer,
      final IExistingIdentitySupplierProducer existingIdentitySupplierProducer,
      final INewIdentitySupplierProducer newIdentitySupplierProducer,
      final INamedASTBuilderProducer namedASTProducer,
      final IMetadataASTBuilderProducer metadataASTProducer,
      final INamedASTOutputWriterProducer namedASTOutputWriterProducer,
      final IMappingRuntimeConfigurationProducer mappingRuntimeConfigurationProducer) {
        this.obfuscatedToOfficialRemapperProducer = obfuscatedToOfficialRemapperProducer;
        this.existingIdentitySupplierProducer = existingIdentitySupplierProducer;
        this.newIdentitySupplierProducer = newIdentitySupplierProducer;
        this.namedASTProducer = namedASTProducer;
        this.metadataASTProducer = metadataASTProducer;
        this.namedASTOutputWriterProducer = namedASTOutputWriterProducer;
        this.mappingRuntimeConfigurationProducer = mappingRuntimeConfigurationProducer;
    }

    public void run(String[] args)
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

        final AbstractOptionSpec<Boolean> writeLambdaMetaInformation = parser.acceptsAll(
          Lists.newArrayList("writeLambdaMetaInformation", "wli"),
          "Indicates if the writer needs to write the lambda meta information.")
         .withOptionalArg()
         .ofType(Boolean.class)
         .defaultsTo(false);

        final OptionSet parsed = parser.parse(args);

        final List<String> existingNames = parsed.valuesOf(existingNamesOption);
        final List<File> existingJars = parsed.valuesOf(existingJarsOption);
        final List<File> existingMappings = parsed.valuesOf(existingMappingsOption);
        final List<File> existingIdentifiers = parsed.valuesOf(existingIdentifiersOption);

        final String inputName = parsed.valueOf(inputNameOption);
        final File inputJar = parsed.valueOf(inputJarOption);
        final File inputMetadata = parsed.valueOf(inputMetadataOption);
        final File inputMapping = parsed.valueOf(inputMappingOption);

        final File outputPath = parsed.valueOf(outputPathOption);

        final boolean writeLambdaMetaInformationValue = parsed.valueOf(writeLambdaMetaInformation);

        if (existingNames.size() != existingJars.size() || existingNames.size() != existingMappings.size() || existingNames.size() != existingIdentifiers.size())
        {
            LOGGER.error("The number of existing names, jars, mappings and identifiers must be equal.");
            return;
        }

        if (existingNames.size() == 0)
        {
            LOGGER.error("No existing names were given.");
            return;
        }

        final LinkedList<InputConfiguration> inputConfigurations = new LinkedList<>();
        for (int i = 0; i < existingNames.size(); i++)
        {
            final String name = existingNames.get(i);
            final Path jar = existingJars.get(i).toPath();
            final Optional<IRemapper> remapped = Optional.of(obfuscatedToOfficialRemapperProducer.from(existingMappings.get(i).toPath()));
            final Optional<IExistingIdentitySupplier> identifier = Optional.of(existingIdentitySupplierProducer.from(
              existingIdentifiers.get(i).toPath(),
              existingMappings.get(i).toPath()
            ));

            inputConfigurations.add(new InputConfiguration(name, jar, remapped, identifier));
        }

        inputConfigurations.add(
          new InputConfiguration(inputName, inputJar.toPath(), Optional.of(obfuscatedToOfficialRemapperProducer.from(inputMapping.toPath())), Optional.empty())
        );

        final OutputConfiguration outputConfiguration = new OutputConfiguration(
          outputPath.toPath(),
          newIdentitySupplierProducer.from(existingIdentifiers.get(existingIdentifiers.size() - 1).toPath()),
          namedASTProducer.from(inputMapping.toPath()),
          metadataASTProducer.from(inputMetadata.toPath()),
          namedASTOutputWriterProducer.create(),
          new MetadataWritingConfiguration(writeLambdaMetaInformationValue)
        );

        final MappingRuntimeConfiguration runtimeConfiguration = mappingRuntimeConfigurationProducer.create();

        final Configuration configuration = new Configuration(
          inputConfigurations,
          outputConfiguration,
          runtimeConfiguration
        );

        final IJammer jammer = new Jammer();

        try
        {
            jammer.run(configuration);
        }
        catch (Exception ex)
        {
            LOGGER.error("An error occurred while running the jammer.", ex);
        }
    }

    @FunctionalInterface
    public interface IRemapperProducer {

        IRemapper from(final Path mappingsPath);
    }

    @FunctionalInterface
    public interface IExistingIdentitySupplierProducer {
        IExistingIdentitySupplier from(final Path existingIdentifiers, final Path existingMappings);
    }

    @FunctionalInterface
    public interface INewIdentitySupplierProducer {
        INewIdentitySupplier from(final Path existingIdentifiers);
    }

    @FunctionalInterface
    public interface INamedASTBuilderProducer
    {
        INamedASTBuilder from(final Path inputMappingPath);
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
        MappingRuntimeConfiguration create();
    }
}
