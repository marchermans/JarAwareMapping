package com.ldtteam.jam.ast;

import com.google.common.collect.BiMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.ldtteam.jam.spi.asm.ClassData;
import com.ldtteam.jam.spi.asm.FieldData;
import com.ldtteam.jam.spi.asm.MethodData;
import com.ldtteam.jam.spi.asm.ParameterData;
import com.ldtteam.jam.spi.ast.metadata.IMetadataAST;
import com.ldtteam.jam.spi.ast.metadata.IMetadataClass;
import com.ldtteam.jam.spi.ast.named.INamedClass;
import com.ldtteam.jam.spi.ast.named.INamedField;
import com.ldtteam.jam.spi.ast.named.INamedMethod;
import com.ldtteam.jam.spi.ast.named.builder.INamedClassBuilder;
import com.ldtteam.jam.spi.ast.named.builder.INamedFieldBuilder;
import com.ldtteam.jam.spi.ast.named.builder.INamedMethodBuilder;
import com.ldtteam.jam.spi.name.INameProvider;
import com.ldtteam.jam.spi.name.INotObfuscatedFilter;
import com.ldtteam.jam.spi.name.IRemapper;
import com.ldtteam.jam.util.MethodDataUtils;
import com.ldtteam.jam.util.NamingUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class NamedClassBuilder implements INamedClassBuilder
{

    public record ClassNamingInformation(ClassData target, ClassData mappedFrom, Integer id, Optional<INamedClass> outerNamedClass) {}

    public static INamedClassBuilder create(
      IRemapper runtimeToASTRemapper, INameProvider<ClassNamingInformation> classNameProvider, INamedFieldBuilder namedFieldBuilder, INamedMethodBuilder namedMethodBuilder, INotObfuscatedFilter<ClassData> classNotObfuscatedFilter, INotObfuscatedFilter<MethodData> methodNotObfuscatedFilter
    )
    {
        return new NamedClassBuilder(runtimeToASTRemapper, classNameProvider, namedFieldBuilder, namedMethodBuilder, classNotObfuscatedFilter, methodNotObfuscatedFilter);
    }

    private final IRemapper              runtimeToASTRemapper;
    private final INameProvider<ClassNamingInformation> classNameProvider;
    private final INamedFieldBuilder     namedFieldBuilder;
    private final INamedMethodBuilder    namedMethodBuilder;
    private final INotObfuscatedFilter<ClassData> classNotObfuscatedFilter;
    private final INotObfuscatedFilter<MethodData> methodNotObfuscatedFilter;

    private NamedClassBuilder(
            IRemapper runtimeToASTRemapper,
            INameProvider<ClassNamingInformation> classNameProvider,
            INamedFieldBuilder namedFieldBuilder,
            INamedMethodBuilder namedMethodBuilder,
            INotObfuscatedFilter<ClassData> classNotObfuscatedFilter,
            INotObfuscatedFilter<MethodData> methodNotObfuscatedFilter)
    {
        this.runtimeToASTRemapper = runtimeToASTRemapper;
        this.classNameProvider = classNameProvider;
        this.namedFieldBuilder = namedFieldBuilder;
        this.namedMethodBuilder = namedMethodBuilder;
        this.classNotObfuscatedFilter = classNotObfuscatedFilter;
        this.methodNotObfuscatedFilter = methodNotObfuscatedFilter;
    }

    @Override
    public INamedClass build(final ClassData classData,
                             final IMetadataAST metadataAST,
                             final Map<String, ClassData> classDatasByAstName,
                             final Multimap<ClassData, ClassData> inheritanceVolumes,
                             final Map<MethodData, MethodData> rootMethodsByOverride,
                             final Multimap<MethodData, MethodData> overrideTree,
                             final BiMap<ClassData, ClassData> classMappings,
                             final BiMap<FieldData, FieldData> fieldMappings,
                             final BiMap<MethodData, MethodData> methodMappings,
                             final BiMap<ParameterData, ParameterData> parameterMappings,
                             final BiMap<ClassData, Integer> classIds,
                             final BiMap<FieldData, Integer> fieldIds,
                             final BiMap<MethodData, Integer> methodIds,
                             final BiMap<ParameterData, Integer> parameterIds,
                             final BiMap<String, INamedClass> alreadyNamedClasses) {
        final String originalClassName = runtimeToASTRemapper.remapClass(classData.node().name)
                .orElseThrow(() -> new IllegalStateException("Failed to remap class: %s".formatted(classData.node().name)));

        final IMetadataClass classMetadata = metadataAST.getClassesByName().computeIfAbsent(originalClassName, (key) -> {
            throw new IllegalStateException("Missing metadata for %s (%s)".formatted(classData.node().name, key));
        });

        final Map<String, String> identifiedFieldNamesByOriginalFieldName = Maps.newHashMap();
        final Collection<INamedField> fields = new ArrayList<>();
        classData.node().fields.forEach(fieldNode -> {
            final INamedField field = namedFieldBuilder.build(
                    classData,
                    new FieldData(classData, fieldNode),
                    classMetadata,
                    inheritanceVolumes,
                    fieldMappings,
                    fieldIds
            );

            fields.add(field);
            identifiedFieldNamesByOriginalFieldName.put(field.originalName(), field.identifiedName());
        });

        final Collection<INamedMethod> methods = new ArrayList<>();
        classData.node().methods.forEach(methodNode -> {
            final INamedMethod method = namedMethodBuilder.build(
                    classData,
                    new MethodData(classData, methodNode),
                    classMetadata,
                    classDatasByAstName,
                    inheritanceVolumes,
                    rootMethodsByOverride,
                    overrideTree,
                    identifiedFieldNamesByOriginalFieldName,
                    methodMappings,
                    parameterMappings,
                    methodIds,
                    parameterIds
            );

            methods.add(method);
        });

        final String outerClassName = NamingUtils.getOuterClassName(originalClassName);
        final Optional<INamedClass> outerClassNaming = Optional.ofNullable(alreadyNamedClasses.get(outerClassName));

        final ClassNamingInformation classNamingInformation = new ClassNamingInformation(
                classData,
                classMappings.get(classData),
                classIds.get(classData),
                outerClassNaming
        );

        String identifiedClassName = isNotObfuscated(classData, rootMethodsByOverride) ? classData.node().name : classNameProvider.getName(classNamingInformation);

        return new NamedClass(
                originalClassName,
                identifiedClassName,
                classIds.get(classData),
                fields,
                methods
        );
    }

    private boolean isNotObfuscated(ClassData classData, Map<MethodData, MethodData> rootMethodsByOverride) {
        return classNotObfuscatedFilter.isNotObfuscated(classData) || classData.node().methods.stream().anyMatch(methodNode -> {
            final MethodData data = new MethodData(classData, methodNode);
            final MethodData rootMethod = rootMethodsByOverride.getOrDefault(data, data);
            return methodNotObfuscatedFilter.isNotObfuscated(rootMethod);
        });
    }

    private record NamedClass(String originalName, String identifiedName, int id, Collection<INamedField> fields, Collection<INamedMethod> methods) implements INamedClass {}
}
