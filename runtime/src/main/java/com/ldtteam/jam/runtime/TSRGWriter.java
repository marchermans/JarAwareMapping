package com.ldtteam.jam.runtime;

import com.google.common.collect.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.ldtteam.jam.spi.meta.IASMData;
import com.ldtteam.jam.spi.name.INameProvider;
import com.ldtteam.jam.spi.writer.IOutputWriter;
import com.machinezoo.noexception.Exceptions;
import net.minecraftforge.srgutils.IMappingBuilder;
import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.INamedMappingFile;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

public class TSRGWriter implements IOutputWriter
{

    private static final Gson GSON                      = new GsonBuilder().create();
    private static final Type TYPE_MAP_STRING_CLASSINFO = new TypeToken<Map<String, MappingToyMetadata.ClassInfo>>() {}.getType();
    private final IMappingFile                              officialToObfuscatedMapping;
    private final IMappingFile                              obfuscatedToOfficialMapping;
    private final Map<String, MappingToyMetadata.ClassInfo> mappingToyMetadata;
    private TSRGWriter(final Path officialToObfuscatedMappingFile, final Path metadataFile)
    {
        final INamedMappingFile officialToObfuscatedMapping = Exceptions.sneak().get(() -> INamedMappingFile.load(Files.newInputStream(officialToObfuscatedMappingFile)));
        this.officialToObfuscatedMapping = officialToObfuscatedMapping.getMap("left", "right");
        this.obfuscatedToOfficialMapping = officialToObfuscatedMapping.getMap("right", "left");
        this.mappingToyMetadata = GSON.fromJson(
          Exceptions.sneak().get(() -> Files.readString(metadataFile)),
          TYPE_MAP_STRING_CLASSINFO
        );
    }

    public static IOutputWriter create(final Path officialToObfuscatedMappingFile, final Path metadataFile)
    {
        return new TSRGWriter(officialToObfuscatedMappingFile, metadataFile);
    }

    public static Optional<Integer> parseId(String s)
    {
        try
        {
            return Optional.of(Integer.parseInt(s));
        }
        catch (NumberFormatException | NullPointerException e)
        {
            return Optional.empty();
        }
    }

    @Override
    public void write(
      final Path outputDirectory,
      final BiMap<ClassNode, Integer> classIds,
      final BiMap<MethodNode, Integer> methodIds,
      final BiMap<FieldNode, Integer> fieldIds,
      final BiMap<ParameterNode, Integer> parameterIds,
      final IASMData asmData)
    {
        IMappingBuilder builder = IMappingBuilder.create("obf", "srg", "id");

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
          mappingToyMetadata
        );

        final Map<String, ClassNode> classNodesByName = asmData.classes().stream()
          .collect(Collectors.toMap(INameProvider.classes(), Function.identity()));

        final Map<String, ClassNode> classNodesByObfuscatedName = asmData.classes().stream()
          .collect(Collectors.toMap(classNode -> officialToObfuscatedMapping.remapClass(classNode.name), Function.identity()));

