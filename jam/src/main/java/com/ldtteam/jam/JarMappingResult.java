package com.ldtteam.jam;

import com.ldtteam.jam.spi.asm.ClassData;
import com.ldtteam.jam.spi.asm.FieldData;
import com.ldtteam.jam.spi.asm.MethodData;
import com.ldtteam.jam.spi.asm.ParameterData;
import com.ldtteam.jam.spi.mapping.MappingResult;

public record JarMappingResult(MappingResult<ClassData> classes, MappingResult<MethodData> methods, MappingResult<FieldData> fields, MappingResult<ParameterData> parameters)
{
}
