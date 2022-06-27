package com.ldtteam.jam.spi.name;

import com.ldtteam.jam.spi.asm.ClassData;
import com.ldtteam.jam.spi.asm.FieldData;
import com.ldtteam.jam.spi.asm.MethodData;
import com.ldtteam.jam.spi.asm.ParameterData;

import java.util.Optional;

public interface IExistingNameSupplier
{
    Optional<String> getClassName(final ClassData classData);

    Optional<String> getFieldName(final FieldData fieldData);

    Optional<String> getMethodName(final MethodData methodData);

    Optional<String> getParameterName(final ParameterData parameterData);
}
