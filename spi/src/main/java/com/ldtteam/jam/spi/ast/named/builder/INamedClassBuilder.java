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

public interface INamedClassBuilder
{
    INamedClass build(
            ClassData classData,
            IMetadataAST metadataAST,
            Map<String, ClassData> classDatasByAstName,
            Multimap<ClassData, ClassData> inheritanceVolumes,
            Map<MethodData, MethodData> rootMethodsByOverride,
            Multimap<MethodData, MethodData> overrideTree, BiMap<ClassData, ClassData> classMappings,
            BiMap<FieldData, FieldData> fieldMappings,
            BiMap<MethodData, MethodData> methodMappings,
            BiMap<ParameterData, ParameterData> parameterMappings,
            BiMap<ClassData, Integer> classIds,
            BiMap<FieldData, Integer> fieldIds,
            BiMap<MethodData, Integer> methodIds,
            BiMap<ParameterData, Integer> parameterIds,
            BiMap<String, INamedClass> alreadyNamedClasses
    );
}
