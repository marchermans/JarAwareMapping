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
import com.ldtteam.jam.util.NamingUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class NamedClassBuilder<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> implements INamedClassBuilder<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload>
{

    public record ClassNamingInformation<TClassPayload>(ClassData<TClassPayload> target, ClassData<TClassPayload> mappedFrom, Integer id, Optional<INamedClass> outerNamedClass) {}

    public static <TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> INamedClassBuilder<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> create(
      IRemapper runtimeToASTRemapper, INameProvider<ClassNamingInformation<TClassPayload>> classNameProvider, INamedFieldBuilder namedFieldBuilder, INamedMethodBuilder namedMethodBuilder, INotObfuscatedFilter<ClassData<TClassPayload>> filter
    )
    {
        return new NamedClassBuilder<>(runtimeToASTRemapper, classNameProvider, namedFieldBuilder, namedMethodBuilder, filter);
    }

    private final IRemapper              runtimeToASTRemapper;
    private final INameProvider<ClassNamingInformation<TClassPayload>> classNameProvider;
    private final INamedFieldBuilder     namedFieldBuilder;
    private final INamedMethodBuilder    namedMethodBuilder;
    private final INotObfuscatedFilter<ClassData<TClassPayload>> filter;

    private NamedClassBuilder(
            IRemapper runtimeToASTRemapper,
            INameProvider<ClassNamingInformation<TClassPayload>> classNameProvider,
            INamedFieldBuilder namedFieldBuilder,
            INamedMethodBuilder namedMethodBuilder, INotObfuscatedFilter<ClassData<TClassPayload>> filter)
    {
        this.runtimeToASTRemapper = runtimeToASTRemapper;
        this.classNameProvider = classNameProvider;
        this.namedFieldBuilder = namedFieldBuilder;
        this.namedMethodBuilder = namedMethodBuilder;
        this.filter = filter;
    }

    @Override
    public INamedClass build(ClassData<TClassPayload> classData,
                             IMetadataAST metadataAST,
                             Map<String, ClassData<TClassPayload>> classDatasByAstName,
                             Multimap<ClassData<TClassPayload>, ClassData<TClassPayload>> inheritanceVolumes,
                             Map<MethodData<TClassPayload, TMethodPayload>, MethodData<TClassPayload, TMethodPayload>> rootMethodsByOverride,
                             Multimap<MethodData<TClassPayload, TMethodPayload>, MethodData<TClassPayload, TMethodPayload>> overrideTree, BiMap<ClassData<TClassPayload>, ClassData<TClassPayload>> classMappings,
                             BiMap<FieldData<TClassPayload, TFieldPayload>, FieldData<TClassPayload, TFieldPayload>> fieldMappings,
                             BiMap<MethodData<TClassPayload, TMethodPayload>, MethodData<TClassPayload, TMethodPayload>> methodMappings,
                             BiMap<ParameterData<TClassPayload, TMethodPayload, TParameterPayload>, ParameterData<TClassPayload, TMethodPayload, TParameterPayload>> parameterMappings,
                             BiMap<ClassData<TClassPayload>, Integer> classIds,
                             BiMap<FieldData<TClassPayload, TFieldPayload>, Integer> fieldIds,
                             BiMap<MethodData<TClassPayload, TMethodPayload>, Integer> methodIds,
                             BiMap<ParameterData<TClassPayload, TMethodPayload, TParameterPayload>, Integer> parameterIds,
                             BiMap<String, INamedClass> alreadyNamedClasses) {
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

        String identifiedClassName = filter.isNotObfuscated(classData) ? classData.node().name : classNameProvider.getName(classNamingInformation);

        return new NamedClass(
                originalClassName,
                identifiedClassName,
                classIds.get(classData),
                fields,
                methods
        );
    }

    private record NamedClass(String originalName, String identifiedName, int id, Collection<INamedField> fields, Collection<INamedMethod> methods) implements INamedClass {}
}
