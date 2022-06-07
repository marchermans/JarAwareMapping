package com.ldtteam.jam.ast;

import com.google.common.collect.BiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

import java.util.*;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

public class NamedMethodBuilder implements INamedMethodBuilder {

    public static INamedMethodBuilder create(
            IRemapper runtimeToASTRemapper, IRemapper metadataToRuntimeRemapper, INameProvider<Integer> methodNameProvider, INamedParameterBuilder parameterBuilder
    ) {
        return new NamedMethodBuilder(runtimeToASTRemapper, metadataToRuntimeRemapper, methodNameProvider, parameterBuilder);
    }

    private final IRemapper runtimeToASTRemapper;
    private final IRemapper metadataToRuntimeRemapper;
    private final INameProvider<Integer> methodNameProvider;
    private final INamedParameterBuilder parameterBuilder;

    private NamedMethodBuilder(IRemapper runtimeToASTRemapper, IRemapper metadataToRuntimeRemapper, INameProvider<Integer> methodNameProvider, INamedParameterBuilder parameterBuilder) {
        this.runtimeToASTRemapper = runtimeToASTRemapper;
        this.metadataToRuntimeRemapper = metadataToRuntimeRemapper;
        this.methodNameProvider = methodNameProvider;
        this.parameterBuilder = parameterBuilder;
    }

    @Override
    public INamedMethod build(
            final ClassNode classNode,
            final MethodNode methodNode,
            final IMetadataClass classMetadata,
            final Map<String, ClassNode> classNodesByAstName,
            final Map<MethodNode, MethodNode> rootMethodsByOverride,
            final Map<String, String> identifiedFieldNamesByOriginalName,
            final BiMap<MethodNode, Integer> methodIds,
            final BiMap<ParameterNode, Integer> parameterIds
    ) {
        if (classNode.name.contains("SpawnData") && methodNode.name.contains("entity")) {
            System.out.println("");
        }

        final String obfuscatedDescriptor = runtimeToASTRemapper.remapDescriptor(methodNode.desc)
                .orElseThrow(() -> new IllegalStateException("Failed to remap descriptor of method: %s in class: %s".formatted(methodNode.name, classNode.name)));
        final String obfuscatedMethodName = runtimeToASTRemapper.remapMethod(classNode.name, methodNode.name, methodNode.desc)
                .orElseThrow(() -> new IllegalStateException("Failed to remap method: %s in class: %s".formatted(methodNode.name, classNode.name)));

        if (classMetadata.getMethodsByName() == null) {
            throw new IllegalStateException("The class: %s does not contain any methods!".formatted(classNode.name));
        }

        final IMetadataMethod methodMetadata = classMetadata.getMethodsByName().computeIfAbsent(obfuscatedMethodName + obfuscatedDescriptor, (key) -> {
            throw new IllegalStateException("Failed to find the metadata of the method: %s in class: %s".formatted(methodNode.name, classNode.name));
        });

        MethodNode rootNode = methodNode;
        if (rootMethodsByOverride.containsKey(methodNode))
        {
            rootNode = rootMethodsByOverride.get(methodNode);
        }

        String identifiedName = null;

        if (methodMetadata.getForce() != null)
        {
            identifiedName = methodMetadata.getForce();
        }

        if (identifiedName == null && isInit(methodNode))
        {
            identifiedName = methodNode.name;
        }

        if (isMain(methodNode))
        {
            identifiedName = "main";
        }

        if (identifiedName == null && methodMetadata.getOverrides() != null)
        {
            final Set<Integer> originalIds = Sets.newHashSet();

            for (IMetadataMethodReference override : methodMetadata.getOverrides())
            {
                // if the class isn't in our codebase, then trust the identifiedName
                if (!classNodesByAstName.containsKey(override.getOwner()))
                {
                    identifiedName = override.getName();
                    break;
                }
                else
                { // If it is, then it should have been assigned an ID earlier so use it's id.
                    final MethodNode overriddenMethod = classNodesByAstName.get(override.getOwner()).methods.stream()
                            .filter(candidate -> isValidOverriddenMethod(classNodesByAstName, override, candidate))
                            .findFirst().orElse(null);

                    if (overriddenMethod != null)
                    {
                        final Integer overriddenMethodId =
                                rootMethodsByOverride.containsKey(overriddenMethod) ? methodIds.get(rootMethodsByOverride.get(overriddenMethod)) : methodIds.get(overriddenMethod);
                        originalIds.add(overriddenMethodId);
                    }
                }
            }

            if (identifiedName == null && originalIds.size() > 0)
            {
                identifiedName = methodNameProvider.getName(originalIds.stream().mapToInt(Integer::intValue).min().orElse(-1));
            }
        }

        if (identifiedName == null && classMetadata.getRecords() != null && methodNode.desc.startsWith("()"))
        {
            for (final IMetadataRecordComponent recordInfo : classMetadata.getRecords())
            {
                final Optional<String> officialRecordDescriptor = metadataToRuntimeRemapper.remapDescriptor(recordInfo.getDesc());
                final Optional<String> obfuscatedClassname = runtimeToASTRemapper.remapClass(classNode.name);
                if (obfuscatedClassname.isPresent() &&
                      isRecordComponentForMethod(methodNode, obfuscatedMethodName, obfuscatedClassname.get(), recordInfo, officialRecordDescriptor))
                {
                    if (identifiedFieldNamesByOriginalName.containsKey(recordInfo.getField()))
                    {
                        identifiedName = identifiedFieldNamesByOriginalName.get(recordInfo.getField());
                    }
                }
            }
        }

        if (identifiedName == null)
        {
            identifiedName = methodNameProvider.getName(methodIds.get(rootNode));
        }

        final Collection<INamedParameter> namedParameters = Lists.newArrayList();
        final List<ParameterNode> asmParameters = methodNode.parameters;
        if (asmParameters != null)
        {
            for (int i = 0; i < asmParameters.size(); i++)
            {
                final ParameterNode parameterNode = asmParameters.get(i);

                final INamedParameter parameter = parameterBuilder.build(
                        classNode,
                        methodNode,
                        parameterNode,
                        i,
                        classMetadata,
                        identifiedFieldNamesByOriginalName,
                        parameterIds
                );

                namedParameters.add(parameter);
            }
        }

        return new NamedMethod(
                obfuscatedMethodName,
                identifiedName,
                methodIds.get(methodNode),
                obfuscatedDescriptor,
                methodMetadata.isStatic() || obfuscatedMethodName.equals("<clinit>"),
                methodNode.name.contains("lambda$"),
                namedParameters
        );
    }

