package com.ldtteam.jam.spi.ast.named.builder;

import com.google.common.collect.BiMap;
import com.google.common.collect.Multimap;
import com.ldtteam.jam.spi.asm.ClassData;
import com.ldtteam.jam.spi.asm.MethodData;
import com.ldtteam.jam.spi.asm.ParameterData;
import com.ldtteam.jam.spi.ast.metadata.IMetadataClass;
import com.ldtteam.jam.spi.ast.named.INamedMethod;

import java.util.Map;

public interface INamedMethodBuilder
{
    INamedMethod build(
            ClassData classData,
            MethodData methodData,
            IMetadataClass classMetadata,
            Map<String, ClassData> classDatasByAstName,
            Multimap<ClassData, ClassData> inheritanceVolumes, Map<MethodData, MethodData> rootMethodsByOverride,
            Multimap<MethodData, MethodData> overrideTree,
            Map<String, String> identifiedFieldNamesByASTName,
            BiMap<MethodData, MethodData> methodMappings,
            BiMap<ParameterData, ParameterData> parameterMappings,
            BiMap<MethodData, Integer> methodIds,
            BiMap<ParameterData, Integer> parameterIds);
}
