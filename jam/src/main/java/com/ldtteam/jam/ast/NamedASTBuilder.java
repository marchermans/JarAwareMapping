package com.ldtteam.jam.ast;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.ldtteam.jam.spi.asm.ClassData;
import com.ldtteam.jam.spi.asm.FieldData;
import com.ldtteam.jam.spi.asm.IASMData;
import com.ldtteam.jam.spi.asm.MethodData;
import com.ldtteam.jam.spi.asm.ParameterData;
import com.ldtteam.jam.spi.ast.metadata.IMetadataAST;
import com.ldtteam.jam.spi.ast.metadata.IMetadataClass;
import com.ldtteam.jam.spi.ast.metadata.IMetadataMethod;
import com.ldtteam.jam.spi.ast.metadata.IMetadataMethodReference;
import com.ldtteam.jam.spi.ast.named.INamedAST;
import com.ldtteam.jam.spi.ast.named.INamedClass;
import com.ldtteam.jam.spi.ast.named.builder.INamedASTBuilder;
import com.ldtteam.jam.spi.ast.named.builder.INamedClassBuilder;
import com.ldtteam.jam.spi.name.INameProvider;
import com.ldtteam.jam.spi.name.IRemapper;
import com.ldtteam.jam.spi.payload.IPayloadSupplier;
import com.ldtteam.jam.util.MethodDataUtils;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TooManyListenersException;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NamedASTBuilder<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> implements INamedASTBuilder<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NamedASTBuilder.class);
    private final IRemapper runtimeToASTRemapper;
    private final IRemapper ASTtoRuntimeRemapper;
    private final INamedClassBuilder<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> classBuilder;
    private NamedASTBuilder(IRemapper runtimeToASTRemapper, IRemapper asTtoRuntimeRemapper, INamedClassBuilder<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> classBuilder) {
        this.runtimeToASTRemapper = runtimeToASTRemapper;
        ASTtoRuntimeRemapper = asTtoRuntimeRemapper;
        this.classBuilder = classBuilder;
    }

    public static <TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> INamedASTBuilder<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> create(
            IRemapper runtimeToASTRemapper, IRemapper asTtoRuntimeRemapper, INamedClassBuilder<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> classBuilder
    ) {
        return new NamedASTBuilder<>(runtimeToASTRemapper, asTtoRuntimeRemapper, classBuilder);
    }

    @Override
    public INamedAST build(
            BiMap<ClassData<TClassPayload>, ClassData<TClassPayload>> classMappings,
            BiMap<FieldData<TClassPayload, TFieldPayload>, FieldData<TClassPayload, TFieldPayload>> fieldMappings,
            BiMap<MethodData<TClassPayload, TMethodPayload>, MethodData<TClassPayload, TMethodPayload>> methodMappings,
            BiMap<ParameterData<TClassPayload, TMethodPayload, TParameterPayload>, ParameterData<TClassPayload, TMethodPayload, TParameterPayload>> parameterMappings,
            BiMap<ClassData<TClassPayload>, Integer> classIds,
            BiMap<MethodData<TClassPayload, TMethodPayload>, Integer> methodIds,
            BiMap<FieldData<TClassPayload, TFieldPayload>, Integer> fieldIds,
            BiMap<ParameterData<TClassPayload, TMethodPayload, TParameterPayload>, Integer> parameterIds,
            IASMData<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> asmData,
            IMetadataAST metadataAST
    ) {
        record ClassDatasByMethodDataEntry<TClassPayload, TMethodPayload>(ClassData<TClassPayload> classData, MethodData<TClassPayload, TMethodPayload> methodData) {
        }
        final IPayloadSupplier<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> payloadSupplier = asmData.payloadSupplier().orElse(IPayloadSupplier.empty());
        final Map<MethodData<TClassPayload, TMethodPayload>, ClassData<TClassPayload>> classDatasByMethodData = asmData.classes().stream()
                .flatMap(classData -> classData.node().methods.stream()
                        .map(node -> new ClassDatasByMethodDataEntry<>(classData, new MethodData<>(classData, node, payloadSupplier.forMethod(classData.node(), node)))))
                .collect(Collectors.toMap(ClassDatasByMethodDataEntry::methodData, ClassDatasByMethodDataEntry::classData));

        final Map<ClassData<TClassPayload>, LinkedList<ClassData<TClassPayload>>> inheritanceData = buildInheritanceData(asmData.classes());

        final Multimap<ClassData<TClassPayload>, ClassData<TClassPayload>> inheritanceVolumes = buildInheritanceVolumes(inheritanceData);

        final Map<MethodData<TClassPayload, TMethodPayload>, MethodData<TClassPayload, TMethodPayload>> rootMethodsByOverride = buildForcedMethods(
                asmData.methods(),
                inheritanceData,
                classDatasByMethodData,
                methodIds,
                metadataAST,
                payloadSupplier
        );
        final Multimap<MethodData<TClassPayload, TMethodPayload>, MethodData<TClassPayload, TMethodPayload>> overrideTree = MethodDataUtils.buildOverrideTree(rootMethodsByOverride);

        final Map<String, ClassData<TClassPayload>> classDatasByASTName = asmData.classes().stream()
                .collect(Collectors.toMap(
                        classData -> runtimeToASTRemapper.remapClass(classData.node().name)
                                .orElseThrow(() -> new IllegalStateException("Failed to remap class: %s".formatted(classData.node().name))),
                        Function.identity()));

        final BiMap<String, INamedClass> classes = HashBiMap.create();
        classIds.keySet().forEach(classData -> {
            final INamedClass namedClass = classBuilder.build(
                    classData,
                    metadataAST,
                    classDatasByASTName,
                    inheritanceVolumes,
                    rootMethodsByOverride,
                    overrideTree,
                    classMappings,
                    fieldMappings,
                    methodMappings,
                    parameterMappings,
                    classIds,
                    fieldIds,
                    methodIds,
                    parameterIds,
                    classes);

            classes.put(namedClass.originalName(), namedClass);
        });

        return new NamedAST(classes.values());
    }

    private Multimap<ClassData<TClassPayload>, ClassData<TClassPayload>> buildInheritanceVolumes(Map<ClassData<TClassPayload>, LinkedList<ClassData<TClassPayload>>> inheritanceData) {
        Map<ClassData<TClassPayload>, Set<ClassData<TClassPayload>>> inheritanceVolumes = new HashMap<>();
        for (ClassData<TClassPayload> classData : inheritanceData.keySet()) {
            final Set<ClassData<TClassPayload>> volume = inheritanceVolumes.computeIfAbsent(classData, d -> Sets.newHashSet());
            volume.add(classData);
            inheritanceData.get(classData).forEach(inheritanceClassData -> volume.add(classData));
            inheritanceVolumes.put(classData, volume);
        }

        final Multimap<ClassData<TClassPayload>, ClassData<TClassPayload>> inheritanceVolumesByClass = HashMultimap.create();
        inheritanceVolumes.forEach((classData, volume) -> volume.forEach(c -> {
            inheritanceVolumesByClass.put(classData, c);
            inheritanceVolumesByClass.put(c, classData);
        }));
        return inheritanceVolumesByClass;
    }

    private Map<ClassData<TClassPayload>, LinkedList<ClassData<TClassPayload>>> buildInheritanceData(final Collection<ClassData<TClassPayload>> classes) {
        final Map<String, ClassData<TClassPayload>> classDatasByName = classes.stream()
                .collect(Collectors.toMap(INameProvider.classes(), Function.identity()));

        return classes.stream()
                .collect(Collectors.toMap(Function.identity(), classData -> getInheritanceOf(classDatasByName, classData.node().name, Sets.newHashSet())));
    }

    private LinkedList<ClassData<TClassPayload>> getInheritanceOf(final Map<String, ClassData<TClassPayload>> classDatasByName, final String className, final Set<ClassData<TClassPayload>> superTypes) {
        final ClassData<TClassPayload> classData = classDatasByName.get(className);
        if (classData == null) {
            return new LinkedList<>();
        }

        final LinkedList<ClassData<TClassPayload>> inheritance = new LinkedList<>();
        superTypes.add(classData);
        inheritance.add(classData);

        final String superClassName = classData.node().superName;
        if (superClassName != null) {
            final LinkedList<ClassData<TClassPayload>> superInheritance = getInheritanceOf(classDatasByName, superClassName, superTypes);
            inheritance.addAll(superInheritance);
        }

        final List<String> interfaces = classData.node().interfaces;
        if (interfaces != null) {
            for (String interfaceName : interfaces) {
                final LinkedList<ClassData<TClassPayload>> superInheritance = getInheritanceOf(classDatasByName, interfaceName, superTypes);
                inheritance.addAll(superInheritance);
            }
        }

        return inheritance;
    }

    private Map<MethodData<TClassPayload, TMethodPayload>, MethodData<TClassPayload, TMethodPayload>> buildForcedMethods(
            final Collection<MethodData<TClassPayload, TMethodPayload>> methods,
            final Map<ClassData<TClassPayload>, LinkedList<ClassData<TClassPayload>>> classInheritanceData,
            final Map<MethodData<TClassPayload, TMethodPayload>, ClassData<TClassPayload>> classDatasByMethodData,
            final BiMap<MethodData<TClassPayload, TMethodPayload>, Integer> methodIds,
            final IMetadataAST metadataAST,
            final IPayloadSupplier<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> payloadSupplier) {
        final Map<MethodReference, MethodData<TClassPayload, TMethodPayload>> methodsByReference = collectMethodReferences(methods, classDatasByMethodData);
        final Multimap<MethodData<TClassPayload, TMethodPayload>, MethodData<TClassPayload, TMethodPayload>> overrides = collectMethodOverrides(methods, classInheritanceData, classDatasByMethodData, metadataAST, methodsByReference, payloadSupplier);
        final Set<Set<MethodData<TClassPayload, TMethodPayload>>> combinedTrees = buildOverrideTrees(overrides);
        return determineIdsPerOverrideTree(methodIds, combinedTrees);
    }

    public Map<MethodReference, MethodData<TClassPayload, TMethodPayload>> collectMethodReferences(
            final Collection<MethodData<TClassPayload, TMethodPayload>> methods,
            final Map<MethodData<TClassPayload, TMethodPayload>, ClassData<TClassPayload>> classDatasByMethodData
    ) {
        final Map<MethodReference, MethodData<TClassPayload, TMethodPayload>> methodsByReference = new HashMap<>();
        methods.forEach(methodData -> {
            if (methodData.node().name.startsWith("<")) {
                return;
            }

            final ClassData<TClassPayload> classData = classDatasByMethodData.get(methodData);
            methodsByReference.put(
                    new MethodReference(classData.node().name, methodData.node().name, methodData.node().desc),
                    methodData
            );
        });
        return methodsByReference;
    }

    public Multimap<MethodData<TClassPayload, TMethodPayload>, MethodData<TClassPayload, TMethodPayload>> collectMethodOverrides(
            final Collection<MethodData<TClassPayload, TMethodPayload>> methods,
            final Map<ClassData<TClassPayload>, LinkedList<ClassData<TClassPayload>>> classInheritanceData,
            final Map<MethodData<TClassPayload, TMethodPayload>, ClassData<TClassPayload>> classDatasByMethodData,
            final IMetadataAST metadataAST,
            final Map<MethodReference, MethodData<TClassPayload, TMethodPayload>> methodsByReference,
            final IPayloadSupplier<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> payloadSupplier
    ) {

        final Multimap<MethodData<TClassPayload, TMethodPayload>, MethodData<TClassPayload, TMethodPayload>> overrides = HashMultimap.create();
        methods.forEach(
                methodData -> {
                    if (methodData.node().name.startsWith("<") || (methodData.node().access & (Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE)) != 0) {
                        return;
                    }

                    final ClassData<TClassPayload> classData = classDatasByMethodData.get(methodData);
                    collectMethodOverridesFromASMData(classInheritanceData, overrides, methodData, classData, payloadSupplier);

                    final IMetadataMethod methodInfo = getMetadataForMethod(metadataAST, methodData, classData);
                    if (methodInfo == null) {
                        return;
                    }

                    collectMethodOverridesFromMetadata(methodsByReference, overrides, methodData, methodInfo);

                    collectMethodBouncersFromMetadata(methodsByReference, overrides, methodData, methodInfo);
                }
        );

        return overrides;
    }

    private void collectMethodOverridesFromASMData(
            final Map<ClassData<TClassPayload>, LinkedList<ClassData<TClassPayload>>> classInheritanceData,
            final Multimap<MethodData<TClassPayload, TMethodPayload>, MethodData<TClassPayload, TMethodPayload>> overrides,
            final MethodData<TClassPayload, TMethodPayload> methodData,
            final ClassData<TClassPayload> classData,
            final IPayloadSupplier<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> payloadSupplier) {
        final LinkedList<ClassData<TClassPayload>> superTypes = classInheritanceData.getOrDefault(classData, new LinkedList<>());
        if (!superTypes.isEmpty()) {
            superTypes.forEach(superType -> superType.node().methods.stream()
                    .filter(superMethodData -> !superMethodData.name.equals("<"))
                    .filter(superMethodData -> superMethodData.name.equals(methodData.node().name) && superMethodData.desc.equals(methodData.node().desc))
                    .forEach(superMethodData -> overrides.put(methodData, new MethodData<>(superType, superMethodData, payloadSupplier.forMethod(superType.node(), superMethodData)))));
        }
    }

    private IMetadataMethod getMetadataForMethod(final IMetadataAST metadataAST, final MethodData<TClassPayload, TMethodPayload> methodData, final ClassData<TClassPayload> classData) {
        final String obfuscatedOwnerClassName = runtimeToASTRemapper.remapClass(classData.node().name)
                .orElseThrow(() -> new IllegalStateException("Failed to remap class: %s".formatted(classData.node().name)));
        if (Objects.equals(obfuscatedOwnerClassName, classData.node().name)) {
            //Not an obfuscated class. Ignore
            return null;
        }
        if (!metadataAST.getClassesByName().containsKey(obfuscatedOwnerClassName)) {
            //Not a class we have metadata for, would be weird, so log and ignore.
            LOGGER.warn("Could not find metadata for class: " + classData.node().name + " its obfuscated class name: " + obfuscatedOwnerClassName
                    + " does not seems to be found in the json metadata.");
            return null;
        }
        final IMetadataClass classInfo = metadataAST.getClassesByName().get(obfuscatedOwnerClassName);
        if (classInfo == null) {
            //Not a class we have metadata for, would be weird, so log and ignore.
            LOGGER.warn("Could not find metadata for class: " + classData.node().name + " its obfuscated class name: " + obfuscatedOwnerClassName
                    + " does not seems to be found in the json metadata.");
            return null;
        }

        final String obfuscatedMethodName = runtimeToASTRemapper.remapMethod(classData.node().name, methodData.node().name, methodData.node().desc)
                .orElseThrow(() -> new IllegalStateException("Failed to remap method: %s in class: %s".formatted(methodData.node().name, classData.node().name)));
        if (Objects.equals(obfuscatedMethodName, methodData.node().name)) {
            //Not obfuscated method without metadata. Ignore
            return null;
        }
        final String obfuscatedDescriptor = runtimeToASTRemapper.remapDescriptor(methodData.node().desc)
                .orElseThrow(() -> new IllegalStateException("Failed to remap descriptor: %s".formatted(methodData.node().desc)));
        if (classInfo.getMethodsByName() == null) {
            throw new IllegalStateException("The given class does contain methods");
        }
        final IMetadataMethod methodInfo = classInfo.getMethodsByName().get(obfuscatedMethodName + obfuscatedDescriptor);
        if (methodInfo == null) {
            //Not a class we have metadata for, would be weird, so log and ignore.
            LOGGER.warn("Could not find metadata for method: %s(%s) in class: %s does not seems to be found in the json metadata.".formatted(methodData.node().name,
                    methodData.node().desc,
                    classData.node().name));
            return null;
        }
        return methodInfo;
    }

    private void collectMethodOverridesFromMetadata(
            final Map<MethodReference, MethodData<TClassPayload, TMethodPayload>> methodsByReference,
            final Multimap<MethodData<TClassPayload, TMethodPayload>, MethodData<TClassPayload, TMethodPayload>> overrides,
            final MethodData<TClassPayload, TMethodPayload> methodData,
            final IMetadataMethod methodInfo) {
        if (methodInfo.getOverrides() == null) {
            //No overrides available.
            return;
        }

        methodInfo.getOverrides().stream()
                .map(method -> getMethodData(methodsByReference, method))
                .filter(Objects::nonNull)
                .forEach(superMethodData -> overrides.put(methodData, superMethodData));
    }

    private void collectMethodBouncersFromMetadata(
            final Map<MethodReference, MethodData<TClassPayload, TMethodPayload>> methodsByReference,
            final Multimap<MethodData<TClassPayload, TMethodPayload>, MethodData<TClassPayload, TMethodPayload>> overrides,
            final MethodData<TClassPayload, TMethodPayload> methodData,
            final IMetadataMethod methodInfo) {
        if (methodInfo.getBouncer() == null) {
            //Is not a bouncer method
            return;
        }

        MethodData<TClassPayload, TMethodPayload> target = getMethodData(methodsByReference, methodInfo.getBouncer().getTarget());
        if (target == null) {
            return;
        }

        overrides.put(methodData, target);
        overrides.putAll(methodData, overrides.get(target));
    }

    private MethodData<TClassPayload, TMethodPayload> getMethodData(
            final Map<MethodReference, MethodData<TClassPayload, TMethodPayload>> methodsByReference,
            final IMetadataMethodReference method) {
        final String officialClassName = ASTtoRuntimeRemapper.remapClass(method.getOwner())
                .orElseThrow(() -> new IllegalStateException("Failed to remap class: %s".formatted(method.getOwner())));
        final String officialDescriptor = ASTtoRuntimeRemapper.remapDescriptor(method.getDesc())
                .orElseThrow(() -> new IllegalStateException("Failed to remap descriptor: %s".formatted(method.getDesc())));
        if (officialClassName == null || officialDescriptor == null
                || officialClassName.equals(method.getOwner()) || officialDescriptor.equals(method.getDesc())) {
            //Not re-mappable or not obfuscated. Ignore.
            return null;
        }

        final String officialMethodName = ASTtoRuntimeRemapper.remapMethod(method.getOwner(), method.getName(), method.getDesc())
                .orElseThrow(() -> new IllegalStateException("Failed to remap method: %s in class: %s".formatted(method.getName(),
                        method.getOwner())));
        final MethodReference reference = new MethodReference(officialClassName, officialMethodName, officialDescriptor);
        if (!methodsByReference.containsKey(reference)) {
            //Not a class we have metadata for, would be weird, so log and ignore.
            LOGGER.warn("Could not find method data for method: %s(%s) in class: %s".formatted(officialMethodName, officialDescriptor, officialDescriptor));
            return null;
        }

        return methodsByReference.get(reference);
    }

    public Set<Set<MethodData<TClassPayload, TMethodPayload>>> buildOverrideTrees(
            final Multimap<MethodData<TClassPayload, TMethodPayload>, MethodData<TClassPayload, TMethodPayload>> overrides
    ) {
        final Set<MethodData<TClassPayload, TMethodPayload>> processedData = Sets.newHashSet();
        final Set<Set<MethodData<TClassPayload, TMethodPayload>>> combinedTrees = Sets.newHashSet();
        final Map<MethodData<TClassPayload, TMethodPayload>, Collection<MethodData<TClassPayload, TMethodPayload>>> overrideBranchMap = overrides.asMap();

        overrideBranchMap.keySet().forEach(
                overriddenMethod -> {
                    if (processedData.contains(overriddenMethod)) {
                        return;
                    }

                    if (overrideBranchMap.get(overriddenMethod).size() == 1) {
                        if (overrideBranchMap.get(overriddenMethod).contains(overriddenMethod)) {
                            return;
                        }
                    }

                    processedData.add(overriddenMethod);
                    final Set<MethodData<TClassPayload, TMethodPayload>> workingSet = Sets.newHashSet(overrides.get(overriddenMethod));
                    for (final Collection<MethodData<TClassPayload, TMethodPayload>> overrideBranch : overrideBranchMap.values()) {
                        if (overrideBranch.stream().anyMatch(workingSet::contains)) {
                            workingSet.addAll(overrideBranch);
                        }
                    }

                    combinedTrees.add(workingSet);
                    processedData.addAll(workingSet);
                }
        );

        return combinedTrees;
    }

    public Map<MethodData<TClassPayload, TMethodPayload>, MethodData<TClassPayload, TMethodPayload>> determineIdsPerOverrideTree(
            final BiMap<MethodData<TClassPayload, TMethodPayload>, Integer> methodIds,
            final Set<Set<MethodData<TClassPayload, TMethodPayload>>> combinedTrees
    ) {
        final BiMap<Integer, MethodData<TClassPayload, TMethodPayload>> methodDatasById = methodIds.inverse();
        final Map<MethodData<TClassPayload, TMethodPayload>, MethodData<TClassPayload, TMethodPayload>> overridesByMethod = new HashMap<>();
        for (final Set<MethodData<TClassPayload, TMethodPayload>> combinedTree : combinedTrees) {
            final MethodData<TClassPayload, TMethodPayload> rootData = combinedTree.stream()
                    .mapToInt(methodIds::get)
                    .min()
                    .stream()
                    .mapToObj(methodDatasById::get)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No root data found"));

            for (final MethodData<TClassPayload, TMethodPayload> methodData : combinedTree) {
                overridesByMethod.put(methodData, rootData);
            }
        }
        return overridesByMethod;
    }

    private record NamedAST(Collection<INamedClass> classes) implements INamedAST {
    }

    private record MethodReference(String owner, String name, String descriptor) {
    }
}
