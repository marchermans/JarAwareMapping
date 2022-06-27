package com.ldtteam.jam.spi.ast.named.builder;

import com.google.common.collect.BiMap;
import com.ldtteam.jam.spi.asm.ClassData;
import com.ldtteam.jam.spi.asm.MethodData;
import com.ldtteam.jam.spi.asm.ParameterData;
import com.ldtteam.jam.spi.ast.metadata.IMetadataClass;
import com.ldtteam.jam.spi.ast.named.INamedParameter;

import java.util.Map;

public interface INamedParameterBuilder
{
    INamedParameter build(
      ClassData classData,
      MethodData methodData,
      ParameterData parameterData,
      int index,
      IMetadataClass classMetadata,
      Map<String, String> identifiedFieldNamesByASTName,
      BiMap<ParameterData, ParameterData> parameterMappings,
      BiMap<ParameterData, Integer> parameterIds
    );
}
