package com.ldtteam.jam.spi.identification;

import com.ldtteam.jam.spi.asm.ClassData;
import com.ldtteam.jam.spi.asm.FieldData;
import com.ldtteam.jam.spi.asm.MethodData;
import com.ldtteam.jam.spi.asm.ParameterData;

public interface IExistingIdentitySupplier
{
    int getClassIdentity(final ClassData classData);

    int getMethodIdentity(final MethodData methodData);

    int getFieldIdentity(final FieldData fieldData);

    int getParameterIdentity(final ParameterData parameterData);
}