        classIds.forEach((classNode, id) -> {
            final String obfuscatedClassName = officialToObfuscatedMapping.remapClass(classNode.name);
            final IMappingBuilder.IClass classMapping = builder.addClass(
              obfuscatedClassName,
              "net/minecraft/src/" + getClassName(
                classIds,
                classNodesByName,
                classNode
              ),
              id.toString()
            );

            final MappingToyMetadata.ClassInfo classMetadata = mappingToyMetadata.computeIfAbsent(obfuscatedClassName, (key) -> {
                throw new IllegalStateException("Missing metadata for " + classNode.name + " (" + key + ")");
            });

            String recordDescriptor = null;
            if (classMetadata.getRecords() != null)
            {
                final StringBuilder recordDescBuilder = new StringBuilder("(");
                for (final MappingToyMetadata.ClassInfo.RecordInfo recordInfo : classMetadata.getRecords())
                {
                    recordDescBuilder.append(recordInfo.getDesc());
                }
                recordDescriptor = recordDescBuilder.append(")V").toString();
            }

            final int FLAG = Opcodes.ACC_FINAL | Opcodes.ACC_ENUM | Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
            final Map<String, String> obfuscatedToMappedFieldNames = Maps.newHashMap();
            classNode.fields.forEach(fieldNode -> {
                final String obfuscatedFieldName = officialToObfuscatedMapping.getClass(classNode.name).remapField(fieldNode.name);
                String name = null;

                final MappingToyMetadata.ClassInfo.FieldInfo fieldMetadata = classMetadata.getFields().computeIfAbsent(obfuscatedFieldName, (key) -> {
                    throw new IllegalStateException("Missing metadata for " + fieldNode.name + " (" + key + ") in " + classNode.name);
                });

                if (fieldMetadata.getForce() != null)
                {
                    name = fieldMetadata.getForce();
                }

                if (name == null && ((fieldNode.access & FLAG) == FLAG || "$VALUES".equals(fieldNode.name)))
                {
                    name = fieldNode.name;
                }

                if (name == null && obfuscatedClassName.equals(fieldNode.name))
                {
                    name = fieldNode.name;
                }

                if (name == null)
                {
                    name = "f_" + fieldIds.get(fieldNode) + "_";
                }

                final IMappingBuilder.IField field = classMapping.field(
                  obfuscatedFieldName,
                  name,
                  fieldIds.get(fieldNode).toString()
                );

                obfuscatedToMappedFieldNames.put(obfuscatedFieldName, name);
            });

            final String finalRecordDescriptor = recordDescriptor;
            classNode.methods.forEach(methodNode -> {
                final String obfuscatedDescriptor = officialToObfuscatedMapping.remapDescriptor(methodNode.desc);
                final String obfuscatedMethodName = officialToObfuscatedMapping.getClass(classNode.name).remapMethod(methodNode.name, methodNode.desc);

                final MappingToyMetadata.ClassInfo.MethodInfo methodMetadata = classMetadata.getMethods().getOrDefault(obfuscatedMethodName + obfuscatedDescriptor, null);

                MethodNode rootNode = methodNode;
                if (rootMethodsByOverride.containsKey(methodNode))
                {
                    rootNode = rootMethodsByOverride.get(methodNode);
                }

                String name = null;

                if (methodMetadata != null && methodMetadata.getForce() != null)
                {
                    name = methodMetadata.getForce();
                }

                if (name == null && (methodNode.name.equals("<init>") || methodNode.name.equals("<clinit>")))
                {
                    name = methodNode.name;
                }

                if (methodNode.name.equals("main") &&
                      methodNode.desc.equals("([Ljava/lang/String;)V") &&
                      (methodNode.access & (ACC_STATIC | ACC_PUBLIC)) == (ACC_STATIC | ACC_PUBLIC))
                {
                    name = "main";
                }

                if (name == null && methodMetadata != null && methodMetadata.getOverrides() != null)
                {
                    final Set<Integer> originalIds = Sets.newHashSet();

                    for (MappingToyMetadata.Method override : methodMetadata.getOverrides())
                    {
                        // if the class isn't in our codebase, then trust the name
                        if (!classNodesByObfuscatedName.containsKey(override.getOwner()))
                        {
                            name = override.getName();
                            break;
                        }
                        else
                        { // If it is, then it should of been assigned an ID earlier so use it's id.
                            final MethodNode overridenMethod = classNodesByObfuscatedName.get(override.getOwner()).methods.stream()
                              .filter(candidate -> officialToObfuscatedMapping.getClass(classNodesByObfuscatedName.get(override.getOwner()).name)
                                .remapMethod(candidate.name, candidate.desc)
                                .equals(override.getName()) &&
                                                     officialToObfuscatedMapping.remapDescriptor(candidate.desc).equals(override.getDesc())).findFirst().orElse(null);


                            if (overridenMethod != null)
                            {
                                final Integer overridenMethodId =
                                  rootMethodsByOverride.containsKey(overridenMethod) ? methodIds.get(rootMethodsByOverride.get(overridenMethod)) : methodIds.get(overridenMethod);
                                originalIds.add(overridenMethodId);
                            }
                        }
                    }

                    if (name == null && originalIds.size() > 0)
                    {
                        name = "m_" + originalIds.stream().mapToInt(Integer::intValue).min().orElse(-1) + "_";
                    }
                }

                if (name == null && classMetadata.getRecords() != null && methodNode.desc.startsWith("()"))
                {
                    for (final MappingToyMetadata.ClassInfo.RecordInfo recordInfo : classMetadata.getRecords())
                    {
                        if (methodNode.desc.endsWith(recordInfo.getDesc()) && recordInfo.getMethods().contains(obfuscatedMethodName))
                        {
                            final IMappingFile.IClass obfuscatedClass = obfuscatedToOfficialMapping.getClass(officialToObfuscatedMapping.remapClass(classNode.name));
                            final IMappingFile.IField obfuscatedField = obfuscatedClass.getField(recordInfo.getField());
                            final IMappingFile.IMethod obfuscatedMethod = obfuscatedClass.getMethod(obfuscatedMethodName, obfuscatedDescriptor);
                            if (obfuscatedField != null && obfuscatedMethod != null && Objects.equals(obfuscatedField.getMapped(), obfuscatedMethod.getMapped()))
                            {
                                if (obfuscatedToMappedFieldNames.containsKey(recordInfo.getField()))
                                {
                                    name = obfuscatedToMappedFieldNames.get(recordInfo.getField());
                                }
                            }
                        }
                    }
                }

                if (name == null)
                {
                    name = "m_" + methodIds.get(rootNode) + "_";
                }

                final IMappingBuilder.IMethod methodMapping = classMapping.method(
                  officialToObfuscatedMapping.remapDescriptor(methodNode.desc),
                  officialToObfuscatedMapping.getClass(classNode.name).remapMethod(methodNode.name, methodNode.desc),
                  name,
                  methodIds.get(methodNode).toString()
                );

                if ((methodNode.access & Opcodes.ACC_STATIC) != 0)
                {
                    methodMapping.meta("is_static", "true");
                }

                List<ParameterNode> parameters = methodNode.parameters;
                if (parameters != null)
                {
                    for (int i = 0; i < parameters.size(); i++)
                    {
                        final ParameterNode parameterNode = parameters.get(i);

                        final String obfuscatedParameterName = Objects.requireNonNull(officialToObfuscatedMapping.getClass(classNode.name)
                            .getMethod(methodNode.name, methodNode.desc))
                          .remapParameter(i, parameterNode.name);


                        String mappedParameterName = null;

                        if (obfuscatedMethodName.equals("<init>") && classMetadata.getMethods() != null && finalRecordDescriptor != null && finalRecordDescriptor.equals(obfuscatedDescriptor)) {
                            final MappingToyMetadata.ClassInfo.RecordInfo recordInfo = classMetadata.getRecords().get(i);
                            if (obfuscatedToMappedFieldNames.containsKey(recordInfo.getField())) {
                                mappedParameterName = obfuscatedToMappedFieldNames.get(recordInfo.getField());
                            }
                        }

                        if (mappedParameterName == null) {
                            mappedParameterName = "p_" + parameterIds.get(parameterNode) + "_";
                        }

                        methodMapping.parameter(
                          i,
                          obfuscatedParameterName,
                          mappedParameterName,
                          parameterIds.get(parameterNode).toString()
                        );
                    }
                }
            });
        });

