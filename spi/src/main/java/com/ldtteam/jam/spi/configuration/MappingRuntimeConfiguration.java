package com.ldtteam.jam.spi.configuration;

import com.ldtteam.jam.spi.asm.ClassData;
import com.ldtteam.jam.spi.asm.FieldData;
import com.ldtteam.jam.spi.asm.MethodData;
import com.ldtteam.jam.spi.asm.ParameterData;
import com.ldtteam.jam.spi.mapping.IMapper;

public record MappingRuntimeConfiguration<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload>(IMapper<ClassData<TClassPayload>> classMapper, IMapper<MethodData<TClassPayload, TMethodPayload>> methodMapper, IMapper<FieldData<TClassPayload, TFieldPayload>> fieldMapper, IMapper<ParameterData<TClassPayload, TMethodPayload, TParameterPayload>> parameterMapper)
{
}
