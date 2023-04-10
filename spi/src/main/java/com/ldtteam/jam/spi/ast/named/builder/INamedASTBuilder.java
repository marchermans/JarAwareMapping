package com.ldtteam.jam.spi.ast.named.builder;

import com.google.common.collect.BiMap;
import com.ldtteam.jam.spi.asm.*;
import com.ldtteam.jam.spi.ast.metadata.IMetadataAST;
import com.ldtteam.jam.spi.ast.named.INamedAST;

public interface INamedASTBuilder<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload>
{
    INamedAST build(
      BiMap<ClassData<TClassPayload>, ClassData<TClassPayload>> classMappings,
      BiMap<FieldData<TClassPayload, TFieldPayload>, FieldData<TClassPayload, TFieldPayload>> fieldMappings,
      BiMap<MethodData<TClassPayload, TMethodPayload>, MethodData<TClassPayload, TMethodPayload>> methodMappings,
      BiMap<ParameterData<TClassPayload, TMethodPayload, TParameterPayload>, ParameterData<TClassPayload, TMethodPayload, TParameterPayload>> parameterMappings,
      BiMap<ClassData<TClassPayload>, Integer> classIds,
      BiMap<MethodData<TClassPayload, TMethodPayload>, Integer> methodIds,
      BiMap<FieldData<TClassPayload, TFieldPayload>, Integer> fieldIds,
      BiMap<ParameterData<TClassPayload, TMethodPayload, TParameterPayload>, Integer> parameterIds,
      IASMData<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> asmData,
      IMetadataAST metadataAST
    );
}