        final INamedMappingFile mappingFile = builder.build();
        Exceptions.sneak().run(
          () -> mappingFile.write(
            outputDirectory.resolve("joined.tsrg"),
            IMappingFile.Format.TSRG2
          )
        );
    }

    private String getClassName(final BiMap<ClassNode, Integer> classIds, final Map<String, ClassNode> classNodesByNameMap, final ClassNode classNode)
    {
        if (!classIds.containsKey(classNode))
        {
            return classNode.name;
        }

        if (!classNode.name.contains("$"))
        {
            return "C_" + classIds.get(classNode) + "_";
        }

        final String outerName = classNode.name.substring(0, classNode.name.lastIndexOf('$'));
        if (!classNodesByNameMap.containsKey(outerName))
        {
            return outerName;
        }

        final String innerName = classNode.name.substring(classNode.name.lastIndexOf('$') + 1);
        final Optional<Integer> anonClassName = parseId(innerName);
        final String innerId = anonClassName.map(String::valueOf).orElse("C_" + classIds.get(classNode) + "_");

        return getClassName(
          classIds,
          classNodesByNameMap,
          classNodesByNameMap.get(outerName)
        ) + "$" + innerId;
    }

    private LinkedHashMap<MethodNode, MethodNode> buildForcedMethods(
      final Collection<MethodNode> methods,
      final Map<ClassNode, LinkedList<ClassNode>> classInheritanceData,
      final Map<MethodNode, ClassNode> classNodesByMethodNode,
      final BiMap<MethodNode, Integer> methodIds,
      final Map<String, MappingToyMetadata.ClassInfo> classInfoMap)
    {
        final Multimap<MethodNode, MethodNode> overrides = HashMultimap.create();
        methods.forEach(
          methodNode -> {
              if (methodNode.name.startsWith("<"))
              {
                  return;
              }

              final ClassNode classNode = classNodesByMethodNode.get(methodNode);
              final LinkedList<ClassNode> superTypes = classInheritanceData.getOrDefault(classNode, new LinkedList<>());
              if (superTypes.isEmpty())
              {
                  return;
              }

              superTypes.forEach(superType -> {
                  superType.methods.stream()
                    .filter(superMethodNode -> !superMethodNode.name.equals("<"))
                    .filter(superMethodNode -> superMethodNode.name.equals(methodNode.name) && superMethodNode.desc.equals(methodNode.desc))
                    .findFirst()
                    .ifPresent(superMethodNode -> overrides.put(methodNode, superMethodNode));
              });
          }
        );

        final Set<MethodNode> processedNode = new HashSet<>();
        final Set<Set<MethodNode>> combinedTrees = new HashSet<>();
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
              final Set<MethodNode> workingSet = new HashSet<>(overrides.get(overridenMethod));
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

        final BiMap<Integer, MethodNode> methodNodesById = methodIds.inverse();
        final LinkedHashMap<MethodNode, MethodNode> overridesByMethod = new LinkedHashMap<>();
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

    private Map<ClassNode, LinkedList<ClassNode>> buildInheritanceData(final Collection<ClassNode> classes)
    {
        final Map<String, ClassNode> classNodesByName = classes.stream()
          .collect(Collectors.toMap(INameProvider.classes(), Function.identity()));

        return classes.stream()
          .collect(Collectors.toMap(Function.identity(), classNode -> getInheritanceOf(classNodesByName, classNode.name, new HashSet<>())));
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
}
