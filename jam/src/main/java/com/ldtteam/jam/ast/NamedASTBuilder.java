package com.ldtteam.jam.ast;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.ldtteam.jam.spi.asm.IASMData;
import com.ldtteam.jam.spi.ast.metadata.IMetadataAST;
import com.ldtteam.jam.spi.ast.metadata.IMetadataClass;
import com.ldtteam.jam.spi.ast.metadata.IMetadataMethod;
import com.ldtteam.jam.spi.ast.named.*;
import com.ldtteam.jam.spi.ast.named.builder.INamedASTBuilder;
import com.ldtteam.jam.spi.ast.named.builder.INamedClassBuilder;
import com.ldtteam.jam.spi.name.INameProvider;
import com.ldtteam.jam.spi.name.IRemapper;
import com.ldtteam.jam.util.SetsUtil;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NamedASTBuilder implements INamedASTBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(NamedASTBuilder.class);

    public static INamedASTBuilder create(
            IRemapper runtimeToASTRemapper, IRemapper asTtoRuntimeRemapper, INamedClassBuilder classBuilder
    ) {
        return new NamedASTBuilder(runtimeToASTRemapper, asTtoRuntimeRemapper, classBuilder);
    }

    private final IRemapper runtimeToASTRemapper;
    private final IRemapper ASTtoRuntimeRemapper;
    private final INamedClassBuilder classBuilder;

    private NamedASTBuilder(IRemapper runtimeToASTRemapper, IRemapper asTtoRuntimeRemapper, INamedClassBuilder classBuilder) {
        this.runtimeToASTRemapper = runtimeToASTRemapper;
        ASTtoRuntimeRemapper = asTtoRuntimeRemapper;
        this.classBuilder = classBuilder;
    }


    @Override
    public INamedAST build(
            final BiMap<ClassNode, Integer> classIds,
            final BiMap<MethodNode, Integer> methodIds,
            final BiMap<FieldNode, Integer> fieldIds,
            final BiMap<ParameterNode, Integer> parameterIds,
            final IASMData asmData,
            final IMetadataAST metadataAST
    ) {
        record ClassNodesByMethodNodeEntry(ClassNode classNode, MethodNode methodNode) {}
        final Map<MethodNode, ClassNode> classNodesByMethodNode = asmData.classes().stream()
                .flatMap(classNode -> classNode.methods.stream().map(methodNode -> new ClassNodesByMethodNodeEntry(classNode, methodNode)))
                .collect(Collectors.toMap(ClassNodesByMethodNodeEntry::methodNode, ClassNodesByMethodNodeEntry::classNode));
        final Map<ClassNode, LinkedList<ClassNode>> inheritanceData = buildInheritanceData(asmData.classes());
        final Map<MethodNode, MethodNode> rootMethodsByOverride = buildForcedMethods(
                asmData.methods(),
                inheritanceData,
                classNodesByMethodNode,
                methodIds,
                metadataAST
        );

        final Map<String, ClassNode> classNodesByASTName = asmData.classes().stream()
                .collect(Collectors.toMap(
                        classNode -> runtimeToASTRemapper.remapClass(classNode.name)
                                .orElseThrow(() -> new IllegalStateException("Failed to remap class: %s".formatted(classNode.name))),
                        Function.identity()));

        final Collection<INamedClass> classes = new ArrayList<>();
        classIds.keySet().forEach(classNode -> {
            final INamedClass namedClass = classBuilder.build(
                    classNode,
                    metadataAST,
                    classNodesByASTName,
                    rootMethodsByOverride,
                    classIds,
                    fieldIds,
                    methodIds,
                    parameterIds
            );

            classes.add(namedClass);
        });

        return new NamedAST(classes);
    }

    private Map<ClassNode, LinkedList<ClassNode>> buildInheritanceData(final Collection<ClassNode> classes)
    {
        final Map<String, ClassNode> classNodesByName = classes.stream()
                .collect(Collectors.toMap(INameProvider.classes(), Function.identity()));

        return classes.stream()
                .collect(Collectors.toMap(Function.identity(), classNode -> getInheritanceOf(classNodesByName, classNode.name, Sets.newHashSet())));
    }

    private LinkedList<ClassNode> getInheritanceOf(final Map<String, ClassNode> classNodesByName, final String className, final Set<ClassNode> superTypes)
    {
        final ClassNode classNode = classNodesByName.get(className);
        if (classNode == null)
        {
            return new LinkedList<>();
        }

        final LinkedList<ClassNode> inheritance = new LinkedList<>();
        superTypes.add(classNode);
        inheritance.add(classNode);

        final String superClassName = classNode.superName;
        if (superClassName != null)
        {
            final LinkedList<ClassNode> superInheritance = getInheritanceOf(classNodesByName, superClassName, superTypes);
            inheritance.addAll(superInheritance);
        }

        final List<String> interfaces = classNode.interfaces;
        if (interfaces != null)
        {
            for (String interfaceName : interfaces)
            {
                final LinkedList<ClassNode> superInheritance = getInheritanceOf(classNodesByName, interfaceName, superTypes);
                inheritance.addAll(superInheritance);
            }
        }

        return inheritance;
    }

    private Map<MethodNode, MethodNode> buildForcedMethods(
            final Collection<MethodNode> methods,
            final Map<ClassNode, LinkedList<ClassNode>> classInheritanceData,
            final Map<MethodNode, ClassNode> classNodesByMethodNode,
            final BiMap<MethodNode, Integer> methodIds,
            final IMetadataAST metadataAST)
    {
        final Map<MethodReference, MethodNode> methodsByReference = collectMethodReferences(methods, classNodesByMethodNode);
        final Multimap<MethodNode, MethodNode> overrides = collectMethodOverrides(methods, classInheritanceData, classNodesByMethodNode, metadataAST, methodsByReference);
        final Set<Set<MethodNode>> combinedTrees = buildOverrideTrees(overrides);
        return determineIdsPerOverrideTree(methodIds, combinedTrees);
    }

    public Map<MethodReference, MethodNode> collectMethodReferences(
      final Collection<MethodNode> methods,
      final Map<MethodNode, ClassNode> classNodesByMethodNode
    ) {
        final Map<MethodReference, MethodNode> methodsByReference = new HashMap<>();
        methods.forEach(methodNode -> {
            if (methodNode.name.startsWith("<"))
            {
                return;
            }

            final ClassNode classNode = classNodesByMethodNode.get(methodNode);
            methodsByReference.put(
              new MethodReference(classNode.name, methodNode.name, methodNode.desc),
              methodNode
            );
        });
        return methodsByReference;
    }

    public Multimap<MethodNode, MethodNode> collectMethodOverrides(
      final Collection<MethodNode> methods,
      final Map<ClassNode, LinkedList<ClassNode>> classInheritanceData,
      final Map<MethodNode, ClassNode> classNodesByMethodNode,
      final IMetadataAST metadataAST,
      final Map<MethodReference, MethodNode> methodsByReference
    ) {

        final Multimap<MethodNode, MethodNode> overrides = HashMultimap.create();
        methods.forEach(
          methodNode -> {
              if (methodNode.name.startsWith("<"))
              {
                  return;
              }

              final ClassNode classNode = classNodesByMethodNode.get(methodNode);
              collectMethodOverridesFromASMData(classInheritanceData, overrides, methodNode, classNode);

              final IMetadataMethod methodInfo = getMetadataForMethod(metadataAST, methodNode, classNode);
              if (methodInfo == null)
              {
                  return;
              }

              collectMethodOverridesFromMetadata(methodsByReference, overrides, methodNode, methodInfo);
          }
        );

        return overrides;
    }

    private void collectMethodOverridesFromASMData(
      final Map<ClassNode, LinkedList<ClassNode>> classInheritanceData,
      final Multimap<MethodNode, MethodNode> overrides,
      final MethodNode methodNode,
      final ClassNode classNode)
    {
        final LinkedList<ClassNode> superTypes = classInheritanceData.getOrDefault(classNode, new LinkedList<>());
        if (!superTypes.isEmpty())
        {
            superTypes.forEach(superType -> superType.methods.stream()
                                              .filter(superMethodNode -> !superMethodNode.name.equals("<"))
                                              .filter(superMethodNode -> superMethodNode.name.equals(methodNode.name) && superMethodNode.desc.equals(methodNode.desc))
                                              .forEach(superMethodNode -> overrides.put(methodNode, superMethodNode)));
        }
    }

    private IMetadataMethod getMetadataForMethod(final IMetadataAST metadataAST, final MethodNode methodNode, final ClassNode classNode)
    {
        final String obfuscatedOwnerClassName = runtimeToASTRemapper.remapClass(classNode.name)
                                                  .orElseThrow(() -> new IllegalStateException("Failed to remap class: %s".formatted(classNode.name)));
        if (Objects.equals(obfuscatedOwnerClassName, classNode.name)) {
            //Not an obfuscated class. Ignore
            return null;
        }
        if (!metadataAST.getClassesByName().containsKey(obfuscatedOwnerClassName)) {
            //Not a class we have metadata for, would be weird, so log and ignore.
            LOGGER.warn("Could not find metadata for class: " + classNode.name + " its obfuscated class name: " + obfuscatedOwnerClassName + " does not seems to be found in the json metadata.");
            return null;
        }
        final IMetadataClass classInfo = metadataAST.getClassesByName().get(obfuscatedOwnerClassName);
        if (classInfo == null) {
            //Not a class we have metadata for, would be weird, so log and ignore.
            LOGGER.warn("Could not find metadata for class: " + classNode.name + " its obfuscated class name: " + obfuscatedOwnerClassName + " does not seems to be found in the json metadata.");
            return null;
        }

        final String obfuscatedMethodName = runtimeToASTRemapper.remapMethod(classNode.name, methodNode.name, methodNode.desc)
                                              .orElseThrow(() -> new IllegalStateException("Failed to remap method: %s in class: %s".formatted(methodNode.name, classNode.name)));
        if (Objects.equals(obfuscatedMethodName, methodNode.name)) {
            //Not obfuscated method without metadata. Ignore
            return null;
        }
        final String obfuscatedDescriptor = runtimeToASTRemapper.remapDescriptor(methodNode.desc)
                                              .orElseThrow(() -> new IllegalStateException("Failed to remap descriptor: %s".formatted(methodNode.desc)));
        if (classInfo.getMethodsByName() == null) {
            throw new IllegalStateException("The given class does contain methods");
        }
        final IMetadataMethod methodInfo = classInfo.getMethodsByName().get(obfuscatedMethodName + obfuscatedDescriptor);
        if (methodInfo == null) {
            //Not a class we have metadata for, would be weird, so log and ignore.
            LOGGER.warn("Could not find metadata for method: %s(%s) in class: %s does not seems to be found in the json metadata.".formatted(methodNode.name, methodNode.desc, classNode.name));
            return null;
        }
        return methodInfo;
    }

    private void collectMethodOverridesFromMetadata(
      final Map<MethodReference, MethodNode> methodsByReference,
      final Multimap<MethodNode, MethodNode> overrides,
      final MethodNode methodNode,
      final IMetadataMethod methodInfo)
    {
        if (methodInfo.getOverrides() == null)
        {
            //No overrides available.
            return;
        }

        methodInfo.getOverrides().stream()
          .map(method -> {
              final String officialClassName = ASTtoRuntimeRemapper.remapClass(method.getOwner())
                                                 .orElseThrow(() -> new IllegalStateException("Failed to remap class: %s".formatted(method.getOwner())));
              final String officialDescriptor = ASTtoRuntimeRemapper.remapDescriptor(method.getDesc())
                                                  .orElseThrow(() -> new IllegalStateException("Failed to remap descriptor: %s".formatted(method.getDesc())));
              if (officialClassName == null || officialDescriptor == null
                    || officialClassName.equals(method.getOwner()) || officialDescriptor.equals(method.getDesc()))
              {
                  //Not remappable or not obfuscated. Ignore.
                  return null;
              }

              final String officialMethodName = ASTtoRuntimeRemapper.remapMethod(method.getOwner(), method.getName(), method.getDesc())
                                                  .orElseThrow(() -> new IllegalStateException("Failed to remap method: %s in class: %s".formatted(method.getName(), method.getOwner())));
              final MethodReference reference = new MethodReference(officialClassName, officialMethodName, officialDescriptor);
              if (!methodsByReference.containsKey(reference)) {
                  //Not a class we have metadata for, would be weird, so log and ignore.
                  LOGGER.warn("Could not find method node for method: %s(%s) in class: %s".formatted(officialMethodName, officialDescriptor, officialDescriptor));
                  return null;
              }

              return methodsByReference.get(reference);
          })
          .filter(Objects::nonNull)
          .forEach(superMethodNode -> overrides.put(methodNode, superMethodNode));
    }

    public Set<Set<MethodNode>> buildOverrideTrees(
      final Multimap<MethodNode, MethodNode> overrides
    ) {
        final Set<MethodNode> processedNode = Sets.newHashSet();
        final Set<Set<MethodNode>> combinedTrees = Sets.newHashSet();
        final Map<MethodNode, Collection<MethodNode>> overrideBranchMap = overrides.asMap();

        overrideBranchMap.keySet().forEach(
          overridenMethod -> {
              if (processedNode.contains(overridenMethod))
              {
                  return;
              }

              if (overrideBranchMap.get(overridenMethod).size() == 1)
              {
                  if (overrideBranchMap.get(overridenMethod).contains(overridenMethod))
                  {
                      return;
                  }
              }

              processedNode.add(overridenMethod);
              final Set<MethodNode> workingSet = Sets.newHashSet(overrides.get(overridenMethod));
              for (final Collection<MethodNode> overrideBranch : overrideBranchMap.values())
              {
                  if (overrideBranch.stream().anyMatch(workingSet::contains))
                  {
                      workingSet.addAll(overrideBranch);
                  }
              }

              combinedTrees.add(workingSet);
              processedNode.addAll(workingSet);
          }
        );

        return combinedTrees;
    }

    public Map<MethodNode, MethodNode> determineIdsPerOverrideTree(
      final BiMap<MethodNode, Integer> methodIds,
      final Set<Set<MethodNode>> combinedTrees
    ) {
        final BiMap<Integer, MethodNode> methodNodesById = methodIds.inverse();
        final Map<MethodNode, MethodNode> overridesByMethod = new HashMap<>();
        for (final Set<MethodNode> combinedTree : combinedTrees)
        {
            final MethodNode rootNode = combinedTree.stream()
                                          .mapToInt(methodIds::get)
                                          .min()
                                          .stream()
                                          .mapToObj(methodNodesById::get)
                                          .findFirst()
                                          .orElseThrow(() -> new IllegalStateException("No root node found"));

            for (final MethodNode methodNode : combinedTree)
            {
                overridesByMethod.put(methodNode, rootNode);
            }
        }
        return overridesByMethod;
    }

    private record NamedAST(Collection<INamedClass> classes) implements INamedAST {}

    private record MethodReference(String owner, String name, String descriptor) {}
}
