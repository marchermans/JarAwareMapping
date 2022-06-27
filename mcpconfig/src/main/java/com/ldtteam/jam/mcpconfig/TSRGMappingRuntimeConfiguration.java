package com.ldtteam.jam.mcpconfig;

import com.ldtteam.jam.mapping.*;
import com.ldtteam.jam.matching.instructionlist.DelegatingInstructionListMatcher;
import com.ldtteam.jam.matching.instructionlist.DiffBasedInstructionListMatcher;
import com.ldtteam.jam.matching.instructionlist.DirectInstructionListMatcher;
import com.ldtteam.jam.spi.configuration.MappingConfiguration;
import com.ldtteam.jam.spi.configuration.MappingRuntimeConfiguration;
import com.ldtteam.jam.spi.matching.IMatcher;
import org.objectweb.asm.tree.InsnList;

public class TSRGMappingRuntimeConfiguration
{

    private TSRGMappingRuntimeConfiguration()
    {
        throw new IllegalStateException("Can not instantiate an instance of: TSRGMappingRuntimeConfiguration. This is a utility class");
    }

    public static MappingRuntimeConfiguration create(MappingConfiguration mappingConfiguration) {
        final IMatcher<InsnList> instructionListMatcher = DelegatingInstructionListMatcher.create(
          DirectInstructionListMatcher.create(),
          DiffBasedInstructionListMatcher.create(mappingConfiguration.mappingThresholdPercentage(), mappingConfiguration.minimalInstructionCount())
        );

        return new MappingRuntimeConfiguration(
          NameBasedMapper.classes(),
          LambdaAwareMethodMapper.create(
            PhasedMapper.create(
              NameBasedMapper.methods(),
              ConstantBooleanReturnValuesFlippedMethodMapper.create(instructionListMatcher),
              ByteCodeBasedMethodMapper.create(
                instructionListMatcher
              )
            ),
            PhasedMapper.create(
              AlignedMapper.methods(
                ByteCodeBasedMethodMapper.create(instructionListMatcher)
              ),
              NameBasedMapper.methods()
            )
          ),
          NameBasedMapper.fields(),
          AlignedMapper.parameters(
            TypeAwareParameterMapper.create()
          )
        );
    }
}
