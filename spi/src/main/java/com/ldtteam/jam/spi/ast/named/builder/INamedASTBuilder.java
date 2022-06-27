package com.ldtteam.jam.spi.ast.named.builder;

import com.google.common.collect.BiMap;
import com.ldtteam.jam.spi.asm.*;
import com.ldtteam.jam.spi.ast.metadata.IMetadataAST;
import com.ldtteam.jam.spi.ast.named.INamedAST;

public interface INamedASTBuilder
{
    INamedAST build(
      BiMap<ClassData, ClassData> classMappings,
      BiMap<FieldData, FieldData> fieldMappings,
      BiMap<MethodData, MethodData> methodMappings,
      BiMap<ParameterData, ParameterData> parameterMappings,
      BiMap<ClassData, Integer> classIds,
      BiMap<MethodData, Integer> methodIds,
      BiMap<FieldData, Integer> fieldIds,
      BiMap<ParameterData, Integer> parameterIds,
      IASMData asmData,
      IMetadataAST metadataAST
    );
}
