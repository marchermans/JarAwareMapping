package com.ldtteam.jam.ast;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.ldtteam.jam.spi.asm.*;
import com.ldtteam.jam.spi.ast.metadata.IMetadataAST;
import com.ldtteam.jam.spi.ast.metadata.IMetadataClass;
import com.ldtteam.jam.spi.ast.metadata.IMetadataMethod;
import com.ldtteam.jam.spi.ast.named.INamedAST;
import com.ldtteam.jam.spi.ast.named.INamedClass;
import com.ldtteam.jam.spi.ast.named.builder.INamedASTBuilder;
import com.ldtteam.jam.spi.ast.named.builder.INamedClassBuilder;
import com.ldtteam.jam.spi.name.INameProvider;
import com.ldtteam.jam.spi.name.IRemapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NamedASTBuilder implements INamedASTBuilder
{

    private static final Logger LOGGER = LoggerFactory.getLogger(NamedASTBuilder.class);

    public static INamedASTBuilder create(
      IRemapper runtimeToASTRemapper, IRemapper asTtoRuntimeRemapper, INamedClassBuilder classBuilder
    )
    {
        return new NamedASTBuilder(runtimeToASTRemapper, asTtoRuntimeRemapper, classBuilder);
    }

    private final IRemapper          runtimeToASTRemapper;
    private final IRemapper          ASTtoRuntimeRemapper;
    private final INamedClassBuilder classBuilder;

    private NamedASTBuilder(IRemapper runtimeToASTRemapper, IRemapper asTtoRuntimeRemapper, INamedClassBuilder classBuilder)
    {
        this.runtimeToASTRemapper = runtimeToASTRemapper;
        ASTtoRuntimeRemapper = asTtoRuntimeRemapper;
        this.classBuilder = classBuilder;
    }

    @Override
    public INamedAST build(
      final BiMap<ClassData, ClassData> classMappings,
      final BiMap<FieldData, FieldData> fieldMappings,
      final BiMap<MethodData, MethodData> methodMappings,
      final BiMap<ParameterData, ParameterData> parameterMappings,
      final BiMap<ClassData, Integer> classIds,
      final BiMap<MethodData, Integer> methodIds,
      final BiMap<FieldData, Integer> fieldIds,
      final BiMap<ParameterData, Integer> parameterIds,
      final IASMData asmData,
      final IMetadataAST metadataAST
    )
    {
        record ClassDatasByMethodDataEntry(ClassData classData, MethodData methodData) {}
        final Map<MethodData, ClassData> classDatasByMethodData = asmData.classes().stream()
                                                                    .flatMap(classData -> classData.node().methods.stream()
                                                                                            .map(node -> new ClassDatasByMethodDataEntry(classData, new MethodData(classData, node))))
                                                                    .collect(Collectors.toMap(ClassDatasByMethodDataEntry::methodData, ClassDatasByMethodDataEntry::classData));
        final Map<ClassData, LinkedList<ClassData>> inheritanceData = buildInheritanceData(asmData.classes());
        final Map<MethodData, MethodData> rootMethodsByOverride = buildForcedMethods(
          asmData.methods(),
          inheritanceData,
          classDatasByMethodData,
          methodIds,
          metadataAST
        );

        final Map<String, ClassData> classDatasByASTName = asmData.classes().stream()
                                                             .collect(Collectors.toMap(
                                                               classData -> runtimeToASTRemapper.remapClass(classData.node().name)
                                                                              .orElseThrow(() -> new IllegalStateException("Failed to remap class: %s".formatted(classData.node().name))),
                                                               Function.identity()));

        final Collection<INamedClass> classes = new ArrayList<>();
        classIds.keySet().forEach(classData -> {
            final INamedClass namedClass = classBuilder.build(
              classData,
              metadataAST,
              classDatasByASTName,
              rootMethodsByOverride,
              classMappings,
              fieldMappings,
              methodMappings,
              parameterMappings,
              classIds,
              fieldIds,
              methodIds,
              parameterIds
            );

            classes.add(namedClass);
        });

        return new NamedAST(classes);
    }

    private Map<ClassData, LinkedList<ClassData>> buildInheritanceData(final Collection<ClassData> classes)
    {
        final Map<String, ClassData> classDatasByName = classes.stream()
                                                          .collect(Collectors.toMap(INameProvider.classes(), Function.identity()));

        return classes.stream()
                 .collect(Collectors.toMap(Function.identity(), classData -> getInheritanceOf(classDatasByName, classData.node().name, Sets.newHashSet())));
    }

    private LinkedList<ClassData> getInheritanceOf(final Map<String, ClassData> classDatasByName, final String className, final Set<ClassData> superTypes)
    {
        final ClassData classData = classDatasByName.get(className);
        if (classData == null)
        {
            return new LinkedList<>();
        }

        final LinkedList<ClassData> inheritance = new LinkedList<>();
        superTypes.add(classData);
        inheritance.add(classData);

        final String superClassName = classData.node().superName;
        if (superClassName != null)
        {
            final LinkedList<ClassData> superInheritance = getInheritanceOf(classDatasByName, superClassName, superTypes);
            inheritance.addAll(superInheritance);
        }

        final List<String> interfaces = classData.node().interfaces;
        if (interfaces != null)
        {
            for (String interfaceName : interfaces)
            {
                final LinkedList<ClassData> superInheritance = getInheritanceOf(classDatasByName, interfaceName, superTypes);
                inheritance.addAll(superInheritance);
            }
        }

        return inheritance;
    }

    private Map<MethodData, MethodData> buildForcedMethods(
      final Collection<MethodData> methods,
      final Map<ClassData, LinkedList<ClassData>> classInheritanceData,
      final Map<MethodData, ClassData> classDatasByMethodData,
      final BiMap<MethodData, Integer> methodIds,
      final IMetadataAST metadataAST)
    {
        final Map<MethodReference, MethodData> methodsByReference = collectMethodReferences(methods, classDatasByMethodData);
        final Multimap<MethodData, MethodData> overrides = collectMethodOverrides(methods, classInheritanceData, classDatasByMethodData, metadataAST, methodsByReference);
        final Set<Set<MethodData>> combinedTrees = buildOverrideTrees(overrides);
        return determineIdsPerOverrideTree(methodIds, combinedTrees);
    }

    public Map<MethodReference, MethodData> collectMethodReferences(
      final Collection<MethodData> methods,
      final Map<MethodData, ClassData> classDatasByMethodData
    )
    {
        final Map<MethodReference, MethodData> methodsByReference = new HashMap<>();
        methods.forEach(methodData -> {
            if (methodData.node().name.startsWith("<"))
            {
                return;
            }

            final ClassData classData = classDatasByMethodData.get(methodData);
            methodsByReference.put(
              new MethodReference(classData.node().name, methodData.node().name, methodData.node().desc),
              methodData
            );
        });
        return methodsByReference;
    }

    public Multimap<MethodData, MethodData> collectMethodOverrides(
      final Collection<MethodData> methods,
      final Map<ClassData, LinkedList<ClassData>> classInheritanceData,
      final Map<MethodData, ClassData> classDatasByMethodData,
      final IMetadataAST metadataAST,
      final Map<MethodReference, MethodData> methodsByReference
    )
    {

        final Multimap<MethodData, MethodData> overrides = HashMultimap.create();
        methods.forEach(
          methodData -> {
              if (methodData.node().name.startsWith("<"))
              {
                  return;
              }

              final ClassData classData = classDatasByMethodData.get(methodData);
              collectMethodOverridesFromASMData(classInheritanceData, overrides, methodData, classData);

              final IMetadataMethod methodInfo = getMetadataForMethod(metadataAST, methodData, classData);
              if (methodInfo == null)
              {
                  return;
              }

              collectMethodOverridesFromMetadata(methodsByReference, overrides, methodData, methodInfo);
          }
        );

        return overrides;
    }

    private void collectMethodOverridesFromASMData(
      final Map<ClassData, LinkedList<ClassData>> classInheritanceData,
      final Multimap<MethodData, MethodData> overrides,
      final MethodData methodData,
      final ClassData classData)
    {
        final LinkedList<ClassData> superTypes = classInheritanceData.getOrDefault(classData, new LinkedList<>());
        if (!superTypes.isEmpty())
        {
            superTypes.forEach(superType -> superType.node().methods.stream()
                                              .filter(superMethodData -> !superMethodData.name.equals("<"))
                                              .filter(superMethodData -> superMethodData.name.equals(methodData.node().name) && superMethodData.desc.equals(methodData.node().desc))
                                              .forEach(superMethodData -> overrides.put(methodData, new MethodData(superType, superMethodData))));
        }
    }

    private IMetadataMethod getMetadataForMethod(final IMetadataAST metadataAST, final MethodData methodData, final ClassData classData)
    {
        final String obfuscatedOwnerClassName = runtimeToASTRemapper.remapClass(classData.node().name)
                                                  .orElseThrow(() -> new IllegalStateException("Failed to remap class: %s".formatted(classData.node().name)));
        if (Objects.equals(obfuscatedOwnerClassName, classData.node().name))
        {
            //Not an obfuscated class. Ignore
            return null;
        }
        if (!metadataAST.getClassesByName().containsKey(obfuscatedOwnerClassName))
        {
            //Not a class we have metadata for, would be weird, so log and ignore.
            LOGGER.warn("Could not find metadata for class: " + classData.node().name + " its obfuscated class name: " + obfuscatedOwnerClassName
                          + " does not seems to be found in the json metadata.");
            return null;
        }
        final IMetadataClass classInfo = metadataAST.getClassesByName().get(obfuscatedOwnerClassName);
        if (classInfo == null)
        {
            //Not a class we have metadata for, would be weird, so log and ignore.
            LOGGER.warn("Could not find metadata for class: " + classData.node().name + " its obfuscated class name: " + obfuscatedOwnerClassName
                          + " does not seems to be found in the json metadata.");
            return null;
        }

        final String obfuscatedMethodName = runtimeToASTRemapper.remapMethod(classData.node().name, methodData.node().name, methodData.node().desc)
                                              .orElseThrow(() -> new IllegalStateException("Failed to remap method: %s in class: %s".formatted(methodData.node().name, classData.node().name)));
        if (Objects.equals(obfuscatedMethodName, methodData.node().name))
        {
            //Not obfuscated method without metadata. Ignore
            return null;
        }
        final String obfuscatedDescriptor = runtimeToASTRemapper.remapDescriptor(methodData.node().desc)
                                              .orElseThrow(() -> new IllegalStateException("Failed to remap descriptor: %s".formatted(methodData.node().desc)));
        if (classInfo.getMethodsByName() == null)
        {
            throw new IllegalStateException("The given class does contain methods");
        }
        final IMetadataMethod methodInfo = classInfo.getMethodsByName().get(obfuscatedMethodName + obfuscatedDescriptor);
        if (methodInfo == null)
        {
            //Not a class we have metadata for, would be weird, so log and ignore.
            LOGGER.warn("Could not find metadata for method: %s(%s) in class: %s does not seems to be found in the json metadata.".formatted(methodData.node().name,
              methodData.node().desc,
              classData.node().name));
            return null;
        }
        return methodInfo;
    }

    private void collectMethodOverridesFromMetadata(
      final Map<MethodReference, MethodData> methodsByReference,
      final Multimap<MethodData, MethodData> overrides,
      final MethodData methodData,
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
                  //Not re-mappable or not obfuscated. Ignore.
                  return null;
              }

              final String officialMethodName = ASTtoRuntimeRemapper.remapMethod(method.getOwner(), method.getName(), method.getDesc())
                                                  .orElseThrow(() -> new IllegalStateException("Failed to remap method: %s in class: %s".formatted(method.getName(),
                                                    method.getOwner())));
              final MethodReference reference = new MethodReference(officialClassName, officialMethodName, officialDescriptor);
              if (!methodsByReference.containsKey(reference))
              {
                  //Not a class we have metadata for, would be weird, so log and ignore.
                  LOGGER.warn("Could not find method data for method: %s(%s) in class: %s".formatted(officialMethodName, officialDescriptor, officialDescriptor));
                  return null;
              }

              return methodsByReference.get(reference);
          })
          .filter(Objects::nonNull)
          .forEach(superMethodData -> overrides.put(methodData, superMethodData));
    }

    public Set<Set<MethodData>> buildOverrideTrees(
      final Multimap<MethodData, MethodData> overrides
    )
    {
        final Set<MethodData> processedData = Sets.newHashSet();
        final Set<Set<MethodData>> combinedTrees = Sets.newHashSet();
        final Map<MethodData, Collection<MethodData>> overrideBranchMap = overrides.asMap();

        overrideBranchMap.keySet().forEach(
          overriddenMethod -> {
              if (processedData.contains(overriddenMethod))
              {
                  return;
              }

              if (overrideBranchMap.get(overriddenMethod).size() == 1)
              {
                  if (overrideBranchMap.get(overriddenMethod).contains(overriddenMethod))
                  {
                      return;
                  }
              }

              processedData.add(overriddenMethod);
              final Set<MethodData> workingSet = Sets.newHashSet(overrides.get(overriddenMethod));
              for (final Collection<MethodData> overrideBranch : overrideBranchMap.values())
              {
                  if (overrideBranch.stream().anyMatch(workingSet::contains))
                  {
                      workingSet.addAll(overrideBranch);
                  }
              }

              combinedTrees.add(workingSet);
              processedData.addAll(workingSet);
          }
        );

        return combinedTrees;
    }

    public Map<MethodData, MethodData> determineIdsPerOverrideTree(
      final BiMap<MethodData, Integer> methodIds,
      final Set<Set<MethodData>> combinedTrees
    )
    {
        final BiMap<Integer, MethodData> methodDatasById = methodIds.inverse();
        final Map<MethodData, MethodData> overridesByMethod = new HashMap<>();
        for (final Set<MethodData> combinedTree : combinedTrees)
        {
            final MethodData rootData = combinedTree.stream()
                                          .mapToInt(methodIds::get)
                                          .min()
                                          .stream()
                                          .mapToObj(methodDatasById::get)
                                          .findFirst()
                                          .orElseThrow(() -> new IllegalStateException("No root data found"));

            for (final MethodData methodData : combinedTree)
            {
                overridesByMethod.put(methodData, rootData);
            }
        }
        return overridesByMethod;
    }

    private record NamedAST(Collection<INamedClass> classes) implements INamedAST {}

    private record MethodReference(String owner, String name, String descriptor) {}
}
