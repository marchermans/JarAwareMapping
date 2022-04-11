package com.ldtteam.jam.jamspec.mapping;

import java.util.Set;

public interface IMapper<T>
{
    MappingResult<T> map(final Set<T> sources, final Set<T> candidates);
}
