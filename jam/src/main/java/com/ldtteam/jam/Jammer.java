package com.ldtteam.jam;

import com.google.common.collect.*;
import com.ldtteam.jam.loader.ASMDataLoader;
import com.ldtteam.jam.loader.LoadedASMData;
import com.ldtteam.jam.spi.IJammer;
import com.ldtteam.jam.spi.asm.*;
import com.ldtteam.jam.spi.ast.named.INamedAST;
import com.ldtteam.jam.spi.configuration.Configuration;
import com.ldtteam.jam.spi.configuration.InputConfiguration;
import com.ldtteam.jam.spi.configuration.MappingRuntimeConfiguration;
import com.ldtteam.jam.spi.configuration.OutputConfiguration;
import com.ldtteam.jam.spi.mapping.MappingResult;
import com.ldtteam.jam.spi.name.IExistingNameSupplier;
import com.ldtteam.jam.statistics.MappingStatistics;
import com.ldtteam.jam.util.MethodDataUtils;
import com.ldtteam.jam.util.SetsUtil;
import com.machinezoo.noexception.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Jammer implements IJammer {
    private final Logger LOGGER = LoggerFactory.getLogger(Jammer.class);

    @Override
    public void run(final Configuration configuration) {
        LOGGER.info("Starting Jammer. Version: " + getClass().getPackage().getImplementationVersion());
        LOGGER.info("Validating configuration");
        validateConfiguration(configuration);

        LOGGER.info("Preparing...");
        prepare(configuration);


        LOGGER.info("Loading data...");
        final Map<String, InputConfiguration> configurationsByName = configuration.inputs().stream()
                .collect(Collectors.toMap(InputConfiguration::name, Function.identity()));

        record RemapperCandidateByInputName(String name, Optional<IExistingNameSupplier> remapper) {
        }
        final BiMap<String, Optional<IExistingNameSupplier>> existingNameSupplierCandidateByName = configurationsByName
                .entrySet()
                .stream()
                .map(entry -> new RemapperCandidateByInputName(entry.getKey(), entry.getValue().names()))
                .collect(
                        Collectors.collectingAndThen(
                                Collectors.toMap(RemapperCandidateByInputName::name, RemapperCandidateByInputName::remapper),
                                HashBiMap::create
                        )
                );


        final Set<LoadedASMData> data = configuration.inputs().stream()
                .map(ASMDataLoader::load)
                .collect(Collectors.toSet());

        record LoadedASMDataByInputName(String name, LoadedASMData data) {
        }
        final BiMap<String, IASMData> dataByInputName = data.stream()
                .map(d -> new LoadedASMDataByInputName(d.name(), d))
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(LoadedASMDataByInputName::name, LoadedASMDataByInputName::data),
                        HashBiMap::create));
        record ConfigurationNameByClassDataEntry(ClassData classData, String name) {
        }
        final Map<ClassData, String> configurationNameByClassDatas = data.stream()
                .flatMap(inputData -> inputData.classes()
                        .stream()
                        .map(classData -> new ConfigurationNameByClassDataEntry(classData,
                                inputData.name())))
                .collect(Collectors.toMap(ConfigurationNameByClassDataEntry::classData,
                        ConfigurationNameByClassDataEntry::name));

        record ConfigurationNameByMethodDataEntry(MethodData methodData, String name) {
        }
        final Map<MethodData, String> configurationNameByMethodDatas = data.stream()
                .flatMap(inputData -> inputData.methods()
                        .stream()
                        .map(methodData -> new ConfigurationNameByMethodDataEntry(methodData,
                                inputData.name())))
                .collect(Collectors.toMap(ConfigurationNameByMethodDataEntry::methodData,
                        ConfigurationNameByMethodDataEntry::name));

        record ConfigurationNameByFieldDataEntry(FieldData fieldData, String name) {
        }
        final Map<FieldData, String> configurationNameByFieldDatas = data.stream()
                .flatMap(inputData -> inputData.fields()
                        .stream()
                        .map(fieldData -> new ConfigurationNameByFieldDataEntry(fieldData,
                                inputData.name())))
                .collect(Collectors.toMap(ConfigurationNameByFieldDataEntry::fieldData,
                        ConfigurationNameByFieldDataEntry::name));

        record ConfigurationNameByParameterDataEntry(ParameterData parameterData, String name) {
        }
        final Map<ParameterData, String> configurationNameByParameterDatas = data.stream()
                .flatMap(inputData -> inputData.parameters()
                        .stream()
                        .map(parameterData -> new ConfigurationNameByParameterDataEntry(parameterData,
                                inputData.name())))
                .collect(Collectors.toMap(ConfigurationNameByParameterDataEntry::parameterData,
                        ConfigurationNameByParameterDataEntry::name));

        record ClassDatasByMethodDataEntry(MethodData methodData, ClassData classData) {
        }
        final Map<MethodData, ClassData> classDatasByMethodDatas = data.stream()
                .flatMap(inputData -> inputData.classes().stream())
                .flatMap(classData -> classData.node().methods.stream()
                        .map(node -> new MethodData(classData, node))
                        .map(methodData -> new ClassDatasByMethodDataEntry(methodData, classData)))
                .collect(Collectors.toMap(ClassDatasByMethodDataEntry::methodData, ClassDatasByMethodDataEntry::classData));

        record ClassDatasByFieldDataEntry(FieldData fieldData, ClassData classData) {
        }
        final Map<FieldData, ClassData> classDatasByFieldDatas = data.stream()
                .flatMap(inputData -> inputData.classes().stream())
                .flatMap(classData -> classData.node().fields.stream()
                        .map(node -> new FieldData(classData, node))
                        .map(fieldData -> new ClassDatasByFieldDataEntry(fieldData, classData)))
                .collect(Collectors.toMap(ClassDatasByFieldDataEntry::fieldData, ClassDatasByFieldDataEntry::classData));

        record MethodDatasByParameterDataEntry(ParameterData parameterData, MethodData methodData) {
        }
        final Map<ParameterData, MethodData> methodDatasByParameterDatas = data.stream()
                .flatMap(inputData -> inputData.methods().stream())
                .flatMap(methodData -> MethodDataUtils.parametersAsList(methodData).stream()
                        .map(parameterData -> new MethodDatasByParameterDataEntry(parameterData, methodData)))
                .collect(Collectors.toMap(MethodDatasByParameterDataEntry::parameterData, MethodDatasByParameterDataEntry::methodData));

        LOGGER.info("Mapping direct inputs...");
        final LinkedHashMap<TransitionMappingResultKey, JarMappingResult> transitionMappings = buildTransitionMap(configuration, dataByInputName);
        final JarMappingResult lastMappingResult = transitionMappings.values().iterator().next();

        LOGGER.info("Collecting primary mapping statistics...");
        MappingStatistics mappingStatistics = collectMappingStatistics(lastMappingResult);

        LOGGER.info("Reconstructing transitively lost class mappings...");
        final Set<ClassData> unmappedClasses = Sets.newHashSet(lastMappingResult.classes().unmappedSources());
        final Set<MethodData> unmappedMethods = Sets.newHashSet(lastMappingResult.methods().unmappedSources());
        final Set<FieldData> unmappedFields = Sets.newHashSet(lastMappingResult.fields().unmappedSources());
        final Set<ParameterData> unmappedParameters = Sets.newHashSet(lastMappingResult.parameters().unmappedSources());

        final BiMap<ClassData, ClassData> mappedClasses = HashBiMap.create(lastMappingResult.classes().mappings());
        final BiMap<MethodData, MethodData> mappedMethods = HashBiMap.create(lastMappingResult.methods().mappings());
        final BiMap<FieldData, FieldData> mappedFields = HashBiMap.create(lastMappingResult.fields().mappings());
        final BiMap<ParameterData, ParameterData> mappedParameters = HashBiMap.create(lastMappingResult.parameters().mappings());

        final BiMap<ClassData, ClassData> additionallyMappedClasses = transitivelyMapRemainingClasses(
                lastMappingResult,
                transitionMappings,
                configuration.runtimeConfiguration()
        );

        mappedClasses.putAll(additionallyMappedClasses);
        unmappedClasses.removeAll(additionallyMappedClasses.keySet());

        LOGGER.info("Reconstructing transitively lost method and parameters mappings...");
        final Set<MethodData> rejuvenatedMethods = Sets.newHashSet();
        final Set<ParameterData> rejuvenatedParameters = Sets.newHashSet();
        additionallyMappedClasses.forEach((nextGenClass, transitiveCurrentGenClass) -> {
            //We are talking about the A_A case here, so no methods inside this will be mapped.
            final Set<MethodData> unmappedNextGenMethods = nextGenClass.node().methods.stream().map(node -> new MethodData(nextGenClass, node)).collect(SetsUtil.methods());
            final Set<MethodData> unmappedCurrentGenMethods = transitiveCurrentGenClass.node().methods.stream().map(node -> new MethodData(transitiveCurrentGenClass, node)).collect(SetsUtil.methods());

            final MappingResult<MethodData> transitiveMethodMapping = configuration.runtimeConfiguration().methodMapper().map(unmappedNextGenMethods, unmappedCurrentGenMethods);

            unmappedMethods.removeAll(transitiveMethodMapping.mappings().keySet());
            mappedMethods.putAll(transitiveMethodMapping.mappings());
            rejuvenatedMethods.addAll(transitiveMethodMapping.mappings().keySet());

            transitiveMethodMapping.mappings()
                    .forEach((nextGenMethod, transitiveCurrentGenMethod) -> {
                        final Set<ParameterData> unmappedNextGenParameters = MethodDataUtils.parametersAsSet(nextGenMethod);
                        final Set<ParameterData> unmappedCurrentGenParameters = MethodDataUtils.parametersAsSet(transitiveCurrentGenMethod);

                        final MappingResult<ParameterData> transitiveParameterMapping = configuration.runtimeConfiguration().parameterMapper().map(unmappedNextGenParameters, unmappedCurrentGenParameters);

                        unmappedParameters.removeAll(transitiveParameterMapping.mappings().keySet());
                        mappedParameters.putAll(transitiveParameterMapping.mappings());
                        rejuvenatedParameters.addAll(transitiveParameterMapping.mappings().keySet());
                    });
        });

        LOGGER.info("Reconstructing transitively lost field mappings...");
        final Set<FieldData> rejuvenatedFields = Sets.newHashSet();
        additionallyMappedClasses.forEach((nextGenClass, transitiveCurrentGenClass) -> {
            //We are talking about the A_A case here, so no fields inside this will be mapped.
            final Set<FieldData> unmappedNextGenFields = nextGenClass.node().fields.stream().map(node -> new FieldData(nextGenClass, node)).collect(SetsUtil.fields());
            final Set<FieldData> unmappedCurrentGenFields = transitiveCurrentGenClass.node().fields.stream().map(node -> new FieldData(transitiveCurrentGenClass, node)).collect(SetsUtil.fields());

            final MappingResult<FieldData> transitiveFieldMapping = configuration.runtimeConfiguration().fieldMapper().map(unmappedNextGenFields, unmappedCurrentGenFields);

            unmappedFields.removeAll(transitiveFieldMapping.mappings().keySet());
            mappedFields.putAll(transitiveFieldMapping.mappings());
            rejuvenatedFields.addAll(transitiveFieldMapping.mappings().keySet());
        });

        LOGGER.info("Collecting rejuvenation statistics...");
        collectRejuvenationStatistics(mappingStatistics, additionallyMappedClasses, rejuvenatedMethods, rejuvenatedFields, rejuvenatedParameters);

        LOGGER.info("Building transitive class mappings...");
        Map<ClassData, List<HistoricalClassMapping>> transitiveClassMappings = buildTransitiveClassMappings(mappedClasses, transitionMappings.values());

        LOGGER.info("Building transitive method mappings...");
        Map<MethodData, List<HistoricalMethodMapping>> transitiveMethodMappings = buildTransitiveMethodMappings(mappedMethods, transitionMappings.values());

        LOGGER.info("Building transitive method mappings...");
        final BiMap<MethodData, MethodData> transitivelyMappedMethodMappings =
                mapMethodsTransitively(unmappedMethods, classDatasByMethodDatas, transitiveClassMappings, configuration.runtimeConfiguration());
        unmappedMethods.removeAll(transitivelyMappedMethodMappings.keySet());
        mappedMethods.putAll(transitivelyMappedMethodMappings);

        LOGGER.info("Building transitive parameter mappings...");
        final BiMap<ParameterData, ParameterData> transitivelyMappedParameterMappings =
                mapParametersTransitively(unmappedParameters, methodDatasByParameterDatas, transitiveMethodMappings, configuration.runtimeConfiguration());
        unmappedParameters.removeAll(transitivelyMappedParameterMappings.keySet());
        mappedParameters.putAll(transitivelyMappedParameterMappings);

        LOGGER.info("Building transitive field mappings...");
        final BiMap<FieldData, FieldData> transitivelyMappedFieldMappings =
                mapFieldsTransitively(unmappedFields, classDatasByFieldDatas, transitiveClassMappings, configuration.runtimeConfiguration());
        unmappedFields.removeAll(transitivelyMappedFieldMappings.keySet());
        mappedFields.putAll(transitivelyMappedFieldMappings);

        LOGGER.info("Collecting renaming statistics...");
        collectRenamingStatistics(mappingStatistics, transitivelyMappedMethodMappings, transitivelyMappedFieldMappings, transitivelyMappedParameterMappings);

        LOGGER.info("Determining class ids...");
        final BiMap<ClassData, Integer> classIds =
                determineClassIds(mappedClasses, unmappedClasses, configurationNameByClassDatas, configurationsByName, configuration.outputConfiguration());

        LOGGER.info("Determining field ids...");
        final BiMap<FieldData, Integer> fieldIds =
                determineFieldIds(mappedFields, unmappedFields, configurationNameByFieldDatas, configurationsByName, configuration.outputConfiguration());

        LOGGER.info("Determining method ids...");
        final BiMap<MethodData, Integer> methodIds =
                determineMethodIds(mappedMethods, unmappedMethods, configurationNameByMethodDatas, configurationsByName, configuration.outputConfiguration());

        LOGGER.info("Determining parameter ids...");
        final BiMap<ParameterData, Integer> parameterIds =
                determineParameterIds(mappedParameters, unmappedParameters, configurationNameByParameterDatas, configurationsByName, configuration.outputConfiguration());

        LOGGER.info("Writing mappings...");
        final IASMData targetASMData = dataByInputName.get(Objects.requireNonNull(configuration.inputs().get(configuration.inputs().size() - 1)).name());
        writeOutput(
                dataByInputName.inverse(),
                existingNameSupplierCandidateByName,
                mappedClasses,
                mappedFields,
                mappedMethods,
                mappedParameters,
                classIds,
                methodIds,
                fieldIds,
                parameterIds,
                configuration.outputConfiguration(),
                targetASMData
        );

        LOGGER.info("Collecting total statistics...");
        collectTotalStatistics(mappingStatistics, mappedClasses, mappedMethods, mappedFields, mappedParameters, unmappedClasses, unmappedMethods, unmappedFields, unmappedParameters);

        LOGGER.info("Writing statistics...");
        writeStatistics(mappingStatistics, configuration);
    }

    private void writeStatistics(final MappingStatistics mappingStatistics, final Configuration configuration) {
        configuration.outputConfiguration().statisticsWriter().write(
                configuration.outputConfiguration().outputDirectory(),
                mappingStatistics,
                configuration
        );
    }

    private void collectTotalStatistics(
            final MappingStatistics mappingStatistics,
            final BiMap<ClassData, ClassData> mappedClasses,
            final BiMap<MethodData, MethodData> mappedMethods,
            final BiMap<FieldData, FieldData> mappedFields,
            final BiMap<ParameterData, ParameterData> mappedParameters,
            final Set<ClassData> unmappedClasses,
            final Set<MethodData> unmappedMethods,
            final Set<FieldData> unmappedFields,
            final Set<ParameterData> unmappedParameters) {
        mappingStatistics.getTotalClassStatistics().load(0, mappedClasses.size(), unmappedClasses.size());
        mappingStatistics.getTotalMethodStatistics().load(0, mappedMethods.size(), unmappedMethods.size());
        mappingStatistics.getTotalFieldStatistics().load(0, mappedFields.size(), unmappedFields.size());
        mappingStatistics.getTotalParameterStatistics().load(0, mappedParameters.size(), unmappedParameters.size());
    }

    private void collectRenamingStatistics(
            final MappingStatistics statistics,
            final BiMap<MethodData, MethodData> transitiveMethodMappings,
            final BiMap<FieldData, FieldData> transitiveFieldMappings,
            final BiMap<ParameterData, ParameterData> transitiveParameterMappings) {
        statistics.getRenamedClassStatistics().load(0, 0, 0); //Renaming never happens, jammer for now does not support this scenario.
        statistics.getRenamedMethodStatistics().load(0, transitiveMethodMappings.size(), 0);
        statistics.getRenamedFieldStatistics().load(0, transitiveFieldMappings.size(), 0);
        statistics.getRenamedParameterStatistics().load(0, transitiveParameterMappings.size(), 0);
    }

    private void collectRejuvenationStatistics(
            final MappingStatistics statistics,
            final BiMap<ClassData, ClassData> additionallyMappedClasses,
            final Set<MethodData> rejuvenatedMethods,
            final Set<FieldData> rejuvenatedFields,
            final Set<ParameterData> rejuvenatedParameters) {
        statistics.getRejuvenatedClassStatistics().load(0, additionallyMappedClasses.size(), 0); //This phase can not find or lose any entries.
        statistics.getRejuvenatedMethodStatistics().load(0, rejuvenatedMethods.size(), 0);
        statistics.getRejuvenatedFieldStatistics().load(0, rejuvenatedFields.size(), 0);
        statistics.getRejuvenatedParameterStatistics().load(0, rejuvenatedParameters.size(), 0);
    }

    private MappingStatistics collectMappingStatistics(final JarMappingResult lastMappingResult) {
        final MappingStatistics statistics = new MappingStatistics();

        statistics.getDirectClassStatistics().loadFromMappingResult(lastMappingResult.classes());
        statistics.getDirectMethodStatistics().loadFromMappingResult(lastMappingResult.methods());
        statistics.getDirectFieldStatistics().loadFromMappingResult(lastMappingResult.fields());
        statistics.getDirectParameterStatistics().loadFromMappingResult(lastMappingResult.parameters());

        return statistics;
    }

    private void validateConfiguration(final Configuration configuration) {
        validateInputConfigurations(configuration.inputs());
    }

    private void prepare(final Configuration configuration) {
        Exceptions.log(LOGGER).run(
                Exceptions.sneak().runnable(
                        () -> Files.createDirectories(configuration.outputConfiguration().outputDirectory())
                )
        );
    }

    private void validateInputConfigurations(final List<InputConfiguration> inputs) {
        if (inputs.size() < 2) {
            throw new IllegalStateException("At least two inputs are required.");
        }

        boolean doCheckIdentifier = false;
        boolean allHaveIdentifier;

        for (int i = 0; i < inputs.size(); i++) {
            final InputConfiguration input = inputs.get(i);
            if (!Files.exists(input.path())) {
                throw new IllegalStateException("The given configuration: " + input.name() + " targets an input path which does not exist: " + input.path());
            }

            if (i != inputs.size() - 1) {
                doCheckIdentifier |= input.identifier().isPresent();
                if (doCheckIdentifier) {
                    allHaveIdentifier = input.identifier().isPresent();
                    if (!allHaveIdentifier || i != 0) {
                        throw new IllegalStateException("Not all inputs have an identity supplier. Only for the last input the supplier can be omitted.");
                    }
                }
            }
        }
    }

    private LinkedHashMap<TransitionMappingResultKey, JarMappingResult> buildTransitionMap(final Configuration configuration, final Map<String, IASMData> dataByInputName) {
        final LinkedHashMap<TransitionMappingResultKey, JarMappingResult> transitionMappingResults = Maps.newLinkedHashMap();

        final LinkedList<InputConfiguration> inputs = new LinkedList<>(configuration.inputs());

        while (
                inputs.size() > 1
        ) {
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

    private BiMap<ClassData, ClassData> transitivelyMapRemainingClasses(
            final JarMappingResult currentGenToNextGenResult,
            final LinkedHashMap<TransitionMappingResultKey, JarMappingResult> transitions,
            MappingRuntimeConfiguration runtimeConfiguration
    ) {
        final BiMap<ClassData, ClassData> additionallyMappedClasses = HashBiMap.create();

        final Set<ClassData> nextGenUnmappedClasses = currentGenToNextGenResult.classes().unmappedSources();
        final Iterator<JarMappingResult> mappingResultIterator = transitions.values().iterator();
        mappingResultIterator.next(); //Skip the first since we already have that transition and are not interested in it.

        while (mappingResultIterator.hasNext() && !nextGenUnmappedClasses.isEmpty()) {
            final MappingResult<ClassData> transitiveMapping =
                    runtimeConfiguration.classMapper().map(nextGenUnmappedClasses, mappingResultIterator.next().classes().unmappedCandidates());

            nextGenUnmappedClasses.clear();
            nextGenUnmappedClasses.addAll(transitiveMapping.unmappedSources());

            additionallyMappedClasses.putAll(transitiveMapping.mappings());
        }

        return additionallyMappedClasses;
    }

    Map<ClassData, List<HistoricalClassMapping>> buildTransitiveClassMappings(final BiMap<ClassData, ClassData> currentClassMappings, final Collection<JarMappingResult> mappings) {
        final Iterator<JarMappingResult> iterator = mappings.iterator();
        final JarMappingResult initial = iterator.next(); //Skip the first since we already have that transition and are not interested in it.

        final Map<ClassData, List<HistoricalClassMapping>> transitiveClassMappings = Maps.newHashMap();
        final BiMap<ClassData, ClassData> currentLastEntry = HashBiMap.create();

        currentClassMappings.forEach((source, target) -> {
            final Set<MethodData> availableMethods = target.node().methods.stream().map(node -> new MethodData(target, node)).collect(SetsUtil.methods());
            final Set<FieldData> availableFields = target.node().fields.stream().map(node -> new FieldData(target, node)).collect(SetsUtil.fields());

            if (initial.classes().mappings().containsKey(source)) {

                availableMethods.removeIf(method -> !initial.methods().unmappedCandidates().contains(method));
                availableFields.removeIf(field -> !initial.fields().unmappedCandidates().contains(field));
            } else {
                for (final JarMappingResult mapping : mappings) {
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

        final BiMap<ClassData, ClassData> invertedCurrentLastEntry = currentLastEntry.inverse();

        while (iterator.hasNext()) {
            final JarMappingResult next = iterator.next();

            final Set<ClassData> lastEntries = currentLastEntry.values().stream().collect(SetsUtil.classes());
            for (final ClassData lastEntry : lastEntries) {
                if (next.classes().mappings().containsKey(lastEntry)) {
                    final ClassData newLastInChain = next.classes().mappings().get(lastEntry);
                    final ClassData nextGenData = invertedCurrentLastEntry.get(lastEntry);

                    final Set<MethodData> availableMethods = newLastInChain.node().methods.stream().map(node -> new MethodData(newLastInChain, node)).collect(SetsUtil.methods());
                    final Set<FieldData> availableFields = newLastInChain.node().fields.stream().map(node -> new FieldData(newLastInChain, node)).collect(SetsUtil.fields());

                    availableMethods.removeIf(method -> !next.methods().unmappedCandidates().contains(method));
                    availableFields.removeIf(field -> !next.fields().unmappedCandidates().contains(field));

                    transitiveClassMappings.computeIfAbsent(nextGenData, (key) -> new LinkedList<>())
                            .add(new HistoricalClassMapping(newLastInChain, availableMethods, availableFields));

                    currentLastEntry.remove(nextGenData);
                    currentLastEntry.put(nextGenData, newLastInChain);
                }
            }
        }

        return transitiveClassMappings;
    }

    Map<MethodData, List<HistoricalMethodMapping>> buildTransitiveMethodMappings(final BiMap<MethodData, MethodData> currentMethodMappings, final Collection<JarMappingResult> mappings) {
        final Iterator<JarMappingResult> iterator = mappings.iterator();
        final JarMappingResult initial = iterator.next(); //Skip the first since we already have that transition and are not interested in it.

        final Map<MethodData, List<HistoricalMethodMapping>> transitiveMethodMappings = Maps.newHashMap();
        final BiMap<MethodData, MethodData> currentLastEntry = HashBiMap.create();

        currentMethodMappings.forEach((source, target) -> {
            final Set<ParameterData> availableParameters = MethodDataUtils.parametersAsSet(target);

            if (initial.methods().mappings().containsKey(source)) {
                availableParameters.removeIf(method -> !initial.parameters().unmappedCandidates().contains(method));
            } else {
                for (final JarMappingResult mapping : mappings) {
                    if (mapping.methods().unmappedCandidates().contains(target)) {
                        availableParameters.removeIf(method -> !mapping.parameters().unmappedCandidates().contains(method));
                        break;
                    }
                }
            }

            transitiveMethodMappings.computeIfAbsent(source, k -> new LinkedList<>()).add(new HistoricalMethodMapping(target, availableParameters));
            currentLastEntry.put(source, target);
        });

        final BiMap<MethodData, MethodData> invertedCurrentLastEntry = currentLastEntry.inverse();

        while (iterator.hasNext()) {
            final JarMappingResult next = iterator.next();

            final Set<MethodData> lastEntries = currentLastEntry.values().stream().collect(SetsUtil.methods());
            for (final MethodData lastEntry : lastEntries) {
                if (next.methods().mappings().containsKey(lastEntry)) {
                    final MethodData newLastInChain = next.methods().mappings().get(lastEntry);
                    final MethodData nextGenData = invertedCurrentLastEntry.get(lastEntry);

                    final Set<ParameterData> availableParameters = MethodDataUtils.parametersAsSet(newLastInChain);

                    availableParameters.removeIf(method -> !next.parameters().unmappedCandidates().contains(method));

                    transitiveMethodMappings.computeIfAbsent(nextGenData, (key) -> new LinkedList<>())
                            .add(new HistoricalMethodMapping(newLastInChain, availableParameters));

                    currentLastEntry.remove(nextGenData);
                    currentLastEntry.put(nextGenData, newLastInChain);
                }
            }
        }

        return transitiveMethodMappings;
    }

    private JarMappingResult mapDirectly(final Set<ClassData> currentGenClasses, final Set<ClassData> nextGenClasses, final MappingRuntimeConfiguration runtimeConfiguration) {
        final MappingResult<ClassData> classMappingResult = runtimeConfiguration.classMapper().map(nextGenClasses, currentGenClasses);

        final Set<MethodData> unmappedCurrentGenMethods = Sets.newHashSet();
        final Set<MethodData> unmappedNextGenMethods = Sets.newHashSet();
        final BiMap<MethodData, MethodData> mappedMethods = HashBiMap.create();

        final Set<ParameterData> unmappedCurrentGenParameters = Sets.newHashSet();
        final Set<ParameterData> unmappedNextGenParameters = Sets.newHashSet();
        final BiMap<ParameterData, ParameterData> mappedParameters = HashBiMap.create();

        classMappingResult.unmappedCandidates().stream()
                .flatMap(classData -> classData.node().methods.stream()
                        .map(method -> new MethodData(classData, method)))
                .peek(methodData -> unmappedCurrentGenParameters.addAll(MethodDataUtils.parametersAsSet(methodData)))
                .forEach(unmappedCurrentGenMethods::add);

        classMappingResult.unmappedSources().stream()
                .flatMap(classData -> classData.node().methods.stream()
                        .map(method -> new MethodData(classData, method)))
                .peek(methodData -> unmappedNextGenParameters.addAll(MethodDataUtils.parametersAsSet(methodData)))
                .forEach(unmappedNextGenMethods::add);

        classMappingResult.mappings()
                .forEach((nextGenClass, currentGenClass) -> {
                    final Set<MethodData> nextGenMethods = nextGenClass.node().methods.stream().map(node -> new MethodData(nextGenClass, node)).collect(SetsUtil.methods());
                    final Set<MethodData> currentGenMethods = currentGenClass.node().methods.stream().map(node -> new MethodData(currentGenClass, node)).collect(SetsUtil.methods());

                    final MappingResult<MethodData> classMethodMapping =
                            runtimeConfiguration.methodMapper().map(nextGenMethods, currentGenMethods);

                    unmappedCurrentGenMethods.addAll(classMethodMapping.unmappedCandidates());
                    unmappedNextGenMethods.addAll(classMethodMapping.unmappedSources());
                    mappedMethods.putAll(classMethodMapping.mappings());

                    classMethodMapping.unmappedSources().stream()
                            .flatMap(MethodDataUtils::parametersAsStream)
                            .forEach(unmappedNextGenParameters::add);

                    classMethodMapping.mappings()
                            .forEach((nextGenMethod, currentGenMethod) -> {
                                final Set<ParameterData> nextGenParameters = MethodDataUtils.parametersAsSet(nextGenMethod);
                                final Set<ParameterData> currentGenParameters = MethodDataUtils.parametersAsSet(currentGenMethod);

                                final MappingResult<ParameterData> classParameterMapping =
                                        runtimeConfiguration.parameterMapper().map(nextGenParameters, currentGenParameters);

                                unmappedCurrentGenParameters.addAll(classParameterMapping.unmappedCandidates());
                                unmappedNextGenParameters.addAll(classParameterMapping.unmappedSources());
                                mappedParameters.putAll(classParameterMapping.mappings());
                            });
                });

        final MappingResult<MethodData> methodMappingResult = new MappingResult<>(unmappedNextGenMethods, mappedMethods, unmappedCurrentGenMethods);
        final MappingResult<ParameterData> parameterMappingResult = new MappingResult<>(unmappedNextGenParameters, mappedParameters, unmappedCurrentGenParameters);

        final Set<FieldData> unmappedCurrentGenFields = Sets.newHashSet();
        final Set<FieldData> unmappedNextGenFields = Sets.newHashSet();
        final BiMap<FieldData, FieldData> mappedFields = HashBiMap.create();

        classMappingResult.unmappedCandidates().stream()
                .flatMap(classData -> classData.node().fields.stream()
                        .map(field -> new FieldData(classData, field)))
                .forEach(unmappedCurrentGenFields::add);

        classMappingResult.unmappedSources().stream()
                .flatMap(classData -> classData.node().fields.stream()
                        .map(field -> new FieldData(classData, field)))
                .forEach(unmappedNextGenFields::add);

        classMappingResult.mappings()
                .forEach((nextGenClass, currentGenClass) -> {
                    final Set<FieldData> nextGenFields = nextGenClass.node().fields.stream().map(node -> new FieldData(nextGenClass, node)).collect(SetsUtil.fields());
                    final Set<FieldData> currentGenFields = currentGenClass.node().fields.stream().map(node -> new FieldData(currentGenClass, node)).collect(SetsUtil.fields());

                    final MappingResult<FieldData> classFieldMapping =
                            runtimeConfiguration.fieldMapper().map(nextGenFields, currentGenFields);

                    unmappedCurrentGenFields.addAll(classFieldMapping.unmappedCandidates());
                    unmappedNextGenFields.addAll(classFieldMapping.unmappedSources());
                    mappedFields.putAll(classFieldMapping.mappings());
                });

        final MappingResult<FieldData> fieldMappingResult = new MappingResult<>(unmappedNextGenFields, mappedFields, unmappedCurrentGenFields);

        return new JarMappingResult(
                classMappingResult,
                methodMappingResult,
                fieldMappingResult,
                parameterMappingResult
        );
    }

    private BiMap<MethodData, MethodData> mapMethodsTransitively(
            final Set<MethodData> unmappedMethods,
            final Map<MethodData, ClassData> methodOwners,
            final Map<ClassData, List<HistoricalClassMapping>> history,
            MappingRuntimeConfiguration runtimeConfiguration) {
        final BiMap<MethodData, MethodData> additionallyMappedMethods = HashBiMap.create();
        final Multimap<ClassData, MethodData> unmappedMethodsByOwner = Multimaps.index(unmappedMethods, methodOwners::get);
        unmappedMethodsByOwner.keySet().forEach(
                nextGenClass -> {
                    final List<HistoricalClassMapping> workingHistory = getAdditionalHistoryOfClass(history, nextGenClass);
                    if (workingHistory.isEmpty()) {
                        return;
                    }

                    final Set<MethodData> unmappedMethodsInClass = unmappedMethodsByOwner.get(nextGenClass).stream().collect(SetsUtil.methods());
                    for (final HistoricalClassMapping classMapping : workingHistory) {
                        final MappingResult<MethodData> mappingResult = runtimeConfiguration.methodMapper().map(unmappedMethodsInClass, classMapping.unmappedMethods());
                        additionallyMappedMethods.putAll(mappingResult.mappings());
                        unmappedMethodsInClass.removeAll(mappingResult.mappings().keySet());
                    }
                }
        );

        return additionallyMappedMethods;
    }

    private BiMap<FieldData, FieldData> mapFieldsTransitively(
            final Set<FieldData> unmappedFields,
            final Map<FieldData, ClassData> fieldOwners,
            final Map<ClassData, List<HistoricalClassMapping>> history,
            MappingRuntimeConfiguration runtimeConfiguration) {
        final BiMap<FieldData, FieldData> additionallyMappedFields = HashBiMap.create();
        final Multimap<ClassData, FieldData> unmappedFieldsByOwner = Multimaps.index(unmappedFields, fieldOwners::get);
        unmappedFieldsByOwner.keySet().forEach(
                nextGenClass -> {
                    final List<HistoricalClassMapping> workingHistory = getAdditionalHistoryOfClass(history, nextGenClass);
                    if (workingHistory.isEmpty()) {
                        return;
                    }

                    final Set<FieldData> unmappedFieldsInClass = unmappedFieldsByOwner.get(nextGenClass).stream().collect(SetsUtil.fields());
                    for (final HistoricalClassMapping classMapping : workingHistory) {
                        final MappingResult<FieldData> mappingResult = runtimeConfiguration.fieldMapper().map(unmappedFieldsInClass, classMapping.unmappedFields());
                        additionallyMappedFields.putAll(mappingResult.mappings());
                        unmappedFieldsInClass.removeAll(mappingResult.mappings().keySet());
                    }
                }
        );

        return additionallyMappedFields;
    }

    private BiMap<ParameterData, ParameterData> mapParametersTransitively(
            final Set<ParameterData> unmappedParameters,
            final Map<ParameterData, MethodData> parameterOwners,
            final Map<MethodData, List<HistoricalMethodMapping>> history,
            MappingRuntimeConfiguration runtimeConfiguration) {
        final BiMap<ParameterData, ParameterData> additionallyMappedParameters = HashBiMap.create();
        final Multimap<MethodData, ParameterData> unmappedParametersByOwner = Multimaps.index(unmappedParameters, parameterOwners::get);
        unmappedParametersByOwner.keySet().forEach(
                nextGenMethod -> {
                    final List<HistoricalMethodMapping> workingHistory = getAdditionalHistoryOfMethod(history, nextGenMethod);
                    if (workingHistory.isEmpty()) {
                        return;
                    }

                    final Set<ParameterData> unmappedParametersInMethod = unmappedParametersByOwner.get(nextGenMethod).stream().collect(SetsUtil.parameters());
                    for (final HistoricalMethodMapping methodMapping : workingHistory) {
                        final MappingResult<ParameterData> mappingResult = runtimeConfiguration.parameterMapper().map(unmappedParametersInMethod, methodMapping.unmappedParameters());
                        additionallyMappedParameters.putAll(mappingResult.mappings());
                        unmappedParametersInMethod.removeAll(mappingResult.mappings().keySet());
                    }
                }
        );

        return additionallyMappedParameters;
    }

    private List<HistoricalClassMapping> getAdditionalHistoryOfClass(final Map<ClassData, List<HistoricalClassMapping>> history, final ClassData classData) {
        final List<HistoricalClassMapping> historyForNextGenClass = history.getOrDefault(classData, new LinkedList<>());
        if (historyForNextGenClass.isEmpty()) {
            return Collections.emptyList();
        }

        final LinkedList<HistoricalClassMapping> workingHistory = new LinkedList<>(historyForNextGenClass);
        workingHistory.removeFirst(); //THis is the initial mapping, so we don't need to map it.
        return workingHistory;
    }

    private List<HistoricalMethodMapping> getAdditionalHistoryOfMethod(final Map<MethodData, List<HistoricalMethodMapping>> history, final MethodData methodData) {
        final List<HistoricalMethodMapping> historyForNextGenMethod = history.getOrDefault(methodData, new LinkedList<>());
        if (historyForNextGenMethod.isEmpty()) {
            return Collections.emptyList();
        }

        final LinkedList<HistoricalMethodMapping> workingHistory = new LinkedList<>(historyForNextGenMethod);
        workingHistory.removeFirst(); //THis is the initial mapping, so we don't need to map it.
        return workingHistory;
    }

    private BiMap<ClassData, Integer> determineClassIds(
            final BiMap<ClassData, ClassData> mappedClasses,
            final Set<ClassData> unmappedClasses,
            final Map<ClassData, String> configurationNameByClassDatas,
            final Map<String, InputConfiguration> configurationsByName,
            final OutputConfiguration outputConfiguration
    ) {
        final BiMap<ClassData, Integer> classIds = HashBiMap.create();

        mappedClasses.forEach((nextGenClass, currentGenClass) -> {
            final String originalConfigurationName = configurationNameByClassDatas.get(currentGenClass);
            final InputConfiguration originalConfiguration = configurationsByName.get(originalConfigurationName);

            final int classId = originalConfiguration.identifier()
                    .map(identifier -> identifier.getClassIdentity(currentGenClass))
                    .orElseGet(() -> outputConfiguration.identifier().getClassIdentity(nextGenClass));

            classIds.put(nextGenClass, classId);
        });

        unmappedClasses.forEach(nextGenClass -> classIds.put(nextGenClass, outputConfiguration.identifier().getClassIdentity(nextGenClass)));

        return classIds;
    }

    private BiMap<FieldData, Integer> determineFieldIds(
            final BiMap<FieldData, FieldData> mappedFields,
            final Set<FieldData> unmappedFields,
            final Map<FieldData, String> configurationNameByFieldDatas,
            final Map<String, InputConfiguration> configurationsByName,
            final OutputConfiguration outputConfiguration) {
        final BiMap<FieldData, Integer> fieldIds = HashBiMap.create();

        mappedFields.forEach((nextGenField, currentGenField) -> {
            final String originalConfigurationName = configurationNameByFieldDatas.get(currentGenField);
            final InputConfiguration originalConfiguration = configurationsByName.get(originalConfigurationName);

            final int fieldId = originalConfiguration.identifier()
                    .map(identifier -> identifier.getFieldIdentity(currentGenField))
                    .orElseGet(() -> outputConfiguration.identifier().getFieldIdentity(nextGenField));

            fieldIds.put(nextGenField, fieldId);
        });

        unmappedFields.forEach(nextGenField -> {
            final int fieldId = outputConfiguration.identifier().getFieldIdentity(nextGenField);

            fieldIds.put(nextGenField, fieldId);
        });

        return fieldIds;
    }

    private BiMap<MethodData, Integer> determineMethodIds(
            final BiMap<MethodData, MethodData> mappedMethods,
            final Set<MethodData> unmappedMethods,
            final Map<MethodData, String> configurationNameByMethodDatas,
            final Map<String, InputConfiguration> configurationsByName,
            final OutputConfiguration outputConfiguration
    ) {
        final BiMap<MethodData, Integer> methodIds = HashBiMap.create();

        mappedMethods.forEach((nextGenMethod, currentGenMethod) -> {
            final String originalConfigurationName = configurationNameByMethodDatas.get(currentGenMethod);
            final InputConfiguration originalConfiguration = configurationsByName.get(originalConfigurationName);

            final int methodId = originalConfiguration.identifier()
                    .map(identifier -> identifier.getMethodIdentity(currentGenMethod))
                    .orElseGet(() -> outputConfiguration.identifier().getMethodIdentity(nextGenMethod));

            methodIds.put(nextGenMethod, methodId);
        });

        unmappedMethods.forEach(nextGenMethod -> {
            final int methodId = outputConfiguration.identifier().getMethodIdentity(nextGenMethod);

            methodIds.put(nextGenMethod, methodId);
        });

        return methodIds;
    }

    private BiMap<ParameterData, Integer> determineParameterIds(
            final BiMap<ParameterData, ParameterData> mappedParameters,
            final Set<ParameterData> unmappedParameters,
            final Map<ParameterData, String> configurationNameByParameterDatas,
            final Map<String, InputConfiguration> configurationsByName,
            final OutputConfiguration outputConfiguration
    ) {
        final BiMap<ParameterData, Integer> parameterIds = HashBiMap.create();

        mappedParameters.forEach((nextGenParameter, currentGenParameter) -> {
            final String originalConfigurationName = configurationNameByParameterDatas.get(currentGenParameter);
            final InputConfiguration originalConfiguration = configurationsByName.get(originalConfigurationName);

            final int parameterId = originalConfiguration.identifier()
                    .map(identifier -> identifier.getParameterIdentity(currentGenParameter))
                    .orElseGet(() -> outputConfiguration.identifier().getParameterIdentity(nextGenParameter));

            parameterIds.put(nextGenParameter, parameterId);
        });

        unmappedParameters.forEach(nextGenParameter -> {
            final int parameterId = outputConfiguration.identifier().getParameterIdentity(nextGenParameter);

            parameterIds.put(nextGenParameter, parameterId);
        });

        return parameterIds;
    }

    private void writeOutput(
            final BiMap<IASMData, String> nameByLoadedASMData,
            final BiMap<String, Optional<IExistingNameSupplier>> remapperByName,
            final BiMap<ClassData, ClassData> classMappings,
            final BiMap<FieldData, FieldData> fieldMappings,
            final BiMap<MethodData, MethodData> methodMappings,
            final BiMap<ParameterData, ParameterData> parameterMappings,
            final BiMap<ClassData, Integer> classIds,
            final BiMap<MethodData, Integer> methodIds,
            final BiMap<FieldData, Integer> fieldIds,
            final BiMap<ParameterData, Integer> parameterIds,
            final OutputConfiguration outputConfiguration,
            final IASMData asmData) {
        LOGGER.info("Creating named AST");

        final INamedAST ast = outputConfiguration.astBuilderFactory().create(
                nameByLoadedASMData,
                remapperByName
        ).build(
                classMappings,
                fieldMappings,
                methodMappings,
                parameterMappings,
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

    record TransitionMappingResultKey(String currentGenName, String nextGenName) {
    }

    record HistoricalClassMapping(ClassData classData, Set<MethodData> unmappedMethods, Set<FieldData> unmappedFields) {
    }

    record HistoricalMethodMapping(MethodData methodData, Set<ParameterData> unmappedParameters) {
    }
}
