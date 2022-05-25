package com.ldtteam.jam.mcpconfig;

import com.google.common.collect.Lists;
import com.ldtteam.jam.spi.IJammer;
import com.ldtteam.jam.spi.configuration.Configuration;
import com.ldtteam.jam.spi.configuration.InputConfiguration;
import com.ldtteam.jam.spi.configuration.MappingRuntimeConfiguration;
import com.ldtteam.jam.spi.configuration.OutputConfiguration;
import com.ldtteam.jam.spi.identification.IExistingIdentitySupplier;
import com.ldtteam.jam.spi.name.IRemapper;
import com.ldtteam.jam.Jammer;
import com.ldtteam.jam.mapping.*;
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

public class Main
{
    public static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args)
    {
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
            "THe output path to write the files to.")
          .withRequiredArg()
          .ofType(File.class);

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
            final Optional<IRemapper> remapped = Optional.of(TSRGRemapper.createObfuscatedToOfficial(existingMappings.get(i).toPath()));
            final Optional<IExistingIdentitySupplier> identifier = Optional.of(TSRGIdentitySupplier.create(
              existingIdentifiers.get(i).toPath(),
              existingMappings.get(i).toPath()
            ));

            inputConfigurations.add(new InputConfiguration(name, jar, remapped, identifier));
        }

        inputConfigurations.add(
          new InputConfiguration(inputName, inputJar.toPath(), Optional.of(TSRGRemapper.createObfuscatedToOfficial(inputMapping.toPath())), Optional.empty())
        );

        final OutputConfiguration outputConfiguration = new OutputConfiguration(
          outputPath.toPath(),
          TSRGNewIdentitySupplier.create(existingIdentifiers.get(existingIdentifiers.size() - 1).toPath()),
          TSRGNamedASTBuilder.AST(inputMapping.toPath()),
          TSRGMetadataProvider.create(inputMetadata.toPath()),
          TSRGNamedASTWriter.create()
        );

        final MappingRuntimeConfiguration runtimeConfiguration = new MappingRuntimeConfiguration(
          NameBasedMapper.classes(),
          LambdaAwareMethodMapper.create(
            PhasedMapper.create(
              NameBasedMapper.methods(),
              ByteCodeBasedMethodMapper.create()
            ),
            PhasedMapper.create(
              AlignedMapper.methods(
                ByteCodeBasedMethodMapper.create()
              ),
              NameBasedMapper.methods()
            )
          ),
          NameBasedMapper.fields()
        );

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
}
