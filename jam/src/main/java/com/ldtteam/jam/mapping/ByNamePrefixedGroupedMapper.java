package com.ldtteam.jam.mapping;

import com.ldtteam.jam.spi.mapping.IMapper;
import com.ldtteam.jam.spi.name.INameProvider;

import java.util.function.Function;

public class ByNamePrefixedGroupedMapper<N> extends GroupedMapper<N, String>
{
    public ByNamePrefixedGroupedMapper(
      final INameProvider<N> nameProvider,
      final Function<String, String> prefixSelector,
      final Function<String, IMapper<N>> inner
    )
    {
        super(
          t -> prefixSelector.apply(nameProvider.getName(t)),
          inner
        );
    }
}
