package com.ldtteam.jam;

import com.google.common.collect.*;
import com.ldtteam.jam.spi.IJammer;
import com.ldtteam.jam.spi.ast.named.INamedAST;
import com.ldtteam.jam.spi.configuration.Configuration;
import com.ldtteam.jam.spi.configuration.InputConfiguration;
import com.ldtteam.jam.spi.configuration.MappingRuntimeConfiguration;
import com.ldtteam.jam.spi.configuration.OutputConfiguration;
import com.ldtteam.jam.spi.mapping.MappingResult;
import com.ldtteam.jam.loader.ASMDataLoader;
import com.ldtteam.jam.loader.LoadedASMData;
import com.ldtteam.jam.statistics.MappingStatistics;
import com.ldtteam.jam.util.SetsUtil;
import com.machinezoo.noexception.Exceptions;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Jammer implements IJammer
{
    private final Logger LOGGER = LoggerFactory.getLogger(Jammer.class);

    @Override
    public void run(final Configuration configuration)
    {
        LOGGER.info("Starting Jammer. Version: " + getClass().getPackage().getImplementationVersion());
        LOGGER.info("Validating configuration");
        validateConfiguration(configuration);

        LOGGER.info("Preparing...");
        prepare(configuration);

        LOGGER.info("Loading data...");
        final Map<String, InputConfiguration> configurationsByName = configuration.inputs().stream()
          .collect(Collectors.toMap(InputConfiguration::name, Function.identity()));

        final Set<LoadedASMData> data = configuration.inputs().stream()
          .map(ASMDataLoader::load)
          .collect(Collectors.toSet());

        record LoadedASMDataByInputName(String name, LoadedASMData data) {}
        final Map<String, LoadedASMData> dataByInputName = data.stream()
          .map(d -> new LoadedASMDataByInputName(d.name(), d))
          .collect(Collectors.toMap(LoadedASMDataByInputName::name, LoadedASMDataByInputName::data));

        record ConfigurationNameByClassNodeEntry(ClassNode classNode, String name) {}
        final Map<ClassNode, String> configurationNameByClassNodes = data.stream()
          .flatMap(inputData -> inputData.classes()
            .stream()
            .map(classNode -> new ConfigurationNameByClassNodeEntry(classNode, inputData.name())))
          .collect(Collectors.toMap(ConfigurationNameByClassNodeEntry::classNode, ConfigurationNameByClassNodeEntry::name));

        record ConfigurationNameByMethodNodeEntry(MethodNode methodNode, String name) {}
        final Map<MethodNode, String> configurationNameByMethodNodes = data.stream()
          .flatMap(inputData -> inputData.methods()
            .stream()
            .map(methodNode -> new ConfigurationNameByMethodNodeEntry(methodNode, inputData.name())))
          .collect(Collectors.toMap(ConfigurationNameByMethodNodeEntry::methodNode, ConfigurationNameByMethodNodeEntry::name));

        record ConfigurationNameByFieldNodeEntry(FieldNode fieldNode, String name) {}
        final Map<FieldNode, String> configurationNameByFieldNodes = data.stream()
          .flatMap(inputData -> inputData.fields()
            .stream()
            .map(fieldNode -> new ConfigurationNameByFieldNodeEntry(fieldNode, inputData.name())))
          .collect(Collectors.toMap(ConfigurationNameByFieldNodeEntry::fieldNode, ConfigurationNameByFieldNodeEntry::name));

        record ClassNodesByMethodNodeEntry(MethodNode methodNode, ClassNode classNode) {}
        final Map<MethodNode, ClassNode> classNodesByMethodNodes = data.stream()
          .flatMap(inputData -> inputData.classes().stream())
          .flatMap(classNode -> classNode.methods.stream()
            .map(methodNode -> new ClassNodesByMethodNodeEntry(methodNode, classNode)))
          .collect(Collectors.toMap(ClassNodesByMethodNodeEntry::methodNode, ClassNodesByMethodNodeEntry::classNode));

        record ClassNodesByFieldNodeEntry(FieldNode fieldNode, ClassNode classNode) {}
        final Map<FieldNode, ClassNode> classNodesByFieldNodes = data.stream()
          .flatMap(inputData -> inputData.classes().stream())
          .flatMap(classNode -> classNode.fields.stream()
            .map(fieldNode -> new ClassNodesByFieldNodeEntry(fieldNode, classNode)))
          .collect(Collectors.toMap(ClassNodesByFieldNodeEntry::fieldNode, ClassNodesByFieldNodeEntry::classNode));

        LOGGER.info("Mapping direct inputs...");
        final LinkedHashMap<TransitionMappingResultKey, JarMappingResult> transitionMappings = buildTransitionMap(configuration, dataByInputName);
        final JarMappingResult lastMappingResult = transitionMappings.values().iterator().next();

        LOGGER.info("Collecting primary mapping statistics...");
        MappingStatistics mappingStatistics = collectMappingStatistics(lastMappingResult);

        LOGGER.info("Reconstructing transitively lost class mappings...");
        final Set<ClassNode> unmappedClasses = Sets.newHashSet(lastMappingResult.classes().unmappedSources());
        final Set<MethodNode> unmappedMethods = Sets.newHashSet(lastMappingResult.methods().unmappedSources());
        final Set<FieldNode> unmappedFields = Sets.newHashSet(lastMappingResult.fields().unmappedSources());

        final BiMap<ClassNode, ClassNode> mappedClasses = HashBiMap.create(lastMappingResult.classes().mappings());
        final BiMap<MethodNode, MethodNode> mappedMethods = HashBiMap.create(lastMappingResult.methods().mappings());
        final BiMap<FieldNode, FieldNode> mappedFields = HashBiMap.create(lastMappingResult.fields().mappings());

        final BiMap<ClassNode, ClassNode> additionallyMappedClasses = transitivelyMapRemainingClasses(
          lastMappingResult,
          transitionMappings,
          configuration.runtimeConfiguration()
        );

        mappedClasses.putAll(additionallyMappedClasses);
        unmappedClasses.removeAll(additionallyMappedClasses.keySet());

        LOGGER.info("Reconstructing transitively lost method mappings...");
        final Set<MethodNode> rejuvenatedMethods = Sets.newHashSet();
        additionallyMappedClasses.forEach((nextGenClass, transitiveCurrentGenClass) -> {
            //We are talking about the A_A case here, so no methods inside this will be mapped.
            final Set<MethodNode> unmappedNextGenMethods = nextGenClass.methods.stream().collect(SetsUtil.methods((node) -> nextGenClass));
            final Set<MethodNode> unmappedCurrentGenMethods = transitiveCurrentGenClass.methods.stream().collect(SetsUtil.methods((node) -> transitiveCurrentGenClass));

            final MappingResult<MethodNode> transitiveMethodMapping = configuration.runtimeConfiguration().methodMapper().map(unmappedNextGenMethods, unmappedCurrentGenMethods);

            unmappedMethods.removeAll(transitiveMethodMapping.mappings().keySet());
            mappedMethods.putAll(transitiveMethodMapping.mappings());
            rejuvenatedMethods.addAll(transitiveMethodMapping.mappings().keySet());
        });

        LOGGER.info("Reconstructing transitively lost field mappings...");
        final Set<FieldNode> rejuvenatedFields = Sets.newHashSet();
        additionallyMappedClasses.forEach((nextGenClass, transitiveCurrentGenClass) -> {
            //We are talking about the A_A case here, so no fields inside this will be mapped.
            final Set<FieldNode> unmappedNextGenFields = nextGenClass.fields.stream().collect(SetsUtil.fields((node) -> nextGenClass));
            final Set<FieldNode> unmappedCurrentGenFields = transitiveCurrentGenClass.fields.stream().collect(SetsUtil.fields((node) -> transitiveCurrentGenClass));

            final MappingResult<FieldNode> transitiveFieldMapping = configuration.runtimeConfiguration().fieldMapper().map(unmappedNextGenFields, unmappedCurrentGenFields);

            unmappedFields.removeAll(transitiveFieldMapping.mappings().keySet());
            mappedFields.putAll(transitiveFieldMapping.mappings());
            rejuvenatedFields.addAll(transitiveFieldMapping.mappings().keySet());
        });

        LOGGER.info("Collecting rejuvenation statistics...");
        collectRejuvenationStatistics(mappingStatistics, additionallyMappedClasses, rejuvenatedMethods, rejuvenatedFields);

        LOGGER.info("Building transitive class mappings...");
        Map<ClassNode, List<HistoricalClassMapping>> transitiveClassMappings = buildTransitiveClassMappings(mappedClasses, transitionMappings.values());

        LOGGER.info("Building transitive method mappings...");
        final BiMap<MethodNode, MethodNode> transitiveMethodMappings = mapMethodsTransitively(unmappedMethods, classNodesByMethodNodes, transitiveClassMappings, configuration.runtimeConfiguration());
        unmappedMethods.removeAll(transitiveMethodMappings.keySet());
        mappedMethods.putAll(transitiveMethodMappings);
        
        LOGGER.info("Building transitive field mappings...");
        final BiMap<FieldNode, FieldNode> transitiveFieldMappings = mapFieldsTransitively(unmappedFields, classNodesByFieldNodes, transitiveClassMappings, configuration.runtimeConfiguration());
        unmappedFields.removeAll(transitiveFieldMappings.keySet());
        mappedFields.putAll(transitiveFieldMappings);

        LOGGER.info("Collecting renaming statistics...");
        collectRenamingStatistics(mappingStatistics, transitiveMethodMappings, transitiveFieldMappings);

        LOGGER.info("Determining class ids...");
        final BiMap<ClassNode, Integer> classIds = determineClassIds(mappedClasses, unmappedClasses, configurationNameByClassNodes, configurationsByName, configuration.outputConfiguration());

        LOGGER.info("Determining field ids...");
        final BiMap<FieldNode, Integer> fieldIds = determineFieldIds(mappedFields, unmappedFields, classNodesByFieldNodes, configurationNameByFieldNodes, configurationsByName, configuration.outputConfiguration());

        LOGGER.info("Determining method and parameter ids...");
        final MethodIdMappingResult methodIdMappingResult = determineMethodIds(mappedMethods, unmappedMethods, classNodesByMethodNodes, configurationNameByMethodNodes, configurationsByName, configuration.outputConfiguration());

        LOGGER.info("Writing mappings...");
        final LoadedASMData targetASMData = dataByInputName.get(Objects.requireNonNull(configuration.inputs().get(configuration.inputs().size() - 1)).name());
        writeOutput(classIds, methodIdMappingResult.methodIds(), fieldIds, methodIdMappingResult.parameterIds(), configuration.outputConfiguration(), targetASMData);

        LOGGER.info("Collecting total statistics...");
        collectTotalStatistics(mappingStatistics, mappedClasses, mappedMethods, mappedFields, unmappedClasses, unmappedMethods, unmappedFields);

        LOGGER.info("Writing statistics...");
        writeStatistics(mappingStatistics, configuration);
    }

    private void writeStatistics(final MappingStatistics mappingStatistics, final Configuration configuration)
    {
        configuration.outputConfiguration().statisticsWriter().write(
          configuration.outputConfiguration().outputDirectory(),
          mappingStatistics,
          configuration
        );
    }

    private void collectTotalStatistics(
      final MappingStatistics mappingStatistics,
      final BiMap<ClassNode, ClassNode> mappedClasses,
      final BiMap<MethodNode, MethodNode> mappedMethods,
      final BiMap<FieldNode, FieldNode> mappedFields,
      final Set<ClassNode> unmappedClasses,
      final Set<MethodNode> unmappedMethods,
      final Set<FieldNode> unmappedFields)
    {
        mappingStatistics.getTotalClassStatistics().load(0, mappedClasses.size(), unmappedClasses.size());
        mappingStatistics.getTotalMethodStatistics().load(0, mappedMethods.size(), unmappedMethods.size());
        mappingStatistics.getTotalFieldStatistics().load(0, mappedFields.size(), unmappedFields.size());
    }

    private void collectRenamingStatistics(
      final MappingStatistics statistics,
      final BiMap<MethodNode, MethodNode> transitiveMethodMappings,
      final BiMap<FieldNode, FieldNode> transitiveFieldMappings)
    {
        statistics.getRenamedClassStatistics().load(0,0,0); //Renaming never happens, jammer for now does not support this scenario.
        statistics.getRenamedMethodStatistics().load(0, transitiveMethodMappings.size(), 0);
        statistics.getRenamedFieldStatistics().load(0, transitiveFieldMappings.size(), 0);
    }

    private void collectRejuvenationStatistics(
      final MappingStatistics statistics,
      final BiMap<ClassNode, ClassNode> additionallyMappedClasses,
      final Set<MethodNode> rejuvenatedMethods,
      final Set<FieldNode> rejuvenatedFields)
    {
        statistics.getRejuvenatedClassStatistics().load(0, additionallyMappedClasses.size(), 0); //This phase can not find or lose any entries.
        statistics.getRejuvenatedMethodStatistics().load(0, rejuvenatedMethods.size(), 0);
        statistics.getRejuvenatedFieldStatistics().load(0, rejuvenatedFields.size(), 0);
    }

    private MappingStatistics collectMappingStatistics(final JarMappingResult lastMappingResult)
    {
        final MappingStatistics statistics = new MappingStatistics();

        statistics.getDirectClassStatistics().loadFromMappingResult(lastMappingResult.classes());
        statistics.getDirectMethodStatistics().loadFromMappingResult(lastMappingResult.methods());
        statistics.getDirectFieldStatistics().loadFromMappingResult(lastMappingResult.fields());

        return statistics;
    }

    private void validateConfiguration(final Configuration configuration)
    {
        validateInputConfigurations(configuration.inputs());
    }

    private void prepare(final Configuration configuration)
    {
        Exceptions.log(LOGGER).run(
          Exceptions.sneak().runnable(
            () -> Files.createDirectories(configuration.outputConfiguration().outputDirectory())
          )
        );
    }

    private void validateInputConfigurations(final List<InputConfiguration> inputs)
    {
        if (inputs.size() < 2)
        {
            throw new IllegalStateException("At least two inputs are required.");
        }

        boolean doCheckIdentifier = false;
        boolean allHaveIdentifier;

        for (int i = 0; i < inputs.size(); i++)
        {
            final InputConfiguration input = inputs.get(i);
            if (!Files.exists(input.path()))
            {
                throw new IllegalStateException("The given configuration: " + input.name() + " targets an input path which does not exist: " + input.path());
            }

            if (i != inputs.size() - 1)
            {
                doCheckIdentifier |= input.identifier().isPresent();
                if (doCheckIdentifier)
                {
                    allHaveIdentifier = input.identifier().isPresent();
                    if (!allHaveIdentifier || i != 0)
                    {
                        throw new IllegalStateException("Not all inputs have an identity supplier. Only for the last input the supplier can be omitted.");
                    }
                }
            }
        }
    }


    private LinkedHashMap<TransitionMappingResultKey, JarMappingResult> buildTransitionMap(final Configuration configuration, final Map<String, LoadedASMData> dataByInputName)
    {
        final LinkedHashMap<TransitionMappingResultKey, JarMappingResult> transitionMappingResults = Maps.newLinkedHashMap();

        final LinkedList<InputConfiguration> inputs = new LinkedList<>(configuration.inputs());

        while (
          inputs.size() > 1
        )
        {
            InputConfiguration target = inputs.removeLast();
            InputConfiguration current = inputs.peekLast();

            LOGGER.info("Mapping {} to {}", Objects.requireNonNull(current).name(), target.name());

            JarMappingResult initialMappingResult = mapDirectly(
              dataByInputName.get(current.name()).classes(),
              dataByInputName.get(target.name()).classes(),
              configuration.runtimeConfiguration()
            );

            transitionMappingResults.put(new TransitionMappingResultKey(current.name(), target.name()), initialMappingResult);
        }

        return transitionMappingResults;
    }

    private BiMap<ClassNode, ClassNode> transitivelyMapRemainingClasses(
      final JarMappingResult currentGenToNextGenResult,
      final LinkedHashMap<TransitionMappingResultKey, JarMappingResult> transitions,
      MappingRuntimeConfiguration runtimeConfiguration
    )
    {
        final BiMap<ClassNode, ClassNode> additionallyMappedClasses = HashBiMap.create();

        final Set<ClassNode> nextGenUnmappedClasses = currentGenToNextGenResult.classes().unmappedSources();
        final Iterator<JarMappingResult> mappingResultIterator = transitions.values().iterator();
        mappingResultIterator.next(); //Skip the first since we already have that transition and are not interested in it.

        while (mappingResultIterator.hasNext() && !nextGenUnmappedClasses.isEmpty())
        {
            final MappingResult<ClassNode> transitiveMapping =
              runtimeConfiguration.classMapper().map(nextGenUnmappedClasses, mappingResultIterator.next().classes().unmappedCandidates());

            nextGenUnmappedClasses.clear();
            nextGenUnmappedClasses.addAll(transitiveMapping.unmappedSources());

            additionallyMappedClasses.putAll(transitiveMapping.mappings());
        }

        return additionallyMappedClasses;
    }

    Map<ClassNode, List<HistoricalClassMapping>> buildTransitiveClassMappings(final BiMap<ClassNode, ClassNode> currentClassMappings, final Collection<JarMappingResult> mappings)
    {
        final Iterator<JarMappingResult> iterator = mappings.iterator();
        final JarMappingResult initial = iterator.next(); //Skip the first since we already have that transition and are not interested in it.

        final Map<ClassNode, List<HistoricalClassMapping>> transitiveClassMappings = Maps.newHashMap();
        final BiMap<ClassNode, ClassNode> currentLastEntry = HashBiMap.create();

        currentClassMappings.forEach((source, target) -> {
            final Set<MethodNode> availableMethods = target.methods.stream().collect(SetsUtil.methods((node) -> target));
            final Set<FieldNode> availableFields = target.fields.stream().collect(SetsUtil.fields((node) -> target));

            if (initial.classes().mappings().containsKey(source)) {

                availableMethods.removeIf(method -> !initial.methods().unmappedCandidates().contains(method));
                availableFields.removeIf(field -> !initial.fields().unmappedCandidates().contains(field));
            }
            else
            {
                for (final JarMappingResult mapping : mappings)
                {
                    if (mapping.classes().unmappedCandidates().contains(target)) {
                        availableMethods.removeIf(method -> !mapping.methods().unmappedCandidates().contains(method));
                        availableFields.removeIf(field -> !mapping.fields().unmappedCandidates().contains(field));
                        break;
                    }
                }
            }

            transitiveClassMappings.computeIfAbsent(source, k -> new LinkedList<>()).add(new HistoricalClassMapping(target, availableMethods, availableFields));
            currentLastEntry.put(source, target);
        });

        final BiMap<ClassNode, ClassNode> invertedCurrentLastEntry = currentLastEntry.inverse();

        while (iterator.hasNext())
        {
            final JarMappingResult next = iterator.next();

            final Set<ClassNode> lastEntries = currentLastEntry.values().stream().collect(SetsUtil.classes());
            for (final ClassNode lastEntry : lastEntries)
            {
                if (next.classes().mappings().containsKey(lastEntry))
                {
                    final ClassNode newLastInChain = next.classes().mappings().get(lastEntry);
                    final ClassNode nextGenNode = invertedCurrentLastEntry.get(lastEntry);

                    final Set<MethodNode> availableMethods = newLastInChain.methods.stream().collect(SetsUtil.methods((node) -> newLastInChain));
                    final Set<FieldNode> availableFields = newLastInChain.fields.stream().collect(SetsUtil.fields((node) -> newLastInChain));

                    availableMethods.removeIf(method -> !next.methods().unmappedCandidates().contains(method));
                    availableFields.removeIf(field -> !next.fields().unmappedCandidates().contains(field));

                    transitiveClassMappings.computeIfAbsent(nextGenNode, (key) -> new LinkedList<>()).add(new HistoricalClassMapping(newLastInChain, availableMethods, availableFields));

                    currentLastEntry.remove(nextGenNode);
                    currentLastEntry.put(nextGenNode, newLastInChain);
                }
            }
        }

        return transitiveClassMappings;
    }

    private JarMappingResult mapDirectly(final Set<ClassNode> currentGenClasses, final Set<ClassNode> nextGenClasses, final MappingRuntimeConfiguration runtimeConfiguration)
    {
        final MappingResult<ClassNode> classMappingResult = runtimeConfiguration.classMapper().map(nextGenClasses, currentGenClasses);

        final Set<MethodNode> unmappedCurrentGenMethods = Sets.newHashSet();
        final Set<MethodNode> unmappedNextGenMethods = Sets.newHashSet();
        final BiMap<MethodNode, MethodNode> mappedMethods = HashBiMap.create();

        classMappingResult.unmappedCandidates().stream()
          .flatMap(classNode -> classNode.methods.stream())
          .forEach(unmappedCurrentGenMethods::add);

        classMappingResult.unmappedSources().stream()
          .flatMap(classNode -> classNode.methods.stream())
          .forEach(unmappedNextGenMethods::add);

        classMappingResult.mappings()
          .forEach((nextGenClass, currentGenClass) -> {
              final Set<MethodNode> nextGenMethods = nextGenClass.methods.stream().collect(SetsUtil.methods((node) -> nextGenClass));
              final Set<MethodNode> currentGenMethods = currentGenClass.methods.stream().collect(SetsUtil.methods((node) -> currentGenClass));
              
              final MappingResult<MethodNode> classMethodMapping =
                runtimeConfiguration.methodMapper().map(nextGenMethods, currentGenMethods);

              unmappedCurrentGenMethods.addAll(classMethodMapping.unmappedCandidates());
              unmappedNextGenMethods.addAll(classMethodMapping.unmappedSources());
              mappedMethods.putAll(classMethodMapping.mappings());
          });

        final MappingResult<MethodNode> methodMappingResult = new MappingResult<>(unmappedNextGenMethods, mappedMethods, unmappedCurrentGenMethods);

        final Set<FieldNode> unmappedCurrentGenFields = Sets.newHashSet();
        final Set<FieldNode> unmappedNextGenFields = Sets.newHashSet();
        final BiMap<FieldNode, FieldNode> mappedFields = HashBiMap.create();

        classMappingResult.unmappedCandidates().stream()
          .flatMap(classNode -> classNode.fields.stream())
          .forEach(unmappedCurrentGenFields::add);

        classMappingResult.unmappedSources().stream()
          .flatMap(classNode -> classNode.fields.stream())
          .forEach(unmappedNextGenFields::add);

        classMappingResult.mappings()
          .forEach((nextGenClass, currentGenClass) -> {
              final Set<FieldNode> nextGenFields = nextGenClass.fields.stream().collect(SetsUtil.fields((node) -> nextGenClass));
              final Set<FieldNode> currentGenFields = currentGenClass.fields.stream().collect(SetsUtil.fields((node) -> currentGenClass));
              
              final MappingResult<FieldNode> classFieldMapping =
                runtimeConfiguration.fieldMapper().map(nextGenFields, currentGenFields);

              unmappedCurrentGenFields.addAll(classFieldMapping.unmappedCandidates());
              unmappedNextGenFields.addAll(classFieldMapping.unmappedSources());
              mappedFields.putAll(classFieldMapping.mappings());
          });

        final MappingResult<FieldNode> fieldMappingResult = new MappingResult<>(unmappedNextGenFields, mappedFields, unmappedCurrentGenFields);

        return new JarMappingResult(
          classMappingResult,
          methodMappingResult,
          fieldMappingResult
        );
    }
    
    private BiMap<MethodNode, MethodNode> mapMethodsTransitively(final Set<MethodNode> unmappedMethods, final Map<MethodNode, ClassNode> methodOwners, final Map<ClassNode, List<HistoricalClassMapping>> history, MappingRuntimeConfiguration runtimeConfiguration)
    {
        final BiMap<MethodNode, MethodNode> additionallyMappedMethods = HashBiMap.create();
        final Multimap<ClassNode, MethodNode> unmappedMethodsByOwner = Multimaps.index(unmappedMethods, methodOwners::get);
        unmappedMethodsByOwner.keySet().forEach(
          nextGenClass -> {
              final List<HistoricalClassMapping> workingHistory = getAdditionalHistoryOfClass(history, nextGenClass);
              if (workingHistory.isEmpty())
                  return;

              final Set<MethodNode> unmappedMethodsInClass = unmappedMethodsByOwner.get(nextGenClass).stream().collect(SetsUtil.methods((node) -> nextGenClass));
              for (final HistoricalClassMapping classMapping : workingHistory)
              {
                  final MappingResult<MethodNode> mappingResult = runtimeConfiguration.methodMapper().map(unmappedMethodsInClass, classMapping.unmappedMethods());
                  additionallyMappedMethods.putAll(mappingResult.mappings());
                  unmappedMethodsInClass.removeAll(mappingResult.mappings().keySet());
              }
          }
        );

        return additionallyMappedMethods;
    }

    private BiMap<FieldNode, FieldNode> mapFieldsTransitively(final Set<FieldNode> unmappedFields, final Map<FieldNode, ClassNode> fieldOwners, final Map<ClassNode, List<HistoricalClassMapping>> history, MappingRuntimeConfiguration runtimeConfiguration)
    {
        final BiMap<FieldNode, FieldNode> additionallyMappedFields = HashBiMap.create();
        final Multimap<ClassNode, FieldNode> unmappedFieldsByOwner = Multimaps.index(unmappedFields, fieldOwners::get);
        unmappedFieldsByOwner.keySet().forEach(
          nextGenClass -> {
              final List<HistoricalClassMapping> workingHistory = getAdditionalHistoryOfClass(history, nextGenClass);
              if (workingHistory.isEmpty())
                  return;

              final Set<FieldNode> unmappedFieldsInClass = unmappedFieldsByOwner.get(nextGenClass).stream().collect(SetsUtil.fields((node) -> nextGenClass));
              for (final HistoricalClassMapping classMapping : workingHistory)
              {
                  final MappingResult<FieldNode> mappingResult = runtimeConfiguration.fieldMapper().map(unmappedFieldsInClass, classMapping.unmappedFields());
                  additionallyMappedFields.putAll(mappingResult.mappings());
                  unmappedFieldsInClass.removeAll(mappingResult.mappings().keySet());
              }
          }
        );

        return additionallyMappedFields;
    }

    private List<HistoricalClassMapping> getAdditionalHistoryOfClass(final Map<ClassNode, List<HistoricalClassMapping>> history, final ClassNode classNode) {
        final List<HistoricalClassMapping> historyForNextGenClass = history.getOrDefault(classNode, new LinkedList<>());
        if (historyForNextGenClass.isEmpty())
            return Collections.emptyList();

        final LinkedList<HistoricalClassMapping> workingHistory = new LinkedList<>(historyForNextGenClass);
        workingHistory.removeFirst(); //THis is the initial mapping, so we don't need to map it.
        return workingHistory;
    }

    private BiMap<ClassNode, Integer> determineClassIds(
      final BiMap<ClassNode, ClassNode> mappedClasses,
      final Set<ClassNode> unmappedClasses,
      final Map<ClassNode, String> configurationNameByClassNodes,
      final Map<String, InputConfiguration> configurationsByName,
      final OutputConfiguration outputConfiguration
    ) {
        final BiMap<ClassNode, Integer> classIds = HashBiMap.create();

        mappedClasses.forEach((nextGenClass, currentGenClass) -> {
            final String originalConfigurationName = configurationNameByClassNodes.get(currentGenClass);
            final InputConfiguration originalConfiguration = configurationsByName.get(originalConfigurationName);

            final int classId = originalConfiguration.identifier()
              .map(identifier -> identifier.getClassIdentity(currentGenClass))
              .orElseGet(() -> outputConfiguration.identifier().getClassIdentity(nextGenClass));

            classIds.put(nextGenClass, classId);
        });

        unmappedClasses.forEach(nextGenClass -> classIds.put(nextGenClass, outputConfiguration.identifier().getClassIdentity(nextGenClass)));

        return classIds;
    }

    private BiMap<FieldNode, Integer> determineFieldIds(
      final BiMap<FieldNode, FieldNode> mappedFields,
      final Set<FieldNode> unmappedFields,
      final Map<FieldNode, ClassNode> classNodesByFieldNodes,
      final Map<FieldNode, String> configurationNameByFieldNodes,
      final Map<String, InputConfiguration> configurationsByName,
      final OutputConfiguration outputConfiguration)
    {
        final BiMap<FieldNode, Integer> fieldIds = HashBiMap.create();

        mappedFields.forEach((nextGenField, currentGenField) -> {
            final String originalConfigurationName = configurationNameByFieldNodes.get(currentGenField);
            final InputConfiguration originalConfiguration = configurationsByName.get(originalConfigurationName);

            final ClassNode currentGenOwner = classNodesByFieldNodes.get(currentGenField);
            final ClassNode nextGenOwner = classNodesByFieldNodes.get(nextGenField);

            final int fieldId = originalConfiguration.identifier()
              .map(identifier -> identifier.getFieldIdentity(currentGenOwner, currentGenField))
              .orElseGet(() -> outputConfiguration.identifier().getFieldIdentity(nextGenOwner, nextGenField));

            fieldIds.put(nextGenField, fieldId);
        });

        unmappedFields.forEach(nextGenField -> {
            final ClassNode nextGenOwner = classNodesByFieldNodes.get(nextGenField);
            final int fieldId = outputConfiguration.identifier().getFieldIdentity(nextGenOwner, nextGenField);

            fieldIds.put(nextGenField, fieldId);
        });

        return fieldIds;
    }

    private MethodIdMappingResult determineMethodIds(
      final BiMap<MethodNode, MethodNode> mappedMethods,
      final Set<MethodNode> unmappedMethods,
      final Map<MethodNode, ClassNode> classNodesByMethodNodes,
      final Map<MethodNode, String> configurationNameByMethodNodes,
      final Map<String, InputConfiguration> configurationsByName,
      final OutputConfiguration outputConfiguration
    ) {
        final BiMap<MethodNode, Integer> methodIds = HashBiMap.create();
        final BiMap<ParameterNode, Integer> parameterIds = HashBiMap.create();

        mappedMethods.forEach((nextGenMethod, currentGenMethod) -> {
            final String originalConfigurationName = configurationNameByMethodNodes.get(currentGenMethod);
            final InputConfiguration originalConfiguration = configurationsByName.get(originalConfigurationName);

            final ClassNode currentGenOwner = classNodesByMethodNodes.get(currentGenMethod);
            final ClassNode nextGenOwner = classNodesByMethodNodes.get(nextGenMethod);

            final int methodId = originalConfiguration.identifier()
                                   .map(identifier -> identifier.getMethodIdentity(currentGenOwner, currentGenMethod))
                                   .orElseGet(() -> outputConfiguration.identifier().getMethodIdentity(nextGenOwner, nextGenMethod));
            
            methodIds.put(nextGenMethod, methodId);

            if (nextGenMethod.parameters != null)
            {
                for (int i = 0; i < nextGenMethod.parameters.size(); i++)
                {
                    final ParameterNode nextGenParameter = nextGenMethod.parameters.get(i);
                    final Type nextGenDescriptor = Type.getMethodType(nextGenMethod.desc);
                    if (currentGenMethod.parameters != null && currentGenMethod.parameters.size() > i) {
                        final ParameterNode currentGenParameter = currentGenMethod.parameters.get(i);
                        final Type currentGenDescriptor = Type.getMethodType(currentGenMethod.desc);

                        if (nextGenParameter.name.equals(currentGenParameter.name) &&
                              nextGenDescriptor.getArgumentTypes()[i].equals(currentGenDescriptor.getArgumentTypes()[i]))
                        {
                            final int parameterIndex = i;
                            final int parameterId = originalConfiguration.identifier()
                              .map(identifier -> identifier.getParameterIdentity(
                                currentGenOwner, currentGenMethod, currentGenParameter, parameterIndex
                              ))
                              .orElseGet(() -> outputConfiguration.identifier().getParameterIdentity(
                                nextGenOwner, nextGenMethod, nextGenParameter, parameterIndex
                              ));

                            if (parameterId >= 0) {
                                parameterIds.put(nextGenParameter, parameterId);

                                continue;
                            }
                        }
                    }

                    final int parameterId = outputConfiguration.identifier().getParameterIdentity(
                      nextGenOwner, nextGenMethod, nextGenParameter, i
                    );
                    parameterIds.put(nextGenParameter, parameterId);
                }
            }
        });
        
        unmappedMethods.forEach(nextGenMethod -> {
            final ClassNode nextGenOwner = classNodesByMethodNodes.get(nextGenMethod);
            final int methodId = outputConfiguration.identifier().getMethodIdentity(nextGenOwner, nextGenMethod);
            
            methodIds.put(nextGenMethod, methodId);

            if (nextGenMethod.parameters != null) {
                for (int i = 0; i < nextGenMethod.parameters.size(); i++)
                {
                    final boolean nextGenIsStatic = (nextGenMethod.access & Opcodes.ACC_STATIC) != 0;

                    final ParameterNode nextGenParameter = nextGenMethod.parameters.get(i);
                    final int parameterId = outputConfiguration.identifier().getParameterIdentity(
                      nextGenOwner, nextGenMethod, nextGenParameter, i + (nextGenIsStatic ? 0 : 1)
                    );
                    parameterIds.put(nextGenParameter, parameterId);
                }
            }
        });
        
        return new MethodIdMappingResult(methodIds, parameterIds);
    }

    private void writeOutput(
      final BiMap<ClassNode, Integer> classIds,
      final BiMap<MethodNode, Integer> methodIds,
      final BiMap<FieldNode, Integer> fieldIds,
      final BiMap<ParameterNode, Integer> parameterIds,
      final OutputConfiguration outputConfiguration,
      final LoadedASMData asmData)
    {
        LOGGER.info("Creating named AST");

        final INamedAST ast = outputConfiguration.astBuilder().build(
                classIds,
                methodIds,
                fieldIds,
                parameterIds,
                asmData,
                outputConfiguration.metadataProvider().ast()
        );

        LOGGER.info("Writing named AST");
        outputConfiguration.writer().write(
          outputConfiguration.outputDirectory(),
          outputConfiguration.metadataWritingConfiguration(),
          ast
        );
    }
    
    record TransitionMappingResultKey(String currentGenName, String nextGenName) {}

    record HistoricalClassMapping(ClassNode classNode, Set<MethodNode> unmappedMethods, Set<FieldNode> unmappedFields) {}

    record MethodIdMappingResult(BiMap<MethodNode, Integer> methodIds, BiMap<ParameterNode, Integer> parameterIds) { }
}
