package com.ldtteam.jam.mapping;

import com.ldtteam.jam.spi.asm.ParameterData;
import com.ldtteam.jam.spi.mapping.IMapper;

import java.util.Optional;
import java.util.Set;

public class TypeAwareParameterMapper extends SingleEntryBasedMapper<ParameterData>
{

    public static IMapper<ParameterData> create() {
        return new TypeAwareParameterMapper();
    }

    private TypeAwareParameterMapper() {
    }

    @Override
    protected Optional<ParameterData> map(final ParameterData source, final Set<ParameterData> candidates)
    {
        for (final ParameterData candidate : candidates) {
            if (source.desc().equals(candidate.desc())) {
                return Optional.of(candidate);
            }
        }

        return Optional.empty();
    }
}
