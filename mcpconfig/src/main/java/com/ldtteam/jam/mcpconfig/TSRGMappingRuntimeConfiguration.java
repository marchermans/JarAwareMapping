package com.ldtteam.jam.mcpconfig;

import com.ldtteam.jam.mapping.*;
import com.ldtteam.jam.spi.configuration.MappingRuntimeConfiguration;

public class TSRGMappingRuntimeConfiguration
{

    private TSRGMappingRuntimeConfiguration()
    {
        throw new IllegalStateException("Can not instantiate an instance of: TSRGMappingRuntimeConfiguration. This is a utility class");
    }

    public static MappingRuntimeConfiguration create() {
        return new MappingRuntimeConfiguration(
          NameBasedMapper.classes(),
          LambdaAwareMethodMapper.create(
            PhasedMapper.create(
              NameBasedMapper.methods(),
              ByteCodeBasedMethodMapper.create()
            ),
            PhasedMapper.create(
              AlignedMapper.methods(
                ByteCodeBasedMethodMapper.create()
              ),
              NameBasedMapper.methods()
            )
          ),
          NameBasedMapper.fields()
        );
    }
}
