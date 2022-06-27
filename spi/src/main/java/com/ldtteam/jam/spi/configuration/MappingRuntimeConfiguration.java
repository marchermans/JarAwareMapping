package com.ldtteam.jam.spi.configuration;

import com.ldtteam.jam.spi.asm.ClassData;
import com.ldtteam.jam.spi.asm.FieldData;
import com.ldtteam.jam.spi.asm.MethodData;
import com.ldtteam.jam.spi.asm.ParameterData;
import com.ldtteam.jam.spi.mapping.IMapper;

public record MappingRuntimeConfiguration(IMapper<ClassData> classMapper, IMapper<MethodData> methodMapper, IMapper<FieldData> fieldMapper, IMapper<ParameterData> parameterMapper)
{
}