    private boolean isRecordComponentForMethod(MethodNode methodNode, String obfuscatedMethodName, String obfuscatedClassName, IMetadataRecordComponent recordInfo, Optional<String> officialRecordDescriptor) {
        return officialRecordDescriptor.isPresent() && recordInfo.getMethods() != null
                 && methodNode.desc.endsWith(officialRecordDescriptor.get())
                 && recordInfo.getMethods().contains(obfuscatedMethodName)
                 && metadataToRuntimeRemapper.remapField(obfuscatedClassName, recordInfo.getField(), recordInfo.getDesc())
                      .map(name -> name.equals(methodNode.name)).orElse(false);
    }

    private boolean isValidOverriddenMethod(Map<String, ClassNode> classNodesByAstName, IMetadataMethodReference override, MethodNode candidate) {
        return runtimeToASTRemapper
                .remapMethod(classNodesByAstName.get(override.getOwner()).name, candidate.name, candidate.desc)
                .filter(remappedOverrideCandidateName -> isValidOverriddenCandidateName(override, candidate, remappedOverrideCandidateName))
                .isPresent();
    }

    private boolean isValidOverriddenCandidateName(IMetadataMethodReference override, MethodNode candidate, String remappedOverrideCandidateName) {
        return remappedOverrideCandidateName.equals(override.getName())
                && runtimeToASTRemapper.remapDescriptor(candidate.desc)
                .filter(remappedOverrideCandidateDescriptor -> remappedOverrideCandidateDescriptor.equals(override.getDesc()))
                .isPresent();
    }

    private boolean isInit(MethodNode methodNode) {
        return methodNode.name.equals("<init>") || methodNode.name.equals("<clinit>");
    }

    private boolean isMain(MethodNode methodNode) {
        return methodNode.name.equals("main") &&
                methodNode.desc.equals("([Ljava/lang/String;)V") &&
                (methodNode.access & (ACC_STATIC | ACC_PUBLIC)) == (ACC_STATIC | ACC_PUBLIC);
    }

    private record NamedMethod(String originalName, String identifiedName, int id, String originalDescriptor, boolean isStatic, boolean isLambda, Collection<INamedParameter> parameters) implements INamedMethod {}

}
