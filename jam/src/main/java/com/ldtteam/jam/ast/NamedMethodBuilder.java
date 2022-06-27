package com.ldtteam.jam.ast;

import com.google.common.collect.BiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ldtteam.jam.spi.asm.ClassData;
import com.ldtteam.jam.spi.asm.MethodData;
import com.ldtteam.jam.spi.asm.ParameterData;
import com.ldtteam.jam.spi.ast.metadata.IMetadataClass;
import com.ldtteam.jam.spi.ast.metadata.IMetadataMethod;
import com.ldtteam.jam.spi.ast.metadata.IMetadataMethodReference;
import com.ldtteam.jam.spi.ast.metadata.IMetadataRecordComponent;
import com.ldtteam.jam.spi.ast.named.INamedMethod;
import com.ldtteam.jam.spi.ast.named.INamedParameter;
import com.ldtteam.jam.spi.ast.named.builder.INamedMethodBuilder;
import com.ldtteam.jam.spi.ast.named.builder.INamedParameterBuilder;
import com.ldtteam.jam.spi.name.INameProvider;
import com.ldtteam.jam.spi.name.IRemapper;
import com.ldtteam.jam.util.MethodDataUtils;

import java.util.*;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class NamedMethodBuilder implements INamedMethodBuilder {
    public record MethodNamingInformation(MethodData target, MethodData mappedFrom, Integer id) {}

    public static INamedMethodBuilder create(
            IRemapper runtimeToASTRemapper, IRemapper metadataToRuntimeRemapper, INameProvider<MethodNamingInformation> methodNameProvider, INamedParameterBuilder parameterBuilder
    ) {
        return new NamedMethodBuilder(runtimeToASTRemapper, metadataToRuntimeRemapper, methodNameProvider, parameterBuilder);
    }

    private final IRemapper runtimeToASTRemapper;
    private final IRemapper metadataToRuntimeRemapper;
    private final INameProvider<MethodNamingInformation> methodNameProvider;
    private final INamedParameterBuilder parameterBuilder;

    private NamedMethodBuilder(
            IRemapper runtimeToASTRemapper,
            IRemapper metadataToRuntimeRemapper,
            INameProvider<MethodNamingInformation> methodNameProvider,
            INamedParameterBuilder parameterBuilder) {
        this.runtimeToASTRemapper = runtimeToASTRemapper;
        this.metadataToRuntimeRemapper = metadataToRuntimeRemapper;
        this.methodNameProvider = methodNameProvider;
        this.parameterBuilder = parameterBuilder;
    }

    @Override
    public INamedMethod build(
            final ClassData classData,
            final MethodData methodData,
            final IMetadataClass classMetadata,
            final Map<String, ClassData> classDatasByAstName,
            final Map<MethodData, MethodData> rootMethodsByOverride,
            final Map<String, String> identifiedFieldNamesByOriginalName,
            final BiMap<MethodData, MethodData> methodMappings,
            final BiMap<ParameterData, ParameterData> parameterMappings,
            final BiMap<MethodData, Integer> methodIds,
            final BiMap<ParameterData, Integer> parameterIds
    ) {
        final String obfuscatedDescriptor = runtimeToASTRemapper.remapDescriptor(methodData.node().desc)
                .orElseThrow(() -> new IllegalStateException("Failed to remap descriptor of method: %s in class: %s".formatted(methodData.node().name,
                        classData.node().name)));
        final String obfuscatedMethodName = runtimeToASTRemapper.remapMethod(classData.node().name, methodData.node().name, methodData.node().desc)
                .orElseThrow(() -> new IllegalStateException("Failed to remap method: %s in class: %s".formatted(methodData.node().name, classData.node().name)));

        if (classMetadata.getMethodsByName() == null) {
            throw new IllegalStateException("The class: %s does not contain any methods!".formatted(classData.node().name));
        }

        final IMetadataMethod methodMetadata = classMetadata.getMethodsByName().computeIfAbsent(obfuscatedMethodName + obfuscatedDescriptor, (key) -> {
            throw new IllegalStateException("Failed to find the metadata of the method: %s in class: %s".formatted(methodData.node().name, classData.node().name));
        });

        MethodData rootData = methodData;
        if (rootMethodsByOverride.containsKey(methodData)) {
            rootData = rootMethodsByOverride.get(methodData);
        }

        String identifiedName = null;

        if (methodMetadata.getForce() != null) {
            identifiedName = methodMetadata.getForce();
        }

        if (identifiedName == null && isInit(methodData)) {
            identifiedName = methodData.node().name;
        }

        if (isMain(methodData)) {
            identifiedName = "main";
        }

        if (identifiedName == null && methodMetadata.getOverrides() != null) {
            final Set<Integer> originalIds = Sets.newHashSet();

            for (IMetadataMethodReference override : methodMetadata.getOverrides()) {
                // if the class isn't in our codebase, then trust the identifiedName
                if (!classDatasByAstName.containsKey(override.getOwner())) {
                    identifiedName = override.getName();
                    break;
                } else { // If it is, then it should have been assigned an ID earlier so use it's id.
                    final ClassData overrideClassData = classDatasByAstName.get(override.getOwner());
                    final MethodData overriddenMethod = overrideClassData.node().methods.stream()
                            .filter(candidate -> isValidOverriddenMethod(classDatasByAstName, override, new MethodData(overrideClassData, candidate)))
                            .findFirst()
                            .map(methodNode -> new MethodData(overrideClassData, methodNode))
                            .orElse(null);

                    if (overriddenMethod != null) {
                        final Integer overriddenMethodId =
                                rootMethodsByOverride.containsKey(overriddenMethod) ? methodIds.get(rootMethodsByOverride.get(overriddenMethod)) : methodIds.get(overriddenMethod);
                        originalIds.add(overriddenMethodId);
                    }
                }
            }

            if (identifiedName == null && originalIds.size() > 0) {
                final MethodNamingInformation methodNamingInformation = new MethodNamingInformation(
                        methodData,
                        methodMappings.get(methodData),
                        originalIds.stream().mapToInt(Integer::intValue).min().orElse(-1)
                );

                identifiedName = methodNameProvider.getName(methodNamingInformation);
            }
        }

        if (identifiedName == null && classMetadata.getRecords() != null && methodData.node().desc.startsWith("()")) {
            for (final IMetadataRecordComponent recordInfo : classMetadata.getRecords()) {
                final Optional<String> officialRecordDescriptor = metadataToRuntimeRemapper.remapDescriptor(recordInfo.getDesc());
                final Optional<String> obfuscatedClassname = runtimeToASTRemapper.remapClass(classData.node().name);
                if (obfuscatedClassname.isPresent() &&
                        isRecordComponentForMethod(methodData, obfuscatedMethodName, obfuscatedClassname.get(), recordInfo, officialRecordDescriptor)) {
                    if (identifiedFieldNamesByOriginalName.containsKey(recordInfo.getField())) {
                        identifiedName = identifiedFieldNamesByOriginalName.get(recordInfo.getField());
                    }
                }
            }
        }

        if (identifiedName == null) {
            final MethodNamingInformation methodNamingInformation = new MethodNamingInformation(
                    methodData,
                    methodMappings.get(methodData),
                    methodIds.get(rootData)
            );

            identifiedName = methodNameProvider.getName(methodNamingInformation);
        }

        final Collection<INamedParameter> namedParameters = Lists.newArrayList();
        final List<ParameterData> asmParameters = MethodDataUtils.parametersAsList(methodData);
        if (asmParameters != null) {
            for (int i = 0; i < asmParameters.size(); i++) {
                final ParameterData parameterData = asmParameters.get(i);

                final INamedParameter parameter = parameterBuilder.build(
                        classData,
                        methodData,
                        parameterData,
                        i,
                        classMetadata,
                        identifiedFieldNamesByOriginalName,
                        parameterMappings,
                        parameterIds
                );

                namedParameters.add(parameter);
            }
        }

        return new NamedMethod(
                obfuscatedMethodName,
                identifiedName,
                methodIds.get(methodData),
                obfuscatedDescriptor,
                methodMetadata.isStatic() || obfuscatedMethodName.equals("<clinit>"),
                methodData.node().name.contains("lambda$"),
                namedParameters
        );
    }

    private boolean isRecordComponentForMethod(
            MethodData methodData,
            String obfuscatedMethodName,
            String obfuscatedClassName,
            IMetadataRecordComponent recordInfo,
            Optional<String> officialRecordDescriptor) {
        return officialRecordDescriptor.isPresent() && recordInfo.getMethods() != null
                && methodData.node().desc.endsWith(officialRecordDescriptor.get())
                && recordInfo.getMethods().contains(obfuscatedMethodName)
                && metadataToRuntimeRemapper.remapField(obfuscatedClassName, recordInfo.getField(), recordInfo.getDesc())
                .map(name -> name.equals(methodData.node().name)).orElse(false);
    }

    private boolean isValidOverriddenMethod(Map<String, ClassData> classDatasByAstName, IMetadataMethodReference override, MethodData candidate) {
        return runtimeToASTRemapper
                .remapMethod(classDatasByAstName.get(override.getOwner()).node().name, candidate.node().name, candidate.node().desc)
                .filter(remappedOverrideCandidateName -> isValidOverriddenCandidateName(override, candidate, remappedOverrideCandidateName))
                .isPresent();
    }

    private boolean isValidOverriddenCandidateName(IMetadataMethodReference override, MethodData candidate, String remappedOverrideCandidateName) {
        return remappedOverrideCandidateName.equals(override.getName())
                && runtimeToASTRemapper.remapDescriptor(candidate.node().desc)
                .filter(remappedOverrideCandidateDescriptor -> remappedOverrideCandidateDescriptor.equals(override.getDesc()))
                .isPresent();
    }

    private boolean isInit(MethodData methodData) {
        return methodData.node().name.equals("<init>") || methodData.node().name.equals("<clinit>");
    }

    private boolean isMain(MethodData methodData) {
        return methodData.node().name.equals("main") &&
                methodData.node().desc.equals("([Ljava/lang/String;)V") &&
                (methodData.node().access & (ACC_STATIC | ACC_PUBLIC)) == (ACC_STATIC | ACC_PUBLIC);
    }

    private record NamedMethod(String originalName, String identifiedName, int id, String originalDescriptor,
                               boolean isStatic, boolean isLambda,
                               Collection<INamedParameter> parameters) implements INamedMethod {
    }
}
