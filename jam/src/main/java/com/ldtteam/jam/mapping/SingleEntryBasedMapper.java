package com.ldtteam.jam.mapping;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.ldtteam.jam.spi.mapping.IMapper;
import com.ldtteam.jam.spi.mapping.MappingResult;
import com.ldtteam.jam.util.SetsUtil;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

abstract class SingleEntryBasedMapper<T> implements IMapper<T>
{
    @Override
    public MappingResult<T> map(final Set<T> sources, final Set<T> candidates)
    {
        final Set<T> unmappedSources = SetsUtil.cloneSet(sources);
        final Set<T> unmappedCandidates = SetsUtil.cloneSet(candidates);
        final BiMap<T, T> mappings = HashBiMap.create(sources.size());

        sources.forEach(source -> {
            if (unmappedCandidates.isEmpty())
                return;

            final Optional<T> candidate = map(source, unmappedCandidates);
            candidate.ifPresent(target -> {
                unmappedSources.remove(source);
                unmappedCandidates.remove(target);
                mappings.put(source, target);
            });
        });

        return new MappingResult<T>(unmappedSources, mappings, unmappedCandidates);
    }

    protected abstract Optional<T> map(T source, Set<T> candidates);
}
