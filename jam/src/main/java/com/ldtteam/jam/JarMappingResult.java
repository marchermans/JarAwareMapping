package com.ldtteam.jam;

import com.ldtteam.jam.spi.asm.ClassData;
import com.ldtteam.jam.spi.asm.FieldData;
import com.ldtteam.jam.spi.asm.MethodData;
import com.ldtteam.jam.spi.asm.ParameterData;
import com.ldtteam.jam.spi.mapping.MappingResult;

public record JarMappingResult<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload>(MappingResult<ClassData<TClassPayload>> classes, MappingResult<MethodData<TClassPayload, TMethodPayload>> methods, MappingResult<FieldData<TClassPayload, TFieldPayload>> fields, MappingResult<ParameterData<TClassPayload, TMethodPayload, TParameterPayload>> parameters)
{
}
