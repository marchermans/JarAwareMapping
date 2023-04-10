package com.ldtteam.jam.spi.ast.named.builder;

import com.google.common.collect.BiMap;
import com.google.common.collect.Multimap;
import com.ldtteam.jam.spi.asm.ClassData;
import com.ldtteam.jam.spi.asm.FieldData;
import com.ldtteam.jam.spi.asm.MethodData;
import com.ldtteam.jam.spi.asm.ParameterData;
import com.ldtteam.jam.spi.ast.metadata.IMetadataAST;
import com.ldtteam.jam.spi.ast.named.INamedClass;

import java.util.Map;

public interface INamedClassBuilder<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload>
{
    INamedClass build(
            ClassData<TClassPayload> classData,
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
            BiMap<String, INamedClass> alreadyNamedClasses
    );
}
