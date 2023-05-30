package com.ldtteam.jam.mcpconfig;

import com.ldtteam.jam.mapping.AlignedMapper;
import com.ldtteam.jam.mapping.ByteCodeBasedMethodMapper;
import com.ldtteam.jam.mapping.ConstantBooleanReturnValuesFlippedMethodMapper;
import com.ldtteam.jam.mapping.LambdaAwareMethodMapper;
import com.ldtteam.jam.mapping.NameBasedMapper;
import com.ldtteam.jam.mapping.PhasedMapper;
import com.ldtteam.jam.mapping.TypeAwareParameterMapper;
import com.ldtteam.jam.matching.instructionlist.DelegatingInstructionListMatcher;
import com.ldtteam.jam.matching.instructionlist.DiffBasedInstructionListMatcher;
import com.ldtteam.jam.matching.instructionlist.DirectInstructionListMatcher;
import com.ldtteam.jam.spi.configuration.MappingConfiguration;
import com.ldtteam.jam.spi.configuration.MappingRuntimeConfiguration;
import com.ldtteam.jam.spi.matching.IMatcher;
import org.objectweb.asm.tree.InsnList;

public class TSRGMappingRuntimeConfiguration {

    private TSRGMappingRuntimeConfiguration() {
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
                                PhasedMapper.create(
                                        ByteCodeBasedMethodMapper.exact(
                                                instructionListMatcher
                                        ),
                                        NameBasedMapper.methodsByNameOnly()
                                )
                        ),
                        PhasedMapper.create(
                                AlignedMapper.methods(
                                        ByteCodeBasedMethodMapper.terminating(instructionListMatcher)
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
